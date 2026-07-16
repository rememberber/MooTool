import { describe, expect, it } from 'vitest'
import { asciiToText, fromUnicode, hexToText, textToAscii, textToHex, toUnicode, urlDecode, urlEncode } from './encodeTools'

describe('encode tools', () => {
  it('round trips Unicode and UTF-8 hex', () => {
    expect(fromUnicode(toUnicode('Moo 工具 🚀'))).toBe('Moo 工具 🚀')
    expect(hexToText(textToHex('你好 Moo'))).toBe('你好 Moo')
  })

  it('round trips URL text in UTF-8 and GB2312', () => {
    expect(urlDecode(urlEncode('你好 a/b', 'utf-8'), 'utf-8')).toBe('你好 a/b')
    expect(urlDecode(urlEncode('编码测试', 'gb2312'), 'gb2312')).toBe('编码测试')
  })

  it('converts Unicode code points to and from decimal or hex ASCII lists', () => {
    expect(textToAscii('A中', 'decimal')).toBe('65 20013')
    expect(textToAscii('A中', 'hex')).toBe('41 4E2D')
    expect(asciiToText('65 4E2D')).toBe('A中')
  })
})
