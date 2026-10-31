export const SETTINGS_SCHEMA_VERSION = 12 as const

export type NavigationStyle = 'classic' | 'card' | 'grouped'

export type AppLanguage = 'zh-CN' | 'en-US' | 'ja-JP'
export type ThemePreference = 'system' | 'light' | 'dark'
export type InterfaceStyle = 'modern' | 'quiet' | 'hero' | 'smartisan' | 'miui-v5' | 'claude'
export type AccentColor =
  | 'blue'
  | 'indigo'
  | 'teal'
  | 'orange'
  | 'yellow'
  | 'coral'
  | 'green'
  | 'red'
  | 'purple'
export type CloseBehavior = 'ask' | 'hide' | 'quit'
export type RuntimeSettingsId = 'java' | 'groovy' | 'python' | 'node'
export type TranslationProvider = 'google' | 'bing' | 'deepl'
export type VaultTreeExpandMode = 'expandAll' | 'collapseAll'

export interface RuntimeRunOption {
  argumentsText: string
  workingDirectory: string
}

export interface CustomToolGroup {
  id: string
  name: string
  toolIds: string[]
}

const PRODUCT_TOOL_IDS = new Set([
  'quick-note', 'text-diff', 'reformat', 'json', 'config', 'runtime', 'protobuf', 'variables',
  'http', 'host', 'network', 'ua', 'encode', 'crypto', 'regex', 'cron', 'qrcode', 'timestamp',
  'message-board', 'translation', 'calculator', 'color', 'image', 'pdf', 'file-preview', 'batch-rename', 'system'
])

export interface AppSettings {
  schemaVersion: typeof SETTINGS_SCHEMA_VERSION
  revision: number
  general: {
    language: AppLanguage
    launchAtLogin: boolean
    closeBehavior: CloseBehavior
    autoCheckUpdates: boolean
    autoDownloadUpdates: boolean
    startMaximized: boolean
    trayEnabled: boolean
    escapeHidesWindow: boolean
  }
  appearance: {
    theme: ThemePreference
    interfaceStyle: InterfaceStyle
    unifiedBackground: boolean
    accentColor: AccentColor
    fontFamily: string
    fontSize: number
  }
  layout: {
    hideNavigationTitles: boolean
    navigationStyle: NavigationStyle
    compactNavigation: boolean
    showSeparators: boolean
    customGroups: CustomToolGroup[]
    paneSizes: Record<string, number[]>
    showRecent: boolean
    hiddenNavigationToolIds: string[]
  }
  editor: {
    softWrap: boolean
    jsonFontName: string
    jsonFontSize: number
    quickNoteFontName: string
    quickNoteFontSize: number
    sqlDialect: string
  }
  network: {
    requestTimeoutMs: number
    proxyEnabled: boolean
    proxyHost: string
    proxyPort: string
    proxyUsername: string
    translationTimeoutMs: number
  }
  runtime: {
    autoDetect: boolean
    javaPath: string
    groovyPath: string
    pythonPath: string
    nodePath: string
    environment: Record<string, string>
    drafts: Record<RuntimeSettingsId, string>
    options: Record<RuntimeSettingsId, RuntimeRunOption>
  }
  data: Record<string, never>
  vault: {
    autoCommit: boolean
    rootDirectory: string | null
    gitRemote: string
    gitUsername: string
    autoCommitIdleSeconds: number
    autoCommitInactiveSeconds: number
    autoPullMinutes: number
    hideGitignoredFiles: boolean
    quickNoteTreeExpandMode: VaultTreeExpandMode
    jsonTreeExpandMode: VaultTreeExpandMode
  }
  shortcuts: {
    search: string
    settings: string
    globalSearchEnabled: boolean
    globalSearch: string
  }
  tools: {
    favorites: string[]
    recent: string[]
    qrCodeSize: number
    qrErrorCorrection: 'L' | 'M' | 'Q' | 'H'
    randomStringLength: number
    exportDirectory: string
    translationProvider: TranslationProvider
    translationSourceLang: string
    translationTargetLang: string
  }
}

