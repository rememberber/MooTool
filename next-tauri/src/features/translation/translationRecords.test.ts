import { describe, expect, it } from 'vitest'
import { listStoredTranslationHistory, listStoredTranslationWords, saveStoredTranslationWord } from './translationRecords'

describe('translation records', () => {
  it('updates the same source and language pair and searches with SQL LIKE', () => {
    const created = saveStoredTranslationWord([], { sourceText: ' hello ', targetText: '你好', sourceLang: 'en', targetLang: 'zh-CN', remark: 'greeting' }, new Date('2026-10-21T04:05:06'))
    const updated = saveStoredTranslationWord([created], { sourceText: 'hello', targetText: '您好', sourceLang: 'en', targetLang: 'zh-CN', remark: 'greeting' }, new Date('2026-10-21T05:00:00'))
    expect(updated.id).toBe(1)
    expect(updated.createTime).toBe(created.createTime)
    const other = saveStoredTranslationWord([updated], { sourceText: 'hello', targetText: 'こんにちは', sourceLang: 'en', targetLang: 'ja', remark: '' }, new Date('2026-10-21T06:00:00'))
    expect(listStoredTranslationWords([updated, other], '您好').map((word) => word.id)).toEqual([1])
    expect(listStoredTranslationWords([updated, other], 'h_llo').map((word) => word.id)).toEqual([2, 1])
    expect(listStoredTranslationWords([updated, other], 'z_z')).toEqual([])
    expect(listStoredTranslationHistory([
      { id: 2, sourceText: 'beta', targetText: '乙', sourceLang: 'en', targetLang: 'zh-CN', translatorType: 'bing', createTime: '2026-10-21 06:00:00' },
      { id: 1, sourceText: 'alpha', targetText: '甲', sourceLang: 'en', targetLang: 'ja', translatorType: 'google', createTime: '2026-10-21 05:00:00' }
    ], 'ja').map((item) => item.id)).toEqual([1])
  })
})