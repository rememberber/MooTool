import { describe, expect, it } from 'vitest'
import { canMoveJsonVaultEntry } from './JsonVaultPanel'

describe('canMoveJsonVaultEntry', () => {
  it('allows moving a nested entry back to the vault root', () => {
    expect(canMoveJsonVaultEntry('folder/data.json', '')).toBe(true)
  })

  it('ignores drops into the current directory', () => {
    expect(canMoveJsonVaultEntry('folder/data.json', 'folder')).toBe(false)
    expect(canMoveJsonVaultEntry('data.json', '')).toBe(false)
  })

  it('rejects moving a directory into itself or its descendants', () => {
    expect(canMoveJsonVaultEntry('folder', 'folder')).toBe(false)
    expect(canMoveJsonVaultEntry('folder', 'folder/nested')).toBe(false)
  })
})
