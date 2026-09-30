import {
  BaseWindow,
  BrowserWindow,
  WebContentsView,
  type WebContents
} from 'electron'
import type {
  ToolId,
  ToolWindowSnapshot,
  ToolWindowStatus,
  ToolWorkspaceBounds,
  WindowState
} from '../../src/shared/contracts/app'
import { WindowMaterialController, type MaterialDependencies } from './windowMaterial'
import { windowChromeOptions, trackWindowChrome } from './windowChrome'
import type { WindowMaterial } from '../../src/shared/contracts/settings'

type DetachableToolId = Exclude<ToolId, 'mootool'>
type ToolViewHost = 'none' | 'main' | 'detached'

type ToolViewRecord = {
  toolId: DetachableToolId
  view: WebContentsView
  host: ToolViewHost
  window: BaseWindow | null
  ready: boolean
  title: string
  material: WindowMaterialController | null
  solidRequested: boolean
  saveTimer?: NodeJS.Timeout
}

type ToolWindowManagerOptions = {
  enabled: boolean
  getMainWindow: () => BrowserWindow | null
  loadTool: (view: WebContentsView, toolId: DetachableToolId) => void
  preloadPath: string
  getWindowState: (toolId: DetachableToolId) => WindowState | undefined
  setWindowState: (toolId: DetachableToolId, state: WindowState) => void
  backgroundColor: () => string
  icon: () => string | undefined
  onWindowFocusChanged: () => void
  materialMode: () => WindowMaterial
  materialDependencies: MaterialDependencies
}

const defaultDetachedWindow: WindowState = {
  bounds: { width: 1100, height: 760 },
  maximized: false
}

export class ToolWindowManager {
  private readonly records = new Map<DetachableToolId, ToolViewRecord>()
  private activeToolId: ToolId = 'mootool'
  private workspaceBounds: ToolWorkspaceBounds | null = null
  private quitting = false

  constructor(private readonly options: ToolWindowManagerOptions) {}

  activate(toolId: ToolId): ToolWindowSnapshot {
    this.activeToolId = toolId
    if (this.options.enabled && toolId !== 'mootool') this.getOrCreate(toolId)
    this.syncMainHost()
    this.focusActiveDockedView()
    this.notify()
    return this.snapshot()
  }

  /** Move keyboard focus into the docked tool WebContentsView instead of the sidebar shell. */
  focusActiveDockedView(): void {
    if (this.activeToolId === 'mootool') return
    const mainWindow = this.options.getMainWindow()
    if (!mainWindow || mainWindow.isDestroyed() || !mainWindow.isFocused()) return
    const record = this.records.get(this.activeToolId as DetachableToolId)
    if (record?.host === 'main' && record.ready && !record.view.webContents.isDestroyed()) {
      record.view.webContents.focus()
    }
  }

  setWorkspaceBounds(bounds: ToolWorkspaceBounds): ToolWindowSnapshot {
    this.workspaceBounds = normalizeBounds(bounds)
    this.syncMainHost()
    return this.snapshot()
  }

  detach(toolId: DetachableToolId): ToolWindowStatus {
    if (!this.options.enabled) throw new Error('Tool windows are disabled')
    const record = this.getOrCreate(toolId)
    if (record.window && !record.window.isDestroyed()) {
      record.window.show()
      record.window.focus()
      return this.status(record)
    }

    this.removeFromHost(record)
    const saved = this.options.getWindowState(toolId) ?? defaultDetachedWindow
    const window = new BaseWindow({
      ...saved.bounds,
      minWidth: 760,
      minHeight: 560,
      show: false,
      title: record.title,
      ...windowChromeOptions(),
      backgroundColor: this.options.backgroundColor(),
      icon: this.options.icon()
    })
    if (process.platform === 'darwin') window.setWindowButtonVisibility(true)
    trackWindowChrome(window, record.view.webContents)
    record.window = window
    record.host = 'detached'
    window.contentView.addChildView(record.view)
    this.resizeDetached(record)
    record.material = new WindowMaterialController(window, this.options.materialMode(), this.options.materialDependencies, (material) => {
      if (record.window !== window || record.view.webContents.isDestroyed()) return
      record.view.setBackgroundColor(material === 'solid' ? this.options.backgroundColor() : '#00000000')
      record.view.webContents.send('window:material-changed', material)
    })
    const materialReady = record.material.initialize(record.solidRequested)

    const saveState = () => this.scheduleWindowStateSave(record)
    window.on('resize', () => {
      this.resizeDetached(record)
      saveState()
    })
    window.on('move', saveState)
    window.on('maximize', saveState)
    window.on('unmaximize', saveState)
    window.on('focus', this.options.onWindowFocusChanged)
    window.on('blur', this.options.onWindowFocusChanged)
    window.on('close', (event) => {
      if (this.quitting) return
      event.preventDefault()
      this.dock(toolId)
    })
    window.on('closed', () => {
      clearTimeout(record.saveTimer)
      if (record.window === window) {
        record.window = null
        record.host = 'none'
        this.resetMaterial(record)
        if (!this.quitting) {
          this.syncMainHost()
          this.notify()
        }
      }
      this.options.onWindowFocusChanged()
    })

    if (saved.maximized) window.maximize()
    void materialReady.then(() => {
      if (window.isDestroyed() || record.window !== window) return
      window.show()
      window.focus()
    })
    this.sendActivity(record)
    this.notify()
    return this.status(record)
  }

