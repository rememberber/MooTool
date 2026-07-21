import { fetch, Agent, ProxyAgent, type Dispatcher } from 'undici'
import type {
  HttpCookieEntry,
  HttpResponseResult,
  HttpSendInput,
  KeyValueEntry,
  TranslationInput,
  TranslationProvider,
  TranslationResult
} from '../../src/shared/contracts/network'

export type ProxyConfiguration = {
  enabled: boolean
  host: string
  port: string
  username: string
  password: string
}

type BingSession = {
  ig: string
  key: string
  token: string
  expiresAt: number
  requestCount: number
}

const maxResponseBytes = 10 * 1024 * 1024
const userAgent = 'Mozilla/5.0 (MooTool Next) AppleWebKit/537.36 Chrome/138 Safari/537.36'
/** Align with Java GoogleTranslatorUtil connect timeout — fail fast when Google is unreachable. */
const translationConnectTimeoutMs = 5_000
const translationBodyTimeoutMs = 10_000
/** After a provider fails, skip it briefly so every keystroke doesn't re-wait on a dead endpoint. */
const providerCooldownMs = 10 * 60 * 1_000

export class NetworkService {
  private readonly controllers = new Map<string, AbortController>()
  private readonly timers = new Map<string, NodeJS.Timeout>()
  private readonly proxyDispatchers = new Map<string, Dispatcher>()
  private readonly translationProxyDispatchers = new Map<string, Dispatcher>()
  private readonly providerCooldownUntil = new Map<TranslationProvider, number>()
  private readonly directTranslationAgent = createTranslationAgent()
  private bingSession: BingSession | null = null

  async sendHttp(input: HttpSendInput, proxy: ProxyConfiguration): Promise<HttpResponseResult> {
    const startedAt = Date.now()
    const controller = this.begin(input.requestId, input.timeoutMs)
    try {
      const url = buildRequestUrl(input.request.url, input.request.method, input.request.params)
      const headers = buildHeaders(input.request.headers, input.request.cookies)
      const body = buildRequestBody(input.request.method, input.request.body, input.request.bodyType, input.request.params, headers)
      const response = await fetch(url, {
        method: input.request.method,
        headers,
        body,
        signal: controller.signal,
        dispatcher: this.getProxyDispatcher(proxy),
        redirect: 'follow'
      })
      const bytes = await readLimitedResponse(response)
      const text = new TextDecoder(response.headers.get('content-type')?.match(/charset=([^;]+)/i)?.[1] || 'utf-8').decode(bytes)
      const responseHeaders = formatHeaders(response.headers)
      const cookies = response.headers.getSetCookie().join('\n')
      return {
        requestId: input.requestId,
        ok: response.ok,
        status: response.status,
        statusText: response.statusText,
        url: response.url,
        durationMs: Date.now() - startedAt,
        body: prettyJson(text),
        headers: responseHeaders,
        cookies
      }
    } catch (error) {
      return {
        requestId: input.requestId,
        ok: false,
        status: 0,
        statusText: errorMessage(error),
        url: input.request.url,
        durationMs: Date.now() - startedAt,
        body: '',
        headers: '',
        cookies: '',
        errorCode: classifyNetworkError(error, controller.signal)
      }
    } finally {
      this.finish(input.requestId)
    }
  }

  async translate(input: TranslationInput, proxy: ProxyConfiguration): Promise<TranslationResult> {
    const controller = this.begin(input.requestId, input.timeoutMs)
    try {
      const dispatcher = this.getTranslationDispatcher(proxy)
      const preferred = input.preferredProvider
      const order = translationProviderOrder(preferred, this.shouldSkipProvider(preferred))
      let lastError: unknown

      for (const provider of order) {
        if (controller.signal.aborted) break
        try {
          const text = await this.translateWith(provider, input, controller.signal, dispatcher)
          this.markProviderSuccess(provider)
          return {
            requestId: input.requestId,
            text,
            provider,
            fallbackUsed: provider !== preferred
          }
        } catch (error) {
          lastError = error
          if (isRequestCancelled(controller.signal, error)) throw error
          this.markProviderFailure(provider)
        }
      }

      throw lastError instanceof Error ? lastError : new Error(errorMessage(lastError))
    } finally {
      this.finish(input.requestId)
    }
  }

