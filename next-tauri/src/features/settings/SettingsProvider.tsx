import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type PropsWithChildren
} from 'react'
import { settingsApi } from '../../platform/api/settingsApi'
import {
  defaultAppSettings,
  type AppSettings
} from '../../platform/contracts/settings'
import { errorMessage } from '../../shared/errors'
import { configureVaultCheckpoints } from './vaultCheckpoints'
import { accentPresets, resolveTheme } from './appearance'
import { cssFontFamily } from '../../shared/fonts/systemFonts'

interface SettingsContextValue {
  settings: AppSettings
  ready: boolean
  error: string
  save(next: AppSettings | ((current: AppSettings) => AppSettings)): Promise<AppSettings>
  reset(): Promise<AppSettings>
  openWindow(): Promise<void>
}

const SettingsContext = createContext<SettingsContextValue | undefined>(undefined)

export function SettingsProvider({ children }: PropsWithChildren) {
  const [settings, setSettings] = useState(defaultAppSettings)
  const [ready, setReady] = useState(false)
  const [error, setError] = useState('')
  const settingsRef = useRef(settings)

  const accept = useCallback((next: AppSettings) => {
    settingsRef.current = next
    setSettings(next)
    setReady(true)
    setError('')
  }, [])

  useEffect(() => {
    let unlisten: (() => void) | undefined
    let cancelled = false
    void settingsApi.get()
      .then((next) => {
        if (!cancelled) accept(next)
      })
      .catch((cause: unknown) => {
        if (!cancelled) {
          setReady(true)
          setError(errorMessage(cause))
        }
      })
    void settingsApi.subscribe((next) => {
      if (!cancelled && next.revision >= settingsRef.current.revision) accept(next)
    }).then((dispose) => {
      if (cancelled) dispose()
      else unlisten = dispose
    }).catch((cause: unknown) => {
      if (!cancelled) setError(errorMessage(cause))
    })
    return () => {
      cancelled = true
      unlisten?.()
    }
  }, [accept])

  useEffect(() => applyDocumentAppearance(settings), [settings])

  const save = useCallback(async (
    next: AppSettings | ((current: AppSettings) => AppSettings)
  ) => {
    const candidate = typeof next === 'function' ? next(settingsRef.current) : next
    try {
      const saved = await settingsApi.update(candidate)
      accept(saved)
      return saved
    } catch (cause) {
      const message = errorMessage(cause)
      setError(message)
      throw new Error(message)
    }
  }, [accept])

  const reset = useCallback(async () => {
    try {
      const saved = await settingsApi.reset()
      accept(saved)
      return saved
    } catch (cause) {
      const message = errorMessage(cause)
      setError(message)
      throw new Error(message)
    }
  }, [accept])

  const openWindow = useCallback(async () => {
    try {
      await settingsApi.openWindow()
    } catch (cause) {
      const message = errorMessage(cause)
      setError(message)
      throw new Error(message)
    }
  }, [])

  useEffect(() => {
    if (!ready) return
    configureVaultCheckpoints(settings)
  }, [ready, settings])

  const value = useMemo<SettingsContextValue>(() => ({
    settings,
    ready,
    error,
    save,
    reset,
    openWindow
  }), [error, openWindow, ready, reset, save, settings])

  return <SettingsContext.Provider value={value}>{children}</SettingsContext.Provider>
}

export function useSettings(): SettingsContextValue {
  const value = useContext(SettingsContext)
  if (!value) throw new Error('useSettings must be used inside SettingsProvider')
  return value
}

function applyDocumentAppearance(settings: AppSettings): () => void {
  const root = document.documentElement
  const media = window.matchMedia('(prefers-color-scheme: dark)')
  const applyTheme = () => {
    const resolved = resolveTheme(settings.appearance.theme, media.matches)
    const accent = accentPresets[settings.appearance.accentColor]
    const fontSize = Math.min(18, Math.max(12, settings.appearance.fontSize || 13))
    root.dataset.theme = resolved
    root.dataset.accent = settings.appearance.accentColor
    root.dataset.interfaceStyle = settings.appearance.interfaceStyle
    root.lang = settings.general.language
    root.style.colorScheme = resolved
    root.style.setProperty('--accent', accent.value)
    root.style.setProperty('--accent-strong', accent.strong)
    root.style.setProperty('--accent-contrast', accent.contrast)
    root.style.setProperty('--app-font-size', `${fontSize}px`)
    root.style.setProperty('--json-editor-font-size', `${settings.editor.jsonFontSize}px`)
    root.style.setProperty('--quick-note-editor-font-size', `${settings.editor.quickNoteFontSize}px`)
    root.style.setProperty('--json-editor-font-family', cssFontFamily(settings.editor.jsonFontName))
    root.style.setProperty('--quick-note-editor-font-family', cssFontFamily(settings.editor.quickNoteFontName))
    root.style.setProperty('--app-font-family', settings.appearance.fontFamily)
    if (window.__TAURI_INTERNALS__) {
      void import('@tauri-apps/api/window')
        .then(({ getCurrentWindow }) => getCurrentWindow().setTheme(resolved))
        .catch(() => undefined)
    }
  }
  applyTheme()
  media.addEventListener('change', applyTheme)
  return () => media.removeEventListener('change', applyTheme)
}