export type SettingsListener = (settings: AppSettings) => void
export type UnlistenSettings = () => void

export interface SettingsApi {
  get(): Promise<AppSettings>
  update(settings: AppSettings): Promise<AppSettings>
  reset(): Promise<AppSettings>
  openWindow(): Promise<void>
  subscribe(listener: SettingsListener): Promise<UnlistenSettings>
}

export function normalizeCloseBehavior(value: unknown): CloseBehavior {
  if (value === 'hide' || value === 'minimizeToTray') return 'hide'
  if (value === 'quit') return 'quit'
  return 'ask'
}

export function defaultAppSettings(): AppSettings {
  return {
    schemaVersion: SETTINGS_SCHEMA_VERSION,
    revision: 0,
    general: {
      language: 'zh-CN',
      launchAtLogin: false,
      closeBehavior: 'ask',
      autoCheckUpdates: true,
      autoDownloadUpdates: true,
      startMaximized: false,
      trayEnabled: true,
      escapeHidesWindow: true
    },
    appearance: {
      theme: 'system',
      interfaceStyle: 'modern',
      unifiedBackground: true,
      accentColor: 'blue',
      fontFamily: 'system-ui',
      fontSize: 13
    },
    layout: {
      hideNavigationTitles: false,
      navigationStyle: 'classic',
      compactNavigation: false,
      showSeparators: true,
      customGroups: [],
      paneSizes: {},
      showRecent: false,
      hiddenNavigationToolIds: []
    },
    editor: {
      softWrap: true,
      jsonFontName: 'ui-monospace',
      jsonFontSize: 14,
      quickNoteFontName: 'ui-monospace',
      quickNoteFontSize: 14,
      sqlDialect: 'Standard SQL'
    },
    network: {
      requestTimeoutMs: 30_000,
      proxyEnabled: false,
      proxyHost: '',
      proxyPort: '',
      proxyUsername: '',
      translationTimeoutMs: 15_000
    },
    runtime: {
      autoDetect: true,
      javaPath: '',
      groovyPath: '',
      pythonPath: '',
      nodePath: '',
      environment: {},
      drafts: { java: '', groovy: '', python: '', node: '' },
      options: {
        java: { argumentsText: '', workingDirectory: '' },
        groovy: { argumentsText: '', workingDirectory: '' },
        python: { argumentsText: '', workingDirectory: '' },
        node: { argumentsText: '', workingDirectory: '' }
      }
    },
    data: {},
    vault: {
      autoCommit: false,
      rootDirectory: null,
      gitRemote: '',
      gitUsername: '',
      autoCommitIdleSeconds: 30,
      autoCommitInactiveSeconds: 120,
      autoPullMinutes: 0,
      hideGitignoredFiles: true,
      quickNoteTreeExpandMode: 'expandAll',
      jsonTreeExpandMode: 'expandAll'
    },
    shortcuts: {
      search: 'CommandOrControl+K',
      settings: 'CommandOrControl+,',
      globalSearchEnabled: false,
      globalSearch: 'CommandOrControl+Shift+Space'
    },
    tools: {
      favorites: [],
      recent: [],
      qrCodeSize: 300,
      qrErrorCorrection: 'M',
      randomStringLength: 16,
      exportDirectory: '',
      translationProvider: 'google',
      translationSourceLang: 'auto',
      translationTargetLang: 'zh-CN'
    }
  }
}

export const sqlDialects = [
  'Standard SQL',
  'MySQL',
  'MariaDB',
  'PostgreSQL',
  'Oracle PL/SQL',
  'SQL Server Transact-SQL',
  'IBM DB2',
  'Couchbase N1QL',
  'Amazon Redshift',
  'Spark'
] as const

const NAVIGATION_STYLES: NavigationStyle[] = ['classic', 'card', 'grouped']

export function normalizePaneSizeList(sizes: readonly number[]): number[] {
  const total = sizes.reduce((sum, size) => sum + size, 0)
  if (!(total > 0) || sizes.some((size) => !Number.isFinite(size) || size <= 0)) {
    return sizes.map(() => Number((1 / Math.max(sizes.length, 1)).toFixed(6)))
  }
  return sizes.map((size) => Number((size / total).toFixed(6)))
}

