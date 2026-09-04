import { invoke } from '@tauri-apps/api/core'
import type {
  BoardMessage,
  HostProfile,
  LocalDataApi,
  QuickNote,
  QuickNoteFolder,
  ToolFavorite,
  TranslationHistory,
  TranslationWord
} from '../contracts/localData'

type Invoke = <T>(command: string, args?: Record<string, unknown>) => Promise<T>

const NOTES_KEY = 'mootool-next-tauri:quick-notes:v1'
const NOTE_FOLDERS_KEY = 'mootool-next-tauri:quick-note-folders:v1'
const TOOL_FAVORITES_KEY = 'mootool-next-tauri:tool-favorites:v1'
const MESSAGES_KEY = 'mootool-next-tauri:board-messages:v1'
const HOST_PROFILES_KEY = 'mootool-next-tauri:host-profiles:v1'
const TRANSLATION_WORDS_KEY = 'mootool-next-tauri:translation-words:v1'
const TRANSLATION_HISTORY_KEY = 'mootool-next-tauri:translation-history:v1'

export function createLocalDataApi(invokeCommand: Invoke = invoke): LocalDataApi {
  return {
    listNotes: () => invokeCommand<QuickNote[]>('list_quick_notes'),
    saveNote: (note) => invokeCommand<QuickNote>('save_quick_note', { note }),
    deleteNote: (id) => invokeCommand<boolean>('delete_quick_note', { id }),
    listNoteFolders: () => invokeCommand<QuickNoteFolder[]>('list_quick_note_folders'),
    saveNoteFolder: (folder) => invokeCommand<QuickNoteFolder>('save_quick_note_folder', { folder }),
    renameNoteFolder: (path, nextPath, updatedAt) => invokeCommand<QuickNoteFolder[]>('rename_quick_note_folder', { path, nextPath, updatedAt }),
    deleteNoteFolder: (path) => invokeCommand<number>('delete_quick_note_folder', { path }),
    listToolFavorites: (toolId) => invokeCommand<ToolFavorite[]>('list_tool_favorites', { toolId }),
    saveToolFavorite: (favorite) => invokeCommand<ToolFavorite>('save_tool_favorite', { favorite }),
    deleteToolFavorite: (id) => invokeCommand<boolean>('delete_tool_favorite', { id }),
    listMessages: () => invokeCommand<BoardMessage[]>('list_board_messages'),
    saveMessage: (message) => invokeCommand<BoardMessage>('save_board_message', { message }),
    deleteMessage: (id) => invokeCommand<boolean>('delete_board_message', { id }),
    listHostProfiles: () => invokeCommand<HostProfile[]>('list_host_profiles'),
    saveHostProfile: (profile) => invokeCommand<HostProfile>('save_host_profile', { profile }),
    deleteHostProfile: (id) => invokeCommand<boolean>('delete_host_profile', { id }),
    listTranslationWords: () => invokeCommand<TranslationWord[]>('list_translation_words'),
    saveTranslationWord: (word) => invokeCommand<TranslationWord>('save_translation_word', { word }),
    deleteTranslationWord: (id) => invokeCommand<boolean>('delete_translation_word', { id }),
    listTranslationHistory: () => invokeCommand<TranslationHistory[]>('list_translation_history'),
    deleteTranslationHistory: (id) => invokeCommand<boolean>('delete_translation_history', { id }),
    clearTranslationHistory: () => invokeCommand<number>('clear_translation_history')
  }
}

