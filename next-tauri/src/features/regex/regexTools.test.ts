import { describe, expect, it } from 'vitest'
import { commonRegexes, matchRegex, regexFlags, replaceRegex, type RegexOptions } from './regexTools'

const options: RegexOptions = {
  global: true,
  ignoreCase: false,
  multiline: false,
  dotAll: false,
  unicode: true
}

describe('regex tools', () => {
  it('returns offsets, numbered groups and named groups', () => {
    expect(matchRegex('(?<name>moo)(\\d+)', 'moo1 moo22', options)).toEqual([
      {
        index: 0,
        end: 4,
        value: 'moo1',
        groups: ['moo', '1'],
        namedGroups: { name: 'moo' }
      },
      {
        index: 5,
        end: 10,
        value: 'moo22',
        groups: ['moo', '22'],
        namedGroups: { name: 'moo' }
      }
    ])
  })

  it('advances safely after zero-width Unicode matches', () => {
    expect(matchRegex('(?=🐮)', '🐮🐮', options).map((match) => match.index)).toEqual([0, 2])
  })

  it('builds deterministic flags and supports replacements', () => {
    expect(regexFlags({ ...options, ignoreCase: true, multiline: true, dotAll: true })).toBe('gimsu')
    expect(replaceRegex('(moo)(\\d+)', 'moo1 moo22', '$1-$2', options)).toBe('moo-1 moo-22')
  })

  it('ships the frozen common-pattern set', () => {
    expect(commonRegexes).toHaveLength(21)
    expect(new Set(commonRegexes.map((item) => item.id)).size).toBe(21)
  })
})
