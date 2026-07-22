import { describe, expect, it } from 'vitest'
import { clipboardContainsImage, prepareMarkdownImageInsertion } from './quickNoteAttachments'

function clipboardData(input: { itemTypes?: string[]; files?: Array<{ name: string; type: string }>; types?: string[] }) {
  return {
    items: (input.itemTypes ?? []).map((type) => ({ type, getAsFile: () => null })),
    files: input.files ?? [],
    types: input.types ?? []
  } as unknown as DataTransfer
}

describe('clipboardContainsImage', () => {
  it('recognizes images exposed as clipboard items, files, or MIME types', () => {
    expect(clipboardContainsImage(clipboardData({ itemTypes: ['image/png'] }))).toBe(true)
    expect(clipboardContainsImage(clipboardData({ files: [{ name: 'photo', type: 'image/jpeg' }] }))).toBe(true)
    expect(clipboardContainsImage(clipboardData({ files: [{ name: 'photo.WEBP', type: '' }] }))).toBe(true)
    expect(clipboardContainsImage(clipboardData({ types: ['IMAGE/WEBP'] }))).toBe(true)
  })

  it('leaves regular text and HTML pastes alone', () => {
    expect(clipboardContainsImage(clipboardData({ itemTypes: ['text/plain'], types: ['text/plain', 'text/html'] }))).toBe(false)
    expect(clipboardContainsImage(null)).toBe(false)
  })
})

describe('prepareMarkdownImageInsertion', () => {
  const markdown = '![image](attachments/pixel.png)'

  it('keeps an image on its own Markdown line when pasted inside text', () => {
    expect(prepareMarkdownImageInsertion('beforeafter', { start: 6, end: 6 }, markdown)).toEqual({
      start: 6,
      end: 6,
      text: `\n${markdown}\n`,
      caret: 6 + markdown.length + 2
    })
  })

  it('does not add duplicate line breaks at line boundaries', () => {
    expect(prepareMarkdownImageInsertion('before\nafter', { start: 7, end: 7 }, markdown)).toEqual({
      start: 7,
      end: 7,
      text: `${markdown}\n`,
      caret: 7 + markdown.length + 1
    })
  })

  it('clamps an out-of-range selection', () => {
    expect(prepareMarkdownImageInsertion('', { start: 20, end: 40 }, markdown)).toEqual({
      start: 0,
      end: 0,
      text: markdown,
      caret: markdown.length
    })
  })
})
