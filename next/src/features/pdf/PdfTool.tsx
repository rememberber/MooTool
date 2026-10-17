import { CircleHelp, FilePlus2, Merge, Play, Split, Trash2 } from 'lucide-react'
import { useEffect, useRef, useState, type Dispatch, type SetStateAction } from 'react'
import { Dialog } from '@/shared/components/Dialog'
import { ToolPageHeader, ToolTabs } from '@/shared/components/ToolPage'
import { isPdfCancellation } from '@/shared/contracts/pdf'
import type { PdfFileInfo, PdfSplitRule, PdfProgress } from '@/shared/contracts/pdf'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useDesktopDialog } from '@/shared/feedback/DesktopDialogProvider'
import { useI18n } from '@/shared/i18n/I18nProvider'

type PdfTab = 'split' | 'merge'
type TaskStatus = 'ready' | 'running' | 'done' | 'error' | 'cancelled'
type SplitRow = PdfFileInfo & { selected: boolean; pageRange: string; rule: PdfSplitRule; customRule: string; status: TaskStatus }
type MergeRow = PdfFileInfo & { selected: boolean; pages: string; status: TaskStatus }

export function PdfTool() {
  const { t } = useI18n()
  const actions = useToolActions('pdf')
  const desktopDialog = useDesktopDialog()
  const [tab, setTab] = useState<PdfTab>('split')
  const [splitRows, setSplitRows] = useState<SplitRow[]>([])
  const [mergeRows, setMergeRows] = useState<MergeRow[]>([])
  const [busy, setBusy] = useState(false)
  const [helpOpen, setHelpOpen] = useState(false)
  const [lastOutputs, setLastOutputs] = useState<string[]>([])
  const [progress, setProgress] = useState<PdfProgress | null>(null)
  const activeJob = useRef<string | null>(null)
  const [cancelling, setCancelling] = useState(false)
  const [summary, setSummary] = useState<{ kind: 'done' | 'cancelled' | 'error'; text: string } | null>(null)
  useEffect(() => {
    const unsubscribe = window.mootool.onPdfProgress(value => {
      if (value.jobId === activeJob.current) setProgress(value)
    })
    return () => { unsubscribe(); if (activeJob.current) void window.mootool.cancelPdfJob(activeJob.current) }
  }, [])

  function startJob(): string {
    const id = crypto.randomUUID()
    activeJob.current = id
    setCancelling(false)
    setSummary(null)
    setProgress(null)
    setBusy(true)
    return id
  }

  function finishJob(): void { activeJob.current = null; setBusy(false); setCancelling(false); setProgress(null) }

  async function cancelJob(): Promise<void> {
    const id = activeJob.current
    if (!id) return
    setCancelling(true)
    try {
      const accepted = await window.mootool.cancelPdfJob(id)
      if (activeJob.current === id && !accepted) setCancelling(false)
    } catch (error) { if (activeJob.current === id) { setCancelling(false); actions.reportError(error) } }
  }

  async function addSplitFiles(): Promise<void> {
    const id = startJob()
    try {
      const files = await window.mootool.choosePdfFiles(id)
      setSplitRows((current) => appendUnique(current, files.map((file) => ({ ...file, selected: true, pageRange: `1-${file.pageCount}`, rule: 'odd' as const, customRule: '', status: 'ready' as const }))))
    } catch (error) {
      if (isPdfCancellation(error)) setSummary({ kind: 'cancelled', text: t('pdf.status.cancelled') })
      else { setSummary({ kind: 'error', text: t('pdf.status.error') }); actions.reportError(error) }
    }
    finally { finishJob() }
  }

  async function addMergeFiles(): Promise<void> {
    const id = startJob()
    try {
      const files = await window.mootool.choosePdfFiles(id)
      setMergeRows((current) => appendUnique(current, files.map((file) => ({ ...file, selected: true, pages: `1-${file.pageCount}`, status: 'ready' as const }))))
    } catch (error) {
      if (isPdfCancellation(error)) setSummary({ kind: 'cancelled', text: t('pdf.status.cancelled') })
      else { setSummary({ kind: 'error', text: t('pdf.status.error') }); actions.reportError(error) }
    }
    finally { finishJob() }
  }

  async function splitFiles(): Promise<void> {
    const selected = splitRows.filter((row) => row.selected)
    if (selected.length === 0) { actions.toast.error(t('pdf.selectTask')); return }
    if (!await desktopDialog.confirm(t('pdf.confirmSplit'))) return
    const id = startJob()
    setLastOutputs([])
    setSplitRows((rows) => rows.map((row) => row.selected ? { ...row, status: 'running' } : row))
    try {
      const result = await window.mootool.splitPdfFiles(selected.map((row) => ({ path: row.path, pageRange: row.pageRange, rule: row.rule, customRule: row.customRule })), id)
      setLastOutputs(result.outputs)
      setSplitRows((rows) => rows.map((row) => row.selected ? { ...row, status: 'done' } : row))
      setSummary({ kind: 'done', text: t('pdf.splitComplete', { count: String(result.pageCount) }) })
      actions.toast.success(t('pdf.splitComplete', { count: String(result.pageCount) }))
    } catch (error) {
      setSplitRows((rows) => rows.map((row) => row.selected ? { ...row, status: isPdfCancellation(error) ? 'cancelled' : 'error' } : row))
      setSummary({ kind: isPdfCancellation(error) ? 'cancelled' : 'error', text: t(isPdfCancellation(error) ? 'pdf.status.cancelled' : 'pdf.status.error') })
      if (!isPdfCancellation(error)) actions.reportError(error)
    } finally { finishJob() }
  }

  async function mergeFiles(): Promise<void> {
    const selected = mergeRows.filter((row) => row.selected)
    if (selected.length < 2) { actions.toast.error(t('pdf.selectTwo')); return }
    const id = startJob()
    setLastOutputs([])
    setMergeRows((rows) => rows.map((row) => row.selected ? { ...row, status: 'running' } : row))
    try {
      const result = await window.mootool.mergePdfFiles(selected.map((row) => ({ path: row.path, pages: row.pages })), id)
      if (!result) { setMergeRows(rows => rows.map(row => row.status === 'running' ? { ...row, status: 'cancelled' } : row)); setSummary({ kind: 'cancelled', text: t('pdf.status.cancelled') }); return }
      setLastOutputs(result.outputs)
      setMergeRows((rows) => rows.map((row) => row.selected ? { ...row, status: 'done' } : row))
      setSummary({ kind: 'done', text: t('pdf.mergeComplete', { count: String(result.pageCount) }) })
      actions.toast.success(t('pdf.mergeComplete', { count: String(result.pageCount) }))
    } catch (error) {
      setMergeRows((rows) => rows.map((row) => row.selected ? { ...row, status: isPdfCancellation(error) ? 'cancelled' : 'error' } : row))
      setSummary({ kind: isPdfCancellation(error) ? 'cancelled' : 'error', text: t(isPdfCancellation(error) ? 'pdf.status.cancelled' : 'pdf.status.error') })
      if (!isPdfCancellation(error)) actions.reportError(error)
    } finally { finishJob() }
  }

  return (
    <section className="tool-page p4-tool">
      <ToolPageHeader title={t('pdf.title')} />
      <div className="local-tool-shell pdf-workspace">
        <ToolTabs tabs={[{ id: 'split', label: t('pdf.tab.split') }, { id: 'merge', label: t('pdf.tab.merge') }]} active={tab} onChange={next => { if (!busy) setTab(next) }} windowDrag actions={<button className="toolbar-button toolbar-button--icon" type="button" aria-label={t('common.help')} onClick={() => setHelpOpen(true)}><CircleHelp size={15} /></button>} />
        <div className="p4-toolbar"><span className="p4-toolbar__spacer" /><button className="dialog-button" type="button" disabled={busy || (tab === 'split' ? splitRows.length : mergeRows.length) >= 20} onClick={() => { void (tab === 'split' ? addSplitFiles() : addMergeFiles()) }}><FilePlus2 size={14} />{tab === 'split' ? t('pdf.addTask') : t('pdf.addFile')}</button><button className="primary-command" type="button" disabled={busy} onClick={() => { void (tab === 'split' ? splitFiles() : mergeFiles()) }}>{tab === 'split' ? <Split size={14} /> : <Merge size={14} />}{busy ? t('common.processing') : tab === 'split' ? t('pdf.startSplit') : t('pdf.startMerge')}</button></div>
        {busy && <div className="pdf-output-strip" role="status">
          <span>{progress ? t(`pdf.phase.${progress.phase}`) : t('common.processing')}</span>
          {progress && <><progress value={progress.completed} max={Math.max(1, progress.total)} /><span>{progress.completed}/{progress.total}</span></>}
          <button type="button" className="dialog-button" disabled={cancelling || progress?.phase === 'saving'} onClick={() => { void cancelJob() }}>{t(cancelling ? 'pdf.cancelling' : 'common.cancel')}</button>
        </div>}
        {tab === 'split' ? <SplitTable busy={busy} rows={splitRows} setRows={setSplitRows} t={t} /> : <MergeTable busy={busy} rows={mergeRows} setRows={setMergeRows} t={t} />}
        {summary && <p className={`pdf-operation-summary pdf-status--${summary.kind}`} role={summary.kind === 'error' ? 'alert' : 'status'}>{summary.text}</p>}
        <div className="pdf-output-strip pdf-output-strip--results"><span>{t('pdf.output')}</span>{lastOutputs.length === 0 ? <em>—</em> : lastOutputs.map(output => <div className="pdf-output-item" key={output}>
          <code title={output}>{output}</code>
          <div className="pdf-output-item__actions">
            <button className="dialog-button" type="button" aria-label={`${t('pdf.openOutput')} ${output}`} onClick={() => { void window.mootool.actOnPdfOutput(output, 'open').catch(actions.reportError) }}>{t('pdf.openOutput')}</button>
            <button className="dialog-button" type="button" aria-label={`${t('pdf.revealOutput')} ${output}`} onClick={() => { void window.mootool.actOnPdfOutput(output, 'reveal').catch(actions.reportError) }}>{t('pdf.revealOutput')}</button>
            <button className="dialog-button" type="button" aria-label={`${t('pdf.copyOutput')} ${output}`} onClick={() => { void actions.copy(output) }}>{t('pdf.copyOutput')}</button>
          </div>
        </div>)}</div>
      </div>
      <Dialog title={tab === 'split' ? t('pdf.helpSplitTitle') : t('pdf.helpMergeTitle')} open={helpOpen} width={620} onClose={() => setHelpOpen(false)} footer={<button className="dialog-button" type="button" onClick={() => setHelpOpen(false)}>{t('common.close')}</button>}><div className="pdf-help">{(tab === 'split' ? ['pdf.help.split1', 'pdf.help.split2', 'pdf.help.split3', 'pdf.help.split4'] : ['pdf.help.merge1', 'pdf.help.merge2', 'pdf.help.merge3']).map((key) => <p key={key}>{t(key as 'pdf.help.split1')}</p>)}</div></Dialog>
    </section>
  )
}

