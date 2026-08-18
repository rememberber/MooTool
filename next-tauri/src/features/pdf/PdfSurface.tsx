import { CheckCircle2, Download, FilePlus2, GripVertical, Merge, Play, Split, Square, Trash2, TriangleAlert } from 'lucide-react'
import { PDFDocument } from 'pdf-lib'
import { useMemo, useRef, useState } from 'react'
import {
  useLocalizedMessages,
  type LocalizedMessageKey,
  type LocalizedTranslator,
  type MessageValues
} from '../../app/localizedMessages'
import { pdfFilesApi } from '../../platform/api/pdfFilesApi'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import { PdfPageRangeError, parsePageSelection, selectSplitPages, splitOutputName, type PdfSplitRule } from './pageRanges'
import { pdfMessages } from './pdfMessages'

type Tab = 'split' | 'merge'
type TaskStatus = 'ready' | 'running' | 'done' | 'error' | 'cancelled'
interface PdfSource { id: string; name: string; sizeBytes: number; pageCount: number; bytes: Uint8Array }
interface SplitTask extends PdfSource { selected: boolean; pageRange: string; rule: PdfSplitRule; customRule: string; status: TaskStatus; outputPages?: number }
interface MergeTask extends PdfSource { selected: boolean; pageRange: string; status: TaskStatus }
type PdfMessageKey = LocalizedMessageKey<typeof pdfMessages>
type Notice = { key: PdfMessageKey; values?: MessageValues } | { raw: string }

class PdfLocalizedError extends Error {
  constructor(readonly key: PdfMessageKey, readonly values?: MessageValues) {
    super(key)
  }
}

