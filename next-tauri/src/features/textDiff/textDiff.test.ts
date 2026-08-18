import { describe, expect, it } from 'vitest'
import {
  collapseUnchangedRows,
  compareText,
  createUnifiedDiff,
  limitRenderedRows
} from './textDiff'

const strict = { ignoreCase: false, ignoreWhitespace: false }

describe('text diff', () => {
  it('aligns changed, added, removed, and unchanged lines', () => {
    const result = compareText(
      ['alpha', 'name = old', 'removed', 'omega'].join('\n'),
      ['alpha', 'name = new', 'added one', 'added two', 'omega'].join('\n'),
      strict
    )

    expect(result.stats).toEqual({ added: 2, removed: 1, changed: 1, unchanged: 2 })
    expect(result.rows.map((row) => row.kind)).toEqual([
      'equal',
      'changed',
      'removed',
      'added',
      'added',
      'equal'
    ])
    expect(result.rows[1].left?.segments.some((segment) => segment.kind === 'changed')).toBe(true)
    expect(result.rows[1].right?.segments.some((segment) => segment.kind === 'changed')).toBe(true)
  })

  it('supports case and whitespace normalization without changing displayed text', () => {
    const result = compareText('Hello   World', 'hello world', {
      ignoreCase: true,
      ignoreWhitespace: true
    })

    expect(result.identical).toBe(true)
    expect(result.rows[0].left?.text).toBe('Hello   World')
    expect(result.rows[0].right?.text).toBe('hello world')
  })

  it('collapses distant unchanged context and reports the hidden count', () => {
    const left = Array.from({ length: 12 }, (_, index) => `line ${index}`).join('\n')
    const right = left.replace('line 7', 'line seven')
    const result = compareText(left, right, strict)

    const visible = collapseUnchangedRows(result.rows, 1)

    expect(visible.some((row) => row.kind === 'collapsed')).toBe(true)
    expect(visible.filter((row) => row.kind === 'changed')).toHaveLength(1)
  })

  it('exports a deterministic unified representation', () => {
    const result = compareText('a\nold', 'a\nnew\nextra', strict)

    expect(createUnifiedDiff('before', 'after', result)).toBe(
      ['--- before', '+++ after', ' a', '-old', '+new', '+extra'].join('\n')
    )
  })

  it('handles empty documents', () => {
    expect(compareText('', '', strict)).toMatchObject({
      rows: [],
      identical: true,
      stats: { added: 0, removed: 0, changed: 0, unchanged: 0 }
    })
    expect(compareText('', 'new', strict).rows[0].kind).toBe('added')
    expect(compareText('old', '', strict).rows[0].kind).toBe('removed')
  })

  it('uses bounded large-document fallbacks and render limits', () => {
    const left = Array.from({ length: 3000 }, (_, index) => `left ${index}`).join('\n')
    const right = Array.from({ length: 3000 }, (_, index) => `right ${index}`).join('\n')

    const result = compareText(left, right, strict)
    const rendered = limitRenderedRows(result.rows, 100)

    expect(result.stats).toEqual({
      added: 3000,
      removed: 3000,
      changed: 0,
      unchanged: 0
    })
    expect(rendered).toHaveLength(101)
    expect(rendered[50]).toEqual({ kind: 'collapsed', hiddenRows: 5900, reason: 'limit' })
  })
})
