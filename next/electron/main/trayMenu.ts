import type { MenuItemConstructorOptions } from 'electron'
import type { HostProfile } from '../../src/shared/contracts/system'

export type TrayMenuLabels = {
  open: string
  settings: string
  colorPicker: string
  screenshot: string
  translation: string
  quit: string
}

export type TrayMenuActions = {
  openApp: () => void
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
  actions: TrayMenuActions
): MenuItemConstructorOptions[] {
  return [
    { label: labels.open, click: actions.openApp },
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
