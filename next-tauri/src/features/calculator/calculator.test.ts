import { describe, expect, it } from 'vitest'
import {
  combination,
  convertBase,
  evaluateExpression,
  greatestCommonDivisor,
  leastCommonMultiple,
  permutation
} from './calculator'

describe('independent calculator domain', () => {
  it('evaluates arithmetic without executing arbitrary code', () => {
    expect(evaluateExpression('2 * (3 + 4)=')).toBe('14')
    expect(evaluateExpression('-3 + 10 / 2')).toBe('2')
    expect(evaluateExpression('.5 * 8')).toBe('4')
    expect(() => evaluateExpression('globalThis')).toThrow()
    expect(() => evaluateExpression('1 / 0')).toThrow()
    expect(() => evaluateExpression('(1 + 2')).toThrow()
  })

  it('converts integer bases exactly', () => {
    expect(convertBase('255', 10, 16)).toBe('ff')
    expect(convertBase('11111111', 2, 10)).toBe('255')
    expect(convertBase('-ff', 16, 10)).toBe('-255')
  })

  it('performs exact integer operations', () => {
    expect(greatestCommonDivisor('54', '24')).toBe('6')
    expect(leastCommonMultiple('6', '8')).toBe('24')
    expect(permutation('5', '2')).toBe('20')
    expect(combination('5', '2')).toBe('10')
  })
})
