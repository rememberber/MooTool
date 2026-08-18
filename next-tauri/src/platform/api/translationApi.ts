import { invoke } from '@tauri-apps/api/core'
import type { TranslationApi, TranslationResult } from '../contracts/translation'

export const translationApi: TranslationApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? {
      translate: (request) => invoke<TranslationResult>('translate_text', { request }),
      cancel: (requestId) => invoke<boolean>('cancel_translation', { requestId })
    }
  : {
      translate: async () => {
        throw new Error('翻译网络服务需要在 Tauri 桌面应用中运行')
      },
      cancel: async () => false
    }