export function PdfSurface() {
  const { t } = useLocalizedMessages(pdfMessages)
  const splitInput = useRef<HTMLInputElement>(null)
  const mergeInput = useRef<HTMLInputElement>(null)
  const cancelled = useRef(false)
  const [tab, setTab] = useState<Tab>('split')
  const [splitTasks, setSplitTasks] = useState<SplitTask[]>([])
  const [mergeTasks, setMergeTasks] = useState<MergeTask[]>([])
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState<Notice>({ key: 'notice.ready' })
  const [failed, setFailed] = useState(false)
  const [outputs, setOutputs] = useState<{ name: string; pages: number }[]>([])
  const noticeText = 'raw' in notice ? notice.raw : t(notice.key, notice.values)
  const session = useMemo(() => ({
    digest: JSON.stringify({ tab, split: splitTasks.map(taskDigest), merge: mergeTasks.map(taskDigest), busy }),
    summary: t('session.summary', {
      mode: t(tab === 'split' ? 'session.split' : 'session.merge'),
      count: tab === 'split' ? splitTasks.length : mergeTasks.length,
      busy: busy ? t('session.busy') : ''
    })
  }), [busy, mergeTasks, splitTasks, t, tab])
  const { sessionId, reportError } = useToolSessionReport('pdf', session.digest, session.summary)
  const recordOperation = useOperationHistory('pdf')

  async function addFiles(files: File[], mode: Tab) {
    setFailed(false)
    try {
      const existingSize = (mode === 'split' ? splitTasks : mergeTasks)
        .reduce((sum, task) => sum + task.sizeBytes, 0)
      const addedSize = files.slice(0, 20).reduce((sum, file) => sum + file.size, 0)
      if (existingSize + addedSize > 500 * 1024 * 1024) throw new PdfLocalizedError('error.queueLimit')
      const inspected: PdfSource[] = []
      for (const file of files.slice(0, 20)) inspected.push(await inspectFile(file))
      if (mode === 'split') {
        setSplitTasks((current) => appendUnique(current, inspected.map((file) => ({ ...file, selected: true, pageRange: `1-${file.pageCount}`, rule: 'odd', customRule: '', status: 'ready' }))).slice(0, 20))
      } else {
        setMergeTasks((current) => appendUnique(current, inspected.map((file) => ({ ...file, selected: true, pageRange: `1-${file.pageCount}`, status: 'ready' }))).slice(0, 20))
      }
      succeed('notice.added', { count: inspected.length })
    } catch (cause) { fail(cause) }
  }

  async function splitSelected() {
    const selectedTasks = splitTasks.filter((task) => task.selected)
    if (!selectedTasks.length) return failMessage('error.selectSplit')
    const names = selectedTasks.map((task) => splitOutputName(task.name))
    let exportSession
    try { exportSession = await pdfFilesApi.begin(names) } catch (cause) { fail(cause); return }
    if (!exportSession) {
      succeed('notice.exportCancelled')
      return
    }
    cancelled.current = false
    setBusy(true)
    setOutputs([])
    setFailed(false)
    setSplitTasks((tasks) => tasks.map((task) => task.selected ? { ...task, status: 'running' } : task))
    const nextOutputs: { name: string; pages: number }[] = []
    try {
      for (let taskIndex = 0; taskIndex < selectedTasks.length; taskIndex += 1) {
        const task = selectedTasks[taskIndex]!
        ensureNotCancelled(cancelled)
        const source = await PDFDocument.load(task.bytes)
        const pages = selectSplitPages(task.pageRange, task.rule, task.customRule, source.getPageCount())
        const output = await copySelectedPages(source, pages, cancelled)
        const name = names[taskIndex]!
        const bytes = await output.save()
        await exportSession.write(taskIndex, bytes, () => ensureNotCancelled(cancelled))
        nextOutputs.push({ name, pages: pages.length })
        setSplitTasks((tasks) => tasks.map((item) => item.id === task.id ? { ...item, status: 'done', outputPages: pages.length } : item))
        await yieldToUi()
      }
      const paths = await exportSession.finish()
      setOutputs(nextOutputs)
      const pageCount = nextOutputs.reduce((sum, item) => sum + item.pages, 0)
      succeed(paths.length === 1 ? 'notice.splitDoneOne' : 'notice.splitDoneMany', paths.length === 1
        ? { pages: pageCount, path: paths[0]! }
        : { pages: pageCount, count: paths.length })
      recordOperation(t('operation.split'), t('operation.summary', { count: nextOutputs.length, pages: pageCount }), 'success')
    } catch (cause) {
      await exportSession.cancel().catch(() => undefined)
      const isCancelled = cause instanceof Error && cause.message === 'PDF_TASK_CANCELLED'
      setSplitTasks((tasks) => tasks.map((task) => selectedTasks.some((selected) => selected.id === task.id) ? { ...task, status: isCancelled ? 'cancelled' : 'error' } : task))
      if (isCancelled) { succeed('notice.splitCancelled'); recordOperation(t('operation.split'), t('operation.cancelled'), 'info') } else { fail(cause); recordOperation(t('operation.split'), localizedErrorText(cause, t), 'error') }
    } finally { setBusy(false) }
  }

  async function mergeSelected() {
    const selectedTasks = mergeTasks.filter((task) => task.selected)
    if (selectedTasks.length < 2) return failMessage('error.selectMerge')
    const name = `MooTool-merged-${timestamp()}.pdf`
    let exportSession
    try { exportSession = await pdfFilesApi.begin([name]) } catch (cause) { fail(cause); return }
    if (!exportSession) {
      succeed('notice.exportCancelled')
      return
    }
    cancelled.current = false
    setBusy(true)
    setOutputs([])
    setFailed(false)
    setMergeTasks((tasks) => tasks.map((task) => task.selected ? { ...task, status: 'running' } : task))
    try {
      const output = await PDFDocument.create()
      let pageCount = 0
      for (const task of selectedTasks) {
        ensureNotCancelled(cancelled)
        const source = await PDFDocument.load(task.bytes)
        const pages = parsePageSelection(task.pageRange, source.getPageCount())
        for (const page of pages) {
          ensureNotCancelled(cancelled)
          const [copied] = await output.copyPages(source, [page - 1])
          if (copied) output.addPage(copied)
          pageCount += 1
          if (pageCount % 10 === 0) await yieldToUi()
        }
        setMergeTasks((tasks) => tasks.map((item) => item.id === task.id ? { ...item, status: 'done' } : item))
      }
      if (!pageCount) throw new PdfPageRangeError('emptySelection')
      const bytes = await output.save()
      await exportSession.write(0, bytes, () => ensureNotCancelled(cancelled))
      const [path] = await exportSession.finish()
      setOutputs([{ name, pages: pageCount }])
      succeed('notice.mergeDone', { pages: pageCount, path: path ? t('notice.pathSuffix', { path }) : '' })
      recordOperation(t('operation.merge'), t('operation.summary', { count: selectedTasks.length, pages: pageCount }), 'success')
    } catch (cause) {
      await exportSession.cancel().catch(() => undefined)
      const isCancelled = cause instanceof Error && cause.message === 'PDF_TASK_CANCELLED'
      setMergeTasks((tasks) => tasks.map((task) => selectedTasks.some((selected) => selected.id === task.id) ? { ...task, status: isCancelled ? 'cancelled' : 'error' } : task))
      if (isCancelled) { succeed('notice.mergeCancelled'); recordOperation(t('operation.merge'), t('operation.cancelled'), 'info') } else { fail(cause); recordOperation(t('operation.merge'), localizedErrorText(cause, t), 'error') }
    } finally { setBusy(false) }
  }

  function stop() {
    cancelled.current = true
    succeed('notice.cancelling')
  }

  function succeed(key: PdfMessageKey, values?: MessageValues) { setNotice({ key, values }); setFailed(false) }
  function failMessage(key: PdfMessageKey, values?: MessageValues) { setNotice({ key, values }); setFailed(true) }
  function fail(cause: unknown) {
    setFailed(true)
    if (cause instanceof PdfLocalizedError) setNotice({ key: cause.key, values: cause.values })
    else if (cause instanceof PdfPageRangeError) setNotice({ key: `error.${cause.code}`, values: cause.values })
    else setNotice({ raw: cause instanceof Error ? cause.message : String(cause) })
  }

  return (
    <main className="utility-workbench pdf-workbench">
      <header className="utility-header">
        <div><span className="eyebrow">LOCAL PDF-LIB WORKBENCH</span><h1>{t('title')}</h1></div>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>
      <nav className="utility-segments pdf-tabs">
        <button className={tab === 'split' ? 'utility-segment utility-segment--active' : 'utility-segment'} type="button" onClick={() => setTab('split')}>{t('tab.split')}</button>
        <button className={tab === 'merge' ? 'utility-segment utility-segment--active' : 'utility-segment'} type="button" onClick={() => setTab('merge')}>{t('tab.merge')}</button>
      </nav>
      <div className="pdf-toolbar">
        <span>{t('toolbar.limits')}</span>
        <input ref={splitInput} hidden multiple type="file" accept="application/pdf,.pdf" onChange={(event) => { void addFiles([...event.target.files ?? []], 'split'); event.target.value = '' }} />
        <input ref={mergeInput} hidden multiple type="file" accept="application/pdf,.pdf" onChange={(event) => { void addFiles([...event.target.files ?? []], 'merge'); event.target.value = '' }} />
        <button type="button" disabled={busy || (tab === 'split' ? splitTasks.length : mergeTasks.length) >= 20} onClick={() => (tab === 'split' ? splitInput : mergeInput).current?.click()}><FilePlus2 />{t(tab === 'split' ? 'action.addSplit' : 'action.addMerge')}</button>
        {busy
          ? <button className="pdf-stop" type="button" onClick={stop}><Square />{t('action.stop')}</button>
          : <button className="primary-button" type="button" onClick={() => void (tab === 'split' ? splitSelected() : mergeSelected())}>{tab === 'split' ? <Split /> : <Merge />}{t(tab === 'split' ? 'action.startSplit' : 'action.startMerge')}</button>}
      </div>
      <section className="pdf-table-wrap">{tab === 'split' ? <SplitTable tasks={splitTasks} setTasks={setSplitTasks} busy={busy} /> : <MergeTable tasks={mergeTasks} setTasks={setMergeTasks} busy={busy} />}</section>
      <section className="pdf-outputs"><strong><Download />{t('output.title')}</strong>{outputs.length ? outputs.map((output, index) => <span key={`${output.name}-${index}`}><code>{output.name}</code>{t('pages', { count: output.pages })}</span>) : <em>{t('output.empty')}</em>}</section>
      <footer className={failed ? 'utility-status utility-status--error' : 'utility-status'}><span>{failed ? <TriangleAlert /> : <CheckCircle2 />}{noticeText}</span><span>{t('footer.capabilities')}</span><code>{session.summary}</code></footer>
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
    </main>
  )
}

