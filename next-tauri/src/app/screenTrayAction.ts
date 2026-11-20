export type ScreenTrayAction = 'color' | 'capture'

export interface ScreenActionRequest {
  kind: ScreenTrayAction
  sequence: number
}

let sequence = 0
let pending: ScreenActionRequest | null = null
const claimed = new Set<number>()
const listeners = new Set<(request: ScreenActionRequest) => void>()

export function screenTrayAction(value: unknown): ScreenTrayAction | null {
  return value === 'color' || value === 'capture' ? value : null
}

export function publishScreenAction(kind: ScreenTrayAction): ScreenActionRequest {
  const request = { kind, sequence: ++sequence }
  pending = request
  for (const listener of listeners) listener(request)
  return request
}

export function subscribeScreenAction(listener: (request: ScreenActionRequest) => void): () => void {
  listeners.add(listener)
  if (pending) listener(pending)
  return () => listeners.delete(listener)
}

export function claimScreenAction(request: ScreenActionRequest): boolean {
  if (claimed.has(request.sequence)) return false
  claimed.add(request.sequence)
  if (pending?.sequence === request.sequence) pending = null
  return true
}
