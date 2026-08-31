export const SETTINGS_SCHEMA_VERSION = 7 as const

export type AppLanguage = 'zh-CN' | 'en-US' | 'ja-JP'
export type ThemePreference = 'system' | 'light' | 'dark'
export type AccentColor = 'blue' | 'indigo' | 'teal' | 'orange'
export type CloseBehavior = 'ask' | 'minimizeToTray' | 'quit'
export type InterfaceDensity = 'compact' | 'comfortable'
export type ProxyMode = 'system' | 'direct' | 'manual'

export interface CustomToolGroup {
  id: string
  name: string
  toolIds: string[]
}

const PRODUCT_TOOL_IDS = new Set([
  'quick-note', 'text-diff', 'reformat', 'json', 'config', 'runtime', 'protobuf', 'variables',
  'http', 'host', 'network', 'ua', 'encode', 'crypto', 'regex', 'cron', 'qrcode', 'timestamp',
  'message-board', 'translation', 'calculator', 'color', 'image', 'pdf', 'system'
])

export interface AppSettings {
  schemaVersion: typeof SETTINGS_SCHEMA_VERSION
  revision: number
  general: {
    language: AppLanguage
    launchAtLogin: boolean
    closeBehavior: CloseBehavior
    autoCheckUpdates: boolean
    startMaximized: boolean
    trayEnabled: boolean
  }
  appearance: {
    theme: ThemePreference
    accentColor: AccentColor
    fontFamily: 'system' | 'mono'
    uiScale: 90 | 100 | 110
  }
  layout: {
    sidebarCompact: boolean
    density: InterfaceDensity
    customGroups: CustomToolGroup[]
    paneSizes: Record<string, number>
    showRecent: boolean
    showGroupTitles: boolean
    hiddenTools: string[]
  }
  editor: {
    fontSize: number
    tabSize: 2 | 4 | 8
    wordWrap: boolean
    jsonFontSize: number
    quickNoteFontSize: number
  }
  network: {
    timeoutSeconds: number
    proxyMode: ProxyMode
    proxyHost: string
    proxyPort: number
    proxyUsername: string
    translationTimeoutSeconds: number
  }
  runtime: {
    autoDetect: boolean
    javaPath: string
    groovyPath: string
    pythonPath: string
    nodePath: string
    environment: Record<string, string>
  }
  data: {
    historyLimit: number
  }
  vault: {
    autoCommit: boolean
    rootDirectory: string | null
  }
  shortcuts: {
    globalSearch: string
    settings: string
  }
  tools: {
    favorites: string[]
    recent: string[]
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

export function defaultAppSettings(): AppSettings {
  return {
    schemaVersion: SETTINGS_SCHEMA_VERSION,
    revision: 0,
    general: {
      language: 'zh-CN',
      launchAtLogin: false,
      closeBehavior: 'ask',
      autoCheckUpdates: true,
      startMaximized: false,
      trayEnabled: true
    },
    appearance: {
      theme: 'system',
      accentColor: 'blue',
      fontFamily: 'system',
      uiScale: 100
    },
    layout: {
      sidebarCompact: false,
      density: 'comfortable',
      customGroups: [],
      paneSizes: {},
      showRecent: true,
      showGroupTitles: true,
      hiddenTools: []
    },
    editor: {
      fontSize: 13,
      tabSize: 2,
      wordWrap: true,
      jsonFontSize: 13,
      quickNoteFontSize: 13
    },
    network: {
      timeoutSeconds: 30,
      proxyMode: 'system',
      proxyHost: '',
      proxyPort: 8080,
      proxyUsername: '',
      translationTimeoutSeconds: 20
    },
    runtime: {
      autoDetect: true,
      javaPath: '',
      groovyPath: '',
      pythonPath: '',
      nodePath: '',
      environment: {}
    },
    data: {
      historyLimit: 500
    },
    vault: {
      autoCommit: false,
      rootDirectory: null
    },
    shortcuts: {
      globalSearch: 'CommandOrControl+K',
      settings: 'CommandOrControl+,'
    },
    tools: {
      favorites: [],
      recent: []
    }
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
