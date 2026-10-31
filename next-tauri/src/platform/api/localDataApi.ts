import { invoke } from '@tauri-apps/api/core'
import { listStoredHostProfiles, replaceStoredHostProfile, saveStoredHostProfile } from '../../features/host/hostProfileStore'
import { listStoredTranslationHistory, listStoredTranslationWords, saveStoredTranslationWord } from '../../features/translation/translationRecords'
import {
  normalizeNoteSyntax,
  type BoardMessage,
  type HostProfile,
  type LocalDataApi,
  type CreateQuickNoteInput,
  type QuickNote,
  type QuickNoteFolder,
  type ToolFavorite,
  type TranslationHistory,
  type TranslationWord
} from '../contracts/localData'

type Invoke = <T>(command: string, args?: Record<string, unknown>) => Promise<T>

const NOTES_KEY = 'mootool-next-tauri:quick-notes:v1'
const NOTE_FOLDERS_KEY = 'mootool-next-tauri:quick-note-folders:v1'
const TOOL_FAVORITES_KEY = 'mootool-next-tauri:tool-favorites:v1'
const MESSAGES_KEY = 'mootool-next-tauri:board-messages:v1'
const HOST_PROFILES_KEY = 'mootool-next-tauri:host-profiles:v2'
const TRANSLATION_WORDS_KEY = 'mootool-next-tauri:translation-words:v2'
const TRANSLATION_HISTORY_KEY = 'mootool-next-tauri:translation-history:v2'

export function createLocalDataApi(invokeCommand: Invoke = invoke): LocalDataApi {
  return {
    listNotes: () => invokeCommand<QuickNote[]>('list_quick_notes'),
    saveNote: (note) => invokeCommand<QuickNote>('save_quick_note', { note }),
    createNote: (input) => invokeCommand<QuickNote>('create_quick_note', { ...input }),
    duplicateNote: (relativePath) => invokeCommand<QuickNote>('duplicate_quick_note', { relativePath }),
    moveNoteEntry: (relativePath, targetDirectory) => invokeCommand<QuickNote>('move_quick_note_entry', { relativePath, targetDirectory }),
    renameNoteEntry: (relativePath, name) => invokeCommand<QuickNote>('rename_quick_note_entry', { relativePath, name }),
    deleteNote: (id) => invokeCommand<boolean>('delete_quick_note', { id }),
    revealNoteEntry: (relativePath) => invokeCommand<void>('reveal_quick_note_entry', { relativePath }),
    exportNotes: (directory, relativePaths) => invokeCommand<number>('export_quick_note_files', { directory, relativePaths }),
    openNoteVault: () => invokeCommand<void>('open_quick_note_vault'),
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
    listHostProfiles: (keyword = '', includeContent = true) => invokeCommand<HostProfile[]>('list_host_profiles', { keyword, includeContent }),
    saveHostProfile: (profile) => invokeCommand<HostProfile>('save_host_profile', { profile }),
    deleteHostProfile: (id) => invokeCommand<boolean>('delete_host_profile', { id }),
    listTranslationWords: (keyword = '') => invokeCommand<TranslationWord[]>('list_translation_words', { keyword }),
    saveTranslationWord: (word) => invokeCommand<TranslationWord>('save_translation_word', { word }),
    deleteTranslationWord: (id) => invokeCommand<boolean>('delete_translation_word', { id }),
    listTranslationHistory: (keyword = '') => invokeCommand<TranslationHistory[]>('list_translation_history', { keyword }),
    deleteTranslationHistory: (id) => invokeCommand<boolean>('delete_translation_history', { id }),
    clearTranslationHistory: () => invokeCommand<number>('clear_translation_history')
  }
}

