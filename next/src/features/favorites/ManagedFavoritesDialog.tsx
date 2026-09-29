import { useEffect, useState } from 'react'
import { Dialog } from '@/shared/components/Dialog'
import type { FavoriteFolderRecord, FavoriteRecord } from '@/shared/contracts/favorites'
import { useDesktopDialog } from '@/shared/feedback/DesktopDialogProvider'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function ManagedFavoritesDialog({ kind, open, currentValue, onClose, onApply }: { kind: 'cron' | 'regex'; open: boolean; currentValue: string; onClose: () => void; onApply: (value: string) => void }) {
  const { t, language } = useI18n()
  const dialog = useDesktopDialog()
  const [folders, setFolders] = useState<FavoriteFolderRecord[]>([])
  const [folderId, setFolderId] = useState<number>()
  const [records, setRecords] = useState<FavoriteRecord[]>([])
  const [selected, setSelected] = useState<number[]>([])
  const [editing, setEditing] = useState<number>()
  const [name, setName] = useState('')
  const [value, setValue] = useState(currentValue)
  const [description, setDescription] = useState('')
  const [error, setError] = useState('')
  const words = language === 'zh-CN' ? ['编辑', '上移', '下移', '备注', '保存', '添加当前表达式'] : language === 'ja-JP' ? ['編集', '上へ', '下へ', 'メモ', '保存', '現在の式を追加'] : ['Edit', 'Move up', 'Move down', 'Notes', 'Save', 'Add current expression']
  async function load(preferred = folderId) {
    const list = await window.mootool.listFavoriteFolders(kind)
    setFolders(list)
    const id = list.some(folder => folder.id === preferred) ? preferred : list[0]?.id
    setFolderId(id)
    const items = await window.mootool.listFavorites(kind, id)
    setRecords(items)
    setSelected(current => current.filter(selectedId => items.some(item => item.id === selectedId)))
  }
  async function run(action: () => Promise<unknown>) {
    try { await action(); setError('') } catch (caught) { setError(String(caught)) }
  }
  useEffect(() => { if (open) { setSelected([]); setEditing(undefined); setName(''); setValue(currentValue); setDescription(''); void run(() => load()) } }, [open])
  async function folderAction(action: 'create' | 'rename' | 'delete') {
    const folder = folders.find(item => item.id === folderId)
    if (action === 'delete') {
      if (!folder || !await dialog.confirm(t('favorite.deleteFolderConfirm', { name: folder.title }), { danger: true })) return
      await window.mootool.deleteFavoriteFolder(folder.id)
      await load(); return
    }
    const title = await dialog.prompt(t('favorite.folderName'), { defaultValue: action === 'rename' ? folder?.title : '' })
    if (!title?.trim()) return
    const saved = action === 'create' ? await window.mootool.createFavoriteFolder({ kind, title }) : await window.mootool.renameFavoriteFolder({ id: folderId!, title })
    await load(saved.id)
  }
  return <Dialog title={t('favorite.title')} open={open} width={820} onClose={onClose}>
    <div className="cron-favorite-toolbar">
      <select aria-label={t('favorite.folder')} value={folderId ?? ''} onChange={event => { setEditing(undefined); void run(() => load(Number(event.target.value))) }}>{folders.map(folder => <option key={folder.id} value={folder.id}>{folder.title}</option>)}</select>
      {(['create', 'rename', 'delete'] as const).map((action, i) => <button className="dialog-button" type="button" key={action} onClick={() => void run(() => folderAction(action))}>{t((['favorite.newFolder', 'favorite.renameFolder', 'favorite.deleteFolder'] as const)[i])}</button>)}
    </div>
    <div className="cron-favorite-form">
      <input aria-label={t('common.name')} placeholder={t('common.name')} value={name} onChange={event => setName(event.target.value)} />
      <input aria-label={t(kind === 'cron' ? 'cron.expression' : 'regex.expression')} value={value} onChange={event => setValue(event.target.value)} />
      <input aria-label={words[3]} placeholder={words[3]} value={description} onChange={event => setDescription(event.target.value)} />
      <button className="dialog-button" disabled={!name.trim() || !value.trim() || !folderId} onClick={() => void run(async () => { await window.mootool.saveFavorite({ id: editing, kind, folderId, name, value, description }); setEditing(undefined); setName(''); await load() })}>{words[4]}</button>
      <button className="dialog-button" onClick={() => { setEditing(undefined); setName(''); setValue(currentValue); setDescription('') }}>{words[5]}</button>
    </div>
    <div className="cron-favorite-toolbar">
      <label><input type="checkbox" aria-label={language === 'zh-CN' ? '全选' : language === 'ja-JP' ? 'すべて選択' : 'Select all'} checked={records.length > 0 && selected.length === records.length} onChange={event => setSelected(event.target.checked ? records.map(record => record.id) : [])} />{selected.length} / {records.length}</label>
      <button className="dialog-button" disabled={!selected.length} onClick={() => void run(async () => {
        if (!await dialog.confirm(`${t('common.action.delete')} (${selected.length})`, { danger: true })) return
        for (const id of selected) await window.mootool.deleteFavorite(id)
        setEditing(undefined); await load()
      })}>{t('common.action.delete')}</button>
      {([-1, 1] as const).map((direction, i) => <button className="dialog-button" key={direction} disabled={!selected.length || selected.includes(records[direction === -1 ? 0 : records.length - 1]?.id)} onClick={() => void run(async () => {
        const ordered = records.filter(record => selected.includes(record.id))
        if (direction === 1) ordered.reverse()
        for (const record of ordered) await window.mootool.moveFavorite(record.id, direction)
        await load()
      })}>{words[i + 1]}</button>)}
    </div>
    {error && <p role="alert" className="result-status--error">{error}</p>}
    <div className="favorite-list">{records.length === 0 ? <p>{t('favorite.empty')}</p> : records.map((record, index) => <article className="favorite-item" key={record.id}>
      <input type="checkbox" aria-label={record.name} checked={selected.includes(record.id)} onChange={event => setSelected(current => event.target.checked ? [...current, record.id] : current.filter(id => id !== record.id))} />
      <button type="button" onClick={() => { onApply(record.value); onClose() }}><strong>{record.name}</strong><code>{record.value}</code><span>{record.description}</span></button>
      <button className="dialog-button" onClick={() => { setEditing(record.id); setName(record.name); setValue(record.value); setDescription(record.description) }}>{words[0]}</button>
      {([-1, 1] as const).map((direction, i) => <button className="dialog-button" key={direction} disabled={direction === -1 ? index === 0 : index === records.length - 1} onClick={() => void run(async () => { await window.mootool.moveFavorite(record.id, direction); await load() })}>{words[i + 1]}</button>)}
      <button className="dialog-button" onClick={() => void run(async () => { await window.mootool.deleteFavorite(record.id); if (editing === record.id) setEditing(undefined); await load() })}>{t('common.action.delete')}</button>
    </article>)}</div>
  </Dialog>
}
