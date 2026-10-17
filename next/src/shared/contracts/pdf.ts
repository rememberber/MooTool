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

export type PdfProgress = {
  jobId: string
  phase: 'reading' | 'processing' | 'saving'
  completed: number
  total: number
  name: string
}

export const pdfCancellationCode = 'PDF_OPERATION_CANCELLED'
export function isPdfCancellation(error: unknown): boolean {
  return error instanceof Error && error.name === 'AbortError'
    || String(error instanceof Error ? error.message : error).includes(pdfCancellationCode)
}
