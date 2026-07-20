import { createServer } from 'node:http'
import { afterAll, beforeAll, describe, expect, it } from 'vitest'
import { NetworkService, bingLanguage, buildRequestUrl, googleLanguage, parseChromiumProxyDirective, splitTranslationText, translationProviderOrder } from '../../electron/main/networkService'
import { normalizeTranslationLanguageCode, normalizeTranslationLanguagePair, type HttpRequestDraft } from './contracts/network'

let baseUrl = ''
const server = createServer((request, response) => {
  const chunks: Buffer[] = []
  request.on('data', (chunk: Buffer) => chunks.push(chunk))
  request.on('end', () => {
    response.setHeader('content-type', 'application/json')
    response.setHeader('set-cookie', ['session=abc; Path=/', 'mode=test; Path=/'])
    response.end(JSON.stringify({ method: request.method, url: request.url, header: request.headers['x-mootool'], body: Buffer.concat(chunks).toString('utf8') }))
  })
})

beforeAll(async () => {
  await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve))
  const address = server.address()
  if (!address || typeof address === 'string') throw new Error('Test server failed to bind')
  baseUrl = `http://127.0.0.1:${address.port}`
})

afterAll(async () => {
  await new Promise<void>((resolve, reject) => server.close((error) => error ? reject(error) : resolve()))
})

describe('NetworkService', () => {
  it('builds query params and sends a request through the main-process service', async () => {
    const request: HttpRequestDraft = {
      name: 'Local request', method: 'GET', url: `${baseUrl}/inspect`, body: '', bodyType: 'application/json',
      params: [{ id: 'p1', name: 'query', value: 'Moo Tool', enabled: true }],
      headers: [{ id: 'h1', name: 'X-MooTool', value: 'Next', enabled: true }],
      cookies: [{ id: 'c1', name: 'client', value: 'desktop', enabled: true, domain: '', path: '/', expires: '' }]
    }
    const result = await new NetworkService().sendHttp({ requestId: 'local-get', request, timeoutMs: 3_000 }, { enabled: false, host: '', port: '', username: '', password: '' })

    expect(result.ok).toBe(true)
    expect(result.status).toBe(200)
    expect(JSON.parse(result.body)).toMatchObject({ method: 'GET', url: '/inspect?query=Moo+Tool', header: 'Next' })
    expect(result.cookies).toContain('session=abc')
  })

  it('uses form params for a body when no explicit body is present', async () => {
    const request: HttpRequestDraft = {
      name: 'Form request', method: 'POST', url: `${baseUrl}/form`, body: '', bodyType: 'application/json',
      params: [{ id: 'p1', name: 'name', value: 'Moo Tool', enabled: true }], headers: [], cookies: []
    }
    const result = await new NetworkService().sendHttp({ requestId: 'local-post', request, timeoutMs: 3_000 }, { enabled: false, host: '', port: '', username: '', password: '' })
    expect(JSON.parse(result.body).body).toBe('name=Moo+Tool')
  })
})

describe('network helpers', () => {
  it('normalizes schemes and excludes disabled params', () => {
    expect(buildRequestUrl('example.com/path', 'GET', [
      { id: '1', name: 'a', value: '1', enabled: true },
      { id: '2', name: 'b', value: '2', enabled: false }
    ])).toBe('http://example.com/path?a=1')
  })

  it('splits long translation text without losing content', () => {
    expect(() => splitTranslationText('text', 1)).toThrow('Invalid translation chunk size')
    const text = `${'a'.repeat(8)}\n${'b'.repeat(8)}`
    expect(splitTranslationText(text, 10).join('')).toBe(text)
    expect(splitTranslationText(text, 10).every((chunk) => chunk.length <= 10)).toBe(true)

    const words = 'hello '.repeat(10)
    const wordChunks = splitTranslationText(words, 20)
    expect(wordChunks.join('')).toBe(words)
    expect(wordChunks.slice(0, -1).every((chunk) => /\s$/u.test(chunk))).toBe(true)

    const emoji = `${'a'.repeat(9)}😀${'b'.repeat(9)}`
    const emojiChunks = splitTranslationText(emoji, 10)
    expect(emojiChunks.join('')).toBe(emoji)
    expect(emojiChunks.every((chunk) => chunk.length <= 10)).toBe(true)
    expect(emojiChunks[0]).toBe('a'.repeat(9))
    expect(emojiChunks[1].startsWith('😀')).toBe(true)
  })

  it('converts legacy language codes for Google and Bing', () => {
    const commonMappings = {
      wyw: 'lzh', jp: 'ja', kor: 'ko', fra: 'fr', spa: 'es', ara: 'ar', bul: 'bg', est: 'et',
      dan: 'da', fin: 'fi', rom: 'ro', slo: 'sl', swe: 'sv', vie: 'vi'
    }
    for (const [legacyCode, providerCode] of Object.entries(commonMappings)) {
      expect(googleLanguage(legacyCode)).toBe(providerCode)
      expect(bingLanguage(legacyCode, false)).toBe(providerCode)
    }
    expect(googleLanguage('cht')).toBe('zh-TW')
    expect(bingLanguage('cht', false)).toBe('zh-Hant')
    expect(bingLanguage('zh-CN', false)).toBe('zh-Hans')
    expect(bingLanguage('auto', true)).toBe('auto-detect')
  })

  it('normalizes persisted localized language names', () => {
    expect(normalizeTranslationLanguageCode('English', 'auto')).toBe('en')
    expect(normalizeTranslationLanguageCode('英语', 'auto')).toBe('en')
    expect(normalizeTranslationLanguageCode('英語', 'auto')).toBe('en')
    expect(normalizeTranslationLanguageCode('Romanian', 'auto')).toBe('rom')
    expect(normalizeTranslationLanguageCode('auto', 'zh-CN', false)).toBe('zh-CN')
    expect(normalizeTranslationLanguagePair('English', 'en')).toEqual({ sourceLang: 'auto', targetLang: 'en' })
  })

  it('orders translation providers with sticky skip for an unreachable preferred engine', () => {
    expect(translationProviderOrder('google', false)).toEqual(['google', 'bing'])
    expect(translationProviderOrder('google', true)).toEqual(['bing', 'google'])
    expect(translationProviderOrder('bing', false)).toEqual(['bing', 'google'])
    expect(translationProviderOrder('bing', true)).toEqual(['google', 'bing'])
  })

  it('parses Chromium resolveProxy directives for HTTP CONNECT proxies', () => {
    expect(parseChromiumProxyDirective('DIRECT')).toBeNull()
    expect(parseChromiumProxyDirective('PROXY 127.0.0.1:9674')).toEqual({ host: '127.0.0.1', port: 9674 })
    expect(parseChromiumProxyDirective('HTTPS 127.0.0.1:9674; SOCKS 127.0.0.1:9674')).toEqual({ host: '127.0.0.1', port: 9674 })
    expect(parseChromiumProxyDirective('SOCKS5 127.0.0.1:9674')).toBeNull()
    expect(parseChromiumProxyDirective('PROXY proxy.example:8080; PROXY backup:8080')).toEqual({ host: 'proxy.example', port: 8080 })
  })
})
