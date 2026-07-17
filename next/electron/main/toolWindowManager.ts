import {
  BaseWindow,
  BrowserWindow,
  screen,
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

type DetachableToolId = Exclude<ToolId, 'mootool'>
type ToolViewHost = 'none' | 'main' | 'detached'

type ToolViewRecord = {
  toolId: DetachableToolId
  view: WebContentsView
  host: ToolViewHost
  window: BaseWindow | null
  ready: boolean
  title: string
  windowControlsVisible: boolean
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
}

const defaultDetachedWindow: WindowState = {
  bounds: { width: 1100, height: 760 },
  maximized: false
}

const windowControlsPollInterval = 80
const brandRegionInset = { top: 18, left: 20, height: 32, maximumWidth: 420, reservedRight: 360 }

export class ToolWindowManager {
  private readonly records = new Map<DetachableToolId, ToolViewRecord>()
  private activeToolId: ToolId = 'mootool'
  private workspaceBounds: ToolWorkspaceBounds | null = null
  private windowControlsTimer?: NodeJS.Timeout
  private quitting = false

  constructor(private readonly options: ToolWindowManagerOptions) {}

  activate(toolId: ToolId): ToolWindowSnapshot {
    this.activeToolId = toolId
    if (this.options.enabled && toolId !== 'mootool') this.getOrCreate(toolId)
    this.syncMainHost()
    this.notify()
    return this.snapshot()
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
      this.ensureWindowControlsTracking()
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
      titleBarStyle: 'hiddenInset',
      trafficLightPosition: { x: 18, y: 18 },
      backgroundColor: this.options.backgroundColor(),
      icon: this.options.icon()
    })
    if (process.platform === 'darwin') window.setWindowButtonVisibility(false)
    record.windowControlsVisible = false
    record.window = window
    record.host = 'detached'
    window.contentView.addChildView(record.view)
    this.resizeDetached(record)

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
        this.updateWindowControls(record, false)
        this.stopWindowControlsTrackingIfIdle()
        if (!this.quitting) {
          this.syncMainHost()
          this.notify()
        }
      }
      this.options.onWindowFocusChanged()
    })

    if (saved.maximized) window.maximize()
    window.show()
    window.focus()
    this.ensureWindowControlsTracking()
    this.sendActivity(record)
    this.notify()
    return this.status(record)
  }

  dock(toolId: DetachableToolId): ToolWindowStatus {
    if (!this.options.enabled) throw new Error('Tool windows are disabled')
    const record = this.getOrCreate(toolId)
    const window = record.window
    this.updateWindowControls(record, false)
    if (window && !window.isDestroyed()) {
      this.saveWindowState(record)
      window.contentView.removeChildView(record.view)
    }
    record.window = null
    record.host = 'none'
    if (window && !window.isDestroyed()) window.destroy()
    this.stopWindowControlsTrackingIfIdle()
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

  sendToAll(channel: string, payload: unknown): void {
    for (const record of this.records.values()) {
      if (!record.view.webContents.isDestroyed()) record.view.webContents.send(channel, payload)
    }
  }

  updateBackground(color: string): void {
    for (const record of this.records.values()) {
      record.view.setBackgroundColor(color)
      record.window?.setBackgroundColor(color)
    }
  }

  dispose(): void {
    this.quitting = true
    clearInterval(this.windowControlsTimer)
    this.windowControlsTimer = undefined
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
      windowControlsVisible: false
    }
    this.records.set(toolId, record)
    view.setBackgroundColor(this.options.backgroundColor())
    view.webContents.setWindowOpenHandler(() => ({ action: 'deny' }))
    view.webContents.on('did-finish-load', () => {
      record.ready = true
      this.syncMainHost()
      this.sendActivity(record)
      this.notify()
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

  private ensureWindowControlsTracking(): void {
    if (process.platform !== 'darwin' || this.windowControlsTimer) return
    this.windowControlsTimer = setInterval(() => this.syncWindowControlsWithCursor(), windowControlsPollInterval)
    this.windowControlsTimer.unref()
    this.syncWindowControlsWithCursor()
  }

  private stopWindowControlsTrackingIfIdle(): void {
    if ([...this.records.values()].some((record) => record.window && !record.window.isDestroyed())) return
    clearInterval(this.windowControlsTimer)
    this.windowControlsTimer = undefined
  }

  private syncWindowControlsWithCursor(): void {
    if (process.platform !== 'darwin') return
    const cursor = screen.getCursorScreenPoint()
    for (const record of this.records.values()) {
      const window = record.window
      if (!window || window.isDestroyed()) continue
      const bounds = window.getContentBounds()
      const width = Math.min(brandRegionInset.maximumWidth, Math.max(0, bounds.width - brandRegionInset.reservedRight))
      const hovered = window.isVisible()
        && cursor.x >= bounds.x + brandRegionInset.left
        && cursor.x <= bounds.x + brandRegionInset.left + width
        && cursor.y >= bounds.y + brandRegionInset.top
        && cursor.y <= bounds.y + brandRegionInset.top + brandRegionInset.height
      this.updateWindowControls(record, hovered)
    }
  }

  private updateWindowControls(record: ToolViewRecord, visible: boolean): void {
    if (process.platform !== 'darwin' || record.windowControlsVisible === visible) return
    record.windowControlsVisible = visible
    if (record.window && !record.window.isDestroyed()) record.window.setWindowButtonVisibility(visible)
    if (!record.view.webContents.isDestroyed()) {
      record.view.webContents.send('tool-window:controls-visibility-changed', visible)
    }
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
