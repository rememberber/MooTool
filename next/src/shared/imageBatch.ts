export type ImageBatchProgress = {
  total: number
  completed: number
  succeeded: number
  current: string
  failures: { name: string; message: string }[]
  cancelled: boolean
}

export async function runImageBatch(names: string[], process: (name: string, signal: AbortSignal) => Promise<void>, signal: AbortSignal, progress: (value: ImageBatchProgress) => void): Promise<ImageBatchProgress> {
  const state: ImageBatchProgress = { total: names.length, completed: 0, succeeded: 0, current: '', failures: [], cancelled: false }
  const publish = () => progress({ ...state, failures: [...state.failures] })
  for (const name of names) {
    if (signal.aborted) break
    state.current = name
    publish()
    try {
      await process(name, signal)
      state.succeeded++
    } catch (error) {
      if (signal.aborted) break
      state.failures.push({ name, message: error instanceof Error ? error.message : String(error) })
    }
    state.completed++
    publish()
    // Allow cancel input and painting between items, including very fast transforms.
    await new Promise(resolve => setTimeout(resolve, 0))
  }
  state.cancelled = signal.aborted
  state.current = ''
  publish()
  return state
}
