import { describe, expect, it } from 'vitest'
import { createLocalizedTranslator, defineMessages, validateMessageCatalog } from './localizedMessages'

const valid = defineMessages({
  'zh-CN': { greeting: '你好，{name}', saved: '已保存' },
  'en-US': { greeting: 'Hello, {name}', saved: 'Saved' },
  'ja-JP': { greeting: 'こんにちは、{name}', saved: '保存しました' }
})

describe('localized tool messages', () => {
  it('formats the selected locale and preserves unknown placeholders', () => {
    const translator = createLocalizedTranslator(valid, 'en-US')
    expect(translator.t('greeting', { name: 'MooTool' })).toBe('Hello, MooTool')
    expect(translator.t('greeting')).toBe('Hello, {name}')
  })

  it('detects missing keys and placeholder drift', () => {
    expect(validateMessageCatalog(valid)).toEqual([])
    expect(validateMessageCatalog({
      'zh-CN': { greeting: '你好，{name}' },
      'en-US': { greeting: 'Hello, {user}' },
      'ja-JP': {}
    })).toEqual([
      'en-US.greeting placeholders differ: expected name, received user',
      'ja-JP is missing: greeting',
      'ja-JP.greeting is empty'
    ])
  })
})
