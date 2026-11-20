import { describe, expect, it } from 'vitest'
import { defaultCalculatorDraft, normalizeCalculatorDraft } from './calculatorDraft'

describe('calculator draft', () => {
  it('uses the Electron defaults when nothing is stored', () => {
    expect(normalizeCalculatorDraft(null)).toEqual(defaultCalculatorDraft)
    expect(normalizeCalculatorDraft({ expression: 12, floatBits: 64, log: ['ok', 1] })).toEqual(defaultCalculatorDraft)
  })

  it('keeps the Electron fields and an explicit JavaScript-sized float draft', () => {
    expect(normalizeCalculatorDraft({
      expression: '7*8',
      hex: 'abcd',
      floatBits: '64',
      floatByteOrder: 'little',
      gcdFirst: '8',
      log: ['表达式: 7*8 = 56']
    })).toMatchObject({
      expression: '7*8',
      hex: 'abcd',
      decimal: '255',
      floatBits: '64',
      floatByteOrder: 'little',
      gcdFirst: '8',
      gcdSecond: '24',
      log: ['表达式: 7*8 = 56']
    })
  })
})
