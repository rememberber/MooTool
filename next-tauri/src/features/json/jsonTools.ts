import { JSONPath } from 'jsonpath-plus'
export { contentFingerprint } from '../../shared/fingerprint'

export type JsonValidation =
  | { kind: 'idle' }
  | { kind: 'valid'; rootType: string }
  | { kind: 'error'; cause: unknown }

export type JsonToolErrorCode = 'pathEmpty' | 'notString' | 'contentEmpty' | 'parseDetail' | 'parsePosition' | 'swapValue' | 'xmlParse' | 'duplicateKeys' | 'objectRequired' | 'emptyJavaBean' | 'noJavaFields'

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
  if (result === undefined) {
    return undefined
  }
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

export function swapJsonKeysAndValues(input: string, indent = 2): string {
  const value = parseJson(input)
  if (!isJsonObject(value) || Object.values(value).some((item) => !['string', 'number', 'boolean'].includes(typeof item))) {
    throw new JsonToolError('swapValue')
  }
  const swapped: Record<string, string> = {}
  for (const [key, item] of Object.entries(value)) {
    const swappedKey = String(item)
    if (Object.hasOwn(swapped, swappedKey)) throw new JsonToolError('swapValue')
    swapped[swappedKey] = key
  }
  return JSON.stringify(swapped, null, indent)
}

export function jsonToXml(input: string): string {
  const value = parseJson(input)
  return `<?xml version="1.0" encoding="UTF-8"?>\n${xmlNode('root', value, 0)}`
}

export function xmlToJson(input: string, indent = 2): string {
  const parser = new DOMParser()
  const document = parser.parseFromString(input, 'application/xml')
  if (document.querySelector('parsererror') || !document.documentElement) throw new JsonToolError('xmlParse')
  return JSON.stringify({ [document.documentElement.tagName]: xmlElementValue(document.documentElement) }, null, indent)
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
  try {
    return JSON.parse(input)
  } catch (cause) {
    if (!(cause instanceof SyntaxError)) throw cause
    const position = readErrorPosition(cause.message)
    if (position === undefined) {
      throw new JsonToolError('parseDetail', { detail: cause.message })
    }
    const prefix = input.slice(0, position)
    const line = prefix.split('\n').length
    const column = position - prefix.lastIndexOf('\n')
    throw new JsonToolError('parsePosition', { line, column })
  }
}

function readErrorPosition(message: string): number | undefined {
  const match = /position\s+(\d+)/i.exec(message)
  return match ? Number(match[1]) : undefined
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
    if (first === undefined) return 'java.util.List<Object>'
    if (isJsonObject(first)) {
      const name = toPascalCase(singularize(key))
      children.push({ name, value: first })
      return `java.util.List<${name}>`
    }
    return `java.util.List<${inferJavaType(key, first, children)}>`
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
  const identifier = /^\d/.test(normalized) ? `_${normalized}` : normalized
  return JAVA_RESERVED_WORDS.has(identifier) ? `${identifier}_` : identifier
}

function singularize(value: string): string {
  return value.endsWith('ies') ? `${value.slice(0, -3)}y` : value.endsWith('s') ? value.slice(0, -1) : value
}

const JAVA_RESERVED_WORDS = new Set([
  'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class',
  'const', 'continue', 'default', 'do', 'double', 'else', 'enum', 'extends', 'final',
  'finally', 'float', 'for', 'goto', 'if', 'implements', 'import', 'instanceof', 'int',
  'interface', 'long', 'native', 'new', 'package', 'private', 'protected', 'public',
  'return', 'short', 'static', 'strictfp', 'super', 'switch', 'synchronized', 'this',
  'throw', 'throws', 'transient', 'try', 'void', 'volatile', 'while'
])

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

function xmlNode(name: string, value: unknown, depth: number): string {
  const padding = '  '.repeat(depth)
  const safeName = /^[A-Za-z_][\w.-]*$/.test(name) ? name : 'item'
  if (Array.isArray(value)) return value.map((item) => xmlNode(safeName, item, depth)).join('\n')
  if (isJsonObject(value)) {
    const children = Object.entries(value).map(([key, item]) => xmlNode(key, item, depth + 1)).join('\n')
    return `${padding}<${safeName}>${children ? `\n${children}\n${padding}` : ''}</${safeName}>`
  }
  if (value === null) return `${padding}<${safeName} />`
  return `${padding}<${safeName}>${escapeXml(String(value))}</${safeName}>`
}

function xmlElementValue(element: Element): unknown {
  const children = [...element.children]
  if (!children.length) return element.textContent ?? ''
  const output: Record<string, unknown> = {}
  for (const child of children) {
    const value = xmlElementValue(child)
    const current = output[child.tagName]
    output[child.tagName] = current === undefined ? value : Array.isArray(current) ? [...current, value] : [current, value]
  }
  return output
}

function escapeXml(value: string): string {
  return value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&apos;')
}

function jsonType(value: unknown): string {
  if (Array.isArray(value)) return 'Array'
  if (value === null) return 'Null'
  if (typeof value === 'object') return 'Object'
  return value === undefined
    ? 'Undefined'
    : `${typeof value}`.replace(/^./, (letter) => letter.toUpperCase())
}
