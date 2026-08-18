import { describe, expect, it } from 'vitest'
import { alternateTargetLanguage, includesTranslationQuery, languageLabel } from './translationTools'

describe('translation tools', () => {
  it('provides stable language labels and alternate targets', () => {
    expect(languageLabel('jp', 'zh-CN')).toBe('日语')
    expect(languageLabel('unknown')).toBe('unknown')
    expect(alternateTargetLanguage('zh-CN')).toBe('en')
    expect(alternateTargetLanguage('en')).toBe('zh-CN')
  })

  it('searches source, target, language, and remarks', () => {
    const item = { sourceText: 'MooTool', targetText: '开发工具', sourceLang: 'en', targetLang: 'zh-CN', remark: 'desktop' }
    expect(includesTranslationQuery(item, '开发')).toBe(true)
    expect(includesTranslationQuery(item, 'DESKTOP')).toBe(true)
    expect(includesTranslationQuery(item, 'missing')).toBe(false)
  })
})
