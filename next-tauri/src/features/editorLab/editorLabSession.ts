export interface EditorLabSessionState {
  content: string
  anchor: number
  head: number
  line: number
  scrollTop: number
  scrollLeft: number
  searchQuery: string
  changeCount: number
  compositionStarts: number
  compositionEnds: number
}

export interface EditorLabStateDescription {
  digest: string
  summary: string
}

export function describeEditorLabState(
  state: EditorLabSessionState
): EditorLabStateDescription {
  const selectionStart = Math.min(state.anchor, state.head)
  const selectionEnd = Math.max(state.anchor, state.head)
  return {
    digest: JSON.stringify({
      content: state.content,
      anchor: state.anchor,
      head: state.head,
      line: state.line,
      scrollTop: Math.round(state.scrollTop),
      scrollLeft: Math.round(state.scrollLeft),
      searchQuery: state.searchQuery,
      changeCount: state.changeCount,
      compositionStarts: state.compositionStarts,
      compositionEnds: state.compositionEnds
    }),
    summary: `${state.content.length} 字符 · 选区 ${selectionStart}:${selectionEnd} · 第 ${state.line} 行 · IME ${state.compositionStarts}/${state.compositionEnds}`
  }
}
