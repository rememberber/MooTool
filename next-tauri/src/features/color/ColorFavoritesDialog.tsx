import { MoreHorizontal, Star, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { localDataApi } from '../../platform/api/localDataApi'
import type { FavoriteFolder, NamedFavorite } from '../../platform/contracts/localData'
import { useDesktopDialog } from '../../shared/DesktopDialogProvider'
import { toolFavoriteMessages } from '../favorites/toolFavoriteMessages'
import { colorMessages } from './colorMessages'
import { parseFavoriteColor } from './colorTools'

export function ColorFavoritesDialog({ color, open, mode, onClose, onApply }: {
  color: string
  open: boolean
  mode: 'save' | 'manage'
  onClose: () => void
  onApply(value: string): void
}) {
  const { t } = useLocalizedMessages(colorMessages)
  const foldersText = useLocalizedMessages(toolFavoriteMessages)
  const dialog = useDesktopDialog()
  const [folders, setFolders] = useState<FavoriteFolder[]>([])
  const [folderId, setFolderId] = useState<number>()
  const [records, setRecords] = useState<NamedFavorite[]>([])
  const [selected, setSelected] = useState<number[]>([])
  const [selecting, setSelecting] = useState(false)
  const [editing, setEditing] = useState<{ id?: number; name: string; value: string; description: string } | null>(null)
  const [menu, setMenu] = useState<{ ids: number[]; left: number; top: number } | null>(null)
  const [error, setError] = useState('')
  const [anchor, setAnchor] = useState<number>()

  async function load(preferred = folderId): Promise<void> {
    const list = await localDataApi.listFavoriteFolders('color')
    const id = list.some((folder) => folder.id === preferred) ? preferred : list[0]?.id
    setFolders(list)
    setFolderId(id)
    setRecords(id == null ? [] : await localDataApi.listNamedFavorites('color', id))
    setSelected((current) => current.filter((item) => records.some((record) => record.id === item)))
  }

  useEffect(() => {
    if (!open) return
    setError('')
    setMenu(null)
    setSelecting(false)
    setSelected([])
    if (mode === 'save') setEditing({ name: `Color-${color}`, value: color, description: '' })
    else setEditing(null)
    void load().catch((cause: unknown) => setError(cause instanceof Error ? cause.message : String(cause)))
  }, [open, mode, color])

  if (!open) return null

  async function run(action: () => Promise<unknown>): Promise<void> {
    try { await action(); setError('') } catch (cause) { setError(cause instanceof Error ? cause.message : String(cause)) }
  }

  async function folderAction(action: 'create' | 'rename' | 'delete'): Promise<void> {
    const folder = folders.find((item) => item.id === folderId)
    if (action === 'delete') {
      if (!folder || !await dialog.confirm(foldersText.t('dialog.deleteFolderConfirm', { name: folder.title }), { dangerous: true })) return
      await localDataApi.deleteFavoriteFolder(folder.id)
      await load()
      return
    }
    const title = await dialog.prompt(foldersText.t('dialog.folderName'), action === 'rename' ? folder?.title : '')
    if (!title?.trim()) return
    const saved = action === 'create'
      ? await localDataApi.createFavoriteFolder('color', title)
      : await localDataApi.renameFavoriteFolder(folderId ?? 0, title)
    setSelected([])
    await load(saved.id)
  }

  async function saveEditing(): Promise<void> {
    if (!editing || folderId == null) return
    const value = parseFavoriteColor(editing.value)
    await localDataApi.saveNamedFavorite({ id: editing.id, kind: 'color', folderId, name: editing.name.trim(), value, description: editing.description })
    if (mode === 'save') onClose()
    else { setEditing(null); await load() }
  }

  function choose(record: NamedFavorite, event: React.MouseEvent): void {
    if (!selecting && !event.metaKey && !event.ctrlKey && !event.shiftKey) {
      onApply(record.value)
      onClose()
      return
    }
    setSelecting(true)
    if (event.shiftKey && records.some((item) => item.id === anchor)) {
      const start = records.findIndex((item) => item.id === anchor)
      const end = records.findIndex((item) => item.id === record.id)
      setSelected(records.slice(Math.min(start, end), Math.max(start, end) + 1).map((item) => item.id))
    } else {
      setAnchor(record.id)
      setSelected((current) => current.includes(record.id) ? current.filter((id) => id !== record.id) : [...current, record.id])
    }
  }

  const menuRecord = menu?.ids.length === 1 ? records.find((item) => item.id === menu.ids[0]) : undefined
  return (
    <section className="http-history-modal func-history-modal" role="dialog" aria-modal="true" aria-label={editing && mode === 'save' ? t('favorite.edit') : t('action.favorites')}>
      <div>
        <header>
          <div><strong>{editing && mode === 'save' ? t('favorite.edit') : t('action.favorites')}</strong></div>
          <button type="button" aria-label={foldersText.t('dialog.close')} onClick={() => { if (menu) setMenu(null); else if (editing && mode === 'manage') setEditing(null); else onClose() }}><X /></button>
        </header>
        {mode === 'manage' && !editing && (
          <>
            <div className="cron-favorite-toolbar">
              <select aria-label={foldersText.t('dialog.folder')} value={folderId ?? ''} onChange={(event) => { setSelected([]); setAnchor(undefined); void run(() => load(Number(event.target.value))) }}>
                {folders.map((folder) => <option key={folder.id} value={folder.id}>{folder.title}</option>)}
              </select>
              <button className="dialog-button" type="button" onClick={() => void run(() => folderAction('create'))}>{foldersText.t('dialog.newFolder')}</button>
              <button className="dialog-button" type="button" onClick={() => void run(() => folderAction('rename'))}>{foldersText.t('dialog.renameFolder')}</button>
              <button className="dialog-button" type="button" onClick={() => void run(() => folderAction('delete'))}>{foldersText.t('dialog.deleteFolder')}</button>
              <button className="dialog-button" type="button" onClick={() => { setSelecting((current) => !current); setSelected([]) }}>{selecting ? t('favorite.done') : t('favorite.select')}</button>
              <button className="dialog-button" type="button" onClick={() => setEditing({ name: `Color-${color}`, value: color, description: '' })}><Star size={14} />{t('action.favorite')}</button>
            </div>
            <p>{t('favorite.hint')}</p>
            {error && <p role="alert">{error}</p>}
            <main>
              {records.length === 0 ? <p>{foldersText.t('dialog.empty')}</p> : records.map((record) => (
                <article key={record.id}>
                  <button type="button" aria-label={`${record.name} ${record.value}`} onClick={(event) => choose(record, event)} onContextMenu={(event) => { event.preventDefault(); setMenu({ ids: selecting && selected.includes(record.id) ? selected : [record.id], left: event.clientX, top: event.clientY }) }}>
                    <span style={{ background: preview(record.value) }} />
                    <strong>{record.name}</strong>
                    <code>{record.value}</code>
                  </button>
                  <button className="dialog-button" type="button" aria-label={`${t('favorite.edit')} · ${record.name}`} onClick={(event) => { const rect = event.currentTarget.getBoundingClientRect(); setMenu({ ids: [record.id], left: rect.right, top: rect.bottom }) }}><MoreHorizontal size={16} /></button>
                </article>
              ))}
            </main>
            {menu && (
              <div role="menu" style={{ position: 'fixed', left: menu.left, top: menu.top }}>
                <button type="button" role="menuitem" disabled={!menuRecord} onClick={() => { if (menuRecord) setEditing({ id: menuRecord.id, name: menuRecord.name, value: menuRecord.value, description: menuRecord.description }); setMenu(null) }}>{t('favorite.edit')}</button>
                <button type="button" role="menuitem" onClick={() => void run(async () => { for (const id of menu.ids) await localDataApi.moveNamedFavorite(id, -1); setMenu(null); await load() })}>{t('favorite.up')}</button>
                <button type="button" role="menuitem" onClick={() => void run(async () => { for (const id of [...menu.ids].reverse()) await localDataApi.moveNamedFavorite(id, 1); setMenu(null); await load() })}>{t('favorite.down')}</button>
                <button type="button" role="menuitem" onClick={() => void run(async () => {
                  setMenu(null)
                  if (!await dialog.confirm(`${t('favorite.delete')} (${menu.ids.length})`, { dangerous: true })) return
                  for (const id of menu.ids) await localDataApi.deleteNamedFavorite(id)
                  await load()
                })}>{t('favorite.delete')}</button>
              </div>
            )}
          </>
        )}
        {editing && (
          <form onSubmit={(event) => { event.preventDefault(); void run(saveEditing) }}>
            <div className="color-favorite-preview"><span style={{ background: preview(editing.value) }} /><strong>{editing.value}</strong></div>
            <label>{t('favorite.name')}<input aria-label={t('favorite.name')} value={editing.name} onChange={(event) => setEditing({ ...editing, name: event.target.value })} /></label>
            <label>HEX / RGB<input aria-label="HEX / RGB" value={editing.value} onChange={(event) => setEditing({ ...editing, value: event.target.value })} /></label>
            <label>{foldersText.t('dialog.folder')}
              <select aria-label={foldersText.t('dialog.folder')} value={folderId ?? ''} onChange={(event) => setFolderId(Number(event.target.value))}>
                {folders.map((folder) => <option key={folder.id} value={folder.id}>{folder.title}</option>)}
              </select>
            </label>
            <button className="dialog-button" type="button" onClick={() => void run(() => folderAction('create'))}>{foldersText.t('dialog.newFolder')}</button>
            <label>{t('favorite.notes')}<input aria-label={t('favorite.notes')} value={editing.description} onChange={(event) => setEditing({ ...editing, description: event.target.value })} /></label>
            {error && <p role="alert">{error}</p>}
            <footer>
              <button className="dialog-button" type="button" onClick={() => mode === 'save' ? onClose() : setEditing(null)}>{t('favorite.cancel')}</button>
              <button className="dialog-button" type="submit" disabled={!editing.name.trim() || !editing.value.trim() || folderId == null}>{t('favorite.save')}</button>
            </footer>
          </form>
        )}
      </div>
    </section>
  )
}

function preview(value: string): string {
  try { return parseFavoriteColor(value) } catch { return 'transparent' }
}
