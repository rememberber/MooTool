import { EditorSelection, EditorState } from '@codemirror/state'
import { describe, expect, it } from 'vitest'
import { columnEditingExtensions } from './TextCodeEditor'

describe('columnEditingExtensions', () => {
  const twoCursors = EditorSelection.create([
    EditorSelection.cursor(0),
    EditorSelection.cursor(3)
  ])

  it('keeps the multiple selections produced by a rectangular drag', () => {
    const state = EditorState.create({
      doc: 'aa\nbb',
      extensions: columnEditingExtensions(true)
    })
    const withTwoCursors = state.update({ selection: twoCursors }).state
    const edited = withTwoCursors.update(withTwoCursors.replaceSelection('x')).state

    expect(withTwoCursors.selection.ranges).toHaveLength(2)
    expect(edited.doc.toString()).toBe('xaa\nxbb')
  })

  it('leaves other editors in single-selection mode', () => {
    const state = EditorState.create({
      doc: 'aa\nbb',
      extensions: columnEditingExtensions(false)
    })

    expect(state.update({ selection: twoCursors }).state.selection.ranges).toHaveLength(1)
  })
})
