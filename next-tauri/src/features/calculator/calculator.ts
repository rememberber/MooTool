export type CalculatorToolErrorCode =
  | 'expressionInvalid'
  | 'nonFinite'
  | 'valueRequired'
  | 'baseMismatch'
  | 'repeatingFraction'
  | 'floatDigits'
  | 'integerRequired'
  | 'countRange'
  | 'expressionFormat'
  | 'parenthesesMismatch'

export class CalculatorToolError extends Error {
  constructor(readonly code: CalculatorToolErrorCode, readonly values?: Record<string, string | number>) {
    super(code)
    this.name = 'CalculatorToolError'
  }
}

export function evaluateExpression(expression: string): string {
  const source = expression.trim().replace(/=$/, '')
  if (!source || source.length > 500 || !/^[\d+\-*/().\s]+$/.test(source)) {
    throw new CalculatorToolError('expressionInvalid')
  }

  const result = new ArithmeticParser(source).parse()
  if (!Number.isFinite(result)) throw new CalculatorToolError('nonFinite')
  return Number.parseFloat(result.toPrecision(14)).toString()
}

export function convertBase(value: string, from: 2 | 10 | 16, to: 2 | 10 | 16): string {
  const normalized = value.trim()
  if (!normalized) throw new CalculatorToolError('valueRequired')
  const sign = normalized.startsWith('-') ? -1n : 1n
  let unsigned = normalized.replace(/^[+-]/, '')
  if (from === 16) unsigned = unsigned.replace(/^0x/i, '')
  if (from === 2) unsigned = unsigned.replace(/^0b/i, '')
  const valid = from === 2 ? /^(?:[01]+(?:\.[01]*)?|\.[01]+)$/ : from === 10 ? /^(?:\d+(?:\.\d*)?|\.\d+)$/ : /^(?:[\da-f]+(?:\.[\da-f]*)?|\.[\da-f]+)$/i
  if (!valid.test(unsigned)) throw new CalculatorToolError('baseMismatch')
  const [integer, fraction = ''] = unsigned.split('.')
  const digits = `${integer || '0'}${fraction}`
  const parsed = from === 10 ? BigInt(digits) : BigInt(`${from === 2 ? '0b' : '0x'}${digits}`)
  if (!fraction) return `${sign < 0 ? '-' : ''}${parsed.toString(to)}`
  let denominator = BigInt(from) ** BigInt(fraction.length)
  const divisor = gcdBigInt(parsed, denominator)
  const numerator = parsed / divisor
  denominator /= divisor

  let remaining = denominator
  const base = BigInt(to)
  while (remaining !== 1n) {
    const factor = gcdBigInt(remaining, base)
    if (factor === 1n) throw new CalculatorToolError('repeatingFraction')
    remaining /= factor
  }
  let output = (numerator / denominator).toString(to)
  let remainder = numerator % denominator
  if (remainder !== 0n) output += '.'
  while (remainder !== 0n) {
    remainder *= base
    output += (remainder / denominator).toString(to)
    remainder %= denominator
  }
  return `${sign < 0 ? '-' : ''}${output}`
}

export function hexFloatToDecimal(value: string, bits: 32 | 64, byteOrder: 'big' | 'little' = 'big'): string {
  const hex = value.trim().replace(/^0x/i, '').replace(/\s+/g, '')
  const length = bits / 4
  if (!new RegExp(`^[\\da-f]{${length}}$`, 'i').test(hex)) {
    throw new CalculatorToolError('floatDigits', { bits, digits: length })
  }
  const bytes = new DataView(new ArrayBuffer(bits / 8))
  for (let index = 0; index < hex.length; index += 2) bytes.setUint8(index / 2, Number.parseInt(hex.slice(index, index + 2), 16))
  const result = bits === 32 ? bytes.getFloat32(0, byteOrder === 'little') : bytes.getFloat64(0, byteOrder === 'little')
  if (Object.is(result, -0)) return '-0.0'
  const output = String(result)
  return Number.isFinite(result) && !/[.e]/i.test(output) ? `${output}.0` : output
}

function gcdBigInt(a: bigint, b: bigint): bigint {
  while (b !== 0n) [a, b] = [b, a % b]
  return a
}

export function greatestCommonDivisor(left: string, right: string): string {
  let a = absolute(parseInteger(left))
  let b = absolute(parseInteger(right))
  while (b !== 0n) [a, b] = [b, a % b]
  return String(a)
}

export function leastCommonMultiple(left: string, right: string): string {
  const a = parseInteger(left)
  const b = parseInteger(right)
  if (a === 0n || b === 0n) return '0'
  return String(absolute((a / BigInt(greatestCommonDivisor(left, right))) * b))
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
  for (let index = 1; index <= m; index += 1) {
    result = (result * BigInt(n - m + index)) / BigInt(index)
  }
  return String(result)
}

function parseInteger(value: string): bigint {
  if (!/^[+-]?\d+$/.test(value.trim())) throw new CalculatorToolError('integerRequired')
  return BigInt(value.trim())
}

function parseCountPair(nValue: string, mValue: string): [number, number] {
  const n = Number(nValue)
  const m = Number(mValue)
  if (!Number.isSafeInteger(n) || !Number.isSafeInteger(m) || n < 0 || m < 0 || m > n || n > 5000) {
    throw new CalculatorToolError('countRange')
  }
  return [n, m]
}

function absolute(value: bigint): bigint {
  return value < 0n ? -value : value
}

class ArithmeticParser {
  private position = 0

  constructor(private readonly source: string) {}

  parse(): number {
    const result = this.parseAddition()
    this.skipWhitespace()
    if (this.position !== this.source.length) throw new CalculatorToolError('expressionFormat')
    return result
  }

  private parseAddition(): number {
    let result = this.parseMultiplication()
    while (true) {
      if (this.consume('+')) result += this.parseMultiplication()
      else if (this.consume('-')) result -= this.parseMultiplication()
      else return result
    }
  }

  private parseMultiplication(): number {
    let result = this.parseUnary()
    while (true) {
      if (this.consume('*')) result *= this.parseUnary()
      else if (this.consume('/')) result /= this.parseUnary()
      else return result
    }
  }

  private parseUnary(): number {
    if (this.consume('+')) return this.parseUnary()
    if (this.consume('-')) return -this.parseUnary()
    return this.parsePrimary()
  }

  private parsePrimary(): number {
    if (this.consume('(')) {
      const result = this.parseAddition()
      if (!this.consume(')')) throw new CalculatorToolError('parenthesesMismatch')
      return result
    }

    this.skipWhitespace()
    const match = /^(?:\d+(?:\.\d*)?|\.\d+)/.exec(this.source.slice(this.position))
    if (!match) throw new CalculatorToolError('expressionFormat')
    this.position += match[0].length
    return Number(match[0])
  }

  private consume(token: string): boolean {
    this.skipWhitespace()
    if (this.source[this.position] !== token) return false
    this.position += 1
    return true
  }

  private skipWhitespace(): void {
    while (/\s/.test(this.source[this.position] ?? '')) this.position += 1
  }
}
