import { ArrowLeft, Copy, Search, Star, WandSparkles, X } from 'lucide-react'
import { Fragment, useEffect, useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { homeTool, navigationToolCatalog, type ToolId } from '../../app/toolCatalog'
import { useI18n } from '../../app/i18n'
import { useLocalizedMessages, type LocalizedMessageKey } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { useSettings } from '../settings/SettingsProvider'
import { commandActions, runCommandAction, searchCommandActions, type CommandAction, type CommandActionMatch } from './commandActions'
import { commandPaletteMessages } from './commandPaletteMessages'
import {
  loadCommandPaletteState,
  orderedCommandActions,
  saveCommandPaletteState,
  updateCommandPaletteState,
  type CommandActionId,
  type CommandPaletteOperation,
  type CommandPaletteState,
  type CommandSection
} from './commandPaletteState'

interface CommandPaletteProps {
  open: boolean
  onClose(): void
  onOpenTool(toolId: ToolId): void
}

type PaletteMessageKey = LocalizedMessageKey<typeof commandPaletteMessages>
type Result =
  | { kind: 'tool'; id: ToolId; label: string; detail: string; section?: 'tools' }
  | { kind: 'action'; match: CommandActionMatch; label: string; detail: string; section?: CommandSection }

export function CommandPalette({ open, onClose, onOpenTool }: CommandPaletteProps) {
  const { t: shell, toolTitle, groupTitle } = useI18n()
  const { t } = useLocalizedMessages(commandPaletteMessages)
  const { settings } = useSettings()
  const inputRef = useRef<HTMLInputElement>(null)
  const [query, setQuery] = useState('')
  const [selectedIndex, setSelectedIndex] = useState(0)
  const [preferences, setPreferences] = useState<CommandPaletteState>(loadCommandPaletteState)
  const [preview, setPreview] = useState<{ actionId: CommandActionId; title: string; text: string } | null>(null)
  const [busy, setBusy] = useState(false)
  const [copied, setCopied] = useState(false)
  const [error, setError] = useState('')

  const results = useMemo(() => {
    const tools = visibleTools(query, settings.layout.hiddenNavigationToolIds, toolTitle, groupTitle)
    const actions = searchCommandActions(query, (action) => t(action.titleKey))
    if (!query.trim()) {
      return [
        ...orderedCommandActions(preferences).map((item) => actionResult(commandActions.find((action) => action.id === item.actionId)!, t, item.section)),
        ...tools.map((tool) => ({ ...tool, section: 'tools' as const }))
      ]
    }
    if (actions.some((match) => match.argument !== undefined)) return actions.map((match) => actionResult(match.action, t, undefined, match.argument))
    return [...tools, ...actions.map((match) => actionResult(match.action, t))]
  }, [groupTitle, preferences, query, settings.layout.hiddenNavigationToolIds, t, toolTitle])

  useEffect(() => {
    if (!open) return
    setQuery('')
    setSelectedIndex(0)
    setPreview(null)
    setError('')
    setCopied(false)
    setPreferences(loadCommandPaletteState())
    window.requestAnimationFrame(() => inputRef.current?.focus())
  }, [open])

  useEffect(() => {
    setSelectedIndex((current) => Math.min(current, Math.max(results.length - 1, 0)))
  }, [results.length])

  useEffect(() => {
    if (!open) return
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key !== 'Escape') return
      event.preventDefault()
      if (preview) setPreview(null)
      else onClose()
    }
    window.addEventListener('keydown', closeOnEscape, true)
    return () => window.removeEventListener('keydown', closeOnEscape, true)
  }, [onClose, open, preview])

  if (!open) return null

  const selected = results[selectedIndex]
  const selectedActionId = preview?.actionId ?? (selected?.kind === 'action' ? selected.match.action.id : null)
  const pinned = selectedActionId !== null && preferences.pinnedActionIds.includes(selectedActionId)

  function applyPreference(operation: CommandPaletteOperation): void {
    setPreferences((current) => {
      const next = updateCommandPaletteState(current, operation)
      saveCommandPaletteState(next)
      return next
    })
  }

  function chooseTool(toolId: ToolId): void {
    onOpenTool(toolId)
    onClose()
  }

  async function chooseAction(match: CommandActionMatch): Promise<void> {
    if (busy) return
    setBusy(true)
    setError('')
    setCopied(false)
    try {
      const text = await runCommandAction(match, readClipboardText, readClipboardHtml, Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC')
      setPreview({ actionId: match.action.id, title: t(match.action.titleKey), text })
      applyPreference({ type: 'record', actionId: match.action.id })
    } catch (cause) {
      setError(actionError(cause, t))
    } finally {
      setBusy(false)
    }
  }

  async function copyResult(): Promise<void> {
    if (!preview) return
    try {
      await clipboardApi.writeText(preview.text)
      setCopied(true)
      setError('')
    } catch {
      setError(t('copyFailed'))
    }
  }

  return createPortal(
    <div className="command-palette-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="command-palette" role="dialog" aria-modal="true" aria-label={shell('shell.search')} aria-busy={busy} onMouseDown={(event) => event.stopPropagation()}>
        <div className="command-palette__search">
          {preview ? <button className="icon-button" type="button" aria-label={t('back')} onClick={() => setPreview(null)}><ArrowLeft /></button> : <Search aria-hidden="true" />}
          {preview ? <strong>{preview.title}</strong> : (
            <input
              ref={inputRef}
              value={query}
              placeholder={shell('shell.search')}
              aria-label={shell('shell.search')}
              aria-controls="command-palette-results"
              aria-activedescendant={results[selectedIndex] ? resultId(results[selectedIndex]) : undefined}
              onChange={(event) => { setQuery(event.target.value); setSelectedIndex(0); setError('') }}
              onKeyDown={(event) => {
                if (event.key === 'ArrowDown') { event.preventDefault(); setSelectedIndex((current) => Math.min(current + 1, results.length - 1)) }
                else if (event.key === 'ArrowUp') { event.preventDefault(); setSelectedIndex((current) => Math.max(current - 1, 0)) }
                else if (event.key === 'Enter' && results[selectedIndex]) {
                  event.preventDefault()
                  const result = results[selectedIndex]
                  if (result.kind === 'tool') chooseTool(result.id)
                  else void chooseAction(result.match)
                }
              }}
            />
          )}
          <button className="icon-button" type="button" aria-label={shell('settings.close')} onClick={onClose}><X /></button>
        </div>
        {preview ? (
          <div className="command-palette__preview">
            <textarea aria-label={t('preview')} readOnly value={preview.text} />
            <div className="command-palette__preview-actions">
              <span role="status">{copied ? t('copied') : ''}</span>
              <button type="button" onClick={() => void copyResult()}><Copy />{t('copy')}</button>
            </div>
          </div>
        ) : (
          <div id="command-palette-results" className="command-palette__results" role="listbox">
            {results.length === 0 ? <p className="command-palette__empty">{shell('shell.searchEmpty')}</p> : results.map((result, index) => {
              const Icon = result.kind === 'tool' ? toolIcon(result.id) : WandSparkles
              return (
                <Fragment key={resultId(result)}>
                  {result.section && result.section !== results[index - 1]?.section && <div className="command-palette__section">{t(`section.${result.section}`)}</div>}
                  <button
                    id={resultId(result)}
                    className={index === selectedIndex ? 'command-result command-result--selected' : 'command-result'}
                    type="button"
                    role="option"
                    aria-selected={index === selectedIndex}
                    onMouseEnter={() => setSelectedIndex(index)}
                    onClick={() => { if (result.kind === 'tool') chooseTool(result.id); else void chooseAction(result.match) }}
                  >
                    <Icon aria-hidden="true" />
                    <span>{result.label}<small>{result.detail}</small></span>
                    <small>{result.kind === 'action' && preferences.pinnedActionIds.includes(result.match.action.id) ? <Star aria-label={t('pinned')} /> : t(result.kind === 'action' ? 'action' : 'tool')}</small>
                  </button>
                </Fragment>
              )
            })}
          </div>
        )}
        {(selectedActionId || preferences.recentActionIds.length > 0) && (
          <div className="command-palette__preferences">
            {selectedActionId && <button type="button" aria-pressed={pinned} disabled={busy} onClick={() => applyPreference({ type: 'pin', actionId: selectedActionId, pinned: !pinned })}><Star />{t(pinned ? 'unpin' : 'pin')}</button>}
            {preferences.recentActionIds.length > 0 && <button type="button" onClick={() => applyPreference({ type: 'clear-recent' })}>{t('clearRecent')}</button>}
          </div>
        )}
        {error && <p className="command-palette__error" role="alert">{error}</p>}
        <footer className="command-palette__hint">{busy ? t('busy') : t(preview ? 'previewHint' : 'hint')}</footer>
      </section>
    </div>,
    document.body
  )
}

