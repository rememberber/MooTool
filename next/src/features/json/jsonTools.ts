import type { MessageKey } from '@/shared/i18n/messages'
import { XMLBuilder, XMLParser } from 'fast-xml-parser'
import { JSONPath } from 'jsonpath-plus'

export type JsonStatus =
  | { kind: 'idle'; message: string }
  | { kind: 'valid'; message: string }
  | { kind: 'error'; message: string }

type Translate = (key: MessageKey, params?: Record<string, string>) => string

export type JsonFormatOptions = {
  spaces: number
  sortKeys: boolean
  ignoreCase: boolean
  checkDuplicateKeys: boolean
}

export type JsonPathEntry = {
  path: string
  label: string
  value: unknown
  depth: number
}

export function formatJson(input: string, t: Translate, spaces = 2): string {
  return JSON.stringify(parseJson(input, t), null, spaces)
}

export function compressJson(input: string, t: Translate): string {
  return JSON.stringify(parseJson(input, t))
}

export function formatJsonAdvanced(input: string, t: Translate, options: JsonFormatOptions): string {
  if (options.checkDuplicateKeys) {
    const duplicates = findDuplicateJsonKeys(input, options.ignoreCase)
    if (duplicates.length > 0) {
      throw new Error(t('json.error.duplicateKeys', { paths: duplicates.join(', ') }))
    }
  }
  const parsed = parseJson(input, t)
  const value = options.sortKeys ? sortJsonKeys(parsed, options.ignoreCase) : parsed
  return JSON.stringify(value, null, options.spaces)
}

export function validateJson(input: string, t: Translate): JsonStatus {
  if (!input.trim()) {
    return { kind: 'idle', message: t('json.valid.idle') }
  }

  try {
    const value = parseJson(input, t)
    const type = Array.isArray(value) ? 'Array' : typeof value === 'object' && value !== null ? 'Object' : typeof value
    return { kind: 'valid', message: t('json.valid.ok', { type }) }
  } catch (error) {
    return { kind: 'error', message: error instanceof Error ? error.message : t('json.valid.error') }
  }
}

export function escapeJsonString(input: string): string {
  return JSON.stringify(input)
}

export function unescapeJsonString(input: string, t: Translate): string {
  const parsed = parseJson(input, t)
  if (typeof parsed !== 'string') {
    throw new Error(t('json.error.notString'))
  }
  return parsed
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
  return JSON.parse(`"${input.replaceAll('"', '\\"')}"`) as string
}

export function jsonToXml(input: string, t: Translate): string {
  const value = parseJson(input, t)
  const builder = new XMLBuilder({
    format: true,
    ignoreAttributes: false,
    suppressEmptyNode: true
  })
  return builder.build({ root: value })
}

export function xmlToJson(input: string, t: Translate): string {
  if (!input.trim()) {
    throw new Error(t('json.error.emptyXml'))
  }
  const parser = new XMLParser({
    ignoreAttributes: false,
    parseTagValue: true,
    parseAttributeValue: true,
    trimValues: true
  })
  return JSON.stringify(parser.parse(input), null, 2)
}

export function queryJsonPath(input: string, path: string, t: Translate): string {
  if (!path.trim()) {
    throw new Error(t('json.error.emptyPath'))
  }
  const value = JSONPath({
    path: path.trim(),
    json: parseJson(input, t) as string | number | boolean | object | null,
    wrap: false
  })
  return formatJsonPathValue(value)
}

export function swapJsonKeysAndValues(input: string, t: Translate): string {
  const value = parseJson(input, t)
  if (!isJsonObject(value)) {
    throw new Error(t('json.error.objectRequired'))
  }
  return JSON.stringify(swapObject(value), null, 2)
}

export function javaBeanToJson(input: string, t: Translate): string {
  if (!input.trim()) {
    throw new Error(t('json.error.emptyJavaBean'))
  }

  const result: Record<string, unknown> = {}
  const fieldPattern = /^(?:(?:public|protected|private)\s+)?(?:(?:static|final|transient|volatile)\s+)*([\w$.<>?, \[\]]+?)\s+(\w+)\s*(?:=.*)?$/
  for (const statement of input.split(';')) {
    const boundary = Math.max(statement.lastIndexOf('{'), statement.lastIndexOf('}'))
    const match = fieldPattern.exec(statement.slice(boundary + 1).trim())
    if (!match) continue
    const type = match[1].trim()
    const name = match[2]
    if (name === 'serialVersionUID') continue
    result[name] = mockJavaValue(type)
  }

  if (Object.keys(result).length === 0) {
    throw new Error(t('json.error.noJavaFields'))
  }
  return JSON.stringify(result, null, 2)
}

export function jsonToJavaBean(input: string, t: Translate, rootClassName = 'Root'): string {
  const value = parseJson(input, t)
  if (!isJsonObject(value)) {
    throw new Error(t('json.error.objectRequired'))
  }

  return buildJavaClass(toPascalCase(rootClassName), value, 0, true)
}

export function findDuplicateJsonKeys(input: string, ignoreCase = false): string[] {
  JSON.parse(input)
  return new DuplicateKeyParser(input, ignoreCase).parse()
}

export function listJsonPaths(input: string, t: Translate): JsonPathEntry[] {
  const entries: JsonPathEntry[] = []
  collectJsonPaths(parseJson(input, t), '$', '$', 0, entries)
  return entries
}

