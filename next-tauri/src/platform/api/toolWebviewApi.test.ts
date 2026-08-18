import { describe, expect, it, vi } from 'vitest'
import { createToolWebviewApi } from './toolWebviewApi'

describe('tool WebView API adapter', () => {
  it('scopes lifecycle operations to the requested managed tool', async () => {
    const snapshot = {
      toolId: 'calculator' as const,
      exists: true,
      visible: true,
      placement: 'docked' as const,
      reparentOperations: 200,
      pageLoads: 1,
      sessionId: 'calculator-a',
      stateRevision: 9,
      stateDigest: '9*9=81',
      stateSummary: '9*9 = 81 · 2 条记录',
      lastStressCycles: 100,
      lastStressPassed: true
    }
    const invoke = vi.fn().mockResolvedValue(snapshot)
    const api = createToolWebviewApi('calculator', invoke)
    const bounds = { x: 280, y: 120, width: 800, height: 560 }

    await api.getSnapshot()
    await api.open(bounds)
    await api.updateBounds(bounds)
    await api.setVisible(false)
    await api.detach()
    await api.dock(bounds)
    await api.stress(bounds, 100)
    await api.close()
    await api.report({
      sessionId: 'calculator-a',
      stateRevision: 9,
      stateDigest: '9*9=81',
      stateSummary: '9*9 = 81 · 2 条记录'
    })

    expect(invoke.mock.calls).toEqual([
      ['get_tool_webview_snapshot', { toolId: 'calculator' }],
      ['open_tool_webview', { toolId: 'calculator', bounds }],
      ['update_tool_webview_bounds', { toolId: 'calculator', bounds }],
      ['set_tool_webview_visible', { toolId: 'calculator', visible: false }],
      ['detach_tool_webview', { toolId: 'calculator' }],
      ['dock_tool_webview', { toolId: 'calculator', bounds }],
      ['stress_tool_webview_reparent', { toolId: 'calculator', bounds, cycles: 100 }],
      ['close_tool_webview', { toolId: 'calculator' }],
      ['report_tool_webview_session', {
        report: {
          sessionId: 'calculator-a',
          stateRevision: 9,
          stateDigest: '9*9=81',
          stateSummary: '9*9 = 81 · 2 条记录'
        }
      }]
    ])
  })
})
