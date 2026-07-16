import type { PdfSplitRule } from '../contracts/pdf'

export function parsePageSelection(expression: string, maxPage: number): number[] {
  if (!Number.isSafeInteger(maxPage) || maxPage < 1) throw new Error('PDF has no pages')
  const value = expression.trim().replace(/[，,]/g, ';').replace(/\s+/g, '')
  if (!value || !/^\d+(?:-\d+)?(?:;\d+(?:-\d+)?)*$/.test(value)) throw new Error('Use page ranges such as 1-5;8;10-12')
  const pages = new Set<number>()
  for (const token of value.split(';')) {
    const [startText, endText = startText] = token.split('-')
    const start = Number(startText)
    const end = Number(endText)
    if (start < 1 || end < start || end > maxPage) throw new Error(`Page range must stay between 1 and ${maxPage}`)
    for (let page = start; page <= end; page += 1) pages.add(page)
  }
  return [...pages]
}

export function selectSplitPages(pageRange: string, rule: PdfSplitRule, customRule: string, maxPage: number): number[] {
  const candidates = parsePageSelection(pageRange, maxPage)
  if (rule === 'odd') return candidates.filter((page) => page % 2 === 1)
  if (rule === 'even') return candidates.filter((page) => page % 2 === 0)
  const selected = new Set(parsePageSelection(customRule, maxPage))
  return candidates.filter((page) => selected.has(page))
}
