import { FilePlus2, Play, RefreshCw, Undo2 } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { ToolPageHeader, WorkspaceDragZone } from '@/shared/components/ToolPage'
import { defaultRenameOptions, type RenameFile, type RenameOptions, type RenamePreview, type RenameStatus } from '@/shared/contracts/batchRename'
import { useI18n } from '@/shared/i18n/I18nProvider'
import type { MessageKey } from '@/shared/i18n/messages'

export function BatchRenameTool() {
  const { t } = useI18n()
  const [files, setFiles] = useState<RenameFile[]>([])
  const [selected, setSelected] = useState<string[]>([])
  const [options, setOptions] = useState(defaultRenameOptions)
  const [preview, setPreview] = useState<RenamePreview | null>(null)
  const [status, setStatus] = useState<RenameStatus | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const revision = useRef(0)

  function report(reason: unknown): void {
    const message = reason instanceof Error ? reason.message : String(reason)
    const key = message.match(/batchRename\.error\.\w+/)?.[0]
    setError(key ? t(key as MessageKey) : message)
  }
  async function refreshStatus(): Promise<void> {
    try { setStatus(await window.mootool.getBatchRenameStatus()) } catch (reason) { report(reason) }
  }
  useEffect(() => {
    void refreshStatus()
    return window.mootool.onBatchRenameStatusChange(setStatus)
  }, [])

  function invalidate(): void { revision.current += 1; setPreview(null); setError(''); setNotice('') }
  function change(patch: Partial<RenameOptions>): void { invalidate(); setOptions((current) => ({ ...current, ...patch })) }

  async function chooseFiles(): Promise<void> {
    setBusy(true)
    setError('')
    try {
      const result = await window.mootool.selectRenameFiles()
      if (result) { invalidate(); setFiles(result); setSelected(result.map((file) => file.id)) }
    } catch (reason) { report(reason) }
    finally { setBusy(false) }
  }
  async function buildPreview(): Promise<void> {
    const current = ++revision.current
    setBusy(true)
    setError('')
    setNotice('')
    setPreview(null)
    try {
      const result = await window.mootool.previewBatchRename(files.filter((file) => selected.includes(file.id)).map((file) => file.id), options)
      if (revision.current === current) setPreview(result)
    } catch (reason) { report(reason) }
    finally { setBusy(false); void refreshStatus() }
  }
  async function execute(undo = false): Promise<void> {
    if (!undo && !preview?.token) return
    setBusy(true)
    setError('')
    setNotice('')
    try {
      const result = undo ? await window.mootool.undoBatchRename() : await window.mootool.executeBatchRename(preview!.token!)
      revision.current += 1
      setPreview(null)
      setFiles(result.files)
      setSelected((current) => current.filter((id) => result.files.some((file) => file.id === id)))
      setStatus(result.status)
      setNotice(t(undo ? 'batchRename.restored' : 'batchRename.complete', { count: String(result.changedCount) }))
    } catch (reason) { setPreview(null); report(reason) }
    finally { setBusy(false); void refreshStatus() }
  }
  const rows = new Map(preview?.rows.map((row) => [row.id, row]) ?? [])

  return <section className="tool-page batch-rename-tool" aria-label={t('app.nav.batchRename')}>
    <ToolPageHeader title={t('app.nav.batchRename')} />
    <div className="local-tool-shell batch-rename-workspace">
      <header className="p4-toolbar batch-rename-toolbar">
        <strong>{t('app.nav.batchRename')}</strong><WorkspaceDragZone />
        <button className="dialog-button" type="button" disabled={busy} onClick={() => void chooseFiles()}><FilePlus2 size={14} />{t('batchRename.selectFiles')}</button>
        <button className="dialog-button" type="button" disabled={busy || !status?.undoAvailable} onClick={() => void execute(true)}><Undo2 size={14} />{t(status?.recoveryRequired ? 'batchRename.recover' : 'batchRename.undo')}</button>
      </header>
      <div className="batch-rename-rules">
        {(['find', 'replacement', 'prefix', 'suffix'] as const).map((key) => <label key={key}>{t(`batchRename.${key}`)}<input value={options[key]} maxLength={120} disabled={busy} onChange={(event) => change({ [key]: event.target.value })} /></label>)}
        <div className="batch-rename-toggles">
          {(['caseSensitive', 'preserveExtension', 'numbering'] as const).map((key) => <label key={key}><input type="checkbox" checked={options[key]} disabled={busy} onChange={(event) => change({ [key]: event.target.checked })} />{t(`batchRename.${key}`)}</label>)}
        </div>
        <label>{t('batchRename.start')}<input type="number" min={0} max={1_000_000} value={options.start} disabled={busy || !options.numbering} onChange={(event) => change({ start: Number(event.target.value) })} /></label>
        <label>{t('batchRename.width')}<input type="number" min={1} max={8} value={options.width} disabled={busy || !options.numbering} onChange={(event) => change({ width: Number(event.target.value) })} /></label>
        <label>{t('batchRename.separator')}<input value={options.separator} maxLength={120} disabled={busy || !options.numbering} onChange={(event) => change({ separator: event.target.value })} /></label>
      </div>
      <div className="p4-toolbar batch-rename-preview-toolbar">
        <span>{t('batchRename.counts', { selected: String(selected.length), changed: String(preview?.changedCount ?? 0) })}</span>
        <span className="p4-toolbar__spacer" />
        <button className="dialog-button" type="button" disabled={busy || selected.length === 0 || status?.recoveryRequired} onClick={() => void buildPreview()}><RefreshCw size={14} />{t('batchRename.preview')}</button>
        <button className="primary-command" type="button" disabled={busy || !preview?.token} onClick={() => void execute()}><Play size={14} />{busy ? t('common.processing') : t('batchRename.execute')}</button>
      </div>
      {error && <p className="batch-rename-error" role="alert">{error}</p>}
      {preview?.rows.some((row) => row.status === 'conflict') && <p className="batch-rename-error" role="alert">{t('batchRename.conflicts')}</p>}
      {notice && <p className="batch-rename-notice" role="status">{notice}</p>}
      {status?.recoveryRequired && <div className="batch-rename-error" role="alert">{t('batchRename.recoveryHint')}<code>{status.journalPath}</code></div>}
      <div className="batch-rename-table-wrap"><table className="batch-rename-table">
        <thead><tr><th><input type="checkbox" aria-label={t('batchRename.selectAll')} checked={files.length > 0 && selected.length === files.length} disabled={busy || files.length === 0}
          onChange={(event) => { invalidate(); setSelected(event.target.checked ? files.map((file) => file.id) : []) }} /></th><th>#</th><th>{t('batchRename.oldName')}</th><th>{t('batchRename.newName')}</th><th>{t('batchRename.status')}</th></tr></thead>
        <tbody>{files.length === 0 ? <tr><td colSpan={5} className="batch-rename-empty">{t('batchRename.empty')}</td></tr> : files.map((file, index) => {
          const row = rows.get(file.id)
          return <tr key={file.id} data-rename-id={file.id} className={row?.status === 'conflict' ? 'batch-rename-row--conflict' : ''}>
            <td><input type="checkbox" aria-label={file.name} checked={selected.includes(file.id)} disabled={busy} onChange={(event) => {
              invalidate(); setSelected((current) => event.target.checked ? [...current, file.id] : current.filter((id) => id !== file.id))
            }} /></td><td>{index + 1}</td><td><strong>{file.name}</strong><small title={file.path}>{file.path}</small></td>
            <td className="batch-rename-new-name">{row?.newName ?? '—'}</td><td>{row ? t(row.issue ? `batchRename.issue.${row.issue}` : `batchRename.status.${row.status}`) : '—'}</td>
          </tr>
        })}</tbody>
      </table></div>
      <footer className="batch-rename-help">{t('batchRename.help')}</footer>
    </div>
  </section>
}
