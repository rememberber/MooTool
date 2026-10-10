import { describe, expect, it, vi } from 'vitest'
import { actionRegistry, executeAction, searchActions } from './actionRegistry'
import { messages, type MessageKey } from '@/shared/i18n/messages'

const t = (key: MessageKey, params?: Record<string, string>) => Object.entries(params ?? {})
  .reduce<string>((text, [name, value]) => text.replaceAll(`{${name}}`, value), messages['zh-CN'][key])
const context = { t, zone: 'Asia/Shanghai' }

describe('command actions', () => {
  it('matches aliases before interpreting arguments, preserving text whitespace', async () => {
    const [match] = searchActions('  JSON minify {"text":"a b"}', t)
    expect(match.action.id).toBe('json-minify')
    expect(await executeAction(match, vi.fn(), context)).toBe('{"text":"a b"}')
    const [encode] = searchActions('base64 encode  hello ', t)
    expect(encode.argument).toBe(' hello ')
    expect(await executeAction(encode, vi.fn(), context)).toBe('IGhlbGxvIA==')
  })

  it('searches translated labels and keywords without evaluating clipboard content', () => {
    expect(searchActions('去重', t).map(({ action }) => action.id)).toContain('deduplicate-lines')
    expect(searchActions('base64', t)).toHaveLength(2)
    expect(searchActions('json', t).map(({ action }) => action.id)).toEqual(['json-format', 'json-minify'])
    expect(searchActions('uuid', t)[0].argument).toBeUndefined()
    expect(new Set(actionRegistry.map((action) => action.id)).size).toBe(actionRegistry.length)
  })

  it('only reads clipboard for text actions without an inline argument', async () => {
    const readClipboard = vi.fn(async () => '{"a":1}')
    const uuid = await executeAction(searchActions('uuid', t)[0], readClipboard, context)
    expect(uuid).toMatch(/^[\da-f]{8}-[\da-f]{4}-4[\da-f]{3}-[89ab][\da-f]{3}-[\da-f]{12}$/)
    expect(readClipboard).not.toHaveBeenCalled()
    expect(await executeAction(searchActions('时间戳 0', t)[0], readClipboard, context)).toBe('1970-01-01 08:00:00 (Asia/Shanghai)')
    expect(readClipboard).not.toHaveBeenCalled()
    expect(await executeAction(searchActions('json format', t)[0], readClipboard, context)).toBe('{\n  "a": 1\n}')
    expect(readClipboard).toHaveBeenCalledOnce()
  })

  it('rejects empty/oversized text and invalid data without producing a misleading result', async () => {
    await expect(executeAction(searchActions('时间戳 abc', t)[0], vi.fn(), context)).rejects.toThrow(t('time.error.timestamp'))
    await expect(executeAction(searchActions('json', t)[0], async () => ' ', context)).rejects.toThrow(t('app.command.emptyInput'))
    await expect(executeAction(searchActions('json', t)[0], async () => 'a'.repeat(100_001), context)).rejects.toThrow(t('app.command.tooLarge'))
    await expect(executeAction(searchActions('json {"a":1,"a":2}', t)[0], vi.fn(), context)).rejects.toThrow()
    await expect(executeAction(searchActions('base64 decode !!!', t)[0], vi.fn(), context)).rejects.toThrow(t('app.command.invalidBase64'))
    await expect(executeAction(searchActions('base64 decode /w==', t)[0], vi.fn(), context)).rejects.toThrow(t('app.command.invalidBase64'))
    await expect(executeAction(searchActions('url decode %xy', t)[0], vi.fn(), context)).rejects.toThrow(t('app.command.invalidUrl'))
  })

  it('handles UTF-8 Base64, form URL escapes, milliseconds and line deduplication', async () => {
    expect(await executeAction(searchActions('base64 decode 5L2g5aW9\n', t)[0], vi.fn(), context)).toBe('你好')
    expect(await executeAction(searchActions('url decode %E4%BD%A0%E5%A5%BD+world', t)[0], vi.fn(), context)).toBe('你好 world')
    expect(await executeAction(searchActions('时间戳 1728000000000', t)[0], vi.fn(), context)).toBe('2024-10-04 08:00:00 (Asia/Shanghai)')
    expect(await executeAction(searchActions('按行去重 a\r\na\r\nb', t)[0], vi.fn(), context)).toBe('a\nb')
  })
})