function createBrowserLocalDataApi(): LocalDataApi {
  return {
    listNotes: async () => readBrowserList<QuickNote>(NOTES_KEY).map(normalizeQuickNote),
    saveNote: async (note) => {
      writeBrowserList(NOTES_KEY, upsert(readBrowserList<QuickNote>(NOTES_KEY), note))
      return note
    },
    deleteNote: async (id) => deleteBrowserItem<QuickNote>(NOTES_KEY, id),
    listNoteFolders: async () => readFolderList(),
    saveNoteFolder: async (folder) => {
      const values = readFolderList()
      writeBrowserList(NOTE_FOLDERS_KEY, [folder, ...values.filter((item) => item.path !== folder.path)])
      return folder
    },
    renameNoteFolder: async (path, nextPath, updatedAt) => {
      const rename = (value: string) => value === path ? nextPath : value.startsWith(`${path}/`) ? `${nextPath}${value.slice(path.length)}` : value
      const folders = readFolderList().map((folder) => ({ ...folder, path: rename(folder.path), updatedAt }))
      const notes = readBrowserList<QuickNote>(NOTES_KEY).map(normalizeQuickNote).map((note) => ({ ...note, folderPath: rename(note.folderPath), updatedAt: note.folderPath === rename(note.folderPath) ? note.updatedAt : updatedAt }))
      writeBrowserList(NOTE_FOLDERS_KEY, folders)
      writeBrowserList(NOTES_KEY, notes)
      return folders
    },
    deleteNoteFolder: async (path) => {
      const inside = (value: string) => value === path || value.startsWith(`${path}/`)
      const folders = readFolderList()
      const notes = readBrowserList<QuickNote>(NOTES_KEY).map(normalizeQuickNote)
      const moved = notes.filter((note) => inside(note.folderPath)).length
      writeBrowserList(NOTE_FOLDERS_KEY, folders.filter((folder) => !inside(folder.path)))
      writeBrowserList(NOTES_KEY, notes.map((note) => inside(note.folderPath) ? { ...note, folderPath: '', updatedAt: Date.now() } : note))
      return moved
    },
    listToolFavorites: async (toolId) => readToolFavorites().filter((item) => item.toolId === toolId).sort((left, right) => right.updatedAt - left.updatedAt),
    saveToolFavorite: async (favorite) => {
      const values = readToolFavorites()
      const existing = values.find((item) => item.toolId === favorite.toolId && item.name === favorite.name)
      const saved = existing ? { ...favorite, id: existing.id, createdAt: existing.createdAt } : favorite
      writeBrowserList(TOOL_FAVORITES_KEY, [saved, ...values.filter((item) => item.id !== saved.id && !(item.toolId === saved.toolId && item.name === saved.name))])
      return saved
    },
    deleteToolFavorite: async (id) => {
      const values = readToolFavorites()
      const next = values.filter((item) => item.id !== id)
      writeBrowserList(TOOL_FAVORITES_KEY, next)
      return next.length !== values.length
    },
    listMessages: async () => readBrowserList<BoardMessage>(MESSAGES_KEY),
    saveMessage: async (message) => {
      writeBrowserList(MESSAGES_KEY, upsert(readBrowserList<BoardMessage>(MESSAGES_KEY), message))
      return message
    },
    deleteMessage: async (id) => deleteBrowserItem<BoardMessage>(MESSAGES_KEY, id),
    listHostProfiles: async () => readPlainBrowserList<HostProfile>(HOST_PROFILES_KEY),
    saveHostProfile: async (profile) => {
      const values = readPlainBrowserList<HostProfile>(HOST_PROFILES_KEY)
      writeBrowserList(HOST_PROFILES_KEY, upsert(values, profile))
      return profile
    },
    deleteHostProfile: async (id) => deletePlainBrowserItem<HostProfile>(HOST_PROFILES_KEY, id),
    listTranslationWords: async () => readPlainBrowserList<TranslationWord>(TRANSLATION_WORDS_KEY),
    saveTranslationWord: async (word) => {
      writeBrowserList(
        TRANSLATION_WORDS_KEY,
        upsert(readPlainBrowserList<TranslationWord>(TRANSLATION_WORDS_KEY), word)
      )
      return word
    },
    deleteTranslationWord: async (id) => deletePlainBrowserItem<TranslationWord>(TRANSLATION_WORDS_KEY, id),
    listTranslationHistory: async () => readHistoryBrowserList(),
    deleteTranslationHistory: async (id) => {
      const values = readHistoryBrowserList()
      const next = values.filter((item) => item.id !== id)
      writeBrowserList(TRANSLATION_HISTORY_KEY, next)
      return values.length !== next.length
    },
    clearTranslationHistory: async () => {
      const count = readHistoryBrowserList().length
      window.localStorage.removeItem(TRANSLATION_HISTORY_KEY)
      return count
    }
  }
}

