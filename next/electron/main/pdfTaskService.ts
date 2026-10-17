import { Worker } from 'node:worker_threads'
import { randomUUID } from 'node:crypto'
import { copyFile, mkdtemp, rename, rm } from 'node:fs/promises'
import { join, dirname } from 'node:path'
import { tmpdir } from 'node:os'
import { pdfCancellationCode } from '../../src/shared/contracts/pdf'
import type { PdfFileInfo, PdfMergeSource, PdfOperationResult, PdfProgress, PdfSplitTask } from '../../src/shared/contracts/pdf'

export type PdfWorkerTask =
  | { kind: 'inspect'; paths: string[] }
  | { kind: 'merge'; sources: PdfMergeSource[]; outputPath: string }
  | { kind: 'split'; tasks: PdfSplitTask[] }
type PdfResult<T extends PdfWorkerTask> = T extends { kind: 'inspect' } ? PdfFileInfo[] : PdfOperationResult
type PdfWorkerMessage =
  | { type: 'progress'; progress: Omit<PdfProgress, 'jobId'> }
  | { type: 'error'; error: string }
  | { type: 'done'; result: PdfFileInfo[] | PdfOperationResult; staged: { path: string; stagedPath: string }[] }

export class PdfJob {
  private readonly controller = new AbortController()
  private committing = false
  constructor(readonly id: string, private readonly workerPath: string, private readonly progress: (value: PdfProgress) => void) {}

  cancel(): boolean {
    if (this.committing) return false
    this.controller.abort()
    return true
  }

  async execute<T extends PdfWorkerTask>(task: T): Promise<PdfResult<T>> {
    const { signal } = this.controller
    if (signal.aborted) throw new DOMException(pdfCancellationCode, 'AbortError')
    const directory = await mkdtemp(join(tmpdir(), 'mootool-pdf-'))
    let worker: Worker | undefined
    const adjacentFiles: { path: string; temporary: string }[] = []
    try {
      if (signal.aborted) throw new DOMException(pdfCancellationCode, 'AbortError')
      worker = new Worker(this.workerPath, { workerData: { task, directory } })
      const activeWorker = worker
      const message = await new Promise<Extract<PdfWorkerMessage, { type: 'done' }>>((resolve, reject) => {
        const abort = () => reject(new DOMException(pdfCancellationCode, 'AbortError'))
        signal.addEventListener('abort', abort, { once: true })
        const cleanup = () => signal.removeEventListener('abort', abort)
        activeWorker.on('message', (message: PdfWorkerMessage) => {
          if (message.type === 'progress') this.progress({ ...message.progress, jobId: this.id })
          else { cleanup(); message.type === 'error' ? reject(new Error(message.error)) : resolve(message) }
        })
        activeWorker.once('error', error => { cleanup(); reject(error) })
        activeWorker.once('exit', code => { cleanup(); reject(new Error(`PDF worker exited (${code})`)) })
        if (signal.aborted) { cleanup(); abort() }
      })
      if (signal.aborted) throw new DOMException(pdfCancellationCode, 'AbortError')
      // Workers write only to staging. Cancellation never interrupts a destination write.
      for (const { path, stagedPath } of message.staged) {
        const temporary = join(dirname(path), `.mootool-pdf-${randomUUID()}.tmp`)
        adjacentFiles.push({ path, temporary })
        await copyFile(stagedPath, temporary)
        if (signal.aborted) throw new DOMException(pdfCancellationCode, 'AbortError')
      }
      this.committing = true
      this.progress({ jobId: this.id, phase: 'saving', completed: 0, total: adjacentFiles.length, name: '' })
      for (const [index, { path, temporary }] of adjacentFiles.entries()) {
        await rename(temporary, path)
        this.progress({ jobId: this.id, phase: 'saving', completed: index + 1, total: adjacentFiles.length, name: path })
      }
      return message.result as PdfResult<T>
    } finally {
      await worker?.terminate()
      await Promise.all(adjacentFiles.map(({ temporary }) => rm(temporary, { force: true }).catch(() => undefined)))
      await rm(directory, { recursive: true, force: true })
    }
  }
}

export class PdfTaskService {
  private readonly jobs = new Map<number, PdfJob>()
  constructor(private readonly workerPath = join(__dirname, 'pdfWorker.js')) {}

  async run<T>(owner: number, id: string, progress: (value: PdfProgress) => void, operation: (job: PdfJob) => Promise<T>): Promise<T> {
    if (this.jobs.has(owner)) throw new Error('A PDF operation is already running')
    const job = new PdfJob(id, this.workerPath, progress)
    this.jobs.set(owner, job)
    try { return await operation(job) }
    finally { if (this.jobs.get(owner) === job) this.jobs.delete(owner) }
  }

  cancel(owner: number, id?: string): boolean {
    const job = this.jobs.get(owner)
    return Boolean(job && (!id || job.id === id) && job.cancel())
  }

  dispose(): void { for (const job of this.jobs.values()) job.cancel() }
}
