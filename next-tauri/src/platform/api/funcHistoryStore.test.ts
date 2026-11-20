import { describe, expect, it } from 'vitest'
import { createFuncHistoryStore } from './funcHistoryStore'

describe('function history store', () => {
  it('keeps the newest 200 calculator records and searches the summary', () => {
    const storage = new Map<string, string>()
    const history = createFuncHistoryStore({
      getItem: (key) => storage.get(key) ?? null,
      setItem: (key, value) => { storage.set(key, value) }
    })
    expect(history.save({ funcType: 'calculator', inputText: '   ', outputText: ' ' })).toBeNull()
    for (let index = 1; index <= 201; index += 1) {
      history.save({
        funcType: 'calculator',
        summary: `表达式 ${index}`,
        inputText: `${index}*2`,
        outputText: String(index * 2)
      }, new Date(2026, 0, 1, 0, 0, index))
    }
    const listed = history.list('calculator')
    expect(listed).toHaveLength(200)
    expect(listed[0]).toMatchObject({ summary: '表达式 201', outputText: '402', extraData: null })
    expect(listed[0].createTime).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/)
    expect(history.list('calculator', '表达式 200')).toHaveLength(1)
    expect(history.clear('calculator')).toBe(200)
    expect(history.list('calculator')).toEqual([])
  })
})