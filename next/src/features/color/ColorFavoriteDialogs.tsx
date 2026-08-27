import { Star, Trash2 } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Dialog } from '@/shared/components/Dialog'
import type { FavoriteRecord } from '@/shared/contracts/favorites'
import { useToast } from '@/shared/feedback/ToastProvider'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function SaveColorFavoriteDialog({ color, open, onClose }: {
  color: string
  open: boolean
  onClose: () => void
}) {
  const { t } = useI18n()
  const toast = useToast()
  const [name, setName] = useState('')

  useEffect(() => {
    if (open) setName(`Color-${color}`)
  }, [color, open])

  async function save(): Promise<void> {
    if (!name.trim()) return
    await window.mootool.saveFavorite({ kind: 'color', name: name.trim(), value: color })
    toast.success(t('favorite.saved'))
    onClose()
  }

  return (
    <Dialog
      title={t('color.favoriteDialog')}
      open={open}
      width={420}
      onClose={onClose}
      footer={(
        <>
          <button className="dialog-button" type="button" onClick={onClose}>{t('common.cancel')}</button>
          <button className="dialog-button dialog-button--primary" type="button" disabled={!name.trim()} onClick={() => { void save() }}>
            <Star size={14} />{t('color.favorite')}
          </button>
        </>
      )}
    >
      <div className="color-favorite-preview">
        <span style={{ background: color }} />
        <strong>{color}</strong>
      </div>
      <label className="color-favorite-name">
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
  const [records, setRecords] = useState<FavoriteRecord[]>([])
  const load = useCallback(async () => setRecords(await window.mootool.listFavorites('color')), [])

  useEffect(() => { if (open) void load() }, [load, open])

  async function remove(id: number): Promise<void> {
    await window.mootool.deleteFavorite(id)
    await load()
    toast.success(t('favorite.deleted'))
  }

  return (
    <Dialog
      title={t('color.favorites')}
      open={open}
      width={620}
      onClose={onClose}
      footer={<button className="dialog-button" type="button" onClick={onClose}>{t('common.close')}</button>}
    >
      <div className="favorite-list color-favorite-list">
        {records.length === 0 ? <div className="history-empty">{t('favorite.empty')}</div> : records.map((record) => (
          <article className="favorite-item color-favorite-item" key={record.id}>
            <button type="button" onClick={() => { onApply(record.value); onClose() }}>
              <span className="color-favorite-item__swatch" style={{ background: record.value }} />
              <span className="color-favorite-item__text"><strong>{record.name}</strong><code>{record.value}</code></span>
            </button>
            <button className="icon-button" type="button" aria-label={t('common.action.delete')} onClick={() => { void remove(record.id) }}><Trash2 size={14} /></button>
          </article>
        ))}
      </div>
    </Dialog>
  )
}
