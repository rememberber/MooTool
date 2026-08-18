export type TranslationProvider = 'google' | 'bing'

export interface TranslationRequest {
  requestId: string
  text: string
  sourceLang: string
  targetLang: string
  preferredProvider: TranslationProvider
  timeoutMs: number
}

export interface TranslationResult {
  requestId: string
  text: string
  provider: TranslationProvider
  fallbackUsed: boolean
}

export interface TranslationApi {
  translate(request: TranslationRequest): Promise<TranslationResult>
  cancel(requestId: string): Promise<boolean>
}
