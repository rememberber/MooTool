import { CircleHelp, FilePlus2, Merge, Play, Split, Trash2 } from 'lucide-react'
import { useState, type Dispatch, type SetStateAction } from 'react'
import { Dialog } from '@/shared/components/Dialog'
import { ToolPageHeader, ToolTabs } from '@/shared/components/ToolPage'
import type { PdfFileInfo, PdfSplitRule } from '@/shared/contracts/pdf'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'

type PdfTab = 'split' | 'merge'
type TaskStatus = 'ready' | 'running' | 'done' | 'error'
type SplitRow = PdfFileInfo & { selected: boolean; pageRange: string; rule: PdfSplitRule; customRule: string; status: TaskStatus }
type MergeRow = PdfFileInfo & { selected: boolean; pages: string; status: TaskStatus }

export function PdfTool() {
  const { t } = useI18n()
  const actions = useToolActions('pdf')
  const [tab, setTab] = useState<PdfTab>('split')
  const [splitRows, setSplitRows] = useState<SplitRow[]>([])
  const [mergeRows, setMergeRows] = useState<MergeRow[]>([])
  const [busy, setBusy] = useState(false)
  const [helpOpen, setHelpOpen] = useState(false)
  const [lastOutputs, setLastOutputs] = useState<string[]>([])

  async function addSplitFiles(): Promise<void> {
    try {
      const files = await window.mootool.choosePdfFiles()
      setSplitRows((current) => appendUnique(current, files.map((file) => ({ ...file, selected: true, pageRange: `1-${file.pageCount}`, rule: 'odd' as const, customRule: '', status: 'ready' as const }))))
    } catch (error) { actions.reportError(error) }
  }

  async function addMergeFiles(): Promise<void> {
    try {
      const files = await window.mootool.choosePdfFiles()
      setMergeRows((current) => appendUnique(current, files.map((file) => ({ ...file, selected: true, pages: `1-${file.pageCount}`, status: 'ready' as const }))))
    } catch (error) { actions.reportError(error) }
  }

  async function splitFiles(): Promise<void> {
    const selected = splitRows.filter((row) => row.selected)
    if (selected.length === 0) { actions.toast.error(t('pdf.selectTask')); return }
    if (!window.confirm(t('pdf.confirmSplit'))) return
    setBusy(true)
    setSplitRows((rows) => rows.map((row) => row.selected ? { ...row, status: 'running' } : row))
    try {
      const result = await window.mootool.splitPdfFiles(selected.map((row) => ({ path: row.path, pageRange: row.pageRange, rule: row.rule, customRule: row.customRule })))
      setLastOutputs(result.outputs)
      setSplitRows((rows) => rows.map((row) => row.selected ? { ...row, status: 'done' } : row))
      actions.toast.success(t('pdf.splitComplete', { count: String(result.pageCount) }))
    } catch (error) {
      setSplitRows((rows) => rows.map((row) => row.selected ? { ...row, status: 'error' } : row))
      actions.reportError(error)
    } finally { setBusy(false) }
  }

  async function mergeFiles(): Promise<void> {
    const selected = mergeRows.filter((row) => row.selected)
    if (selected.length < 2) { actions.toast.error(t('pdf.selectTwo')); return }
    setBusy(true)
    setMergeRows((rows) => rows.map((row) => row.selected ? { ...row, status: 'running' } : row))
    try {
      const result = await window.mootool.mergePdfFiles(selected.map((row) => ({ path: row.path, pages: row.pages })))
      if (!result) { setMergeRows((rows) => rows.map((row) => row.status === 'running' ? { ...row, status: 'ready' } : row)); return }
      setLastOutputs(result.outputs)
      setMergeRows((rows) => rows.map((row) => row.selected ? { ...row, status: 'done' } : row))
      actions.toast.success(t('pdf.mergeComplete', { count: String(result.pageCount) }))
    } catch (error) {
      setMergeRows((rows) => rows.map((row) => row.selected ? { ...row, status: 'error' } : row))
      actions.reportError(error)
    } finally { setBusy(false) }
  }

  return (
    <section className="tool-page p4-tool">
      <ToolPageHeader title={t('pdf.title')} actions={<button className="toolbar-button toolbar-button--icon" type="button" aria-label={t('common.help')} onClick={() => setHelpOpen(true)}><CircleHelp size={15} /></button>} />
      <div className="local-tool-shell pdf-workspace">
        <ToolTabs tabs={[{ id: 'split', label: t('pdf.tab.split') }, { id: 'merge', label: t('pdf.tab.merge') }]} active={tab} onChange={setTab} />
        <div className="p4-toolbar"><span className="p4-toolbar__spacer" /><button className="dialog-button" type="button" disabled={busy || (tab === 'split' ? splitRows.length : mergeRows.length) >= 20} onClick={() => { void (tab === 'split' ? addSplitFiles() : addMergeFiles()) }}><FilePlus2 size={14} />{tab === 'split' ? t('pdf.addTask') : t('pdf.addFile')}</button><button className="primary-command" type="button" disabled={busy} onClick={() => { void (tab === 'split' ? splitFiles() : mergeFiles()) }}>{tab === 'split' ? <Split size={14} /> : <Merge size={14} />}{busy ? t('common.processing') : tab === 'split' ? t('pdf.startSplit') : t('pdf.startMerge')}</button></div>
        {tab === 'split' ? <SplitTable rows={splitRows} setRows={setSplitRows} t={t} /> : <MergeTable rows={mergeRows} setRows={setMergeRows} t={t} />}
        <div className="pdf-output-strip"><span>{t('pdf.output')}</span>{lastOutputs.length === 0 ? <em>—</em> : lastOutputs.map((output) => <code key={output}>{output}</code>)}</div>
      </div>
      <Dialog title={tab === 'split' ? t('pdf.helpSplitTitle') : t('pdf.helpMergeTitle')} open={helpOpen} width={620} onClose={() => setHelpOpen(false)} footer={<button className="dialog-button" type="button" onClick={() => setHelpOpen(false)}>{t('common.close')}</button>}><div className="pdf-help">{(tab === 'split' ? ['pdf.help.split1', 'pdf.help.split2', 'pdf.help.split3', 'pdf.help.split4'] : ['pdf.help.merge1', 'pdf.help.merge2', 'pdf.help.merge3']).map((key) => <p key={key}>{t(key as 'pdf.help.split1')}</p>)}</div></Dialog>
    </section>
  )
}

