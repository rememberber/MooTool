import { useMemo } from 'react'
import { useSettings } from '../features/settings/SettingsProvider'
import type { AppLanguage } from '../platform/contracts/settings'

export type MessageValues = Record<string, string | number>
export type LocalizedCatalog = Record<AppLanguage, Record<string, string>>

export function defineMessages<const Chinese extends Record<string, string>>(catalog: {
  'zh-CN': Chinese
  'en-US': { [Key in keyof Chinese]: string }
  'ja-JP': { [Key in keyof Chinese]: string }
}) {
  return catalog
}

export type LocalizedMessageKey<Catalog extends LocalizedCatalog> = Extract<keyof Catalog['zh-CN'], string>

export interface LocalizedTranslator<Catalog extends LocalizedCatalog> {
  locale: AppLanguage
  t(key: LocalizedMessageKey<Catalog>, values?: MessageValues): string
}

export function createLocalizedTranslator<Catalog extends LocalizedCatalog>(
  catalog: Catalog,
  locale: AppLanguage
): LocalizedTranslator<Catalog> {
  return {
    locale,
    t: (key, values) => formatMessage(catalog[locale][key], values)
  }
}

export function useLocalizedMessages<Catalog extends LocalizedCatalog>(
  catalog: Catalog
): LocalizedTranslator<Catalog> {
  const { settings } = useSettings()
  return useMemo(
    () => createLocalizedTranslator(catalog, settings.general.language),
    [catalog, settings.general.language]
  )
}

export function formatMessage(template: string, values?: MessageValues): string {
  if (!values) return template
  return template.replace(/\{(\w+)\}/g, (_, key: string) => String(values[key] ?? `{${key}}`))
}

export function validateMessageCatalog(catalog: LocalizedCatalog): string[] {
  const locales: AppLanguage[] = ['zh-CN', 'en-US', 'ja-JP']
  const expectedKeys = Object.keys(catalog['zh-CN']).sort()
  const errors: string[] = []
  for (const locale of locales) {
    const keys = Object.keys(catalog[locale]).sort()
    const missing = expectedKeys.filter((key) => !keys.includes(key))
    const extra = keys.filter((key) => !expectedKeys.includes(key))
    if (missing.length) errors.push(`${locale} is missing: ${missing.join(', ')}`)
    if (extra.length) errors.push(`${locale} has extra keys: ${extra.join(', ')}`)
    for (const key of expectedKeys) {
      const message = catalog[locale][key]
      if (!message?.trim()) {
        errors.push(`${locale}.${key} is empty`)
        continue
      }
      const expectedVariables = placeholders(catalog['zh-CN'][key] ?? '')
      const actualVariables = placeholders(message)
      if (expectedVariables.join('|') !== actualVariables.join('|')) {
        errors.push(`${locale}.${key} placeholders differ: expected ${expectedVariables.join(', ')}, received ${actualVariables.join(', ')}`)
      }
    }
  }
  return errors
}

function placeholders(value: string): string[] {
  return [...value.matchAll(/\{(\w+)\}/g)].map((match) => match[1]!).sort()
}