  private getTranslationDispatcher(proxy: ProxyConfiguration): Dispatcher {
    if (!proxy.enabled) return this.directTranslationAgent
    const key = proxyDispatcherKey(proxy)
    const existing = this.translationProxyDispatchers.get(key)
    if (existing) return existing
    const dispatcher = createTranslationProxyDispatcher(proxy)
    this.translationProxyDispatchers.set(key, dispatcher)
    return dispatcher
  }

  private getProxyDispatcher(proxy: ProxyConfiguration): Dispatcher | undefined {
    if (!proxy.enabled) return undefined
    const key = proxyDispatcherKey(proxy)
    const existing = this.proxyDispatchers.get(key)
    if (existing) return existing
    const dispatcher = createProxyDispatcher(proxy)
    if (!dispatcher) return undefined
    this.proxyDispatchers.set(key, dispatcher)
    return dispatcher
  }

  private shouldSkipProvider(provider: TranslationProvider): boolean {
    const until = this.providerCooldownUntil.get(provider)
    return until != null && until > Date.now()
  }

  private markProviderFailure(provider: TranslationProvider): void {
    this.providerCooldownUntil.set(provider, Date.now() + providerCooldownMs)
  }

  private markProviderSuccess(provider: TranslationProvider): void {
    this.providerCooldownUntil.delete(provider)
  }

  cancel(requestId: string): boolean {
    const controller = this.controllers.get(requestId)
    if (!controller) return false
    controller.abort(new Error('ABORTED'))
    this.finish(requestId)
    return true
  }

  private begin(requestId: string, timeoutMs: number): AbortController {
    this.cancel(requestId)
    const controller = new AbortController()
    this.controllers.set(requestId, controller)
    const timer = setTimeout(() => controller.abort(new Error('TIMEOUT')), timeoutMs)
    timer.unref()
    this.timers.set(requestId, timer)
    controller.signal.addEventListener('abort', () => this.finish(requestId), { once: true })
    return controller
  }

  private finish(requestId: string): void {
    const timer = this.timers.get(requestId)
    if (timer) clearTimeout(timer)
    this.timers.delete(requestId)
    this.controllers.delete(requestId)
  }

  private async translateWith(provider: TranslationProvider, input: TranslationInput, signal: AbortSignal, dispatcher?: Dispatcher): Promise<string> {
    if (provider === 'google') return translateGoogle(input.text, input.sourceLang, input.targetLang, signal, dispatcher)
    return this.translateBing(input.text, input.sourceLang, input.targetLang, signal, dispatcher)
  }

  private async translateBing(text: string, sourceLang: string, targetLang: string, signal: AbortSignal, dispatcher?: Dispatcher): Promise<string> {
    // Align with Java: one POST for the full text (no client-side chunking).
    const session = await this.getBingSession(signal, dispatcher)
    session.requestCount += 2
    const body = new URLSearchParams({
      fromLang: bingLanguage(sourceLang, true),
      to: bingLanguage(targetLang, false),
      text,
      token: session.token,
      key: session.key,
      tryFetchingGenderDebiasedTranslations: 'true'
    })
    const response = await fetch(`https://cn.bing.com/ttranslatev3?isVertical=1&IG=${encodeURIComponent(session.ig)}&IID=translator.5026.${session.requestCount}`, {
      method: 'POST',
      headers: {
        'content-type': 'application/x-www-form-urlencoded',
        origin: 'https://cn.bing.com',
        referer: 'https://cn.bing.com/translator',
        'user-agent': userAgent
      },
      body: body.toString(),
      signal,
      dispatcher
    })
    if (!response.ok) throw new Error(`Bing HTTP ${response.status}`)
    const payload = await response.json() as Array<{ translations?: Array<{ text?: string }> }>
    const translated = payload[0]?.translations?.[0]?.text
    if (!translated) throw new Error('Bing returned no translation')
    return translated
  }

