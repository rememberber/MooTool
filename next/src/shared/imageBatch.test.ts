import { describe, expect, it } from 'vitest'
import { runImageBatch, type ImageBatchProgress } from './imageBatch'

describe('image batch', () => {
  it('continues after a failure and reports actual successes', async () => {
    const visited: string[] = []
    const progress: ImageBatchProgress[] = []
    const result = await runImageBatch(['a', 'bad', 'c'], async name => { visited.push(name); if (name === 'bad') throw new Error('decode failed') }, new AbortController().signal, value => progress.push(value))
    expect(visited).toEqual(['a', 'bad', 'c'])
    expect(result).toMatchObject({ completed: 3, succeeded: 2, cancelled: false, failures: [{ name: 'bad', message: 'decode failed' }] })
    expect(progress.map(item => item.completed)).toContain(1)
  })
  it('stops before the next item and retains completed results', async () => {
    const controller = new AbortController()
    const visited: string[] = []
    const result = await runImageBatch(['a', 'b'], async name => { visited.push(name); controller.abort() }, controller.signal, () => {})
    expect(visited).toEqual(['a'])
    expect(result).toMatchObject({ succeeded: 1, completed: 1, cancelled: true, failures: [] })
  })
  it('does not count an interrupted conversion as a failure or a success', async () => {
    const controller = new AbortController()
    const result = await runImageBatch(['a', 'b'], async () => { controller.abort(); controller.signal.throwIfAborted() }, controller.signal, () => {})
    expect(result).toMatchObject({ succeeded: 0, completed: 0, cancelled: true, failures: [] })
  })
})
