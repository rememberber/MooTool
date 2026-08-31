import { Save, Star, Trash2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { localDataApi } from '../../platform/api/localDataApi'
import type { ToolFavorite } from '../../platform/contracts/localData'
import { errorMessage } from '../../shared/errors'
import { useDesktopDialog } from '../../shared/DesktopDialogProvider'
import { toolFavoriteMessages } from './toolFavoriteMessages'

export function ToolFavoriteBar({ toolId, defaultName, payload, onApply }: {
  toolId: string
  defaultName: string
  payload: Record<string, unknown>
  onApply(payload: Record<string, unknown>): void
}) {
  const { t } = useLocalizedMessages(toolFavoriteMessages)
  const dialog = useDesktopDialog()
  const [items, setItems] = useState<ToolFavorite[]>([])
  const [selectedId, setSelectedId] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    void localDataApi.listToolFavorites(toolId).then((loaded) => {
      if (!cancelled) { setItems(loaded); setError('') }
    }).catch((cause: unknown) => { if (!cancelled) setError(errorMessage(cause)) })
    return () => { cancelled = true }
  }, [toolId])

  function apply(id: string): void {
    setSelectedId(id)
    const item = items.find((candidate) => candidate.id === id)
    if (!item) return
    try {
      const value = JSON.parse(item.payloadJson) as unknown
      if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error(t('error.payload'))
      onApply(value as Record<string, unknown>)
      setError('')
    } catch (cause) { setError(errorMessage(cause)) }
  }

  async function save(): Promise<void> {
    const name = (await dialog.prompt(t('prompt.name'), defaultName.slice(0, 80)))?.trim()
    if (!name) return
    const now = Date.now()
    try {
      const saved = await localDataApi.saveToolFavorite({
        id: crypto.randomUUID(), toolId, name, payloadJson: JSON.stringify(payload), createdAt: now, updatedAt: now
      })
      setItems((current) => [saved, ...current.filter((item) => item.id !== saved.id && item.name !== saved.name)])
      setSelectedId(saved.id)
      setError('')
    } catch (cause) { setError(errorMessage(cause)) }
  }

  async function remove(): Promise<void> {
    const selected = items.find((item) => item.id === selectedId)
    if (!selected || !await dialog.confirm(t('confirm.delete', { name: selected.name }), { dangerous: true })) return
    try {
      await localDataApi.deleteToolFavorite(selected.id)
      setItems((current) => current.filter((item) => item.id !== selected.id))
      setSelectedId('')
      setError('')
    } catch (cause) { setError(errorMessage(cause)) }
  }

  return <div className="tool-favorite-bar" title={error || t('label')}>
    <Star />
    <select aria-label={t('label')} value={selectedId} onChange={(event) => apply(event.target.value)}>
      <option value="">{t('empty')}</option>
      {items.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}
    </select>
    <button type="button" aria-label={t('save')} title={t('save')} onClick={() => void save()}><Save /></button>
    <button type="button" aria-label={t('delete')} title={t('delete')} disabled={!selectedId} onClick={() => void remove()}><Trash2 /></button>
    {error && <span role="alert">!</span>}
  </div>
}
