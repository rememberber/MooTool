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

describe('on-demand editor grammars', () => {
  it('loads each supported language and preserves syntax support', async () => {
    const { loadCodeEditorLanguage, codeEditorLanguageExtensions, textCodeEditorLanguages } = await import('./codeEditorLanguage')
    const { EditorState } = await import('@codemirror/state')
    const { syntaxTree } = await import('@codemirror/language')
    for (const language of textCodeEditorLanguages) {
      await Promise.all([loadCodeEditorLanguage(language), loadCodeEditorLanguage(language)])
      const state = EditorState.create({ doc: '{}', extensions: codeEditorLanguageExtensions(language) })
      if (language === 'json') expect(syntaxTree(state).topNode.name).toBe('JsonText')
    }
  })
})
