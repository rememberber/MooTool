import {
  ArrowLeftRight,
  ChevronDown,
  ChevronUp,
  Copy,
  History,
  Play,
  Trash2
} from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { HistoryDialog } from '@/features/history/HistoryDialog'
import { ToolPageHeader } from '@/shared/components/ToolPage'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import {
  TextCodeEditor,
  type TextCodeEditorDecoration,
  type TextCodeEditorHandle,
  type TextCodeEditorScroll
} from '@/shared/components/TextCodeEditor'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'
import type { MessageKey } from '@/shared/i18n/messages'
import {
  compareText,
  type DiffResult,
  type DiffSegment,
  type UnifiedDiffView,
  type UnifiedSpanType
} from './diffTools'

type DiffMode = 'side' | 'unified'
type HighlightMode = 'both' | 'characters' | 'lines'
type StatusState =
  | { kind: 'ready' | 'enterText' | 'complete' | 'cleared' | 'swapped' | 'copied' | 'noCopy' }
  | { kind: 'navigation'; current: number; total: number }
type ComparisonState = { result: DiffResult; status: StatusState }

const textDiffDraftKey = 'mootool:text-diff:draft'

export function TextDiffTool() {
  const { t } = useI18n()
  const actions = useToolActions('textDiff')
  const initialDraft = useMemo(readTextDiffDraft, [])
  const initialResult = useMemo(
    () => compareText(initialDraft.left, initialDraft.right, false),
    [initialDraft.left, initialDraft.right]
  )
  const [left, setLeft] = useState(initialDraft.left)
  const [right, setRight] = useState(initialDraft.right)
  const [mode, setMode] = useState<DiffMode>('side')
  const [highlightMode, setHighlightMode] = useState<HighlightMode>('both')
  const [ignoreWhitespace, setIgnoreWhitespace] = useState(false)
  const [realtime, setRealtime] = useState(true)
  const [comparison, setComparison] = useState<ComparisonState>({
    result: initialResult,
    status: initialDraft.left || initialDraft.right ? { kind: 'complete' } : { kind: 'ready' }
  })
  const [historyOpen, setHistoryOpen] = useState(false)
  const leftEditorRef = useRef<TextCodeEditorHandle>(null)
  const rightEditorRef = useRef<TextCodeEditorHandle>(null)
  const syncingScroll = useRef(false)
  const navigationIndex = useRef(-1)
  const { result, status } = comparison

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

  useEffect(() => {
    if (!realtime) return
    const timer = window.setTimeout(() => {
      const next = compareText(left, right, ignoreWhitespace)
      setComparison({
        result: next,
        status: left || right ? { kind: 'complete' } : { kind: 'enterText' }
      })
      navigationIndex.current = -1
    }, 160)
    return () => window.clearTimeout(timer)
  }, [ignoreWhitespace, left, realtime, right])

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

  function applyComparison(next: DiffResult, nextStatus: StatusState = { kind: 'complete' }): void {
    setComparison({
      result: next,
      status: next.leftText || next.rightText ? nextStatus : { kind: 'enterText' }
    })
    navigationIndex.current = -1
  }

  function updateStatus(nextStatus: StatusState): void {
    setComparison((current) => ({ ...current, status: nextStatus }))
  }

  function compare(): void {
    const next = compareText(left, right, ignoreWhitespace)
    applyComparison(next)
    if (!left && !right) return
    void actions.saveHistory(
      t('diff.summary', { added: String(next.added), removed: String(next.removed), changed: String(next.changed) }),
      left,
      right,
      JSON.stringify({ ignoreWhitespace, highlightMode })
    )
  }

  function clear(): void {
    setLeft('')
    setRight('')
    setComparison({ result: compareText('', '', ignoreWhitespace), status: { kind: 'cleared' } })
    navigationIndex.current = -1
  }

  function swap(): void {
    const next = compareText(right, left, ignoreWhitespace)
    setLeft(right)
    setRight(left)
    applyComparison(next, { kind: 'swapped' })
  }

  async function copyDiff(): Promise<void> {
    if (!result.unified) {
      updateStatus({ kind: 'noCopy' })
      return
    }
    await actions.copy(result.unified)
    updateStatus({ kind: 'copied' })
  }

  function syncEditorScroll(scroll: TextCodeEditorScroll, target: TextCodeEditorHandle | null): void {
    if (!target || syncingScroll.current) return
    syncingScroll.current = true
    target.syncScroll(scroll.scrollTop, scroll.scrollLeft)
    window.requestAnimationFrame(() => { syncingScroll.current = false })
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
    updateStatus({ kind: 'navigation', current: nextNavigation + 1, total: visibleSegments.length })
  }

  function changeIgnoreWhitespace(checked: boolean): void {
    setIgnoreWhitespace(checked)
    applyComparison(compareText(left, right, checked))
  }

  function changeRealtime(checked: boolean): void {
    setRealtime(checked)
    if (checked) applyComparison(compareText(left, right, ignoreWhitespace))
  }

  function changeDisplayMode(nextMode: DiffMode): void {
    setMode(nextMode)
    if (left || right) applyComparison(compareText(left, right, ignoreWhitespace))
  }

  const editors = (
    <ResizableColumns
      className="diff-editor-grid"
      columns={2}
      defaultSizes={[1, 1]}
      minPaneWidths={[240, 240]}
      storageKey="text-diff"
    >
      <div className="diff-editor-pane">
        <span>{t('diff.left')}</span>
        <TextCodeEditor
          ref={leftEditorRef}
          ariaLabel={t('diff.left')}
          className="diff-editor"
          decorations={leftDecorations}
          value={left}
          wrap={false}
          onScroll={(scroll) => syncEditorScroll(scroll, rightEditorRef.current)}
          onChange={setLeft}
        />
      </div>
      <div className="diff-editor-pane">
        <span>{t('diff.right')}</span>
        <TextCodeEditor
          ref={rightEditorRef}
          ariaLabel={t('diff.right')}
          className="diff-editor"
          decorations={rightDecorations}
          value={right}
          wrap={false}
          onScroll={(scroll) => syncEditorScroll(scroll, leftEditorRef.current)}
          onChange={setRight}
        />
      </div>
    </ResizableColumns>
  )

  return (
    <section className="tool-page p3-tool">
      <ToolPageHeader
        title={t('diff.title')}
        actions={
          <button className="toolbar-button" type="button" onClick={() => setHistoryOpen(true)}>
            <History size={14} />{t('common.action.history')}
          </button>
        }
      />
      <div className="local-tool-shell diff-workspace">
        <div className="diff-toolbar">
          <button type="button" onClick={compare}><Play size={14} />{t('diff.compare')}</button>
          <button type="button" onClick={clear}><Trash2 size={14} />{t('common.action.clear')}</button>
          <button type="button" onClick={swap}><ArrowLeftRight size={14} />{t('common.action.swap')}</button>
          <button type="button" onClick={() => { void copyDiff() }}><Copy size={14} />{t('diff.copy')}</button>
          <button type="button" disabled={visibleSegments.length === 0} onClick={() => navigateDifference(-1)}>
            <ChevronUp size={14} />{t('diff.previous')}
          </button>
          <button type="button" disabled={visibleSegments.length === 0} onClick={() => navigateDifference(1)}>
            <ChevronDown size={14} />{t('diff.next')}
          </button>
          <label>
            <input type="checkbox" checked={realtime} onChange={(event) => changeRealtime(event.target.checked)} />
            {t('diff.realtime')}
          </label>
          <label>
            <input type="checkbox" checked={ignoreWhitespace} onChange={(event) => changeIgnoreWhitespace(event.target.checked)} />
            {t('diff.ignoreWhitespace')}
          </label>
          <select
            aria-label={t('diff.highlightMode')}
            value={highlightMode}
            onChange={(event) => {
              setHighlightMode(event.target.value as HighlightMode)
              navigationIndex.current = -1
              updateStatus(left || right ? { kind: 'complete' } : { kind: 'enterText' })
            }}
          >
            <option value="both">{t('diff.highlightBoth')}</option>
            <option value="characters">{t('diff.highlightCharacters')}</option>
            <option value="lines">{t('diff.highlightLines')}</option>
          </select>
          <select
            aria-label={t('diff.displayMode')}
            value={mode}
            onChange={(event) => changeDisplayMode(event.target.value as DiffMode)}
          >
            <option value="side">{t('diff.sideBySide')}</option>
            <option value="unified">{t('diff.unified')}</option>
          </select>
          <span className="diff-status" aria-live="polite">
            {statusText(status, highlightMode, visibleSegments.length, t)}
          </span>
        </div>
        <div className={mode === 'unified' ? 'diff-content diff-content--unified' : 'diff-content'}>
          {editors}
          {mode === 'unified' && (
            <div className="diff-unified-pane">
              <span>{t('diff.unifiedPanel')}</span>
              <TextCodeEditor
                ariaLabel={t('diff.unifiedPanel')}
                className="unified-diff"
                decorations={unifiedDecorations}
                value={result.unified}
                readOnly
                wrap={false}
              />
            </div>
          )}
        </div>
      </div>
      <HistoryDialog
        funcType="textDiff"
        open={historyOpen}
        onClose={() => setHistoryOpen(false)}
        onApply={setRight}
        onApplyRecord={(record) => {
          setLeft(record.inputText)
          setRight(record.outputText)
        }}
      />
    </section>
  )
}

