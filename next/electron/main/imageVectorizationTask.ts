import { Worker } from 'node:worker_threads'
import { join } from 'node:path'
import type { ImageVectorizeOptions } from '../../src/shared/contracts/images'

export function vectorizeInWorker(png: Uint8Array, options: ImageVectorizeOptions, signal: AbortSignal): Promise<string> {
  signal.throwIfAborted()
  return new Promise((resolve, reject) => {
    const worker = new Worker(join(__dirname, 'imageVectorizationWorker.js'), { workerData: { png, options } })
    const finish = (error?: Error, svg?: string) => {
      signal.removeEventListener('abort', abort)
      worker.removeAllListeners()
      void worker.terminate()
      error ? reject(error) : resolve(svg!)
    }
    const abort = () => finish(new Error('Cancelled'))
    signal.addEventListener('abort', abort, { once: true })
    worker.once('message', (message: { svg?: string; error?: string }) => finish(message.error ? new Error(message.error) : undefined, message.svg))
    worker.once('error', error => finish(error instanceof Error ? error : new Error(String(error))))
    worker.once('exit', code => finish(new Error(`Vectorizer exited (${code})`)))
    if (signal.aborted) abort()
  })
}
