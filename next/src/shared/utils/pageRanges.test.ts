import { describe, expect, it } from 'vitest'
import { parsePageSelection, selectSplitPages } from './pageRanges'

describe('PDF page ranges', () => {
  it('parses ranges, specified pages and duplicates in source order', () => {
    expect(parsePageSelection('1-3;2;7;9-10', 10)).toEqual([1, 2, 3, 7, 9, 10])
    expect(parsePageSelection('1-2, 4，6', 10)).toEqual([1, 2, 4, 6])
  })

  it('rejects malformed and out-of-range selections', () => {
    expect(() => parsePageSelection('3-1', 5)).toThrow()
    expect(() => parsePageSelection('1;;2', 5)).toThrow()
    expect(() => parsePageSelection('1-6', 5)).toThrow()
  })

  it('applies odd, even and custom split rules inside the outer range', () => {
    expect(selectSplitPages('2-8', 'odd', '', 10)).toEqual([3, 5, 7])
    expect(selectSplitPages('2-8', 'even', '', 10)).toEqual([2, 4, 6, 8])
    expect(selectSplitPages('2-8', 'custom', '1;3-4;8-10', 10)).toEqual([3, 4, 8])
  })
})
