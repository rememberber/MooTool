/// <reference types="vite/client" />

interface Window {
  mootool: {
    platform: string
    getAppVersion: () => Promise<string>
    getSystemTheme: () => Promise<'light' | 'dark'>
    onSystemThemeChange: (callback: (theme: 'light' | 'dark') => void) => () => void
  }
}
