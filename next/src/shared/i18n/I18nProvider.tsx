import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
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
  const [language, setLanguageState] = useState<Language>(() => detectInitialLanguage())

  const value = useMemo<I18nContextValue>(() => {
    return {
      language,
      setLanguage: (nextLanguage) => {
        window.localStorage.setItem('mootool.language', nextLanguage)
        setLanguageState(nextLanguage)
      },
      languageLabels,
      languages,
      t: (key, params) => interpolate(messages[language][key] ?? messages['zh-CN'][key], params)
    }
  }, [language])

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}

export function useI18n(): I18nContextValue {
  const context = useContext(I18nContext)
  if (!context) {
    throw new Error('useI18n must be used inside I18nProvider')
  }
  return context
}

function detectInitialLanguage(): Language {
  const savedLanguage = window.localStorage.getItem('mootool.language')
  if (isLanguage(savedLanguage)) {
    return savedLanguage
  }

  const browserLanguage = window.navigator.language
  if (browserLanguage.startsWith('zh')) {
    return 'zh-CN'
  }
  if (browserLanguage.startsWith('ja')) {
    return 'ja-JP'
  }
  return 'en-US'
}

function isLanguage(value: string | null): value is Language {
  return languages.includes(value as Language)
}

function interpolate(message: string, params?: Record<string, string>): string {
  if (!params) {
    return message
  }
  return Object.entries(params).reduce((result, [key, value]) => result.replaceAll(`{${key}}`, value), message)
}
