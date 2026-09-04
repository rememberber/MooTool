import { describe, expect, it, vi } from 'vitest'
import type { BoardMessage, QuickNote, ToolFavorite } from '../contracts/localData'
import { createLocalDataApi } from './localDataApi'

describe('local data API', () => {
  it('maps notes and board messages to the owned Rust commands', async () => {
    const note: QuickNote = {
      id: 'note-1',
      title: 'Tauri',
      content: 'Independent',
      tags: [],
      color: 'default',
      folderPath: '',
      editorFont: 'default',
      lineHeight: 'normal',
      lineWrapping: true,
      syntax: 'markdown',
      pinned: false,
      createdAt: 1,
      updatedAt: 1
    }
    const message: BoardMessage = {
      id: 'message-1',
      content: 'Ship',
      color: 'blue',
      pinned: true,
      createdAt: 2,
      updatedAt: 2
    }
    const invoke = vi.fn()
      .mockResolvedValueOnce([note])
      .mockResolvedValueOnce(note)
      .mockResolvedValueOnce(true)
      .mockResolvedValueOnce([message])
      .mockResolvedValueOnce(message)
      .mockResolvedValueOnce(true)
    const api = createLocalDataApi(invoke)

    await expect(api.listNotes()).resolves.toEqual([note])
    await api.saveNote(note)
    await api.deleteNote(note.id)
    await expect(api.listMessages()).resolves.toEqual([message])
    await api.saveMessage(message)
    await api.deleteMessage(message.id)

    expect(invoke.mock.calls).toEqual([
      ['list_quick_notes'],
      ['save_quick_note', { note }],
      ['delete_quick_note', { id: 'note-1' }],
      ['list_board_messages'],
      ['save_board_message', { message }],
      ['delete_board_message', { id: 'message-1' }]
    ])
  })

  it('maps nested quick-note folder operations to owned Rust commands', async () => {
    const folder = { path: 'work/tauri', createdAt: 1, updatedAt: 2 }
    const invoke = vi.fn()
      .mockResolvedValueOnce([folder])
      .mockResolvedValueOnce(folder)
      .mockResolvedValueOnce([{ ...folder, path: 'projects/tauri' }])
      .mockResolvedValueOnce(3)
    const api = createLocalDataApi(invoke)

    await api.listNoteFolders()
    await api.saveNoteFolder(folder)
    await api.renameNoteFolder('work', 'projects', 3)
    await api.deleteNoteFolder('projects')

    expect(invoke.mock.calls).toEqual([
      ['list_quick_note_folders'],
      ['save_quick_note_folder', { folder }],
      ['rename_quick_note_folder', { path: 'work', nextPath: 'projects', updatedAt: 3 }],
      ['delete_quick_note_folder', { path: 'projects' }]
    ])
  })

  it('maps tool preset favorites to owned Rust commands', async () => {
    const favorite: ToolFavorite = {
      id: 'favorite-1',
      toolId: 'regex',
      name: 'Identifiers',
      payloadJson: '{"pattern":"[a-z]+"}',
      createdAt: 1,
      updatedAt: 2
    }
    const invoke = vi.fn()
      .mockResolvedValueOnce([favorite])
      .mockResolvedValueOnce(favorite)
      .mockResolvedValueOnce(true)
    const api = createLocalDataApi(invoke)

    await expect(api.listToolFavorites('regex')).resolves.toEqual([favorite])
    await api.saveToolFavorite(favorite)
    await api.deleteToolFavorite(favorite.id)

    expect(invoke.mock.calls).toEqual([
      ['list_tool_favorites', { toolId: 'regex' }],
      ['save_tool_favorite', { favorite }],
      ['delete_tool_favorite', { id: 'favorite-1' }]
    ])
  })
})
