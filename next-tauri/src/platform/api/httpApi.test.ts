import { describe, expect, it, vi } from 'vitest'
import { emptyHttpRequest } from '../../features/http/httpTools'
import { createHttpApi } from './httpApi'

describe('HTTP API adapter', () => {
  it('keeps execution, collections and history behind Tauri commands', async () => {
    const invoke = vi.fn().mockResolvedValue(undefined)
    const channel = { onmessage: vi.fn() }
    const api = createHttpApi(invoke, () => channel)
    const request = emptyHttpRequest()
    const item = { id: 'saved-1', name: 'Example', request, createdAt: 1, updatedAt: 1 }

    await api.execute(request, vi.fn())
    await api.cancel(request.requestId)
    await api.listSaved('example')
    await api.save(item)
    await api.deleteSaved('saved-1')
    await api.listHistory('example')
    await api.deleteHistory('history-1')
    await api.clearHistory()

    expect(invoke.mock.calls).toEqual([
      ['execute_http_request', { request, progress: channel }],
      ['cancel_http_request', { requestId: request.requestId }],
      ['list_saved_http_requests', { query: 'example' }],
      ['save_http_request', { item }],
      ['delete_saved_http_request', { id: 'saved-1' }],
      ['list_http_request_history', { query: 'example' }],
      ['delete_http_request_history', { id: 'history-1' }],
      ['clear_http_request_history']
    ])
  })
})
