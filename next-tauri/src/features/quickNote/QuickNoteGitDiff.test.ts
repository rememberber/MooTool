import { createElement } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { GitDiffView } from './QuickNoteGitPanel'

const labels = {
  title: 'Diff',
  before: 'Before',
  after: 'After',
  empty: 'Empty',
  binary: 'Binary',
  tooLarge: 'Too large',
  file: 'File'
}

describe('GitDiffView', () => {
  it('labels the before and after editors with the git status', () => {
    const html = renderToStaticMarkup(createElement(GitDiffView, {
      files: [{ path: 'Work/note.md', status: ' M', before: 'alpha\nkeep', after: 'beta\nkeep', preview: 'text' }],
      selectedPath: 'Work/note.md',
      onSelectPath: () => undefined,
      labels
    }))
    expect(html).toContain('Before')
    expect(html).toContain('After')
    expect(html).toContain('M  Work/note.md')
    expect(html).toContain('git-diff-editor')
  })

  it('shows the binary placeholder instead of editors', () => {
    const html = renderToStaticMarkup(createElement(GitDiffView, {
      files: [{ path: 'image.png', status: 'A', before: '', after: '', preview: 'binary' }],
      selectedPath: 'image.png',
      onSelectPath: () => undefined,
      labels
    }))
    expect(html).toContain('Binary')
    expect(html).not.toContain('git-diff-editor')
  })
})
