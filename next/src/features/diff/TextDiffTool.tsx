import { ArrowLeftRight, ChevronDown, ChevronUp, Copy, History, Play, Trash2 } from 'lucide-react'
import { useEffect, useMemo, useRef, useState, type UIEvent } from 'react'
import { HistoryDialog } from '@/features/history/HistoryDialog'
import { ToolPageHeader, ToolTabs } from '@/shared/components/ToolPage'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { compareText, type DiffResult } from './diffTools'

type DiffMode = 'side' | 'unified'

export function TextDiffTool() {
  const { t } = useI18n()
  const actions = useToolActions('textDiff')
  const [left, setLeft] = useState('MooTool\nquiet desktop tools\nold line\n')
  const [right, setRight] = useState('MooTool\nquiet desktop toolkit\nnew line\n')
  const [mode, setMode] = useState<DiffMode>('side')
  const [ignoreWhitespace, setIgnoreWhitespace] = useState(false)
  const [realtime, setRealtime] = useState(true)
  const [result, setResult] = useState<DiffResult>(() => compareText(left, right, false))
  const [historyOpen, setHistoryOpen] = useState(false)
  const [navigationIndex, setNavigationIndex] = useState(-1)
  const leftEditorRef = useRef<HTMLTextAreaElement>(null)
  const rightEditorRef = useRef<HTMLTextAreaElement>(null)
  const syncingScroll = useRef(false)
  const changedIndices = useMemo(() => result.segments.map((segment, index) => segment.added || segment.removed ? index : -1).filter((index) => index >= 0), [result])

  useEffect(() => {
    if (!realtime) return
    const timer = window.setTimeout(() => setResult(compareText(left, right, ignoreWhitespace)), 160)
    return () => window.clearTimeout(timer)
  }, [ignoreWhitespace, left, realtime, right])

  function compare(): void {
    const next = compareText(left, right, ignoreWhitespace)
    setResult(next)
    void actions.saveHistory(t('diff.summary', { added: String(next.added), removed: String(next.removed), changed: String(next.changed) }), left, right, JSON.stringify({ ignoreWhitespace }))
  }

  function clear(): void { setLeft(''); setRight(''); setResult(compareText('', '', ignoreWhitespace)) }

  function syncEditorScroll(event: UIEvent<HTMLTextAreaElement>, target: HTMLTextAreaElement | null): void {
    if (!target || syncingScroll.current) return
    syncingScroll.current = true
    const source = event.currentTarget
    const maxSource = source.scrollHeight - source.clientHeight
    const maxTarget = target.scrollHeight - target.clientHeight
    target.scrollTop = maxSource > 0 ? (source.scrollTop / maxSource) * maxTarget : 0
    target.scrollLeft = source.scrollLeft
    window.requestAnimationFrame(() => { syncingScroll.current = false })
  }

  function navigateDifference(step: number): void {
    if (changedIndices.length === 0) return
    const nextNavigation = (navigationIndex + step + changedIndices.length) % changedIndices.length
    const segmentIndex = changedIndices[nextNavigation]
    setNavigationIndex(nextNavigation)
    window.requestAnimationFrame(() => document.querySelector(`[data-diff-index="${segmentIndex}"]`)?.scrollIntoView({ block: 'center' }))
  }

  return (
    <section className="tool-page p3-tool">
      <ToolPageHeader title={t('diff.title')} actions={<button className="toolbar-button" type="button" onClick={() => setHistoryOpen(true)}><History size={14} />{t('common.action.history')}</button>} />
      <div className="local-tool-shell diff-workspace">
        <div className="diff-toolbar">
          <ToolTabs tabs={[{ id: 'side', label: t('diff.sideBySide') }, { id: 'unified', label: t('diff.unified') }]} active={mode} onChange={setMode} />
          <label><input type="checkbox" checked={ignoreWhitespace} onChange={(event) => setIgnoreWhitespace(event.target.checked)} />{t('diff.ignoreWhitespace')}</label>
          <label><input type="checkbox" checked={realtime} onChange={(event) => setRealtime(event.target.checked)} />{t('diff.realtime')}</label>
          <button type="button" onClick={compare}><Play size={14} />{t('diff.compare')}</button>
          <button type="button" aria-label={t('diff.previous')} disabled={changedIndices.length === 0} onClick={() => navigateDifference(-1)}><ChevronUp size={14} /></button>
          <button type="button" aria-label={t('diff.next')} disabled={changedIndices.length === 0} onClick={() => navigateDifference(1)}><ChevronDown size={14} /></button>
          <button type="button" aria-label={t('common.action.swap')} onClick={() => { setLeft(right); setRight(left) }}><ArrowLeftRight size={14} /></button>
          <button type="button" aria-label={t('diff.copy')} onClick={() => { void actions.copy(result.unified) }}><Copy size={14} /></button>
          <button type="button" aria-label={t('common.action.clear')} onClick={clear}><Trash2 size={14} /></button>
        </div>
        <div className="diff-summary">{result.added || result.removed || result.changed ? t('diff.summary', { added: String(result.added), removed: String(result.removed), changed: String(result.changed) }) : t('diff.identical')}</div>
        {mode === 'side' ? <div className="diff-editor-grid"><label><span>{t('diff.left')}</span><textarea ref={leftEditorRef} value={left} spellCheck={false} onScroll={(event) => syncEditorScroll(event, rightEditorRef.current)} onChange={(event) => setLeft(event.target.value)} /></label><label><span>{t('diff.right')}</span><textarea ref={rightEditorRef} value={right} spellCheck={false} onScroll={(event) => syncEditorScroll(event, leftEditorRef.current)} onChange={(event) => setRight(event.target.value)} /></label><pre className="diff-preview diff-preview--left">{result.segments.map((item, index) => item.added ? null : <mark data-diff-index={item.removed ? index : undefined} className={item.removed ? 'diff-removed' : ''} key={`${index}-${item.value}`}>{item.value}</mark>)}</pre><pre className="diff-preview diff-preview--right">{result.segments.map((item, index) => item.removed ? null : <mark data-diff-index={item.added ? index : undefined} className={item.added ? 'diff-added' : ''} key={`${index}-${item.value}`}>{item.value}</mark>)}</pre></div> : <pre className="unified-diff">{result.unified}</pre>}
      </div>
      <HistoryDialog funcType="textDiff" open={historyOpen} onClose={() => setHistoryOpen(false)} onApply={(value) => setRight(value)} onApplyRecord={(record) => { setLeft(record.inputText); setRight(record.outputText) }} />
    </section>
  )
}
