import { contextBridge, ipcRenderer } from 'electron'
import type { IpcRendererEvent } from 'electron'

contextBridge.exposeInMainWorld('mootool', {
  platform: process.platform,
  getAppVersion: () => ipcRenderer.invoke('app:get-version'),
  getSystemTheme: () => ipcRenderer.invoke('theme:get-system'),
  onSystemThemeChange: (callback: (theme: 'light' | 'dark') => void) => {
    const listener = (_event: IpcRendererEvent, theme: 'light' | 'dark') => callback(theme)
    ipcRenderer.on('theme:system-changed', listener)
    return () => ipcRenderer.removeListener('theme:system-changed', listener)
  }
})
