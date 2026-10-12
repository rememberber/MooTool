import { describe, expect, it } from 'vitest'
import { classifyPreview, decodePreviewText, jsonPreview, readPreviewBytes, scanPreviewMatches } from './filePreview'
import { defaultFindReplaceOptions } from '@/shared/components/findReplace'

describe('file preview', () => {
  it('classifies local files without executing HTML or SVG', () => {
    expect(classifyPreview('photo.PNG')).toBe('image')
    expect(classifyPreview('report.pdf')).toBe('pdf')
    expect(classifyPreview('config.jsonc')).toBe('json')
    expect(classifyPreview('index.html')).toBe('text')
    expect(classifyPreview('remote.svg')).toBe('text')
    expect(classifyPreview('Dockerfile')).toBe('text')
    expect(classifyPreview('archive.zip')).toBe('unsupported')
  })
  it('decodes UTF-8 and UTF-16 BOMs and Chinese legacy text explicitly', () => {
    expect(decodePreviewText(new TextEncoder().encode('\ufeff中文')).text).toBe('中文')
    expect(decodePreviewText(new Uint8Array([255, 254, 45, 78]))).toEqual({ text: '中', encoding: 'utf-16le' })
    expect(decodePreviewText(new Uint8Array([254, 255, 78, 45]))).toEqual({ text: '中', encoding: 'utf-16be' })
    expect(decodePreviewText(new Uint8Array([214, 208]), 'gb18030').text).toBe('中')
  })
  it('rejects binary and malformed encodings instead of replacing characters', () => {
    expect(() => decodePreviewText(new Uint8Array([97, 0]))).toThrow('binary')
    expect(() => decodePreviewText(new Uint8Array([214, 208]))).toThrow('encoding')
    expect(() => decodePreviewText(new Uint8Array([255, 254, 45]))).toThrow('encoding')
  })
  it('formats JSON without rounding integers or dropping duplicate keys', () => {
    const result = jsonPreview('{"id":12345678901234567890,"x":1,"x":2}', 'data.json')
    expect(result.valid).toBe(true)
    expect(result.formatted).toContain('12345678901234567890')
    expect(result.formatted).toContain('"x": 1')
    expect(result.formatted).toContain('"x": 2')
  })
  it('keeps invalid JSON readable and applies JSONC rules by extension', () => {
    const text = '{"broken":}'
    expect(jsonPreview(text, 'bad.json')).toEqual({ valid: false, formatted: text })
    expect(jsonPreview('{/* comment */"a":1,}', 'config.jsonc').valid).toBe(true)
    expect(jsonPreview('{/* comment */"a":1,}', 'config.json').valid).toBe(false)
  })
  it('checks byte limits before reading a file', async () => {
    const file = { size: 3 * 1024 ** 2, arrayBuffer: () => { throw new Error('must not read') } } as unknown as File
    await expect(readPreviewBytes(file, 'text')).rejects.toThrow('tooLarge')
    await expect(readPreviewBytes({ size: 0, arrayBuffer: async () => { throw new Error('deleted') } } as unknown as File, 'text')).rejects.toThrow('unreadable')
  })
  it('finds literal text and wraps in both directions without interpreting regex', () => {
    const text = '中文 a.b A.B aXb 中文'
    const options = { ...defaultFindReplaceOptions, regex: true }
    expect(scanPreviewMatches(text, 'a.b', options)).toEqual({ count: 2, match: { start: 3, end: 6 }, wrapped: false })
    expect(scanPreviewMatches(text, 'a.b', options, 6)).toEqual({ count: 2, match: { start: 7, end: 10 }, wrapped: false })
    expect(scanPreviewMatches(text, 'a.b', options, 10)).toEqual({ count: 2, match: { start: 3, end: 6 }, wrapped: true })
    expect(scanPreviewMatches(text, 'a.b', options, 0, false)).toEqual({ count: 2, match: { start: 7, end: 10 }, wrapped: true })
    expect(scanPreviewMatches(text, '中文', options).count).toBe(2)
  })
  it('counts matches in large files and honors case and whole-word options', () => {
    expect(scanPreviewMatches('a'.repeat(100_000), 'a', defaultFindReplaceOptions, 90_000).match).toEqual({ start: 90_000, end: 90_001 })
    expect(scanPreviewMatches('cat CAT category', 'cat', { ...defaultFindReplaceOptions, matchCase: true, wholeWord: true }).count).toBe(1)
    expect(scanPreviewMatches('text', '', defaultFindReplaceOptions)).toEqual({ count: 0, match: null, wrapped: false })
    expect(scanPreviewMatches('text', 'missing', defaultFindReplaceOptions).match).toBeNull()
  })
})
