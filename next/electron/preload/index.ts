import { contextBridge, ipcRenderer } from 'electron'

contextBridge.exposeInMainWorld('mootool', {
  platform: process.platform,
  getAppVersion: () => ipcRenderer.invoke('app:get-version')
})
