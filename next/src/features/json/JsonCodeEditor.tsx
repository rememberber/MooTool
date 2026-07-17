import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands'
import { json } from '@codemirror/lang-json'
import { bracketMatching, HighlightStyle, indentOnInput, syntaxHighlighting } from '@codemirror/language'
import { Compartment, EditorState, type Extension } from '@codemirror/state'
import {
  drawSelection,
  dropCursor,
  EditorView,
  highlightActiveLine,
  highlightActiveLineGutter,
  keymap,
  lineNumbers
} from '@codemirror/view'
import { tags } from '@lezer/highlight'
import { forwardRef, useEffect, useImperativeHandle, useRef, type CSSProperties } from 'react'
import { codeEditorSearchHighlight } from '@/shared/components/codeEditorSearchHighlight'
import {
  clampCodeEditorSelection,
  readCodeEditorViewState,
  type CodeEditorViewState
} from '@/shared/components/codeEditorViewState'

export type JsonCodeEditorHandle = {
  focus: () => void
  getViewState: () => CodeEditorViewState | null
  selectRange: (anchor: number, head: number) => void
}

type JsonCodeEditorProps = {
  value: string
  wrap: boolean
  fontSize: number
  searchQuery: string
  ariaLabel: string
  initialViewState?: CodeEditorViewState
  onChange: (value: string) => void
  onViewStateChange?: (state: CodeEditorViewState) => void
}

const richJsonDocumentLimit = 1_000_000
const jsonHighlightStyle = HighlightStyle.define([
  { tag: tags.propertyName, color: 'var(--syntax-property)' },
  { tag: tags.string, color: 'var(--syntax-string)' },
  { tag: tags.number, color: 'var(--syntax-number)' },
  { tag: [tags.bool, tags.null], color: 'var(--syntax-literal)' }
])

function jsonLanguageExtensions(enabled: boolean): Extension {
  return enabled
    ? [json(), syntaxHighlighting(jsonHighlightStyle)]
    : []
}

function editorMetrics(fontSize: number): Extension {
  return EditorView.theme({ '&': { fontSize: `${fontSize}px` } })
}

function useCompartment(): Compartment {
  const compartmentRef = useRef<Compartment | null>(null)
  if (compartmentRef.current === null) compartmentRef.current = new Compartment()
  return compartmentRef.current
}

