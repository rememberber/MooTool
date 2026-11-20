import { Channel, invoke } from '@tauri-apps/api/core'
import type {
  HttpApi,
  HttpProgressEvent,
  HttpRequestHistory,
  HttpResponseData,
  SavedHttpRequest,
  SaveHttpRequestInput
} from '../contracts/http'
import { sqliteDate } from '../../features/host/hostProfileStore'
import { httpFailureCode, prepareHttpExchange } from '../../features/http/httpTools'

const browserControllers = new Map<string, AbortController>()
const SAVED_KEY = 'mootool-next-tauri:http-saved:v2'
const HISTORY_KEY = 'mootool-next-tauri:http-history:v2'

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
    const timeout = window.setTimeout(() => controller.abort(new Error('TIMEOUT')), request.timeoutMs)
    const started = performance.now()
    onProgress({ kind: 'started' })
    const prepared = prepareHttpExchange(request)
    const headers = new Headers()
    for (const entry of request.headers.filter((item) => item.enabled && item.name.trim())) {
      headers.append(entry.name.trim(), entry.value)
    }
    const cookies = request.cookies
      .filter((item) => item.enabled && item.name.trim())
      .map((item) => `${item.name.trim()}=${item.value}`)
      .join('; ')
    if (cookies) headers.set('Cookie', cookies)
    if (prepared.body && prepared.contentType && !headers.has('Content-Type')) headers.set('Content-Type', prepared.contentType)
    try {
      const response = await fetch(prepared.url, {
        method: request.method,
        headers,
        body: prepared.body,
        redirect: request.followRedirects ? 'follow' : 'manual',
        signal: controller.signal
      })
      onProgress({ kind: 'headers', status: response.status })
      const declaredLength = Number(response.headers.get('content-length') || 0)
      if (declaredLength > 10 * 1024 * 1024) return finishBrowserFailure(request, 'RESPONSE_TOO_LARGE', started)
      const bytes = new Uint8Array(await response.arrayBuffer())
      if (bytes.byteLength > 10 * 1024 * 1024) return finishBrowserFailure(request, 'RESPONSE_TOO_LARGE', started)
      const charset = /charset=([^;]+)/i.exec(response.headers.get('content-type') ?? '')?.[1]?.trim().replace(/^['"]|['"]$/g, '') || 'utf-8'
      let rawBody: string
      try {
        rawBody = new TextDecoder(charset).decode(bytes)
      } catch {
        return finishBrowserFailure(request, 'INVALID_REQUEST', started)
      }
      const bodyText = prettyJson(rawBody)
      onProgress({ kind: 'download', receivedBytes: bodyText.length })
      const result: HttpResponseData = {
        status: response.status,
        finalUrl: response.url,
        headers: [...response.headers.entries()],
        bodyText,
        bodyBase64: '',
        contentType: response.headers.get('content-type') ?? '',
        sizeBytes: bytes.byteLength,
        truncated: false,
        durationMs: Math.round(performance.now() - started)
      }
      const values = readList<HttpRequestHistory>(HISTORY_KEY)
      const now = sqliteDate()
      const history: HttpRequestHistory = {
        id: nextId(values),
        requestIdValue: request.savedId ?? null,
        title: request.name.trim(),
        name: request.name.trim(),
        method: request.method,
        url: request.url,
        params: request.params,
        headers: request.headers,
        cookies: request.cookies,
        body: request.body,
        bodyType: request.bodyType,
        responseBody: result.bodyText,
        responseHeaders: result.headers.map(([name, value]) => `${name}: ${value}`).join('\n'),
        responseCookies: result.headers.filter(([name]) => name.toLowerCase() === 'set-cookie').map(([, value]) => value).join('\n'),
        status: `${result.status} ${result.statusText ?? ''}`.trim(),
        costTime: result.durationMs,
        createTime: now,
        modifiedTime: now
      }
      writeList(HISTORY_KEY, [...values, history].slice(-500))
      return result
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error)
      const reason = controller.signal.reason instanceof Error ? controller.signal.reason.message : ''
      return finishBrowserFailure(request, httpFailureCode(`${message} ${reason}`, controller.signal.aborted), started)
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
  save: async (item: SaveHttpRequestInput) => {
    const values = readList<SavedHttpRequest>(SAVED_KEY)
    const name = item.name.trim()
    const now = sqliteDate()
    const index = item.id && item.id > 0
      ? values.findIndex((value) => value.id === item.id)
      : values.findIndex((value) => value.name === name)
    const current = index >= 0 ? values[index] : undefined
    const saved: SavedHttpRequest = {
      ...item,
      id: current?.id ?? nextId(values),
      name,
      url: item.url.trim(),
      bodyType: item.bodyType.trim() || 'application/json',
      responseBody: item.responseBody ?? '',
      responseHeaders: item.responseHeaders ?? '',
      responseCookies: item.responseCookies ?? '',
      createTime: current?.createTime ?? now,
      modifiedTime: now
    }
    const next = index >= 0 ? values.map((value, valueIndex) => valueIndex === index ? saved : value) : [saved, ...values]
    writeList(SAVED_KEY, next)
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

function deleteItem<T extends { id: number }>(key: string, id: number): boolean {
  const values = readList<T>(key)
  const next = values.filter((item) => item.id !== id)
  writeList(key, next)
  return next.length !== values.length
}

function like(value: string, query: string): boolean {
  const pattern = `%${query.trim()}%`.replace(/[.*+?^${}()|[\]\\]/g, '\\$&').replaceAll('%', '.*').replaceAll('_', '.')
  return new RegExp(`^${pattern}$`, 'i').test(value)
}

function finishBrowserFailure(request: Parameters<HttpApi['execute']>[0], statusText: string, started: number): HttpResponseData {
  const result: HttpResponseData = {
    status: 0,
    finalUrl: request.url,
    headers: [],
    bodyText: '',
    bodyBase64: '',
    contentType: '',
    sizeBytes: 0,
    truncated: false,
    durationMs: Math.round(performance.now() - started),
    statusText
  }
  const values = readList<HttpRequestHistory>(HISTORY_KEY)
  const now = sqliteDate()
  writeList(HISTORY_KEY, [...values, {
    id: nextId(values),
    requestIdValue: request.savedId ?? null,
    title: request.name.trim(),
    name: request.name.trim(),
    method: request.method,
    url: request.url,
    params: request.params,
    headers: request.headers,
    cookies: request.cookies,
    body: request.body,
    bodyType: request.bodyType,
    responseBody: '',
    responseHeaders: '',
    responseCookies: '',
    status: `0 ${statusText}`,
    costTime: result.durationMs,
    createTime: now,
    modifiedTime: now
  }].slice(-500))
  return result
}

function prettyJson(value: string): string {
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

function nextId(values: Array<{ id: number }>): number {
  return values.reduce((highest, item) => Math.max(highest, item.id), 0) + 1
}

function filterSaved(values: SavedHttpRequest[], query: string): SavedHttpRequest[] {
  return values
    .filter((item) => !query.trim() || like(item.name, query))
    .sort((left, right) => right.modifiedTime.localeCompare(left.modifiedTime) || right.id - left.id)
}

function filterHistory(values: HttpRequestHistory[], query: string): HttpRequestHistory[] {
  return values
    .filter((item) => !query.trim() || like(item.title, query) || like(item.url, query))
    .sort((left, right) => right.id - left.id)
    .slice(0, 500)
}

export const httpApi: HttpApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__ ? createHttpApi() : browserApi
