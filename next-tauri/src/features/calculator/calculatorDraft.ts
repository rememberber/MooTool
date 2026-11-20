export const calculatorDraftKey = 'mootool.calculator.draft'

export interface CalculatorDraft {
  expression: string
  result: string
  decimal: string
  hex: string
  binary: string
  floatHex: string
  floatDecimal: string
  floatBits: '32' | '64'
  floatByteOrder: 'big' | 'little'
  gcdFirst: string
  gcdSecond: string
  lcmFirst: string
  lcmSecond: string
  permutationN: string
  permutationM: string
  combinationN: string
  combinationM: string
  log: string[]
}

export const defaultCalculatorDraft: CalculatorDraft = {
  expression: '2 * (3 + 4)',
  result: '14',
  decimal: '255',
  hex: 'ff',
  binary: '11111111',
  floatHex: '3F800000',
  floatDecimal: '1.0',
  floatBits: '32',
  floatByteOrder: 'big',
  gcdFirst: '54',
  gcdSecond: '24',
  lcmFirst: '54',
  lcmSecond: '24',
  permutationN: '5',
  permutationM: '2',
  combinationN: '5',
  combinationM: '2',
  log: ['2 * (3 + 4) = 14']
}

export function normalizeCalculatorDraft(value: unknown): CalculatorDraft {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return defaultCalculatorDraft
  const result = { ...defaultCalculatorDraft }
  const saved = value as Partial<Record<keyof CalculatorDraft, unknown>>
  for (const key of Object.keys(defaultCalculatorDraft) as (keyof CalculatorDraft)[]) {
    const fallback = defaultCalculatorDraft[key]
    const candidate = saved[key]
    if (Array.isArray(fallback)) {
      if (Array.isArray(candidate) && candidate.every((item) => typeof item === 'string')) result.log = candidate
    } else if (typeof candidate === typeof fallback) {
      Object.assign(result, { [key]: candidate })
    }
  }
  if (result.floatBits !== '64') result.floatBits = '32'
  if (result.floatByteOrder !== 'little') result.floatByteOrder = 'big'
  return result
}

export function readCalculatorDraft(): CalculatorDraft {
  try {
    return normalizeCalculatorDraft(JSON.parse(window.localStorage.getItem(calculatorDraftKey) ?? 'null'))
  } catch {
    return defaultCalculatorDraft
  }
}
