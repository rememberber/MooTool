import { splitTranslationText } from './text'
import { fetch, type Dispatcher } from 'undici'
import type { TranslationInput } from '../../../src/shared/contracts/network'
import type { TranslationContext, TranslationEngine } from './types'
const userAgent = 'Mozilla/5.0 (MooTool Next) AppleWebKit/537.36 Chrome/138 Safari/537.36'

export class GoogleTranslator implements TranslationEngine {
  readonly id = 'google' as const
  async translate(input: TranslationInput, context: TranslationContext): Promise<string> {
    return translateGoogle(input.text, input.sourceLang, input.targetLang, context.signal, context.dispatcher)
  }
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

