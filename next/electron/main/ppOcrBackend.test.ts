import { EventEmitter } from 'node:events'
import { describe, expect, it } from 'vitest'
import { PpOcrBackend, runPpOcrWorker, type ForkOcrProcess } from './ppOcrBackend'

class Child extends EventEmitter {
  killed = false
  input: any
  postMessage(value: unknown) { this.input = value }
  kill() { if (this.killed) return false; this.killed = true; setImmediate(() => this.emit('exit', 0)); return true }
}
function setup() { const child = new Child(); const fork = (() => { setImmediate(() => child.emit('spawn')); return child }) as unknown as ForkOcrProcess; return { child, fork } }
describe('isolated PP-OCR process', () => {
  it('waits for process exit after receiving a result before releasing resources', async () => {
    const { child, fork } = setup()
    const pending = runPpOcrWorker(fork, 'worker.js', { operation: 'recognize' }, new AbortController().signal)
    await new Promise(resolve => setImmediate(resolve))
    expect(child.input).toEqual({ operation: 'recognize' })
    child.emit('message', { result: { width: 100, height: 30, lines: [] } })
    expect(child.killed).toBe(true)
    expect(await pending).toEqual({ width: 100, height: 30, lines: [] })
  })
  it('kills on cancellation and timeout, including cancellation before spawn', async () => {
    const { child, fork } = setup(), controller = new AbortController()
    const pending = runPpOcrWorker(fork, 'worker.js', {}, controller.signal)
    controller.abort()
    await expect(pending).rejects.toThrow('image.ocr.error.cancelled'); expect(child.killed).toBe(true)
    const delayed = setup()
    await expect(runPpOcrWorker(delayed.fork, 'worker.js', {}, new AbortController().signal, 20)).rejects.toThrow('image.ocr.error.timeout')
    expect(delayed.child.killed).toBe(true)
  })
  it('bounds responses and reports crashes and sanitized native failures', async () => {
    for (const value of [{ result: 'x'.repeat(3 * 1024 * 1024) }, { error: 'tooComplex' }, { error: '/private/native/details' }]) {
      const { child, fork } = setup()
      const pending = runPpOcrWorker(fork, 'worker.js', {}, new AbortController().signal)
      child.emit('message', value)
      await expect(pending).rejects.toThrow(value.error === 'tooComplex' ? 'image.ocr.error.tooComplex' : value.result ? 'image.ocr.error.tooLarge' : 'image.ocr.error.failed')
    }
    const crash = setup(), pending = runPpOcrWorker(crash.fork, 'worker.js', {}, new AbortController().signal)
    crash.child.emit('exit', 1)
    await expect(pending).rejects.toThrow('image.ocr.error.failed')
    const closed = setup()
    closed.child.postMessage = () => { throw new Error('Transport closed') }
    await expect(runPpOcrWorker(closed.fork, 'worker.js', {}, new AbortController().signal)).rejects.toThrow('image.ocr.error.failed')
    expect(closed.child.killed).toBe(true)
  })
  it('checks runtime availability and only advertises the unified multilingual model', async () => {
    const { child, fork } = setup()
    const backend = new PpOcrBackend('resources', 'worker.js', fork, 'win32', 'x64')
    const pending = backend.status()
    await new Promise(resolve => setImmediate(resolve)); child.emit('message', { ready: true })
    expect(await pending).toEqual({ available: true, engine: 'pp-ocrv6-small', languages: ['auto'], reason: null })
    await expect(backend.status()).resolves.toMatchObject({ available: true })
    expect(await new PpOcrBackend('resources', 'worker.js', fork, 'darwin', 'x64').status()).toMatchObject({ available: false, reason: 'platform' })
  })
})
