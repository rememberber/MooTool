import { Buffer } from 'buffer'
import { Namespace, parse, Reader, Type, type Root } from 'protobufjs'

export type ProtobufBinaryFormat = 'base64' | 'hex'
export type ProtobufToolErrorCode = 'jsonParse' | 'jsonObject' | 'messageConvert' | 'messageValidate' | 'decode' | 'wireTag' | 'hex' | 'base64' | 'binaryLimit' | 'schemaEmpty' | 'schemaLimit' | 'schemaParse' | 'messageRequired' | 'messageMissing'

export class ProtobufToolError extends Error {
  constructor(readonly code: ProtobufToolErrorCode, readonly values?: Record<string, string>) {
    super(`PROTOBUF_TOOL_${code}`)
    this.name = 'ProtobufToolError'
  }
}

export interface ProtobufSchemaInfo {
  messageNames: string[]
  packageName: string
}

export function inspectProtobufSchema(schema: string): ProtobufSchemaInfo {
  const root = parseSchema(schema)
  const messageNames: string[] = []
  collectTypes(root, '', messageNames)
  return {
    messageNames: messageNames.sort(),
    packageName: parse(schema, { keepCase: true }).package ?? ''
  }
}

export function encodeProtobuf(
  schema: string,
  messageName: string,
  json: string,
  format: ProtobufBinaryFormat
): string {
  const type = resolveType(schema, messageName)
  const object = JSON.parse(json) as Record<string, unknown>
  const error = type.verify(object)
  if (error) throw new Error(error)
  return formatBinary(type.encode(type.fromObject(object)).finish(), format)
}

export function decodeProtobuf(
  schema: string,
  messageName: string,
  binary: string,
  format: ProtobufBinaryFormat
): string {
  const type = resolveType(schema, messageName)
  const message = type.decode(parseBinary(binary, format))
  return JSON.stringify(type.toObject(message, { defaults: true, longs: String, enums: String, bytes: String }), null, 2)
}

export function convertProtobufBinary(
  binary: string,
  from: ProtobufBinaryFormat,
  to: ProtobufBinaryFormat
): string {
  return formatBinary(parseBinary(binary, from), to)
}

export function inspectWire(binary: string, format: ProtobufBinaryFormat): string {
  const reader = Reader.create(parseBinary(binary, format))
  const lines: string[] = []
  let index = 0
  while (reader.pos < reader.len) {
    const tag = reader.uint32()
    if (tag === 0) break
    const field = tag >>> 3
    const wireType = tag & 7
    index += 1
    const value = readWireValue(reader, wireType)
    lines.push(`#${index}  field=${field}  wire_type=${wireType} (${wireTypeName(wireType)})  value=${value}`)
  }
  return lines.join('\n') || '(empty data)'
}

export function formatProtoDefinition(proto: string): string {
  if (!proto.trim()) return ''
  const tokens = tokenizeProto(proto)
  const lines: string[] = []
  let level = 0
  for (const token of tokens) {
    if (token === '}') level = Math.max(0, level - 1)
    lines.push(`${'  '.repeat(level)}${token}`)
    if (token.endsWith('{')) level += 1
  }
  return lines.join('\n')
}

export function parseBinary(binary: string, format: ProtobufBinaryFormat): Uint8Array {
  const value = binary.replace(/\s+/g, '')
  if (format === 'base64') {
    if (!/^[A-Za-z0-9+/]*={0,2}$/.test(value)) throw new Error('Invalid Base64 input')
    return Uint8Array.from(Buffer.from(value, 'base64'))
  }
  if (!/^(?:[0-9a-fA-F]{2})*$/.test(value)) throw new Error('Invalid Hex input')
  return Uint8Array.from(Buffer.from(value, 'hex'))
}

function formatBinary(bytes: Uint8Array, format: ProtobufBinaryFormat): string {
  return Buffer.from(bytes).toString(format)
}

function parseSchema(schema: string): Root {
  if (!schema.trim()) throw new Error('.proto definition is required')
  return parse(schema, { keepCase: true }).root
}

function resolveType(schema: string, messageName: string): Type {
  if (!messageName.trim()) throw new Error('Message name is required')
  return parseSchema(schema).lookupType(messageName.trim())
}

function collectTypes(root: Root | Namespace, prefix: string, output: string[]): void {
  for (const item of root.nestedArray) {
    const name = prefix ? `${prefix}.${item.name}` : item.name
    if (item instanceof Type) output.push(name)
    if (item instanceof Namespace) collectTypes(item, name, output)
  }
}

function readWireValue(reader: Reader, wireType: number): string {
  if (wireType === 0) return reader.uint64().toString()
  if (wireType === 1) return `0x${readRawHex(reader, 8)}`
  if (wireType === 2) {
    const bytes = reader.bytes()
    if (bytes.length === 0) return '""'
    const text = decodePrintableUtf8(bytes)
    return text === null ? `bytes[${bytes.length}]=${Buffer.from(bytes).toString('hex')}` : JSON.stringify(text)
  }
  if (wireType === 5) return `0x${readRawHex(reader, 4)}`
  if (wireType === 3 || wireType === 4) {
    reader.skipType(wireType)
    return '(group)'
  }
  throw new Error(`Unsupported wire type: ${wireType}`)
}

function readRawHex(reader: Reader, length: number): string {
  if (reader.pos + length > reader.len) throw new Error('Unexpected end of wire data')
  const bytes = reader.buf.subarray(reader.pos, reader.pos + length)
  reader.pos += length
  return Buffer.from(bytes).toString('hex')
}

function decodePrintableUtf8(bytes: Uint8Array): string | null {
  try {
    const text = new TextDecoder('utf-8', { fatal: true }).decode(bytes)
    return [...text].every((char) => char === '\n' || char === '\r' || char === '\t' || (char.codePointAt(0) ?? 0) >= 32) ? text : null
  } catch {
    return null
  }
}

function wireTypeName(value: number): string {
  return ['Varint', '64-bit', 'Length-delimited', 'Start group', 'End group', '32-bit'][value] ?? 'Unknown'
}

function tokenizeProto(proto: string): string[] {
  const lines: string[] = []
  let current = ''
  let quote = ''
  let escaped = false
  let lineComment = false
  const flush = (): void => {
    const value = current.trim()
    if (value) lines.push(value)
    current = ''
  }
  for (let index = 0; index < proto.length; index += 1) {
    const char = proto[index]
    if (lineComment) {
      current += char
      if (char === '\n') { flush(); lineComment = false }
      continue
    }
    if (escaped) { current += char; escaped = false; continue }
    if (char === '\\') { current += char; escaped = true; continue }
    if (quote) { current += char; if (char === quote) quote = ''; continue }
    if (char === '"' || char === "'") { quote = char; current += char; continue }
    if (char === '/' && proto[index + 1] === '/') { lineComment = true; current += '//'; index += 1; continue }
    if (char === '{') { current = `${current.trimEnd()} {`; flush() }
    else if (char === '}') { flush(); lines.push('}') }
    else if (char === ';') { current = `${current.trimEnd()};`; flush() }
    else if (char === '\n' || char === '\r') flush()
    else current += char
  }
  flush()
  return lines
}
