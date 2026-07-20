import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands'
import { Compartment, EditorState, StateEffect, StateField, type Extension } from '@codemirror/state'
import {
  Decoration,
  type DecorationSet,
  drawSelection,
  dropCursor,
  EditorView,
  highlightActiveLine,
  highlightActiveLineGutter,
  keymap,
  lineNumbers,
  placeholder as editorPlaceholder
} from '@codemirror/view'
import { forwardRef, useEffect, useImperativeHandle, useRef, type CSSProperties } from 'react'
import { codeEditorSearchHighlight } from './codeEditorSearchHighlight'
import {
  clampCodeEditorSelection,
  readCodeEditorViewState,
  type CodeEditorViewState
} from './codeEditorViewState'
import { defaultFindReplaceOptions, type FindReplaceOptions } from './findReplace'

export type TextCodeEditorHandle = {
  focus: () => void
  getSelection: () => { start: number; end: number }
  getViewState: () => CodeEditorViewState | null
  selectRange: (anchor: number, head: number) => void
  syncScroll: (scrollTop: number, scrollLeft: number) => void
}

export type TextCodeEditorScroll = {
  scrollTop: number
  scrollLeft: number
}

export type TextCodeEditorDecoration = {
  from: number
  to?: number
  className: string
  type: 'line' | 'mark'
}

export type TextCodeEditorProps = {
  value: string
  ariaLabel: string
  className?: string
  id?: string
  testId?: string
  placeholder?: string
  readOnly?: boolean
  wrap?: boolean
  fontSize?: number
  fontFamily?: string
  searchQuery?: string
  searchOptions?: FindReplaceOptions
  decorations?: readonly TextCodeEditorDecoration[]
  initialViewState?: CodeEditorViewState
  onChange?: (value: string) => void
  onPasteText?: (value: string) => void
  onKeyDown?: (event: KeyboardEvent) => void
  onScroll?: (scroll: TextCodeEditorScroll) => void
  onViewStateChange?: (state: CodeEditorViewState) => void
}

const emptyDecorations: readonly TextCodeEditorDecoration[] = []
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

function buildDecorations(decorations: readonly TextCodeEditorDecoration[], documentLength: number): DecorationSet {
  const ranges = decorations.flatMap((decoration) => {
    const from = Math.max(0, Math.min(decoration.from, documentLength))
    if (decoration.type === 'line') {
      return [Decoration.line({ attributes: { class: decoration.className } }).range(from)]
    }
    const to = Math.max(from, Math.min(decoration.to ?? from, documentLength))
    return to > from ? [Decoration.mark({ class: decoration.className }).range(from, to)] : []
  })
  return Decoration.set(ranges, true)
}

function editorMetrics(fontFamily?: string, fontSize?: number): Extension {
  const editor: Record<string, string> = {}
  const scroller: Record<string, string> = {}
  if (fontFamily) {
    editor.fontFamily = fontFamily
    scroller.fontFamily = fontFamily
  }
  if (fontSize) editor.fontSize = `${fontSize}px`
  return EditorView.theme({ '&': editor, '.cm-scroller': scroller })
}

function editorAttributes(ariaLabel: string, id?: string, testId?: string): Record<string, string> {
  return {
    'aria-label': ariaLabel,
    spellcheck: 'false',
    ...(id ? { id } : {}),
    ...(testId ? { 'data-testid': testId } : {})
  }
}

function readOnlyExtensions(readOnly: boolean): Extension {
  return [EditorState.readOnly.of(readOnly), EditorView.editable.of(!readOnly)]
}

function useCompartment(): Compartment {
  const compartmentRef = useRef<Compartment | null>(null)
  if (compartmentRef.current === null) compartmentRef.current = new Compartment()
  return compartmentRef.current
}

