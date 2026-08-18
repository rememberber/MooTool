import { describe, expect, it, vi } from 'vitest'
import { createNativeDesktopApi } from './nativeDesktopApi'

describe('native desktop API adapter', () => {
  it('keeps capture, color sampling, and display sleep behind Rust commands', async () => {
    const invoke = vi.fn()
      .mockResolvedValueOnce({ assets: [], monitorCount: 0 })
      .mockResolvedValueOnce({ hex: '#2F6FED' })
      .mockResolvedValueOnce({ active: false })
      .mockResolvedValueOnce({ active: true })
    const api = createNativeDesktopApi(invoke)

    await api.captureDisplays(400)
    await api.sampleScreenColor(2_000)
    await api.getDisplaySleepStatus('system-tool')
    await api.setDisplaySleepPrevention('system-tool', true)

    expect(invoke.mock.calls).toEqual([
      ['capture_display_images', { delayMs: 400 }],
      ['sample_screen_color', { delayMs: 2_000 }],
      ['get_display_sleep_status', { owner: 'system-tool' }],
      ['set_display_sleep_prevention', { owner: 'system-tool', enabled: true }]
    ])
  })
})