type Translate = ReturnType<typeof useI18n>['t']

function SplitTable({ rows, setRows, t, busy }: { rows: SplitRow[]; setRows: Dispatch<SetStateAction<SplitRow[]>>; t: Translate; busy: boolean }) {
  return <div className="pdf-table-wrap" inert={busy}><table className="pdf-table"><thead><tr><th>#</th><th>{t('pdf.fileName')}</th><th>{t('pdf.pageRange')}</th><th>{t('pdf.rule')}</th><th>{t('pdf.customRule')}</th><th>{t('pdf.progress')}</th><th /></tr></thead><tbody>{rows.length === 0 ? <tr><td className="pdf-empty" colSpan={7}>{t('pdf.empty')}</td></tr> : rows.map((row) => <tr key={row.path}><td><input type="checkbox" aria-label={`${t('pdf.select')} ${row.name}`} checked={row.selected} onChange={(event) => updateRow(setRows, row.path, { selected: event.target.checked })} /></td><td><strong>{row.name}</strong><small>{row.pageCount} {t('pdf.pages')} · {formatBytes(row.size)}</small></td><td><input aria-label={`${row.name} ${t('pdf.pageRange')}`} value={row.pageRange} onChange={(event) => updateRow(setRows, row.path, { pageRange: event.target.value })} /></td><td><select aria-label={`${row.name} ${t('pdf.rule')}`} value={row.rule} onChange={(event) => updateRow(setRows, row.path, { rule: event.target.value as PdfSplitRule })}><option value="odd">{t('pdf.rule.odd')}</option><option value="even">{t('pdf.rule.even')}</option><option value="custom">{t('pdf.rule.custom')}</option></select></td><td><input aria-label={`${row.name} ${t('pdf.customRule')}`} disabled={row.rule !== 'custom'} value={row.customRule} placeholder="1-5;8;10" onChange={(event) => updateRow(setRows, row.path, { customRule: event.target.value })} /></td><td><Status value={row.status} t={t} /></td><td><button className="icon-button" type="button" aria-label={t('common.action.delete')} onClick={() => setRows((current) => current.filter((item) => item.path !== row.path))}><Trash2 size={14} /></button></td></tr>)}</tbody></table></div>
}

