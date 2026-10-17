import type { ComponentState } from './optionalComponent'

export const ocrLanguages = ['auto', 'zh-Hans', 'zh-Hant', 'en-US', 'ja-JP'] as const
export type OcrLanguage = (typeof ocrLanguages)[number]
export type OcrEngine = 'apple-vision' | 'pp-ocrv6-small'
export type OcrStatus = {
  component?: ComponentState
  available: boolean
  engine: OcrEngine | null
  languages: OcrLanguage[]
  reason: 'platform' | 'component' | 'system' | null
}
export type OcrSource = { name: string; dataUrl: string; width: number; height: number }
export type OcrRequest = { requestId: string; language: OcrLanguage; source: Pick<OcrSource, 'name' | 'dataUrl'> }
export type OcrLine = { text: string; confidence: number; box: { x: number; y: number; width: number; height: number } }
export type OcrResult = {
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
export type OcrErrorCode = (typeof ocrErrorCodes)[number]
