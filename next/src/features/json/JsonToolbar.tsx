import { Copy, Download, Eraser, FolderOpen, History, Minimize2, MoreHorizontal, Search, Sparkles, WrapText } from 'lucide-react'
import type { ReactNode } from 'react'
import { FindReplaceBar } from '@/shared/components/FindReplaceBar'
import { Tooltip } from '@/shared/components/Tooltip'
import type { FindReplaceOptions } from '@/shared/components/findReplace'
import { useI18n } from '@/shared/i18n/I18nProvider'

type JsonToolbarProps = {
  wrap: boolean
  copied: boolean
  findOpen: boolean
  findText: string
  replaceText: string
  findOptions: FindReplaceOptions
  matchCount: number
  replacedCount: number
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
  onFindTextChange: (value: string) => void
  onReplaceTextChange: (value: string) => void
  onFindOptionsChange: (options: FindReplaceOptions) => void
  onFind: () => void
  onFindPrevious: () => void
  onFindNext: () => void
  onReplace: () => void
  onReplaceAll: () => void
  onCloseFind: () => void
}

export function JsonToolbar({
  wrap,
  copied,
  findOpen,
  findText,
  replaceText,
  findOptions,
  matchCount,
  replacedCount,
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
  onFindTextChange,
  onReplaceTextChange,
  onFindOptionsChange,
  onFind,
  onFindPrevious,
  onFindNext,
  onReplace,
  onReplaceAll,
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
        <FindReplaceBar
          className="json-findbar"
          findText={findText}
          replaceText={replaceText}
          options={findOptions}
          matchCount={matchCount}
          replacedCount={replacedCount}
          onFindTextChange={onFindTextChange}
          onReplaceTextChange={onReplaceTextChange}
          onOptionsChange={onFindOptionsChange}
          onFind={onFind}
          onFindPrevious={onFindPrevious}
          onFindNext={onFindNext}
          onReplace={onReplace}
          onReplaceAll={onReplaceAll}
          onClose={onCloseFind}
        />
      )}
    </>
  )
}

function IconAction({ label, onClick, children, className = '' }: { label: string; onClick: () => void; children: ReactNode; className?: string }) {
  return <Tooltip content={label}><button className={`toolbar-button toolbar-button--icon ${className}`} type="button" aria-label={label} onClick={onClick}>{children}</button></Tooltip>
}
