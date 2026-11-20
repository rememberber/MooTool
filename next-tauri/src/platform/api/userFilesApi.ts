import { invoke } from '@tauri-apps/api/core'
import type { UserFilesApi, UserTextFile } from '../contracts/userFiles'

const browserApi: UserFilesApi = {
  pickText: (kind) => new Promise((resolve, reject) => {
    const input = document.createElement('input')
    input.type = 'file'
    const source = kind === 'source'
    input.accept = source
      ? '.java,.xml,.html,.htm,.conf,.nginx,.txt,.text,.log'
      : 'text/*,.txt,.hosts,.conf,.md,.json,.yaml,.yml,.svg'
    input.onchange = () => {
      const file = input.files?.[0]
      if (!file) { resolve(null); return }
      if (source && file.size > 20 * 1024 * 1024) { reject(new Error('File exceeds 20 MB limit')); return }
      if (!source && file.size > 5 * 1024 * 1024) { reject(new Error('文本文件不能超过 5 MiB')); return }
      void file.text()
        .then((content) => resolve({ name: file.name, path: file.name, content }))
        .catch(reject)
    }
    input.click()
  }),
  exportText: async (defaultName, content) => {
    const anchor = document.createElement('a')
    anchor.href = URL.createObjectURL(new Blob([content], { type: 'text/plain;charset=utf-8' }))
    anchor.download = defaultName
    anchor.click()
    window.setTimeout(() => URL.revokeObjectURL(anchor.href), 1_000)
    return defaultName
  },
  exportDataUrl: async (defaultName, dataUrl) => {
    const anchor = document.createElement('a')
    anchor.href = dataUrl
    anchor.download = defaultName
    anchor.click()
    return defaultName
  }
}

export const userFilesApi: UserFilesApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? {
      pickText: (kind) => invoke<UserTextFile | null>('pick_text_file', kind ? { kind } : {}),
      exportText: (defaultName, content) => invoke<string | null>('export_text_file', { defaultName, content }),
      exportDataUrl: (defaultName, dataUrl) => invoke<string | null>('export_binary_data_url', { defaultName, dataUrl })
    }
  : browserApi
