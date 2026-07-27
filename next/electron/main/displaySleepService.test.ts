import { describe, expect, it, vi } from 'vitest'
import { DisplaySleepService, type PowerSaveBlockerAdapter } from './displaySleepService'

function setup() {
  let nextId = 1
  const active = new Set<number>()
  const adapter: PowerSaveBlockerAdapter = {
    start: vi.fn(() => {
      const id = nextId++
      active.add(id)
      return id
    }),
    stop: vi.fn((id) => { active.delete(id) }),
    isStarted: vi.fn((id) => active.has(id))
  }
  return { active, adapter, service: new DisplaySleepService(adapter) }
}

describe('DisplaySleepService', () => {
  it('keeps one blocker active while any requester needs the display', () => {
    const { active, adapter, service } = setup()

    expect(service.set(11, true)).toBe(true)
    expect(service.set(22, true)).toBe(true)
    expect(adapter.start).toHaveBeenCalledTimes(1)
    expect(active.size).toBe(1)

    expect(service.set(11, false)).toBe(true)
    expect(active.size).toBe(1)
    expect(service.set(22, false)).toBe(false)
    expect(active.size).toBe(0)
    expect(adapter.stop).toHaveBeenCalledTimes(1)
  })

  it('releases the blocker on dispose', () => {
    const { active, service } = setup()
    service.set(11, true)
    service.dispose()
    expect(active.size).toBe(0)
  })
})
