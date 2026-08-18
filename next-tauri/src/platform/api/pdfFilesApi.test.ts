import { describe, expect, it, vi } from 'vitest'
import { createPdfFilesApi } from './pdfFilesApi'

describe('PDF files API adapter', () => {
  it('streams generated PDFs through an owned export session', async () => {
    const length = 600_000
    const invoke = vi.fn()
      .mockResolvedValueOnce({ sessionId: 'pdf-1', targetPaths: ['/tmp/output.pdf'] })
      .mockResolvedValueOnce(512 * 1024)
      .mockResolvedValueOnce(length)
      .mockResolvedValueOnce(['/tmp/output.pdf'])
    const api = createPdfFilesApi(invoke)
    const session = await api.begin(['output.pdf'])
    expect(session).not.toBeNull()

    const progress = vi.fn()
    await session!.write(0, new Uint8Array(length), progress)
    await expect(session!.finish()).resolves.toEqual(['/tmp/output.pdf'])

    expect(invoke.mock.calls.map(([command]) => command)).toEqual([
      'begin_pdf_export',
      'write_pdf_export_chunk',
      'write_pdf_export_chunk',
      'finish_pdf_export'
    ])
    expect(invoke.mock.calls[1]![1].chunk).toHaveLength(512 * 1024)
    expect(invoke.mock.calls[2]![1].chunk).toHaveLength(length - 512 * 1024)
    expect(progress.mock.calls).toEqual([[512 * 1024], [length]])
  })

  it('cancels an unfinished native session and preserves dialog cancellation', async () => {
    const invoke = vi.fn()
      .mockResolvedValueOnce({ sessionId: 'pdf-2', targetPaths: ['/tmp/output.pdf'] })
      .mockResolvedValueOnce(true)
      .mockResolvedValueOnce(null)
    const api = createPdfFilesApi(invoke)
    const session = await api.begin(['output.pdf'])
    await session!.cancel()
    await expect(api.begin(['cancelled.pdf'])).resolves.toBeNull()
    expect(invoke.mock.calls).toEqual([
      ['begin_pdf_export', { names: ['output.pdf'] }],
      ['cancel_pdf_export', { sessionId: 'pdf-2' }],
      ['begin_pdf_export', { names: ['cancelled.pdf'] }]
    ])
  })
})
