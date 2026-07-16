import { Copy, History, Trash2 } from 'lucide-react'
import type { ReactNode } from 'react'
import { Tooltip } from './Tooltip'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function ToolPageHeader({ title, actions }: { title: string; actions?: ReactNode }) {
  return (
    <div className="tool-page__header">
      <h1>{title}</h1>
      {actions && <div className="tool-header-actions">{actions}</div>}
    </div>
  )
}

export function ToolTabs<T extends string>({ tabs, active, onChange }: {
  tabs: ReadonlyArray<{ id: T; label: string }>
  active: T
  onChange: (id: T) => void
}) {
  return (
    <div className="tool-tabs" role="tablist">
      {tabs.map((tab) => (
        <button
          className={active === tab.id ? 'tool-tab tool-tab--active' : 'tool-tab'}
          type="button"
          role="tab"
          aria-selected={active === tab.id}
          key={tab.id}
          onClick={() => onChange(tab.id)}
        >
          {tab.label}
        </button>
      ))}
    </div>
  )
}

export function ToolHeaderButtons({ onHistory, onClear, onCopy, copyLabel }: {
  onHistory?: () => void
  onClear?: () => void
  onCopy?: () => void
  copyLabel?: string
}) {
  const { t } = useI18n()
  return (
    <>
      {onHistory && <button className="toolbar-button" type="button" onClick={onHistory}><History size={14} />{t('common.action.history')}</button>}
      {onCopy && <Tooltip content={copyLabel ?? t('common.action.copy')}><button className="toolbar-button toolbar-button--icon" type="button" aria-label={copyLabel ?? t('common.action.copy')} onClick={onCopy}><Copy size={14} /></button></Tooltip>}
      {onClear && <Tooltip content={t('common.action.clear')}><button className="toolbar-button toolbar-button--icon" type="button" aria-label={t('common.action.clear')} onClick={onClear}><Trash2 size={14} /></button></Tooltip>}
    </>
  )
}

export function TextPane({ label, value, placeholder, readOnly, onChange }: {
  label: string
  value: string
  placeholder?: string
  readOnly?: boolean
  onChange?: (value: string) => void
}) {
  return (
    <label className="text-pane">
      <span>{label}</span>
      <textarea
        value={value}
        placeholder={placeholder}
        readOnly={readOnly}
        spellCheck={false}
        onChange={onChange ? (event) => onChange(event.target.value) : undefined}
      />
    </label>
  )
}
