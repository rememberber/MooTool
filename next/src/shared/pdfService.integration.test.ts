import { mkdtempSync, rmSync } from 'node:fs'
import { readFile, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { PDFDocument } from 'pdf-lib'
import { afterEach, describe, expect, it } from 'vitest'
import { inspectPdf, mergePdfs, splitPdfs } from '../../electron/main/pdfService'

const tempDirectories: string[] = []

afterEach(() => {
  for (const directory of tempDirectories.splice(0)) {
    rmSync(directory, { recursive: true, force: true })
  }
})

async function createPdf(name: string, pageCount: number): Promise<string> {
  const directory = tempDirectories[0] ?? mkdtempSync(join(tmpdir(), 'mootool-pdf-'))
  if (tempDirectories.length === 0) tempDirectories.push(directory)
  const document = await PDFDocument.create()
  for (let index = 0; index < pageCount; index += 1) {
    document.addPage([320 + index, 480 + index])
  }
  const path = join(directory, name)
  await writeFile(path, await document.save())
  return path
}

async function pageCount(path: string): Promise<number> {
  return (await PDFDocument.load(await readFile(path))).getPageCount()
}

describe('PDF service', () => {
  it('inspects and merges selected page ranges in source order', async () => {
    const first = await createPdf('First.pdf', 4)
    const second = await createPdf('Second.pdf', 3)
    const output = join(tempDirectories[0], 'merge.pdf')

    expect(await inspectPdf(first)).toMatchObject({ name: 'First.pdf', pageCount: 4 })
    const result = await mergePdfs([
      { path: first, pages: '2-3' },
      { path: second, pages: '1,3' }
    ], output)

    expect(result).toEqual({ outputs: [output], pageCount: 4 })
    expect(await pageCount(output)).toBe(4)
  })

  it('splits odd pages and uses the Java-compatible output suffix', async () => {
    const source = await createPdf('Quarterly.PDF', 6)
    const result = await splitPdfs([{ path: source, pageRange: '1-6', rule: 'odd', customRule: '' }])

    expect(result.pageCount).toBe(3)
    expect(result.outputs[0]).toBe(join(tempDirectories[0], 'quarterly_split.pdf'))
    expect(await pageCount(result.outputs[0])).toBe(3)
  })

  it('rejects unsupported files and empty merge selections', async () => {
    await expect(inspectPdf('/tmp/not-a-pdf.txt')).rejects.toThrow('Only PDF files')
    await expect(mergePdfs([], '/tmp/unused.pdf')).rejects.toThrow('at least two')
  })
})
