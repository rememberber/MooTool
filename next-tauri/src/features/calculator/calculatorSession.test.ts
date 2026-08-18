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
      gcdValues: ['54', '24'],
      lcmValues: ['54', '24'],
      permutationValues: ['5', '2'],
      combinationValues: ['5', '2'],
      history: [
        { operation: 'expression', input: '9 * 9', output: '81' },
        { operation: 'expression', input: '2 * (3 + 4)', output: '14' }
      ],
      error: null
    }, 'zh-CN')

    expect(JSON.parse(description.stateDigest)).toMatchObject({
      expression: '9 * 9',
      result: '81',
      history: [
        { operation: 'expression', input: '9 * 9', output: '81' },
        { operation: 'expression', input: '2 * (3 + 4)', output: '14' }
      ]
    })
    expect(description.stateSummary).toBe('9 * 9 = 81 · 2 条记录')
  })
})
