import { describe, expect, it } from 'vitest'
import { accentPresets, resolveTheme } from './appearance'

describe('settings appearance', () => {
  it('resolves explicit and system themes', () => {
    expect(resolveTheme('light', true)).toBe('light')
    expect(resolveTheme('dark', false)).toBe('dark')
    expect(resolveTheme('system', true)).toBe('dark')
    expect(resolveTheme('system', false)).toBe('light')
  })

  it('uses the Electron blue accent', () => {
    expect(accentPresets.blue).toEqual({ value: '#4f83cc', strong: '#3f6fae', contrast: '#ffffff' })
  })
})