  private async getBingSession(signal: AbortSignal, dispatcher?: Dispatcher): Promise<BingSession> {
    if (this.bingSession && this.bingSession.expiresAt > Date.now()) return this.bingSession
    const response = await fetch('https://cn.bing.com/translator', { headers: { 'user-agent': userAgent }, signal, dispatcher })
    if (!response.ok) throw new Error(`Bing session HTTP ${response.status}`)
    const html = await response.text()
    const ig = html.match(/IG:"([A-F0-9]{32})"/)?.[1]
    const abuse = html.match(/params_AbusePreventionHelper\s*=\s*\[(\d+),"([^"]+)",(\d+)\]/)
    if (!ig || !abuse) throw new Error('Bing session token unavailable')
    this.bingSession = { ig, key: abuse[1], token: abuse[2], expiresAt: Date.now() + Number(abuse[3]) - 60_000, requestCount: 0 }
    return this.bingSession
  }
}

export function buildRequestUrl(value: string, method: string, params: KeyValueEntry[]): string {
  const normalized = /^https?:\/\//i.test(value.trim()) ? value.trim() : `http://${value.trim()}`
  const url = new URL(normalized)
  if (method === 'GET' || method === 'HEAD' || method === 'OPTIONS') {
    for (const entry of enabledEntries(params)) url.searchParams.append(entry.name, entry.value)
  }
  return url.toString()
}

function buildHeaders(entries: KeyValueEntry[], cookies: HttpCookieEntry[]): Record<string, string> {
  const headers = Object.fromEntries(enabledEntries(entries).map((entry) => [entry.name, entry.value]))
  const cookie = cookies.filter((entry) => entry.enabled && entry.name.trim()).map((entry) => `${entry.name}=${entry.value}`).join('; ')
  if (cookie) headers.Cookie = cookie
  return headers
}

function buildRequestBody(method: string, body: string, bodyType: string, params: KeyValueEntry[], headers: Record<string, string>): string | undefined {
  if (method === 'GET' || method === 'HEAD' || method === 'OPTIONS') return undefined
  if (body) {
    if (!hasHeader(headers, 'content-type')) headers['Content-Type'] = bodyType || 'text/plain'
    return body
  }
  const form = new URLSearchParams(enabledEntries(params).map((entry) => [entry.name, entry.value]))
  if (!form.size) return undefined
  if (!hasHeader(headers, 'content-type')) headers['Content-Type'] = 'application/x-www-form-urlencoded'
  return form.toString()
}

const googleChunkConcurrency = 3

async function translateGoogle(text: string, sourceLang: string, targetLang: string, signal: AbortSignal, dispatcher?: Dispatcher): Promise<string> {
  const chunks = splitTranslationText(text, 1800)
  if (chunks.length === 1) return translateGoogleChunk(chunks[0], sourceLang, targetLang, signal, dispatcher)
  const results = await mapPool(chunks, googleChunkConcurrency, (chunk) => translateGoogleChunk(chunk, sourceLang, targetLang, signal, dispatcher))
  return results.join('')
}

async function translateGoogleChunk(chunk: string, sourceLang: string, targetLang: string, signal: AbortSignal, dispatcher?: Dispatcher): Promise<string> {
  const query = new URLSearchParams({ client: 'gtx', sl: googleLanguage(sourceLang), tl: googleLanguage(targetLang), dt: 't', q: chunk })
  const response = await fetch(`https://translate.googleapis.com/translate_a/single?${query}`, { headers: { 'user-agent': userAgent }, signal, dispatcher })
  if (!response.ok) throw new Error(`Google HTTP ${response.status}`)
  const payload = await response.json() as [Array<[string]>]
  const translated = payload[0]?.map((part) => part[0] || '').join('')
  if (!translated) throw new Error('Google returned no translation')
  return translated
}

