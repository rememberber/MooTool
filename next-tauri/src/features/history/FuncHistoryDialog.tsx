import { Copy, Search, Trash2, X } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { localDataApi } from '../../platform/api/localDataApi'
import { clipboardApi } from '../../platform/api/clipboardApi'
import type { FuncHistoryRecord } from '../../platform/contracts/localData'
import { useDesktopDialog } from '../../shared/DesktopDialogProvider'
import { funcHistoryMessages } from './funcHistoryMessages'

export function FuncHistoryDialog({ funcType, onClose, onApply }: {
  funcType: string
  onClose: () => void
  onApply: (record: FuncHistoryRecord) => void
}) {
  const { locale, t } = useLocalizedMessages(funcHistoryMessages)
  const dialog = useDesktopDialog()
  const [records, setRecords] = useState<FuncHistoryRecord[]>([])
  const [query, setQuery] = useState('')
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    try {
      setRecords(await localDataApi.listFuncHistory(funcType, query))
      setError('')
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause))
    }
  }, [funcType, query])

  useEffect(() => {
    const timer = window.setTimeout(() => { void load() }, 120)
    return () => window.clearTimeout(timer)
  }, [load])

  async function clearAll(): Promise<void> {
    if (!await dialog.confirm(t('confirmClear'), { dangerous: true })) return
    await localDataApi.clearFuncHistory(funcType)
    await load()
  }

  return (
    <section className="http-history-modal func-history-modal" role="dialog" aria-modal="true" aria-label={t('title')}>
      <div>
        <header>
          <div><strong>{t('title')}</strong></div>
          <button type="button" aria-label={t('close')} onClick={onClose}><X /></button>
        </header>
        <label>
          <Search />
          <input autoFocus aria-label={t('search')} value={query} placeholder={t('search')} onChange={(event) => setQuery(event.target.value)} />
        </label>
        <main>
          {error && <p>{error}</p>}
          {records.length === 0 && !error
            ? <p>{t('empty')}</p>
            : records.map((record) => (
              <article key={record.id}>
                <button type="button" onClick={() => onApply(record)}>
                  <strong>{record.summary}</strong>
                  <span>{new Date(record.createTime.replace(' ', 'T')).toLocaleString(locale)}</span>
                  <small>{record.outputText || record.inputText}</small>
                </button>
                <button type="button" aria-label={t('copyInput')} onClick={() => void clipboardApi.writeText(record.inputText)}><Copy /></button>
                <button type="button" aria-label={t('copyOutput')} onClick={() => void clipboardApi.writeText(record.outputText)}><Copy /></button>
                <button type="button" aria-label={t('delete')} onClick={() => void localDataApi.deleteFuncHistory(record.id).then(load)}><Trash2 /></button>
              </article>
            ))}
        </main>
        <footer>
          <button type="button" disabled={records.length === 0} onClick={() => void clearAll()}><Trash2 />{t('clearAll')}</button>
          <button type="button" onClick={onClose}>{t('close')}</button>
        </footer>
      </div>
    </section>
  )
}
