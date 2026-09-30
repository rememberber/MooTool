import type { BaseWindow, BaseWindowConstructorOptions, WebContents } from 'electron'

/** Keep native frame behavior on every OS; extend content on macOS only. */
export function windowChromeOptions(platform = process.platform): BaseWindowConstructorOptions {
  return platform === 'darwin' ? {
    titleBarStyle: 'hiddenInset',
    trafficLightPosition: { x: 18, y: 18 },
    transparent: true,
    visualEffectState: 'followWindow'
  } : {}
}

/** Fullscreen changes the safe area, not the lifetime of the tool renderer. */
export function trackWindowChrome(window: BaseWindow, contents: WebContents): void {
  const send = () => {
    if (!window.isDestroyed() && !contents.isDestroyed()) {
      contents.send('window:fullscreen-changed', window.isFullScreen())
    }
  }
  window.on('enter-full-screen', send)
  window.on('leave-full-screen', send)
  contents.on('did-finish-load', send)
  window.once('closed', () => contents.removeListener('did-finish-load', send))
  send()
}
