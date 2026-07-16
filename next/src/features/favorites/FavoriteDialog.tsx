import { Star, Trash2 } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Dialog } from '@/shared/components/Dialog'
import type { FavoriteKind, FavoriteRecord } from '@/shared/contracts/favorites'
import { useToast } from '@/shared/feedback/ToastProvider'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function FavoriteDialog({ kind, open, currentValue, onClose, onApply }: {
  kind: FavoriteKind
  open: boolean
  currentValue: string
  onClose: () => void
  onApply: (value: string) => void
}) {
  const { t } = useI18n()
  const toast = useToast()
  const [records, setRecords] = useState<FavoriteRecord[]>([])
  const [name, setName] = useState('')

  const load = useCallback(async () => setRecords(await window.mootool.listFavorites(kind)), [kind])
  useEffect(() => { if (open) void load() }, [load, open])

  async function save(): Promise<void> {
    if (!name.trim() || !currentValue.trim()) return
    await window.mootool.saveFavorite({ kind, name, value: currentValue })
    setName('')
    await load()
    toast.success(t('favorite.saved'))
  }

  async function remove(id: number): Promise<void> {
    await window.mootool.deleteFavorite(id)
    await load()
    toast.success(t('favorite.deleted'))
  }

  return (
    <Dialog title={t('favorite.title')} open={open} width={620} onClose={onClose} footer={<button className="dialog-button" type="button" onClick={onClose}>{t('common.close')}</button>}>
      <div className="favorite-create">
        <input value={name} aria-label={t('common.name')} placeholder={t('favorite.namePlaceholder')} onChange={(event) => setName(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') void save() }} />
        <button className="dialog-button dialog-button--primary" type="button" disabled={!name.trim() || !currentValue.trim()} onClick={() => { void save() }}><Star size={14} />{t('favorite.add')}</button>
      </div>
      <div className="favorite-list">
        {records.length === 0 ? <div className="history-empty">{t('favorite.empty')}</div> : records.map((record) => (
          <article className="favorite-item" key={record.id}>
            <button type="button" onClick={() => { onApply(record.value); onClose() }}><strong>{record.name}</strong><code>{record.value}</code></button>
            <button className="icon-button" type="button" aria-label={t('common.action.delete')} onClick={() => { void remove(record.id) }}><Trash2 size={14} /></button>
          </article>
        ))}
      </div>
    </Dialog>
  )
}