function normalizeQuickNote(note: QuickNote): QuickNote {
  return {
    ...note,
    tags: Array.isArray(note.tags) ? note.tags.filter((tag) => typeof tag === 'string') : [],
    color: ['default', 'coral', 'yellow', 'green', 'blue', 'purple', 'red'].includes(note.color) ? note.color : 'default',
    folderPath: typeof note.folderPath === 'string' ? note.folderPath : '',
    editorFont: ['default', 'mono', 'serif'].includes(note.editorFont) ? note.editorFont : 'default',
    lineHeight: ['compact', 'normal', 'relaxed'].includes(note.lineHeight) ? note.lineHeight : 'normal',
    lineWrapping: typeof note.lineWrapping === 'boolean' ? note.lineWrapping : true,
    syntax: ['markdown', 'plain', 'json', 'yaml'].includes(note.syntax) ? note.syntax : 'markdown'
  }
}

function readHistoryBrowserList(): TranslationHistory[] {
  try {
    const raw = window.localStorage.getItem(TRANSLATION_HISTORY_KEY)
    const values = raw ? JSON.parse(raw) as TranslationHistory[] : []
    return values.sort((left, right) => right.createdAt - left.createdAt).slice(0, 500)
  } catch {
    return []
  }
}

function readFolderList(): QuickNoteFolder[] {
  try {
    const raw = window.localStorage.getItem(NOTE_FOLDERS_KEY)
    const values = raw ? JSON.parse(raw) as QuickNoteFolder[] : []
    return values.filter((folder) => folder && typeof folder.path === 'string').sort((left, right) => left.path.localeCompare(right.path))
  } catch {
    return []
  }
}

function readToolFavorites(): ToolFavorite[] {
  try {
    const raw = window.localStorage.getItem(TOOL_FAVORITES_KEY)
    return raw ? JSON.parse(raw) as ToolFavorite[] : []
  } catch {
    return []
  }
}

function readPlainBrowserList<T extends { id: string; updatedAt: number }>(key: string): T[] {
  try {
    const raw = window.localStorage.getItem(key)
    const values = raw ? JSON.parse(raw) as T[] : []
    return values.sort((left, right) => right.updatedAt - left.updatedAt)
  } catch {
    return []
  }
}

function deletePlainBrowserItem<T extends { id: string; updatedAt: number }>(key: string, id: string): boolean {
  const values = readPlainBrowserList<T>(key)
  const next = values.filter((item) => item.id !== id)
  writeBrowserList(key, next)
  return values.length !== next.length
}

function readBrowserList<T extends { id: string; pinned: boolean; updatedAt: number }>(key: string): T[] {
  try {
    const raw = window.localStorage.getItem(key)
    const values = raw ? JSON.parse(raw) as T[] : []
    return values.sort((left, right) => Number(right.pinned) - Number(left.pinned) || right.updatedAt - left.updatedAt)
  } catch {
    return []
  }
}

function writeBrowserList<T>(key: string, values: T[]): void {
  window.localStorage.setItem(key, JSON.stringify(values))
}

function upsert<T extends { id: string }>(values: T[], value: T): T[] {
  return [value, ...values.filter((item) => item.id !== value.id)]
}

function deleteBrowserItem<T extends { id: string; pinned: boolean; updatedAt: number }>(
  key: string,
  id: string
): boolean {
  const values = readBrowserList<T>(key)
  const next = values.filter((item) => item.id !== id)
  writeBrowserList(key, next)
  return next.length !== values.length
}

export const localDataApi: LocalDataApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? createLocalDataApi()
  : createBrowserLocalDataApi()