export function storedPaneSizes(
  paneSizes: Record<string, number[]>,
  id: string,
  legacyId: string | undefined,
  count: number
): number[] | null {
  const current = paneSizes[id]
  if (current?.length === count) return current
  const legacy = legacyId ? paneSizes[legacyId] : undefined
  return legacy?.length === count ? legacy : null
}

export function normalizePaneSizes(value: unknown): Record<string, number[]> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return {}
  return Object.fromEntries(Object.entries(value as Record<string, unknown>)
    .slice(0, 64)
    .flatMap(([key, sizes]) => {
      if (!/^[a-z0-9-]{1,64}$/i.test(key) || !Array.isArray(sizes) || sizes.length < 2 || sizes.length > 4) return []
      const validSizes = sizes.map(Number)
      if (validSizes.some((size) => !Number.isFinite(size) || size <= 0)) return []
      return [[key, normalizePaneSizeList(validSizes)]]
    }))
}

export function normalizeShortcutSettings(value: unknown): AppSettings['shortcuts'] {
  const defaults = defaultAppSettings().shortcuts
  const record = value && typeof value === 'object' ? value as Record<string, unknown> : {}
  const legacyPalette = typeof record.globalSearchEnabled !== 'boolean'
  const search = typeof record.search === 'string'
    ? record.search
    : legacyPalette && typeof record.globalSearch === 'string'
      ? record.globalSearch
      : defaults.search
  return {
    search,
    settings: typeof record.settings === 'string' ? record.settings : defaults.settings,
    globalSearchEnabled: record.globalSearchEnabled === true,
    globalSearch: legacyPalette ? defaults.globalSearch : typeof record.globalSearch === 'string' ? record.globalSearch : defaults.globalSearch
  }
}

export function normalizeLayoutSettings(value: unknown): AppSettings['layout'] {
  const defaults = defaultAppSettings().layout
  const record = value && typeof value === 'object' ? value as Record<string, unknown> : {}
  const legacyHidden = record.hiddenNavigationToolIds ?? record.hiddenTools
  const hiddenNavigationToolIds = Array.isArray(legacyHidden)
    ? [...new Set(legacyHidden.filter((toolId): toolId is string => typeof toolId === 'string' && PRODUCT_TOOL_IDS.has(toolId)))]
    : []
  return {
    ...defaults,
    navigationStyle: normalizeNavigationStyle(record.navigationStyle),
    compactNavigation: record.compactNavigation === true,
    showSeparators: record.showSeparators !== false,
    customGroups: normalizeCustomGroups(record.customGroups),
    paneSizes: normalizePaneSizes(record.paneSizes),
    showRecent: record.showRecent === true,
    hideNavigationTitles: typeof record.hideNavigationTitles === 'boolean'
      ? record.hideNavigationTitles
      : record.sidebarCompact === true,
    hiddenNavigationToolIds
  }
}

export function normalizeNavigationStyle(value: unknown): NavigationStyle {
  return typeof value === 'string' && NAVIGATION_STYLES.includes(value as NavigationStyle)
    ? value as NavigationStyle
    : 'classic'
}

export function normalizeInterfaceFontFamily(value: unknown): string {
  if (typeof value !== 'string') return 'system-ui'
  const trimmed = value.trim().slice(0, 120)
  if (!trimmed || trimmed === 'system') return 'system-ui'
  if (trimmed === 'mono') return 'ui-monospace'
  return trimmed
}

export function normalizeEditorFontName(value: unknown): string {
  if (typeof value !== 'string') return 'ui-monospace'
  const trimmed = value.trim().slice(0, 120)
  if (!trimmed || trimmed === 'mono') return 'ui-monospace'
  if (trimmed === 'system') return 'system-ui'
  return trimmed
}

