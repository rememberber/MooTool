import { spawn } from 'node:child_process'
import { access } from 'node:fs/promises'
import { constants } from 'node:fs'
import { ocrLanguages, ocrErrorCodes, type OcrLanguage, type OcrStatus, type OcrErrorCode } from '../../src/shared/contracts/ocr'

export function ocrFailure(code: OcrErrorCode): never { throw new Error(`image.ocr.error.${code}`) }
export interface OcrBackend {
  status(): Promise<OcrStatus>
  recognize(path: string, language: OcrLanguage, signal: AbortSignal): Promise<unknown>
}

export function runOcrHelper(executable: string, args: string[], input: unknown, signal: AbortSignal, timeoutMs = 120000): Promise<unknown> {
  if (signal.aborted) return Promise.reject(new Error('image.ocr.error.cancelled'))
  return new Promise((resolve, reject) => {
    const child = spawn(executable, args, { shell: false, stdio: ['pipe', 'pipe', 'ignore'], windowsHide: true })
    let bytes = 0
    let output = ''
    let errorCode: OcrErrorCode | null = null
    const stop = (code: OcrErrorCode) => { errorCode ??= code; child.kill('SIGKILL') }
    const abort = () => stop('cancelled')
    const timer = setTimeout(() => stop('timeout'), timeoutMs)
    signal.addEventListener('abort', abort, { once: true })
    const clean = () => { clearTimeout(timer); signal.removeEventListener('abort', abort) }
    child.stdout.setEncoding('utf8')
    child.stdout.on('data', (chunk: string) => {
      bytes += Buffer.byteLength(chunk)
      if (bytes > 2 * 1024 * 1024) stop('tooLarge')
      else output += chunk
    })
    child.stdin.on('error', () => { /* A killed helper may close stdin first. */ })
    child.on('error', () => { clean(); reject(new Error('image.ocr.error.unavailable')) })
    child.on('close', (code) => {
      clean()
      if (errorCode) { reject(new Error(`image.ocr.error.${errorCode}`)); return }
      try {
        const parsed = JSON.parse(output)
        if (parsed?.error && ocrErrorCodes.includes(parsed.error)) reject(new Error(`image.ocr.error.${parsed.error}`))
        else if (code !== 0) reject(new Error('image.ocr.error.failed'))
        else resolve(parsed)
      } catch { reject(new Error('image.ocr.error.failed')) }
    })
    child.stdin.end(input === null ? '' : JSON.stringify(input))
  })
}

export class VisionOcrBackend implements OcrBackend {
  private availability: Promise<OcrStatus> | null = null
  constructor(private readonly helper: string, private readonly platform = process.platform, private readonly macMajor = 13) {}
  status(): Promise<OcrStatus> {
    this.availability ??= this.loadStatus()
    return this.availability
  }
  private async loadStatus(): Promise<OcrStatus> {
    const unavailable = (reason: OcrStatus['reason']): OcrStatus => ({ available: false, engine: null, languages: [], reason })
    if (this.platform !== 'darwin') return unavailable('platform')
    if (this.macMajor < 13) return unavailable('system')
    try {
      await access(this.helper, constants.X_OK)
      const value = await runOcrHelper(this.helper, ['--status'], null, new AbortController().signal, 5000) as { languages?: unknown }
      if (!Array.isArray(value?.languages)) return unavailable('component')
      const supported = value.languages
      const languages = ocrLanguages.filter(language => language !== 'auto' && supported.includes(language))
      if (!languages.length) return unavailable('system')
      return { available: true, engine: 'apple-vision', languages: ['auto', ...languages], reason: null }
    } catch { return unavailable('component') }
  }
  recognize(path: string, language: OcrLanguage, signal: AbortSignal): Promise<unknown> {
    return runOcrHelper(this.helper, ['--recognize'], { path, language }, signal)
  }
}
