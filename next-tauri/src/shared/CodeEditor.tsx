import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands'
import { bracketMatching, defaultHighlightStyle, syntaxHighlighting } from '@codemirror/language'
import { EditorState, type Extension } from '@codemirror/state'
import { openSearchPanel, search, searchKeymap } from '@codemirror/search'
import {
  drawSelection,
  EditorView,
  highlightActiveLine,
  highlightActiveLineGutter,
  keymap,
  lineNumbers
} from '@codemirror/view'
import { useEffect, useRef } from 'react'
import { useI18n } from '../app/i18n'

interface CodeEditorProps {
  ariaLabel: string
  value: string
  onChange?(value: string): void
  onReady?(view: EditorView): void
  extensions?: Extension[]
  className?: string
  readOnly?: boolean
  lineWrapping?: boolean
}

export function CodeEditor({
  ariaLabel,
  value,
  onChange,
  onReady,
  extensions = [],
  className = '',
  readOnly = false,
  lineWrapping = true
}: CodeEditorProps) {
  const { t } = useI18n()
  const hostRef = useRef<HTMLDivElement>(null)
  const viewRef = useRef<EditorView | undefined>(undefined)
  const onChangeRef = useRef(onChange)
  const onReadyRef = useRef(onReady)
  onChangeRef.current = onChange
  onReadyRef.current = onReady

  useEffect(() => {
    const host = hostRef.current
    if (!host) return
    const view = new EditorView({
      parent: host,
      state: EditorState.create({
        doc: value,
        extensions: [
          history(),
          lineNumbers(),
          drawSelection(),
          highlightActiveLine(),
          highlightActiveLineGutter(),
          bracketMatching(),
          search({ top: false }),
          syntaxHighlighting(defaultHighlightStyle, { fallback: true }),
          keymap.of([
            ...searchKeymap,
            ...defaultKeymap,
            ...historyKeymap,
            indentWithTab
          ]),
          ...(lineWrapping ? [EditorView.lineWrapping] : []),
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
          }),
          ...extensions
        ]
      })
    })
    viewRef.current = view
    onReadyRef.current?.(view)
    return () => {
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

  return (
    <div className={`shared-code-editor ${className}`.trim()} ref={hostRef}>
      <button
        className="shared-code-editor__search"
        type="button"
        tabIndex={-1}
        aria-hidden="true"
        onClick={() => viewRef.current && openSearchPanel(viewRef.current)}
      >
        {t('editor.find')}
      </button>
    </div>
  )
}
