import { describe, expect, it } from 'vitest'
import {
  buildSearchRegExp,
  findAllMatches,
  findNextMatch,
  replaceAllMatches,
  replaceCurrentMatch
} from './findReplace'

describe('findReplace', () => {
  it('finds literal matches case-insensitively by default', () => {
    expect(findAllMatches('Foo foo FOO', 'foo', { matchCase: false, wholeWord: false, regex: false }))
      .toEqual([
        { start: 0, end: 3 },
        { start: 4, end: 7 },
        { start: 8, end: 11 }
      ])
  })

  it('respects match case', () => {
    expect(findAllMatches('Foo foo FOO', 'Foo', { matchCase: true, wholeWord: false, regex: false }))
      .toEqual([{ start: 0, end: 3 }])
  })

  it('supports whole word matching', () => {
    expect(findAllMatches('cat category cat', 'cat', { matchCase: false, wholeWord: true, regex: false }))
      .toEqual([
        { start: 0, end: 3 },
        { start: 13, end: 16 }
      ])
  })

  it('supports regex search', () => {
    expect(findAllMatches('a1 b22 c3', '\\d+', { matchCase: false, wholeWord: false, regex: true }))
      .toEqual([
        { start: 1, end: 2 },
        { start: 4, end: 6 },
        { start: 8, end: 9 }
      ])
  })

  it('returns null for invalid regex', () => {
    expect(buildSearchRegExp('(', { matchCase: false, wholeWord: false, regex: true })).toBeNull()
    expect(findAllMatches('abc', '(', { matchCase: false, wholeWord: false, regex: true })).toEqual([])
  })

  it('finds next and previous with wrap', () => {
    const content = 'one two one'
    const options = { matchCase: false, wholeWord: false, regex: false }
    expect(findNextMatch(content, 'one', options, 0, true)).toEqual({ start: 0, end: 3 })
    expect(findNextMatch(content, 'one', options, 1, true)).toEqual({ start: 8, end: 11 })
    expect(findNextMatch(content, 'one', options, 11, true)).toEqual({ start: 0, end: 3 })
    expect(findNextMatch(content, 'one', options, 8, false)).toEqual({ start: 0, end: 3 })
    expect(findNextMatch(content, 'one', options, 0, false)).toEqual({ start: 8, end: 11 })
  })

  it('replaces the current exact selection', () => {
    const result = replaceCurrentMatch(
      'hello world',
      'world',
      'moo',
      { matchCase: false, wholeWord: false, regex: false },
      { start: 6, end: 11 }
    )
    expect(result).toEqual({
      content: 'hello moo',
      nextFrom: 9,
      replaced: true,
      match: { start: 6, end: 9 }
    })
  })

  it('finds and replaces the next match when selection is not an exact match', () => {
    const result = replaceCurrentMatch(
      'foo bar foo',
      'foo',
      'x',
      { matchCase: false, wholeWord: false, regex: false },
      { start: 0, end: 0 }
    )
    expect(result.content).toBe('x bar foo')
    expect(result.replaced).toBe(true)
  })

  it('replaces all matches and expands regex captures', () => {
    const result = replaceAllMatches(
      'a1 b2',
      '(\\w)(\\d)',
      '$2$1',
      { matchCase: false, wholeWord: false, regex: true }
    )
    expect(result).toEqual({ content: '1a 2b', count: 2 })
  })

  it('finds newline matches written as \\n in regex mode', () => {
    expect(findAllMatches('a\nb\nc', '\\n', { matchCase: false, wholeWord: false, regex: true }))
      .toEqual([
        { start: 1, end: 2 },
        { start: 3, end: 4 }
      ])
  })

  it('inserts a newline when replacing with \\n in regex mode', () => {
    expect(replaceAllMatches(
      'a,b,c',
      ',',
      '\\n',
      { matchCase: false, wholeWord: false, regex: true }
    )).toEqual({ content: 'a\nb\nc', count: 2 })
  })

  it('keeps \\n literal when regex mode is off', () => {
    expect(replaceAllMatches(
      'a,b',
      ',',
      '\\n',
      { matchCase: false, wholeWord: false, regex: false }
    )).toEqual({ content: 'a\\nb', count: 1 })
  })

  it('expands \\n together with capture groups', () => {
    expect(replaceAllMatches(
      'a1',
      '(\\w)(\\d)',
      '$2\\n$1',
      { matchCase: false, wholeWord: false, regex: true }
    )).toEqual({ content: '1\na', count: 1 })
  })

  it('treats \\\\n as a literal backslash-n in regex replacements', () => {
    expect(replaceAllMatches(
      'a',
      'a',
      '\\\\n',
      { matchCase: false, wholeWord: false, regex: true }
    )).toEqual({ content: '\\n', count: 1 })
  })
})
