import { ColorFavoritesManager } from './ColorFavoritesManager'
import { FolderPlus, Star } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Dialog } from '@/shared/components/Dialog'
import type { FavoriteFolderRecord } from '@/shared/contracts/favorites'
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

export function ColorFavoritesDialog({ open, currentValue, onClose, onApply }: {
  open: boolean; currentValue: string; onClose: () => void; onApply: (value: string) => void
}) {
  return <ColorFavoritesManager open={open} currentValue={currentValue} onClose={onClose} onApply={onApply} />
}
