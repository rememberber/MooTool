import { describe, expect, it } from 'vitest'
import { clampCodeEditorSelection } from './codeEditorViewState'

describe('clampCodeEditorSelection', () => {
  it('keeps a restored selection inside the current document', () => {
    expect(clampCodeEditorSelection({ anchor: 7, head: 20 }, 12)).toEqual({ anchor: 7, head: 12 })
    expect(clampCodeEditorSelection({ anchor: -4, head: 3 }, 12)).toEqual({ anchor: 0, head: 3 })
  })

  it('preserves reverse selection direction', () => {
    expect(clampCodeEditorSelection({ anchor: 9, head: 2 }, 12)).toEqual({ anchor: 9, head: 2 })
  })

  it('does not create a selection when no session state exists', () => {
    expect(clampCodeEditorSelection(undefined, 12)).toBeUndefined()
  })
})
