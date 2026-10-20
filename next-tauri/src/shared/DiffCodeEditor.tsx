import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands'
import { EditorState, StateEffect, StateField, type Extension } from '@codemirror/state'
import {
  Decoration,
  type DecorationSet,
  drawSelection,
  EditorView,
  highlightActiveLine,
  highlightActiveLineGutter,
  keymap,
  lineNumbers
} from '@codemirror/view'
import { forwardRef, useEffect, useImperativeHandle, useRef } from 'react'
import { electronCodeHighlighting } from './codeHighlight'

export type DiffEditorDecoration = {
  from: number
  to?: number
  className: string
  type: 'line' | 'mark'
}

export type DiffEditorScroll = {
  scrollTop: number
  scrollLeft: number
}

export type DiffEditorHandle = {
  syncScroll: (scrollTop: number, scrollLeft: number) => void
  selectRange: (anchor: number, head: number) => void
}

const replaceDecorations = StateEffect.define<DecorationSet>()

const externalDecorations = StateField.define<DecorationSet>({
  create: () => Decoration.none,
  update: (decorations, transaction) => {
    let next = decorations.map(transaction.changes)
    for (const effect of transaction.effects) {
      if (effect.is(replaceDecorations)) next = effect.value
    }
    return next
  },
  provide: (field) => EditorView.decorations.from(field)
})

function buildDecorations(decorations: readonly DiffEditorDecoration[], documentLength: number): DecorationSet {
  const ranges = decorations.flatMap((decoration) => {
    const from = Math.max(0, Math.min(decoration.from, documentLength))
    if (decoration.type === 'line') {
      if (documentLength === 0) return []
      return [Decoration.line({ attributes: { class: decoration.className } }).range(Math.min(from, documentLength))]
    }
    const to = Math.max(from, Math.min(decoration.to ?? from, documentLength))
    return to > from ? [Decoration.mark({ class: decoration.className }).range(from, to)] : []
  })
  return Decoration.set(ranges, true)
}

export const DiffCodeEditor = forwardRef<DiffEditorHandle, {
  value: string
  ariaLabel: string
  decorations: readonly DiffEditorDecoration[]
  extensions?: Extension[]
  languageKey?: string
  className?: string
  readOnly?: boolean
  onChange?: (value: string) => void
  onScroll?: (scroll: DiffEditorScroll) => void
}>(function DiffCodeEditor({
  value,
  ariaLabel,
  decorations,
  extensions = [],
  languageKey = '',
  className = 'git-diff-editor',
  readOnly = true,
  onChange,
  onScroll
}, ref) {
  const hostRef = useRef<HTMLDivElement>(null)
  const viewRef = useRef<EditorView | null>(null)
  const onScrollRef = useRef(onScroll)
  const onChangeRef = useRef(onChange)
  const extensionsRef = useRef(extensions)
  const valueRef = useRef(value)
  onScrollRef.current = onScroll
  onChangeRef.current = onChange
  extensionsRef.current = extensions
  valueRef.current = value

  useImperativeHandle(ref, () => ({
    syncScroll: (scrollTop, scrollLeft) => {
      const scroller = viewRef.current?.scrollDOM
      if (!scroller) return
      scroller.scrollTop = scrollTop
      scroller.scrollLeft = scrollLeft
    },
    selectRange: (anchor, head) => {
      const view = viewRef.current
      if (!view) return
      const start = Math.max(0, Math.min(anchor, view.state.doc.length))
      const end = Math.max(start, Math.min(head, view.state.doc.length))
      view.dispatch({ selection: { anchor: start, head: end }, scrollIntoView: true })
      view.focus()
    }
  }), [])

  useEffect(() => {
    const host = hostRef.current
    if (!host) return
    const view = new EditorView({
      parent: host,
      state: EditorState.create({
        doc: valueRef.current,
        extensions: [
          lineNumbers(),
          drawSelection(),
          highlightActiveLine(),
          highlightActiveLineGutter(),
          electronCodeHighlighting(),
          externalDecorations,
          ...(readOnly ? [] : [history(), keymap.of([...defaultKeymap, ...historyKeymap, indentWithTab])]),
          EditorState.readOnly.of(readOnly),
          EditorView.editable.of(!readOnly),
          EditorView.updateListener.of((update) => {
            if (update.docChanged) onChangeRef.current?.(update.state.doc.toString())
          }),
          EditorView.contentAttributes.of({
            'aria-label': ariaLabel,
            spellcheck: 'false'
          }),
          ...extensionsRef.current
        ]
      })
    })
    viewRef.current = view
    const scroller = view.scrollDOM
    const handleScroll = () => {
      onScrollRef.current?.({ scrollTop: scroller.scrollTop, scrollLeft: scroller.scrollLeft })
    }
    scroller.addEventListener('scroll', handleScroll)
    return () => {
      scroller.removeEventListener('scroll', handleScroll)
      view.destroy()
      viewRef.current = null
    }
  }, [ariaLabel, languageKey, readOnly])

  useEffect(() => {
    const view = viewRef.current
    if (!view || view.state.doc.toString() === value) return
    view.dispatch({ changes: { from: 0, to: view.state.doc.length, insert: value } })
  }, [value])

  useEffect(() => {
    const view = viewRef.current
    if (!view) return
    view.dispatch({ effects: replaceDecorations.of(buildDecorations(decorations, view.state.doc.length)) })
  }, [decorations, value])

  return <div className={`shared-code-editor ${className}`.trim()} ref={hostRef} />
})
