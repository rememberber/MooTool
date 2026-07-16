import { createContext, use, useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import {
  defaultAppSettings,
  mergeSettings,
  type AppSettings,
  type SettingsPatch
} from '@/shared/contracts/settings'

type SettingsContextValue = {
  settings: AppSettings
  ready: boolean
  updateSettings: (patch: SettingsPatch) => Promise<AppSettings>
}

const SettingsContext = createContext<SettingsContextValue | null>(null)

export function SettingsProvider({ children }: { children: ReactNode }) {
  const [settings, setSettings] = useState(defaultAppSettings)
  const [ready, setReady] = useState(false)

  useEffect(() => {
    let cancelled = false
    const unsubscribe = window.mootool.onSettingsChange((nextSettings) => {
      if (!cancelled) {
        setSettings(nextSettings)
        setReady(true)
      }
    })

    window.mootool.getSettings()
      .then((nextSettings) => {
        if (!cancelled) {
          setSettings(nextSettings)
          setReady(true)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setReady(true)
        }
      })

    return () => {
      cancelled = true
      unsubscribe()
    }
  }, [])

  const updateSettings = useCallback(async (patch: SettingsPatch) => {
    setSettings((current) => mergeSettings(current, patch))
    try {
      const saved = await window.mootool.updateSettings(patch)
      setSettings(saved)
      return saved
    } catch (error) {
      const restored = await window.mootool.getSettings().catch(() => defaultAppSettings)
      setSettings(restored)
      throw error
    }
  }, [])

  const value = useMemo<SettingsContextValue>(() => ({ settings, ready, updateSettings }), [ready, settings, updateSettings])
  return <SettingsContext.Provider value={value}>{children}</SettingsContext.Provider>
}

export function useSettings(): SettingsContextValue {
  const context = use(SettingsContext)
  if (!context) {
    throw new Error('useSettings must be used inside SettingsProvider')
  }
  return context
}
