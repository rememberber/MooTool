import { parse, parseDocument, stringify } from 'yaml'

const maxDepth = 64

export type ConfigToolErrorCode = 'emptyKey' | 'rootObject' | 'invalidPath' | 'unsafeKey' | 'arrayPath' | 'maxDepth'

export class ConfigToolError extends Error {
  constructor(readonly code: ConfigToolErrorCode, readonly values?: Record<string, string | number>) {
    super(code)
    this.name = 'ConfigToolError'
  }
}

export function propertiesToYaml(source: string): string {
  const root: Record<string, unknown> = Object.create(null) as Record<string, unknown>
  for (const rawLine of logicalPropertyLines(source)) {
    const line = rawLine.trimStart()
    if (!line || line.startsWith('#') || line.startsWith('!')) continue
    const separator = findSeparator(rawLine)
    const rawKey = separator.index < 0 ? rawLine : rawLine.slice(0, separator.index)
    const rawValue = separator.index < 0
      ? ''
      : rawLine.slice(separator.index + separator.width).trimStart()
    const key = rawKey.trim()
    if (!key) throw new ConfigToolError('emptyKey')
    assignPath(root, tokenizePath(key), decodeProperty(rawValue))
  }
  return stringify(root, { indent: 2, lineWidth: 0 })
}

export function yamlToProperties(source: string): string {
  const value: unknown = parse(source)
  if (value == null || typeof value !== 'object' || Array.isArray(value)) {
    throw new ConfigToolError('rootObject')
  }
  const lines: string[] = []
  flattenYaml(value, '', lines, 0)
  return lines.join('\n')
}

export function formatYaml(source: string): string {
  const document = parseDocument(source, { prettyErrors: true })
  if (document.errors.length) throw document.errors[0]
  return document.toString({ indent: 2, lineWidth: 0 })
}

export function validateYaml(source: string): { valid: boolean; message: string } {
  const document = parseDocument(source, { prettyErrors: true })
  return document.errors.length
    ? { valid: false, message: document.errors.map((error) => error.message).join('\n') }
    : { valid: true, message: '' }
}

function logicalPropertyLines(source: string): string[] {
  const output: string[] = []
  let current = ''
  for (const line of source.split(/\r?\n/)) {
    current += line
    if (hasContinuation(current)) {
      current = current.slice(0, -1)
      continue
    }
    output.push(current)
    current = ''
  }
  if (current) output.push(current)
  return output
}

function hasContinuation(line: string): boolean {
  let slashes = 0
  for (let index = line.length - 1; index >= 0 && line[index] === '\\'; index -= 1) slashes += 1
  return slashes % 2 === 1
}

function findSeparator(line: string): { index: number; width: number } {
  let escaped = false
  for (let index = 0; index < line.length; index += 1) {
    const character = line[index]
    if (!escaped && (character === '=' || character === ':')) return { index, width: 1 }
    if (!escaped && /\s/.test(character)) {
      let cursor = index
      while (cursor < line.length && /\s/.test(line[cursor])) cursor += 1
      if (line[cursor] === '=' || line[cursor] === ':') cursor += 1
      return { index, width: cursor - index }
    }
    if (character === '\\' && !escaped) escaped = true
    else escaped = false
  }
  return { index: -1, width: 0 }
}

function tokenizePath(key: string): Array<string | number> {
  const tokens: Array<string | number> = []
  for (const part of splitUnescapedDots(key)) {
    const expression = /([^\[\]]+)|\[(\d+)\]/g
    let match: RegExpExecArray | null
    while ((match = expression.exec(part))) {
      tokens.push(match[2] === undefined ? decodeProperty(match[1]) : Number(match[2]))
    }
  }
  if (!tokens.length || tokens.length > maxDepth) throw new ConfigToolError('invalidPath')
  return tokens
}

function splitUnescapedDots(key: string): string[] {
  const parts: string[] = []
  let current = ''
  let escaped = false
  for (const character of key) {
    if (character === '.' && !escaped) {
      parts.push(current)
      current = ''
    } else {
      current += character
    }
    if (character === '\\' && !escaped) escaped = true
    else escaped = false
  }
  parts.push(current)
  return parts
}

function assignPath(
  root: Record<string, unknown>,
  tokens: Array<string | number>,
  value: string
): void {
  let current: Record<string, unknown> | unknown[] = root
  tokens.forEach((token, index) => {
    const last = index === tokens.length - 1
    if (typeof token === 'string' && ['__proto__', 'prototype', 'constructor'].includes(token)) {
      throw new ConfigToolError('unsafeKey', { key: token })
    }
    if (last) {
      if (Array.isArray(current) && typeof token === 'number') current[token] = value
      else if (!Array.isArray(current) && typeof token === 'string') current[token] = value
      else throw new ConfigToolError('arrayPath')
      return
    }
    const nextIsArray = typeof tokens[index + 1] === 'number'
    if (Array.isArray(current) && typeof token === 'number') {
      current[token] ??= nextIsArray ? [] : Object.create(null)
      current = current[token] as Record<string, unknown> | unknown[]
    } else if (!Array.isArray(current) && typeof token === 'string') {
      current[token] ??= nextIsArray ? [] : Object.create(null)
      current = current[token] as Record<string, unknown> | unknown[]
    } else {
      throw new ConfigToolError('arrayPath')
    }
  })
}

function flattenYaml(value: unknown, prefix: string, lines: string[], depth: number): void {
  if (depth > maxDepth) throw new ConfigToolError('maxDepth')
  if (Array.isArray(value)) {
    value.forEach((item, index) => flattenYaml(item, `${prefix}[${index}]`, lines, depth + 1))
  } else if (value && typeof value === 'object') {
    for (const [key, item] of Object.entries(value)) {
      const escapedKey = escapePropertyKey(key)
      flattenYaml(item, prefix ? `${prefix}.${escapedKey}` : escapedKey, lines, depth + 1)
    }
  } else {
    lines.push(`${prefix}=${encodePropertyValue(value)}`)
  }
}

function escapePropertyKey(value: string): string {
  return value
    .replace(/\\/g, '\\\\')
    .replace(/\./g, '\\.')
    .replace(/([:= ])/g, '\\$1')
}

function encodePropertyValue(value: unknown): string {
  return String(value ?? '')
    .replace(/\\/g, '\\\\')
    .replace(/\n/g, '\\n')
    .replace(/\r/g, '\\r')
    .replace(/\t/g, '\\t')
}

function decodeProperty(value: string): string {
  return value.replace(/\\u([\da-fA-F]{4})|\\(.)/g, (_match, hex: string | undefined, escaped: string | undefined) => {
    if (hex) return String.fromCharCode(Number.parseInt(hex, 16))
    return { n: '\n', r: '\r', t: '\t', f: '\f' }[escaped ?? ''] ?? (escaped ?? '')
  })
}
