import { describe, expect, it, vi } from 'vitest'
import { createToolWebviewApi } from './toolWebviewApi'

describe('tool WebView API adapter', () => {
  it('maps lifecycle operations to Tauri-owned domain commands', async () => {
    const snapshot = {
      exists: true,
      visible: true,
      placement: 'docked' as const,
      reparentOperations: 200,
      pageLoads: 1,
      sessionId: 'session-a',
      counter: 9,
      draft: 'preserve me',
      lastStressCycles: 100,
      lastStressPassed: true
    }
    const invoke = vi.fn().mockResolvedValue(snapshot)
    const api = createToolWebviewApi(invoke)
    const bounds = { x: 280, y: 120, width: 800, height: 560 }

    await api.open(bounds)
    await api.updateBounds(bounds)
    await api.setVisible(false)
    await api.detach()
    await api.dock(bounds)
    await api.stress(bounds, 100)
    await api.close()
    await api.report({ sessionId: 'session-a', counter: 9, draft: 'preserve me' })

    expect(invoke.mock.calls).toEqual([
      ['open_tool_webview', { bounds }],
      ['update_tool_webview_bounds', { bounds }],
      ['set_tool_webview_visible', { visible: false }],
      ['detach_tool_webview'],
      ['dock_tool_webview', { bounds }],
      ['stress_tool_webview_reparent', { bounds, cycles: 100 }],
      ['close_tool_webview'],
      ['report_tool_webview_probe', {
        report: { sessionId: 'session-a', counter: 9, draft: 'preserve me' }
      }]
    ])
  })
})
