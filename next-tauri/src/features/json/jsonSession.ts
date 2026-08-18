import { contentFingerprint } from './jsonTools'

export interface JsonSessionState {
  content: string
  anchor: number
  head: number
  line: number
  scrollTop: number
  wrap: boolean
  indent: number
  sortKeys: boolean
  jsonPath: string
  compositionStarts: number
  compositionEnds: number
}

export function describeJsonSession(state: JsonSessionState): {
  digest: string
  contentLength: number
  line: number
  selectionFrom: number
  selectionTo: number
} {
  const from = Math.min(state.anchor, state.head)
  const to = Math.max(state.anchor, state.head)
  return {
    digest: JSON.stringify({
      contentLength: state.content.length,
      contentHash: contentFingerprint(state.content),
      selection: [from, to],
      line: state.line,
      scrollTop: Math.round(state.scrollTop),
      wrap: state.wrap,
      indent: state.indent,
      sortKeys: state.sortKeys,
      jsonPath: state.jsonPath,
      composition: [state.compositionStarts, state.compositionEnds]
    }),
    contentLength: state.content.length,
    line: state.line,
    selectionFrom: from,
    selectionTo: to
  }
}