export function normalizeEditorSettings(editor: unknown): AppSettings['editor'] {
  const defaults = defaultAppSettings().editor
  const legacy = editor && typeof editor === 'object' ? editor as Partial<AppSettings['editor']> & { jsonFontFamily?: unknown; quickNoteFontFamily?: unknown; wordWrap?: unknown; fontSize?: unknown; tabSize?: unknown } : undefined
  const softWrap = typeof legacy?.softWrap === 'boolean'
    ? legacy.softWrap
    : typeof legacy?.wordWrap === 'boolean'
      ? legacy.wordWrap
      : defaults.softWrap
  const next = {
    ...defaults,
    ...legacy,
    jsonFontName: normalizeEditorFontName(legacy?.jsonFontName ?? legacy?.jsonFontFamily),
    quickNoteFontName: normalizeEditorFontName(legacy?.quickNoteFontName ?? legacy?.quickNoteFontFamily),
    softWrap
  }
  delete (next as { jsonFontFamily?: unknown }).jsonFontFamily
  delete (next as { quickNoteFontFamily?: unknown }).quickNoteFontFamily
  delete (next as { wordWrap?: unknown }).wordWrap
  delete (next as { fontSize?: unknown }).fontSize
  delete (next as { tabSize?: unknown }).tabSize
  return next
}

function clampTimeoutMs(value: unknown, fallback: number): number {
  const number = typeof value === 'number' && Number.isFinite(value) ? Math.round(value) : fallback
  return Math.max(1_000, Math.min(120_000, number))
}

export function normalizeNetworkSettings(value: unknown): AppSettings['network'] {
  const defaults = defaultAppSettings().network
  const record = value && typeof value === 'object' ? value as Record<string, unknown> : {}
  const enabled = typeof record.proxyEnabled === 'boolean' ? record.proxyEnabled : record.proxyMode === 'manual'
  let proxyPort = ''
  if (typeof record.proxyPort === 'string') proxyPort = record.proxyPort.trim().slice(0, 16)
  else if (typeof record.proxyPort === 'number' && Number.isFinite(record.proxyPort)) {
    proxyPort = !enabled && record.proxyPort === 8080 ? '' : String(record.proxyPort)
  }
  const requestTimeoutMs = typeof record.requestTimeoutMs === 'number'
    ? record.requestTimeoutMs
    : typeof record.timeoutSeconds === 'number' ? record.timeoutSeconds * 1_000 : undefined
  const translationTimeoutMs = typeof record.translationTimeoutMs === 'number'
    ? record.translationTimeoutMs
    : typeof record.translationTimeoutSeconds === 'number' ? record.translationTimeoutSeconds * 1_000 : undefined
  return {
    ...defaults,
    requestTimeoutMs: clampTimeoutMs(requestTimeoutMs, defaults.requestTimeoutMs),
    translationTimeoutMs: clampTimeoutMs(translationTimeoutMs, defaults.translationTimeoutMs),
    proxyEnabled: enabled,
    proxyHost: typeof record.proxyHost === 'string' ? record.proxyHost : defaults.proxyHost,
    proxyPort,
    proxyUsername: typeof record.proxyUsername === 'string' ? record.proxyUsername : defaults.proxyUsername
  }
}

export function normalizeCustomGroups(value: unknown): CustomToolGroup[] {
  if (!Array.isArray(value)) return []
  const groupIds = new Set<string>()
  const groups: CustomToolGroup[] = []
  for (const candidate of value.slice(0, 12)) {
    if (!candidate || typeof candidate !== 'object' || Array.isArray(candidate)) continue
    const record = candidate as Record<string, unknown>
    const id = typeof record.id === 'string' ? record.id : ''
    const name = typeof record.name === 'string' ? record.name.trim() : ''
    if (!/^[a-z0-9-]{1,64}$/.test(id) || groupIds.has(id) || !name || name.length > 40) continue
    const toolIds = Array.isArray(record.toolIds)
      ? [...new Set(record.toolIds.filter((toolId): toolId is string => (
          typeof toolId === 'string' && PRODUCT_TOOL_IDS.has(toolId)
        )))].slice(0, PRODUCT_TOOL_IDS.size)
      : []
    if (!toolIds.length) continue
    groupIds.add(id)
    groups.push({ id, name, toolIds })
  }
  return groups
}
