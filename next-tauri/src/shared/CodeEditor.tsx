import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands'
import { bracketMatching, foldGutter } from '@codemirror/language'
import { EditorState, type Extension, Compartment } from '@codemirror/state'
import {
  drawSelection,
  EditorView,
  highlightActiveLine,
  highlightActiveLineGutter,
  keymap,
  lineNumbers
} from '@codemirror/view'
import { useEffect, useRef } from 'react'
import { clampCodeEditorSelection, readCodeEditorViewState, type CodeEditorViewState } from './codeEditorViewState'
import { electronCodeHighlighting } from './codeHighlight'
import { codeEditorSearchHighlight } from './codeEditorSearchHighlight'
import { defaultFindReplaceOptions, type FindReplaceOptions } from './findReplace'

interface CodeEditorProps {
  ariaLabel: string
  value: string
  onChange?(value: string): void
  onReady?(view: EditorView): void
  extensions?: Extension[]
  className?: string
  readOnly?: boolean
  lineWrapping?: boolean
  searchQuery?: string
  searchOptions?: FindReplaceOptions
  initialViewState?: CodeEditorViewState
  onViewStateChange?(state: CodeEditorViewState): void
}

export function CodeEditor({
  ariaLabel,
  value,
  onChange,
  onReady,
  extensions = [],
  className = '',
  readOnly = false,
  lineWrapping = true,
  searchQuery,
  searchOptions,
  initialViewState,
  onViewStateChange
}: CodeEditorProps) {
  const hostRef = useRef<HTMLDivElement>(null)
  const viewRef = useRef<EditorView | undefined>(undefined)
  const searchCompartment = useRef(new Compartment())
  const onChangeRef = useRef(onChange)
  const onReadyRef = useRef(onReady)
  const onViewStateChangeRef = useRef(onViewStateChange)
  const initialViewStateRef = useRef(initialViewState)
  onChangeRef.current = onChange
  onReadyRef.current = onReady
  onViewStateChangeRef.current = onViewStateChange
  initialViewStateRef.current = initialViewState

  useEffect(() => {
    const host = hostRef.current
    if (!host) return
    const view = new EditorView({
      parent: host,
      state: EditorState.create({
        doc: value,
        selection: clampCodeEditorSelection(initialViewStateRef.current, value.length),
        extensions: [
          history(),
          lineNumbers(),
          drawSelection(),
          highlightActiveLine(),
          highlightActiveLineGutter(),
          bracketMatching(),
          foldGutter(),
          electronCodeHighlighting(),
          keymap.of([
            ...defaultKeymap,
            ...historyKeymap,
            indentWithTab
          ]),
          ...(lineWrapping ? [EditorView.lineWrapping] : []),
          searchCompartment.current.of([]),
          EditorState.readOnly.of(readOnly),
          EditorView.editable.of(!readOnly),
          EditorView.contentAttributes.of({
            'aria-label': ariaLabel,
            spellcheck: 'false',
            autocapitalize: 'off',
            autocomplete: 'off'
          }),
          EditorView.updateListener.of((update) => {
            if (update.docChanged) onChangeRef.current?.(update.state.doc.toString())
            if (update.docChanged || update.selectionSet) onViewStateChangeRef.current?.(readCodeEditorViewState(update.view))
          }),
          ...extensions
        ]
      })
    })
    viewRef.current = view
    const saved = initialViewStateRef.current
    const restoreFrame = window.requestAnimationFrame(() => {
      if (!saved) return
      view.scrollDOM.scrollTop = saved.scrollTop
      view.scrollDOM.scrollLeft = saved.scrollLeft
    })
    const handleScroll = () => onViewStateChangeRef.current?.(readCodeEditorViewState(view))
    view.scrollDOM.addEventListener('scroll', handleScroll, { passive: true })
    onReadyRef.current?.(view)
    return () => {
      window.cancelAnimationFrame(restoreFrame)
      view.scrollDOM.removeEventListener('scroll', handleScroll)
      viewRef.current = undefined
      view.destroy()
    }
    // Extensions configure the editor for its lifetime. Callers remount by key when
    // changing language instead of creating a new array on every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ariaLabel, lineWrapping, readOnly])

  useEffect(() => {
    const view = viewRef.current
    if (!view) return
    const current = view.state.doc.toString()
    if (current === value) return
    view.dispatch({
      changes: { from: 0, to: view.state.doc.length, insert: value },
      selection: { anchor: Math.min(view.state.selection.main.head, value.length) }
    })
  }, [value])

  useEffect(() => {
    const view = viewRef.current
    if (!view || searchQuery === undefined) return
    view.dispatch({
      effects: searchCompartment.current.reconfigure(
        searchQuery ? codeEditorSearchHighlight(searchQuery, { ...defaultFindReplaceOptions, ...searchOptions }) : []
      )
    })
  }, [searchOptions, searchQuery])

  return <div className={`shared-code-editor ${className}`.trim()} ref={hostRef} />
}
