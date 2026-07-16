import { describe, expect, it } from 'vitest'
import { canMoveToDirectory } from './QuickNoteTree'

describe('canMoveToDirectory', () => {
  it('allows moving a nested entry back to the tree root', () => {
    expect(canMoveToDirectory('folder/note.txt', '')).toBe(true)
  })

  it('ignores drops into the current directory', () => {
    expect(canMoveToDirectory('folder/note.txt', 'folder')).toBe(false)
    expect(canMoveToDirectory('note.txt', '')).toBe(false)
  })

  it('rejects moving a directory into itself or its descendants', () => {
    expect(canMoveToDirectory('folder', 'folder')).toBe(false)
    expect(canMoveToDirectory('folder', 'folder/nested')).toBe(false)
  })
})
