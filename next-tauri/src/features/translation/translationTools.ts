export const translationLanguages = [
  'auto', 'zh-CN', 'cht', 'en', 'jp', 'kor', 'fra', 'de', 'spa', 'it', 'pt', 'ru',
  'ara', 'th', 'vie', 'nl', 'pl', 'swe', 'fin', 'dan', 'cs', 'el', 'hu', 'rom',
  'slo', 'bul', 'est', 'yue', 'wyw'
] as const

const displayLanguageCodes: Record<string, string> = {
  'zh-CN': 'zh-Hans', cht: 'zh-Hant', en: 'en', jp: 'ja', kor: 'ko', fra: 'fr', de: 'de',
  spa: 'es', it: 'it', pt: 'pt', ru: 'ru', ara: 'ar', th: 'th', vie: 'vi', nl: 'nl',
  pl: 'pl', swe: 'sv', fin: 'fi', dan: 'da', cs: 'cs', el: 'el', hu: 'hu', rom: 'ro',
  slo: 'sl', bul: 'bg', est: 'et', yue: 'yue', wyw: 'lzh'
}

export function languageLabel(code: string, locale = 'en-US', autoLabel = 'Auto detect'): string {
  if (code === 'auto') return autoLabel
  const displayCode = displayLanguageCodes[code]
  if (!displayCode) return code
  return new Intl.DisplayNames([locale], { type: 'language' }).of(displayCode) ?? code
}

export function alternateTargetLanguage(code: string): string {
  return code === 'zh-CN' ? 'en' : 'zh-CN'
}

export function includesTranslationQuery(
  item: { sourceText: string; targetText: string; sourceLang: string; targetLang: string; remark?: string },
  query: string
): boolean {
  const normalized = query.trim().toLocaleLowerCase()
  if (!normalized) return true
  return [item.sourceText, item.targetText, item.sourceLang, item.targetLang, item.remark ?? '']
    .some((value) => value.toLocaleLowerCase().includes(normalized))
}
