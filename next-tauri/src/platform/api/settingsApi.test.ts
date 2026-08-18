import { describe, expect, it, vi } from 'vitest'
import { defaultAppSettings } from '../contracts/settings'
import { createSettingsApi, SETTINGS_CHANGED_EVENT } from './settingsApi'

describe('settings API adapter', () => {
  it('maps settings operations to owned Tauri commands', async () => {
    const settings = defaultAppSettings()
    const invoke = vi.fn()
      .mockResolvedValueOnce(settings)
      .mockResolvedValueOnce({ ...settings, revision: 1 })
      .mockResolvedValueOnce(settings)
      .mockResolvedValueOnce(undefined)
    const subscribe = vi.fn().mockResolvedValue(() => undefined)
    const api = createSettingsApi(invoke, subscribe)

    await expect(api.get()).resolves.toEqual(settings)
    await expect(api.update(settings)).resolves.toEqual({ ...settings, revision: 1 })
    await expect(api.reset()).resolves.toEqual(settings)
    await expect(api.openWindow()).resolves.toBeUndefined()

    expect(invoke.mock.calls).toEqual([
      ['get_settings'],
      ['update_settings', { settings }],
      ['reset_settings'],
      ['open_settings_window']
    ])
  })

  it('forwards the Rust synchronization event payload', async () => {
    const settings = { ...defaultAppSettings(), revision: 4 }
    const subscribe = vi.fn(async (_event, handler) => {
      handler({ payload: settings })
      return () => undefined
    })
    const listener = vi.fn()
    const api = createSettingsApi(vi.fn(), subscribe)

    await api.subscribe(listener)

    expect(subscribe).toHaveBeenCalledWith(SETTINGS_CHANGED_EVENT, expect.any(Function))
    expect(listener).toHaveBeenCalledWith(settings)
  })
})
