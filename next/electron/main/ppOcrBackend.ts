import type { UtilityProcess } from 'electron'
import { ocrErrorCodes, type OcrLanguage, type OcrStatus } from '../../src/shared/contracts/ocr'
import { ocrFailure, type OcrBackend } from './ocrBackend'
import { ppOcrPlatforms } from './ocrResources'

export type ForkOcrProcess = (path: string) => Pick<UtilityProcess, 'on' | 'once' | 'removeListener' | 'postMessage' | 'kill'>
export function runPpOcrWorker(fork: ForkOcrProcess, worker: string, input: unknown, signal: AbortSignal, timeout = 120000): Promise<unknown> {
  if (signal.aborted) return Promise.reject(new Error('image.ocr.error.cancelled'))
  return new Promise((resolve, reject) => {
    let child: ReturnType<ForkOcrProcess>
    try { child = fork(worker) } catch { reject(new Error('image.ocr.error.unavailable')); return }
    let response: any, error: string | null = null
    const stop = (code: string) => { error ??= code; child.kill() }
    const abort = () => stop('cancelled')
    const timer = setTimeout(() => stop('timeout'), timeout)
    signal.addEventListener('abort', abort, { once: true })
    child.once('spawn', () => {
      if (error || signal.aborted) { error ??= 'cancelled'; child.kill() }
      else { try { child.postMessage(input) } catch { stop('failed') } }
    })
    child.on('message', (value: unknown) => {
      try { if (JSON.stringify(value).length > 2 * 1024 * 1024) { stop('tooLarge'); return } }
      catch { stop('failed'); return }
      response = value; child.kill()
    })
    child.once('exit', () => {
      clearTimeout(timer); signal.removeEventListener('abort', abort)
      if (error) { reject(new Error(`image.ocr.error.${error}`)); return }
      if (response?.error && ocrErrorCodes.includes(response.error)) { reject(new Error(`image.ocr.error.${response.error}`)); return }
      if (response?.ready === true) resolve(response)
      else if (response?.result) resolve(response.result)
      else reject(new Error('image.ocr.error.failed'))
    })
  })
}
export class PpOcrBackend implements OcrBackend {
  private availability: Promise<OcrStatus> | null = null
  constructor(private readonly resources: string, private readonly worker: string, private readonly fork: ForkOcrProcess, private readonly platform = process.platform, private readonly arch = process.arch) {}
  status() { this.availability ??= this.loadStatus(); return this.availability }
  private async loadStatus(): Promise<OcrStatus> {
    const unavailable = (reason: OcrStatus['reason']): OcrStatus => ({ available: false, engine: null, languages: [], reason })
    if (!ppOcrPlatforms.some(target => target === `${this.platform}-${this.arch}`)) return unavailable('platform')
    try {
      await runPpOcrWorker(this.fork, this.worker, { operation: 'status', resources: this.resources }, new AbortController().signal, 30000)
      return { available: true, engine: 'pp-ocrv6-small', languages: ['auto'], reason: null }
    } catch { return unavailable('component') }
  }
  recognize(path: string, language: OcrLanguage, signal: AbortSignal) {
    if (language !== 'auto') ocrFailure('language')
    return runPpOcrWorker(this.fork, this.worker, { operation: 'recognize', resources: this.resources, image: path, language }, signal)
  }
}
