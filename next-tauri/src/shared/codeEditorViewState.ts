import type { EditorView } from '@codemirror/view'

export type CodeEditorViewState = {
  anchor: number
  head: number
  scrollTop: number
  scrollLeft: number
}

export function readCodeEditorViewState(view: EditorView): CodeEditorViewState {
  const selection = view.state.selection.main
  return {
    anchor: selection.anchor,
    head: selection.head,
    scrollTop: view.scrollDOM.scrollTop,
    scrollLeft: view.scrollDOM.scrollLeft
  }
}

export function clampCodeEditorSelection(
  state: Pick<CodeEditorViewState, 'anchor' | 'head'> | undefined,
  documentLength: number
): { anchor: number; head: number } | undefined {
  if (!state) return undefined
  return {
    anchor: Math.max(0, Math.min(state.anchor, documentLength)),
    head: Math.max(0, Math.min(state.head, documentLength))
  }
}
