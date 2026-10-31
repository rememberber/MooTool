import { describe, expect, it, vi } from 'vitest'
import { cssFontFamily, fallbackEditorFonts, fontSelectOptions, listSystemFontFamilies } from './systemFonts'

describe('cssFontFamily', () => {
  it('uses the app UI stack for empty and system-ui values', () => {
    expect(cssFontFamily('')).toBe('var(--app-font-family), system-ui, sans-serif')
    expect(cssFontFamily('system-ui')).toBe('var(--app-font-family), system-ui, sans-serif')
  })

  it('keeps a monospace fallback stack for ui-monospace', () => {
    expect(cssFontFamily('ui-monospace')).toBe('ui-monospace, SFMono-Regular, Menlo, Consolas, monospace')
  })

  it('quotes font names that need CSS escaping', () => {
    expect(cssFontFamily('PingFang SC')).toBe('"PingFang SC", ui-monospace, SFMono-Regular, Menlo, Consolas, monospace')
    expect(cssFontFamily('Font "Demo"')).toBe('"Font \\"Demo\\"", ui-monospace, SFMono-Regular, Menlo, Consolas, monospace')
  })
})

describe('fontSelectOptions', () => {
  it('keeps ui-monospace first and includes the current font', () => {
    expect(fontSelectOptions(['Georgia', 'ui-monospace', 'Arial'], 'Comic Sans MS')).toEqual([
      'ui-monospace',
      'Arial',
      'Comic Sans MS',
      'Georgia'
    ])
  })
})

describe('listSystemFontFamilies', () => {
  it('merges queryLocalFonts families with the fallback list', async () => {
    vi.stubGlobal('queryLocalFonts', async () => [{ family: '  Custom Editor Font  ' }, { family: '' }])
    try {
      const fonts = await listSystemFontFamilies()
      expect(fonts).toContain('Custom Editor Font')
      expect(fonts).toEqual(expect.arrayContaining(fallbackEditorFonts))
    } finally {
      vi.unstubAllGlobals()
    }
  })
})
