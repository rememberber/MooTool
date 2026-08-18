import { describe, expect, it, vi } from 'vitest'
import type { BoardMessage, QuickNote } from '../contracts/localData'
import { createLocalDataApi } from './localDataApi'

describe('local data API', () => {
  it('maps notes and board messages to the owned Rust commands', async () => {
    const note: QuickNote = {
      id: 'note-1',
      title: 'Tauri',
      content: 'Independent',
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
})
