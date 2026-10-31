import { describe, expect, it } from 'vitest'
import { quickNoteLanguage } from './quickNoteLanguage'

describe('quick note languages', () => {
  it('provides the Electron grammar for every highlighted syntax', () => {
    for (const syntax of ['text/markdown', 'application/json', 'text/java', 'text/javascript', 'text/typescript', 'text/python', 'text/html', 'text/xml', 'text/yaml', 'text/sql'] as const) {
      expect(quickNoteLanguage(syntax)).toHaveLength(1)
    }
    expect(quickNoteLanguage('text/plain')).toEqual([])
  })
})