function SplitTable({ tasks, setTasks, busy }: { tasks: SplitTask[]; setTasks: React.Dispatch<React.SetStateAction<SplitTask[]>>; busy: boolean }) {
  const { t } = useLocalizedMessages(pdfMessages)
  return <table className="pdf-table"><thead><tr><th>{t('table.select')}</th><th>{t('table.filename')}</th><th>{t('table.pageRange')}</th><th>{t('table.rule')}</th><th>{t('table.customRange')}</th><th>{t('table.status')}</th><th /></tr></thead><tbody>{tasks.length ? tasks.map((task) => <tr key={task.id}><td><input type="checkbox" disabled={busy} checked={task.selected} onChange={(event) => updateTask(setTasks, task.id, { selected: event.target.checked })} /></td><td><strong>{task.name}</strong><small>{t('pages', { count: task.pageCount })} · {formatBytes(task.sizeBytes)}</small></td><td><input disabled={busy} value={task.pageRange} onChange={(event) => updateTask(setTasks, task.id, { pageRange: event.target.value, status: 'ready' })} /></td><td><select disabled={busy} value={task.rule} onChange={(event) => updateTask(setTasks, task.id, { rule: event.target.value as PdfSplitRule, status: 'ready' })}><option value="odd">{t('rule.odd')}</option><option value="even">{t('rule.even')}</option><option value="custom">{t('rule.custom')}</option></select></td><td><input disabled={busy || task.rule !== 'custom'} value={task.customRule} placeholder="1-5,8,10" onChange={(event) => updateTask(setTasks, task.id, { customRule: event.target.value, status: 'ready' })} /></td><td><Status status={task.status} pages={task.outputPages} /></td><td><button type="button" aria-label={t('action.remove')} disabled={busy} onClick={() => setTasks((items) => items.filter((item) => item.id !== task.id))}><Trash2 /></button></td></tr>) : <tr><td className="pdf-empty" colSpan={7}>{t('table.emptySplit')}</td></tr>}</tbody></table>
}