async function mapPool<T, R>(items: T[], concurrency: number, mapper: (item: T, index: number) => Promise<R>): Promise<R[]> {
  const results = new Array<R>(items.length)
  let nextIndex = 0
  async function worker(): Promise<void> {
    while (nextIndex < items.length) {
      const index = nextIndex
      nextIndex += 1
      results[index] = await mapper(items[index], index)
    }
  }
  const workers = Math.min(Math.max(1, concurrency), items.length)
  await Promise.all(Array.from({ length: workers }, () => worker()))
  return results
}

export function splitTranslationText(text: string, maxLength: number): string[] {
  if (!Number.isInteger(maxLength) || maxLength < 2) throw new Error('Invalid translation chunk size')
  if (text.length <= maxLength) return [text]
  const chunks: string[] = []
  let offset = 0
  while (offset < text.length) {
    let end = Math.min(offset + maxLength, text.length)
    if (end < text.length) {
      const minimumNaturalBreak = offset + Math.floor(maxLength / 2)
      for (let cursor = end; cursor > minimumNaturalBreak; cursor -= 1) {
        if (/[\s.!?。！？,，;；:：]/u.test(text[cursor - 1])) {
          end = cursor
          break
        }
      }
      const previousCodeUnit = text.charCodeAt(end - 1)
      const nextCodeUnit = text.charCodeAt(end)
      if (previousCodeUnit >= 0xD800 && previousCodeUnit <= 0xDBFF && nextCodeUnit >= 0xDC00 && nextCodeUnit <= 0xDFFF) {
        end -= 1
      }
    }
    chunks.push(text.slice(offset, end))
    offset = end
  }
  return chunks
}

function enabledEntries(entries: KeyValueEntry[]): KeyValueEntry[] {
  return entries.filter((entry) => entry.enabled && entry.name.trim())
}

function hasHeader(headers: Record<string, string>, name: string): boolean {
  return Object.keys(headers).some((key) => key.toLowerCase() === name)
}

function formatHeaders(headers: Headers): string {
  return [...headers.entries()].map(([name, value]) => `${name}: ${value}`).join('\n')
}

async function readLimitedResponse(response: Awaited<ReturnType<typeof fetch>>): Promise<Uint8Array> {
  const contentLength = Number(response.headers.get('content-length') || 0)
  if (contentLength > maxResponseBytes) throw new Error('RESPONSE_TOO_LARGE')
  const bytes = new Uint8Array(await response.arrayBuffer())
  if (bytes.byteLength > maxResponseBytes) throw new Error('RESPONSE_TOO_LARGE')
  return bytes
}