function MergeTable({ rows, setRows, t, busy }: { rows: MergeRow[]; setRows: Dispatch<SetStateAction<MergeRow[]>>; t: Translate; busy: boolean }) {
  return <div className="pdf-table-wrap" inert={busy}><table className="pdf-table"><thead><tr><th>#</th><th>{t('pdf.fileName')}</th><th>{t('pdf.mergeRange')}</th><th>{t('pdf.progress')}</th><th /></tr></thead><tbody>{rows.length === 0 ? <tr><td className="pdf-empty" colSpan={5}>{t('pdf.empty')}</td></tr> : rows.map((row) => <tr key={row.path}><td><input type="checkbox" aria-label={`${t('pdf.select')} ${row.name}`} checked={row.selected} onChange={(event) => updateRow(setRows, row.path, { selected: event.target.checked })} /></td><td><strong>{row.name}</strong><small>{row.pageCount} {t('pdf.pages')} · {formatBytes(row.size)}</small></td><td><input aria-label={`${row.name} ${t('pdf.mergeRange')}`} value={row.pages} onChange={(event) => updateRow(setRows, row.path, { pages: event.target.value })} /></td><td><Status value={row.status} t={t} /></td><td><button className="icon-button" type="button" aria-label={t('common.action.delete')} onClick={() => setRows((current) => current.filter((item) => item.path !== row.path))}><Trash2 size={14} /></button></td></tr>)}</tbody></table></div>
}

function Status({ value, t }: { value: TaskStatus; t: Translate }) {
  return <span className={`pdf-status pdf-status--${value}`}>{value === 'running' && <Play size={12} />}{t(`pdf.status.${value}` as 'pdf.status.ready')}</span>
}

function updateRow<Row extends { path: string }>(setter: Dispatch<SetStateAction<Row[]>>, path: string, patch: Partial<Row>): void {
  setter((rows) => rows.map((row) => row.path === path ? { ...row, ...patch } : row))
}

function appendUnique<Row extends { path: string }>(current: Row[], additions: Row[]): Row[] {
  const existing = new Set(current.map((row) => row.path))
  return [...current, ...additions.filter((row) => !existing.has(row.path))].slice(0, 20)
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}