function visibleTools(query: string, hidden: readonly string[], toolTitle: (tool: (typeof navigationToolCatalog)[number]) => string, groupTitle: (group: (typeof navigationToolCatalog)[number]['group']) => string): Array<Extract<Result, { kind: 'tool' }>> {
  const normalized = query.trim().toLocaleLowerCase()
  const tools = [
    { id: homeTool.id, label: 'MooTool', detail: '', keywords: ['home', 'mootool'] },
    ...navigationToolCatalog.filter((tool) => !hidden.includes(tool.id)).map((tool) => ({
      id: tool.id,
      label: toolTitle(tool),
      detail: groupTitle(tool.group),
      keywords: tool.keywords
    }))
  ]
  return tools.filter((tool) => !normalized || [tool.id, tool.label, ...tool.keywords].some((value) => value.toLocaleLowerCase().includes(normalized)))
    .map((tool) => ({ kind: 'tool', id: tool.id, label: tool.label, detail: tool.detail }))
}

function actionResult(action: CommandAction, t: (key: PaletteMessageKey) => string, section?: CommandSection, argument?: string): Extract<Result, { kind: 'action' }> {
  return {
    kind: 'action',
    match: { action, argument },
    label: t(action.titleKey),
    detail: t(argument !== undefined ? 'inline' : action.input === 'none' ? 'noInput' : action.input === 'text' ? 'clipboard' : 'richClipboard'),
    section
  }
}

