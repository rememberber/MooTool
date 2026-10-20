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
  const parts = ['curl', '-X', request.method, shellQuote(request.url)]
  for (const header of request.headers.filter(active)) parts.push('-H', shellQuote(`${header.name}: ${header.value}`))
  const cookieValue = request.cookies.filter(active).map((item) => `${item.name}=${item.value}`).join('; ')
  if (cookieValue) parts.push('-b', shellQuote(cookieValue))
  if (request.body) parts.push('--data-raw', shellQuote(request.body))
  return parts.join(' ')
}

export async function formatHttpBody(content: string, bodyType: string): Promise<string> {
  if (!content.trim()) return ''
  const language = httpBodyLanguage(bodyType)
  if (language === 'json') return JSON.stringify(JSON.parse(content), null, 2)
  if (language === 'xml') {
    return formatWithPlugin(content, 'xml', () => import('@prettier/plugin-xml'), { xmlWhitespaceSensitivity: 'preserve' })
  }
  if (language === 'html') return formatWithPlugin(content, 'html', () => import('prettier/plugins/html'))
  if (language === 'javascript') {
    return formatWithPlugin(content, 'babel', () => import('prettier/plugins/babel'), {}, true)
  }
  return content.split(/\r?\n/).map((line) => line.trimEnd()).join('\n').trimEnd()
}

function httpBodyLanguage(bodyType: string): 'json' | 'xml' | 'html' | 'javascript' | 'text' {
  const normalized = bodyType.trim().toLocaleLowerCase()
  if (normalized === 'json' || normalized === 'application/json' || normalized.endsWith('+json')) return 'json'
  if (normalized === 'xml' || normalized === 'text/xml' || normalized === 'application/xml' || normalized.endsWith('+xml')) return 'xml'
  if (normalized === 'html' || normalized === 'text/html') return 'html'
  if (['javascript', 'js', 'text/javascript', 'application/javascript'].includes(normalized)) return 'javascript'
  return 'text'
}

async function formatWithPlugin(
  content: string,
  parser: string,
  loadPlugin: () => Promise<{ default: import('prettier').Plugin }>,
  parserOptions: Record<string, unknown> = {},
  needsEstree = false
): Promise<string> {
  const [{ format }, parserPlugin, estreePlugin] = await Promise.all([
    import('prettier/standalone'),
    loadPlugin(),
    needsEstree ? import('prettier/plugins/estree') : Promise.resolve(null)
  ])
  return (await format(content, {
    parser,
    plugins: [parserPlugin.default, ...(estreePlugin ? [estreePlugin.default] : [])],
    printWidth: 100,
    tabWidth: 2,
    useTabs: false,
    endOfLine: 'lf',
    ...parserOptions
  })).trimEnd()
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
