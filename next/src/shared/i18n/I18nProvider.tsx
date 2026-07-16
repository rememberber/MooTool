import { createContext, use, useMemo, type ReactNode } from 'react'
import { useSettings } from '@/features/settings/SettingsProvider'
import { type Language, type MessageKey, languageLabels, languages, messages } from './messages'

type I18nContextValue = {
  language: Language
  setLanguage: (language: Language) => void
  languageLabels: Record<Language, string>
  languages: readonly Language[]
  t: (key: MessageKey, params?: Record<string, string>) => string
}

const I18nContext = createContext<I18nContextValue | null>(null)

export function I18nProvider({ children }: { children: ReactNode }) {
  const { settings, updateSettings } = useSettings()
  const language = settings.general.language

  const value = useMemo<I18nContextValue>(() => {
    return {
      language,
      setLanguage: (nextLanguage) => {
        void updateSettings({ general: { language: nextLanguage } })
      },
      languageLabels,
      languages,
      t: (key, params) => interpolate(messages[language][key] ?? messages['zh-CN'][key], params)
    }
  }, [language, updateSettings])

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}

export function useI18n(): I18nContextValue {
  const context = use(I18nContext)
  if (!context) {
    throw new Error('useI18n must be used inside I18nProvider')
  }
  return context
}

function interpolate(message: string, params?: Record<string, string>): string {
  if (!params) {
    return message
  }
  return Object.entries(params).reduce((result, [key, value]) => result.replaceAll(`{${key}}`, value), message)
}
