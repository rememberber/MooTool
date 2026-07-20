import { describe, expect, it } from 'vitest'
import { compareText } from './diffTools'

describe('text diff', () => {
  it('builds Java-compatible character ranges and whole-line changes', () => {
    const result = compareText('one\ntwo\n', 'one\nthree\nplus\n', false)

    expect(result.changed).toBe(1)
    expect(result.added).toBe(1)
    expect(result.removed).toBe(0)
    expect(result.segments).toEqual([
      {
        type: 'change',
        leftStart: 5,
        leftEnd: 7,
        rightStart: 5,
        rightEnd: 9,
        wholeLine: false
      },
      {
        type: 'insert',
        leftStart: -1,
        leftEnd: -1,
        rightStart: 10,
        rightEnd: 14,
        wholeLine: true
      }
    ])
  })

  it('creates the same three-line-context unified format as the Java service', () => {
    const result = compareText('one\ntwo\n', 'one\nthree\nplus\n', false)

    expect(result.unified).toBe([
      '--- old',
      '+++ new',
      '@@ -1,3 +1,4 @@',
      ' one',
      '-two',
      '+three',
      '+plus',
      ' '
    ].join('\n'))
    expect(result.unifiedView.lineSpans.map((span) => span.type)).toEqual([
      'header-line',
      'header-line',
      'hunk-line',
      'delete-line',
      'add-line',
      'add-line'
    ])
    expect(result.unifiedView.characterEventCount).toBe(1)
  })

  it('ignores whitespace-only character differences without hiding the unified patch', () => {
    const result = compareText('one  two\n', 'one two\n', true)

    expect(result.segments).toEqual([])
    expect(result.unified).toContain('-one  two')
    expect(result.unified).toContain('+one two')
    expect(result.unifiedView.characterSpans).toEqual([])
  })

  it('retains trailing empty lines and reports complete inserted and deleted lines', () => {
    const inserted = compareText('same\n', 'same\nnew\n', false)
    const deleted = compareText('same\nold\n', 'same\n', false)

    expect(inserted.segments).toContainEqual({
      type: 'insert',
      leftStart: -1,
      leftEnd: -1,
      rightStart: 5,
      rightEnd: 8,
      wholeLine: true
    })
    expect(deleted.segments).toContainEqual({
      type: 'delete',
      leftStart: 5,
      leftEnd: 8,
      rightStart: -1,
      rightEnd: -1,
      wholeLine: true
    })
  })

  it('returns no patch for identical empty text', () => {
    expect(compareText('', '', false)).toMatchObject({
      segments: [],
      unified: '',
      added: 0,
      removed: 0,
      changed: 0
    })
  })
})
