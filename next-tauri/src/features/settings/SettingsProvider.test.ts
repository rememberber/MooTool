import { describe, expect, it } from 'vitest'
import { resolveTheme } from './appearance'

describe('settings appearance', () => {
  it('resolves explicit and system themes', () => {
    expect(resolveTheme('light', true)).toBe('light')
    expect(resolveTheme('dark', false)).toBe('dark')
    expect(resolveTheme('system', true)).toBe('dark')
    expect(resolveTheme('system', false)).toBe('light')
  })
})
