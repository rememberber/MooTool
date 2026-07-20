export const httpMethods = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS'] as const
export type HttpMethod = (typeof httpMethods)[number]

export type KeyValueEntry = {
  id: string
  name: string
  value: string
  enabled: boolean
}

export type HttpCookieEntry = KeyValueEntry & {
  domain: string
  path: string
  expires: string
}

export type HttpRequestDraft = {
  id?: number
  name: string
  method: HttpMethod
  url: string
  params: KeyValueEntry[]
  headers: KeyValueEntry[]
  cookies: HttpCookieEntry[]
  body: string
  bodyType: string
}

export type SavedHttpRequest = HttpRequestDraft & {
  id: number
  responseBody: string
  responseHeaders: string
  responseCookies: string
  createTime: string
  modifiedTime: string
}

export type HttpSendInput = {
  requestId: string
  request: HttpRequestDraft
  timeoutMs: number
}

export type HttpResponseResult = {
  requestId: string
  ok: boolean
  status: number
  statusText: string
  url: string
  durationMs: number
  body: string
  headers: string
  cookies: string
  errorCode?: 'ABORTED' | 'TIMEOUT' | 'NETWORK' | 'INVALID_REQUEST' | 'RESPONSE_TOO_LARGE'
}

export type HttpRequestHistory = SavedHttpRequest & {
  requestIdValue: number | null
  title: string
  status: string
  costTime: number
}

export type TranslationProvider = 'google' | 'bing'

export const translationLanguageCodes = [
  'auto', 'zh-CN', 'en', 'yue', 'wyw', 'jp', 'kor', 'fra', 'spa', 'th', 'ara', 'ru', 'pt', 'de', 'it',
  'el', 'nl', 'pl', 'bul', 'est', 'dan', 'fin', 'cs', 'rom', 'slo', 'swe', 'hu', 'cht', 'vie'
] as const
export type TranslationLanguageCode = (typeof translationLanguageCodes)[number]

const translationLanguageAliases: ReadonlyArray<readonly [TranslationLanguageCode, ...string[]]> = [
  ['auto', '自动检测', 'Detect language', '自動検出', 'auto-detect'],
  ['zh-CN', '中文（简体）', 'Chinese (Simplified)', '中国語（簡体字）', 'zh', 'zh-Hans'],
  ['en', '英语', 'English', '英語'],
  ['yue', '粤语', 'Cantonese', '広東語'],
  ['wyw', '文言文', 'Classical Chinese', '漢文', 'lzh'],
  ['jp', '日语', 'Japanese', '日本語', 'ja'],
  ['kor', '韩语', 'Korean', '韓国語', 'ko'],
  ['fra', '法语', 'French', 'フランス語', 'fr'],
  ['spa', '西班牙语', 'Spanish', 'スペイン語', 'es'],
  ['th', '泰语', 'Thai', 'タイ語'],
  ['ara', '阿拉伯语', 'Arabic', 'アラビア語', 'ar'],
  ['ru', '俄语', 'Russian', 'ロシア語'],
  ['pt', '葡萄牙语', 'Portuguese', 'ポルトガル語'],
  ['de', '德语', 'German', 'ドイツ語'],
  ['it', '意大利语', 'Italian', 'イタリア語'],
  ['el', '希腊语', 'Greek', 'ギリシャ語'],
  ['nl', '荷兰语', 'Dutch', 'オランダ語'],
  ['pl', '波兰语', 'Polish', 'ポーランド語'],
  ['bul', '保加利亚语', 'Bulgarian', 'ブルガリア語', 'bg'],
  ['est', '爱沙尼亚语', 'Estonian', 'エストニア語', 'et'],
  ['dan', '丹麦语', 'Danish', 'デンマーク語', 'da'],
  ['fin', '芬兰语', 'Finnish', 'フィンランド語', 'fi'],
  ['cs', '捷克语', 'Czech', 'チェコ語'],
  ['rom', '罗马尼亚语', 'Romanian', 'ルーマニア語', 'ro'],
  ['slo', '斯洛文尼亚语', 'Slovenian', 'スロベニア語', 'sl'],
  ['swe', '瑞典语', 'Swedish', 'スウェーデン語', 'sv'],
  ['hu', '匈牙利语', 'Hungarian', 'ハンガリー語'],
  ['cht', '繁体中文', 'Chinese (Traditional)', '中国語（繁体字）', 'zh-TW', 'zh-Hant'],
  ['vie', '越南语', 'Vietnamese', 'ベトナム語', 'vi']
]

const translationLanguageAliasMap = new Map<string, TranslationLanguageCode>(
  translationLanguageAliases.flatMap(([code, ...aliases]) => [code, ...aliases].map((alias) => [alias.toLocaleLowerCase(), code]))
)

export function normalizeTranslationLanguageCode(
  value: unknown,
  fallback: TranslationLanguageCode,
  allowAuto = true
): TranslationLanguageCode {
  if (typeof value !== 'string') return fallback
  const code = translationLanguageAliasMap.get(value.trim().toLocaleLowerCase())
  return code && (allowAuto || code !== 'auto') ? code : fallback
}

export function normalizeTranslationLanguagePair(source: unknown, target: unknown): {
  sourceLang: TranslationLanguageCode
  targetLang: TranslationLanguageCode
} {
  const targetLang = normalizeTranslationLanguageCode(target, 'zh-CN', false)
  const sourceLang = normalizeTranslationLanguageCode(source, 'auto')
  return { sourceLang: sourceLang === targetLang ? 'auto' : sourceLang, targetLang }
}

export type TranslationInput = {
  requestId: string
  text: string
  sourceLang: string
  targetLang: string
  preferredProvider: TranslationProvider
  timeoutMs: number
}

export type TranslationResult = {
  requestId: string
  text: string
  provider: TranslationProvider
  fallbackUsed: boolean
}

export type TranslationWord = {
  id: number
  sourceText: string
  targetText: string
  sourceLang: string
  targetLang: string
  remark: string
  createTime: string
  modifiedTime: string
}

export type SaveTranslationWordInput = Omit<TranslationWord, 'id' | 'createTime' | 'modifiedTime'> & { id?: number }

export type TranslationHistory = {
  id: number
  sourceText: string
  targetText: string
  sourceLang: string
  targetLang: string
  translatorType: TranslationProvider
  createTime: string
}

export type SaveTranslationHistoryInput = Omit<TranslationHistory, 'id' | 'createTime'>
