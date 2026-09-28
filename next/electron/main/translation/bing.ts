import { fetch, type Dispatcher } from 'undici'
import type { TranslationInput } from '../../../src/shared/contracts/network'
import type { TranslationContext, TranslationEngine } from './types'
const userAgent = 'Mozilla/5.0 (MooTool Next) AppleWebKit/537.36 Chrome/138 Safari/537.36'

type BingSession = { ig: string; key: string; token: string; expiresAt: number; requestCount: number }

export class BingTranslator implements TranslationEngine {
  readonly id = 'bing' as const
  private bingSession: BingSession | null = null
  async translate({ text, sourceLang, targetLang }: TranslationInput, { signal, dispatcher }: TranslationContext): Promise<string> {
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
