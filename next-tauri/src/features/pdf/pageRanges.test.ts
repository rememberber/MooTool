import { describe, expect, it } from 'vitest'
import { PdfPageRangeError, parsePageSelection, selectSplitPages, splitOutputName } from './pageRanges'

describe('PDF page ranges', () => {
  it('parses, deduplicates, and orders page selections', () => {
    expect(parsePageSelection('3,1-2;2,5', 5)).toEqual([1, 2, 3, 5])
    expect(parsePageSelection('', 3)).toEqual([1, 2, 3])
    expectRangeError(() => parsePageSelection('0-2', 4), 'outOfRange')
    expectRangeError(() => parsePageSelection('3-2', 4), 'outOfRange')
  })

  it('selects odd, even, and custom pages inside the input range', () => {
    expect(selectSplitPages('1-6', 'odd', '', 6)).toEqual([1, 3, 5])
    expect(selectSplitPages('2-6', 'even', '', 6)).toEqual([2, 4, 6])
    expect(selectSplitPages('2-6', 'custom', '1,3,6', 6)).toEqual([3, 6])
    expect(splitOutputName('Quarterly.PDF')).toBe('Quarterly_split.pdf')
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
