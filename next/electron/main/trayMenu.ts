import type { MenuItemConstructorOptions } from 'electron'
import type { HostProfile } from '../../src/shared/contracts/system'
import type { CommandActionId } from '../../src/shared/contracts/commandPalette'

export type TrayMenuLabels = {
  open: string
  commandPalette: string
  quickActions: string
  settings: string
  colorPicker: string
  screenshot: string
  translation: string
  quit: string
}

export type TrayMenuActions = {
  openApp: () => void
  openCommandPalette: () => void
  runCommandAction: (actionId: CommandActionId) => void
  openSettings: () => void
  openColorPicker: () => void
  captureScreen: () => void
  openTranslation: () => void
  switchHost: (profile: HostProfile) => void
  quit: () => void
}

export function buildTrayMenuTemplate(
  labels: TrayMenuLabels,
  profiles: HostProfile[],
  activeHostId: number | null,
  actions: TrayMenuActions,
  quickActions: Array<{ actionId: CommandActionId; label: string }> = []
): MenuItemConstructorOptions[] {
  return [
    { label: labels.open, click: actions.openApp },
    { label: labels.commandPalette, click: actions.openCommandPalette },
    ...(quickActions.length ? [{ label: labels.quickActions, submenu: quickActions.map(({ actionId, label }) => ({ label, click: () => actions.runCommandAction(actionId) })) }] : []),
    { label: labels.settings, click: actions.openSettings },
    { type: 'separator' },
    { label: labels.colorPicker, click: actions.openColorPicker },
    { label: labels.screenshot, click: actions.captureScreen },
    { label: labels.translation, click: actions.openTranslation },
    { type: 'separator' },
    ...profiles.map((profile) => ({
      label: profile.name,
      type: 'checkbox' as const,
      checked: profile.id === activeHostId,
      click: () => actions.switchHost(profile)
    })),
    ...(profiles.length > 0 ? [{ type: 'separator' as const }] : []),
    { label: labels.quit, click: actions.quit }
  ]
}
