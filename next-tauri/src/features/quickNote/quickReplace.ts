export const quickReplaceActionIds = [
  'trim',
  'removeBlankLines',
  'removeTabs',
  'scientificToNormal',
  'normalToScientific',
  'thousandsToNormal',
  'normalToThousands',
  'underscoreToCamel',
  'camelToUnderscore',
  'uppercase',
  'lowercase',
  'linesToComma',
  'linesToSingleQuoted',
  'linesToDoubleQuoted',
  'commaToLines',
  'tabsToLines',
  'clearNewlines',
  'deduplicateLines',
  'deduplicateWithCount',
  'escape',
  'unescape',
  'reverseLines',
  'sortAscending',
  'sortDescending'
] as const

export type QuickReplaceActionId = (typeof quickReplaceActionIds)[number]

export function runQuickReplace(input: string, action: QuickReplaceActionId): string {
  const lines = normalizeLines(input)
  switch (action) {
    case 'trim':
      return lines.map((line) => line.trim()).join('\n')
    case 'removeBlankLines':
      return lines.filter((line) => line.trim()).join('\n')
    case 'removeTabs':
      return input.replaceAll('\t', '')
    case 'scientificToNormal':
      return replaceNumbers(input, (value) => scientificToNormal(value))
    case 'normalToScientific':
      return replaceNumbers(input, (value) => Number(value.replaceAll(',', '')).toExponential())
    case 'thousandsToNormal':
      return input.replace(/(?<=\d),(?=\d{3}(?:\D|$))/g, '')
    case 'normalToThousands':
      return replaceNumbers(input, addThousandsSeparators)
    case 'underscoreToCamel':
      return input.replace(/_([a-zA-Z0-9])/g, (_match, value: string) => value.toUpperCase())
    case 'camelToUnderscore':
      return input.replace(/([a-z0-9])([A-Z])/g, '$1_$2').toLocaleLowerCase()
    case 'uppercase':
      return input.toLocaleUpperCase()
    case 'lowercase':
      return input.toLocaleLowerCase()
    case 'linesToComma':
      return nonEmptyLines(lines).join(',')
    case 'linesToSingleQuoted':
      return nonEmptyLines(lines).map((line) => `'${line.replaceAll("'", "\\'")}'`).join(',')
    case 'linesToDoubleQuoted':
      return nonEmptyLines(lines).map((line) => `"${line.replaceAll('"', '\\"')}"`).join(',')
    case 'commaToLines':
      return input.split(',').map((item) => unquote(item.trim())).filter(Boolean).join('\n')
    case 'tabsToLines':
      return input.split('\t').join('\n')
    case 'clearNewlines':
      return lines.join('')
    case 'deduplicateLines':
      return [...new Set(lines)].join('\n')
    case 'deduplicateWithCount': {
      const counts = new Map<string, number>()
      for (const line of lines) counts.set(line, (counts.get(line) ?? 0) + 1)
      return [...counts].map(([line, count]) => `${line}\t${count}`).join('\n')
    }
    case 'escape':
      return JSON.stringify(input).slice(1, -1)
    case 'unescape':
      return JSON.parse(`"${input.replaceAll('"', '\\"')}"`) as string
    case 'reverseLines':
      return lines.reverse().join('\n')
    case 'sortAscending':
      return lines.sort((left, right) => left.localeCompare(right)).join('\n')
    case 'sortDescending':
      return lines.sort((left, right) => right.localeCompare(left)).join('\n')
  }
}

function normalizeLines(value: string): string[] {
  return value.replaceAll('\r\n', '\n').replaceAll('\r', '\n').split('\n')
}

function nonEmptyLines(lines: string[]): string[] {
  return lines.map((line) => line.trim()).filter(Boolean)
}

function replaceNumbers(input: string, transform: (value: string) => string): string {
  return input.replace(/[-+]?(?:\d[\d,]*\.?\d*|\.\d+)(?:e[-+]?\d+)?/gi, (value) => {
    const parsed = Number(value.replaceAll(',', ''))
    return Number.isFinite(parsed) ? transform(value) : value
  })
}

function scientificToNormal(value: string): string {
  const normalized = value.replaceAll(',', '')
  if (!/[eE]/.test(normalized)) return normalized
  const [coefficient, exponentText] = normalized.toLocaleLowerCase().split('e')
  const exponent = Number(exponentText)
  const negative = coefficient.startsWith('-')
  const unsigned = coefficient.replace(/^[+-]/, '')
  const [integer, fraction = ''] = unsigned.split('.')
  const digits = `${integer}${fraction}`
  const decimalIndex = integer.length + exponent
  const result = decimalIndex <= 0
    ? `0.${'0'.repeat(-decimalIndex)}${digits}`
    : decimalIndex >= digits.length
      ? `${digits}${'0'.repeat(decimalIndex - digits.length)}`
      : `${digits.slice(0, decimalIndex)}.${digits.slice(decimalIndex)}`
  return negative ? `-${result}` : result
}

function addThousandsSeparators(value: string): string {
  const normalized = value.replaceAll(',', '')
  if (/[eE]/.test(normalized)) return normalized
  const [integer, fraction] = normalized.split('.')
  const sign = integer.startsWith('-') || integer.startsWith('+') ? integer[0] : ''
  const digits = sign ? integer.slice(1) : integer
  const grouped = digits.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return `${sign}${grouped}${fraction === undefined ? '' : `.${fraction}`}`
}

function unquote(value: string): string {
  if (value.length >= 2 && ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'")))) {
    return value.slice(1, -1)
  }
  return value
}
