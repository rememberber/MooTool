import { app, BrowserWindow, ipcMain, nativeTheme } from 'electron'
import { join } from 'node:path'

const isDev = Boolean(process.env.ELECTRON_RENDERER_URL)

function getDevelopmentIconPath(): string {
  const filename = process.platform === 'darwin' ? 'icon-mac.png' : process.platform === 'win32' ? 'icon.ico' : 'icon.png'
  return join(__dirname, '../../resources', filename)
}

function createMainWindow(): void {
  const systemTheme = nativeTheme.shouldUseDarkColors ? 'dark' : 'light'
  const window = new BrowserWindow({
    width: 1440,
    height: 920,
    minWidth: 1080,
    minHeight: 720,
    show: false,
    backgroundColor: systemTheme === 'dark' ? '#171719' : '#f7f7f8',
    titleBarStyle: 'hiddenInset',
    trafficLightPosition: { x: 18, y: 18 },
    vibrancy: 'sidebar',
    visualEffectState: 'active',
    icon: app.isPackaged ? undefined : getDevelopmentIconPath(),
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      sandbox: false,
      contextIsolation: true,
      nodeIntegration: false
    }
  })

  window.once('ready-to-show', () => {
    window.show()
  })

  if (isDev && process.env.ELECTRON_RENDERER_URL) {
    window.loadURL(process.env.ELECTRON_RENDERER_URL)
  } else {
    window.loadFile(join(__dirname, '../renderer/index.html'))
  }
}

app.whenReady().then(() => {
  if (process.platform === 'darwin' && !app.isPackaged) {
    app.dock?.setIcon(getDevelopmentIconPath())
  }

  ipcMain.handle('app:get-version', () => app.getVersion())
  ipcMain.handle('theme:get-system', () => (nativeTheme.shouldUseDarkColors ? 'dark' : 'light'))

  nativeTheme.themeSource = 'system'
  nativeTheme.on('updated', () => {
    const systemTheme = nativeTheme.shouldUseDarkColors ? 'dark' : 'light'
    for (const window of BrowserWindow.getAllWindows()) {
      window.setBackgroundColor(systemTheme === 'dark' ? '#171719' : '#f7f7f8')
      window.webContents.send('theme:system-changed', systemTheme)
    }
  })
  createMainWindow()

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createMainWindow()
    }
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})
