import { ArrowLeftRight, ChevronDown, ChevronUp, Copy, History, Play, Trash2 } from 'lucide-react'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { DiffCodeEditor, type DiffEditorDecoration, type DiffEditorHandle, type DiffEditorScroll } from '../../shared/DiffCodeEditor'
import { ResizableColumns } from '../../shared/ResizableColumns'
import { HistoryPanel } from '../history/HistoryPanel'
import { useOperationHistory } from '../history/useOperationHistory'
import { useSettings } from '../settings/SettingsProvider'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import {
  compareText,
  type DiffResult,
  type DiffSegment,
  type UnifiedDiffView,
  type UnifiedSpanType
} from './diffSegments'
import { describeTextDiffSession } from './textDiffSession'
import { textDiffMessages } from './textDiffMessages'

type DiffMode = 'side' | 'unified'
type HighlightMode = 'both' | 'characters' | 'lines'
type StatusState =
  | { kind: 'ready' | 'enterText' | 'complete' | 'cleared' | 'swapped' | 'copied' | 'noCopy' }
  | { kind: 'navigation'; current: number; total: number }

const textDiffDraftKey = 'mootool:text-diff:draft'

export function TextDiffSurface() {
  const { t } = useLocalizedMessages(textDiffMessages)
  const { settings } = useSettings()
  const recordOperation = useOperationHistory('text-diff')
  const initialDraft = useMemo(readTextDiffDraft, [])
  const initialResult = useMemo(() => compareText('', '', false), [])
  const [left, setLeft] = useState(initialDraft.left)
  const [right, setRight] = useState(initialDraft.right)
  const [mode, setMode] = useState<DiffMode>('side')
  const [highlightMode, setHighlightMode] = useState<HighlightMode>('both')
  const [ignoreWhitespace, setIgnoreWhitespace] = useState(false)
  const [completed, setCompleted] = useState<DiffResult>(() => compareText(initialDraft.left, initialDraft.right, false))
  const [status, setStatus] = useState<StatusState>(initialDraft.left || initialDraft.right ? { kind: 'complete' } : { kind: 'ready' })
  const [historyOpen, setHistoryOpen] = useState(false)
  const [calculating, setCalculating] = useState(false)
  const [actionError, setActionError] = useState('')
  const leftEditorRef = useRef<DiffEditorHandle>(null)
  const rightEditorRef = useRef<DiffEditorHandle>(null)
  const syncingScroll = useRef(false)
  const navigationIndex = useRef(-1)
  const timerRef = useRef<number | undefined>(undefined)
  const highlightModeRef = useRef(highlightMode)
  highlightModeRef.current = highlightMode
  const result = completed.leftText === left && completed.rightText === right ? completed : initialResult
  const visibleSegments = useMemo(
    () => highlightMode === 'characters' ? result.segments.filter((segment) => !segment.wholeLine) : result.segments,
    [highlightMode, result.segments]
  )
  const leftDecorations = useMemo(
    () => createSideDecorations(result.leftText, visibleSegments, 'left', highlightMode),
    [highlightMode, result.leftText, visibleSegments]
  )
  const rightDecorations = useMemo(
    () => createSideDecorations(result.rightText, visibleSegments, 'right', highlightMode),
    [highlightMode, result.rightText, visibleSegments]
  )
  const unifiedDecorations = useMemo(
    () => createUnifiedDecorations(result.unifiedView, highlightMode),
    [highlightMode, result.unifiedView]
  )
  const session = useMemo(() => describeTextDiffSession({
    left,
    right,
    ignoreWhitespace,
    added: result.added,
    removed: result.removed,
    changed: result.changed
  }), [ignoreWhitespace, left, result.added, result.changed, result.removed, right])
  const { reportError } = useToolSessionReport('text-diff', session.digest, t('summary.line', {
    added: String(result.added),
    removed: String(result.removed),
    changed: String(result.changed)
  }))

  const compareNow = useCallback((saveHistory: boolean) => {
    const next = compareText(left, right, ignoreWhitespace)
    setCompleted(next)
    navigationIndex.current = -1
    setStatus(left || right ? { kind: 'complete' } : { kind: 'enterText' })
    if (saveHistory && (left || right)) {
      recordOperation(
        t('action.compare'),
        t('summary.line', { added: String(next.added), removed: String(next.removed), changed: String(next.changed) }),
        'success',
        { inputText: left, outputText: right, metadata: { ignoreWhitespace, highlightMode: highlightModeRef.current } }
      )
    }
  }, [ignoreWhitespace, left, recordOperation, right, t])

  useEffect(() => {
    setCalculating(true)
    timerRef.current = window.setTimeout(() => {
      compareNow(false)
      setCalculating(false)
    }, 160)
    return () => window.clearTimeout(timerRef.current)
  }, [compareNow])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      try {
        window.localStorage.setItem(textDiffDraftKey, JSON.stringify({ left, right }))
      } catch {
        // Draft persistence is best effort when storage is unavailable.
      }
    }, 250)
    return () => window.clearTimeout(timer)
  }, [left, right])

  function syncEditorScroll(scroll: DiffEditorScroll, target: DiffEditorHandle | null): void {
    if (!target || syncingScroll.current) return
    syncingScroll.current = true
    target.syncScroll(scroll.scrollTop, scroll.scrollLeft)
    window.requestAnimationFrame(() => { syncingScroll.current = false })
  }

  function clear(): void {
    window.clearTimeout(timerRef.current)
    setCalculating(false)
    setLeft('')
    setRight('')
    setCompleted(compareText('', '', ignoreWhitespace))
    setStatus({ kind: 'cleared' })
    navigationIndex.current = -1
  }

  function swap(): void {
    window.clearTimeout(timerRef.current)
    setCalculating(false)
    setLeft(right)
    setRight(left)
    setStatus({ kind: 'swapped' })
  }

  async function copyDiff(): Promise<void> {
    if (!result.unified) {
      setStatus({ kind: 'noCopy' })
      return
    }
    try {
      await clipboardApi.writeText(result.unified)
      setActionError('')
      setStatus({ kind: 'copied' })
    } catch {
      setActionError(t('error.clipboard'))
    }
  }

  function navigateDifference(step: number): void {
    if (visibleSegments.length === 0) return
    const nextNavigation = (navigationIndex.current + step + visibleSegments.length) % visibleSegments.length
    const segment = visibleSegments[nextNavigation]
    navigationIndex.current = nextNavigation
    window.requestAnimationFrame(() => {
      focusSegment(leftEditorRef.current, left, segment.leftStart, segment.leftEnd)
      focusSegment(rightEditorRef.current, right, segment.rightStart, segment.rightEnd)
    })
    setStatus({ kind: 'navigation', current: nextNavigation + 1, total: visibleSegments.length })
  }

  const editors = (
    <ResizableColumns id="text-diff" className="diff-editor-grid" initialPrimary={480} minPrimary={240} minSecondary={240}>
      <div className="diff-editor-pane">
        <span>{t('pane.original')}</span>
        <DiffCodeEditor
          ref={leftEditorRef}
          ariaLabel={t('pane.original')}
          className="diff-editor"
          decorations={leftDecorations}
          readOnly={false}
          value={left}
          onChange={setLeft}
          onScroll={(scroll) => syncEditorScroll(scroll, rightEditorRef.current)}
        />
      </div>
      <div className="diff-editor-pane">
        <span>{t('pane.right')}</span>
        <DiffCodeEditor
          ref={rightEditorRef}
          ariaLabel={t('pane.right')}
          className="diff-editor"
          decorations={rightDecorations}
          readOnly={false}
          value={right}
          onChange={setRight}
          onScroll={(scroll) => syncEditorScroll(scroll, leftEditorRef.current)}
        />
      </div>
    </ResizableColumns>
  )

  return (
    <main className="text-diff-workbench">
      <div className="local-tool-shell diff-workspace">
        <div className="diff-toolbar">
          <div className="diff-toolbar__commands">
            <button className="primary-execution-button" type="button" onClick={() => compareNow(true)}><Play size={14} />{t('action.compare')}</button>
            {calculating && <button type="button" onClick={() => { window.clearTimeout(timerRef.current); setCalculating(false) }}>{t('action.cancel')}</button>}
            <button type="button" onClick={clear}><Trash2 size={14} />{t('action.clearShort')}</button>
            <button type="button" onClick={swap}><ArrowLeftRight size={14} />{t('action.swap')}</button>
            <button type="button" onClick={() => void copyDiff()}><Copy size={14} />{t('action.copy')}</button>
          </div>
          <div className="diff-toolbar__navigation">
            <button type="button" disabled={visibleSegments.length === 0} onClick={() => navigateDifference(-1)}>
              <ChevronUp size={14} />{t('action.previousDifference')}
            </button>
            <button type="button" disabled={visibleSegments.length === 0} onClick={() => navigateDifference(1)}>
              <ChevronDown size={14} />{t('action.nextDifference')}
            </button>
          </div>
          <div className="diff-toolbar__options">
            <label>
              <input type="checkbox" checked={ignoreWhitespace} onChange={(event) => setIgnoreWhitespace(event.target.checked)} />
              {t('option.whitespaceDiff')}
            </label>
            <select aria-label={t('highlight.mode')} value={highlightMode} onChange={(event) => {
              setHighlightMode(event.target.value as HighlightMode)
              navigationIndex.current = -1
              setStatus(left || right ? { kind: 'complete' } : { kind: 'enterText' })
            }}>
              <option value="both">{t('highlight.both')}</option>
              <option value="characters">{t('highlight.characters')}</option>
              <option value="lines">{t('highlight.lines')}</option>
            </select>
            <select aria-label={t('display.mode')} value={mode} onChange={(event) => setMode(event.target.value as DiffMode)}>
              <option value="side">{t('display.side')}</option>
              <option value="unified">{t('display.unified')}</option>
            </select>
          </div>
          <div className="diff-toolbar__tail">
            <span className="diff-status" aria-live="polite">{calculating ? t('status.processing') : statusText(status, highlightMode, visibleSegments.length, t)}</span>
            <button type="button" onClick={() => setHistoryOpen(true)}><History size={14} />{t('action.history')}</button>
          </div>
        </div>
        <div className={mode === 'unified' ? 'diff-content diff-content--unified' : 'diff-content'}>
          {editors}
          {mode === 'unified' && (
            <div className="diff-unified-pane">
              <span>{t('unified.panel')}</span>
              <DiffCodeEditor
                ariaLabel={t('unified.panel')}
                className="unified-diff"
                decorations={unifiedDecorations}
                readOnly
                value={result.unified}
              />
            </div>
          )}
        </div>
      </div>
      {(reportError || actionError) && <p className="tool-surface-report-error">{actionError || t('report.error', { error: reportError })}</p>}
      {historyOpen && (
        <HistoryPanel
          limit={settings.data.historyLimit}
          toolId="text-diff"
          onClose={() => setHistoryOpen(false)}
          onRestore={(entry) => {
            setLeft(entry.inputText)
            setRight(entry.outputText)
            setHistoryOpen(false)
          }}
        />
      )}
    </main>
  )
}

