import { httpMethods, normalizeTranslationLanguagePair, type HttpCookieEntry, type HttpRequestDraft, type HttpResponseResult, type HttpSendInput, type KeyValueEntry, type SaveTranslationHistoryInput, type SaveTranslationWordInput, type TranslationInput } from '../../src/shared/contracts/network'
import type { NetworkCommandInput, SaveHostProfileInput } from '../../src/shared/contracts/system'

const networkActions = ['interfaces', 'connections', 'ping', 'flush-dns', 'resolve', 'whois'] as const

export function normalizeHttpRequest(value: unknown): HttpRequestDraft {
  const record = objectValue(value, 'Invalid HTTP request')
  const method = stringValue(record.method, 16)
  if (!httpMethods.includes(method as HttpRequestDraft['method'])) throw new Error('Unsupported HTTP method')
  return {
    id: optionalId(record.id),
    name: stringValue(record.name, 200).trim() || 'Untitled',
    method: method as HttpRequestDraft['method'],
    url: stringValue(record.url, 8_192).trim(),
    params: normalizeEntries(record.params),
    headers: normalizeEntries(record.headers),
    cookies: normalizeCookies(record.cookies),
    body: stringValue(record.body, 5 * 1024 * 1024),
    bodyType: stringValue(record.bodyType, 200).trim() || 'application/json'
  }
}

export function normalizeHttpSendInput(value: unknown): HttpSendInput {
  const record = objectValue(value, 'Invalid HTTP send input')
  return { requestId: requestId(record.requestId), request: normalizeHttpRequest(record.request), timeoutMs: timeout(record.timeoutMs) }
}

export function normalizeHttpResponse(value: unknown): HttpResponseResult {
  const record = objectValue(value, 'Invalid HTTP response')
  const errorCodes: HttpResponseResult['errorCode'][] = ['ABORTED', 'TIMEOUT', 'NETWORK', 'INVALID_REQUEST', 'RESPONSE_TOO_LARGE']
  const errorCode = errorCodes.includes(record.errorCode as HttpResponseResult['errorCode']) ? record.errorCode as HttpResponseResult['errorCode'] : undefined
  return {
    requestId: requestId(record.requestId), ok: record.ok === true, status: boundedNumber(record.status, 0, 999),
    statusText: stringValue(record.statusText, 2_000), url: stringValue(record.url, 8_192), durationMs: boundedNumber(record.durationMs, 0, 120_000),
    body: stringValue(record.body, 10 * 1024 * 1024), headers: stringValue(record.headers, 2 * 1024 * 1024), cookies: stringValue(record.cookies, 2 * 1024 * 1024), errorCode
  }
}

export function normalizeTranslationInput(value: unknown): TranslationInput {
  const record = objectValue(value, 'Invalid translation input')
  const provider = record.preferredProvider === 'bing' ? 'bing' : 'google'
  const languages = normalizeTranslationLanguagePair(record.sourceLang, record.targetLang)
  return {
    requestId: requestId(record.requestId),
    text: stringValue(record.text, 50_000),
    sourceLang: languages.sourceLang,
    targetLang: languages.targetLang,
    preferredProvider: provider,
    timeoutMs: timeout(record.timeoutMs)
  }
}

export function normalizeTranslationWordInput(value: unknown): SaveTranslationWordInput {
  const record = objectValue(value, 'Invalid word')
  const sourceText = stringValue(record.sourceText, 50_000).trim()
  if (!sourceText) throw new Error('Source text is required')
  const languages = normalizeTranslationLanguagePair(record.sourceLang, record.targetLang)
  return {
    id: optionalId(record.id), sourceText, targetText: stringValue(record.targetText, 50_000),
    sourceLang: languages.sourceLang, targetLang: languages.targetLang,
    remark: stringValue(record.remark, 2_000)
  }
}

export function normalizeTranslationHistoryInput(value: unknown): SaveTranslationHistoryInput {
  const word = normalizeTranslationWordInput(value)
  const record = value as Record<string, unknown>
  return { sourceText: word.sourceText, targetText: word.targetText, sourceLang: word.sourceLang, targetLang: word.targetLang, translatorType: record.translatorType === 'bing' ? 'bing' : 'google' }
}

export function normalizeHostInput(value: unknown): SaveHostProfileInput {
  const record = objectValue(value, 'Invalid host profile')
  const name = stringValue(record.name, 200).trim()
  if (!name) throw new Error('Host profile name is required')
  return { id: optionalId(record.id), name, content: stringValue(record.content, 2 * 1024 * 1024) }
}

export function normalizeNetworkInput(value: unknown): NetworkCommandInput {
  const record = objectValue(value, 'Invalid network command')
  const action = stringValue(record.action, 32)
  if (!networkActions.includes(action as NetworkCommandInput['action'])) throw new Error('Unsupported network action')
  return { requestId: requestId(record.requestId), action: action as NetworkCommandInput['action'], target: optionalString(record.target, 300), timeoutMs: timeout(record.timeoutMs) }
}

export function normalizeKeyword(value: unknown): string {
  return typeof value === 'string' ? value.slice(0, 200) : ''
}

export function normalizePositiveId(value: unknown): number {
  const id = optionalId(value)
  if (!id) throw new Error('Invalid id')
  return id
}

export function normalizeRequestId(value: unknown): string {
  return requestId(value)
}

function normalizeEntries(value: unknown): KeyValueEntry[] {
  if (!Array.isArray(value) || value.length > 100) throw new Error('Invalid HTTP entries')
  return value.map((entry, index) => {
    const record = objectValue(entry, 'Invalid HTTP entry')
    return { id: optionalString(record.id, 100) || `entry-${index}`, name: stringValue(record.name, 1_000), value: stringValue(record.value, 50_000), enabled: record.enabled !== false }
  })
}

function normalizeCookies(value: unknown): HttpCookieEntry[] {
  if (!Array.isArray(value) || value.length > 100) throw new Error('Invalid HTTP cookies')
  return value.map((entry, index) => {
    const record = objectValue(entry, 'Invalid HTTP cookie')
    return {
      id: optionalString(record.id, 100) || `cookie-${index}`, name: stringValue(record.name, 1_000), value: stringValue(record.value, 50_000), enabled: record.enabled !== false,
      domain: optionalString(record.domain, 1_000), path: optionalString(record.path, 1_000), expires: optionalString(record.expires, 100)
    }
  })
}

function timeout(value: unknown): number {
  return Number.isFinite(value) ? Math.max(1_000, Math.min(120_000, Math.round(Number(value)))) : 30_000
}

function boundedNumber(value: unknown, minimum: number, maximum: number): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) throw new Error('Invalid number value')
  return Math.max(minimum, Math.min(maximum, Math.round(value)))
}

function requestId(value: unknown): string {
  const id = stringValue(value, 100)
  if (!/^[a-zA-Z0-9._:-]+$/.test(id)) throw new Error('Invalid request id')
  return id
}

function optionalId(value: unknown): number | undefined {
  return Number.isSafeInteger(value) && Number(value) > 0 ? Number(value) : undefined
}

function stringValue(value: unknown, maxLength: number): string {
  if (typeof value !== 'string' || value.length > maxLength) throw new Error('Invalid string value')
  return value
}

function optionalString(value: unknown, maxLength: number): string {
  if (value == null) return ''
  return stringValue(value, maxLength)
}

function objectValue(value: unknown, message: string): Record<string, unknown> {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) throw new Error(message)
  return value as Record<string, unknown>
}
