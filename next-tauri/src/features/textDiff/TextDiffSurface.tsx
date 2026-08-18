import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands'
import { EditorState } from '@codemirror/state'
import { openSearchPanel, search, searchKeymap } from '@codemirror/search'
import {
  drawSelection,
  EditorView,
  highlightActiveLine,
  keymap,
  lineNumbers
} from '@codemirror/view'
import {
  ArrowLeftRight,
  CheckCircle2,
  Clipboard,
  Copy,
  Eraser,
  FileDiff,
  Search,
  Sparkles
} from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import {
  collapseUnchangedRows,
  compareText,
  createUnifiedDiff,
  limitRenderedRows,
  type DiffRow,
  type DiffSide,
  type TextDiffOptions
} from './textDiff'
import { describeTextDiffSession } from './textDiffSession'
import { textDiffMessages } from './textDiffMessages'

export function TextDiffSurface() {
  const { t } = useLocalizedMessages(textDiffMessages)
  const samples = useRef({ left: t('sample.left'), right: t('sample.right') })
  const leftEditor = useRef<EditorView | undefined>(undefined)
  const rightEditor = useRef<EditorView | undefined>(undefined)
  const [left, setLeft] = useState(samples.current.left)
  const [right, setRight] = useState(samples.current.right)
  const [options, setOptions] = useState<TextDiffOptions>({
    ignoreCase: false,
    ignoreWhitespace: false
  })
  const [context, setContext] = useState(3)
  const [copied, setCopied] = useState(false)
  const [actionError, setActionError] = useState('')

  const result = useMemo(() => compareText(left, right, options), [left, options, right])
  const visibleRows = useMemo(
    () => limitRenderedRows(collapseUnchangedRows(result.rows, context)),
    [context, result.rows]
  )
  const session = useMemo(() => describeTextDiffSession({
    left,
    right,
    options,
    context,
    result
  }), [context, left, options, result, right])
  const sessionSummary = t('session.summary', session.stats)
  const { sessionId, reportError } = useToolSessionReport(
    'text-diff',
    session.digest,
    sessionSummary
  )

  async function copyUnifiedDiff(): Promise<void> {
    try {
      await clipboardApi.writeText(createUnifiedDiff('before', 'after', result))
      setCopied(true)
      setActionError('')
      window.setTimeout(() => setCopied(false), 1200)
    } catch {
      setActionError(t('error.clipboard'))
    }
  }

  function swap(): void {
    setLeft(right)
    setRight(left)
  }

  return (
    <main className="text-diff-workbench">
      <header className="text-diff-header">
        <div>
          <span className="eyebrow">TAURI TEXT DIFF</span>
          <h1>{t('title')}</h1>
        </div>
        <span className="text-diff-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>

      <section className="text-diff-toolbar" aria-label={t('aria.options')}>
        <button
          className="secondary-button"
          type="button"
          onClick={() => {
            setLeft(samples.current.left)
            setRight(samples.current.right)
          }}
        >
          <Sparkles />{t('action.sample')}
        </button>
        <button className="secondary-button" type="button" onClick={swap}>
          <ArrowLeftRight />{t('action.swap')}
        </button>
        <button
          className="secondary-button"
          type="button"
          onClick={() => {
            openSearchPanel(leftEditor.current!)
            openSearchPanel(rightEditor.current!)
          }}
        >
          <Search />{t('action.find')}
        </button>
        <label>
          <input
            type="checkbox"
            checked={options.ignoreWhitespace}
            onChange={(event) => setOptions((current) => ({
              ...current,
              ignoreWhitespace: event.target.checked
            }))}
          />
          {t('option.whitespace')}
        </label>
        <label>
          <input
            type="checkbox"
            checked={options.ignoreCase}
            onChange={(event) => setOptions((current) => ({
              ...current,
              ignoreCase: event.target.checked
            }))}
          />
          {t('option.case')}
        </label>
        <label className="text-diff-context">
          {t('option.context')}
          <select value={context} onChange={(event) => setContext(Number(event.target.value))}>
            {[0, 1, 3, 5].map((count) => <option value={count} key={count}>{t('unit.lines', { count })}</option>)}
            <option value={-1}>{t('option.all')}</option>
          </select>
        </label>
        <button className="secondary-button" type="button" onClick={() => void copyUnifiedDiff()}>
          {copied ? <Clipboard /> : <Copy />}{copied ? t('action.copied') : t('action.copyDiff')}
        </button>
        <button
          className="icon-button text-diff-clear"
          type="button"
          aria-label={t('action.clear')}
          onClick={() => {
            setLeft('')
            setRight('')
          }}
        >
          <Eraser />
        </button>
      </section>

      <section className="text-diff-editors">
        <EditorPane
          label={t('pane.original')}
          value={left}
          onChange={setLeft}
          onReady={(view) => {
            leftEditor.current = view
          }}
        />
        <EditorPane
          label={t('pane.target')}
          value={right}
          onChange={setRight}
          onReady={(view) => {
            rightEditor.current = view
          }}
        />
      </section>

      <section className="text-diff-summary" aria-label={t('aria.summary')}>
        <span className={result.identical ? 'text-diff-identical' : ''}>
          {result.identical ? <CheckCircle2 /> : <FileDiff />}
          {result.identical ? t('summary.identical') : t('summary.different')}
        </span>
        <span><i className="diff-dot diff-dot--changed" />{t('summary.changed')} <strong>{result.stats.changed}</strong></span>
        <span><i className="diff-dot diff-dot--added" />{t('summary.added')} <strong>{result.stats.added}</strong></span>
        <span><i className="diff-dot diff-dot--removed" />{t('summary.removed')} <strong>{result.stats.removed}</strong></span>
        <span>{t('summary.unchanged')} <strong>{result.stats.unchanged}</strong></span>
      </section>

      <section className="text-diff-result" aria-label={t('aria.result')}>
        <div className="text-diff-result__heading">
          <span>{t('pane.original')}</span>
          <span>{t('pane.target')}</span>
        </div>
        <div className="text-diff-rows">
          {visibleRows.map((row, index) => row.kind === 'collapsed'
            ? (
                <div className="text-diff-collapsed" key={`collapsed-${index}`}>
                  {row.reason === 'limit'
                    ? t('result.limit', { count: row.hiddenRows })
                    : t('result.collapsed', { count: row.hiddenRows })}
                </div>
              )
            : <DiffResultRow row={row} key={`${row.left?.lineNumber ?? '-'}:${row.right?.lineNumber ?? '-'}`} />)}
          {visibleRows.length === 0 && (
            <div className="text-diff-empty">{t('result.empty')}</div>
          )}
        </div>
      </section>

      <footer className="text-diff-footer">
        <span>{t('footer.capabilities')}</span>
        <code>{sessionSummary}</code>
      </footer>

      {(reportError || actionError) && (
        <p className="tool-surface-report-error">
          {actionError || t('report.error', { error: reportError })}
        </p>
      )}
    </main>
  )
}

