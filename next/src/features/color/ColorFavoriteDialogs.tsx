import { FolderPlus, Pencil, Star, Trash2 } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Dialog } from '@/shared/components/Dialog'
import type { FavoriteFolderRecord, FavoriteRecord } from '@/shared/contracts/favorites'
import { useDesktopDialog } from '@/shared/feedback/DesktopDialogProvider'
import { useToast } from '@/shared/feedback/ToastProvider'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function SaveColorFavoriteDialog({ color, open, onClose }: {
  color: string
  open: boolean
  onClose: () => void
}) {
  const { t } = useI18n()
  const toast = useToast()
  const desktopDialog = useDesktopDialog()
  const [name, setName] = useState('')
  const [folders, setFolders] = useState<FavoriteFolderRecord[]>([])
  const [folderId, setFolderId] = useState<number>()

  const loadFolders = useCallback(async (preferredId?: number) => {
    const next = await window.mootool.listFavoriteFolders('color')
    setFolders(next)
    setFolderId((current) => next.some((folder) => folder.id === (preferredId ?? current)) ? (preferredId ?? current) : next[0]?.id)
  }, [])

  useEffect(() => {
    if (!open) return
    setName(`Color-${color}`)
    void loadFolders()
  }, [color, loadFolders, open])

  async function createFolder(): Promise<void> {
    const title = await desktopDialog.prompt(t('favorite.folderName'), {
      title: t('favorite.newFolder'),
      confirmLabel: t('common.add')
    })
    if (!title?.trim()) return
    try {
      const folder = await window.mootool.createFavoriteFolder({ kind: 'color', title: title.trim() })
      await loadFolders(folder.id)
    } catch {
      toast.error(t('favorite.duplicateFolder'))
    }
  }

  async function save(): Promise<void> {
    if (!name.trim() || !folderId) return
    await window.mootool.saveFavorite({ kind: 'color', folderId, name: name.trim(), value: color })
    toast.success(t('favorite.saved'))
    onClose()
  }

  return (
    <Dialog
      title={t('color.favoriteDialog')}
      open={open}
      width={440}
      onClose={onClose}
      footer={(
        <>
          <button className="dialog-button" type="button" onClick={onClose}>{t('common.cancel')}</button>
          <button className="dialog-button dialog-button--primary" type="button" disabled={!name.trim() || !folderId} onClick={() => { void save() }}>
            <Star size={14} />{t('color.favorite')}
          </button>
        </>
      )}
    >
      <div className="color-favorite-preview">
        <span style={{ background: color }} />
        <strong>{color}</strong>
      </div>
      <label className="color-favorite-field">
        <span>{t('favorite.folder')}</span>
        <span className="color-favorite-folder-select">
          <select aria-label={t('favorite.folder')} value={folderId ?? ''} onChange={(event) => setFolderId(Number(event.target.value))}>
            {folders.map((folder) => <option key={folder.id} value={folder.id}>{folder.title}</option>)}
          </select>
          <button className="dialog-button" type="button" onClick={() => { void createFolder() }}><FolderPlus size={14} />{t('favorite.newFolder')}</button>
        </span>
      </label>
      <label className="color-favorite-field">
        <span>{t('common.name')}</span>
        <input
          autoFocus
          value={name}
          aria-label={t('common.name')}
          onChange={(event) => setName(event.target.value)}
          onKeyDown={(event) => { if (event.key === 'Enter') void save() }}
        />
      </label>
    </Dialog>
  )
}

