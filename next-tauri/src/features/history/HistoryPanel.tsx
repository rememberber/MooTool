import { CheckCircle2, Circle, Search, Trash2, TriangleAlert, X } from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { toolCatalog } from '../../app/toolCatalog'
import { useI18n } from '../../app/i18n'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { historyApi } from '../../platform/api/historyApi'
import type { OperationHistory, OperationStatus } from '../../platform/contracts/history'
import { errorMessage } from '../../shared/errors'
import { historyMessages } from './historyMessages'

export function HistoryPanel({ limit, onClose }: { limit: number; onClose: () => void }) {
  const { toolTitle } = useI18n()
  const { locale, t } = useLocalizedMessages(historyMessages)
  const [items, setItems] = useState<OperationHistory[]>([])
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState<OperationStatus | 'all'>('all')
  const [error, setError] = useState('')
  const load = useCallback(async () => {
    try {
      setItems(await historyApi.list(limit))
      setError('')
    } catch (cause) { setError(errorMessage(cause)) }
  }, [limit])

  useEffect(() => {
    void load()
    let dispose: (() => void) | undefined
    void historyApi.subscribe(() => void load()).then((value) => { dispose = value })
    return () => dispose?.()
  }, [load])

  const visible = useMemo(() => {
    const normalized = query.trim().toLocaleLowerCase()
    return items.filter((item) => (status === 'all' || item.status === status) && (!normalized || [resolveToolTitle(item.toolId), item.action, item.summary].some((value) => value.toLocaleLowerCase().includes(normalized))))
  }, [items, query, status, toolTitle, t])

  function resolveToolTitle(toolId: string): string {
    const tool = toolCatalog.find((candidate) => candidate.id === toolId)
    return tool ? toolTitle(tool) : toolId === 'system-data' ? t('tool.systemData') : toolId
  }

  return (
    <div className="history-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
      <aside className="history-panel" role="dialog" aria-modal="true" aria-label={t('aria.dialog')}>
        <header><div><span className="eyebrow">TAURI SQLITE HISTORY</span><h2>{t('title')}</h2><p>{t('description')}</p></div><button type="button" aria-label={t('action.close')} onClick={onClose}><X /></button></header>
        <div className="history-toolbar"><label><Search /><input autoFocus value={query} placeholder={t('search.placeholder')} onChange={(event) => setQuery(event.target.value)} /></label><select value={status} onChange={(event) => setStatus(event.target.value as OperationStatus | 'all')}><option value="all">{t('status.all')}</option><option value="info">{t('status.info')}</option><option value="success">{t('status.success')}</option><option value="error">{t('status.error')}</option></select><button type="button" disabled={!items.length} onClick={() => { if (window.confirm(t('confirm.clear'))) void historyApi.clear() }}><Trash2 />{t('action.clear')}</button></div>
        <div className="history-list">{visible.length ? visible.map((item) => <article key={item.id}><StatusIcon status={item.status} /><div><header><strong>{resolveToolTitle(item.toolId)}</strong><span>{item.action}</span><time>{new Date(item.createdAt).toLocaleString(locale)}</time></header><p>{item.summary || '—'}</p></div><button type="button" aria-label={t('action.delete')} onClick={() => void historyApi.delete(item.id)}><Trash2 /></button></article>) : <div className="history-panel-empty">{t('empty')}</div>}</div>
        <footer><span>{t('footer.limit', { count: limit })}</span><span>{t('footer.count', { count: items.length })}</span>{error && <strong>{error}</strong>}</footer>
      </aside>
    </div>
  )
}

function StatusIcon({ status }: { status: OperationStatus }) {
  if (status === 'success') return <CheckCircle2 className="history-icon history-icon--success" />
  if (status === 'error') return <TriangleAlert className="history-icon history-icon--error" />
  return <Circle className="history-icon" />
}