function resultId(result: Result): string {
  return result.kind === 'tool' ? `command-result-${result.id}` : `command-result-action-${result.match.action.id}`
}

function toolIcon(id: ToolId) {
  return id === homeTool.id ? homeTool.icon : navigationToolCatalog.find((tool) => tool.id === id)?.icon ?? WandSparkles
}

async function readClipboardText(): Promise<string> {
  try {
    return await navigator.clipboard.readText()
  } catch {
    throw new Error('clipboardFailed')
  }
}

async function readClipboardHtml(): Promise<string> {
  try {
    if (!navigator.clipboard.read) return ''
    for (const item of await navigator.clipboard.read()) {
      if (item.types.includes('text/html')) return await (await item.getType('text/html')).text()
    }
    return ''
  } catch {
    throw new Error('clipboardFailed')
  }
}

function actionError(cause: unknown, t: (key: PaletteMessageKey) => string): string {
  if (!(cause instanceof Error)) return String(cause)
  const known = cause.message.startsWith('app.command.') ? cause.message.slice('app.command.'.length) : cause.message
  const keys: PaletteMessageKey[] = ['tooLarge', 'noHtml', 'emptyInput', 'clipboardFailed', 'copyFailed', 'invalidBase64', 'invalidUrl', 'invalidJson', 'invalidTimestamp', 'invalidTable', 'tableHeaders', 'tableLimit', 'unsupportedTable', 'complexHtml']
  if (keys.includes(known as PaletteMessageKey)) return t(known as PaletteMessageKey)
  if (cause.name === 'JsonToolError') return t('invalidJson')
  if (cause.name === 'TimeToolError') return t('invalidTimestamp')
  if (cause.name === 'EncodeToolError') return t('invalidBase64')
  return cause.message
}
