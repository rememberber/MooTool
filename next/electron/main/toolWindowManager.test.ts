import { describe, expect, it, vi } from 'vitest'
import type { BrowserWindow, WebContentsView } from 'electron'
import { ToolWindowManager } from './toolWindowManager'

vi.mock('electron', async () => {
  const { EventEmitter } = await import('node:events')
  return {
    BaseWindow: class {}, BrowserWindow: class {},
    WebContentsView: class {
      webContents = Object.assign(new EventEmitter(), { isDestroyed: () => false, focus: vi.fn(), send: vi.fn(), setWindowOpenHandler: vi.fn() })
      setBackgroundColor = vi.fn()
      setBounds = vi.fn()
      setVisible = vi.fn()
    }
  }
})
type MockView = { webContents: { emit: (event: string) => void; focus: ReturnType<typeof vi.fn> } }
function fixture() {
  const views: MockView[] = []
  const main = { isDestroyed: vi.fn(() => false), isFocused: vi.fn(() => true), webContents: { focus: vi.fn(), send: vi.fn() }, contentView: { addChildView: vi.fn(), removeChildView: vi.fn() } }
  const manager = new ToolWindowManager({
    enabled: true, getMainWindow: () => main as unknown as BrowserWindow,
    loadTool: (view: WebContentsView) => { views.push(view as unknown as MockView) }, preloadPath: '',
    getWindowState: () => undefined, setWindowState: vi.fn(), backgroundColor: () => '#fff', icon: () => undefined,
    onWindowFocusChanged: vi.fn(), materialMode: () => 'solid',
    materialDependencies: { platform: 'linux', systemVersion: '', dark: () => false, highContrast: () => false, loadGlass: async () => ({ addView: () => 0 }) }
  })
  manager.setWorkspaceBounds({ x: 100, y: 0, width: 900, height: 600 })
  return { manager, main, views }
}

describe('workspace keyboard focus', () => {
  it('hands keyboard focus to the shell after removing a docked tool', () => {
    const { manager, main, views } = fixture()
    manager.activate('calculator'); views[0].webContents.emit('did-finish-load')
    expect(views[0].webContents.focus).toHaveBeenCalled()
    manager.activate('mootool')
    expect(main.webContents.focus).toHaveBeenCalledOnce()
    expect(main.contentView.removeChildView.mock.invocationCallOrder.at(-1)).toBeLessThan(main.webContents.focus.mock.invocationCallOrder[0])
    expect(manager.snapshot().activeToolId).toBe('mootool')
  })
  it('does not steal native focus from another application', () => {
    const { manager, main, views } = fixture()
    main.isFocused.mockReturnValue(false)
    manager.activate('calculator'); views[0].webContents.emit('did-finish-load')
    manager.activate('mootool')
    expect(main.webContents.focus).not.toHaveBeenCalled()
    expect(views[0].webContents.focus).not.toHaveBeenCalled()
  })
  it('does not let a late tool load take focus back from an overlay', () => {
    const { manager, main, views } = fixture()
    manager.activate('calculator'); manager.activate('mootool')
    views[0].webContents.emit('did-finish-load')
    expect(main.webContents.focus).toHaveBeenCalledOnce()
    expect(main.contentView.addChildView).not.toHaveBeenCalled()
    expect(views[0].webContents.focus).not.toHaveBeenCalled()
  })
})

describe('tool renderer recovery', () => {
  it('reloads the same docked view after a crash and restores native focus', async () => {
    const { manager, main, views } = fixture()
    manager.activate('calculator')
    const original = views[0]
    original.webContents.emit('did-finish-load')
    original.webContents.emit('render-process-gone')
    expect(views).toHaveLength(2)
    expect(views[1]).toBe(original)
    expect(manager.snapshot().tools[0]).toMatchObject({ ready: false })
    expect(main.contentView.removeChildView).toHaveBeenCalled()
    original.webContents.emit('did-finish-load')
    expect(manager.snapshot().tools[0]).toMatchObject({ ready: true })
    expect(original.webContents.focus).toHaveBeenCalledTimes(2)
  })

  it('bounds automatic crash retries and permits an explicit retry', () => {
    const { manager, views } = fixture()
    manager.activate('calculator')
    const original = views[0]
    original.webContents.emit('render-process-gone')
    original.webContents.emit('did-finish-load')
    original.webContents.emit('render-process-gone')
    expect(views).toHaveLength(2)
    expect(manager.snapshot().tools[0]).toMatchObject({ ready: false, failed: true })
    manager.activate('calculator')
    expect(views).toHaveLength(3)
    expect(views[2]).toBe(original)
    original.webContents.emit('did-finish-load')
    expect(manager.snapshot().tools[0]).toMatchObject({ ready: true })
    expect(manager.snapshot().tools[0].failed).toBeUndefined()
  })
})
