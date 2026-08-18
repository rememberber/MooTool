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

export function parsePageSelection(value: string, pageCount: number): number[] {
  if (!Number.isInteger(pageCount) || pageCount < 1) throw new PdfPageRangeError('invalidPageCount')
  const normalized = value.trim()
  if (!normalized) return Array.from({ length: pageCount }, (_, index) => index + 1)
  const pages = new Set<number>()
  for (const rawToken of normalized.split(/[;,，；\s]+/)) {
    if (!rawToken) continue
    const range = rawToken.match(/^(\d+)(?:\s*-\s*(\d+))?$/)
    if (!range) throw new PdfPageRangeError('invalidToken', { token: rawToken })
    const start = Number(range[1])
    const end = Number(range[2] ?? range[1])
    if (start < 1 || end < start || end > pageCount) throw new PdfPageRangeError('outOfRange', { pageCount, token: rawToken })
    for (let page = start; page <= end; page += 1) pages.add(page)
  }
  if (!pages.size) throw new PdfPageRangeError('emptySelection')
  return [...pages].sort((left, right) => left - right)
}

export function selectSplitPages(pageRange: string, rule: PdfSplitRule, customRule: string, pageCount: number): number[] {
  const allowed = new Set(parsePageSelection(pageRange, pageCount))
  const pages = rule === 'custom'
    ? parsePageSelection(customRule, pageCount).filter((page) => allowed.has(page))
    : [...allowed].filter((page) => rule === 'odd' ? page % 2 === 1 : page % 2 === 0)
  if (!pages.length) throw new PdfPageRangeError('splitEmpty')
  return pages
}

export function splitOutputName(name: string): string {
  const base = name.replace(/\.pdf$/i, '')
  return `${base}_split.pdf`
}
