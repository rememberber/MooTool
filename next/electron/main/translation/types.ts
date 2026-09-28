import type { Dispatcher } from 'undici'
import type { TranslationInput, TranslationProvider } from '../../../src/shared/contracts/network'

/** Credentials stay in the main process and are never part of translation IPC input. */
export type TranslationCredentials = { deeplApiKey?: string }
export type TranslationContext = {
  signal: AbortSignal
  dispatcher?: Dispatcher
  credentials: TranslationCredentials
}
export interface TranslationEngine {
  readonly id: TranslationProvider
  translate(input: TranslationInput, context: TranslationContext): Promise<string>
}
