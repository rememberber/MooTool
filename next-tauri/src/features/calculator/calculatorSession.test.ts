import { describe, expect, it } from 'vitest'
import { describeCalculatorState } from './calculatorSession'

describe('Calculator session description', () => {
  it('captures user inputs, result and history in the state digest', () => {
    const description = describeCalculatorState({
      expression: '9 * 9',
      result: '81',
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
      log: ['表达式: 9 * 9 = 81', '2 * (3 + 4) = 14'],
      error: null
    }, 'zh-CN')

    expect(JSON.parse(description.stateDigest)).toMatchObject({
      expression: '9 * 9',
      result: '81',
      log: ['表达式: 9 * 9 = 81', '2 * (3 + 4) = 14']
    })
    expect(description.stateSummary).toBe('9 * 9 = 81 · 2 条记录')
  })
})
