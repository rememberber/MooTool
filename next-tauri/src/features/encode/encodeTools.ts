import { Buffer } from 'buffer'
import iconv from 'iconv-lite'

export const encodeTabs = ['unicode', 'url', 'base64', 'hex', 'ascii'] as const

export type EncodeTab = (typeof encodeTabs)[number]
export type UrlCharset = 'utf-8' | 'gb2312'
export type AsciiFormat = 'decimal' | 'hex'
export type ConversionDirection = 'forward' | 'reverse'
export type EncodeToolErrorCode = 'unicodeRange' | 'urlEscape' | 'base64' | 'hex' | 'codePoint' | 'codePointRange'

export class EncodeToolError extends Error {
  constructor(readonly code: EncodeToolErrorCode, readonly values?: Record<string, string>) {
    super(`ENCODE_TOOL_${code}`)
    this.name = 'EncodeToolError'
  }
}

export function convertEncoding(
  tab: EncodeTab,
  direction: ConversionDirection,
  value: string,
  options: { charset: UrlCharset; asciiFormat: AsciiFormat }
): string {
  if (tab === 'unicode') return direction === 'forward' ? toUnicode(value) : fromUnicode(value)
  if (tab === 'url') {
    return direction === 'forward'
      ? urlEncode(value, options.charset)
      : urlDecode(value, options.charset)
  }
  if (tab === 'base64') return direction === 'forward' ? textToBase64(value) : base64ToText(value)
  if (tab === 'hex') return direction === 'forward' ? textToHex(value) : hexToText(value)
  return direction === 'forward'
    ? textToAscii(value, options.asciiFormat)
    : asciiToText(value)
}

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
  return value
    .replace(/\\u\{([\da-fA-F]{1,6})\}/g, (_match, hex: string) => {
      const codePoint = Number.parseInt(hex, 16)
      if (codePoint > 0x10ffff) throw new EncodeToolError('unicodeRange', { value: hex })
      return String.fromCodePoint(codePoint)
    })
    .replace(/\\u([\da-fA-F]{4})/g, (_match, hex: string) => (
      String.fromCharCode(Number.parseInt(hex, 16))
    ))
}

export function urlEncode(value: string, charset: UrlCharset): string {
  const bytes = iconv.encode(value, charset)
  return Array.from(bytes, (byte) => (
    isUnreserved(byte)
      ? String.fromCharCode(byte)
      : `%${byte.toString(16).toUpperCase().padStart(2, '0')}`
  )).join('')
}

export function urlDecode(value: string, charset: UrlCharset): string {
  const bytes: number[] = []
  const source = value.replace(/\+/g, ' ')
  for (let index = 0; index < source.length;) {
    if (source[index] === '%') {
      const hex = source.slice(index + 1, index + 3)
      if (!/^[\da-fA-F]{2}$/.test(hex)) throw new EncodeToolError('urlEscape', { value: source.slice(index, index + 3) })
      bytes.push(Number.parseInt(hex, 16))
      index += 3
    } else {
      const codePoint = source.codePointAt(index)
      const character = codePoint === undefined ? '' : String.fromCodePoint(codePoint)
      bytes.push(...iconv.encode(character, charset))
      index += character.length
    }
  }
  return iconv.decode(Buffer.from(bytes), charset)
}

export function textToBase64(value: string): string {
  return Buffer.from(value, 'utf8').toString('base64')
}

export function base64ToText(value: string): string {
  const normalized = value
    .trim()
    .replace(/^data:[^,]*;base64,/i, '')
    .replace(/\s+/g, '')
    .replace(/-/g, '+')
    .replace(/_/g, '/')
  if (!normalized) return ''
  if (!/^[A-Za-z0-9+/]*={0,2}$/.test(normalized) || normalized.length % 4 === 1) {
    throw new EncodeToolError('base64')
  }
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
  const bytes = Buffer.from(padded, 'base64')
  const output = new TextDecoder('utf-8', { fatal: true }).decode(bytes)
  const canonical = bytes.toString('base64').replace(/=+$/, '')
  if (canonical !== padded.replace(/=+$/, '')) throw new EncodeToolError('base64')
  return output
}

export function textToHex(value: string): string {
  return Array.from(
    new TextEncoder().encode(value),
    (byte) => byte.toString(16).padStart(2, '0')
  ).join('')
}

export function hexToText(value: string): string {
  const normalized = value.replace(/[\s:_-]+/g, '')
  if (!normalized) return ''
  if (normalized.length % 2 !== 0 || !/^[\da-fA-F]+$/.test(normalized)) {
    throw new EncodeToolError('hex')
  }
  const bytes = Uint8Array.from(
    normalized.match(/../g) ?? [],
    (pair) => Number.parseInt(pair, 16)
  )
  return new TextDecoder('utf-8', { fatal: true }).decode(bytes)
}

export function textToAscii(value: string, format: AsciiFormat): string {
  return Array.from(value, (character) => {
    const code = character.codePointAt(0) ?? 0
    return format === 'hex' ? `0x${code.toString(16).toUpperCase()}` : String(code)
  }).join(' ')
}

export function asciiToText(value: string): string {
  if (!value.trim()) return ''
  return value.trim().split(/[\s,;]+/).map((part) => {
    const radix = /^0x/i.test(part) || /[a-f]/i.test(part) ? 16 : 10
    const normalized = part.replace(/^0x/i, '')
    if (!/^[\da-f]+$/i.test(normalized)) throw new EncodeToolError('codePoint', { value: part })
    const codePoint = Number.parseInt(normalized, radix)
    if (!Number.isInteger(codePoint) || codePoint < 0 || codePoint > 0x10ffff) {
      throw new EncodeToolError('codePointRange', { value: part })
    }
    return String.fromCodePoint(codePoint)
  }).join('')
}

function isUnreserved(byte: number): boolean {
  return (byte >= 0x41 && byte <= 0x5a)
    || (byte >= 0x61 && byte <= 0x7a)
    || (byte >= 0x30 && byte <= 0x39)
    || [0x2d, 0x2e, 0x5f, 0x7e].includes(byte)
}
