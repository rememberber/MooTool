import { useMemo, useState } from 'react'
import { Dialog } from '@/shared/components/Dialog'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { listJsonPaths } from './jsonTools'

type JsonPathPickerProps = {
  content: string
  open: boolean
  onClose: () => void
  onChoose: (path: string) => void
}

export function JsonPathPicker({ content, open, onClose, onChoose }: JsonPathPickerProps) {
  const { t } = useI18n()
  const entries = useMemo(() => {
    try {
      return listJsonPaths(content, t)
    } catch {
      return []
    }
  }, [content, t])
  const [selectedPath, setSelectedPath] = useState('$')
  const selected = entries.find((entry) => entry.path === selectedPath)

  return (
    <Dialog
      title={t('json.pathPicker.title')}
      open={open}
      width={760}
      onClose={onClose}
      footer={(
        <>
          <button className="dialog-button" type="button" onClick={onClose}>{t('common.cancel')}</button>
          <button className="dialog-button dialog-button--primary" type="button" disabled={!selected} onClick={() => { if (selected) onChoose(selected.path) }}>
            {t('json.pathPicker.use')}
          </button>
        </>
      )}
    >
      <div className="path-picker">
        <div className="path-picker__tree">
          {entries.map((entry) => (
            <button
              className={entry.path === selectedPath ? 'path-node path-node--selected' : 'path-node'}
              type="button"
              key={entry.path}
              style={{ paddingLeft: 10 + entry.depth * 18 }}
              onClick={() => setSelectedPath(entry.path)}
              onDoubleClick={() => onChoose(entry.path)}
            >
              <span>{entry.label}</span><small>{entry.path}</small>
            </button>
          ))}
        </div>
        <div className="path-picker__preview">
          <label>{t('json.pathPicker.path')}</label>
          <code>{selected?.path ?? ''}</code>
          <label>{t('json.pathPicker.preview')}</label>
          <pre>{formatPreview(selected?.value)}</pre>
        </div>
      </div>
    </Dialog>
  )
}

function formatPreview(value: unknown): string {
  if (value === undefined) return ''
  if (typeof value === 'string') return value
  return JSON.stringify(value, null, 2)
}
