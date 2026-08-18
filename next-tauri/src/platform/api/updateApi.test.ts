import { describe, expect, it, vi } from 'vitest'
import type { ProductUpdateEvent } from '../contracts/update'
import { createProductUpdateApi } from './updateApi'

describe('product update API', () => {
  it('uses only the product-owned update commands', async () => {
    const calls: Array<[string, Record<string, unknown> | undefined]> = []
    const invoke = async <T>(command: string, args?: Record<string, unknown>): Promise<T> => {
      calls.push([command, args])
      if (command === 'check_for_product_update') {
        return {
          status: 'upToDate',
          currentVersion: '1.0.0',
          latestVersion: null,
          releaseNotes: null,
          publishedAt: null,
          releaseUrl: null
        } as T
      }
      return (command === 'cancel_product_update' ? true : undefined) as T
    }
    const channel = { onmessage: null as ((event: ProductUpdateEvent) => void) | null }
    const api = createProductUpdateApi(invoke, () => channel)
    const listener = vi.fn()

    await expect(api.check()).resolves.toMatchObject({ status: 'upToDate' })
    await api.install(listener)
    await expect(api.cancel()).resolves.toBe(true)
    await api.relaunch()

    expect(channel.onmessage).toBe(listener)
    expect(calls.map(([command]) => command)).toEqual([
      'check_for_product_update',
      'install_product_update',
      'cancel_product_update',
      'relaunch_after_product_update'
    ])
    expect(calls[1]?.[1]?.onEvent).toBe(channel)
  })
})