function createSideDecorations(
  text: string,
  segments: DiffSegment[],
  side: 'left' | 'right',
  highlightMode: HighlightMode
): TextCodeEditorDecoration[] {
  const decorations: TextCodeEditorDecoration[] = []
  for (const segment of segments) {
    const from = side === 'left' ? segment.leftStart : segment.rightStart
    const to = side === 'left' ? segment.leftEnd : segment.rightEnd
    if (from < 0 || to < 0) continue
    const name = segment.type === 'insert' ? 'added' : segment.type === 'delete' ? 'removed' : 'changed'
    if (highlightMode !== 'characters' && to > from) {
      decorations.push({
        type: 'line',
        from: lineStartAt(text, from),
        className: `cm-diff-line-${name}`
      })
    }
    if (highlightMode !== 'lines' && to > from) {
      decorations.push({ type: 'mark', from, to, className: `cm-diff-character-${name}` })
    }
  }
  return decorations
}

function createUnifiedDecorations(
  unifiedView: UnifiedDiffView,
  highlightMode: HighlightMode
): TextCodeEditorDecoration[] {
  const decorations: TextCodeEditorDecoration[] = unifiedView.lineSpans.map((span) => ({
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

function focusSegment(
  editor: TextCodeEditorHandle | null,
  text: string,
  start: number,
  end: number
): void {
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
  t: (key: MessageKey, parameters?: Record<string, string>) => string
): string {
  if (status.kind === 'navigation') {
    return t('diff.status.navigation', { current: String(status.current), total: String(status.total) })
  }
  if (status.kind === 'complete') {
    return highlightMode === 'characters'
      ? t('diff.status.characterComplete', { count: String(differenceCount) })
      : t('diff.status.complete', { count: String(differenceCount) })
  }
  const statusKeys: Record<Exclude<StatusState['kind'], 'navigation' | 'complete'>, MessageKey> = {
    ready: 'diff.status.ready',
    enterText: 'diff.status.enterText',
    cleared: 'diff.status.cleared',
    swapped: 'diff.status.swapped',
    copied: 'diff.status.copied',
    noCopy: 'diff.status.noCopy'
  }
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