function MergeTable({ tasks, setTasks, busy }: { tasks: MergeTask[]; setTasks: React.Dispatch<React.SetStateAction<MergeTask[]>>; busy: boolean }) {
  const { t } = useLocalizedMessages(pdfMessages)
  function move(id: string, offset: number) { setTasks((items) => { const index = items.findIndex((item) => item.id === id); const nextIndex = index + offset; if (index < 0 || nextIndex < 0 || nextIndex >= items.length) return items; const next = [...items]; [next[index], next[nextIndex]] = [next[nextIndex]!, next[index]!]; return next }) }
  return <table className="pdf-table"><thead><tr><th>{t('table.select')}</th><th>{t('table.order')}</th><th>{t('table.filename')}</th><th>{t('table.mergeRange')}</th><th>{t('table.status')}</th><th /></tr></thead><tbody>{tasks.length ? tasks.map((task, index) => <tr key={task.id}><td><input type="checkbox" disabled={busy} checked={task.selected} onChange={(event) => updateTask(setTasks, task.id, { selected: event.target.checked })} /></td><td><span className="pdf-order"><GripVertical /><button aria-label={t('action.moveUp')} disabled={busy || index === 0} type="button" onClick={() => move(task.id, -1)}>↑</button><button aria-label={t('action.moveDown')} disabled={busy || index === tasks.length - 1} type="button" onClick={() => move(task.id, 1)}>↓</button></span></td><td><strong>{task.name}</strong><small>{t('pages', { count: task.pageCount })} · {formatBytes(task.sizeBytes)}</small></td><td><input disabled={busy} value={task.pageRange} onChange={(event) => updateTask(setTasks, task.id, { pageRange: event.target.value, status: 'ready' })} /></td><td><Status status={task.status} /></td><td><button type="button" aria-label={t('action.remove')} disabled={busy} onClick={() => setTasks((items) => items.filter((item) => item.id !== task.id))}><Trash2 /></button></td></tr>) : <tr><td className="pdf-empty" colSpan={6}>{t('table.emptyMerge')}</td></tr>}</tbody></table>
}

