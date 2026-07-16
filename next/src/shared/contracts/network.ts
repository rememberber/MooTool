export const httpMethods = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS'] as const
export type HttpMethod = (typeof httpMethods)[number]

export type KeyValueEntry = {
  id: string
  name: string
  value: string
  enabled: boolean
}

export type HttpCookieEntry = KeyValueEntry & {
  domain: string
  path: string
  expires: string
}

export type HttpRequestDraft = {
  id?: number
  name: string
  method: HttpMethod
  url: string
  params: KeyValueEntry[]
  headers: KeyValueEntry[]
  cookies: HttpCookieEntry[]
  body: string
  bodyType: string
}

export type SavedHttpRequest = HttpRequestDraft & {
  id: number
  responseBody: string
  responseHeaders: string
  responseCookies: string
  createTime: string
  modifiedTime: string
}

export type HttpSendInput = {
  requestId: string
  request: HttpRequestDraft
  timeoutMs: number
}

export type HttpResponseResult = {
  requestId: string
  ok: boolean
  status: number
  statusText: string
  url: string
  durationMs: number
  body: string
  headers: string
  cookies: string
  errorCode?: 'ABORTED' | 'TIMEOUT' | 'NETWORK' | 'INVALID_REQUEST' | 'RESPONSE_TOO_LARGE'
}

export type HttpRequestHistory = SavedHttpRequest & {
  requestIdValue: number | null
  title: string
  status: string
  costTime: number
}

export type TranslationProvider = 'google' | 'bing'

export const translationLanguageCodes = [
  'auto', 'zh-CN', 'en', 'yue', 'wyw', 'jp', 'kor', 'fra', 'spa', 'th', 'ara', 'ru', 'pt', 'de', 'it',
  'el', 'nl', 'pl', 'bul', 'est', 'dan', 'fin', 'cs', 'rom', 'slo', 'swe', 'hu', 'cht', 'vie'
] as const
export type TranslationLanguageCode = (typeof translationLanguageCodes)[number]

export type TranslationInput = {
  requestId: string
  text: string
  sourceLang: string
  targetLang: string
  preferredProvider: TranslationProvider
  timeoutMs: number
}

export type TranslationResult = {
  requestId: string
  text: string
  provider: TranslationProvider
  fallbackUsed: boolean
}

export type TranslationWord = {
  id: number
  sourceText: string
  targetText: string
  sourceLang: string
  targetLang: string
  remark: string
  createTime: string
  modifiedTime: string
}

export type SaveTranslationWordInput = Omit<TranslationWord, 'id' | 'createTime' | 'modifiedTime'> & { id?: number }

export type TranslationHistory = {
  id: number
  sourceText: string
  targetText: string
  sourceLang: string
  targetLang: string
  translatorType: TranslationProvider
  createTime: string
}

export type SaveTranslationHistoryInput = Omit<TranslationHistory, 'id' | 'createTime'>