type Translate = ReturnType<typeof useI18n>['t']

function SplitTable({ rows, setRows, t }: { rows: SplitRow[]; setRows: Dispatch<SetStateAction<SplitRow[]>>; t: Translate }) {
  return <div className="pdf-table-wrap"><table className="pdf-table"><thead><tr><th>#</th><th>{t('pdf.fileName')}</th><th>{t('pdf.pageRange')}</th><th>{t('pdf.rule')}</th><th>{t('pdf.customRule')}</th><th>{t('pdf.progress')}</th><th /></tr></thead><tbody>{rows.length === 0 ? <tr><td className="pdf-empty" colSpan={7}>{t('pdf.empty')}</td></tr> : rows.map((row) => <tr key={row.path}><td><input type="checkbox" aria-label={`${t('pdf.select')} ${row.name}`} checked={row.selected} onChange={(event) => updateRow(setRows, row.path, { selected: event.target.checked })} /></td><td><strong>{row.name}</strong><small>{row.pageCount} {t('pdf.pages')} · {formatBytes(row.size)}</small></td><td><input aria-label={`${row.name} ${t('pdf.pageRange')}`} value={row.pageRange} onChange={(event) => updateRow(setRows, row.path, { pageRange: event.target.value })} /></td><td><select aria-label={`${row.name} ${t('pdf.rule')}`} value={row.rule} onChange={(event) => updateRow(setRows, row.path, { rule: event.target.value as PdfSplitRule })}><option value="odd">{t('pdf.rule.odd')}</option><option value="even">{t('pdf.rule.even')}</option><option value="custom">{t('pdf.rule.custom')}</option></select></td><td><input aria-label={`${row.name} ${t('pdf.customRule')}`} disabled={row.rule !== 'custom'} value={row.customRule} placeholder="1-5;8;10" onChange={(event) => updateRow(setRows, row.path, { customRule: event.target.value })} /></td><td><Status value={row.status} t={t} /></td><td><button className="icon-button" type="button" aria-label={t('common.action.delete')} onClick={() => setRows((current) => current.filter((item) => item.path !== row.path))}><Trash2 size={14} /></button></td></tr>)}</tbody></table></div>
}

function MergeTable({ rows, setRows, t }: { rows: MergeRow[]; setRows: Dispatch<SetStateAction<MergeRow[]>>; t: Translate }) {
  return <div className="pdf-table-wrap"><table className="pdf-table"><thead><tr><th>#</th><th>{t('pdf.fileName')}</th><th>{t('pdf.mergeRange')}</th><th>{t('pdf.progress')}</th><th /></tr></thead><tbody>{rows.length === 0 ? <tr><td className="pdf-empty" colSpan={5}>{t('pdf.empty')}</td></tr> : rows.map((row) => <tr key={row.path}><td><input type="checkbox" aria-label={`${t('pdf.select')} ${row.name}`} checked={row.selected} onChange={(event) => updateRow(setRows, row.path, { selected: event.target.checked })} /></td><td><strong>{row.name}</strong><small>{row.pageCount} {t('pdf.pages')} · {formatBytes(row.size)}</small></td><td><input aria-label={`${row.name} ${t('pdf.mergeRange')}`} value={row.pages} onChange={(event) => updateRow(setRows, row.path, { pages: event.target.value })} /></td><td><Status value={row.status} t={t} /></td><td><button className="icon-button" type="button" aria-label={t('common.action.delete')} onClick={() => setRows((current) => current.filter((item) => item.path !== row.path))}><Trash2 size={14} /></button></td></tr>)}</tbody></table></div>
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
