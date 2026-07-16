import { Buffer } from 'buffer'
import iconv from 'iconv-lite'

export type UrlCharset = 'utf-8' | 'gb2312'
export type AsciiFormat = 'decimal' | 'hex'

export function toUnicode(value: string): string {
  return Array.from(value, (character) => {
    const codePoint = character.codePointAt(0) ?? 0
    if (codePoint <= 0x7f) return character
    if (codePoint <= 0xffff) return `\\u${codePoint.toString(16).padStart(4, '0')}`
    const offset = codePoint - 0x10000
    const high = 0xd800 + (offset >> 10)
    const low = 0xdc00 + (offset & 0x3ff)
    return `\\u${high.toString(16)}\\u${low.toString(16)}`
  }).join('')
}

export function fromUnicode(value: string): string {
  return value.replace(/\\u([\da-fA-F]{4})/g, (_match, hex: string) => String.fromCharCode(Number.parseInt(hex, 16)))
}

export function urlEncode(value: string, charset: UrlCharset): string {
  const bytes = iconv.encode(value, charset)
  return Array.from(bytes, (byte) => isUnreserved(byte) ? String.fromCharCode(byte) : `%${byte.toString(16).toUpperCase().padStart(2, '0')}`).join('')
}

export function urlDecode(value: string, charset: UrlCharset): string {
  const bytes: number[] = []
  const source = value.replace(/\+/g, ' ')
  for (let index = 0; index < source.length;) {
    if (source[index] === '%' && /^[\da-fA-F]{2}$/.test(source.slice(index + 1, index + 3))) {
      bytes.push(Number.parseInt(source.slice(index + 1, index + 3), 16))
      index += 3
    } else {
      bytes.push(...iconv.encode(source[index], charset))
      index += 1
    }
  }
  return iconv.decode(Buffer.from(bytes), charset)
}

export function textToHex(value: string): string {
  return Array.from(new TextEncoder().encode(value), (byte) => byte.toString(16).padStart(2, '0')).join('')
}

export function hexToText(value: string): string {
  const normalized = value.replace(/[\s:_-]+/g, '')
  if (!normalized || normalized.length % 2 !== 0 || !/^[\da-fA-F]+$/.test(normalized)) throw new Error('Invalid hexadecimal input')
  const bytes = Uint8Array.from(normalized.match(/../g) ?? [], (pair) => Number.parseInt(pair, 16))
  return new TextDecoder('utf-8', { fatal: true }).decode(bytes)
}

export function textToAscii(value: string, format: AsciiFormat): string {
  return Array.from(value, (character) => {
    const code = character.codePointAt(0) ?? 0
    return format === 'hex' ? code.toString(16).toUpperCase() : String(code)
  }).join(' ')
}

export function asciiToText(value: string): string {
  if (!value.trim()) return ''
  return value.trim().split(/[\s,;]+/).map((part) => {
    const radix = /^0x/i.test(part) || /[a-f]/i.test(part) ? 16 : 10
    const normalized = part.replace(/^0x/i, '')
    const codePoint = Number.parseInt(normalized, radix)
    if (!Number.isInteger(codePoint) || codePoint < 0 || codePoint > 0x10ffff) throw new Error(`Invalid code point: ${part}`)
    return String.fromCodePoint(codePoint)
  }).join('')
}

function isUnreserved(byte: number): boolean {
  return (byte >= 0x41 && byte <= 0x5a) || (byte >= 0x61 && byte <= 0x7a) || (byte >= 0x30 && byte <= 0x39) || [0x2d, 0x2e, 0x5f, 0x7e].includes(byte)
}