function createBrowserLocalDataApi(): LocalDataApi {
  return {
    listNotes: async () => readBrowserList<QuickNote>(NOTES_KEY).map(normalizeQuickNote),
    saveNote: async (note) => {
      const notes = readBrowserList<QuickNote>(NOTES_KEY).map(normalizeQuickNote)
      const existing = notes.find((item) => item.id === note.id)
      const normalized = normalizeQuickNote({ ...note, relativePath: note.relativePath || existing?.relativePath || '' })
      const saved = { ...normalized, relativePath: browserNotePath(normalized) }
      writeBrowserList(NOTES_KEY, upsert(notes, saved))
      return saved
    },
    createNote: async (input) => createBrowserNote(input),
    renameNoteEntry: async (relativePath, name) => {
      const notes = readBrowserList<QuickNote>(NOTES_KEY).map(normalizeQuickNote)
      const source = notes.find((note) => note.relativePath === relativePath || note.id === relativePath)
      if (!source) throw new Error('Vault entry does not exist')
      const title = name.trim()
      if (!title || title.length > 180 || /[\r\n\0]/.test(title)) throw new Error('Invalid title')
      const sanitized = title.replace(/[\\/:*?"<>|]/g, '_').replace(/^\.+/, '').trim()
      if (!sanitized) throw new Error('Invalid file name')
      const leaf = (source.relativePath || `${source.title || 'note'}.txt`).split('/').pop() ?? 'note.txt'
      const dot = leaf.lastIndexOf('.')
      const extension = dot > 0 ? leaf.slice(dot) : ''
      const nextLeaf = `${sanitized.slice(0, 180)}${extension}`
      const parent = source.folderPath
      const taken = notes.some((note) => note.id !== source.id && note.folderPath === parent && (note.relativePath || '').split('/').pop() === nextLeaf)
      if (taken) throw new Error('An entry with that name already exists')
      const renamed: QuickNote = {
        ...source,
        title,
        relativePath: parent ? `${parent}/${nextLeaf}` : nextLeaf,
        updatedAt: Date.now()
      }
      writeBrowserList(NOTES_KEY, notes.map((note) => note.id === source.id ? renamed : note))
      return renamed
    },
    moveNoteEntry: async (relativePath, targetDirectory) => {
      const notes = readBrowserList<QuickNote>(NOTES_KEY).map(normalizeQuickNote)
      const source = notes.find((note) => note.relativePath === relativePath || note.id === relativePath)
      if (!source) throw new Error('Vault entry does not exist')
      const currentParent = source.folderPath
      if (currentParent === targetDirectory) return source
      const leaf = (source.relativePath || `${source.title || 'note'}.txt`).split('/').pop() ?? 'note.txt'
      const taken = new Set(notes.filter((note) => note.id !== source.id && note.folderPath === targetDirectory).map((note) => (note.relativePath || '').split('/').pop()))
      let name = leaf
      if (taken.has(name)) {
        const dot = leaf.lastIndexOf('.')
        const stem = dot > 0 ? leaf.slice(0, dot) : leaf
        const extension = dot > 0 ? leaf.slice(dot) : ''
        for (let index = 1; index < 10_000 && taken.has(name); index += 1) name = `${stem} (${index + 1})${extension}`
      }
      const moved: QuickNote = {
        ...source,
        folderPath: targetDirectory,
        relativePath: targetDirectory ? `${targetDirectory}/${name}` : name,
        updatedAt: Date.now()
      }
      writeBrowserList(NOTES_KEY, notes.map((note) => note.id === source.id ? moved : note))
      return moved
    },
    duplicateNote: async (relativePath) => {
      const notes = readBrowserList<QuickNote>(NOTES_KEY).map(normalizeQuickNote)
      const source = notes.find((note) => note.relativePath === relativePath || note.id === relativePath)
      if (!source) throw new Error('Vault entry does not exist')
      const now = Date.now()
      const copy: QuickNote = {
        ...source,
        id: crypto.randomUUID(),
        title: `${source.title.trim() || 'note'} Copy`,
        pinned: false,
        relativePath: '',
        createdAt: now,
        updatedAt: now
      }
      writeBrowserList(NOTES_KEY, [copy, ...notes])
      return copy
    },
    deleteNote: async (id) => deleteBrowserItem<QuickNote>(NOTES_KEY, id),
    revealNoteEntry: async () => { throw new Error('Reveal in the file manager requires the Tauri desktop app') },
    exportNotes: async () => { throw new Error('Exporting quick notes requires the Tauri desktop app') },
    openNoteVault: async () => { throw new Error('Opening the quick note vault requires the Tauri desktop app') },
    listNoteFolders: async () => readFolderList(),
    saveNoteFolder: async (folder) => {
      const values = readFolderList()
      if (values.some((item) => item.path === folder.path)) throw new Error('An entry with that name already exists')
      const parent = folder.path.includes('/') ? folder.path.slice(0, folder.path.lastIndexOf('/')) : ''
      const notes = readBrowserList<QuickNote>(NOTES_KEY).map(normalizeQuickNote)
      const parentExists = !parent || values.some((item) => item.path === parent) || notes.some((note) => note.folderPath === parent || note.folderPath.startsWith(`${parent}/`))
      if (!parentExists) throw new Error('failed to create folder: parent does not exist')
      writeBrowserList(NOTE_FOLDERS_KEY, [folder, ...values])
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
      writeBrowserList(NOTES_KEY, notes.filter((note) => !inside(note.folderPath)))
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
    listHostProfiles: async (keyword = '', includeContent = true) => listStoredHostProfiles(readHostProfiles(), keyword, includeContent),
    saveHostProfile: async (profile) => {
      const saved = saveStoredHostProfile(readHostProfiles(), profile)
      writeBrowserList(HOST_PROFILES_KEY, replaceStoredHostProfile(readHostProfiles(), saved))
      return saved
    },
    deleteHostProfile: async (id) => {
      const values = readHostProfiles()
      writeBrowserList(HOST_PROFILES_KEY, values.filter((profile) => profile.id !== id))
      return values.some((profile) => profile.id === id)
    },
    listTranslationWords: async (keyword = '') => listStoredTranslationWords(readTranslationWords(), keyword),
    saveTranslationWord: async (word) => {
      const saved = saveStoredTranslationWord(readTranslationWords(), word)
      writeBrowserList(TRANSLATION_WORDS_KEY, listStoredTranslationWords([saved, ...readTranslationWords().filter((item) => item.id !== saved.id)]))
      return saved
    },
    deleteTranslationWord: async (id) => {
      const values = readTranslationWords()
      writeBrowserList(TRANSLATION_WORDS_KEY, values.filter((word) => word.id !== id))
      return values.some((word) => word.id === id)
    },
    listTranslationHistory: async (keyword = '') => listStoredTranslationHistory(readTranslationHistory(), keyword),
    deleteTranslationHistory: async (id) => {
      const values = readTranslationHistory()
      writeBrowserList(TRANSLATION_HISTORY_KEY, values.filter((item) => item.id !== id))
      return values.some((item) => item.id === id)
    },
    clearTranslationHistory: async () => {
      const count = readTranslationHistory().length
      window.localStorage.removeItem(TRANSLATION_HISTORY_KEY)
      return count
    }
  }
}

function clampNoteFontSize(value: number): number {
  return Number.isFinite(value) ? Math.min(48, Math.max(8, Math.round(value))) : 14
}

function normalizeLineSpacing(value: number, lineHeight: QuickNote['lineHeight']): number {
  const choices = [1, 1.2, 1.4, 1.6, 1.8, 2]
  if (choices.includes(value)) return value
  if (lineHeight === 'compact') return 1
  if (lineHeight === 'relaxed') return 1.6
  return 1.2
}

function browserNotePath(note: Pick<QuickNote, 'title' | 'folderPath' | 'syntax' | 'relativePath'>): string {
  const extension = note.syntax === 'text/markdown' ? 'md'
    : note.syntax === 'application/json' ? 'json'
      : note.syntax === 'text/java' ? 'java'
        : note.syntax === 'text/javascript' ? 'js'
          : note.syntax === 'text/typescript' ? 'ts'
            : note.syntax === 'text/python' ? 'py'
              : note.syntax === 'text/html' ? 'html'
                : note.syntax === 'text/xml' ? 'xml'
                  : note.syntax === 'text/yaml' ? 'yaml'
                    : note.syntax === 'text/sql' ? 'sql'
                      : 'txt'
  const current = note.relativePath.trim()
  const leaf = current ? current.split('/').pop() ?? current : `${note.title.trim().replace(/[\\/:*?"<>|]/g, '_').replace(/^\.+/, '').trim().slice(0, 180) || 'note'}.${extension}`
  const stem = leaf.replace(/\.[^.]+$/, '') || 'note'
  const nextLeaf = `${stem}.${extension}`
  return note.folderPath ? `${note.folderPath}/${nextLeaf}` : nextLeaf
}

function noteFontName(note: Pick<QuickNote, 'fontName' | 'editorFont'>): string {
  const name = typeof note.fontName === 'string' ? note.fontName.slice(0, 120) : ''
  if (name.trim()) return name
  if (note.editorFont === 'mono') return 'ui-monospace'
  if (note.editorFont === 'serif') return 'Georgia'
  return ''
}

function normalizeQuickNote(note: QuickNote): QuickNote {
  const syntax = normalizeNoteSyntax(note.syntax)
  const normalized = {
    ...note,
    tags: Array.isArray(note.tags) ? note.tags.filter((tag) => typeof tag === 'string') : [],
    color: ['default', 'coral', 'yellow', 'green', 'blue', 'purple', 'red'].includes(note.color) ? note.color : 'default',
    folderPath: typeof note.folderPath === 'string' ? note.folderPath : '',
    editorFont: ['default', 'mono', 'serif'].includes(note.editorFont) ? note.editorFont : 'default',
    lineHeight: ['compact', 'normal', 'relaxed'].includes(note.lineHeight) ? note.lineHeight : 'normal',
    fontSize: clampNoteFontSize(note.fontSize),
    fontName: noteFontName(note),
    lineSpacing: normalizeLineSpacing(note.lineSpacing, note.lineHeight),
    lineWrapping: typeof note.lineWrapping === 'boolean' ? note.lineWrapping : true,
    syntax
  }
  return {
    ...normalized,
    relativePath: typeof note.relativePath === 'string' && note.relativePath ? note.relativePath : browserNotePath(normalized)
  }
}

function readTranslationWords(): TranslationWord[] {
  try {
    const raw = window.localStorage.getItem(TRANSLATION_WORDS_KEY)
    const values = raw ? JSON.parse(raw) as TranslationWord[] : []
    return values.filter((word) => Number.isInteger(word.id) && typeof word.sourceText === 'string')
  } catch {
    return []
  }
}

function readTranslationHistory(): TranslationHistory[] {
  try {
    const raw = window.localStorage.getItem(TRANSLATION_HISTORY_KEY)
    const values = raw ? JSON.parse(raw) as TranslationHistory[] : []
    return values.filter((item) => Number.isInteger(item.id) && typeof item.sourceText === 'string')
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

function readHostProfiles(): HostProfile[] {
  try {
    const raw = window.localStorage.getItem(HOST_PROFILES_KEY)
    const values = raw ? JSON.parse(raw) as HostProfile[] : []
    return values.filter((profile) => Number.isInteger(profile.id) && typeof profile.name === 'string')
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

function createBrowserNote(input: CreateQuickNoteInput): QuickNote {
  const title = input.title.trim()
  if (!title || title.length > 180 || /[\r\n\0]/.test(title)) throw new Error('Invalid title')
  const sanitized = title.replace(/[\\/:*?"<>|]/g, '_').replace(/^\.+/, '').trim().slice(0, 180)
  if (!sanitized) throw new Error('Invalid file name')
  const notes = readBrowserList<QuickNote>(NOTES_KEY).map(normalizeQuickNote)
  const taken = new Set(notes.filter((note) => note.folderPath === input.parentPath).map((note) => (note.relativePath || '').split('/').pop()))
  let leaf = `${sanitized}.txt`
  for (let index = 1; index < 10_000 && taken.has(leaf); index += 1) leaf = `${sanitized} (${index + 1}).txt`
  const now = Date.now()
  const note: QuickNote = {
    id: crypto.randomUUID(),
    title,
    content: '',
    tags: [],
    color: 'default',
    folderPath: input.parentPath,
    editorFont: 'default',
    lineHeight: 'normal',
    fontSize: input.fontSize,
    fontName: input.fontName.slice(0, 120),
    lineSpacing: 1,
    relativePath: input.parentPath ? `${input.parentPath}/${leaf}` : leaf,
    lineWrapping: input.lineWrap,
    syntax: 'text/plain',
    pinned: false,
    createdAt: now,
    updatedAt: now
  }
  writeBrowserList(NOTES_KEY, [note, ...notes])
  return note
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
