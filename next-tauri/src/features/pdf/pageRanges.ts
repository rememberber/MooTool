export type PdfSplitRule = 'odd' | 'even' | 'custom'
export type PdfPageRangeErrorCode = 'invalidPageCount' | 'invalidToken' | 'outOfRange' | 'emptySelection' | 'splitEmpty'

export class PdfPageRangeError extends Error {
  constructor(
    readonly code: PdfPageRangeErrorCode,
    readonly values: Record<string, string | number> = {}
  ) {
    super(`PDF_PAGE_RANGE_${code}`)
    this.name = 'PdfPageRangeError'
  }
}

export function parsePageSelection(expression: string, pageCount: number): number[] {
  if (!Number.isSafeInteger(pageCount) || pageCount < 1) throw new PdfPageRangeError('invalidPageCount')
  const value = expression.trim().replace(/[，,]/g, ';').replace(/\s+/g, '')
  if (!value || !/^\d+(?:-\d+)?(?:;\d+(?:-\d+)?)*$/.test(value)) {
    throw new PdfPageRangeError('invalidToken', { token: expression.trim() || value })
  }
  const pages = new Set<number>()
  for (const token of value.split(';')) {
    const [startText, endText = startText] = token.split('-')
    const start = Number(startText)
    const end = Number(endText)
    if (start < 1 || end < start || end > pageCount) throw new PdfPageRangeError('outOfRange', { pageCount, token })
    for (let page = start; page <= end; page += 1) pages.add(page)
  }
  return [...pages]
}

export function selectSplitPages(pageRange: string, rule: PdfSplitRule, customRule: string, pageCount: number): number[] {
  const candidates = parsePageSelection(pageRange, pageCount)
  const selected = rule === 'custom' ? new Set(parsePageSelection(customRule, pageCount)) : null
  const pages = candidates.filter((page) => {
    if (rule === 'odd') return page % 2 === 1
    if (rule === 'even') return page % 2 === 0
    return selected?.has(page) ?? false
  })
  if (!pages.length) throw new PdfPageRangeError('splitEmpty')
  return pages
}

export function splitOutputName(name: string): string {
  const base = name.replace(/\.pdf$/i, '').toLowerCase()
  return `${base}_split.pdf`
}
