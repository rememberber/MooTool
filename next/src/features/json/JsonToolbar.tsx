import { Copy, Download, Eraser, FolderOpen, History, Minimize2, MoreHorizontal, Search, Sparkles, WrapText, X } from 'lucide-react'
import type { ReactNode } from 'react'
import { Tooltip } from '@/shared/components/Tooltip'
import { useI18n } from '@/shared/i18n/I18nProvider'

type JsonToolbarProps = {
  wrap: boolean
  copied: boolean
  findOpen: boolean
  findQuery: string
  findMatchCount: number
  onFormat: () => void
  onCompress: () => void
  onToggleWrap: () => void
  onCopy: () => void
  onToggleFind: () => void
  onImport: () => void
  onExport: () => void
  onHistory: () => void
  onToggleInspector: () => void
  onClear: () => void
  onFindQueryChange: (value: string) => void
  onNextMatch: () => void
  onCloseFind: () => void
}

export function JsonToolbar({
  wrap,
  copied,
  findOpen,
  findQuery,
  findMatchCount,
  onFormat,
  onCompress,
  onToggleWrap,
  onCopy,
  onToggleFind,
  onImport,
  onExport,
  onHistory,
  onToggleInspector,
  onClear,
  onFindQueryChange,
  onNextMatch,
  onCloseFind
}: JsonToolbarProps) {
  const { t } = useI18n()
  return (
    <>
      <div className="editor-toolbar">
        <button className="toolbar-button toolbar-button--primary" type="button" onClick={onFormat}><Sparkles size={14} />{t('json.action.format')}</button>
        <button className="toolbar-button" type="button" onClick={onCompress}><Minimize2 size={14} />{t('json.action.compress')}</button>
        <IconAction label={wrap ? t('json.action.wrap') : t('json.action.nowrap')} onClick={onToggleWrap}><WrapText size={14} /></IconAction>
        <IconAction label={copied ? t('json.action.copied') : t('json.action.copy')} onClick={onCopy}><Copy size={14} /></IconAction>
        <span className="toolbar-divider" />
        <IconAction label={t('json.action.find')} onClick={onToggleFind}><Search size={14} /></IconAction>
        <IconAction label={t('json.action.import')} onClick={onImport}><FolderOpen size={14} /></IconAction>
        <IconAction label={t('json.action.export')} onClick={onExport}><Download size={14} /></IconAction>
        <IconAction label={t('json.action.history')} onClick={onHistory}><History size={14} /></IconAction>
        <IconAction label={t('json.action.more')} onClick={onToggleInspector}><MoreHorizontal size={14} /></IconAction>
        <IconAction className="toolbar-button--quiet" label={t('json.action.clear')} onClick={onClear}><Eraser size={14} /></IconAction>
      </div>
      {findOpen && (
        <div className="json-findbar">
          <Search size={14} />
          <input
            aria-label={t('json.find.placeholder')}
            placeholder={t('json.find.placeholder')}
            value={findQuery}
            onChange={(event) => onFindQueryChange(event.target.value)}
            onKeyDown={(event) => { if (event.key === 'Enter') onNextMatch() }}
          />
          <span>{t('json.find.matches', { count: String(findMatchCount) })}</span>
          <Tooltip content={t('json.find.next')}><button className="icon-ghost" type="button" aria-label={t('json.find.next')} onClick={onNextMatch}><Search size={14} /></button></Tooltip>
          <Tooltip content={t('json.find.close')}><button className="icon-ghost" type="button" aria-label={t('json.find.close')} onClick={onCloseFind}><X size={14} /></button></Tooltip>
        </div>
      )}
    </>
  )
}

function IconAction({ label, onClick, children, className = '' }: { label: string; onClick: () => void; children: ReactNode; className?: string }) {
  return <Tooltip content={label}><button className={`toolbar-button toolbar-button--icon ${className}`} type="button" aria-label={label} onClick={onClick}>{children}</button></Tooltip>
}
