export interface HttpEntry {
  id: string
  name: string
  value: string
  enabled: boolean
}

export interface HttpCookieEntry extends HttpEntry {
  domain: string
  path: string
  expires: string
}

export interface HttpRequestSpec {
  requestId: string
  name: string
  method: string
  url: string
  params: HttpEntry[]
  headers: HttpEntry[]
  cookies: HttpCookieEntry[]
  body: string
  bodyType: string
  timeoutMs: number
  followRedirects: boolean
  savedId?: number
}

export interface HttpResponseData {
  status: number
  finalUrl: string
  headers: [string, string][]
  bodyText: string
  bodyBase64: string
  contentType: string
  sizeBytes: number
  truncated: boolean
  durationMs: number
  statusText?: string
}

export interface SaveHttpRequestInput {
  id?: number
  name: string
  method: string
  url: string
  params: HttpEntry[]
  headers: HttpEntry[]
  cookies: HttpCookieEntry[]
  body: string
  bodyType: string
  responseBody?: string
  responseHeaders?: string
  responseCookies?: string
}

export interface SavedHttpRequest extends SaveHttpRequestInput {
  id: number
  responseBody: string
  responseHeaders: string
  responseCookies: string
  createTime: string
  modifiedTime: string
}

export interface HttpRequestHistory extends SavedHttpRequest {
  requestIdValue: number | null
  title: string
  status: string
  costTime: number
}

export type HttpProgressEvent =
  | { kind: 'started' }
  | { kind: 'headers'; status: number }
  | { kind: 'download'; receivedBytes: number }

export interface HttpApi {
  execute(request: HttpRequestSpec, onProgress: (event: HttpProgressEvent) => void): Promise<HttpResponseData>
  cancel(requestId: string): Promise<boolean>
  listSaved(query?: string): Promise<SavedHttpRequest[]>
  save(item: SaveHttpRequestInput): Promise<SavedHttpRequest>
  deleteSaved(id: number): Promise<boolean>
  listHistory(query?: string): Promise<HttpRequestHistory[]>
  deleteHistory(id: number): Promise<boolean>
  clearHistory(): Promise<number>
}
