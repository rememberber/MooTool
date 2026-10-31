import { invoke } from '@tauri-apps/api/core'
import { listen } from '@tauri-apps/api/event'
import {
  defaultAppSettings,
  normalizeCloseBehavior,
  normalizeCustomGroups,
  normalizeEditorSettings,
  normalizeInterfaceFontFamily,
  normalizeLayoutSettings,
  normalizeNetworkSettings,
  normalizeShortcutSettings,
  normalizePaneSizes,
  SETTINGS_SCHEMA_VERSION,
  type AppSettings,
  type SettingsApi,
  type SettingsListener,
  type UnlistenSettings
} from '../contracts/settings'

export const SETTINGS_CHANGED_EVENT = 'mootool://settings-changed'
const BROWSER_SETTINGS_KEY = 'mootool-next-tauri:settings'

type Invoke = <T>(command: string, args?: Record<string, unknown>) => Promise<T>
type Listen = (
  event: string,
  handler: (event: { payload: AppSettings }) => void
) => Promise<UnlistenSettings>

export function createSettingsApi(
  invokeCommand: Invoke = invoke,
  listenEvent: Listen = listen
): SettingsApi {
  return {
    get: () => invokeCommand<AppSettings>('get_settings'),
    update: (settings) => invokeCommand<AppSettings>('update_settings', { settings }),
    reset: () => invokeCommand<AppSettings>('reset_settings'),
    openWindow: () => invokeCommand<void>('open_settings_window'),
    subscribe: (listener) => listenEvent(
      SETTINGS_CHANGED_EVENT,
      (event) => listener(event.payload)
    )
  }
}

function loadBrowserSettings(): AppSettings {
  try {
    const saved = window.localStorage.getItem(BROWSER_SETTINGS_KEY)
    if (!saved) return defaultAppSettings()
    const parsed = JSON.parse(saved) as Partial<AppSettings>
    const defaults = defaultAppSettings()
    return {
      ...defaults,
      ...parsed,
      general: {
        ...defaults.general,
        ...parsed.general,
        closeBehavior: normalizeCloseBehavior(parsed.general?.closeBehavior),
        escapeHidesWindow: parsed.general?.escapeHidesWindow !== false
      },
      appearance: {
        ...defaults.appearance,
        theme: parsed.appearance?.theme ?? defaults.appearance.theme,
        interfaceStyle: parsed.appearance?.interfaceStyle ?? defaults.appearance.interfaceStyle,
        unifiedBackground: parsed.appearance?.unifiedBackground ?? defaults.appearance.unifiedBackground,
        accentColor: parsed.appearance?.accentColor ?? defaults.appearance.accentColor,
        fontFamily: normalizeInterfaceFontFamily(parsed.appearance?.fontFamily),
        fontSize: parsed.appearance?.fontSize ?? defaults.appearance.fontSize
      },
      layout: normalizeLayoutSettings(parsed.layout),
      editor: normalizeEditorSettings(parsed.editor),
      network: normalizeNetworkSettings(parsed.network),
      runtime: { ...defaults.runtime, ...parsed.runtime },
      data: {},
      vault: {
        ...defaults.vault,
        ...parsed.vault,
        quickNoteTreeExpandMode: parsed.vault?.quickNoteTreeExpandMode === 'collapseAll' ? 'collapseAll' : 'expandAll',
        jsonTreeExpandMode: parsed.vault?.jsonTreeExpandMode === 'collapseAll' ? 'collapseAll' : 'expandAll'
      },
      shortcuts: normalizeShortcutSettings(parsed.shortcuts),
      tools: { ...defaults.tools, ...parsed.tools },
      schemaVersion: SETTINGS_SCHEMA_VERSION
    }
  } catch {
    return defaultAppSettings()
  }
}

function saveBrowserSettings(settings: AppSettings): AppSettings {
  const saved = {
    ...settings,
    schemaVersion: SETTINGS_SCHEMA_VERSION,
    revision: settings.revision + 1,
    layout: {
      ...settings.layout,
      customGroups: normalizeCustomGroups(settings.layout.customGroups),
      paneSizes: normalizePaneSizes(settings.layout.paneSizes)
    }
  }
  window.localStorage.setItem(BROWSER_SETTINGS_KEY, JSON.stringify(saved))
  window.dispatchEvent(new CustomEvent<AppSettings>(SETTINGS_CHANGED_EVENT, { detail: saved }))
  return saved
}

function subscribeBrowserSettings(listener: SettingsListener): Promise<UnlistenSettings> {
  const handleCustomEvent = (event: Event) => {
    listener((event as CustomEvent<AppSettings>).detail)
  }
  const handleStorage = (event: StorageEvent) => {
    if (event.key === BROWSER_SETTINGS_KEY && event.newValue) {
      listener(JSON.parse(event.newValue) as AppSettings)
    }
  }
  window.addEventListener(SETTINGS_CHANGED_EVENT, handleCustomEvent)
  window.addEventListener('storage', handleStorage)
  return Promise.resolve(() => {
    window.removeEventListener(SETTINGS_CHANGED_EVENT, handleCustomEvent)
    window.removeEventListener('storage', handleStorage)
  })
}

function createBrowserSettingsApi(): SettingsApi {
  return {
    get: async () => loadBrowserSettings(),
    update: async (settings) => saveBrowserSettings(settings),
    reset: async () => saveBrowserSettings(defaultAppSettings()),
    openWindow: async () => {
      window.open(`${window.location.pathname}?surface=settings`, 'mootool-settings', 'width=760,height=720')
    },
    subscribe: subscribeBrowserSettings
  }
}

export const settingsApi: SettingsApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? createSettingsApi()
  : createBrowserSettingsApi()
