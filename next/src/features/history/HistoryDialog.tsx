import { Copy, Search, Trash2 } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Dialog } from '@/shared/components/Dialog'
import type { FuncHistoryRecord } from '@/shared/contracts/history'
import { useToast } from '@/shared/feedback/ToastProvider'
import { useI18n } from '@/shared/i18n/I18nProvider'

type HistoryDialogProps = {
  funcType: string
  open: boolean
  onClose: () => void
  onApply: (value: string) => void
  onApplyRecord?: (record: FuncHistoryRecord) => void
}

export function HistoryDialog({ funcType, open, onClose, onApply, onApplyRecord }: HistoryDialogProps) {
  const { language, t } = useI18n()
  const toast = useToast()
  const [records, setRecords] = useState<FuncHistoryRecord[]>([])
  const [query, setQuery] = useState('')

  const load = useCallback(async () => {
    const nextRecords = await window.mootool.listHistory({ funcType, keyword: query })
    setRecords(nextRecords)
  }, [funcType, query])

  useEffect(() => {
    if (!open) return
    const timer = window.setTimeout(() => { void load() }, 120)
    return () => window.clearTimeout(timer)
  }, [load, open])

  async function copy(value: string): Promise<void> {
    await navigator.clipboard.writeText(value)
    toast.success(t('json.notice.copied'))
  }

  async function deleteRecord(id: number): Promise<void> {
    await window.mootool.deleteHistory(id)
    await load()
  }

  async function clearAll(): Promise<void> {
    if (!window.confirm(t('history.confirmClear'))) return
    await window.mootool.clearHistory(funcType)
    await load()
  }

  return (
    <Dialog
      title={t('history.title')}
      open={open}
      width={760}
      onClose={onClose}
      footer={(
        <>
          <button className="dialog-button dialog-button--danger" type="button" disabled={records.length === 0} onClick={() => { void clearAll() }}>
            <Trash2 size={14} />{t('history.clearAll')}
          </button>
          <button className="dialog-button" type="button" onClick={onClose}>{t('common.close')}</button>
        </>
      )}
    >
      <div className="history-search">
        <Search size={15} />
        <input value={query} aria-label={t('history.search')} placeholder={t('history.search')} onChange={(event) => setQuery(event.target.value)} />
      </div>
      <div className="history-list">
        {records.length === 0 ? <div className="history-empty">{t('history.empty')}</div> : records.map((record) => (
          <article className="history-item" key={record.id}>
            <button className="history-item__main" type="button" onClick={() => { onApplyRecord ? onApplyRecord(record) : onApply(record.outputText || record.inputText); onClose() }}>
              <strong>{record.summary}</strong>
              <span>{new Date(record.createTime.replace(' ', 'T')).toLocaleString(language)}</span>
              <p>{record.outputText || record.inputText}</p>
            </button>
            <div className="history-item__actions">
              <button className="icon-button" type="button" aria-label={t('history.copyInput')} onClick={() => { void copy(record.inputText) }}><Copy size={14} /></button>
              <button className="icon-button" type="button" aria-label={t('history.copyOutput')} onClick={() => { void copy(record.outputText) }}><Copy size={14} /></button>
              <button className="icon-button" type="button" aria-label={t('history.delete')} onClick={() => { void deleteRecord(record.id) }}><Trash2 size={14} /></button>
            </div>
          </article>
        ))}
      </div>
    </Dialog>
  )
}
