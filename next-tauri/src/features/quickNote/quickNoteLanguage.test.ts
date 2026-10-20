import { describe, expect, it } from 'vitest'
import { quickNoteLanguage } from './quickNoteLanguage'

describe('quick note languages', () => {
  it('provides the Electron grammar for every highlighted syntax', () => {
    for (const syntax of ['markdown', 'json', 'java', 'javascript', 'typescript', 'python', 'html', 'xml', 'yaml', 'sql'] as const) {
      expect(quickNoteLanguage(syntax)).toHaveLength(1)
    }
    expect(quickNoteLanguage('plain')).toEqual([])
  })
})