function EditorPane({ label, value, onChange, onReady }: {
  label: string
  value: string
  onChange(value: string): void
  onReady(view: EditorView): void
}) {
  const { t } = useLocalizedMessages(textDiffMessages)
  const hostRef = useRef<HTMLDivElement>(null)
  const viewRef = useRef<EditorView | undefined>(undefined)
  const onChangeRef = useRef(onChange)
  const onReadyRef = useRef(onReady)
  onChangeRef.current = onChange
  onReadyRef.current = onReady

  useEffect(() => {
    if (!hostRef.current) return
    const view = new EditorView({
      parent: hostRef.current,
      state: EditorState.create({
        doc: value,
        extensions: [
          history(),
          lineNumbers(),
          drawSelection(),
          highlightActiveLine(),
          search({ top: false }),
          EditorView.lineWrapping,
          keymap.of([
            ...searchKeymap,
            ...defaultKeymap,
            ...historyKeymap,
            indentWithTab
          ]),
          EditorView.contentAttributes.of({
            'aria-label': label,
            spellcheck: 'false',
            autocapitalize: 'off',
            autocomplete: 'off'
          }),
          EditorView.updateListener.of((update) => {
            if (update.docChanged) onChangeRef.current(update.state.doc.toString())
          })
        ]
      })
    })
    viewRef.current = view
    onReadyRef.current(view)
    return () => {
      viewRef.current = undefined
      view.destroy()
    }
  }, [label])

  useEffect(() => {
    const view = viewRef.current
    if (!view || view.state.doc.toString() === value) return
    view.dispatch({
      changes: { from: 0, to: view.state.doc.length, insert: value }
    })
  }, [value])

  return (
    <article className="text-diff-editor-pane">
      <header>{label}<span>{t('pane.characters', { count: value.length })}</span></header>
      <div ref={hostRef} className="text-diff-editor" />
    </article>
  )
}

function DiffResultRow({ row }: { row: DiffRow }) {
  const { t } = useLocalizedMessages(textDiffMessages)
  return (
    <div className={`text-diff-row text-diff-row--${row.kind}`}>
      <DiffCell side={row.left} emptyLabel={row.kind === 'added' ? t('row.added') : ''} />
      <DiffCell side={row.right} emptyLabel={row.kind === 'removed' ? t('row.removed') : ''} />
    </div>
  )
}

function DiffCell({ side, emptyLabel }: { side?: DiffSide; emptyLabel: string }) {
  if (!side) {
    return <div className="text-diff-cell text-diff-cell--empty"><span /><em>{emptyLabel}</em></div>
  }
  return (
    <div className="text-diff-cell">
      <span>{side.lineNumber}</span>
      <code>
        {side.segments.map((segment, index) => (
          <mark className={segment.kind === 'changed' ? 'text-diff-inline-change' : ''} key={index}>
            {segment.text}
          </mark>
        ))}
      </code>
    </div>
  )
}
