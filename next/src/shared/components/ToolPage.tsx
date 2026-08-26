import { Copy, History, Trash2 } from 'lucide-react'
import type { ReactNode } from 'react'
import { TextCodeEditor } from './TextCodeEditor'
import { Tooltip } from './Tooltip'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function WorkspaceDragZone({ className = '' }: { className?: string }) {
  return (
    <div
      className={['workspace-drag-zone', className].filter(Boolean).join(' ')}
      data-window-drag-zone
      aria-hidden="true"
    />
  )
}

export function ToolPageHeader({ title, actions }: { title: string; actions?: ReactNode }) {
  return (
    <div className={actions ? 'tool-page__header tool-page__header--actions' : 'tool-page__header tool-page__header--semantic'}>
      <h1 className="visually-hidden">{title}</h1>
      {actions && <WorkspaceDragZone />}
      {actions && <div className="tool-header-actions">{actions}</div>}
    </div>
  )
}

export function ToolTabs<T extends string>({ tabs, active, onChange, windowDrag = false, actions }: {
  tabs: ReadonlyArray<{ id: T; label: string }>
  active: T
  onChange: (id: T) => void
  windowDrag?: boolean
  actions?: ReactNode
}) {
  return (
    <div className={windowDrag ? 'tool-tabs tool-tabs--window-drag' : 'tool-tabs'} role="tablist">
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
      {windowDrag && <WorkspaceDragZone className="workspace-drag-zone--tabs" />}
      {actions && <div className="tool-tabs__actions">{actions}</div>}
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
    <div className="text-pane">
      <span>{label}</span>
      <TextCodeEditor
        ariaLabel={label}
        value={value}
        placeholder={placeholder}
        readOnly={readOnly}
        onChange={onChange}
      />
    </div>
  )
}
