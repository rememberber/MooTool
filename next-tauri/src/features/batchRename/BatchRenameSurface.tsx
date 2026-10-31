import { open } from '@tauri-apps/plugin-dialog'
import { invoke } from '@tauri-apps/api/core'
import { FilePlus2, Play, RefreshCw, Undo2 } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { defaultRenameOptions, type RenameFile, type RenameOptions, type RenamePreview, type RenameStatus } from './batchRename'
import { batchRenameMessages } from './batchRenameMessages'

export function BatchRenameSurface() {
  const { t } = useLocalizedMessages(batchRenameMessages)
  const [files, setFiles] = useState<RenameFile[]>([])
  const [selected, setSelected] = useState<string[]>([])
  const [options, setOptions] = useState(defaultRenameOptions)
  const [preview, setPreview] = useState<RenamePreview | null>(null)
  const [status, setStatus] = useState<RenameStatus | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const revision = useRef(0)
  const rows = new Map(preview?.rows.map((row) => [row.id, row]) ?? [])

  function report(reason: unknown): void {
    const message = reason instanceof Error ? reason.message : String(reason)
    const key = message.match(/batchRename\.error\.(\w+)/)?.[1]
    setError(key ? t(`error.${key}` as 'error.file') : message)
  }

  async function refreshStatus(): Promise<void> {
    if (!window.__TAURI_INTERNALS__) return
    try { setStatus(await invoke<RenameStatus>('get_batch_rename_status')) } catch (reason) { report(reason) }
  }

  useEffect(() => { void refreshStatus() }, [])

  function invalidate(): void { revision.current += 1; setPreview(null); setError(''); setNotice('') }
  function change(patch: Partial<RenameOptions>): void { invalidate(); setOptions((current) => ({ ...current, ...patch })) }

  async function chooseFiles(): Promise<void> {
    setBusy(true)
    setError('')
    try {
      if (!window.__TAURI_INTERNALS__) throw new Error('batchRename.error.file')
      const picked = await open({ multiple: true, directory: false, title: t('action.selectFiles') })
      const paths = picked == null ? [] : Array.isArray(picked) ? picked : [picked]
      if (!paths.length) return
      const result = await invoke<RenameFile[]>('select_rename_files', { paths })
      invalidate()
      setFiles(result)
      setSelected(result.map((file) => file.id))
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
      const ids = files.filter((file) => selected.includes(file.id)).map((file) => file.id)
      const result = await invoke<RenamePreview>('preview_batch_rename', { ids, options })
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
      const result = undo
        ? await invoke<{ changedCount: number; files: RenameFile[]; status: RenameStatus }>('undo_batch_rename')
        : await invoke<{ changedCount: number; files: RenameFile[]; status: RenameStatus }>('execute_batch_rename', { token: preview?.token })
      revision.current += 1
      setPreview(null)
      setFiles(result.files)
      setSelected((current) => current.filter((id) => result.files.some((file) => file.id === id)))
      setStatus(result.status)
      setNotice(t(undo ? 'notice.restored' : 'notice.complete', { count: result.changedCount }))
    } catch (reason) { setPreview(null); report(reason) }
    finally { setBusy(false); void refreshStatus() }
  }

  return (
    <main className="utility-workbench batch-rename-workbench" aria-label={t('title')}>
      <header className="batch-rename-toolbar">
        <h1>{t('title')}</h1>
        <button type="button" disabled={busy} onClick={() => void chooseFiles()}><FilePlus2 size={14} />{t('action.selectFiles')}</button>
        <button type="button" disabled={busy || !status?.undoAvailable} onClick={() => void execute(true)}><Undo2 size={14} />{t(status?.recoveryRequired ? 'action.recover' : 'action.undo')}</button>
      </header>
      <div className="batch-rename-rules">
        {(['find', 'replacement', 'prefix', 'suffix'] as const).map((key) => (
          <label key={key}>{t(`field.${key}`)}<input value={options[key]} maxLength={120} disabled={busy} onChange={(event) => change({ [key]: event.target.value })} /></label>
        ))}
        <div className="batch-rename-toggles">
          {(['caseSensitive', 'preserveExtension', 'numbering'] as const).map((key) => (
            <label key={key}><input type="checkbox" checked={options[key]} disabled={busy} onChange={(event) => change({ [key]: event.target.checked })} />{t(`field.${key}`)}</label>
          ))}
        </div>
        <label>{t('field.start')}<input type="number" min={0} max={1_000_000} value={options.start} disabled={busy || !options.numbering} onChange={(event) => change({ start: Number(event.target.value) })} /></label>
        <label>{t('field.width')}<input type="number" min={1} max={8} value={options.width} disabled={busy || !options.numbering} onChange={(event) => change({ width: Number(event.target.value) })} /></label>
        <label>{t('field.separator')}<input value={options.separator} maxLength={120} disabled={busy || !options.numbering} onChange={(event) => change({ separator: event.target.value })} /></label>
      </div>
      <div className="batch-rename-toolbar">
        <span>{t('counts', { selected: selected.length, changed: preview?.changedCount ?? 0 })}</span>
        <button type="button" disabled={busy || selected.length === 0 || status?.recoveryRequired} onClick={() => void buildPreview()}><RefreshCw size={14} />{t('action.preview')}</button>
        <button type="button" disabled={busy || !preview?.token} onClick={() => void execute()}><Play size={14} />{t('action.execute')}</button>
      </div>
      {error && <p className="batch-rename-error" role="alert">{error}</p>}
      {preview?.rows.some((row) => row.status === 'conflict') && <p className="batch-rename-error" role="alert">{t('conflicts')}</p>}
      {notice && <p className="batch-rename-notice" role="status">{notice}</p>}
      {status?.recoveryRequired && <div className="batch-rename-error" role="alert">{t('recoveryHint')}<code>{status.journalPath}</code></div>}
      <div className="batch-rename-table-wrap">
        <table className="batch-rename-table">
          <thead>
            <tr>
              <th><input type="checkbox" aria-label={t('action.selectAll')} checked={files.length > 0 && selected.length === files.length} disabled={busy || files.length === 0} onChange={(event) => { invalidate(); setSelected(event.target.checked ? files.map((file) => file.id) : []) }} /></th>
              <th>#</th>
              <th>{t('column.oldName')}</th>
              <th>{t('column.newName')}</th>
              <th>{t('column.status')}</th>
            </tr>
          </thead>
          <tbody>
            {files.length === 0 ? <tr><td colSpan={5} className="batch-rename-empty">{t('empty')}</td></tr> : files.map((file, index) => {
              const row = rows.get(file.id)
              return (
                <tr key={file.id} data-rename-id={file.id} className={row?.status === 'conflict' ? 'batch-rename-row--conflict' : ''}>
                  <td><input type="checkbox" aria-label={file.name} checked={selected.includes(file.id)} disabled={busy} onChange={(event) => { invalidate(); setSelected((current) => event.target.checked ? [...current, file.id] : current.filter((id) => id !== file.id)) }} /></td>
                  <td>{index + 1}</td>
                  <td><strong>{file.name}</strong><small title={file.path}>{file.path}</small></td>
                  <td>{row?.newName ?? '—'}</td>
                  <td>{row ? t((row.issue ? `issue.${row.issue}` : `status.${row.status}`) as 'status.ready') : '—'}</td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
      <footer className="batch-rename-help">{t('help')}</footer>
    </main>
  )
}
