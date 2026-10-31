export const ocrLanguages = ['auto', 'zh-Hans', 'zh-Hant', 'en-US', 'ja-JP'] as const
export type OcrLanguage = (typeof ocrLanguages)[number]
export type OcrEngine = 'apple-vision'

export interface OcrStatus {
  available: boolean
  engine: OcrEngine | null
  languages: OcrLanguage[]
  reason: 'platform' | 'component' | 'system' | null
}

export interface OcrSource {
  name: string
  dataUrl: string
  width: number
  height: number
}

export interface OcrLine {
  text: string
  confidence: number
  box: { x: number; y: number; width: number; height: number }
}

export interface OcrResult {
  requestId: string
  sourceName: string
  engine: OcrEngine
  language: OcrLanguage
  width: number
  height: number
  text: string
  lines: OcrLine[]
  durationMs: number
}

export const ocrErrorCodes = ['input', 'tooLarge', 'tooComplex', 'unavailable', 'language', 'busy', 'cancelled', 'timeout', 'failed'] as const
