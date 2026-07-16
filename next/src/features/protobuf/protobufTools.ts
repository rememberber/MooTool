import { Buffer } from 'buffer'
import { Reader, parse, type Type } from 'protobufjs'

export type BinaryFormat = 'Hex' | 'Base64'

export function jsonToProtobuf(protoDefinition: string, messageName: string, json: string, format: BinaryFormat): string {
  const type = resolveMessageType(protoDefinition, messageName)
  const object = JSON.parse(json) as Record<string, unknown>
  const error = type.verify(object)
  if (error) throw new Error(error)
  return formatBinary(type.encode(type.fromObject(object)).finish(), format)
}

export function protobufToJson(protoDefinition: string, messageName: string, input: string, format: BinaryFormat): string {
  const type = resolveMessageType(protoDefinition, messageName)
  const message = type.decode(parseBinary(input, format))
  return JSON.stringify(type.toObject(message, { defaults: true, longs: String, enums: String, bytes: String }), null, 2)
}

export function convertBinary(input: string, from: BinaryFormat, to: BinaryFormat): string {
  return formatBinary(parseBinary(input, from), to)
}

export function parseBinary(input: string, format: BinaryFormat): Uint8Array {
  const value = input.replace(/\s+/g, '')
  if (format === 'Base64') {
    if (!/^[A-Za-z0-9+/]*={0,2}$/.test(value)) throw new Error('Invalid Base64 input')
    return Uint8Array.from(Buffer.from(value, 'base64'))
  }
  if (!/^(?:[0-9a-fA-F]{2})*$/.test(value)) throw new Error('Invalid Hex input')
  return Uint8Array.from(Buffer.from(value, 'hex'))
}

export function formatBinary(bytes: Uint8Array, format: BinaryFormat): string {
  return Buffer.from(bytes).toString(format === 'Base64' ? 'base64' : 'hex')
}

export function decodeWire(input: string, format: BinaryFormat): string {
  const reader = Reader.create(parseBinary(input, format))
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

function resolveMessageType(protoDefinition: string, messageName: string): Type {
  if (!protoDefinition.trim()) throw new Error('.proto definition is required')
  if (!messageName.trim()) throw new Error('Message name is required')
  const root = parse(protoDefinition, { keepCase: true }).root
  const resolved = root.lookupType(messageName.trim())
  return resolved
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
    return [...text].every((char) => char === '\n' || char === '\r' || char === '\t' || char.codePointAt(0)! >= 32) ? text : null
  } catch { return null }
}

function wireTypeName(type: number): string {
  return ['Varint', '64-bit', 'Length-delimited', 'Start group', 'End group', '32-bit'][type] ?? 'Unknown'
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
