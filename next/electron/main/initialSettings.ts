import { defaultAppSettings, mergeSettings, type AppLanguage, type AppSettings } from '../../src/shared/contracts/settings'

/** Used only as store defaults: persisted settings take precedence on later launches. */
export function createInitialSettings(preferredSystemLanguages: readonly string[]): AppSettings {
  const supportedLanguages = new Map<string, AppLanguage>([
    ['zh', 'zh-CN'],
    ['en', 'en-US'],
    ['ja', 'ja-JP']
  ])
  let language: AppLanguage = 'en-US'
  for (const locale of preferredSystemLanguages) {
    const code = locale.trim().toLowerCase().split(/[-_]/)[0]
    const supported = supportedLanguages.get(code)
    if (supported) {
      language = supported
      break
    }
  }
  return mergeSettings(defaultAppSettings, { general: { language } })
}
