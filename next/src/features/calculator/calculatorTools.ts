import { Parser } from 'expr-eval'

const arithmeticParser = new Parser({
  allowMemberAccess: false,
  operators: {
    add: true,
    subtract: true,
    multiply: true,
    divide: true,
    power: false,
    factorial: false,
    remainder: false,
    comparison: false,
    concatenate: false,
    conditional: false,
    logical: false,
    assignment: false,
    fndef: false
  }
})

export function evaluateExpression(expression: string): string {
  const source = expression.trim().replace(/=$/, '')
  if (!source || source.length > 500 || !/^[\d+\-*/().\s]+$/.test(source)) throw new Error('Invalid expression')
  const result: unknown = arithmeticParser.evaluate(source)
  if (typeof result !== 'number' || !Number.isFinite(result)) throw new Error('Expression did not produce a finite number')
  return Number.parseFloat(result.toPrecision(14)).toString()
}

export function convertBase(value: string, from: 2 | 10 | 16, to: 2 | 10 | 16): string {
  const normalized = value.trim()
  if (!normalized) throw new Error('A value is required')
  const sign = normalized.startsWith('-') ? -1n : 1n
  const unsigned = normalized.replace(/^[+-]/, '')
  const valid = from === 2 ? /^[01]+$/ : from === 10 ? /^\d+$/ : /^[\da-f]+$/i
  if (!valid.test(unsigned)) throw new Error('Invalid value for the selected base')
  const parsed = from === 10 ? BigInt(unsigned) : BigInt(`${from === 2 ? '0b' : '0x'}${unsigned}`)
  return `${sign < 0 ? '-' : ''}${parsed.toString(to)}`
}

export function gcd(left: string, right: string): string {
  let a = absBigInt(parseInteger(left))
  let b = absBigInt(parseInteger(right))
  while (b !== 0n) [a, b] = [b, a % b]
  return String(a)
}

export function lcm(left: string, right: string): string {
  const a = parseInteger(left)
  const b = parseInteger(right)
  if (a === 0n || b === 0n) return '0'
  return String(absBigInt((a / BigInt(gcd(left, right))) * b))
}

export function permutation(nValue: string, mValue: string): string {
  const [n, m] = parseCountPair(nValue, mValue)
  let result = 1n
  for (let value = n - m + 1; value <= n; value += 1) result *= BigInt(value)
  return String(result)
}

export function combination(nValue: string, mValue: string): string {
  const [n, requested] = parseCountPair(nValue, mValue)
  const m = Math.min(requested, n - requested)
  let result = 1n
  for (let index = 1; index <= m; index += 1) result = (result * BigInt(n - m + index)) / BigInt(index)
  return String(result)
}

function parseInteger(value: string): bigint {
  if (!/^[+-]?\d+$/.test(value.trim())) throw new Error('An integer is required')
  return BigInt(value.trim())
}

function parseCountPair(nValue: string, mValue: string): [number, number] {
  const n = Number(nValue)
  const m = Number(mValue)
  if (!Number.isSafeInteger(n) || !Number.isSafeInteger(m) || n < 0 || m < 0 || m > n || n > 5000) throw new Error('Require 0 <= m <= n <= 5000')
  return [n, m]
}

function absBigInt(value: bigint): bigint {
  return value < 0n ? -value : value
}
