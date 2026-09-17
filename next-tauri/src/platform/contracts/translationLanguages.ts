import type { AppLanguage } from './settings'

export const translationLanguageCodes = [
  'auto', 'zh-CN', 'en', 'yue', 'wyw', 'jp', 'kor', 'fra', 'spa', 'th', 'ara', 'ru', 'pt', 'de', 'it',
  'el', 'nl', 'pl', 'bul', 'est', 'dan', 'fin', 'cs', 'rom', 'slo', 'swe', 'hu', 'cht', 'vie'
] as const

export type TranslationLanguageCode = (typeof translationLanguageCodes)[number]

const labels: Record<AppLanguage, Record<TranslationLanguageCode, string>> = {
  'zh-CN': {
    auto: '自动检测', 'zh-CN': '中文（简体）', en: '英语', yue: '粤语', wyw: '文言文', jp: '日语', kor: '韩语',
    fra: '法语', spa: '西班牙语', th: '泰语', ara: '阿拉伯语', ru: '俄语', pt: '葡萄牙语', de: '德语', it: '意大利语',
    el: '希腊语', nl: '荷兰语', pl: '波兰语', bul: '保加利亚语', est: '爱沙尼亚语', dan: '丹麦语', fin: '芬兰语',
    cs: '捷克语', rom: '罗马尼亚语', slo: '斯洛文尼亚语', swe: '瑞典语', hu: '匈牙利语', cht: '中文（繁体）', vie: '越南语'
  },
  'en-US': {
    auto: 'Auto detect', 'zh-CN': 'Chinese (Simplified)', en: 'English', yue: 'Cantonese', wyw: 'Classical Chinese',
    jp: 'Japanese', kor: 'Korean', fra: 'French', spa: 'Spanish', th: 'Thai', ara: 'Arabic', ru: 'Russian',
    pt: 'Portuguese', de: 'German', it: 'Italian', el: 'Greek', nl: 'Dutch', pl: 'Polish', bul: 'Bulgarian',
    est: 'Estonian', dan: 'Danish', fin: 'Finnish', cs: 'Czech', rom: 'Romanian', slo: 'Slovenian', swe: 'Swedish',
    hu: 'Hungarian', cht: 'Chinese (Traditional)', vie: 'Vietnamese'
  },
  'ja-JP': {
    auto: '自動検出', 'zh-CN': '中国語（簡体字）', en: '英語', yue: '広東語', wyw: '漢文', jp: '日本語', kor: '韓国語',
    fra: 'フランス語', spa: 'スペイン語', th: 'タイ語', ara: 'アラビア語', ru: 'ロシア語', pt: 'ポルトガル語', de: 'ドイツ語',
    it: 'イタリア語', el: 'ギリシャ語', nl: 'オランダ語', pl: 'ポーランド語', bul: 'ブルガリア語', est: 'エストニア語',
    dan: 'デンマーク語', fin: 'フィンランド語', cs: 'チェコ語', rom: 'ルーマニア語', slo: 'スロベニア語', swe: 'スウェーデン語',
    hu: 'ハンガリー語', cht: '中国語（繁体字）', vie: 'ベトナム語'
  }
}

export function translationLanguageLabel(code: string, language: AppLanguage): string {
  if (translationLanguageCodes.includes(code as TranslationLanguageCode)) {
    return labels[language][code as TranslationLanguageCode]
  }
  return code
}