function parseJson(input: string, t: Translate): unknown {
  if (!input.trim()) {
    throw new Error(t('json.error.empty'))
  }

  return JSON.parse(input)
}

function sortJsonKeys(value: unknown, ignoreCase: boolean): unknown {
  if (Array.isArray(value)) {
    return value.map((item) => sortJsonKeys(item, ignoreCase))
  }
  if (!isJsonObject(value)) {
    return value
  }

  const compare = ignoreCase
    ? (left: string, right: string) => left.localeCompare(right, undefined, { sensitivity: 'base' })
    : (left: string, right: string) => left.localeCompare(right)
  return Object.fromEntries(
    Object.keys(value).sort(compare).map((key) => [key, sortJsonKeys(value[key], ignoreCase)])
  )
}

function collectJsonPaths(value: unknown, path: string, label: string, depth: number, entries: JsonPathEntry[]): void {
  entries.push({ path, label, value, depth })
  if (Array.isArray(value)) {
    value.forEach((item, index) => collectJsonPaths(item, `${path}[${index}]`, `[${index}]`, depth + 1, entries))
    return
  }
  if (isJsonObject(value)) {
    for (const [key, item] of Object.entries(value)) {
      const childPath = /^[a-zA-Z_$][\w$]*$/.test(key) ? `${path}.${key}` : `${path}[${JSON.stringify(key)}]`
      collectJsonPaths(item, childPath, key, depth + 1, entries)
    }
  }
}

function swapObject(value: Record<string, unknown>): Record<string, unknown> {
  const result: Record<string, unknown> = {}
  for (const [key, item] of Object.entries(value)) {
    if (isJsonObject(item)) {
      result[key] = swapObject(item)
      continue
    }
    const swappedKey = Array.isArray(item) ? JSON.stringify(item) : String(item)
    result[swappedKey] = key
  }
  return result
}

function formatJsonPathValue(value: unknown): string {
  if (typeof value === 'string') return JSON.stringify(value)
  if (value === undefined) return 'undefined'
  return JSON.stringify(value, null, 2)
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
    const fieldName = toJavaIdentifier(key)
    const type = inferJavaType(key, item, childClasses)
    fields.push(`${bodyIndent}private ${type} ${fieldName};`)
  }

  const declaration = root ? `public class ${className}` : `public static class ${className}`
  const children = childClasses.map((child) => buildJavaClass(child.name, child.value, depth + 1, false))
  const members = [...fields, ...children]
  return `${indent}${declaration} {\n${members.join('\n\n')}\n${indent}}`
}

function inferJavaType(key: string, value: unknown, childClasses: Array<{ name: string; value: Record<string, unknown> }>): string {
  if (value === null) return 'Object'
  if (typeof value === 'string') return 'String'
  if (typeof value === 'boolean') return 'Boolean'
  if (typeof value === 'number') return Number.isInteger(value) ? 'Long' : 'Double'
  if (Array.isArray(value)) {
    const first = value.find((item) => item !== null)
    if (first === undefined) return 'List<Object>'
    if (isJsonObject(first)) {
      const name = toPascalCase(singularize(key))
      childClasses.push({ name, value: first })
      return `List<${name}>`
    }
    return `List<${inferJavaType(key, first, childClasses)}>`
  }
  if (isJsonObject(value)) {
    const name = toPascalCase(key)
    childClasses.push({ name, value })
    return name
  }
  return 'Object'
}

function isJsonObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
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

  parse(): string[] {
    this.parseValue('$')
    return this.duplicates
  }

  private parseValue(path: string): void {
    this.skipWhitespace()
    const token = this.source[this.index]
    if (token === '{') this.parseObject(path)
    else if (token === '[') this.parseArray(path)
    else if (token === '"') this.parseString()
    else this.parsePrimitive()
  }

  private parseObject(path: string): void {
    this.index++
    this.skipWhitespace()
    const keys = new Set<string>()
    if (this.source[this.index] === '}') {
      this.index++
      return
    }

    while (this.index < this.source.length) {
      this.skipWhitespace()
      const key = this.parseString()
      const normalized = this.ignoreCase ? key.toLocaleLowerCase() : key
      const keyPath = /^[a-zA-Z_$][\w$]*$/.test(key) ? `${path}.${key}` : `${path}[${JSON.stringify(key)}]`
      if (keys.has(normalized)) this.duplicates.push(keyPath)
      keys.add(normalized)
      this.skipWhitespace()
      this.index++
      this.parseValue(keyPath)
      this.skipWhitespace()
      const next = this.source[this.index++]
      if (next === '}') return
    }
  }

  private parseArray(path: string): void {
    this.index++
    this.skipWhitespace()
    if (this.source[this.index] === ']') {
      this.index++
      return
    }
    let itemIndex = 0
    while (this.index < this.source.length) {
      this.parseValue(`${path}[${itemIndex++}]`)
      this.skipWhitespace()
      const next = this.source[this.index++]
      if (next === ']') return
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

  private parsePrimitive(): void {
    while (this.index < this.source.length && !/[\s,}\]]/.test(this.source[this.index])) this.index++
  }

  private skipWhitespace(): void {
    while (/\s/.test(this.source[this.index] ?? '')) this.index++
  }
}