  dock(toolId: DetachableToolId): ToolWindowStatus {
    if (!this.options.enabled) throw new Error('Tool windows are disabled')
    const record = this.getOrCreate(toolId)
    const window = record.window
    this.resetMaterial(record)
    if (window && !window.isDestroyed()) {
      this.saveWindowState(record)
      window.contentView.removeChildView(record.view)
    }
    record.window = null
    record.host = 'none'
    if (window && !window.isDestroyed()) window.destroy()
    this.syncMainHost()
    this.sendActivity(record)
    this.notify()
    return this.status(record)
  }

  focus(toolId: DetachableToolId): boolean {
    const record = this.records.get(toolId)
    if (!record?.window || record.window.isDestroyed()) return false
    record.window.show()
    record.window.focus()
    return true
  }

  setTitle(toolId: DetachableToolId, title: string): void {
    const record = this.records.get(toolId)
    if (!record) return
    record.title = sanitizeTitle(title)
    record.window?.setTitle(record.title)
  }

  getStatus(toolId: DetachableToolId): ToolWindowStatus {
    if (!this.options.enabled) return { toolId, detached: false, ready: false }
    return this.status(this.getOrCreate(toolId))
  }

  snapshot(): ToolWindowSnapshot {
    return {
      enabled: this.options.enabled,
      activeToolId: this.activeToolId,
      tools: [...this.records.values()].map((record) => this.status(record))
    }
  }

  owns(toolId: DetachableToolId, sender: WebContents): boolean {
    return this.records.get(toolId)?.view.webContents.id === sender.id
  }

  resolveOwner(sender: WebContents): BaseWindow | null {
    const record = [...this.records.values()].find((item) => item.view.webContents.id === sender.id)
    return record?.window ?? (record ? this.options.getMainWindow() : null)
  }

  materialFor(sender: WebContents): WindowMaterialController | null {
    return [...this.records.values()].find(record => record.view.webContents.id === sender.id)?.material ?? null
  }

  async setMaterialAccessibility(sender: WebContents, solid: boolean): Promise<void> {
    const record = [...this.records.values()].find(record => record.view.webContents.id === sender.id)
    if (!record) return
    record.solidRequested = solid
    await record.material?.setAccessibility(solid)
  }

  dismissOwner(sender: WebContents): boolean {
    const record = [...this.records.values()].find((item) => item.view.webContents.id === sender.id)
    if (!record?.window || record.window.isDestroyed()) return false
    this.dock(record.toolId)
    return true
  }

  sendToAll(channel: string, payload: unknown): void {
    for (const record of this.records.values()) {
      if (!record.view.webContents.isDestroyed()) record.view.webContents.send(channel, payload)
    }
  }

  updateBackground(color: string): void {
    for (const record of this.records.values()) {
      if (record.material) void record.material.refresh()
      else record.view.setBackgroundColor(color)
    }
  }

  dispose(): void {
    this.quitting = true
    for (const record of this.records.values()) {
      clearTimeout(record.saveTimer)
      this.saveWindowState(record)
      this.removeFromHost(record)
      if (record.window && !record.window.isDestroyed()) record.window.destroy()
      record.window = null
      if (!record.view.webContents.isDestroyed()) record.view.webContents.close()
    }
    this.records.clear()
  }

