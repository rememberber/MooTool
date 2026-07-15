import { useEffect, useState } from 'react'

export type ResolvedTheme = 'light' | 'dark'

export function useSystemTheme(): ResolvedTheme {
  const [theme, setTheme] = useState<ResolvedTheme>(() => {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  })

  useEffect(() => {
    let cleanupElectronListener: (() => void) | undefined
    let cancelled = false

    window.mootool?.getSystemTheme?.().then((systemTheme) => {
      if (!cancelled) {
        setTheme(systemTheme)
      }
    })

    cleanupElectronListener = window.mootool?.onSystemThemeChange?.((systemTheme) => {
      setTheme(systemTheme)
    })

    const media = window.matchMedia('(prefers-color-scheme: dark)')
    const handleMediaChange = (event: MediaQueryListEvent) => {
      setTheme(event.matches ? 'dark' : 'light')
    }
    media.addEventListener('change', handleMediaChange)

    return () => {
      cancelled = true
      cleanupElectronListener?.()
      media.removeEventListener('change', handleMediaChange)
    }
  }, [])

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    document.documentElement.style.colorScheme = theme
  }, [theme])

  return theme
}
