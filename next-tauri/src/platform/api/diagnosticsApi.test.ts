import { describe, expect, it, vi } from 'vitest'
import { createDiagnosticsApi } from './diagnosticsApi'

describe('diagnostics API', () => {
  it('maps diagnostics to owned Rust commands', async () => {
    const invoke = vi.fn()
      .mockResolvedValueOnce([{ name: 'PATH', value: '/bin', sensitive: false }])
      .mockResolvedValueOnce({ osName: 'macOS' })
      .mockResolvedValueOnce(undefined)
      .mockResolvedValueOnce({ bundlePath: '/tmp/diagnostics' })
    const api = createDiagnosticsApi(invoke)
    const report = {
      code: 'network',
      message: 'request failed',
      context: 'translation.submit',
      retryable: true
    }

    await api.environment(false)
    await api.system()
    await api.reportError(report)
    await api.exportBundle('/tmp')

    expect(invoke.mock.calls).toEqual([
      ['get_environment_variables', { revealSensitive: false }],
      ['get_system_snapshot'],
      ['report_frontend_error', { report }],
      ['export_diagnostics_bundle', { destinationDirectory: '/tmp' }]
    ])
  })
})
