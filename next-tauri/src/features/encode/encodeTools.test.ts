import { describe, expect, it } from 'vitest'
import {
  asciiToText,
  base64ToText,
  fromUnicode,
  hexToText,
  textToAscii,
  textToBase64,
  textToHex,
  toUnicode,
  urlDecode,
  urlEncode
} from './encodeTools'

describe('encoding tools', () => {
  it('round-trips Unicode including supplementary-plane characters', () => {
    const source = 'MooTool 编码 🐮'
    expect(fromUnicode(toUnicode(source))).toBe(source)
    expect(toUnicode('🐮')).toBe('\\ud83d\\udc2e')
    expect(fromUnicode('\\u{1F42E}')).toBe('\\u{1F42E}')
  })

  it('round-trips URL encoding in UTF-8 and GB2312', () => {
    const source = '路径/编码'
    expect(urlDecode(urlEncode(source, 'utf-8'), 'utf-8')).toBe(source)
    expect(urlDecode(urlEncode(source, 'gb2312'), 'gb2312')).toBe(source)
    expect(urlDecode('%GG', 'utf-8')).toBe('%GG')
  })

  it('round-trips UTF-8 text through Base64 and URL-safe Base64', () => {
    const source = 'MooTool · 你好'
    const encoded = textToBase64(source)
    expect(base64ToText(encoded)).toBe(source)
    expect(base64ToText(encoded.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')))
      .toBe(source)
    expect(() => base64ToText('???')).toThrow('ENCODE_TOOL_base64')
  })

  it('round-trips UTF-8 text through hexadecimal bytes', () => {
    const source = 'MooTool 你好'
    expect(hexToText(textToHex(source))).toBe(source)
    expect(() => hexToText('abc')).toThrow('ENCODE_TOOL_hex')
  })

  it('converts decimal and hexadecimal code points', () => {
    expect(textToAscii('A中', 'decimal')).toBe('65 20013')
    expect(textToAscii('A中', 'hex')).toBe('41 4E2D')
    expect(asciiToText('65 4E2D')).toBe('A中')
    expect(asciiToText(textToAscii('A牛', 'decimal'))).toBe('A牛')
  })
})
