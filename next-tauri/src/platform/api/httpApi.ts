import { Channel, invoke } from '@tauri-apps/api/core'
import type {
  HttpApi,
  HttpProgressEvent,
  HttpRequestHistory,
  HttpResponseData,
  SavedHttpRequest
} from '../contracts/http'

const browserControllers = new Map<string, AbortController>()
const SAVED_KEY = 'mootool-next-tauri:http-saved:v1'
const HISTORY_KEY = 'mootool-next-tauri:http-history:v1'

type Invoke = <T>(command: string, args?: Record<string, unknown>) => Promise<T>
interface ProgressChannel { onmessage: (event: HttpProgressEvent) => void }

export function createHttpApi(
  invokeCommand: Invoke = invoke,
  createChannel: () => ProgressChannel = () => new Channel<HttpProgressEvent>()
): HttpApi {
  return {
    execute: (request, onProgress) => {
      const progress = createChannel()
      progress.onmessage = onProgress
      return invokeCommand<HttpResponseData>('execute_http_request', { request, progress })
    },
    cancel: (requestId) => invokeCommand<boolean>('cancel_http_request', { requestId }),
    listSaved: (query = '') => invokeCommand<SavedHttpRequest[]>('list_saved_http_requests', { query }),
    save: (item) => invokeCommand<SavedHttpRequest>('save_http_request', { item }),
    deleteSaved: (id) => invokeCommand<boolean>('delete_saved_http_request', { id }),
    listHistory: (query = '') => invokeCommand<HttpRequestHistory[]>('list_http_request_history', { query }),
    deleteHistory: (id) => invokeCommand<boolean>('delete_http_request_history', { id }),
    clearHistory: () => invokeCommand<number>('clear_http_request_history')
  }
}

const browserApi: HttpApi = {
  execute: async (request, onProgress) => {
    const controller = new AbortController()
    browserControllers.set(request.requestId, controller)
    const timeout = window.setTimeout(() => controller.abort(), request.timeoutMs)
    const started = performance.now()
    onProgress({ kind: 'started' })
    const url = new URL(request.url)
    for (const entry of request.params.filter((item) => item.enabled && item.name.trim())) {
      url.searchParams.append(entry.name.trim(), entry.value)
    }
    const headers = new Headers()
    for (const entry of request.headers.filter((item) => item.enabled && item.name.trim())) {
      headers.append(entry.name.trim(), entry.value)
    }
    const cookies = request.cookies
      .filter((item) => item.enabled && item.name.trim())
      .map((item) => `${item.name.trim()}=${item.value}`)
      .join('; ')
    if (cookies) headers.set('Cookie', cookies)
    if (request.body && request.bodyType && !headers.has('Content-Type')) headers.set('Content-Type', request.bodyType)
    try {
      const response = await fetch(url, {
        method: request.method,
        headers,
        body: ['GET', 'HEAD'].includes(request.method) ? undefined : request.body,
        redirect: request.followRedirects ? 'follow' : 'manual',
        signal: controller.signal
      })
      onProgress({ kind: 'headers', status: response.status })
      const bodyText = await response.text()
      onProgress({ kind: 'download', receivedBytes: bodyText.length })
      const result: HttpResponseData = {
        status: response.status,
        finalUrl: response.url,
        headers: [...response.headers.entries()],
        bodyText,
        bodyBase64: '',
        contentType: response.headers.get('content-type') ?? '',
        sizeBytes: new Blob([bodyText]).size,
        truncated: false,
        durationMs: Math.round(performance.now() - started)
      }
      const history: HttpRequestHistory = { id: request.requestId, request, response: result, createdAt: Date.now() }
      writeList(HISTORY_KEY, [history, ...readList<HttpRequestHistory>(HISTORY_KEY)].slice(0, 500))
      return result
    } finally {
      window.clearTimeout(timeout)
      browserControllers.delete(request.requestId)
    }
  },
  cancel: async (requestId) => {
    const controller = browserControllers.get(requestId)
    controller?.abort()
    return Boolean(controller)
  },
  listSaved: async (query = '') => filterSaved(readList<SavedHttpRequest>(SAVED_KEY), query),
  save: async (item) => {
    const values = readList<SavedHttpRequest>(SAVED_KEY)
    const sameName = values.find((value) => value.name === item.name)
    const saved = sameName ? { ...item, id: sameName.id, createdAt: sameName.createdAt } : item
    writeList(SAVED_KEY, [saved, ...values.filter((value) => value.id !== saved.id && value.name !== saved.name)])
    return saved
  },
  deleteSaved: async (id) => deleteItem<SavedHttpRequest>(SAVED_KEY, id),
  listHistory: async (query = '') => filterHistory(readList<HttpRequestHistory>(HISTORY_KEY), query).slice(0, 500),
  deleteHistory: async (id) => deleteItem<HttpRequestHistory>(HISTORY_KEY, id),
  clearHistory: async () => {
    const count = readList<HttpRequestHistory>(HISTORY_KEY).length
    window.localStorage.removeItem(HISTORY_KEY)
    return count
  }
}

function readList<T>(key: string): T[] {
  try {
    const raw = window.localStorage.getItem(key)
    return raw ? JSON.parse(raw) as T[] : []
  } catch {
    return []
  }
}

function writeList<T>(key: string, values: T[]): void {
  window.localStorage.setItem(key, JSON.stringify(values))
}

function deleteItem<T extends { id: string }>(key: string, id: string): boolean {
  const values = readList<T>(key)
  const next = values.filter((item) => item.id !== id)
  writeList(key, next)
  return next.length !== values.length
}

function filterSaved(values: SavedHttpRequest[], query: string): SavedHttpRequest[] {
  const needle = query.trim().toLowerCase()
  return values
    .filter((item) => !needle || item.name.toLowerCase().includes(needle) || item.request.url.toLowerCase().includes(needle))
    .sort((left, right) => right.updatedAt - left.updatedAt)
}

function filterHistory(values: HttpRequestHistory[], query: string): HttpRequestHistory[] {
  const needle = query.trim().toLowerCase()
  return values
    .filter((item) => !needle || item.request.name.toLowerCase().includes(needle) || item.request.url.toLowerCase().includes(needle))
    .sort((left, right) => right.createdAt - left.createdAt)
}

export const httpApi: HttpApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__ ? createHttpApi() : browserApi
