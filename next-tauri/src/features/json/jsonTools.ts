import { XMLBuilder, XMLParser } from 'fast-xml-parser'
import { JSONPath } from 'jsonpath-plus'
export { contentFingerprint } from '../../shared/fingerprint'

export type JsonValidation =
  | { kind: 'idle' }
  | { kind: 'valid'; rootType: string }
  | { kind: 'error'; cause: unknown }

export type JsonToolErrorCode = 'pathEmpty' | 'notString' | 'contentEmpty' | 'parseDetail' | 'parsePosition' | 'swapValue' | 'xmlParse' | 'emptyXml' | 'duplicateKeys' | 'objectRequired' | 'emptyJavaBean' | 'noJavaFields'

export class JsonToolError extends Error {
  constructor(readonly code: JsonToolErrorCode, readonly values?: Record<string, string | number>) {
    super(`JSON_TOOL_${code}`)
    this.name = 'JsonToolError'
  }
}

export interface JsonAnalysis {
  rootType: string
  nodes: number
  keys: number
  maxDepth: number
  bytes: number
}

export interface JsonFormatOptions {
  indent: number
  sortKeys: boolean
  ignoreCase?: boolean
  checkDuplicateKeys?: boolean
}

export interface JsonPathEntry {
  path: string
  label: string
  depth: number
  value: unknown
}

export function formatJson(input: string, options: JsonFormatOptions): string {
  if (options.checkDuplicateKeys) {
    const duplicates = findDuplicateJsonKeys(input, options.ignoreCase)
    if (duplicates.length) throw new JsonToolError('duplicateKeys', { paths: duplicates.join(', ') })
  }
  return JSON.stringify(
    options.sortKeys ? sortJsonValue(parseJson(input), Boolean(options.ignoreCase)) : parseJson(input),
    null,
    options.indent
  )
}

export function findDuplicateJsonKeys(input: string, ignoreCase = false): string[] {
  parseJson(input)
  return new DuplicateKeyParser(input, ignoreCase).parse()
}

export function listJsonPaths(input: string): JsonPathEntry[] {
  const entries: JsonPathEntry[] = []
  collectJsonPaths(parseJson(input), '$', '$', 0, entries)
  return entries.slice(0, 5000)
}

export function javaBeanToJson(input: string): string {
  if (!input.trim()) throw new JsonToolError('emptyJavaBean')
  const result: Record<string, unknown> = {}
  const fieldPattern = /^(?:(?:public|protected|private)\s+)?(?:(?:static|final|transient|volatile)\s+)*([\w$.<>?, \[\]]+?)\s+(\w+)\s*(?:=.*)?$/
  for (const statement of input.split(';')) {
    const boundary = Math.max(statement.lastIndexOf('{'), statement.lastIndexOf('}'))
    const match = fieldPattern.exec(statement.slice(boundary + 1).trim())
    if (!match) continue
    const [, type, name] = match
    if (name === 'serialVersionUID') continue
    result[name] = mockJavaValue(type.trim())
  }
  if (!Object.keys(result).length) throw new JsonToolError('noJavaFields')
  return JSON.stringify(result, null, 2)
}

export function jsonToJavaBean(input: string, rootClassName = 'Root'): string {
  const value = parseJson(input)
  if (!isJsonObject(value)) throw new JsonToolError('objectRequired')
  return buildJavaClass(toPascalCase(rootClassName), value, 0, true)
}

export function minifyJson(input: string): string {
  return JSON.stringify(parseJson(input))
}

export function validateJson(input: string): JsonValidation {
  if (!input.trim()) {
    return { kind: 'idle' }
  }
  try {
    const value = parseJson(input)
    const rootType = jsonType(value)
    return { kind: 'valid', rootType }
  } catch (cause) {
    return { kind: 'error', cause }
  }
}

export function queryJsonPath(input: string, path: string): string | undefined {
  if (!path.trim()) {
    throw new JsonToolError('pathEmpty')
  }
  const result = JSONPath({
    path: path.trim(),
    json: parseJson(input) as object,
    wrap: false
  })
  if (result === undefined) return 'undefined'
  return typeof result === 'string'
    ? JSON.stringify(result)
    : JSON.stringify(result, null, 2)
}

export function escapeJsonString(input: string): string {
  return JSON.stringify(input)
}

export function unescapeJsonString(input: string): string {
  const value = parseJson(input)
  if (typeof value !== 'string') {
    throw new JsonToolError('notString')
  }
  return value
}

