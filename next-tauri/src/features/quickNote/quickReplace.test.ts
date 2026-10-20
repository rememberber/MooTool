import { describe, expect, it } from 'vitest'
import { runQuickReplace } from './quickReplace'

describe('Quick Note quick replace', () => {
  it('normalizes line-oriented values', () => {
    expect(runQuickReplace(' a \n\n b ', 'removeBlankLines')).toBe(' a \n b ')
    expect(runQuickReplace('a\na\nb', 'deduplicateWithCount')).toBe('a\t2\nb\t1')
    expect(runQuickReplace('b\na', 'sortAscending')).toBe('a\nb')
  })

  it('converts identifiers and number formats', () => {
    expect(runQuickReplace('hello_world', 'underscoreToCamel')).toBe('helloWorld')
    expect(runQuickReplace('helloWorld', 'camelToUnderscore')).toBe('hello_world')
    expect(runQuickReplace('1.25e3', 'scientificToNormal')).toBe('1250')
    expect(runQuickReplace('1234567.5', 'normalToThousands')).toBe('1,234,567.5')
  })

  it('converts list delimiters and escapes text', () => {
    expect(runQuickReplace('a\nb', 'linesToSingleQuoted')).toBe("'a','b'")
    expect(runQuickReplace('"a", b', 'commaToLines')).toBe('a\nb')
    const escaped = runQuickReplace('a\nb', 'escape')
    expect(runQuickReplace(escaped, 'unescape')).toBe('a\nb')
  })
})
