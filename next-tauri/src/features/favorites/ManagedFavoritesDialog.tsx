import { X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { localDataApi } from '../../platform/api/localDataApi'
import type { FavoriteFolder, NamedFavorite, NamedFavoriteKind } from '../../platform/contracts/localData'
import { useDesktopDialog } from '../../shared/DesktopDialogProvider'
import { toolFavoriteMessages } from './toolFavoriteMessages'

export function ManagedFavoritesDialog({ kind, currentValue, onClose, onApply }: {
  kind: NamedFavoriteKind
  currentValue: string
  onClose: () => void
  onApply(value: string): void
}) {
  const { t } = useLocalizedMessages(toolFavoriteMessages)
  const dialog = useDesktopDialog()
  const [folders, setFolders] = useState<FavoriteFolder[]>([])
  const [folderId, setFolderId] = useState<number>()
  const [records, setRecords] = useState<NamedFavorite[]>([])
  const [selected, setSelected] = useState<number[]>([])
  const [editing, setEditing] = useState<number>()
  const [name, setName] = useState('')
  const [value, setValue] = useState(currentValue)
  const [description, setDescription] = useState('')
  const [error, setError] = useState('')

  async function load(preferred = folderId): Promise<void> {
    const list = await localDataApi.listFavoriteFolders(kind)
    const id = list.some((folder) => folder.id === preferred) ? preferred : list[0]?.id
    setFolders(list)
    setFolderId(id)
    const items = id == null ? [] : await localDataApi.listNamedFavorites(kind, id)
    setRecords(items)
    setSelected((current) => current.filter((selectedId) => items.some((item) => item.id === selectedId)))
  }

  useEffect(() => {
    void load().catch((cause: unknown) => setError(cause instanceof Error ? cause.message : String(cause)))
  }, [kind])

  async function run(action: () => Promise<unknown>): Promise<void> {
    try {
      await action()
      setError('')
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause))
    }
  }

  async function folderAction(action: 'create' | 'rename' | 'delete'): Promise<void> {
    const folder = folders.find((item) => item.id === folderId)
    if (action === 'delete') {
      if (!folder || !await dialog.confirm(t('dialog.deleteFolderConfirm', { name: folder.title }), { dangerous: true })) return
      await localDataApi.deleteFavoriteFolder(folder.id)
      await load()
      return
    }
    const title = await dialog.prompt(t('dialog.folderName'), action === 'rename' ? folder?.title : '')
    if (!title?.trim()) return
    const saved = action === 'create'
      ? await localDataApi.createFavoriteFolder(kind, title)
      : await localDataApi.renameFavoriteFolder(folderId ?? 0, title)
    await load(saved.id)
  }

  return (
    <section className="http-history-modal func-history-modal" role="dialog" aria-modal="true" aria-label={t('dialog.title')}>
      <div>
        <header>
          <div><strong>{t('dialog.title')}</strong></div>
          <button type="button" aria-label={t('dialog.close')} onClick={onClose}><X /></button>
        </header>
        <div className="cron-favorite-toolbar">
          <select aria-label={t('dialog.folder')} value={folderId ?? ''} onChange={(event) => void run(() => load(Number(event.target.value)))}>
            {folders.map((folder) => <option key={folder.id} value={folder.id}>{folder.title}</option>)}
          </select>
          <button className="dialog-button" type="button" onClick={() => void run(() => folderAction('create'))}>{t('dialog.newFolder')}</button>
          <button className="dialog-button" type="button" onClick={() => void run(() => folderAction('rename'))}>{t('dialog.renameFolder')}</button>
          <button className="dialog-button" type="button" onClick={() => void run(() => folderAction('delete'))}>{t('dialog.deleteFolder')}</button>
        </div>
        <div className="cron-favorite-form">
          <input aria-label={t('dialog.name')} placeholder={t('dialog.name')} value={name} onChange={(event) => setName(event.target.value)} />
          <input aria-label={t('dialog.addCurrent')} value={value} onChange={(event) => setValue(event.target.value)} />
          <input aria-label={t('dialog.notes')} placeholder={t('dialog.notes')} value={description} onChange={(event) => setDescription(event.target.value)} />
          <button className="dialog-button" type="button" disabled={!name.trim() || !value.trim() || folderId == null} onClick={() => void run(async () => {
            await localDataApi.saveNamedFavorite({ id: editing, kind, folderId, name, value, description })
            setEditing(undefined)
            setName('')
            await load()
          })}>{t('dialog.save')}</button>
          <button className="dialog-button" type="button" onClick={() => { setEditing(undefined); setName(''); setValue(currentValue); setDescription('') }}>{t('dialog.addCurrent')}</button>
        </div>
        {error && <p role="alert">{error}</p>}
        <main>
          {records.length === 0
            ? <p>{t('dialog.empty')}</p>
            : records.map((record, index) => (
              <article key={record.id}>
                <button type="button" onClick={() => { onApply(record.value); onClose() }}>
                  <strong>{record.name}</strong>
                  <code>{record.value}</code>
                  <span>{record.description}</span>
                </button>
                <button className="dialog-button" type="button" onClick={() => { setEditing(record.id); setName(record.name); setValue(record.value); setDescription(record.description) }}>{t('dialog.edit')}</button>
                <button className="dialog-button" type="button" disabled={index === 0} onClick={() => void run(async () => { await localDataApi.moveNamedFavorite(record.id, -1); await load() })}>{t('dialog.up')}</button>
                <button className="dialog-button" type="button" disabled={index === records.length - 1} onClick={() => void run(async () => { await localDataApi.moveNamedFavorite(record.id, 1); await load() })}>{t('dialog.down')}</button>
                <button className="dialog-button" type="button" onClick={() => void run(async () => { await localDataApi.deleteNamedFavorite(record.id); if (editing === record.id) setEditing(undefined); await load() })}>{t('dialog.delete')}</button>
              </article>
            ))}
        </main>
        <footer>
          <label>
            <input type="checkbox" aria-label={t('dialog.selectAll')} checked={records.length > 0 && selected.length === records.length} onChange={(event) => setSelected(event.target.checked ? records.map((record) => record.id) : [])} />
            {selected.length} / {records.length}
          </label>
          <button className="dialog-button" type="button" disabled={selected.length === 0} onClick={() => void run(async () => {
            if (!await dialog.confirm(`${t('dialog.delete')} (${selected.length})`, { dangerous: true })) return
            for (const id of selected) await localDataApi.deleteNamedFavorite(id)
            setEditing(undefined)
            await load()
          })}>{t('dialog.delete')}</button>
          <button type="button" onClick={onClose}>{t('dialog.close')}</button>
        </footer>
      </div>
    </section>
  )
}
