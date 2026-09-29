import { app, BrowserWindow, ipcMain } from 'electron'
import { join } from 'node:path'
import type { HttpResponseResult } from '../../src/shared/contracts/network'

const responses = new Map<number, HttpResponseResult>()
export function registerHttpResponseWindows(): void {
  ipcMain.handle('http:response-window', async (_event, response: HttpResponseResult) => {
    if (!response || typeof response.body !== 'string' || typeof response.headers !== 'string' || typeof response.cookies !== 'string' || typeof response.url !== 'string') throw new Error('Invalid HTTP response')
    const window = new BrowserWindow({ width: 1000, height: 760, minWidth: 560, minHeight: 400, show: false, title: 'HTTP Response', webPreferences: { preload: join(__dirname, '../preload/index.js'), sandbox: true, contextIsolation: true, nodeIntegration: false } })
    const id = window.webContents.id
    responses.set(id, response)
    window.on('closed', () => responses.delete(id))
    window.webContents.setWindowOpenHandler(() => ({ action: 'deny' }))
    window.webContents.on('will-navigate', event => event.preventDefault())
    try {
      if (!app.isPackaged && process.env.ELECTRON_RENDERER_URL) {
        const url = new URL(process.env.ELECTRON_RENDERER_URL)
        url.searchParams.set('window', 'http-response')
        await window.loadURL(url.toString())
      } else await window.loadFile(join(__dirname, '../renderer/index.html'), { query: { window: 'http-response' } })
      window.show()
    } catch (error) { window.destroy(); throw error }
  })
  ipcMain.handle('http:response-snapshot', event => responses.get(event.sender.id) ?? null)
}
export function closeHttpResponseWindow(senderId: number): boolean {
  if (!responses.has(senderId)) return false
  BrowserWindow.getAllWindows().find(window => window.webContents.id === senderId)?.close()
  return true
}
