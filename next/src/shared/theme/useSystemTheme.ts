import { useEffect, useState } from 'react'
import { useSettings } from '@/features/settings/SettingsProvider'
import { accentColorPresets } from '@/shared/contracts/settings'

export type ResolvedTheme = 'light' | 'dark'

export function useSystemTheme(): ResolvedTheme {
  const { settings } = useSettings()
  const [systemTheme, setSystemTheme] = useState<ResolvedTheme>(() => {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  })

  useEffect(() => {
    let cleanupElectronListener: (() => void) | undefined
    let cancelled = false

    window.mootool?.getSystemTheme?.().then((systemTheme) => {
      if (!cancelled) {
        setSystemTheme(systemTheme)
      }
    })

    cleanupElectronListener = window.mootool?.onSystemThemeChange?.((systemTheme) => {
      setSystemTheme(systemTheme)
    })

    const media = window.matchMedia('(prefers-color-scheme: dark)')
    const handleMediaChange = (event: MediaQueryListEvent) => {
      setSystemTheme(event.matches ? 'dark' : 'light')
    }
    media.addEventListener('change', handleMediaChange)

    return () => {
      cancelled = true
      cleanupElectronListener?.()
      media.removeEventListener('change', handleMediaChange)
    }
  }, [])

  const theme = settings.appearance.theme === 'system' ? systemTheme : settings.appearance.theme

  useEffect(() => {
    const accent = accentColorPresets.find((preset) => preset.id === settings.appearance.accentColor)
      ?? accentColorPresets[0]
    document.documentElement.dataset.theme = theme
    document.documentElement.style.colorScheme = theme
    document.documentElement.style.setProperty('--accent', accent.value)
    document.documentElement.style.setProperty('--accent-strong', accent.strongValue)
    document.documentElement.style.setProperty('--accent-contrast', accent.contrastValue)
    document.documentElement.style.setProperty('--app-font-size', `${settings.appearance.fontSize}px`)
    document.documentElement.style.setProperty('--app-font-family', settings.appearance.fontFamily)
  }, [settings.appearance.accentColor, settings.appearance.fontFamily, settings.appearance.fontSize, theme])

  return theme
}
