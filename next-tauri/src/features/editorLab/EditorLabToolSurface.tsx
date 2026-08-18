import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands'
import { json } from '@codemirror/lang-json'
import {
  bracketMatching,
  defaultHighlightStyle,
  syntaxHighlighting
} from '@codemirror/language'
import { EditorState } from '@codemirror/state'
import {
  findNext,
  findPrevious,
  openSearchPanel,
  SearchQuery,
  search,
  searchKeymap,
  setSearchQuery
} from '@codemirror/search'
import {
  drawSelection,
  EditorView,
  highlightActiveLine,
  highlightActiveLineGutter,
  keymap,
  lineNumbers
} from '@codemirror/view'
import { Braces, CaseSensitive, MousePointer2, Search, TextCursorInput } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { toolWebviewApis } from '../../platform/api/toolWebviewApi'
import {
  describeEditorLabState,
  type EditorLabSessionState
} from './editorLabSession'

const initialContent = `{
  "title": "CodeMirror P0",
  "message": "你好，世界 / こんにちは世界",
  "status": "等待 IME 输入",
  "items": [
    "selection",
    "search",
    "reparent"
  ]
}`

interface EditorMetrics {
  content: string
  anchor: number
  head: number
  line: number
  scrollTop: number
  scrollLeft: number
  changeCount: number
}

const initialMetrics: EditorMetrics = {
  content: initialContent,
  anchor: 0,
  head: 0,
  line: 1,
  scrollTop: 0,
  scrollLeft: 0,
  changeCount: 0
}

