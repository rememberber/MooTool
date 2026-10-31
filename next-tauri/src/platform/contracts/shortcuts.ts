export type GlobalShortcutState = 'disabled' | 'registered' | 'invalid' | 'conflict' | 'unavailable'

export type GlobalShortcutStatus = {
  state: GlobalShortcutState
  accelerator: string
}

const modifierNames: Record<string, string> = {
  commandorcontrol: 'CommandOrControl',
  cmdorctrl: 'CommandOrControl',
  command: 'Command',
  cmd: 'Command',
  control: 'Control',
  ctrl: 'Control',
  alt: 'Alt',
  option: 'Alt',
  shift: 'Shift',
  super: 'Super',
  win: 'Super',
  meta: 'Super'
}

const specialKeys = [
  'Space', 'Return', 'Tab', 'Backspace', 'Delete', 'Insert', 'Home', 'End',
  'PageUp', 'PageDown', 'Up', 'Down', 'Left', 'Right', 'Escape'
]

export function shortcutPlatform(): string {
  return navigator.platform.toLowerCase().includes('mac') ? 'darwin' : 'win32'
}

export function normalizeShortcut(value: string, platform: string): string | null {
  if (typeof value !== 'string' || value.length > 160) return null
  const parts = value.split('+').map((part) => part.trim())
  if (parts.length < 2 || parts.some((part) => !part)) return null
  const rawKey = parts.pop()!
  const key = /^[a-z0-9]$/i.test(rawKey) || /^F(?:[1-9]|1\d|2[0-4])$/i.test(rawKey)
    ? rawKey.toUpperCase()
    : specialKeys.find((item) => item.toLowerCase() === rawKey.toLowerCase())
      ?? (/^[,./;\[\]\\'`=-]$/.test(rawKey) ? rawKey : null)
  if (!key) return null
  const modifiers = parts.map((part) => modifierNames[part.toLowerCase()]).map((modifier) =>
    modifier === 'CommandOrControl'
      ? platform === 'darwin' ? 'Command' : 'Control'
      : modifier === 'Super' && platform === 'darwin' ? 'Command' : modifier)
  if (modifiers.some((modifier) => !modifier)
    || new Set(modifiers).size !== modifiers.length
    || !modifiers.some((modifier) => modifier !== 'Shift')
    || (platform !== 'darwin' && modifiers.includes('Command'))) return null
  return [...modifiers.sort((left, right) => ['Command', 'Control', 'Alt', 'Shift', 'Super'].indexOf(left!) - ['Command', 'Control', 'Alt', 'Shift', 'Super'].indexOf(right!)), key].join('+')
}

export function validateGlobalShortcut(value: string, platform: string): GlobalShortcutStatus {
  const accelerator = normalizeShortcut(value, platform)
  if (!accelerator) return { state: 'invalid', accelerator: value }
  const reserved = ['K', ',', 'C', 'V', 'X', 'A', 'Z', 'F', 'S', 'Return', 'Shift+F', 'Shift+Z']
    .map((keys) => normalizeShortcut(`CommandOrControl+${keys}`, platform))
  const inAppSearch = accelerator.endsWith('+K')
    && accelerator.split('+').some((part) => ['Control', 'Command', 'Super'].includes(part))
  return {
    state: inAppSearch || reserved.includes(accelerator) ? 'conflict' : 'registered',
    accelerator
  }
}

export function displayShortcut(value: string, platform = shortcutPlatform()): string {
  return (normalizeShortcut(value, platform) ?? value)
    .replace(/\bCommand\b/g, 'Cmd')
    .replace(/\bControl\b/g, 'Ctrl')
}

export function previewGlobalShortcutStatus(
  enabled: boolean,
  configured: string,
  platform: string
): GlobalShortcutStatus {
  if (!enabled) return { state: 'disabled', accelerator: configured }
  const validation = validateGlobalShortcut(configured, platform)
  return validation.state === 'registered'
    ? { ...validation, state: 'unavailable' }
    : validation
}
