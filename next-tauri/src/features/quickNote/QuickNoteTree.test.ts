import { describe, expect, it } from 'vitest'
import type { QuickNote } from '../../platform/contracts/localData'
import { buildNoteTree, canMoveToDirectory } from './QuickNoteTree'

function note(overrides: Partial<QuickNote> & Pick<QuickNote, 'id' | 'title' | 'content' | 'relativePath'>): QuickNote {
  return {
    tags: [],
    color: 'default',
    folderPath: '',
    editorFont: 'default',
    lineHeight: 'normal',
    fontSize: 14,
    fontName: '',
    lineSpacing: 1,
    lineWrapping: true,
    syntax: 'text/markdown',
    pinned: false,
    createdAt: 1,
    updatedAt: 1,
    ...overrides
  }
}

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
      syntax: 'text/markdown',
      pinned: false,
      createdAt: 1,
      updatedAt: 1
    }], [{ path: 'Work', createdAt: 1, updatedAt: 1 }], '', '')
    expect(folder?.path).toBe('Work')
    expect(folder?.children[0]?.path).toBe('Work/API ideas.md')
    expect(folder?.children[0]?.name).toBe('API ideas')
  })

  it('searches title and path by default, and can ignore note content', () => {
    const notes = [note({ id: 'body', title: 'Visible', content: 'secret token', relativePath: 'Visible.md' }), note({ id: 'path', title: 'Other', content: '', relativePath: 'Archive/secret.md', folderPath: 'Archive' })]
    const folders = [{ path: 'Archive', createdAt: 1, updatedAt: 1 }]
    expect(buildNoteTree(notes, folders, 'secret', '').map((node) => node.path)).toEqual(['Archive', 'Visible.md'])
    expect(buildNoteTree(notes, folders, 'secret', '', { includeContent: false }).map((node) => node.path)).toEqual(['Archive'])
  })

  it('sorts directories by name and files by modification time or file name', () => {
    const notes = [
      note({ id: 'old', title: 'Alpha', content: '', relativePath: 'zed.md', updatedAt: 1, createdAt: 5 }),
      note({ id: 'new', title: 'Zed', content: '', relativePath: 'alpha.md', updatedAt: 9, createdAt: 2 })
    ]
    expect(buildNoteTree(notes, [], '', '').map((node) => node.path)).toEqual(['alpha.md', 'zed.md'])
    expect(buildNoteTree(notes, [], '', '', { sort: 'created' }).map((node) => node.path)).toEqual(['zed.md', 'alpha.md'])
    expect(buildNoteTree(notes, [], '', '', { sort: 'name' }).map((node) => node.name)).toEqual(['Zed', 'Alpha'])
  })

  it('keeps ancestor folders when only a nested folder name matches', () => {
    const tree = buildNoteTree([], [{ path: 'Work', createdAt: 1, updatedAt: 1 }, { path: 'Work/API', createdAt: 1, updatedAt: 1 }], 'api', '')
    expect(tree.map((node) => node.path)).toEqual(['Work'])
    expect(tree[0]?.children.map((node) => node.path)).toEqual(['Work/API'])
  })

  it('rejects moving a directory into itself or its descendants', () => {
    expect(canMoveToDirectory('folder', 'folder')).toBe(false)
    expect(canMoveToDirectory('folder', 'folder/nested')).toBe(false)
  })
})
