import type { TranslationProvider } from '../../../src/shared/contracts/network'
import type { TranslationEngine } from './types'
import { GoogleTranslator } from './google'
import { BingTranslator } from './bing'
import { DeepLTranslator } from './deepl'

export function createTranslationEngines(): Record<TranslationProvider, TranslationEngine> {
  return { google: new GoogleTranslator(), bing: new BingTranslator(), deepl: new DeepLTranslator() }
}