export const JsonCodeEditor = forwardRef<JsonCodeEditorHandle, JsonCodeEditorProps>(function JsonCodeEditor(
  { value, wrap, fontSize, searchQuery, ariaLabel, initialViewState, onChange, onViewStateChange },
  ref
) {
  const hostRef = useRef<HTMLDivElement>(null)
  const viewRef = useRef<EditorView>(null)
  const onChangeRef = useRef(onChange)
  const onViewStateChangeRef = useRef(onViewStateChange)
  const localValueRef = useRef(value)
  const applyingExternalValueRef = useRef(false)
  const initialConfigRef = useRef({ wrap, ariaLabel, searchQuery, fontSize, initialViewState })
  const wrapCompartment = useCompartment()
  const attributesCompartment = useCompartment()
  const languageCompartment = useCompartment()
  const searchCompartment = useCompartment()
  const metricsCompartment = useCompartment()
  const richLanguageEnabled = value.length <= richJsonDocumentLimit
  onChangeRef.current = onChange
  onViewStateChangeRef.current = onViewStateChange

  useEffect(() => {
    if (!hostRef.current) return
    const initialConfig = initialConfigRef.current
    const view = new EditorView({
      parent: hostRef.current,
      state: EditorState.create({
        doc: localValueRef.current,
        selection: clampCodeEditorSelection(initialConfig.initialViewState, localValueRef.current.length),
        extensions: [
          history(),
          drawSelection(),
          dropCursor(),
          lineNumbers(),
          highlightActiveLine(),
          highlightActiveLineGutter(),
          indentOnInput(),
          bracketMatching(),
          keymap.of([...defaultKeymap, ...historyKeymap, indentWithTab]),
          languageCompartment.of(jsonLanguageExtensions(localValueRef.current.length <= richJsonDocumentLimit)),
          wrapCompartment.of(initialConfig.wrap ? EditorView.lineWrapping : []),
          attributesCompartment.of(EditorView.contentAttributes.of({ 'aria-label': initialConfig.ariaLabel, spellcheck: 'false' })),
          searchCompartment.of(codeEditorSearchHighlight(initialConfig.searchQuery)),
          metricsCompartment.of(editorMetrics(initialConfig.fontSize)),
          EditorView.updateListener.of((update) => {
            if (update.docChanged) {
              const nextValue = update.state.doc.toString()
              localValueRef.current = nextValue
              if (!applyingExternalValueRef.current) onChangeRef.current(nextValue)
            }
            if (update.docChanged || update.selectionSet) {
              onViewStateChangeRef.current?.(readCodeEditorViewState(update.view))
            }
          })
        ]
      })
    })
    const restoreFrame = window.requestAnimationFrame(() => {
      if (initialConfig.initialViewState) {
        view.scrollDOM.scrollTop = initialConfig.initialViewState.scrollTop
        view.scrollDOM.scrollLeft = initialConfig.initialViewState.scrollLeft
      }
      view.requestMeasure()
      onViewStateChangeRef.current?.(readCodeEditorViewState(view))
    })
    const handleScroll = () => onViewStateChangeRef.current?.(readCodeEditorViewState(view))
    view.scrollDOM.addEventListener('scroll', handleScroll, { passive: true })
    viewRef.current = view
    return () => {
      window.cancelAnimationFrame(restoreFrame)
      view.scrollDOM.removeEventListener('scroll', handleScroll)
      viewRef.current = null
      view.destroy()
    }
  }, [attributesCompartment, languageCompartment, metricsCompartment, searchCompartment, wrapCompartment])

  useEffect(() => {
    const view = viewRef.current
    if (!view || value === localValueRef.current) return
    applyingExternalValueRef.current = true
    view.dispatch({ changes: { from: 0, to: view.state.doc.length, insert: value } })
    applyingExternalValueRef.current = false
    localValueRef.current = value
  }, [value])

  useEffect(() => {
    viewRef.current?.dispatch({
      effects: wrapCompartment.reconfigure(wrap ? EditorView.lineWrapping : [])
    })
  }, [wrap, wrapCompartment])

  useEffect(() => {
    viewRef.current?.dispatch({
      effects: attributesCompartment.reconfigure(
        EditorView.contentAttributes.of({ 'aria-label': ariaLabel, spellcheck: 'false' })
      )
    })
  }, [ariaLabel, attributesCompartment])

  useEffect(() => {
    viewRef.current?.dispatch({
      effects: languageCompartment.reconfigure(jsonLanguageExtensions(richLanguageEnabled))
    })
  }, [languageCompartment, richLanguageEnabled])

  useEffect(() => {
    viewRef.current?.dispatch({
      effects: searchCompartment.reconfigure(codeEditorSearchHighlight(searchQuery))
    })
  }, [searchCompartment, searchQuery])

  useEffect(() => {
    const view = viewRef.current
    if (!view) return
    view.dispatch({ effects: metricsCompartment.reconfigure(editorMetrics(fontSize)) })
    view.requestMeasure()
  }, [fontSize, metricsCompartment])

  useImperativeHandle(ref, () => ({
    focus: () => viewRef.current?.focus(),
    getViewState: () => viewRef.current ? readCodeEditorViewState(viewRef.current) : null,
    selectRange: (anchor, head) => {
      const view = viewRef.current
      if (!view) return
      const start = Math.max(0, Math.min(anchor, view.state.doc.length))
      const end = Math.max(start, Math.min(head, view.state.doc.length))
      view.dispatch({ selection: { anchor: start, head: end }, scrollIntoView: true })
      view.focus()
    }
  }), [])

  return (
    <div
      ref={hostRef}
      className="json-editor"
      style={{ '--json-editor-font-size': `${fontSize}px` } as CSSProperties}
    />
  )
})
