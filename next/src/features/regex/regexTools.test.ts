import { describe, expect, it } from 'vitest'
import { commonRegexes, matchRegex } from './regexTools'

describe('regex tools', () => {
  it('returns all match positions and capture groups', () => {
    expect(matchRegex('(moo)(\\d+)', 'moo1 moo22', { global: true, ignoreCase: false, multiline: false, dotAll: false })).toEqual([
      { index: 0, value: 'moo1', groups: ['moo', '1'] },
      { index: 5, value: 'moo22', groups: ['moo', '22'] }
    ])
  })

  it('handles zero-width global expressions without looping', () => {
    expect(matchRegex('(?=a)', 'aa', { global: true, ignoreCase: false, multiline: false, dotAll: false })).toHaveLength(2)
  })

  it('keeps the complete Java common-pattern catalog', () => {
    expect(commonRegexes).toHaveLength(21)
  })
})
