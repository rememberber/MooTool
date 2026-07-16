export const appSettingsSchemaVersion = 3

export type AppLanguage = 'zh-CN' | 'en-US' | 'ja-JP'
export type ThemePreference = 'system' | 'light' | 'dark'
export type CloseBehavior = 'ask' | 'hide' | 'quit'
export type NavigationStyle = 'classic' | 'card' | 'grouped'
export type SecretKey = 'proxyPassword' | 'gitToken'
export type RuntimeSettingsId = 'java' | 'groovy' | 'python' | 'node'
export type RuntimeRunOption = { arguments: string; workingDirectory: string }

export type SecretStatus = {
  key: SecretKey
  stored: boolean
  encryptionAvailable: boolean
}

export type AppSettings = {
  schemaVersion: number
  general: {
    language: AppLanguage
    autoCheckUpdates: boolean
    startMaximized: boolean
    closeBehavior: CloseBehavior
    trayEnabled: boolean
  }
  appearance: {
    theme: ThemePreference
    accentColor: string
    fontFamily: string
    fontSize: number
    unifiedBackground: boolean
  }
  layout: {
    showRecent: boolean
    compactNavigation: boolean
    showSeparators: boolean
    hideNavigationTitles: boolean
    navigationStyle: NavigationStyle
  }
  editor: {
    sqlDialect: string
    jsonFontSize: number
    quickNoteFontSize: number
    softWrap: boolean
  }
  network: {
    proxyEnabled: boolean
    proxyHost: string
    proxyPort: string
    proxyUsername: string
    requestTimeoutMs: number
    translationTimeoutMs: number
  }
  data: {
    directory: string
  }
  vault: {
    quickNotePath: string
    jsonPath: string
    gitRemote: string
    gitUsername: string
    autoCommit: boolean
    autoPullMinutes: number
    hideGitignoredFiles: boolean
  }
  runtime: {
    javaPath: string
    groovyPath: string
    pythonPath: string
    nodePath: string
    drafts: Record<RuntimeSettingsId, string>
    options: Record<RuntimeSettingsId, RuntimeRunOption>
  }
  tools: {
    qrCodeSize: number
    qrErrorCorrection: 'L' | 'M' | 'Q' | 'H'
    randomStringLength: number
    exportDirectory: string
    translationProvider: 'google' | 'bing'
    translationSourceLang: string
    translationTargetLang: string
  }
  shortcuts: {
    search: string
    settings: string
  }
}

export type SettingsPatch = {
  [Key in keyof Omit<AppSettings, 'schemaVersion'>]?: Partial<AppSettings[Key]>
} & { schemaVersion?: number }

export const accentColorPresets = [
  { id: 'yellow', value: '#e0b22b', strongValue: '#b78300', contrastValue: '#241c00' },
  { id: 'coral', value: '#de8f7d', strongValue: '#cd6f5e', contrastValue: '#ffffff' },
  { id: 'blue', value: '#4f83cc', strongValue: '#3f6fae', contrastValue: '#ffffff' },
  { id: 'green', value: '#4e9275', strongValue: '#34755b', contrastValue: '#ffffff' },
  { id: 'red', value: '#c96761', strongValue: '#a84e49', contrastValue: '#ffffff' },
  { id: 'purple', value: '#8a72b5', strongValue: '#6f579c', contrastValue: '#ffffff' }
] as const

export const defaultAppSettings: AppSettings = {
  schemaVersion: appSettingsSchemaVersion,
  general: {
    language: 'zh-CN',
    autoCheckUpdates: true,
    startMaximized: false,
    closeBehavior: 'ask',
    trayEnabled: true
  },
  appearance: {
    theme: 'system',
    accentColor: 'yellow',
    fontFamily: 'system-ui',
    fontSize: 14,
    unifiedBackground: true
  },
  layout: {
    showRecent: true,
    compactNavigation: false,
    showSeparators: true,
    hideNavigationTitles: false,
    navigationStyle: 'grouped'
  },
  editor: {
    sqlDialect: 'Standard SQL',
    jsonFontSize: 14,
    quickNoteFontSize: 14,
    softWrap: true
  },
  network: {
    proxyEnabled: false,
    proxyHost: '',
    proxyPort: '',
    proxyUsername: '',
    requestTimeoutMs: 30_000,
    translationTimeoutMs: 15_000
  },
  data: {
    directory: ''
  },
  vault: {
    quickNotePath: '',
    jsonPath: '',
    gitRemote: '',
    gitUsername: '',
    autoCommit: false,
    autoPullMinutes: 0,
    hideGitignoredFiles: true
  },
  runtime: {
    javaPath: '',
    groovyPath: '',
    pythonPath: '',
    nodePath: '',
    drafts: { java: '', groovy: '', python: '', node: '' },
    options: {
      java: { arguments: '', workingDirectory: '' },
      groovy: { arguments: '', workingDirectory: '' },
      python: { arguments: '', workingDirectory: '' },
      node: { arguments: '', workingDirectory: '' }
    }
  },
  tools: {
    qrCodeSize: 300,
    qrErrorCorrection: 'M',
    randomStringLength: 16,
    exportDirectory: '',
    translationProvider: 'google',
    translationSourceLang: 'auto',
    translationTargetLang: 'zh-CN'
  },
  shortcuts: {
    search: 'CommandOrControl+K',
    settings: 'CommandOrControl+,'
  }
}

