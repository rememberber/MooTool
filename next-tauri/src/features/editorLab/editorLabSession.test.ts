import { describe, expect, it } from 'vitest'
import { describeEditorLabState } from './editorLabSession'

describe('CodeMirror P0 session description', () => {
  it('captures Unicode content, selection, search and IME counters', () => {
    const description = describeEditorLabState({
      content: '你好，世界 / こんにちは世界',
      anchor: 8,
      head: 2,
      line: 3,
      scrollTop: 41.6,
      scrollLeft: 2.2,
      searchQuery: '世界',
      changeCount: 4,
      compositionStarts: 2,
      compositionEnds: 2
    })

    expect(JSON.parse(description.digest)).toEqual({
      content: '你好，世界 / こんにちは世界',
      anchor: 8,
      head: 2,
      line: 3,
      scrollTop: 42,
      scrollLeft: 2,
      searchQuery: '世界',
      changeCount: 4,
      compositionStarts: 2,
      compositionEnds: 2
    })
    expect(description.summary).toBe('15 字符 · 选区 2:8 · 第 3 行 · IME 2/2')
  })
})
