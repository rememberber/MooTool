import { mkdtemp, readdir, readFile, rm, stat } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { normalizeOcrOutput, normalizeOcrRequest, OcrService } from './ocrService'
import type { OcrBackend } from './ocrBackend'

const request = { requestId: 'sample-id', language: 'auto', source: { name: '截图.png', dataUrl: 'data:image/png;base64,YQ==' } }
const output = { width: 100, height: 20, lines: [{ text: '中文识别', confidence: 0.95, box: { x: 5, y: 3, width: 80, height: 14 } }] }
const decoder = () => ({ png: Buffer.from('snapshot bytes'), width: 100, height: 20 })
function backend(): OcrBackend { return { status: async () => ({ available: true, engine: 'apple-vision', languages: ['auto', 'zh-Hans'], reason: null }), recognize: async () => output } }

describe('OCR service', () => {
  let directory: string
  beforeEach(async () => { directory = await mkdtemp(join(tmpdir(), 'mootool-ocr-test-')) })
  afterEach(async () => { await rm(directory, { recursive: true, force: true }) })

  it('rejects invalid IDs, names, language and non-PNG protocols at the boundary', () => {
    for (const value of [null, { ...request, requestId: '../x' }, { ...request, language: 'shell' }, { ...request, source: { ...request.source, name: '/private/file.png' } }, { ...request, source: { ...request.source, dataUrl: 'file:///tmp/x' } }]) {
      expect(() => normalizeOcrRequest(value)).toThrow('image.ocr.error.input')
    }
    expect(normalizeOcrRequest({ ...request, command: 'extra' })).toEqual(request)
  })

  it('uses a private snapshot, returns text/line geometry and removes temporary data', async () => {
    const selected = backend()
    selected.recognize = async path => {
      expect(await readFile(path, 'utf8')).toBe('snapshot bytes')
      if (process.platform !== 'win32') expect((await stat(path)).mode & 0o777).toBe(0o600)
      expect(path).toContain('mootool-ocr-')
      return output
    }
    const service = new OcrService(selected, directory, decoder)
    const result = await service.recognize(1, request)
    expect(result).toMatchObject({ requestId: 'sample-id', sourceName: '截图.png', engine: 'apple-vision', language: 'auto', text: '中文识别', lines: output.lines, width: 100, height: 20 })
    expect(await readdir(directory)).toEqual([])
  })

  it('bounds global concurrency and permits only the owning request to cancel', async () => {
    const selected = backend()
    let entered!: () => void
    const ready = new Promise<void>(resolve => { entered = resolve })
    selected.recognize = (_path, _language, signal) => new Promise((_resolve, reject) => {
      signal.addEventListener('abort', () => reject(new Error('image.ocr.error.cancelled')), { once: true }); entered()
    })
    const service = new OcrService(selected, directory, decoder)
    const pending = service.recognize(10, request)
    const rejected = expect(pending).rejects.toThrow('image.ocr.error.cancelled')
    await ready
    await expect(service.recognize(20, { ...request, requestId: 'other' })).rejects.toThrow('image.ocr.error.busy')
    expect(await service.cancel(20, 'sample-id')).toBe(false)
    expect(await service.cancel(10, 'wrong')).toBe(false)
    expect(await service.cancel(10, 'sample-id')).toBe(true)
    await rejected
    expect(await readdir(directory)).toEqual([])
    selected.recognize = async () => output
    expect((await service.recognize(20, request)).text).toBe('中文识别')
  })

  it('cancels owner teardown before native recognition starts', async () => {
    let resolveStatus!: (value: Awaited<ReturnType<OcrBackend['status']>>) => void
    const selected = backend()
    selected.status = () => new Promise(resolve => { resolveStatus = resolve })
    selected.recognize = vi.fn(async () => output)
    const service = new OcrService(selected, directory, decoder)
    const pending = service.recognize(1, request)
    const rejected = expect(pending).rejects.toThrow('image.ocr.error.cancelled')
    service.release(2)
    service.release(1)
    resolveStatus({ available: true, engine: 'apple-vision', languages: ['auto'], reason: null })
    await rejected
    expect(selected.recognize).not.toHaveBeenCalled()
    expect(await readdir(directory)).toEqual([])
  })

  it('rejects unavailable languages and oversized images before writing or invoking the helper', async () => {
    const selected = backend()
    const decode = vi.fn(decoder)
    const service = new OcrService(selected, directory, decode)
    await expect(service.recognize(1, { ...request, language: 'ja-JP' })).rejects.toThrow('image.ocr.error.language')
    expect(decode).not.toHaveBeenCalled()
    const oversized = new OcrService(selected, directory, () => ({ png: Buffer.from('x'), width: 8000, height: 8000 }))
    await expect(oversized.recognize(1, request)).rejects.toThrow('image.ocr.error.tooLarge')
    expect(await readdir(directory)).toEqual([])
  })

  it('cleans up after helper failures and validates its output geometry and limits', async () => {
    const selected = backend()
    selected.recognize = async () => { throw new Error('image.ocr.error.failed') }
    await expect(new OcrService(selected, directory, decoder).recognize(1, request)).rejects.toThrow('image.ocr.error.failed')
    expect(await readdir(directory)).toEqual([])
    for (const value of [{ ...output, width: 99 }, { ...output, lines: [{ ...output.lines[0], confidence: NaN }] }, { ...output, lines: [{ ...output.lines[0], box: { x: 80, y: 0, width: 50, height: 20 } }] }]) {
      expect(() => normalizeOcrOutput(value, 100, 20)).toThrow('image.ocr.error.failed')
    }
    expect(() => normalizeOcrOutput({ ...output, lines: Array(2001).fill(output.lines[0]) }, 100, 20)).toThrow('image.ocr.error.tooLarge')
    expect(() => normalizeOcrOutput({ ...output, lines: [{ ...output.lines[0], text: 'x'.repeat(100001) }] }, 100, 20)).toThrow('image.ocr.error.tooLarge')
  })
})
