import { describe, expect, it, vi } from 'vitest'
import { resolveWindowMaterial, WindowMaterialController } from './windowMaterial'
import type { BrowserWindow } from 'electron'

const environment = { platform: 'darwin', systemVersion: '26.0', solidRequested: false, glassAvailable: true }

describe('window material policy', () => {
  it('resolves requested materials and platform fallbacks', () => {
    expect(resolveWindowMaterial('auto', environment)).toBe('liquid-glass')
    expect(resolveWindowMaterial('solid', environment)).toBe('solid')
    expect(resolveWindowMaterial('vibrancy', environment)).toBe('vibrancy')
    expect(resolveWindowMaterial('liquid-glass', { ...environment, systemVersion: '15.7' })).toBe('vibrancy')
    expect(resolveWindowMaterial('auto', { ...environment, glassAvailable: false })).toBe('vibrancy')
    for (const platform of ['win32', 'linux']) {
      expect(resolveWindowMaterial('liquid-glass', { ...environment, platform })).toBe('solid')
    }
  })
  it('honors accessibility over every requested mode', () => {
    for (const mode of ['auto', 'solid', 'vibrancy', 'liquid-glass'] as const) {
      expect(resolveWindowMaterial(mode, { ...environment, solidRequested: true })).toBe('solid')
    }
  })
})

function fixture(loadGlass = vi.fn(async () => ({ addView: vi.fn(() => 1) }))) {
  const win = {
    isDestroyed: vi.fn(() => false), setVibrancy: vi.fn(), setBackgroundColor: vi.fn(),
    setWindowButtonVisibility: vi.fn(), getNativeWindowHandle: () => Buffer.alloc(8),
    webContents: { send: vi.fn() }
  }
  const controller = new WindowMaterialController(win as unknown as BrowserWindow, 'auto', {
    ...environment, dark: () => false, highContrast: () => false, loadGlass
  })
  return { controller, win, loadGlass }
}

it('falls back on both load failure and native sentinel failure', async () => {
  for (const load of [vi.fn(async () => { throw new Error('missing binary') }), vi.fn(async () => ({ addView: vi.fn(() => -1) }))]) {
    const { controller, win } = fixture(load)
    await controller.initialize(false)
    expect(controller.material).toBe('vibrancy')
    expect(win.setVibrancy).toHaveBeenLastCalledWith('sidebar')
    await controller.refresh()
    expect(load).toHaveBeenCalledTimes(1)
  }
})

it('never combines native glass and vibrancy; accessibility masks and restores without another attachment', async () => {
  const addView = vi.fn(() => 1)
  const { controller, win } = fixture(vi.fn(async () => ({ addView })))
  await controller.initialize(false)
  expect(controller.material).toBe('liquid-glass')
  expect(win.setVibrancy.mock.calls.every(([value]) => value === null)).toBe(true)
  await controller.setAccessibility(true)
  expect(controller.material).toBe('solid')
  expect(win.setBackgroundColor).toHaveBeenLastCalledWith('#f7f7f8')
  await controller.setAccessibility(false)
  expect(controller.material).toBe('liquid-glass')
  expect(addView).toHaveBeenCalledTimes(1)
})

it('does not attach glass if accessibility changes during module loading', async () => {
  const addView = vi.fn(() => 1)
  let finish!: (module: { addView: typeof addView }) => void
  const { controller } = fixture(vi.fn(() => new Promise(resolve => { finish = resolve })))
  const pending = controller.initialize(false)
  await Promise.resolve()
  const accessibility = controller.setAccessibility(true)
  finish({ addView })
  await Promise.all([pending, accessibility])
  expect(controller.material).toBe('solid')
  expect(addView).not.toHaveBeenCalled()
})

it('does not touch a destroyed window after asynchronous loading', async () => {
  const addView = vi.fn(() => 1)
  let finish!: (module: { addView: typeof addView }) => void
  const { controller, win } = fixture(vi.fn(() => new Promise(resolve => { finish = resolve })))
  const pending = controller.initialize(false)
  await Promise.resolve()
  win.isDestroyed.mockReturnValue(true)
  finish({ addView })
  await pending
  expect(addView).not.toHaveBeenCalled()
  expect(win.webContents.send).not.toHaveBeenCalled()
})

it('reports actual material, fallback reason and restart state independently of the saved selection', async () => {
  const { controller } = fixture(vi.fn(async () => ({ addView: vi.fn(() => -1) })))
  expect(controller.status('auto').reason).toBe('initializing')
  await controller.initialize(false)
  expect(controller.status('solid')).toEqual({ requested: 'auto', effective: 'vibrancy', reason: 'extension', pendingRestart: true })
  await controller.setAccessibility(true)
  expect(controller.status('auto')).toEqual({ requested: 'auto', effective: 'solid', reason: 'accessibility', pendingRestart: false })
})

it('publishes material for a BaseWindow without accessing browser webContents', async () => {
  const { win } = fixture()
  const { webContents: _unused, ...baseWindow } = win
  const onChange = vi.fn()
  const controller = new WindowMaterialController(baseWindow as unknown as BrowserWindow, 'vibrancy', {
    ...environment, dark: () => true, highContrast: () => false,
    loadGlass: vi.fn(async () => { throw new Error('Must not load glass for vibrancy') })
  }, onChange)
  await controller.initialize(false)
  expect(onChange).toHaveBeenLastCalledWith('vibrancy')
  baseWindow.setVibrancy.mockImplementation(() => { throw new Error('Native effect failed') })
  await controller.refresh()
  expect(controller.status('vibrancy').reason).toBe('native-failure')
  expect(onChange).toHaveBeenLastCalledWith('solid')
})
