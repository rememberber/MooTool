import { describe, expect, it } from 'vitest'
import { normalizeShortcut, validateGlobalShortcut } from './shortcuts'
import { defaultAppSettings, mergeSettings } from './settings'

describe('global shortcut validation', () => {
  it('resolves aliases and modifier order on each platform', () => {
    expect(normalizeShortcut(' shift + CmdOrCtrl + space ', 'darwin')).toBe('Command+Shift+Space')
    expect(normalizeShortcut(' shift + CmdOrCtrl + space ', 'win32')).toBe('Control+Shift+Space')
    expect(normalizeShortcut('alt+ctrl+F12', 'linux')).toBe('Control+Alt+F12')
    expect(normalizeShortcut('Command+Shift+Space', 'win32')).toBeNull()
  })

  it('rejects unmodified keys, modifier-only combinations and ambiguous duplicates', () => {
    for (const shortcut of ['Space', 'Shift+Space', 'Ctrl', 'Ctrl+Alt', 'Ctrl+Ctrl+K', 'Ctrl+CmdOrCtrl+K', 'Alt+Unknown', 'Ctrl++K', 'Ctrl+F25']) {
      expect(normalizeShortcut(shortcut, 'win32')).toBeNull()
    }
  })

  it('detects app shortcut conflicts after resolving platform-specific aliases', () => {
    expect(validateGlobalShortcut('Ctrl+K', 'win32').state).toBe('conflict')
    expect(validateGlobalShortcut('Cmd+,', 'darwin').state).toBe('conflict')
    expect(validateGlobalShortcut('Super+K', 'darwin').state).toBe('conflict')
    expect(validateGlobalShortcut('Ctrl+K', 'darwin').state).toBe('conflict')
    expect(validateGlobalShortcut('Cmd+Alt+Shift+K', 'darwin').state).toBe('conflict')
    expect(validateGlobalShortcut('Shift+Cmd+F', 'darwin').state).toBe('conflict')
    expect(validateGlobalShortcut('CmdOrCtrl+Shift+Space', 'darwin').state).toBe('registered')
  })

  it('migrates old settings without opting into a global shortcut', () => {
    const migrated = mergeSettings(defaultAppSettings, { schemaVersion: 13, shortcuts: { search: 'CommandOrControl+K', settings: 'CommandOrControl+,' } })
    expect(migrated.shortcuts.globalSearchEnabled).toBe(false)
    expect(migrated.shortcuts.globalSearch).toBe('CommandOrControl+Shift+Space')
  })
})
