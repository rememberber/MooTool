import { afterEach, describe, expect, it } from 'vitest'
import { JavaRegexService } from '../../electron/main/javaRegexService'
const service = new JavaRegexService()
const options = { global: true, ignoreCase: false, multiline: false, dotAll: false }
afterEach(() => service.dispose())
describe('Java regex engine', () => {
  it('uses Java quoting, intersections, possessive quantifiers and inline flags', async () => {
    expect((await service.match(1, { pattern: '\\Qfoo.bar\\E', source: 'foo.bar fooXbar', options })).matches).toEqual([{ index: 0, value: 'foo.bar', groups: [] }])
    expect((await service.match(1, { pattern: '[a-z&&[^aeiou]]+', source: 'abcdef', options })).matches.map(m => m.value)).toEqual(['bcd', 'f'])
    expect((await service.match(1, { pattern: 'a++a', source: 'aaa', options })).matches).toEqual([])
    expect((await service.match(1, { pattern: '(?i)moo', source: 'MOO', options })).matches[0].value).toBe('MOO')
  }, 20000)
  it('retains UTF-16 offsets, empty captures and zero-width matches', async () => {
    const result = await service.match(1, { pattern: '(moo)(x)?', source: '😀moo', options })
    expect(result.matches).toEqual([{ index: 2, value: 'moo', groups: ['moo', ''] }])
    expect((await service.match(1, { pattern: '(?=a)', source: 'aa', options })).matches.map(m => m.index)).toEqual([0, 1])
  }, 15000)
  it('reports syntax errors and limits large result sets', async () => {
    await expect(service.match(1, { pattern: '[', source: 'text', options })).rejects.toThrow('Unclosed')
    const result = await service.match(1, { pattern: 'a', source: 'a'.repeat(6000), options })
    expect(result.matches).toHaveLength(5000)
    expect(result.limited).toBe(true)
  }, 15000)
  it('cancels superseded work and validates IPC input', async () => {
    const first = service.match(1, { pattern: 'a', source: 'a', options })
    service.cancel(1)
    await expect(first).rejects.toThrow()
    await expect(service.match(1, { pattern: 'a', source: 'a' })).rejects.toThrow('Invalid regex')
  })
})

it('terminates expensive expressions without blocking the caller', async () => {
  await expect(service.match(2, { pattern: '(a+)+$', source: 'a'.repeat(100000) + '!', options })).rejects.toThrow('REGEX_TIMEOUT')
}, 15000)
