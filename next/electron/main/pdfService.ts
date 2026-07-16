import { readFile, writeFile } from 'node:fs/promises'
import { basename, dirname, extname, join, parse } from 'node:path'
import { PDFDocument } from 'pdf-lib'
import type { PdfFileInfo, PdfMergeSource, PdfOperationResult, PdfSplitTask } from '../../src/shared/contracts/pdf'
import { parsePageSelection, selectSplitPages } from '../../src/shared/utils/pageRanges'

export async function inspectPdf(path: string): Promise<PdfFileInfo> {
  ensurePdfPath(path)
  const bytes = await readFile(path)
  const document = await PDFDocument.load(bytes)
  return { path, name: basename(path), size: bytes.length, pageCount: document.getPageCount() }
}

export async function mergePdfs(sources: PdfMergeSource[], outputPath: string): Promise<PdfOperationResult> {
  if (sources.length < 2) throw new Error('Select at least two PDF files')
  const output = await PDFDocument.create()
  let pageCount = 0
  for (const source of sources) {
    ensurePdfPath(source.path)
    const document = await PDFDocument.load(await readFile(source.path))
    const pages = parsePageSelection(source.pages, document.getPageCount())
    const copied = await output.copyPages(document, pages.map((page) => page - 1))
    copied.forEach((page) => output.addPage(page))
    pageCount += copied.length
  }
  if (pageCount === 0) throw new Error('No pages selected')
  await writeFile(outputPath, await output.save())
  return { outputs: [outputPath], pageCount }
}

export async function splitPdfs(tasks: PdfSplitTask[]): Promise<PdfOperationResult> {
  if (tasks.length === 0) throw new Error('Select at least one PDF task')
  const outputs: string[] = []
  let pageCount = 0
  for (const task of tasks) {
    ensurePdfPath(task.path)
    const source = await PDFDocument.load(await readFile(task.path))
    const pages = selectSplitPages(task.pageRange, task.rule, task.customRule, source.getPageCount())
    if (pages.length === 0) throw new Error(`No pages selected for ${basename(task.path)}`)
    const output = await PDFDocument.create()
    const copied = await output.copyPages(source, pages.map((page) => page - 1))
    copied.forEach((page) => output.addPage(page))
    const parsed = parse(task.path)
    const outputPath = join(dirname(task.path), `${parsed.name.toLowerCase()}_split.pdf`)
    await writeFile(outputPath, await output.save())
    outputs.push(outputPath)
    pageCount += copied.length
  }
  return { outputs, pageCount }
}

function ensurePdfPath(path: string): void {
  if (extname(path).toLowerCase() !== '.pdf') throw new Error('Only PDF files are supported')
}