export function mergeSettings(current: AppSettings, patch: SettingsPatch): AppSettings {
  const merged: AppSettings = {
    schemaVersion: appSettingsSchemaVersion,
    general: { ...current.general, ...patch.general },
    appearance: { ...current.appearance, ...patch.appearance },
    layout: { ...current.layout, ...patch.layout },
    editor: { ...current.editor, ...patch.editor },
    network: { ...current.network, ...patch.network },
    data: { ...current.data, ...patch.data },
    vault: { ...current.vault, ...patch.vault },
    runtime: { ...current.runtime, ...patch.runtime },
    tools: { ...current.tools, ...patch.tools },
    shortcuts: { ...current.shortcuts, ...patch.shortcuts }
  }

  return normalizeSettings(merged)
}

export function normalizeSettings(value: AppSettings): AppSettings {
  const languages: AppLanguage[] = ['zh-CN', 'en-US', 'ja-JP']
  const themes: ThemePreference[] = ['system', 'light', 'dark']
  const closeBehaviors: CloseBehavior[] = ['ask', 'hide', 'quit']
  const navigationStyles: NavigationStyle[] = ['classic', 'card', 'grouped']
  const corrections: AppSettings['tools']['qrErrorCorrection'][] = ['L', 'M', 'Q', 'H']
  const translationProviders: AppSettings['tools']['translationProvider'][] = ['google', 'bing']

  return {
    ...value,
    schemaVersion: appSettingsSchemaVersion,
    general: {
      ...value.general,
      language: languages.includes(value.general.language) ? value.general.language : defaultAppSettings.general.language,
      closeBehavior: closeBehaviors.includes(value.general.closeBehavior) ? value.general.closeBehavior : defaultAppSettings.general.closeBehavior
    },
    appearance: {
      ...value.appearance,
      theme: themes.includes(value.appearance.theme) ? value.appearance.theme : defaultAppSettings.appearance.theme,
      accentColor: accentColorPresets.some((preset) => preset.id === value.appearance.accentColor)
        ? value.appearance.accentColor
        : defaultAppSettings.appearance.accentColor,
      fontSize: clampNumber(value.appearance.fontSize, 12, 18, defaultAppSettings.appearance.fontSize)
    },
    layout: {
      ...value.layout,
      navigationStyle: navigationStyles.includes(value.layout.navigationStyle)
        ? value.layout.navigationStyle
        : defaultAppSettings.layout.navigationStyle
    },
    editor: {
      ...value.editor,
      jsonFontSize: clampNumber(value.editor.jsonFontSize, 11, 24, defaultAppSettings.editor.jsonFontSize),
      quickNoteFontSize: clampNumber(value.editor.quickNoteFontSize, 11, 24, defaultAppSettings.editor.quickNoteFontSize)
    },
    network: {
      ...value.network,
      requestTimeoutMs: clampNumber(value.network.requestTimeoutMs, 1_000, 120_000, defaultAppSettings.network.requestTimeoutMs),
      translationTimeoutMs: clampNumber(value.network.translationTimeoutMs, 1_000, 120_000, defaultAppSettings.network.translationTimeoutMs)
    },
    runtime: {
      ...value.runtime,
      drafts: normalizeRuntimeDrafts(value.runtime.drafts),
      options: normalizeRuntimeOptions(value.runtime.options)
    },
    vault: {
      ...value.vault,
      autoPullMinutes: clampNumber(value.vault.autoPullMinutes, 0, 1440, defaultAppSettings.vault.autoPullMinutes)
    },
    tools: {
      ...value.tools,
      qrCodeSize: clampNumber(value.tools.qrCodeSize, 120, 2000, defaultAppSettings.tools.qrCodeSize),
      qrErrorCorrection: corrections.includes(value.tools.qrErrorCorrection)
        ? value.tools.qrErrorCorrection
        : defaultAppSettings.tools.qrErrorCorrection,
      randomStringLength: clampNumber(value.tools.randomStringLength, 1, 4096, defaultAppSettings.tools.randomStringLength),
      translationProvider: translationProviders.includes(value.tools.translationProvider) ? value.tools.translationProvider : defaultAppSettings.tools.translationProvider,
      translationSourceLang: normalizeLanguageCode(value.tools.translationSourceLang, defaultAppSettings.tools.translationSourceLang),
      translationTargetLang: normalizeLanguageCode(value.tools.translationTargetLang, defaultAppSettings.tools.translationTargetLang)
    }
  }
}

function normalizeRuntimeDrafts(value: Record<RuntimeSettingsId, string> | undefined): Record<RuntimeSettingsId, string> {
  return Object.fromEntries((['java', 'groovy', 'python', 'node'] as RuntimeSettingsId[]).map((id) => [
    id,
    typeof value?.[id] === 'string' ? value[id].slice(0, 1024 * 1024) : ''
  ])) as Record<RuntimeSettingsId, string>
}

function normalizeRuntimeOptions(value: Record<RuntimeSettingsId, RuntimeRunOption> | undefined): Record<RuntimeSettingsId, RuntimeRunOption> {
  return Object.fromEntries((['java', 'groovy', 'python', 'node'] as RuntimeSettingsId[]).map((id) => [id, {
    arguments: typeof value?.[id]?.arguments === 'string' ? value[id].arguments.slice(0, 2000) : '',
    workingDirectory: typeof value?.[id]?.workingDirectory === 'string' ? value[id].workingDirectory.slice(0, 1000) : ''
  }])) as Record<RuntimeSettingsId, RuntimeRunOption>
}

function normalizeLanguageCode(value: string, fallback: string): string {
  return typeof value === 'string' && /^[a-zA-Z-]{2,12}$/.test(value) ? value : fallback
}

function clampNumber(value: number, minimum: number, maximum: number, fallback: number): number {
  if (!Number.isFinite(value)) {
    return fallback
  }
  return Math.min(maximum, Math.max(minimum, Math.round(value)))
}
