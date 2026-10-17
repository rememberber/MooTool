import type { TextTask, TextTaskReply, TextTaskResult } from './textTask'

/** A task owns its worker, so cancelling also stops CPU-bound synchronous code. */
export function runTextTask<T extends TextTask>(task: T, signal: AbortSignal): Promise<TextTaskResult<T>> {
  signal.throwIfAborted()
  return new Promise((resolve, reject) => {
    const worker = new Worker(new URL('./textWorker.ts', import.meta.url), { type: 'module' })
    let finished = false
    const finish = (error?: unknown, result?: TextTaskResult<T>) => {
      if (finished) return
      finished = true
      clearTimeout(timeout)
      signal.removeEventListener('abort', abort)
      worker.terminate()
      if (error) reject(error)
      else resolve(result!)
    }
    const abort = () => finish(new DOMException('Cancelled', 'AbortError'))
    const timeout = setTimeout(() => finish(new Error('Calculation timed out')), 60_000)
    signal.addEventListener('abort', abort, { once: true })
    worker.onmessage = ({ data }: MessageEvent<TextTaskReply>) => {
      if (signal.aborted) { abort(); return }
      data.ok ? finish(undefined, data.result as TextTaskResult<T>) : finish(new Error(data.error))
    }
    worker.onerror = event => { event.preventDefault(); finish(new Error(event.message || 'Calculation worker failed')) }
    worker.onmessageerror = () => finish(new Error('Unable to read calculation result'))
    try { worker.postMessage(task) } catch (error) { finish(error) }
    if (signal.aborted) abort()
  })
}

export function isTaskCancelled(error: unknown): boolean {
  return error instanceof Error && error.name === 'AbortError'
}
