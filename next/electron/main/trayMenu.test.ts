import { describe, expect, it, vi } from 'vitest'
import type { HostProfile } from '../../src/shared/contracts/system'
import { buildTrayMenuTemplate, type TrayMenuActions, type TrayMenuLabels } from './trayMenu'

const labels: TrayMenuLabels = {
  open: 'Open MooTool',
  commandPalette: 'Command Palette',
  quickActions: 'Quick Actions',
  settings: 'Settings…',
  colorPicker: 'Color Picker',
  screenshot: 'Capture',
  translation: 'Translation',
  quit: 'Quit MooTool'
}

function profile(id: number, name: string): HostProfile {
  return { id, name, content: `${id}.local`, createTime: '', modifiedTime: '' }
}

function actions(): TrayMenuActions {
  return {
    openApp: vi.fn(),
    openCommandPalette: vi.fn(),
    runCommandAction: vi.fn(),
    openSettings: vi.fn(),
    openColorPicker: vi.fn(),
    captureScreen: vi.fn(),
    openTranslation: vi.fn(),
    switchHost: vi.fn(),
    quit: vi.fn()
  }
}

describe('buildTrayMenuTemplate', () => {
  it('matches the Java tray actions and marks the active Host profile', () => {
    const handlers = actions()
    const profiles = [profile(1, 'Local'), profile(2, 'Development')]
    const template = buildTrayMenuTemplate(labels, profiles, 2, handlers)

    expect(template.map((item) => item.type === 'separator' ? '-' : item.label)).toEqual([
      'Open MooTool', 'Command Palette', 'Settings…', '-', 'Color Picker', 'Capture', 'Translation', '-', 'Local', 'Development', '-', 'Quit MooTool'
    ])
    expect(template[8]).toMatchObject({ type: 'checkbox', checked: false })
    expect(template[9]).toMatchObject({ type: 'checkbox', checked: true })

    ;(template[9].click as () => void)()
    expect(handlers.switchHost).toHaveBeenCalledWith(profiles[1])
  })

  it('does not add an empty Host separator when no profiles exist', () => {
    const template = buildTrayMenuTemplate(labels, [], null, actions())
    expect(template.map((item) => item.type === 'separator' ? '-' : item.label)).toEqual([
      'Open MooTool', 'Command Palette', 'Settings…', '-', 'Color Picker', 'Capture', 'Translation', '-', 'Quit MooTool'
    ])
  })

  it('routes the screenshot shortcut to its action', () => {
    const handlers = actions()
    const template = buildTrayMenuTemplate(labels, [], null, handlers)
    ;(template[5].click as () => void)()
    expect(handlers.captureScreen).toHaveBeenCalledOnce()
  })

  it('opens the command palette from the tray', () => {
    const handlers = actions()
    const template = buildTrayMenuTemplate(labels, [], null, handlers)
    ;(template[1].click as () => void)()
    expect(handlers.openCommandPalette).toHaveBeenCalledOnce()
  })

  it('dispatches a quick action to the palette using only its validated id', () => {
    const handlers = actions()
    const template = buildTrayMenuTemplate(labels, [], null, handlers, [{ actionId: 'json-format', label: 'Format JSON' }])
    const submenu = template.find((item) => item.label === 'Quick Actions')!.submenu as Array<{ label: string; click: () => void }>
    expect(submenu[0].label).toBe('Format JSON')
    submenu[0].click()
    expect(handlers.runCommandAction).toHaveBeenCalledWith('json-format')
  })
})
