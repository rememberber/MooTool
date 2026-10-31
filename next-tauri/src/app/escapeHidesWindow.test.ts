import { describe, expect, it } from 'vitest'
import { shouldDismissWindowOnEscape } from './escapeHidesWindow'

const escape = {
  key: 'Escape',
  repeat: false,
  metaKey: false,
  ctrlKey: false,
  altKey: false,
  shiftKey: false,
  defaultPrevented: false
}

describe('escape hides window', () => {
  it('hides only on a plain Escape after other controls decline it', () => {
    expect(shouldDismissWindowOnEscape(escape, false)).toBe(true)
    expect(shouldDismissWindowOnEscape({ ...escape, defaultPrevented: true }, false)).toBe(false)
    expect(shouldDismissWindowOnEscape(escape, true)).toBe(false)
    expect(shouldDismissWindowOnEscape({ ...escape, shiftKey: true }, false)).toBe(false)
    expect(shouldDismissWindowOnEscape({ ...escape, key: 'Enter' }, false)).toBe(false)
  })
})