export function ColorFavoritesDialog({ open, onClose, onApply }: {
  open: boolean
  onClose: () => void
  onApply: (value: string) => void
}) {
  const { t } = useI18n()
  const toast = useToast()
  const desktopDialog = useDesktopDialog()
  const [folders, setFolders] = useState<FavoriteFolderRecord[]>([])
  const [selectedFolderId, setSelectedFolderId] = useState<number>()
  const [records, setRecords] = useState<FavoriteRecord[]>([])

  const loadFolders = useCallback(async (preferredId?: number) => {
    const next = await window.mootool.listFavoriteFolders('color')
    setFolders(next)
    setSelectedFolderId((current) => next.some((folder) => folder.id === (preferredId ?? current)) ? (preferredId ?? current) : next[0]?.id)
  }, [])

  const loadRecords = useCallback(async (folderId: number) => {
    setRecords(await window.mootool.listFavorites('color', folderId))
  }, [])

  useEffect(() => { if (open) void loadFolders() }, [loadFolders, open])
  useEffect(() => {
    if (!open || !selectedFolderId) {
      setRecords([])
      return
    }
    void loadRecords(selectedFolderId)
  }, [loadRecords, open, selectedFolderId])

  async function createFolder(): Promise<void> {
    const title = await desktopDialog.prompt(t('favorite.folderName'), {
      title: t('favorite.newFolder'),
      confirmLabel: t('common.add')
    })
    if (!title?.trim()) return
    try {
      const folder = await window.mootool.createFavoriteFolder({ kind: 'color', title: title.trim() })
      await loadFolders(folder.id)
    } catch {
      toast.error(t('favorite.duplicateFolder'))
    }
  }

  async function renameFolder(folder: FavoriteFolderRecord): Promise<void> {
    const title = await desktopDialog.prompt(t('favorite.folderName'), {
      title: t('favorite.renameFolder'),
      confirmLabel: t('common.rename'),
      defaultValue: folder.title
    })
    if (!title?.trim() || title.trim() === folder.title) return
    try {
      await window.mootool.renameFavoriteFolder({ id: folder.id, title: title.trim() })
      await loadFolders(folder.id)
    } catch {
      toast.error(t('favorite.duplicateFolder'))
    }
  }

  async function deleteFolder(folder: FavoriteFolderRecord): Promise<void> {
    const confirmed = await desktopDialog.confirm(t('favorite.deleteFolderConfirm', { name: folder.title }), {
      title: t('favorite.deleteFolder'),
      confirmLabel: t('common.action.delete'),
      danger: true
    })
    if (!confirmed) return
    await window.mootool.deleteFavoriteFolder(folder.id)
    await loadFolders()
  }

  async function remove(id: number): Promise<void> {
    await window.mootool.deleteFavorite(id)
    if (selectedFolderId) await loadRecords(selectedFolderId)
    toast.success(t('favorite.deleted'))
  }

  return (
    <Dialog
      title={t('color.favorites')}
      open={open}
      width={760}
      onClose={onClose}
      footer={<button className="dialog-button" type="button" onClick={onClose}>{t('common.close')}</button>}
    >
      <div className="color-favorites-manager">
        <aside className="color-favorite-folders">
          <button className="color-favorite-folders__new" type="button" onClick={() => { void createFolder() }}>
            <FolderPlus size={14} />{t('favorite.newFolder')}
          </button>
          <div className="color-favorite-folders__list">
            {folders.map((folder) => (
              <article className={folder.id === selectedFolderId ? 'color-favorite-folder color-favorite-folder--active' : 'color-favorite-folder'} key={folder.id}>
                <button type="button" title={folder.title} onClick={() => setSelectedFolderId(folder.id)}>{folder.title}</button>
                <button className="icon-ghost" type="button" aria-label={`${t('favorite.renameFolder')} ${folder.title}`} onClick={() => { void renameFolder(folder) }}><Pencil size={12} /></button>
                <button className="icon-ghost" type="button" aria-label={`${t('favorite.deleteFolder')} ${folder.title}`} onClick={() => { void deleteFolder(folder) }}><Trash2 size={12} /></button>
              </article>
            ))}
          </div>
        </aside>
        <section className="favorite-list color-favorite-list">
          {records.length === 0 ? <div className="history-empty">{t('favorite.empty')}</div> : records.map((record) => (
            <article className="favorite-item color-favorite-item" key={record.id}>
              <button type="button" onClick={() => { onApply(record.value); onClose() }}>
                <span className="color-favorite-item__swatch" style={{ background: record.value }} />
                <span className="color-favorite-item__text"><strong>{record.name}</strong><code>{record.value}</code></span>
              </button>
              <button className="icon-button" type="button" aria-label={t('common.action.delete')} onClick={() => { void remove(record.id) }}><Trash2 size={14} /></button>
            </article>
          ))}
        </section>
      </div>
    </Dialog>
  )
}
