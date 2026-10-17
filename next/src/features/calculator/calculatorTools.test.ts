import { describe, expect, it } from 'vitest'
import { combination, convertBase, evaluateExpression, gcd, hexFloatToDecimal, lcm, permutation } from './calculatorTools'

describe('calculator tools', () => {
  it('evaluates arithmetic without allowing symbols or assignments', () => {
    expect(evaluateExpression('2 * (3 + 4)=')).toBe('14')
    expect(evaluateExpression('-3 + 10 / 2')).toBe('2')
    expect(evaluateExpression('.5 * 8')).toBe('4')
    expect(() => evaluateExpression('x = 2')).toThrow()
    expect(() => evaluateExpression('1 / 0')).toThrow()
    expect(() => evaluateExpression('(1 + 2')).toThrow()
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

  it.each([
    ['A.F', '10.9375'],
    [' -0xA.F ', '-10.9375'],
    ['+0X.8', '0.5'],
    ['.1', '0.0625'],
    ['ff.', '255'],
    ['0.0001', '0.0000152587890625'],
    ['20000000000001.8', '9007199254740993.5'],
    ['0.0000000000000001', '0.0000000000000000000542101086242752217003726400434970855712890625']
  ])('converts hexadecimal fraction %s to decimal exactly', (hex, decimal) => {
    expect(convertBase(hex, 16, 10)).toBe(decimal)
  })

  it('converts terminating fractions back and preserves integer precision', () => {
    expect(convertBase('10.9375', 10, 16)).toBe('a.f')
    expect(convertBase('9007199254740993.5', 10, 16)).toBe('20000000000001.8')
    expect(convertBase('0.0000', 10, 16)).toBe('0')
    expect(convertBase('1010.1111', 2, 10)).toBe('10.9375')
    expect(convertBase('10.9375', 10, 2)).toBe('1010.1111')
    expect(convertBase('-0b1010', 2, 10)).toBe('-10')
    expect(convertBase('ffffffffffffffff', 16, 10)).toBe('18446744073709551615')
    expect(() => convertBase('0.1', 10, 16)).toThrow('repeating fraction')
  })

  it.each(['', '.', '0x', '0x.', 'A..F', '1.G', '1p4', '1 2.3', '--a.f'])('rejects malformed hexadecimal fraction %s', value => {
    expect(() => convertBase(value, 16, 10)).toThrow()
  })

  it.each([
    ['3F800000', 32, '1.0'],
    [' 0x40 49 0f db ', 32, '3.1415927410125732'],
    ['C0200000', 32, '-2.5'],
    ['00000000', 32, '0.0'],
    ['80000000', 32, '-0.0'],
    ['00000001', 32, '1.401298464324817e-45'],
    ['7F7FFFFF', 32, '3.4028234663852886e+38'],
    ['7F800000', 32, 'Infinity'],
    ['FF800000', 32, '-Infinity'],
    ['7FC00000', 32, 'NaN'],
    ['3FF0000000000000', 64, '1.0'],
    ['400921fb54442d18', 64, '3.141592653589793'],
    ['C004000000000000', 64, '-2.5'],
    ['8000000000000000', 64, '-0.0'],
    ['0000000000000001', 64, '5e-324'],
    ['7FEFFFFFFFFFFFFF', 64, '1.7976931348623157e+308'],
    ['7FF0000000000000', 64, 'Infinity'],
    ['FFF0000000000000', 64, '-Infinity'],
    ['7FF8000000000000', 64, 'NaN']
  ] as const)('decodes IEEE 754 %s (%i-bit)', (hex, bits, decimal) => {
    expect(hexFloatToDecimal(hex, bits)).toBe(decimal)
  })

  it('decodes little-endian IEEE 754 byte sequences', () => {
    expect(hexFloatToDecimal('00 00 80 3F', 32, 'little')).toBe('1.0')
    expect(hexFloatToDecimal('00 00 00 00 00 00 F0 3F', 64, 'little')).toBe('1.0')
    expect(hexFloatToDecimal('00 00 00 80', 32, 'little')).toBe('-0.0')
  })

  it.each(['', '3F80000', '3F8000000', '3FF0000000000000', '3G800000', '-3F800000', '3F80.000', '0x0x3F800000'])('rejects malformed IEEE 754 input %s', value => {
    expect(() => hexFloatToDecimal(value, 32)).toThrow('exactly 8 hexadecimal digits')
  })

  it('requires the full 64-bit representation', () => {
    expect(() => hexFloatToDecimal('3F800000', 64)).toThrow('exactly 16 hexadecimal digits')
  })
})
