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

const MAX_BINARY_BYTES = 16 * 1024 * 1024

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
  let value: unknown
  try {
    value = JSON.parse(json)
  } catch (cause) {
    throw new ProtobufToolError('jsonParse', { detail: cause instanceof Error ? cause.message : String(cause) })
  }
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new ProtobufToolError('jsonObject')
  }
  let message
  try {
    message = type.fromObject(value)
  } catch (cause) {
    throw new ProtobufToolError('messageConvert', { detail: cause instanceof Error ? cause.message : String(cause) })
  }
  const validationError = type.verify(message)
  if (validationError) throw new ProtobufToolError('messageValidate', { detail: validationError })
  const bytes = type.encode(message).finish()
  return formatBinary(bytes, format)
}

export function decodeProtobuf(
  schema: string,
  messageName: string,
  binary: string,
  format: ProtobufBinaryFormat
): string {
  const type = resolveType(schema, messageName)
  const bytes = parseBinary(binary, format)
  try {
    const message = type.decode(bytes)
    return JSON.stringify(type.toObject(message, {
      defaults: true,
      arrays: true,
      objects: true,
      longs: String,
      enums: String,
      bytes: String
    }), null, 2)
  } catch (cause) {
    throw new ProtobufToolError('decode', { detail: cause instanceof Error ? cause.message : String(cause) })
  }
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
  while (reader.pos < reader.len) {
    const tag = reader.uint32()
    if (tag === 0) throw new ProtobufToolError('wireTag')
    const field = tag >>> 3
    const wireType = tag & 7
    const start = reader.pos
    reader.skipType(wireType)
    const byteLength = reader.pos - start
    lines.push(`field ${field} · ${wireTypeName(wireType)} · ${byteLength} byte${byteLength === 1 ? '' : 's'}`)
  }
  return lines.join('\n')
}

export function parseBinary(binary: string, format: ProtobufBinaryFormat): Uint8Array {
  const compact = binary.replace(/\s+/g, '')
  if (!compact) return new Uint8Array()
  if (format === 'hex') {
    if (!/^(?:[0-9a-f]{2})+$/i.test(compact)) throw new ProtobufToolError('hex')
  } else if (!/^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$/.test(compact)) {
    throw new ProtobufToolError('base64')
  }
  const bytes = Buffer.from(compact, format)
  if (bytes.length > MAX_BINARY_BYTES) throw new ProtobufToolError('binaryLimit')
  return Uint8Array.from(bytes)
}

function formatBinary(bytes: Uint8Array, format: ProtobufBinaryFormat): string {
  if (bytes.length > MAX_BINARY_BYTES) throw new ProtobufToolError('binaryLimit')
  return Buffer.from(bytes).toString(format)
}

function parseSchema(schema: string): Root {
  if (!schema.trim()) throw new ProtobufToolError('schemaEmpty')
  if (schema.length > 2 * 1024 * 1024) throw new ProtobufToolError('schemaLimit')
  try {
    return parse(schema, { keepCase: true }).root
  } catch (cause) {
    throw new ProtobufToolError('schemaParse', { detail: cause instanceof Error ? cause.message : String(cause) })
  }
}

function resolveType(schema: string, messageName: string): Type {
  if (!messageName.trim()) throw new ProtobufToolError('messageRequired')
  const root = parseSchema(schema)
  try {
    return root.lookupType(messageName.trim())
  } catch {
    throw new ProtobufToolError('messageMissing', { name: messageName.trim() })
  }
}

function collectTypes(root: Root | Namespace, prefix: string, output: string[]): void {
  for (const item of root.nestedArray) {
    const name = prefix ? `${prefix}.${item.name}` : item.name
    if (item instanceof Type) output.push(name)
    if (item instanceof Namespace) collectTypes(item, name, output)
  }
}

function wireTypeName(value: number): string {
  return ['Varint', '64-bit', 'Length-delimited', 'Start group', 'End group', '32-bit'][value]
    ?? `Unknown(${value})`
}
