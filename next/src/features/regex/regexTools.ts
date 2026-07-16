export type RegexOptions = { global: boolean; ignoreCase: boolean; multiline: boolean; dotAll: boolean }
export type RegexMatch = { index: number; value: string; groups: string[] }

export const commonRegexes = [
  { id: 'phone', labelKey: 'regex.common.phone', pattern: '1[3-9]\\d{9}' },
  { id: 'email', labelKey: 'regex.common.email', pattern: '^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$' },
  { id: 'domain', labelKey: 'regex.common.domain', pattern: '^((http:\\/\\/)|(https:\\/\\/))?([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}(\\/)' },
  { id: 'ipv4', labelKey: 'regex.common.ipv4', pattern: '((?:(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d))' },
  { id: 'account', labelKey: 'regex.common.account', pattern: '^[a-zA-Z][a-zA-Z0-9_]{4,15}$' },
  { id: 'htmlId', labelKey: 'regex.common.htmlId', pattern: '(?<=id=")[\\s\\S]*?(?=")' },
  { id: 'color', labelKey: 'regex.common.color', pattern: '#([a-fA-F0-9]{6})' },
  { id: 'jpg', labelKey: 'regex.common.jpg', pattern: 'http[s:]{1,2}//[^\\s\'"<>]*?.jpg' },
  { id: 'magnet', labelKey: 'regex.common.magnet', pattern: 'magnet:\\?xt=urn:btih:[0-9a-fA-F]{40,}' },
  { id: 'chinese', labelKey: 'regex.common.chinese', pattern: '^[\\u4e00-\\u9fa5]{0,}$' },
  { id: 'alnum', labelKey: 'regex.common.alnum', pattern: '^[A-Za-z0-9]+$' },
  { id: 'len3to20', labelKey: 'regex.common.len3to20', pattern: '^.{3,20}$' },
  { id: 'letters26', labelKey: 'regex.common.letters26', pattern: '^[A-Za-z]+$' },
  { id: 'wordUnderscore', labelKey: 'regex.common.wordUnderscore', pattern: '^\\w+$' },
  { id: 'cnEnNum', labelKey: 'regex.common.cnEnNum', pattern: '^[\\u4E00-\\u9FA5A-Za-z0-9_]+$' },
  { id: 'noSpecial', labelKey: 'regex.common.noSpecial', pattern: '[^%&\',;=?$\\x22]+' },
  { id: 'integer', labelKey: 'regex.common.integer', pattern: '^-?[1-9]\\d*$' },
  { id: 'positiveInt', labelKey: 'regex.common.positiveInt', pattern: '^[1-9]\\d*$' },
  { id: 'negativeInt', labelKey: 'regex.common.negativeInt', pattern: '^-[1-9]\\d*$' },
  { id: 'nonNegativeInt', labelKey: 'regex.common.nonNegativeInt', pattern: '^(?:[1-9]\\d*|0)$' },
  { id: 'float', labelKey: 'regex.common.float', pattern: '^-?([1-9]\\d*\\.\\d*|0\\.\\d*[1-9]\\d*|0?\\.0+|0)$' }
] as const

export function matchRegex(pattern: string, source: string, options: RegexOptions): RegexMatch[] {
  const flags = `${options.global ? 'g' : ''}${options.ignoreCase ? 'i' : ''}${options.multiline ? 'm' : ''}${options.dotAll ? 's' : ''}`
  const expression = new RegExp(pattern, flags)
  if (!options.global) {
    const match = expression.exec(source)
    return match ? [{ index: match.index, value: match[0], groups: match.slice(1).map((item) => item ?? '') }] : []
  }
  const matches: RegexMatch[] = []
  let match: RegExpExecArray | null
  while ((match = expression.exec(source))) {
    matches.push({ index: match.index, value: match[0], groups: match.slice(1).map((item) => item ?? '') })
    if (match[0] === '') expression.lastIndex += 1
  }
  return matches
}
