import { writeText as writeNativeText } from '@tauri-apps/plugin-clipboard-manager'

export interface ClipboardApi {
  writeText(value: string): Promise<void>
}

const browserApi: ClipboardApi = {
  writeText: async (value) => {
    if (!navigator.clipboard?.writeText) throw new Error('当前浏览器未开放剪贴板写入')
    await navigator.clipboard.writeText(value)
  }
}

export const clipboardApi: ClipboardApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? { writeText: writeNativeText }
  : browserApi