export const TextCodeEditor = forwardRef<TextCodeEditorHandle, TextCodeEditorProps>(function TextCodeEditor(
  {
    value,
    ariaLabel,
    className = '',
    id,
    testId,
    placeholder = '',
    readOnly = false,
    wrap = true,
    fontSize,
    fontFamily,
    searchQuery = '',
    searchOptions = defaultFindReplaceOptions,
    decorations = emptyDecorations,
    initialViewState,
    onChange,
    onPasteText,
    onKeyDown,
    onScroll,
    onViewStateChange
  },
  ref
) {
  const hostRef = useRef<HTMLDivElement>(null)
  const viewRef = useRef<EditorView>(null)
  const onChangeRef = useRef(onChange)
  const onPasteTextRef = useRef(onPasteText)
  const onKeyDownRef = useRef(onKeyDown)
  const onScrollRef = useRef(onScroll)
  const onViewStateChangeRef = useRef(onViewStateChange)
  const localValueRef = useRef(value)
  const applyingExternalValueRef = useRef(false)
  const initialConfigRef = useRef({ ariaLabel, id, testId, placeholder, readOnly, wrap, fontFamily, fontSize, searchQuery, searchOptions, initialViewState })
  const wrapCompartment = useCompartment()
  const attributesCompartment = useCompartment()
  const placeholderCompartment = useCompartment()
  const readOnlyCompartment = useCompartment()
  const searchCompartment = useCompartment()
  const metricsCompartment = useCompartment()
  onChangeRef.current = onChange
  onPasteTextRef.current = onPasteText
  onKeyDownRef.current = onKeyDown
  onScrollRef.current = onScroll
  onViewStateChangeRef.current = onViewStateChange

  useEffect(() => {
    if (!hostRef.current) return
    const initial = initialConfigRef.current
    const view = new EditorView({
      parent: hostRef.current,
      state: EditorState.create({
        doc: localValueRef.current,
        selection: clampCodeEditorSelection(initial.initialViewState, localValueRef.current.length),
        extensions: [
          history(),
          drawSelection(),
          dropCursor(),
          lineNumbers(),
          highlightActiveLine(),
          highlightActiveLineGutter(),
          keymap.of([...defaultKeymap, ...historyKeymap, indentWithTab]),
          wrapCompartment.of(initial.wrap ? EditorView.lineWrapping : []),
          attributesCompartment.of(EditorView.contentAttributes.of(editorAttributes(initial.ariaLabel, initial.id, initial.testId))),
          placeholderCompartment.of(initial.placeholder ? editorPlaceholder(initial.placeholder) : []),
          readOnlyCompartment.of(readOnlyExtensions(initial.readOnly)),
          searchCompartment.of(codeEditorSearchHighlight(initial.searchQuery, initial.searchOptions)),
          metricsCompartment.of(editorMetrics(initial.fontFamily, initial.fontSize)),
          externalDecorations,
          EditorState.tabSize.of(4),
          EditorView.domEventHandlers({
            keydown: (event) => { onKeyDownRef.current?.(event) }
          }),
          EditorView.updateListener.of((update) => {
            if (update.docChanged) {
              const nextValue = update.state.doc.toString()
              localValueRef.current = nextValue
              if (!applyingExternalValueRef.current) {
                onChangeRef.current?.(nextValue)
                if (update.transactions.some((transaction) => transaction.isUserEvent('input.paste'))) {
                  onPasteTextRef.current?.(nextValue)
                }
              }
            }
            if (update.docChanged || update.selectionSet) {
              onViewStateChangeRef.current?.(readCodeEditorViewState(update.view))
            }
          })
        ]
      })
    })
    const restoreFrame = window.requestAnimationFrame(() => {
      if (initial.initialViewState) {
        view.scrollDOM.scrollTop = initial.initialViewState.scrollTop
        view.scrollDOM.scrollLeft = initial.initialViewState.scrollLeft
      }
      view.requestMeasure()
      onViewStateChangeRef.current?.(readCodeEditorViewState(view))
    })
    const handleScroll = () => {
      const scroller = view.scrollDOM
      onScrollRef.current?.({
        scrollTop: scroller.scrollTop,
        scrollLeft: scroller.scrollLeft
      })
      onViewStateChangeRef.current?.(readCodeEditorViewState(view))
    }
    view.scrollDOM.addEventListener('scroll', handleScroll, { passive: true })
    viewRef.current = view
    return () => {
      window.cancelAnimationFrame(restoreFrame)
      view.scrollDOM.removeEventListener('scroll', handleScroll)
      viewRef.current = null
      view.destroy()
    }
  }, [attributesCompartment, metricsCompartment, placeholderCompartment, readOnlyCompartment, searchCompartment, wrapCompartment])

  useEffect(() => {
    const view = viewRef.current
    if (!view || value === localValueRef.current) return
    applyingExternalValueRef.current = true
    view.dispatch({ changes: { from: 0, to: view.state.doc.length, insert: value } })
    applyingExternalValueRef.current = false
    localValueRef.current = value
  }, [value])

  useEffect(() => {
    const view = viewRef.current
    if (!view) return
    view.dispatch({ effects: replaceDecorations.of(buildDecorations(decorations, view.state.doc.length)) })
  }, [decorations])

  useEffect(() => {
    viewRef.current?.dispatch({ effects: wrapCompartment.reconfigure(wrap ? EditorView.lineWrapping : []) })
  }, [wrap, wrapCompartment])

  useEffect(() => {
    viewRef.current?.dispatch({
      effects: attributesCompartment.reconfigure(EditorView.contentAttributes.of(editorAttributes(ariaLabel, id, testId)))
    })
  }, [ariaLabel, attributesCompartment, id, testId])

  useEffect(() => {
    viewRef.current?.dispatch({
      effects: placeholderCompartment.reconfigure(placeholder ? editorPlaceholder(placeholder) : [])
    })
  }, [placeholder, placeholderCompartment])

  useEffect(() => {
    viewRef.current?.dispatch({ effects: readOnlyCompartment.reconfigure(readOnlyExtensions(readOnly)) })
  }, [readOnly, readOnlyCompartment])

  useEffect(() => {
    viewRef.current?.dispatch({ effects: searchCompartment.reconfigure(codeEditorSearchHighlight(searchQuery, searchOptions)) })
  }, [searchCompartment, searchOptions, searchQuery])

  useEffect(() => {
    const view = viewRef.current
    if (!view) return
    view.dispatch({ effects: metricsCompartment.reconfigure(editorMetrics(fontFamily, fontSize)) })
    view.requestMeasure()
  }, [fontFamily, fontSize, metricsCompartment])

  useImperativeHandle(ref, () => ({
    focus: () => viewRef.current?.focus(),
    getSelection: () => {
      const selection = viewRef.current?.state.selection.main
      return selection ? { start: selection.from, end: selection.to } : { start: 0, end: 0 }
    },
    getViewState: () => viewRef.current ? readCodeEditorViewState(viewRef.current) : null,
    selectRange: (anchor, head) => {
      const view = viewRef.current
      if (!view) return
      const start = Math.max(0, Math.min(anchor, view.state.doc.length))
      const end = Math.max(start, Math.min(head, view.state.doc.length))
      view.dispatch({ selection: { anchor: start, head: end }, scrollIntoView: true })
      view.focus()
    },
    syncScroll: (scrollTop, scrollLeft) => {
      const scroller = viewRef.current?.scrollDOM
      if (!scroller) return
      scroller.scrollTop = scrollTop
      scroller.scrollLeft = scrollLeft
    }
  }), [])

  const style = {
    ...(fontFamily ? { '--text-code-editor-font-family': fontFamily } : {}),
    ...(fontSize ? { '--text-code-editor-font-size': `${fontSize}px` } : {})
  } as CSSProperties

  return <div ref={hostRef} className={`text-code-editor ${className}`.trim()} style={style} />
})
