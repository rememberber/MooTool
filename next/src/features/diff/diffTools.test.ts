import { describe, expect, it } from 'vitest'
import { compareText } from './diffTools'

describe('text diff', () => {
  it('creates side-by-side segments and a unified patch', () => {
    const result = compareText('one\ntwo\n', 'one\nthree\nplus\n', false)
    expect(result.changed).toBe(1)
    expect(result.added).toBe(1)
    expect(result.unified).toContain('-two')
    expect(result.unified).toContain('+three')
  })

  it('can ignore whitespace-only differences', () => {
    const result = compareText('one  two\n', 'one two\n', true)
    expect(result.segments.every((item) => !item.added && !item.removed)).toBe(true)
  })
})
