import { invoke } from '@tauri-apps/api/core'
import { listen } from '@tauri-apps/api/event'
import type { GlobalShortcutStatus } from '../contracts/shortcuts'

export const GLOBAL_SHORTCUT_STATUS_EVENT = 'mootool://global-shortcut-status'

export interface GlobalShortcutApi {
  getStatus(): Promise<GlobalShortcutStatus>
  retry(): Promise<GlobalShortcutStatus>
  subscribe(listener: (status: GlobalShortcutStatus) => void): Promise<() => void>
}

export function createGlobalShortcutApi(): GlobalShortcutApi {
  return {
    getStatus: () => invoke<GlobalShortcutStatus>('get_global_shortcut_status'),
    retry: () => invoke<GlobalShortcutStatus>('retry_global_shortcut'),
    subscribe: (listener) => listen<GlobalShortcutStatus>(
      GLOBAL_SHORTCUT_STATUS_EVENT,
      (event) => listener(event.payload)
    )
  }
}

export const globalShortcutApi: GlobalShortcutApi | null = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? createGlobalShortcutApi()
  : null
