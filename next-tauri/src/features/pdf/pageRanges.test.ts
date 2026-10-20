import { describe, expect, it } from 'vitest'
import { PdfPageRangeError, parsePageSelection, selectSplitPages, splitOutputName } from './pageRanges'

describe('PDF page ranges', () => {
  it('parses ranges in source order and rejects malformed selections', () => {
    expect(parsePageSelection('1-3;2;7;9-10', 10)).toEqual([1, 2, 3, 7, 9, 10])
    expect(parsePageSelection('1-2, 4，6', 10)).toEqual([1, 2, 4, 6])
    expect(parsePageSelection('3,1-2', 5)).toEqual([3, 1, 2])
    expectRangeError(() => parsePageSelection('', 3), 'invalidToken')
    expectRangeError(() => parsePageSelection('1;;2', 5), 'invalidToken')
    expectRangeError(() => parsePageSelection('0-2', 4), 'outOfRange')
    expectRangeError(() => parsePageSelection('3-1', 4), 'outOfRange')
    expectRangeError(() => parsePageSelection('1-6', 5), 'outOfRange')
  })

  it('selects odd, even, and custom pages inside the input range', () => {
    expect(selectSplitPages('2-8', 'odd', '', 10)).toEqual([3, 5, 7])
    expect(selectSplitPages('2-8', 'even', '', 10)).toEqual([2, 4, 6, 8])
    expect(selectSplitPages('2-8', 'custom', '8;3;1', 10)).toEqual([3, 8])
    expect(splitOutputName('Quarterly.PDF')).toBe('quarterly_split.pdf')
  })
})

function expectRangeError(run: () => unknown, code: PdfPageRangeError['code']) {
  try {
    run()
    throw new Error('expected PDF page range validation to fail')
  } catch (cause) {
    expect(cause).toBeInstanceOf(PdfPageRangeError)
    expect((cause as PdfPageRangeError).code).toBe(code)
  }
}
