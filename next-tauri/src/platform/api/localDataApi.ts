import { invoke } from '@tauri-apps/api/core'
import type {
  BoardMessage,
  HostProfile,
  LocalDataApi,
  QuickNote,
  TranslationHistory,
  TranslationWord
} from '../contracts/localData'

type Invoke = <T>(command: string, args?: Record<string, unknown>) => Promise<T>

const NOTES_KEY = 'mootool-next-tauri:quick-notes:v1'
const MESSAGES_KEY = 'mootool-next-tauri:board-messages:v1'
const HOST_PROFILES_KEY = 'mootool-next-tauri:host-profiles:v1'
const TRANSLATION_WORDS_KEY = 'mootool-next-tauri:translation-words:v1'
const TRANSLATION_HISTORY_KEY = 'mootool-next-tauri:translation-history:v1'

export function createLocalDataApi(invokeCommand: Invoke = invoke): LocalDataApi {
  return {
    listNotes: () => invokeCommand<QuickNote[]>('list_quick_notes'),
    saveNote: (note) => invokeCommand<QuickNote>('save_quick_note', { note }),
    deleteNote: (id) => invokeCommand<boolean>('delete_quick_note', { id }),
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
    listNotes: async () => readBrowserList<QuickNote>(NOTES_KEY),
    saveNote: async (note) => {
      writeBrowserList(NOTES_KEY, upsert(readBrowserList<QuickNote>(NOTES_KEY), note))
      return note
    },
    deleteNote: async (id) => deleteBrowserItem<QuickNote>(NOTES_KEY, id),
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

function readHistoryBrowserList(): TranslationHistory[] {
  try {
    const raw = window.localStorage.getItem(TRANSLATION_HISTORY_KEY)
    const values = raw ? JSON.parse(raw) as TranslationHistory[] : []
    return values.sort((left, right) => right.createdAt - left.createdAt).slice(0, 500)
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
