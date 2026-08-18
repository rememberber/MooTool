export interface RegexOptions {
  global: boolean
  ignoreCase: boolean
  multiline: boolean
  dotAll: boolean
  unicode: boolean
}

export interface RegexMatch {
  index: number
  end: number
  value: string
  groups: string[]
  namedGroups: Record<string, string>
}

export const commonRegexes = [
  { id: 'phone', pattern: '1[3-9]\\d{9}' },
  { id: 'email', pattern: '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$' },
  { id: 'url', pattern: "https?:\\/\\/[^\\s<>\"']+" },
  { id: 'domain', pattern: '(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}' },
  { id: 'ipv4', pattern: '(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d)' },
  { id: 'ipv6', pattern: '(?:[\\da-fA-F]{1,4}:){7}[\\da-fA-F]{1,4}' },
  { id: 'account', pattern: '^[a-zA-Z][a-zA-Z0-9_]{4,15}$' },
  { id: 'htmlId', pattern: '(?<=id=")[\\s\\S]*?(?=")' },
  { id: 'color', pattern: '#(?:[a-fA-F0-9]{3}|[a-fA-F0-9]{6}|[a-fA-F0-9]{8})\\b' },
  { id: 'image', pattern: "https?:\\/\\/[^\\s'\"<>]+?\\.(?:png|jpe?g|gif|webp|svg)(?:\\?[^\\s]*)?" },
  { id: 'magnet', pattern: 'magnet:\\?xt=urn:btih:[0-9a-fA-F]{40,}' },
  { id: 'chinese', pattern: '[\\u3400-\\u9fff]+' },
  { id: 'alnum', pattern: '^[A-Za-z0-9]+$' },
  { id: 'len3to20', pattern: '^.{3,20}$' },
  { id: 'letters26', pattern: '^[A-Za-z]+$' },
  { id: 'wordUnderscore', pattern: '^\\w+$' },
  { id: 'cnEnNum', pattern: '^[\\u3400-\\u9fffA-Za-z0-9_]+$' },
  { id: 'integer', pattern: '^-?(?:0|[1-9]\\d*)$' },
  { id: 'positiveInt', pattern: '^[1-9]\\d*$' },
  { id: 'nonNegativeInt', pattern: '^(?:0|[1-9]\\d*)$' },
  { id: 'float', pattern: '^-?(?:0|[1-9]\\d*)\\.\\d+$' }
] as const

const maxMatches = 10_000

export function regexFlags(options: RegexOptions): string {
  return `${options.global ? 'g' : ''}${options.ignoreCase ? 'i' : ''}${options.multiline ? 'm' : ''}${options.dotAll ? 's' : ''}${options.unicode ? 'u' : ''}`
}

export function matchRegex(
  pattern: string,
  source: string,
  options: RegexOptions
): RegexMatch[] {
  const expression = new RegExp(pattern, regexFlags(options))
  if (!options.global) {
    const match = expression.exec(source)
    return match ? [toRegexMatch(match)] : []
  }

  const matches: RegexMatch[] = []
  let match: RegExpExecArray | null
  while ((match = expression.exec(source))) {
    matches.push(toRegexMatch(match))
    if (matches.length >= maxMatches) break
    if (match[0] === '') expression.lastIndex = advanceStringIndex(
      source,
      expression.lastIndex,
      options.unicode
    )
  }
  return matches
}

export function replaceRegex(
  pattern: string,
  source: string,
  replacement: string,
  options: RegexOptions
): string {
  return source.replace(new RegExp(pattern, regexFlags(options)), replacement)
}

function toRegexMatch(match: RegExpExecArray): RegexMatch {
  return {
    index: match.index,
    end: match.index + match[0].length,
    value: match[0],
    groups: match.slice(1).map((item) => item ?? ''),
    namedGroups: Object.fromEntries(
      Object.entries(match.groups ?? {}).map(([name, value]) => [name, value ?? ''])
    )
  }
}

function advanceStringIndex(source: string, index: number, unicode: boolean): number {
  if (!unicode || index >= source.length) return index + 1
  const first = source.charCodeAt(index)
  if (first < 0xd800 || first > 0xdbff || index + 1 >= source.length) return index + 1
  const second = source.charCodeAt(index + 1)
  return second >= 0xdc00 && second <= 0xdfff ? index + 2 : index + 1
}
