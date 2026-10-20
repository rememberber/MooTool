import { describe, expect, it } from 'vitest'
import { buildNoteTree, canMoveToDirectory } from './QuickNoteTree'

describe('canMoveToDirectory', () => {
  it('allows moving a nested entry back to the tree root', () => {
    expect(canMoveToDirectory('folder/note.txt', '')).toBe(true)
  })

  it('ignores drops into the current directory', () => {
    expect(canMoveToDirectory('folder/note.txt', 'folder')).toBe(false)
    expect(canMoveToDirectory('note.txt', '')).toBe(false)
  })

  it('uses the vault file path for notes that have one', () => {
    const [folder] = buildNoteTree([{
      id: 'note-1',
      title: 'API ideas',
      content: '',
      tags: [],
      color: 'default',
      folderPath: 'Work',
      editorFont: 'default',
      lineHeight: 'normal',
      fontSize: 14,
      fontName: '',
      lineSpacing: 1,
      relativePath: 'Work/API ideas.md',
      lineWrapping: true,
      syntax: 'markdown',
      pinned: false,
      createdAt: 1,
      updatedAt: 1
    }], [{ path: 'Work', createdAt: 1, updatedAt: 1 }], '', '')
    expect(folder?.path).toBe('Work')
    expect(folder?.children[0]?.path).toBe('Work/API ideas.md')
    expect(folder?.children[0]?.name).toBe('API ideas')
  })

  it('rejects moving a directory into itself or its descendants', () => {
    expect(canMoveToDirectory('folder', 'folder')).toBe(false)
    expect(canMoveToDirectory('folder', 'folder/nested')).toBe(false)
  })
})