  private getOrCreate(toolId: DetachableToolId): ToolViewRecord {
    const existing = this.records.get(toolId)
    if (existing) return existing

    const view = new WebContentsView({
      webPreferences: {
        preload: this.options.preloadPath,
        sandbox: true,
        contextIsolation: true,
        nodeIntegration: false
      }
    })
    const record: ToolViewRecord = {
      toolId,
      view,
      host: 'none',
      window: null,
      ready: false,
      title: `MooTool — ${toolId}`,
      material: null,
      solidRequested: false
    }
    this.records.set(toolId, record)
    view.setBackgroundColor(this.options.backgroundColor())
    view.webContents.setWindowOpenHandler(() => ({ action: 'deny' }))
    view.webContents.on('did-finish-load', () => {
      record.ready = true
      this.syncMainHost()
      this.sendActivity(record)
      this.notify()
      if (record.toolId === this.activeToolId) this.focusActiveDockedView()
    })
    view.webContents.on('render-process-gone', () => {
      record.ready = false
      this.notify()
    })
    this.options.loadTool(view, toolId)
    return record
  }

  private syncMainHost(): void {
    const mainWindow = this.options.getMainWindow()
    if (!mainWindow || mainWindow.isDestroyed()) return

    for (const record of this.records.values()) {
      const shouldAttach = record.ready && !record.window && record.toolId === this.activeToolId && this.workspaceBounds !== null
      if (shouldAttach) {
        if (record.host !== 'main') {
          this.removeFromHost(record)
          mainWindow.contentView.addChildView(record.view)
          record.host = 'main'
        }
        record.view.setBounds(this.workspaceBounds!)
        record.view.setVisible(true)
      } else if (record.host === 'main') {
        mainWindow.contentView.removeChildView(record.view)
        record.host = 'none'
      }
      this.sendActivity(record)
    }
  }

  private removeFromHost(record: ToolViewRecord): void {
    if (record.host === 'main') {
      const mainWindow = this.options.getMainWindow()
      if (mainWindow && !mainWindow.isDestroyed()) mainWindow.contentView.removeChildView(record.view)
    } else if (record.host === 'detached' && record.window && !record.window.isDestroyed()) {
      record.window.contentView.removeChildView(record.view)
    }
    record.host = 'none'
  }

  private resizeDetached(record: ToolViewRecord): void {
    if (!record.window || record.window.isDestroyed()) return
    const bounds = record.window.getContentBounds()
    record.view.setBounds({ x: 0, y: 0, width: bounds.width, height: bounds.height })
  }

  private scheduleWindowStateSave(record: ToolViewRecord): void {
    clearTimeout(record.saveTimer)
    record.saveTimer = setTimeout(() => this.saveWindowState(record), 250)
  }

  private saveWindowState(record: ToolViewRecord): void {
    const window = record.window
    if (!window || window.isDestroyed()) return
    this.options.setWindowState(record.toolId, {
      bounds: window.isMaximized() ? window.getNormalBounds() : window.getBounds(),
      maximized: window.isMaximized()
    })
  }

  private resetMaterial(record: ToolViewRecord): void {
    record.material = null
    if (record.view.webContents.isDestroyed()) return
    record.view.setBackgroundColor(this.options.backgroundColor())
    record.view.webContents.send('window:material-changed', 'solid')
    record.view.webContents.send('window:fullscreen-changed', false)
  }

  private status(record: ToolViewRecord): ToolWindowStatus {
    return {
      toolId: record.toolId,
      detached: Boolean(record.window && !record.window.isDestroyed()),
      ready: record.ready
    }
  }

  private notify(): void {
    const mainWindow = this.options.getMainWindow()
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.send('tool-window:snapshot-changed', this.snapshot())
    }
    for (const record of this.records.values()) {
      if (!record.view.webContents.isDestroyed()) {
        record.view.webContents.send('tool-window:state-changed', this.status(record))
      }
    }
  }

  private sendActivity(record: ToolViewRecord): void {
    if (record.view.webContents.isDestroyed()) return
    const active = Boolean(record.window) || (record.host === 'main' && record.toolId === this.activeToolId)
    record.view.webContents.send('tool-window:activity-changed', active)
  }
}

function normalizeBounds(bounds: ToolWorkspaceBounds): ToolWorkspaceBounds {
  const safe = (value: number, minimum: number, maximum: number) => Math.min(maximum, Math.max(minimum, Math.round(value)))
  return {
    x: safe(bounds.x, 0, 20_000),
    y: safe(bounds.y, 0, 20_000),
    width: safe(bounds.width, 1, 20_000),
    height: safe(bounds.height, 1, 20_000)
  }
}

function sanitizeTitle(title: string): string {
  const normalized = title.replace(/[\r\n\t]/g, ' ').trim().slice(0, 160)
  return normalized ? `MooTool — ${normalized}` : 'MooTool'
}
