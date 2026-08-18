import { describe, expect, it, vi } from 'vitest'
import { CLOSE_REQUESTED_EVENT, createDesktopApi } from './desktopApi'

describe('desktopApi', () => {
  it('resolves close requests through the owned Tauri command', async () => {
    const invoke = vi.fn().mockResolvedValue(undefined)
    const api = createDesktopApi(invoke)

    await api.resolveCloseRequest('minimizeToTray')

    expect(invoke).toHaveBeenCalledWith('resolve_close_request', {
      decision: 'minimizeToTray'
    })
  })

  it('subscribes to the product-scoped close request event', async () => {
    const listen = vi.fn().mockResolvedValue(() => undefined)
    const listener = vi.fn()
    const api = createDesktopApi(vi.fn(), listen)

    await api.subscribeCloseRequested(listener)

    expect(listen).toHaveBeenCalledWith(CLOSE_REQUESTED_EVENT, expect.any(Function))
    const handler = listen.mock.calls[0][1]
    handler({ payload: { canMinimizeToTray: true } })
    expect(listener).toHaveBeenCalledWith({ canMinimizeToTray: true })
  })
})
