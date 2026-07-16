export type PdfFileInfo = {
  path: string
  name: string
  size: number
  pageCount: number
}

export type PdfMergeSource = {
  path: string
  pages: string
}

export type PdfSplitRule = 'odd' | 'even' | 'custom'

export type PdfSplitTask = {
  path: string
  pageRange: string
  rule: PdfSplitRule
  customRule: string
}

export type PdfOperationResult = {
  outputs: string[]
  pageCount: number
}
