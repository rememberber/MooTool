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
}

export interface SavedHttpRequest {
  id: string
  name: string
  request: HttpRequestSpec
  response?: HttpResponseData
  createdAt: number
  updatedAt: number
}

export interface HttpRequestHistory {
  id: string
  request: HttpRequestSpec
  response: HttpResponseData
  createdAt: number
}

export type HttpProgressEvent =
  | { kind: 'started' }
  | { kind: 'headers'; status: number }
  | { kind: 'download'; receivedBytes: number }

export interface HttpApi {
  execute(request: HttpRequestSpec, onProgress: (event: HttpProgressEvent) => void): Promise<HttpResponseData>
  cancel(requestId: string): Promise<boolean>
  listSaved(query?: string): Promise<SavedHttpRequest[]>
  save(item: SavedHttpRequest): Promise<SavedHttpRequest>
  deleteSaved(id: string): Promise<boolean>
  listHistory(query?: string): Promise<HttpRequestHistory[]>
  deleteHistory(id: string): Promise<boolean>
  clearHistory(): Promise<number>
}
