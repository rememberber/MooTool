import { parse, parseDocument, stringify } from 'yaml'

export function propertiesToYaml(source: string): string {
  const root: Record<string, unknown> = {}
  for (const rawLine of source.split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#') || line.startsWith('!')) continue
    const separator = findSeparator(rawLine)
    const key = (separator < 0 ? rawLine : rawLine.slice(0, separator)).trim()
    const value = (separator < 0 ? '' : rawLine.slice(separator + 1)).trim()
    assignPath(root, tokenizePath(key), decodeProperty(value))
  }
  return stringify(root, { indent: 4, lineWidth: 0 })
}

export function yamlToProperties(source: string): string {
  const value: unknown = parse(source)
  if (value == null || typeof value !== 'object') throw new Error('YAML root must be an object')
  const lines: string[] = []
  flattenYaml(value, '', lines)
  return lines.join('\n')
}

export function formatYaml(source: string): string {
  const document = parseDocument(source, { prettyErrors: true })
  if (document.errors.length) throw document.errors[0]
  return document.toString({ indent: 2, lineWidth: 0 })
}

export function validateYaml(source: string): { valid: boolean; message: string } {
  const document = parseDocument(source, { prettyErrors: true })
  return document.errors.length ? { valid: false, message: document.errors.map((error) => error.message).join('\n') } : { valid: true, message: '' }
}

function findSeparator(line: string): number {
  let escaped = false
  for (let index = 0; index < line.length; index += 1) {
    if (!escaped && (line[index] === '=' || line[index] === ':')) return index
    escaped = !escaped && line[index] === '\\'
    if (line[index] !== '\\') escaped = false
  }
  return -1
}

function tokenizePath(key: string): Array<string | number> {
  return key.split('.').flatMap((part) => {
    const tokens: Array<string | number> = []
    const expression = /([^\[\]]+)|\[(\d+)\]/g
    let match: RegExpExecArray | null
    while ((match = expression.exec(part))) tokens.push(match[2] == null ? match[1] : Number(match[2]))
    return tokens
  })
}

function assignPath(root: Record<string, unknown>, tokens: Array<string | number>, value: string): void {
  let current: Record<string, unknown> | unknown[] = root
  tokens.forEach((token, index) => {
    const last = index === tokens.length - 1
    if (last) {
      if (Array.isArray(current) && typeof token === 'number') current[token] = value
      else if (!Array.isArray(current) && typeof token === 'string') current[token] = value
      return
    }
    const nextIsArray = typeof tokens[index + 1] === 'number'
    if (Array.isArray(current) && typeof token === 'number') {
      current[token] ??= nextIsArray ? [] : {}
      current = current[token] as Record<string, unknown> | unknown[]
    } else if (!Array.isArray(current) && typeof token === 'string') {
      current[token] ??= nextIsArray ? [] : {}
      current = current[token] as Record<string, unknown> | unknown[]
    }
  })
}

function flattenYaml(value: unknown, prefix: string, lines: string[]): void {
  if (Array.isArray(value)) {
    if (value.every((item) => item == null || typeof item !== 'object')) {
      lines.push(`${prefix}=${value.map(propertyValue).join(',')}`)
    } else value.forEach((item, index) => flattenYaml(item, `${prefix}[${index}]`, lines))
  } else if (value && typeof value === 'object') {
    for (const [key, item] of Object.entries(value)) flattenYaml(item, prefix ? `${prefix}.${key}` : key, lines)
  } else {
    lines.push(`${prefix}=${propertyValue(value)}`)
  }
}

function propertyValue(value: unknown): string {
  return String(value ?? ' ').replace(/\\/g, '\\\\').replace(/\n/g, '\\n')
}

function decodeProperty(value: string): string {
  return value.replace(/\\u([\da-fA-F]{4})/g, (_match, hex: string) => String.fromCharCode(Number.parseInt(hex, 16))).replace(/\\n/g, '\n').replace(/\\t/g, '\t').replace(/\\([:= ])/g, '$1')
}
