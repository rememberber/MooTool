import { describe, expect, it } from 'vitest'
import { displayShortcut, normalizeShortcut, previewGlobalShortcutStatus, validateGlobalShortcut } from './shortcuts'

describe('global shortcut contract', () => {
  it('normalizes the Electron default and display form', () => {
    expect(normalizeShortcut('CommandOrControl+Shift+Space', 'darwin')).toBe('Command+Shift+Space')
    expect(normalizeShortcut('cmdorctrl+shift+space', 'win32')).toBe('Control+Shift+Space')
    expect(displayShortcut('CommandOrControl+Shift+Space', 'darwin')).toBe('Cmd+Shift+Space')
    expect(displayShortcut('CommandOrControl+Shift+Space', 'win32')).toBe('Ctrl+Shift+Space')
  })

  it('rejects reserved, shift-only, and conflicting combinations', () => {
    expect(validateGlobalShortcut('CommandOrControl+K', 'darwin').state).toBe('conflict')
    expect(validateGlobalShortcut('CommandOrControl+,', 'linux').state).toBe('conflict')
    expect(validateGlobalShortcut('Shift+A', 'darwin').state).toBe('invalid')
    expect(validateGlobalShortcut('Command+Shift+P', 'win32').state).toBe('invalid')
    expect(previewGlobalShortcutStatus(false, 'CommandOrControl+Shift+Space', 'darwin')).toEqual({
      state: 'disabled',
      accelerator: 'CommandOrControl+Shift+Space'
    })
    expect(previewGlobalShortcutStatus(true, 'Alt+Space', 'darwin').state).toBe('unavailable')
  })
})
