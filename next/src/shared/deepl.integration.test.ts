import { afterEach, describe, expect, it, vi } from 'vitest'
import { MockAgent } from 'undici'
import { DeepLTranslator, deepLLanguage } from '../../electron/main/translation/deepl'
import { GoogleTranslator } from '../../electron/main/translation/google'
import { BingTranslator } from '../../electron/main/translation/bing'
import { NetworkService, translationProviderOrder } from '../../electron/main/networkService'
import { normalizeTranslationInput } from '../../electron/main/p5Validation'
import { defaultAppSettings, mergeSettings } from './contracts/settings'
import type { TranslationInput } from './contracts/network'

const agents: MockAgent[] = []
function mockAgent() { const agent = new MockAgent(); agent.disableNetConnect(); agents.push(agent); return agent }
const input: TranslationInput = { requestId: 'deepl-test', text: 'Hello', sourceLang: 'auto', targetLang: 'zh-CN', preferredProvider: 'deepl', timeoutMs: 3000 }
const proxy = { enabled: false, host: '', port: '', username: '', password: '' }
afterEach(async () => { vi.restoreAllMocks(); await Promise.all(agents.splice(0).map((agent) => agent.close())) })

describe('DeepL adapter', () => {
  it.each([['test:fx', 'https://api-free.deepl.com'], ['test-pro', 'https://api.deepl.com']])('routes %s with header authentication and auto detection', async (key, origin) => {
    const agent = mockAgent()
    agent.get(origin).intercept({ path: '/v2/translate', method: 'POST', headers: { authorization: `DeepL-Auth-Key ${key}` },
      body: JSON.stringify({ text: ['Hello'], target_lang: 'ZH-HANS', preserve_formatting: true })
    }).reply(200, { translations: [{ text: '你好' }] })
    expect(await new DeepLTranslator().translate(input, { signal: new AbortController().signal, dispatcher: agent, credentials: { deeplApiKey: ` ${key} ` } })).toBe('你好')
    agent.assertNoPendingInterceptors()
  })
  it.each([[403, 'DEEPL_INVALID_KEY'], [456, 'DEEPL_QUOTA_EXCEEDED'], [429, 'DEEPL_RATE_LIMITED'], [500, 'DeepL HTTP 500']])('reports status %s without leaking response contents', async (status, message) => {
    const agent = mockAgent()
    agent.get('https://api-free.deepl.com').intercept({ path: '/v2/translate', method: 'POST' }).reply(status, { message: 'secret-must-not-be-shown' })
    await expect(new DeepLTranslator().translate(input, { signal: new AbortController().signal, dispatcher: agent, credentials: { deeplApiKey: 'test:fx' } })).rejects.toThrow(message)
  })
  it('maps legacy language codes and rejects unsupported selections', () => {
    expect(deepLLanguage('jp', false)).toBe('JA')
    expect(deepLLanguage('fra', true)).toBe('FR')
    expect(deepLLanguage('cht', true)).toBe('ZH')
    expect(deepLLanguage('cht', false)).toBe('ZH-HANT')
    expect(() => deepLLanguage('wyw', false)).toThrow('DEEPL_UNSUPPORTED_LANGUAGE')
    expect(() => deepLLanguage('auto', false)).toThrow('DEEPL_UNSUPPORTED_LANGUAGE')
  })
  it('chunks large input into bounded requests in order without losing source text', async () => {
    const agent = mockAgent()
    const received: string[] = []
    agent.get('https://api-free.deepl.com').intercept({ path: '/v2/translate', method: 'POST' }).reply(200, (options) => {
      expect(Buffer.byteLength(String(options.body))).toBeLessThan(128 * 1024)
      const body = JSON.parse(String(options.body)); received.push(body.text[0])
      expect(body.source_lang).toBe('EN')
      return { translations: [{ text: String(received.length) }] }
    }).times(3)
    const text = '😀 hello '.repeat(3000)
    const result = await new DeepLTranslator().translate({ ...input, text, sourceLang: 'en' }, { signal: new AbortController().signal, dispatcher: agent, credentials: { deeplApiKey: 'test:fx' } })
    expect(received.join('')).toBe(text)
    expect(result).toBe('123')
    agent.assertNoPendingInterceptors()
  })
  it('honors cancellation before sending and rejects malformed responses', async () => {
    const agent = mockAgent()
    const controller = new AbortController(); controller.abort(new Error('ABORTED'))
    await expect(new DeepLTranslator().translate(input, { signal: controller.signal, dispatcher: agent, credentials: { deeplApiKey: 'test:fx' } })).rejects.toThrow('ABORTED')
    agent.get('https://api-free.deepl.com').intercept({ path: '/v2/translate', method: 'POST' }).reply(200, { translations: [] })
    await expect(new DeepLTranslator().translate(input, { signal: new AbortController().signal, dispatcher: agent, credentials: { deeplApiKey: 'test:fx' } })).rejects.toThrow('DEEPL_EMPTY_RESPONSE')
  })
})

describe('engine integration', () => {
  it('preserves DeepL through settings and IPC validation without accepting renderer credentials', () => {
    expect(mergeSettings(defaultAppSettings, { tools: { translationProvider: 'deepl' } }).tools.translationProvider).toBe('deepl')
    expect(normalizeTranslationInput({ ...input, deeplApiKey: 'untrusted' })).toEqual(input)
  })
  it('never falls back from DeepL or selects it as an automatic fallback', async () => {
    const google = vi.spyOn(GoogleTranslator.prototype, 'translate')
    const bing = vi.spyOn(BingTranslator.prototype, 'translate')
    const service = new NetworkService()
    await expect(service.translate(input, proxy)).rejects.toThrow('DEEPL_KEY_REQUIRED')
    await expect(service.translate(input, proxy)).rejects.toThrow('DEEPL_KEY_REQUIRED')
    expect(google).not.toHaveBeenCalled(); expect(bing).not.toHaveBeenCalled()
    expect(translationProviderOrder('deepl', true)).toEqual(['deepl'])
    expect(translationProviderOrder('google', false)).toEqual(['google', 'bing'])
  })
  it('retains Google/Bing fallback and reports the actual provider', async () => {
    vi.spyOn(GoogleTranslator.prototype, 'translate').mockRejectedValue(new Error('offline'))
    vi.spyOn(BingTranslator.prototype, 'translate').mockResolvedValue('你好')
    expect(await new NetworkService().translate({ ...input, preferredProvider: 'google' }, proxy)).toMatchObject({ text: '你好', provider: 'bing', fallbackUsed: true })
  })
})