function prettyJson(value: string): string {
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

function proxyDispatcherKey(proxy: ProxyConfiguration): string {
  return `${proxy.host.trim()}|${proxy.port}|${proxy.username}|${proxy.password}`
}

function createTranslationAgent(): Dispatcher {
  return new Agent({
    connect: { timeout: translationConnectTimeoutMs },
    bodyTimeout: translationBodyTimeoutMs,
    headersTimeout: translationBodyTimeoutMs,
    keepAliveTimeout: 60_000,
    keepAliveMaxTimeout: 60_000
  })
}

function createTranslationProxyDispatcher(proxy: ProxyConfiguration): Dispatcher {
  const uri = buildProxyUri(proxy)
  return new ProxyAgent({
    uri: uri.toString(),
    connect: { timeout: translationConnectTimeoutMs },
    bodyTimeout: translationBodyTimeoutMs,
    headersTimeout: translationBodyTimeoutMs,
    keepAliveTimeout: 60_000,
    keepAliveMaxTimeout: 60_000
  })
}

function createProxyDispatcher(proxy: ProxyConfiguration): Dispatcher | undefined {
  if (!proxy.enabled) return undefined
  return new ProxyAgent(buildProxyUri(proxy).toString())
}

function buildProxyUri(proxy: ProxyConfiguration): URL {
  const host = proxy.host.trim()
  const port = Number(proxy.port)
  if (!host || !Number.isInteger(port) || port < 1 || port > 65535) throw new Error('Invalid proxy settings')
  const uri = new URL(/^https?:\/\//i.test(host) ? host : `http://${host}`)
  uri.port = String(port)
  if (proxy.username) uri.username = proxy.username
  if (proxy.password) uri.password = proxy.password
  return uri
}

export function translationProviderOrder(preferred: TranslationProvider, skipPreferred: boolean): TranslationProvider[] {
  const alternate: TranslationProvider = preferred === 'google' ? 'bing' : 'google'
  return skipPreferred ? [alternate, preferred] : [preferred, alternate]
}

/** Parse Chromium `session.resolveProxy` output, preferring HTTP CONNECT over SOCKS (undici ProxyAgent). */
export function parseChromiumProxyDirective(value: string): { host: string; port: number } | null {
  const parts = value.split(';').map((part) => part.trim()).filter(Boolean)
  for (const part of parts) {
    if (/^DIRECT$/i.test(part)) continue
    const match = /^(PROXY|HTTPS|HTTP)\s+(\S+)$/i.exec(part)
    if (!match) continue
    const parsed = parseProxyHostPort(match[2])
    if (parsed) return parsed
  }
  return null
}

function parseProxyHostPort(endpoint: string): { host: string; port: number } | null {
  const value = endpoint.trim()
  const ipv6 = /^\[([^\]]+)\]:(\d+)$/.exec(value)
  if (ipv6) {
    const port = Number(ipv6[2])
    return Number.isInteger(port) && port >= 1 && port <= 65535 ? { host: ipv6[1], port } : null
  }
  const index = value.lastIndexOf(':')
  if (index <= 0) return null
  const host = value.slice(0, index).trim()
  const port = Number(value.slice(index + 1))
  if (!host || !Number.isInteger(port) || port < 1 || port > 65535) return null
  return { host, port }
}

function isRequestCancelled(signal: AbortSignal, error: unknown): boolean {
  if (!signal.aborted) return false
  const message = errorMessage(error)
  return message.includes('ABORTED') || message.includes('TIMEOUT')
}

function classifyNetworkError(error: unknown, signal: AbortSignal): HttpResponseResult['errorCode'] {
  const message = errorMessage(error)
  if (signal.aborted) return message.includes('TIMEOUT') ? 'TIMEOUT' : 'ABORTED'
  if (message.includes('RESPONSE_TOO_LARGE')) return 'RESPONSE_TOO_LARGE'
  if (message.includes('Invalid') || message.includes('URL')) return 'INVALID_REQUEST'
  return 'NETWORK'
}

function errorMessage(error: unknown): string {
  if (error instanceof Error) return error.cause instanceof Error ? `${error.message}: ${error.cause.message}` : error.message
  return String(error)
}

export function googleLanguage(code: string): string {
  return ({
    wyw: 'lzh',
    jp: 'ja',
    kor: 'ko',
    fra: 'fr',
    spa: 'es',
    ara: 'ar',
    bul: 'bg',
    est: 'et',
    dan: 'da',
    fin: 'fi',
    rom: 'ro',
    slo: 'sl',
    swe: 'sv',
    cht: 'zh-TW',
    vie: 'vi'
  } as Record<string, string>)[code] || code || 'auto'
}

export function bingLanguage(code: string, source: boolean): string {
  if (!code || code === 'auto') return source ? 'auto-detect' : 'zh-Hans'
  return ({
    'zh-CN': 'zh-Hans',
    cht: 'zh-Hant',
    wyw: 'lzh',
    jp: 'ja',
    kor: 'ko',
    fra: 'fr',
    spa: 'es',
    ara: 'ar',
    bul: 'bg',
    est: 'et',
    dan: 'da',
    fin: 'fi',
    rom: 'ro',
    slo: 'sl',
    swe: 'sv',
    vie: 'vi'
  } as Record<string, string>)[code] || code
}
