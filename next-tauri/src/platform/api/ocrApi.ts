import { invoke } from '@tauri-apps/api/core'
import type { OcrLanguage, OcrResult, OcrStatus } from '../contracts/ocr'

export interface OcrApi {
  status(): Promise<OcrStatus>
  recognize(request: { requestId: string; language: OcrLanguage; source: { name: string; dataUrl: string } }): Promise<OcrResult>
  cancel(requestId: string): Promise<boolean>
}

const unavailable: OcrStatus = { available: false, engine: null, languages: [], reason: 'platform' }

const desktopApi: OcrApi = {
  status: () => invoke<OcrStatus>('image_text_recognition_status'),
  recognize: (request) => invoke<OcrResult>('recognize_image_text', { request }),
  cancel: (requestId) => invoke<boolean>('cancel_image_text_recognition', { requestId })
}

const browserApi: OcrApi = {
  status: async () => unavailable,
  recognize: async () => { throw new Error('image.ocr.error.unavailable') },
  cancel: async () => false
}

export const ocrApi: OcrApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__ ? desktopApi : browserApi