function createSideDecorations(
  text: string,
  segments: DiffSegment[],
  side: 'left' | 'right',
  highlightMode: HighlightMode
): DiffEditorDecoration[] {
  const decorations: DiffEditorDecoration[] = []
  for (const segment of segments) {
    const from = side === 'left' ? segment.leftStart : segment.rightStart
    const to = side === 'left' ? segment.leftEnd : segment.rightEnd
    if (from < 0 || to < 0) continue
    const name = segment.type === 'insert' ? 'added' : segment.type === 'delete' ? 'removed' : 'changed'
    if (highlightMode !== 'characters' && to > from) {
      decorations.push({ type: 'line', from: lineStartAt(text, from), className: `cm-diff-line-${name}` })
    }
    if (highlightMode !== 'lines' && to > from) {
      decorations.push({ type: 'mark', from, to, className: `cm-diff-character-${name}` })
    }
  }
  return decorations
}

function createUnifiedDecorations(unifiedView: UnifiedDiffView, highlightMode: HighlightMode): DiffEditorDecoration[] {
  const decorations: DiffEditorDecoration[] = unifiedView.lineSpans.map((span) => ({
    type: 'line',
    from: lineStartAt(unifiedView.text, span.start),
    className: unifiedLineClass(span.type)
  }))
  if (highlightMode !== 'lines') {
    decorations.push(...unifiedView.characterSpans.map((span) => ({
      type: 'mark' as const,
      from: span.start,
      to: span.end,
      className: unifiedCharacterClass(span.type)
    })))
  }
  return decorations
}