export function EditorLabToolSurface() {
  const editorHostRef = useRef<HTMLDivElement>(null)
  const editorViewRef = useRef<EditorView | undefined>(undefined)
  const sessionId = useRef(crypto.randomUUID())
  const revision = useRef(0)
  const [metrics, setMetrics] = useState(initialMetrics)
  const [searchQuery, setSearchQueryValue] = useState('世界')
  const [compositionStarts, setCompositionStarts] = useState(0)
  const [compositionEnds, setCompositionEnds] = useState(0)
  const [reportError, setReportError] = useState('')

  useEffect(() => {
    const host = editorHostRef.current
    if (!host) return

    const view = new EditorView({
      parent: host,
      state: EditorState.create({
        doc: initialContent,
        extensions: [
          history(),
          lineNumbers(),
          drawSelection(),
          highlightActiveLine(),
          highlightActiveLineGutter(),
          bracketMatching(),
          json(),
          search({ top: false }),
          syntaxHighlighting(defaultHighlightStyle, { fallback: true }),
          keymap.of([
            ...searchKeymap,
            ...defaultKeymap,
            ...historyKeymap,
            indentWithTab
          ]),
          EditorView.lineWrapping,
          EditorView.contentAttributes.of({
            'aria-label': 'CodeMirror P0 编辑器',
            spellcheck: 'false',
            autocapitalize: 'off',
            autocomplete: 'off'
          }),
          EditorView.updateListener.of((update) => {
            if (!update.docChanged && !update.selectionSet) return
            const selection = update.state.selection.main
            setMetrics((current) => ({
              ...current,
              content: update.state.doc.toString(),
              anchor: selection.anchor,
              head: selection.head,
              line: update.state.doc.lineAt(selection.head).number,
              changeCount: current.changeCount + (update.docChanged ? 1 : 0)
            }))
          })
        ]
      })
    })

    const handleScroll = () => {
      setMetrics((current) => ({
        ...current,
        scrollTop: view.scrollDOM.scrollTop,
        scrollLeft: view.scrollDOM.scrollLeft
      }))
    }
    const handleCompositionStart = () => setCompositionStarts((value) => value + 1)
    const handleCompositionEnd = () => setCompositionEnds((value) => value + 1)

    view.scrollDOM.addEventListener('scroll', handleScroll, { passive: true })
    view.contentDOM.addEventListener('compositionstart', handleCompositionStart)
    view.contentDOM.addEventListener('compositionend', handleCompositionEnd)
    editorViewRef.current = view
    view.focus()

    return () => {
      view.scrollDOM.removeEventListener('scroll', handleScroll)
      view.contentDOM.removeEventListener('compositionstart', handleCompositionStart)
      view.contentDOM.removeEventListener('compositionend', handleCompositionEnd)
      editorViewRef.current = undefined
      view.destroy()
    }
  }, [])

  const sessionState = useMemo<EditorLabSessionState>(() => ({
    ...metrics,
    searchQuery,
    compositionStarts,
    compositionEnds
  }), [compositionEnds, compositionStarts, metrics, searchQuery])

  const description = useMemo(
    () => describeEditorLabState(sessionState),
    [sessionState]
  )

  useEffect(() => {
    revision.current += 1
    void toolWebviewApis['editor-lab'].report({
      sessionId: sessionId.current,
      stateRevision: revision.current,
      stateDigest: description.digest,
      stateSummary: description.summary
    }).then(() => setReportError('')).catch((cause: unknown) => {
      setReportError(cause instanceof Error ? cause.message : String(cause))
    })
  }, [description])

  function applySearch(direction: 'next' | 'previous'): void {
    const view = editorViewRef.current
    if (!view || !searchQuery) return
    view.dispatch({
      effects: setSearchQuery.of(new SearchQuery({
        search: searchQuery,
        caseSensitive: false,
        literal: true
      }))
    })
    if (direction === 'next') {
      findNext(view)
    } else {
      findPrevious(view)
    }
    view.focus()
  }

  function openFindPanel(): void {
    const view = editorViewRef.current
    if (!view) return
    view.dispatch({
      effects: setSearchQuery.of(new SearchQuery({
        search: searchQuery,
        caseSensitive: false,
        literal: true
      }))
    })
    openSearchPanel(view)
  }

  function selectUnicodeSample(): void {
    const view = editorViewRef.current
    if (!view) return
    const content = view.state.doc.toString()
    const start = content.indexOf('你好，世界')
    if (start < 0) return
    view.dispatch({
      selection: { anchor: start, head: start + '你好，世界'.length },
      scrollIntoView: true
    })
    view.focus()
  }

  function appendMarker(): void {
    const view = editorViewRef.current
    if (!view) return
    const marker = `\n// P0 ${metrics.changeCount + 1}: 状态保持`
    const from = view.state.doc.length
    view.dispatch({
      changes: { from, insert: marker },
      selection: { anchor: from + marker.length },
      scrollIntoView: true
    })
    view.focus()
  }

  return (
    <main className="editor-lab-surface">
      <header className="editor-lab-surface__header">
        <div>
          <span className="eyebrow">SYSTEM WEBVIEW EDITOR PROBE</span>
          <h1>CodeMirror P0</h1>
        </div>
        <span className="editor-lab-surface__session">
          会话 <code>{sessionId.current}</code>
        </span>
      </header>

      <section className="editor-lab-controls" aria-label="编辑器验证操作">
        <label>
          <Search />
          <span className="sr-only">查找内容</span>
          <input
            aria-label="查找内容"
            value={searchQuery}
            onChange={(event) => setSearchQueryValue(event.target.value)}
          />
        </label>
        <button className="secondary-button" type="button" onClick={() => applySearch('previous')}>
          上一个
        </button>
        <button className="secondary-button" type="button" onClick={() => applySearch('next')}>
          下一个
        </button>
        <button className="secondary-button" type="button" onClick={openFindPanel}>
          <CaseSensitive />原生查找
        </button>
        <button className="secondary-button" type="button" onClick={selectUnicodeSample}>
          <MousePointer2 />选中中文
        </button>
        <button className="primary-button" type="button" onClick={appendMarker}>
          <TextCursorInput />追加标记
        </button>
      </section>

      <section className="editor-lab-metrics" aria-label="CodeMirror 状态">
        <span><Braces />{metrics.content.length} 字符</span>
        <span>选区 {Math.min(metrics.anchor, metrics.head)}:{Math.max(metrics.anchor, metrics.head)}</span>
        <span>第 {metrics.line} 行</span>
        <span>变更 {metrics.changeCount}</span>
        <span>IME {compositionStarts}/{compositionEnds}</span>
      </section>

      <section className="editor-lab-editor-card">
        <div ref={editorHostRef} className="editor-lab-editor" />
      </section>

      <footer className="editor-lab-surface__footer">
        <span>⌘F 查找 · ⌘Z 撤销 · Tab 缩进</span>
        <code>{description.summary}</code>
      </footer>

      {reportError && (
        <p className="tool-surface-report-error">
          CodeMirror 状态上报失败：{reportError}
        </p>
      )}
    </main>
  )
}
