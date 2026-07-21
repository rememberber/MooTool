import { describe, expect, it } from 'vitest'
import { resolveTextCodeEditorLanguage } from './codeEditorLanguage'

describe('resolveTextCodeEditorLanguage', () => {
  it('maps MIME types used by Quick Note and HTTP bodies', () => {
    expect(resolveTextCodeEditorLanguage('application/json')).toBe('json')
    expect(resolveTextCodeEditorLanguage('application/problem+json')).toBe('json')
    expect(resolveTextCodeEditorLanguage('text/markdown')).toBe('markdown')
    expect(resolveTextCodeEditorLanguage('text/typescript')).toBe('typescript')
    expect(resolveTextCodeEditorLanguage('application/xml')).toBe('xml')
    expect(resolveTextCodeEditorLanguage('text/html')).toBe('html')
  })

  it('accepts short runtime names and safely falls back to text', () => {
    expect(resolveTextCodeEditorLanguage('node')).toBe('javascript')
    expect(resolveTextCodeEditorLanguage('py')).toBe('python')
    expect(resolveTextCodeEditorLanguage('yml')).toBe('yaml')
    expect(resolveTextCodeEditorLanguage('unknown/type')).toBe('text')
    expect(resolveTextCodeEditorLanguage()).toBe('text')
  })
})