function unifiedLineClass(type: UnifiedSpanType): string {
  if (type === 'add-line') return 'cm-diff-line-added'
  if (type === 'delete-line') return 'cm-diff-line-removed'
  if (type === 'hunk-line') return 'cm-diff-line-changed'
  return 'cm-diff-line-header'
}

function unifiedCharacterClass(type: UnifiedSpanType): string {
  if (type === 'add-character') return 'cm-diff-character-added'
  if (type === 'delete-character') return 'cm-diff-character-removed'
  return 'cm-diff-character-changed'
}

function focusSegment(editor: DiffEditorHandle | null, text: string, start: number, end: number): void {
  if (!editor || start < 0 || end < 0) return
  const middle = Math.floor((start + end) / 2)
  const lineStart = lineStartAt(text, middle)
  editor.selectRange(lineStart, lineStart)
}

function lineStartAt(text: string, offset: number): number {
  const position = Math.max(0, Math.min(offset, text.length))
  if (position === 0) return 0
  return text.lastIndexOf('\n', position - 1) + 1
}

function statusText(
  status: StatusState,
  highlightMode: HighlightMode,
  differenceCount: number,
  t: (key: 'status.navigation' | 'status.characterComplete' | 'status.complete' | 'status.ready' | 'status.enterText' | 'status.cleared' | 'status.swapped' | 'status.copied' | 'status.noCopy', values?: Record<string, string | number>) => string
): string {
  if (status.kind === 'navigation') return t('status.navigation', { current: String(status.current), total: String(status.total) })
  if (status.kind === 'complete') {
    return highlightMode === 'characters'
      ? t('status.characterComplete', { count: String(differenceCount) })
      : t('status.complete', { count: String(differenceCount) })
  }
  const statusKeys = {
    ready: 'status.ready',
    enterText: 'status.enterText',
    cleared: 'status.cleared',
    swapped: 'status.swapped',
    copied: 'status.copied',
    noCopy: 'status.noCopy'
  } as const
  return t(statusKeys[status.kind])
}

function readTextDiffDraft(): { left: string; right: string } {
  const fallback = {
    left: 'MooTool\nquiet desktop tools\nold line\n',
    right: 'MooTool\nquiet desktop toolkit\nnew line\n'
  }
  try {
    const value = JSON.parse(window.localStorage.getItem(textDiffDraftKey) ?? '') as unknown
    if (!value || typeof value !== 'object') return fallback
    const draft = value as Record<string, unknown>
    return {
      left: typeof draft.left === 'string' ? draft.left : fallback.left,
      right: typeof draft.right === 'string' ? draft.right : fallback.right
    }
  } catch {
    return fallback
  }
}
