import { afterEach, describe, expect, it, vi } from 'vitest'
import { runTextTask } from './runTextTask'

class MockWorker {
  static instances: MockWorker[] = []
  constructor() { MockWorker.instances.push(this) }
  postMessage = vi.fn()
  terminate = vi.fn()
  onmessage?: (event: { data: unknown }) => void
  onerror?: (event: { message: string; preventDefault: () => void }) => void
}
const task = { kind: 'diff' as const, left: 'a', right: 'b', ignoreWhitespace: false }
afterEach(() => { MockWorker.instances = []; vi.unstubAllGlobals(); vi.useRealTimers() })

function fixture() {
  vi.stubGlobal('Worker', MockWorker)
  const controller = new AbortController()
  const promise = runTextTask(task, controller.signal)
  return { controller, promise, worker: MockWorker.instances.at(-1)! }
}

describe('cancellable text worker tasks', () => {
  it('returns the result and releases the worker', async () => {
    const { promise, worker } = fixture()
    worker.onmessage!({ data: { ok: true, result: { changed: 1 } } })
    await expect(promise).resolves.toEqual({ changed: 1 })
    expect(worker.terminate).toHaveBeenCalledOnce()
  })
  it('terminates running synchronous calculations and ignores late replies', async () => {
    const { controller, promise, worker } = fixture()
    controller.abort()
    worker.onmessage!({ data: { ok: true, result: 'stale' } })
    await expect(promise).rejects.toMatchObject({ name: 'AbortError' })
    expect(worker.terminate).toHaveBeenCalledOnce()
  })
  it('does not start an already cancelled task', () => {
    vi.stubGlobal('Worker', MockWorker)
    expect(() => runTextTask(task, AbortSignal.abort())).toThrow()
    expect(MockWorker.instances).toHaveLength(0)
  })
  it('surfaces worker errors and enforces a time limit', async () => {
    vi.useFakeTimers()
    const { promise, worker } = fixture()
    const result = expect(promise).rejects.toThrow('timed out')
    await vi.advanceTimersByTimeAsync(60_000)
    await result
    expect(worker.terminate).toHaveBeenCalledOnce()
    const next = fixture()
    next.worker.onmessage!({ data: { ok: false, error: 'Invalid JSON' } })
    await expect(next.promise).rejects.toThrow('Invalid JSON')
  })
})
