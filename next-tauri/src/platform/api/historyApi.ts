import { invoke } from '@tauri-apps/api/core'
import { listen } from '@tauri-apps/api/event'
import type { HistoryApi, OperationHistory } from '../contracts/history'

const HISTORY_KEY = 'mootool-next-tauri:operation-history:v1'

export const historyApi: HistoryApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? {
      record: (entry, historyLimit) => invoke<OperationHistory>('record_operation_history', { entry, historyLimit }),
      list: (limit) => invoke<OperationHistory[]>('list_operation_history', { limit }),
      delete: (id) => invoke<boolean>('delete_operation_history', { id }),
      clear: () => invoke<number>('clear_operation_history'),
      subscribe: async (listener) => listen('mootool://operation-history-changed', listener)
    }
  : createBrowserHistoryApi()

function createBrowserHistoryApi(): HistoryApi {
  const notify = () => window.dispatchEvent(new Event('mootool-preview-history'))
  return {
    record: async (entry, historyLimit) => {
      write([entry, ...read().filter((item) => item.id !== entry.id)].slice(0, historyLimit))
      notify()
      return entry
    },
    list: async (limit) => read().slice(0, limit),
    delete: async (id) => {
      const current = read()
      const next = current.filter((item) => item.id !== id)
      write(next)
      notify()
      return current.length !== next.length
    },
    clear: async () => {
      const count = read().length
      write([])
      notify()
      return count
    },
    subscribe: async (listener) => {
      window.addEventListener('mootool-preview-history', listener)
      return () => window.removeEventListener('mootool-preview-history', listener)
    }
  }
}

function read(): OperationHistory[] {
  try {
    const raw = window.localStorage.getItem(HISTORY_KEY)
    const values = raw ? JSON.parse(raw) as OperationHistory[] : []
    return values.sort((left, right) => right.createdAt - left.createdAt)
  } catch {
    return []
  }
}

function write(values: OperationHistory[]) {
  window.localStorage.setItem(HISTORY_KEY, JSON.stringify(values))
}
