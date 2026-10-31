import { describe, expect, it } from 'vitest'
import { canMoveVaultEntry, renamedVaultPath, uniqueDuplicatePath, vaultDragDestination } from './jsonVaultActions'

describe('JSON Vault rename paths', () => {
  it('keeps the parent directory and restores the json extension', () => {
    expect(renamedVaultPath('Work/API/example.json', '接口.json', true)).toBe('Work/API/接口.json')
  })

  it('replaces reserved characters and drops a leading dot', () => {
    expect(renamedVaultPath('notes/example.json', ' ..a:b', true)).toBe('notes/a_b.json')
    expect(renamedVaultPath('Work/API', 'new/name', false)).toBe('Work/new_name')
  })

  it('rejects a blank name', () => {
    expect(() => renamedVaultPath('example.json', '   .json', true)).toThrow('Invalid Vault name')
  })

  it('allows a nested entry to move to the vault root', () => {
    expect(canMoveVaultEntry('folder/data.json', '')).toBe(true)
    expect(vaultDragDestination('folder/data.json', '')).toBe('data.json')
  })

  it('ignores a drop into the current directory', () => {
    expect(canMoveVaultEntry('folder/data.json', 'folder')).toBe(false)
    expect(canMoveVaultEntry('data.json', '')).toBe(false)
  })

  it('rejects moving a directory into itself or a descendant', () => {
    expect(canMoveVaultEntry('folder', 'folder')).toBe(false)
    expect(canMoveVaultEntry('folder', 'folder/nested')).toBe(false)
    expect(vaultDragDestination('Work/API/example.json', 'notes')).toBe('notes/example.json')
  })

  it('names copies the way Electron does, skipping names that already exist', () => {
    expect(uniqueDuplicatePath('Work/example.json', new Set())).toBe('Work/example Copy.json')
    expect(uniqueDuplicatePath('Work/example.json', new Set(['Work/example Copy.json']))).toBe('Work/example Copy (2).json')
  })
})
