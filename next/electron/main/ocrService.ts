import { mkdtemp, rm, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { ocrLanguages, type OcrLine, type OcrRequest, type OcrResult } from '../../src/shared/contracts/ocr'
import { ocrFailure, type OcrBackend } from './ocrBackend'

type ImageSnapshot = { png: Buffer; width: number; height: number }
type Task = { owner: number; requestId: string; controller: AbortController; finished: Promise<void>; finish: () => void }
const record = (value: unknown): value is Record<string, unknown> => typeof value === 'object' && value !== null && !Array.isArray(value)

export function normalizeOcrRequest(value: unknown): OcrRequest {
  if (!record(value) || typeof value.requestId !== 'string' || !/^[\w-]{1,64}$/.test(value.requestId)
    || !ocrLanguages.includes(value.language as OcrRequest['language']) || !record(value.source)
    || typeof value.source.name !== 'string' || !value.source.name.trim() || value.source.name.length > 160
    || /[\\/\u0000-\u001f]/.test(value.source.name) || typeof value.source.dataUrl !== 'string') ocrFailure('input')
  if (value.source.dataUrl.length > 48 * 1024 * 1024) ocrFailure('tooLarge')
  if (!/^data:image\/png;base64,[A-Za-z0-9+/=]+$/.test(value.source.dataUrl)) ocrFailure('input')
  return { requestId: value.requestId, language: value.language as OcrRequest['language'], source: { name: value.source.name, dataUrl: value.source.dataUrl } }
}

export function normalizeOcrOutput(value: unknown, width: number, height: number): OcrLine[] {
  if (!record(value) || value.width !== width || value.height !== height || !Array.isArray(value.lines)) ocrFailure('failed')
  if (value.lines.length > 2000) ocrFailure('tooLarge')
  let characters = 0
  return value.lines.map((line: unknown) => {
    if (!record(line) || typeof line.text !== 'string' || typeof line.confidence !== 'number' || !Number.isFinite(line.confidence)
      || line.confidence < 0 || line.confidence > 1 || !record(line.box)) ocrFailure('failed')
    const box = line.box
    if (!['x', 'y', 'width', 'height'].every(key => typeof box[key] === 'number' && Number.isFinite(box[key]) && box[key] >= -0.01)
      || (box.x as number) + (box.width as number) > width + 0.01 || (box.y as number) + (box.height as number) > height + 0.01) ocrFailure('failed')
    characters += line.text.length + 1
    if (characters > 100000) ocrFailure('tooLarge')
    return { text: line.text, confidence: line.confidence, box: {
      x: Math.max(0, box.x as number), y: Math.max(0, box.y as number),
      width: Math.max(0, box.width as number), height: Math.max(0, box.height as number)
    } }
  })
}

export class OcrService {
  private task: Task | null = null
  constructor(private readonly backend: OcrBackend, private readonly temporaryRoot: string, private readonly decodeImage: (dataUrl: string) => ImageSnapshot) {}
  status() { return this.backend.status() }

  async recognize(owner: number, input: unknown): Promise<OcrResult> {
    const request = normalizeOcrRequest(input)
    if (this.task) ocrFailure('busy')
    let finish!: () => void
    const task: Task = { owner, requestId: request.requestId, controller: new AbortController(), finished: new Promise(resolve => { finish = resolve }), finish: () => finish() }
    this.task = task
    let directory: string | undefined
    const started = performance.now()
    const checkCancelled = () => { if (task.controller.signal.aborted) ocrFailure('cancelled') }
    try {
      const status = await this.backend.status()
      checkCancelled()
      if (!status.available || !status.engine) ocrFailure('unavailable')
      if (!status.languages.includes(request.language)) ocrFailure('language')
      let image: ImageSnapshot
      try { image = this.decodeImage(request.source.dataUrl) }
      catch (error) { if (error instanceof Error && error.message === 'image.ocr.error.tooLarge') throw error; ocrFailure('input') }
      if (![image.width, image.height].every(size => Number.isInteger(size) && size > 0)) ocrFailure('input')
      if (image.width > 16384 || image.height > 16384 || image.width * image.height > 32000000 || image.png.length > 32 * 1024 * 1024) ocrFailure('tooLarge')
      directory = await mkdtemp(join(this.temporaryRoot, 'mootool-ocr-'))
      checkCancelled()
      const path = join(directory, 'snapshot.png')
      await writeFile(path, image.png, { flag: 'wx', mode: 0o600 })
      checkCancelled()
      const output = await this.backend.recognize(path, request.language, task.controller.signal)
      checkCancelled()
      const lines = normalizeOcrOutput(output, image.width, image.height)
      return { requestId: request.requestId, sourceName: request.source.name, engine: status.engine, language: request.language,
        width: image.width, height: image.height, text: lines.map(line => line.text).join('\n'), lines, durationMs: Math.round(performance.now() - started) }
    } finally {
      try { if (directory) await rm(directory, { recursive: true, force: true }) }
      finally { if (this.task === task) this.task = null; task.finish() }
    }
  }

  async cancel(owner: number, requestId: unknown): Promise<boolean> {
    const task = this.task
    if (!task || task.owner !== owner || requestId !== task.requestId) return false
    task.controller.abort()
    await task.finished
    return true
  }
  release(owner: number): void { if (this.task?.owner === owner) this.task.controller.abort() }
  stop(): void { this.task?.controller.abort() }
}
