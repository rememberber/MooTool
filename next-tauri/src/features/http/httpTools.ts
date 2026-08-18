import type { HttpCookieEntry, HttpEntry, HttpRequestSpec } from '../../platform/contracts/http'

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE' | 'HEAD' | 'OPTIONS'
export type HttpToolErrorCode = 'curlStart' | 'curlHeader' | 'curlUrl' | 'httpMethod' | 'curlUnclosed'

export class HttpToolError extends Error {
  constructor(readonly code: HttpToolErrorCode, readonly values?: Record<string, string>) {
    super(`HTTP_TOOL_${code}`)
    this.name = 'HttpToolError'
  }
}

export function emptyHttpRequest(timeoutMs = 30_000, name = ''): HttpRequestSpec {
  return {
    requestId: makeId(),
    name,
    method: 'GET',
    url: '',
    params: [],
    headers: [],
    cookies: [],
    body: '',
    bodyType: 'application/json',
    timeoutMs,
    followRedirects: true
  }
}

export function parseCurl(command: string, timeoutMs = 30_000): HttpRequestSpec {
  const tokens = tokenize(command.trim())
  const curlIndex = tokens.findIndex((token) => token === 'curl' || token.endsWith('/curl'))
  if (curlIndex < 0) throw new HttpToolError('curlStart')
  let method: HttpMethod = 'GET'
  let url = ''
  let body = ''
  let bodyType = 'application/json'
  const headers: HttpEntry[] = []
  const cookies: HttpCookieEntry[] = []
  for (let index = curlIndex + 1; index < tokens.length; index += 1) {
    const token = tokens[index]
    const next = tokens[index + 1]
    if (['-X', '--request'].includes(token) && next) { method = next.toUpperCase() as HttpMethod; index += 1; continue }
    if (['-H', '--header'].includes(token) && next) {
      const [name, ...parts] = next.split(':')
      if (!name || !parts.length) throw new HttpToolError('curlHeader')
      const value = parts.join(':').trim()
      headers.push(entry(name.trim(), value))
      if (name.toLowerCase() === 'content-type') bodyType = value
      index += 1
      continue
    }
    if (['-d', '--data', '--data-raw', '--data-binary', '--data-urlencode'].includes(token) && next) {
      body = next
      if (method === 'GET') method = 'POST'
      index += 1
      continue
    }
    if (['-b', '--cookie'].includes(token) && next) {
      for (const item of next.split(';')) {
        const [name, ...parts] = item.trim().split('=')
        if (name) cookies.push(cookie(name, parts.join('=')))
      }
      index += 1
      continue
    }
    if (token === '--url' && next) { url = next; index += 1; continue }
    if (!token.startsWith('-') && !url) url = token
  }
  if (!url) throw new HttpToolError('curlUrl')
  if (!['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS'].includes(method)) {
    throw new HttpToolError('httpMethod', { method })
  }
  return { ...emptyHttpRequest(timeoutMs), method, url, headers, cookies, body, bodyType }
}

export function buildCurl(request: HttpRequestSpec): string {
  const url = new URL(request.url || 'https://example.com')
  for (const parameter of request.params.filter(active)) url.searchParams.append(parameter.name.trim(), parameter.value)
  const parts = ['curl', '-X', request.method, shellQuote(url.toString())]
  for (const header of request.headers.filter(active)) parts.push('-H', shellQuote(`${header.name}: ${header.value}`))
  const cookieValue = request.cookies.filter(active).map((item) => `${item.name}=${item.value}`).join('; ')
  if (cookieValue) parts.push('-b', shellQuote(cookieValue))
  if (request.body) parts.push('--data-raw', shellQuote(request.body))
  return parts.join(' ')
}

export function entry(name = '', value = ''): HttpEntry {
  return { id: makeId(), name, value, enabled: true }
}

export function cookie(name = '', value = ''): HttpCookieEntry {
  return { ...entry(name, value), domain: '', path: '/', expires: '' }
}

function tokenize(value: string): string[] {
  const tokens: string[] = []
  let token = ''
  let quote: 'single' | 'double' | null = null
  let escaped = false
  for (const character of value) {
    if (escaped) { token += character; escaped = false; continue }
    if (character === '\\' && quote !== 'single') { escaped = true; continue }
    if (character === "'" && quote !== 'double') { quote = quote === 'single' ? null : 'single'; continue }
    if (character === '"' && quote !== 'single') { quote = quote === 'double' ? null : 'double'; continue }
    if (/\s/.test(character) && !quote) {
      if (token) { tokens.push(token); token = '' }
      continue
    }
    token += character
  }
  if (escaped || quote) throw new HttpToolError('curlUnclosed')
  if (token) tokens.push(token)
  return tokens
}

function shellQuote(value: string): string {
  return `'${value.replace(/'/g, `'"'"'`)}'`
}

function active(value: HttpEntry): boolean {
  return value.enabled && Boolean(value.name.trim())
}

function makeId(): string {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`
}
