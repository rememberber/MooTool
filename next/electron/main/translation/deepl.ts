import { fetch } from 'undici'
import type { TranslationInput } from '../../../src/shared/contracts/network'
import { supportsTranslationLanguage } from '../../../src/shared/contracts/translationEngines'
import { splitTranslationText } from './text'
import type { TranslationContext, TranslationEngine } from './types'

export function deepLLanguage(code: string, source: boolean): string | undefined {
  if (!supportsTranslationLanguage('deepl', code, source)) throw new Error('DEEPL_UNSUPPORTED_LANGUAGE')
  if (code === 'auto') return undefined
  if (code === 'zh-CN') return source ? 'ZH' : 'ZH-HANS'
  if (code === 'cht') return source ? 'ZH' : 'ZH-HANT'
  return ({ jp: 'JA', kor: 'KO', fra: 'FR', spa: 'ES', ara: 'AR', bul: 'BG', est: 'ET', dan: 'DA', fin: 'FI', rom: 'RO', slo: 'SL', swe: 'SV', vie: 'VI' } as Record<string, string>)[code] ?? code.toUpperCase()
}

export class DeepLTranslator implements TranslationEngine {
  readonly id = 'deepl' as const

  async translate(input: TranslationInput, { signal, dispatcher, credentials }: TranslationContext): Promise<string> {
    const key = credentials.deeplApiKey?.trim()
    if (!key) throw new Error('DEEPL_KEY_REQUIRED')
    const source = deepLLanguage(input.sourceLang, true)
    const target = deepLLanguage(input.targetLang, false)
    const host = deepLApiOrigin(key)
    const results: string[] = []
    // JSON escaping can use six bytes per UTF-16 code unit. Keep each body below 128 KiB.
    for (const text of splitTranslationText(input.text, 12_000)) {
      signal.throwIfAborted()
      const response = await fetch(`${host}/v2/translate`, {
        method: 'POST',
        headers: { Authorization: `DeepL-Auth-Key ${key}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({ text: [text], source_lang: source, target_lang: target, preserve_formatting: true }),
        signal, dispatcher, redirect: 'error'
      })
      if (!response.ok) {
        // Do not surface remote bodies: they may echo credentials or user text.
        await response.body?.cancel()
        if (response.status === 403 || response.status === 401) throw new Error('DEEPL_INVALID_KEY')
        if (response.status === 456) throw new Error('DEEPL_QUOTA_EXCEEDED')
        if (response.status === 429) throw new Error('DEEPL_RATE_LIMITED')
        throw new Error(`DeepL HTTP ${response.status}`)
      }
      const payload = await response.json() as { translations?: Array<{ text?: unknown }> }
      const translated = payload.translations?.[0]?.text
      if (typeof translated !== 'string' || !translated) throw new Error('DEEPL_EMPTY_RESPONSE')
      results.push(translated)
    }
    return results.join('')
  }
}

export function deepLApiOrigin(key: string): string {
  return key.trim().endsWith(':fx') ? 'https://api-free.deepl.com' : 'https://api.deepl.com'
}