export function escapeJavaString(input: string): string {
  return input
    .replaceAll('\\', '\\\\')
    .replaceAll('\b', '\\b')
    .replaceAll('\f', '\\f')
    .replaceAll('\n', '\\n')
    .replaceAll('\r', '\\r')
    .replaceAll('\t', '\\t')
    .replaceAll('"', '\\"')
}

export function unescapeJsonText(input: string): string {
  const quoted = input.replace(/\\[\s\S]|"/g, (token) => token === '"' ? '\\"' : token)
  return JSON.parse(`"${quoted}"`) as string
}

export function swapJsonKeysAndValues(input: string): string {
  const value = parseJson(input)
  if (!isJsonObject(value)) throw new JsonToolError('objectRequired')
  return JSON.stringify(swapObject(value), null, 2)
}

export function jsonToXml(input: string): string {
  const value = parseJson(input)
  const builder = new XMLBuilder({
    format: true,
    ignoreAttributes: false,
    suppressEmptyNode: true
  })
  return builder.build({ root: value })
}

export function xmlToJson(input: string): string {
  if (!input.trim()) throw new JsonToolError('emptyXml')
  const parser = new XMLParser({
    ignoreAttributes: false,
    parseTagValue: true,
    parseAttributeValue: true,
    trimValues: true
  })
  return JSON.stringify(parser.parse(input), null, 2)
}

export function analyzeJson(input: string): JsonAnalysis | undefined {
  if (!input.trim()) return undefined
  let root: unknown
  try {
    root = JSON.parse(input)
  } catch {
    return undefined
  }

  let nodes = 0
  let keys = 0
  let maxDepth = 0
  const visit = (value: unknown, depth: number) => {
    nodes += 1
    maxDepth = Math.max(maxDepth, depth)
    if (Array.isArray(value)) {
      value.forEach((item) => visit(item, depth + 1))
      return
    }
    if (isJsonObject(value)) {
      const entries = Object.entries(value)
      keys += entries.length
      entries.forEach(([, item]) => visit(item, depth + 1))
    }
  }
  visit(root, 0)

  return {
    rootType: jsonType(root),
    nodes,
    keys,
    maxDepth,
    bytes: new TextEncoder().encode(input).byteLength
  }
}


function parseJson(input: string): unknown {
  if (!input.trim()) {
    throw new JsonToolError('contentEmpty')
  }
  return JSON.parse(input)
}

function sortJsonValue(value: unknown, ignoreCase = false): unknown {
  if (Array.isArray(value)) {
    return value.map((item) => sortJsonValue(item, ignoreCase))
  }
  if (!isJsonObject(value)) {
    return value
  }
  return Object.fromEntries(
    Object.keys(value)
      .sort((left, right) => left.localeCompare(right, undefined, ignoreCase ? { sensitivity: 'base' } : undefined))
      .map((key) => [key, sortJsonValue(value[key], ignoreCase)])
  )
}

function collectJsonPaths(value: unknown, path: string, label: string, depth: number, entries: JsonPathEntry[]): void {
  if (entries.length >= 5000) return
  entries.push({ path, label, value, depth })
  if (Array.isArray(value)) {
    value.forEach((item, index) => collectJsonPaths(item, `${path}[${index}]`, `[${index}]`, depth + 1, entries))
  } else if (isJsonObject(value)) {
    Object.entries(value).forEach(([key, item]) => collectJsonPaths(item, /^[a-zA-Z_$][\w$]*$/.test(key) ? `${path}.${key}` : `${path}[${JSON.stringify(key)}]`, key, depth + 1, entries))
  }
}

function mockJavaValue(type: string): unknown {
  const normalized = type.replaceAll(' ', '')
  if (normalized.endsWith('[]') || /^(List|Set|Collection|Iterable)</.test(normalized)) return []
  if (/^(Map|HashMap|LinkedHashMap)</.test(normalized)) return {}
  if (/^(boolean|Boolean)$/.test(normalized)) return false
  if (/^(byte|short|int|long|float|double|Byte|Short|Integer|Long|Float|Double|BigDecimal|BigInteger)$/.test(normalized)) return 0
  if (/^(char|Character|String|CharSequence)$/.test(normalized)) return ''
  return null
}

function buildJavaClass(className: string, value: Record<string, unknown>, depth: number, root: boolean): string {
  const fields: string[] = []
  const childClasses: Array<{ name: string; value: Record<string, unknown> }> = []
  const indent = '    '.repeat(depth)
  const bodyIndent = '    '.repeat(depth + 1)
  for (const [key, item] of Object.entries(value)) {
    fields.push(`${bodyIndent}private ${inferJavaType(key, item, childClasses)} ${toJavaIdentifier(key)};`)
  }
  const declaration = root ? `public class ${className}` : `public static class ${className}`
  const children = childClasses.map((child) => buildJavaClass(child.name, child.value, depth + 1, false))
  return `${indent}${declaration} {\n${[...fields, ...children].join('\n\n')}\n${indent}}`
}

function inferJavaType(key: string, value: unknown, children: Array<{ name: string; value: Record<string, unknown> }>): string {
  if (value === null) return 'Object'
  if (typeof value === 'string') return 'String'
  if (typeof value === 'boolean') return 'Boolean'
  if (typeof value === 'number') return Number.isInteger(value) ? 'Long' : 'Double'
  if (Array.isArray(value)) {
    const first = value.find((item) => item !== null)
    if (first === undefined) return 'List<Object>'
    if (isJsonObject(first)) {
      const name = toPascalCase(singularize(key))
      children.push({ name, value: first })
      return `List<${name}>`
    }
    return `List<${inferJavaType(key, first, children)}>`
  }
  if (isJsonObject(value)) {
    const name = toPascalCase(key)
    children.push({ name, value })
    return name
  }
  return 'Object'
}

function toPascalCase(value: string): string {
  const normalized = value.replace(/[^a-zA-Z0-9]+(.)/g, (_, letter: string) => letter.toUpperCase())
  const result = normalized.charAt(0).toUpperCase() + normalized.slice(1)
  return /^\d/.test(result) ? `Type${result}` : result || 'Root'
}

function toJavaIdentifier(value: string): string {
  const normalized = value.replace(/[^a-zA-Z0-9_$]/g, '_') || 'value'
  return /^\d/.test(normalized) ? `_${normalized}` : normalized
}

function singularize(value: string): string {
  return value.endsWith('ies') ? `${value.slice(0, -3)}y` : value.endsWith('s') ? value.slice(0, -1) : value
}

class DuplicateKeyParser {
  private index = 0
  private readonly duplicates: string[] = []
  constructor(private readonly source: string, private readonly ignoreCase: boolean) {}
  parse(): string[] { this.parseValue('$'); return this.duplicates }
  private parseValue(path: string): void {
    this.skipWhitespace()
    const token = this.source[this.index]
    if (token === '{') this.parseObject(path)
    else if (token === '[') this.parseArray(path)
    else if (token === '"') this.parseString()
    else this.parsePrimitive()
  }
  private parseObject(path: string): void {
    this.index += 1; this.skipWhitespace()
    const keys = new Set<string>()
    if (this.source[this.index] === '}') { this.index += 1; return }
    while (this.index < this.source.length) {
      this.skipWhitespace()
      const key = this.parseString()
      const normalized = this.ignoreCase ? key.toLocaleLowerCase() : key
      const keyPath = /^[a-zA-Z_$][\w$]*$/.test(key) ? `${path}.${key}` : `${path}[${JSON.stringify(key)}]`
      if (keys.has(normalized)) this.duplicates.push(keyPath)
      keys.add(normalized)
      this.skipWhitespace(); this.index += 1; this.parseValue(keyPath); this.skipWhitespace()
      if (this.source[this.index++] === '}') return
    }
  }
  private parseArray(path: string): void {
    this.index += 1; this.skipWhitespace()
    if (this.source[this.index] === ']') { this.index += 1; return }
    let itemIndex = 0
    while (this.index < this.source.length) {
      this.parseValue(`${path}[${itemIndex++}]`); this.skipWhitespace()
      if (this.source[this.index++] === ']') return
    }
  }
  private parseString(): string {
    const start = this.index++
    let escaped = false
    while (this.index < this.source.length) {
      const character = this.source[this.index++]
      if (escaped) escaped = false
      else if (character === '\\') escaped = true
      else if (character === '"') break
    }
    return JSON.parse(this.source.slice(start, this.index)) as string
  }
  private parsePrimitive(): void { while (this.index < this.source.length && !/[\s,}\]]/.test(this.source[this.index])) this.index += 1 }
  private skipWhitespace(): void { while (/\s/.test(this.source[this.index] ?? '')) this.index += 1 }
}

function isJsonObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function swapObject(value: Record<string, unknown>): Record<string, unknown> {
  const result: Record<string, unknown> = {}
  for (const [key, item] of Object.entries(value)) {
    if (isJsonObject(item)) {
      result[key] = swapObject(item)
      continue
    }
    result[Array.isArray(item) ? JSON.stringify(item) : String(item)] = key
  }
  return result
}

function jsonType(value: unknown): string {
  if (Array.isArray(value)) return 'Array'
  if (typeof value === 'object' && value !== null) return 'Object'
  return typeof value
}
