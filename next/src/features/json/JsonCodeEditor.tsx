import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands'
import { json } from '@codemirror/lang-json'
import { bracketMatching, HighlightStyle, indentOnInput, syntaxHighlighting } from '@codemirror/language'
import { Compartment, EditorState, type Extension } from '@codemirror/state'
import { drawSelection, dropCursor, EditorView, keymap } from '@codemirror/view'
import { tags } from '@lezer/highlight'
import { forwardRef, useEffect, useImperativeHandle, useRef, type CSSProperties } from 'react'

export type JsonCodeEditorHandle = {
  focus: () => void
  selectRange: (anchor: number, head: number) => void
}

type JsonCodeEditorProps = {
  value: string
  wrap: boolean
  fontSize: number
  ariaLabel: string
  onChange: (value: string) => void
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

export const JsonCodeEditor = forwardRef<JsonCodeEditorHandle, JsonCodeEditorProps>(function JsonCodeEditor(
  { value, wrap, fontSize, ariaLabel, onChange },
  ref
) {
  const hostRef = useRef<HTMLDivElement>(null)
  const viewRef = useRef<EditorView>(null)
  const onChangeRef = useRef(onChange)
  const localValueRef = useRef(value)
  const applyingExternalValueRef = useRef(false)
  const wrapCompartmentRef = useRef(new Compartment())
  const attributesCompartmentRef = useRef(new Compartment())
  const languageCompartmentRef = useRef(new Compartment())
  const richLanguageEnabled = value.length <= richJsonDocumentLimit
  onChangeRef.current = onChange

  useEffect(() => {
    if (!hostRef.current) return
    const wrapCompartment = wrapCompartmentRef.current
    const attributesCompartment = attributesCompartmentRef.current
    const languageCompartment = languageCompartmentRef.current
    const view = new EditorView({
      parent: hostRef.current,
      state: EditorState.create({
        doc: localValueRef.current,
        extensions: [
          history(),
          drawSelection(),
          dropCursor(),
          indentOnInput(),
          bracketMatching(),
          keymap.of([...defaultKeymap, ...historyKeymap, indentWithTab]),
          languageCompartment.of(jsonLanguageExtensions(localValueRef.current.length <= richJsonDocumentLimit)),
          wrapCompartment.of(wrap ? EditorView.lineWrapping : []),
          attributesCompartment.of(EditorView.contentAttributes.of({ 'aria-label': ariaLabel, spellcheck: 'false' })),
          EditorView.updateListener.of((update) => {
            if (!update.docChanged) return
            const nextValue = update.state.doc.toString()
            localValueRef.current = nextValue
            if (!applyingExternalValueRef.current) onChangeRef.current(nextValue)
          })
        ]
      })
    })
    viewRef.current = view
    return () => {
      viewRef.current = null
      view.destroy()
    }
  }, [])

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
      effects: wrapCompartmentRef.current.reconfigure(wrap ? EditorView.lineWrapping : [])
    })
  }, [wrap])

  useEffect(() => {
    viewRef.current?.dispatch({
      effects: attributesCompartmentRef.current.reconfigure(
        EditorView.contentAttributes.of({ 'aria-label': ariaLabel, spellcheck: 'false' })
      )
    })
  }, [ariaLabel])

  useEffect(() => {
    viewRef.current?.dispatch({
      effects: languageCompartmentRef.current.reconfigure(jsonLanguageExtensions(richLanguageEnabled))
    })
  }, [richLanguageEnabled])

  useImperativeHandle(ref, () => ({
    focus: () => viewRef.current?.focus(),
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
