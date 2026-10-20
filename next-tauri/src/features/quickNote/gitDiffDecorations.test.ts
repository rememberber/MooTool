import { describe, expect, it } from 'vitest'
import { gitDiffDecorations, gitDiffLanguage } from './gitDiffDecorations'

describe('git diff decorations', () => {
  it('highlights changed words inside the original before and after text', () => {
    const decorations = gitDiffDecorations('keep alpha\n', 'keep beta\n')
    const leftChanged = decorations.left.filter((item) => item.type === 'mark')
    const rightChanged = decorations.right.filter((item) => item.type === 'mark')
    expect(leftChanged).toEqual([
      expect.objectContaining({ from: 5, to: 10, className: 'cm-diff-character-changed' })
    ])
    expect(rightChanged).toEqual([
      expect.objectContaining({ from: 5, to: 9, className: 'cm-diff-character-changed' })
    ])
    expect(decorations.left.some((item) => item.type === 'line' && item.className === 'cm-diff-line-changed')).toBe(true)
    expect(decorations.right.some((item) => item.type === 'line' && item.className === 'cm-diff-line-changed')).toBe(true)
  })

  it('marks inserted and deleted lines on the side that contains them', () => {
    const decorations = gitDiffDecorations('alpha\nkeep', 'beta\nkeep')
    expect(decorations.left.some((item) => item.className.includes('removed') || item.className.includes('changed'))).toBe(true)
    expect(decorations.right.some((item) => item.className.includes('added') || item.className.includes('changed'))).toBe(true)
    const keepStart = 'beta\n'.length
    expect(decorations.right.filter((item) => item.type === 'mark').every((item) => (item.to ?? item.from) <= keepStart)).toBe(true)
  })

  it('selects the note grammar from the file extension', () => {
    expect(gitDiffLanguage('Work/API ideas.md')).toHaveLength(1)
    expect(gitDiffLanguage('schema.json')).toHaveLength(1)
    expect(gitDiffLanguage('notes.txt')).toEqual([])
  })
})
