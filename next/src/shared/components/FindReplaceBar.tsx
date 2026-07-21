import { ChevronDown, ChevronUp, X } from 'lucide-react'
import { useEffect, useRef } from 'react'
import { Tooltip } from '@/shared/components/Tooltip'
import { useI18n } from '@/shared/i18n/I18nProvider'
import {
  defaultFindReplaceOptions,
  type FindReplaceOptions
} from './findReplace'

export type FindReplaceBarProps = {
  findText: string
  replaceText: string
  options?: FindReplaceOptions
  matchCount: number
  replacedCount: number
  className?: string
  autoFocus?: boolean
  onFindTextChange: (value: string) => void
  onReplaceTextChange: (value: string) => void
  onOptionsChange: (options: FindReplaceOptions) => void
  onFind: () => void
  onFindPrevious: () => void
  onFindNext: () => void
  onReplace: () => void
  onReplaceAll: () => void
  onClose: () => void
}

export function FindReplaceBar({
  findText,
  replaceText,
  options = defaultFindReplaceOptions,
  matchCount,
  replacedCount,
  className = '',
  autoFocus = true,
  onFindTextChange,
  onReplaceTextChange,
  onOptionsChange,
  onFind,
  onFindPrevious,
  onFindNext,
  onReplace,
  onReplaceAll,
  onClose
}: FindReplaceBarProps) {
  const { t } = useI18n()
  const findInputRef = useRef<HTMLInputElement>(null)
  const findEnabled = Boolean(findText)

  useEffect(() => {
    if (!autoFocus) return
    const input = findInputRef.current
    if (!input) return
    input.focus()
    input.select()
  }, [autoFocus])

  function patchOptions(patch: Partial<FindReplaceOptions>): void {
    onOptionsChange({ ...options, ...patch })
  }

  return (
    <div
      className={['find-replace-bar', className].filter(Boolean).join(' ')}
      onKeyDown={(event) => {
        if (event.key === 'ArrowUp') {
          event.preventDefault()
          onFindPrevious()
        } else if (event.key === 'ArrowDown') {
          event.preventDefault()
          onFindNext()
        } else if (event.key === 'Escape') {
          event.preventDefault()
          onClose()
        }
      }}
    >
      <div className="find-replace-bar__row">
        <input
          ref={findInputRef}
          aria-label={t('findReplace.findPlaceholder')}
          placeholder={t('findReplace.findPlaceholder')}
          value={findText}
          onChange={(event) => onFindTextChange(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              event.preventDefault()
              onFind()
            }
          }}
        />
        <button type="button" disabled={!findEnabled} onClick={onFind}>{t('findReplace.find')}</button>
        <div className="find-replace-bar__controls">
          <Tooltip content={t('findReplace.previous')}>
            <button className="icon-ghost" type="button" aria-label={t('findReplace.previous')} disabled={!findEnabled} onClick={onFindPrevious}>
              <ChevronUp size={14} />
            </button>
          </Tooltip>
          <Tooltip content={t('findReplace.next')}>
            <button className="icon-ghost" type="button" aria-label={t('findReplace.next')} disabled={!findEnabled} onClick={onFindNext}>
              <ChevronDown size={14} />
            </button>
          </Tooltip>
          <span className="find-replace-bar__count">{t('findReplace.foundPrefix')} {matchCount}</span>
          <label className="find-replace-bar__option">
            <input type="checkbox" checked={options.matchCase} onChange={(event) => patchOptions({ matchCase: event.target.checked })} />
            {t('findReplace.matchCase')}
          </label>
          <label className="find-replace-bar__option">
            <input type="checkbox" checked={options.wholeWord} onChange={(event) => patchOptions({ wholeWord: event.target.checked })} />
            {t('findReplace.wholeWord')}
          </label>
          <label className="find-replace-bar__option">
            <input type="checkbox" checked={options.regex} onChange={(event) => patchOptions({ regex: event.target.checked })} />
            {t('findReplace.regex')}
          </label>
        </div>
        <Tooltip content={t('common.close')}>
          <button className="icon-ghost find-replace-bar__close" type="button" aria-label={t('common.close')} onClick={onClose}>
            <X size={14} />
          </button>
        </Tooltip>
      </div>
      <div className="find-replace-bar__row">
        <input
          aria-label={t('findReplace.replacePlaceholder')}
          placeholder={t('findReplace.replacePlaceholder')}
          value={replaceText}
          onChange={(event) => onReplaceTextChange(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              event.preventDefault()
              onReplace()
            }
          }}
        />
        <button type="button" disabled={!findEnabled} onClick={onReplace}>{t('findReplace.replace')}</button>
        <div className="find-replace-bar__controls">
          <button type="button" disabled={!findEnabled} onClick={onReplaceAll}>{t('findReplace.replaceAll')}</button>
          <span className="find-replace-bar__count">{t('findReplace.replacedPrefix')} {replacedCount}</span>
        </div>
      </div>
    </div>
  )
}