function Status({ status, pages }: { status: TaskStatus; pages?: number }) {
  const { t } = useLocalizedMessages(pdfMessages)
  return <span className={`pdf-status pdf-status--${status}`}>{status === 'running' && <Play />}{t(`status.${status}`)}{pages ? ` · ${t('pages', { count: pages })}` : ''}</span>
}

async function inspectFile(file: File): Promise<PdfSource> {
  if (!file.name.toLowerCase().endsWith('.pdf')) throw new PdfLocalizedError('error.pdfOnly', { file: file.name })
  if (file.size === 0 || file.size > 200 * 1024 * 1024) throw new PdfLocalizedError('error.fileLimit', { file: file.name })
  const bytes = new Uint8Array(await file.arrayBuffer())
  const document = await PDFDocument.load(bytes)
  return { id: crypto.randomUUID(), name: file.name, sizeBytes: file.size, pageCount: document.getPageCount(), bytes }
}

async function copySelectedPages(source: PDFDocument, pages: number[], cancelled: React.MutableRefObject<boolean>): Promise<PDFDocument> {
  const output = await PDFDocument.create()
  for (let index = 0; index < pages.length; index += 1) {
    ensureNotCancelled(cancelled)
    const [page] = await output.copyPages(source, [pages[index]! - 1])
    if (page) output.addPage(page)
    if ((index + 1) % 10 === 0) await yieldToUi()
  }
  return output
}

function ensureNotCancelled(cancelled: React.MutableRefObject<boolean>) {
  if (cancelled.current) throw new Error('PDF_TASK_CANCELLED')
}

function localizedErrorText(cause: unknown, t: LocalizedTranslator<typeof pdfMessages>['t']): string {
  if (cause instanceof PdfLocalizedError) return t(cause.key, cause.values)
  if (cause instanceof PdfPageRangeError) return t(`error.${cause.code}`, cause.values)
  return cause instanceof Error ? cause.message : String(cause)
}

function updateTask<T extends { id: string }>(setter: React.Dispatch<React.SetStateAction<T[]>>, id: string, patch: Partial<T>) {
  setter((items) => items.map((item) => item.id === id ? { ...item, ...patch } : item))
}

function appendUnique<T extends { name: string; sizeBytes: number }>(current: T[], additions: T[]): T[] {
  return [...current, ...additions.filter((addition) => !current.some((item) => item.name === addition.name && item.sizeBytes === addition.sizeBytes))]
}

function taskDigest(task: { name: string; sizeBytes: number; pageCount: number; status: TaskStatus }) {
  return [task.name, task.sizeBytes, task.pageCount, task.status]
}

function yieldToUi(): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, 0))
}

function timestamp(): string {
  return new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19)
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(1)} KiB`
  return `${(bytes / 1024 ** 2).toFixed(2)} MiB`
}
