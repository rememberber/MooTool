import { describe, expect, it } from 'vitest'
import { combination, convertBase, evaluateExpression, gcd, lcm, permutation } from './calculatorTools'

describe('calculator tools', () => {
  it('evaluates arithmetic without allowing symbols or assignments', () => {
    expect(evaluateExpression('2 * (3 + 4)=')).toBe('14')
    expect(() => evaluateExpression('x = 2')).toThrow()
  })

  it('converts integer bases and performs number operations', () => {
    expect(convertBase('255', 10, 16)).toBe('ff')
    expect(convertBase('11111111', 2, 10)).toBe('255')
    expect(gcd('54', '24')).toBe('6')
    expect(lcm('6', '8')).toBe('24')
  })

  it('calculates permutations and combinations exactly', () => {
    expect(permutation('5', '2')).toBe('20')
    expect(combination('5', '2')).toBe('10')
  })
})
