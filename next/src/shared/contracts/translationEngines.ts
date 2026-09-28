import { translationLanguageCodes, type TranslationLanguageCode, type TranslationProvider } from './network'

export type TranslationEngineInfo = {
  id: TranslationProvider
  name: string
  requiresApiKey: boolean
  automatic: boolean
  sourceLanguages: readonly TranslationLanguageCode[]
  targetLanguages: readonly TranslationLanguageCode[]
}

const targets = translationLanguageCodes.filter((code) => code !== 'auto')
const deepLLanguages = translationLanguageCodes.filter((code) => code !== 'yue' && code !== 'wyw')
export const translationEngines: Record<TranslationProvider, TranslationEngineInfo> = {
  google: { id: 'google', name: 'Google', requiresApiKey: false, automatic: true, sourceLanguages: translationLanguageCodes, targetLanguages: targets },
  bing: { id: 'bing', name: 'Bing', requiresApiKey: false, automatic: true, sourceLanguages: translationLanguageCodes, targetLanguages: targets },
  deepl: { id: 'deepl', name: 'DeepL', requiresApiKey: true, automatic: false, sourceLanguages: deepLLanguages, targetLanguages: deepLLanguages.filter((code) => code !== 'auto') }
}

export function supportsTranslationLanguage(provider: TranslationProvider, code: string, source: boolean): boolean {
  const languages = source ? translationEngines[provider].sourceLanguages : translationEngines[provider].targetLanguages
  return languages.some((language) => language === code)
}
