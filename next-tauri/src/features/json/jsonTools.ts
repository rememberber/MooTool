import { JSONPath } from 'jsonpath-plus'
export { contentFingerprint } from '../../shared/fingerprint'

export type JsonValidation =
  | { kind: 'idle' }
  | { kind: 'valid'; rootType: string }
  | { kind: 'error'; cause: unknown }

export type JsonToolErrorCode = 'pathEmpty' | 'notString' | 'contentEmpty' | 'parseDetail' | 'parsePosition' | 'swapValue' | 'xmlParse'

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
}

export function formatJson(input: string, options: JsonFormatOptions): string {
  return JSON.stringify(
    options.sortKeys ? sortJsonValue(parseJson(input)) : parseJson(input),
    null,
    options.indent
  )
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

function sortJsonValue(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map(sortJsonValue)
  }
  if (!isJsonObject(value)) {
    return value
  }
  return Object.fromEntries(
    Object.keys(value)
      .sort((left, right) => left.localeCompare(right))
      .map((key) => [key, sortJsonValue(value[key])])
  )
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
