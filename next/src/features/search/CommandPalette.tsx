import { ArrowLeft, Copy, Search, WandSparkles, X } from 'lucide-react'
import { useEffect, useId, useLayoutEffect, useMemo, useRef, useState, type KeyboardEvent } from 'react'
import { createPortal } from 'react-dom'
import { useAppStore } from '@/app/appStore'
import { executeAction, searchActions, type ActionMatch } from '@/app/actionRegistry'
import { toolGroups, toolRegistry, type ToolDefinition } from '@/app/toolRegistry'
import { useI18n } from '@/shared/i18n/I18nProvider'

type Result = { kind: 'tool'; tool: ToolDefinition } | { kind: 'action'; match: ActionMatch }

export function CommandPalette({ onOpenTool }: { onOpenTool?: (toolId: ToolDefinition['id']) => void }) {
  const { t } = useI18n()
  const open = useAppStore((state) => state.searchOpen)
  const setOpen = useAppStore((state) => state.setSearchOpen)
  const openTool = useAppStore((state) => state.openTool)
  const inputRef = useRef<HTMLInputElement>(null)
  const dialogRef = useRef<HTMLElement>(null)
  const request = useRef(0)
  const running = useRef(false)
  const listId = useId()
  const [query, setQuery] = useState('')
  const [selectedIndex, setSelectedIndex] = useState(0)
  const [preview, setPreview] = useState<{ title: string; text: string } | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [copied, setCopied] = useState(false)

  const results = useMemo<Result[]>(() => {
    const normalized = query.trim().toLocaleLowerCase()
    const actions = searchActions(query, t)
    const tools = toolRegistry.filter((tool) => !normalized || [t(tool.titleKey), tool.id, ...tool.keywords]
      .some((keyword) => keyword.toLocaleLowerCase().includes(normalized)))
    const actionResults: Result[] = actions.map((match) => ({ kind: 'action', match }))
    // Keep tool-name searches compatible with the existing navigation order.
    if (actions.some((match) => match.argument !== undefined)) return actionResults
    const toolResults: Result[] = tools.map((tool) => ({ kind: 'tool', tool }))
    return normalized ? [...toolResults, ...actionResults] : [...actionResults, ...toolResults]
  }, [query, t])

  useLayoutEffect(() => {
    request.current += 1
    running.current = false
    setBusy(false)
    setPreview(null)
    setError('')
    setCopied(false)
    setQuery('')
    setSelectedIndex(0)
    if (!open) return
    const previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null
    return () => {
      request.current += 1
      if (previousFocus?.isConnected) previousFocus.focus()
    }
  }, [open])

  useEffect(() => {
    setSelectedIndex((current) => Math.min(current, Math.max(results.length - 1, 0)))
  }, [results.length])

  useEffect(() => {
    dialogRef.current?.querySelector('[aria-selected="true"]')?.scrollIntoView({ block: 'nearest' })
  }, [selectedIndex, results, open, preview])

  useLayoutEffect(() => {
    if (!open) return
    if (preview) dialogRef.current?.querySelector<HTMLButtonElement>('[data-command-copy]')?.focus()
    else inputRef.current?.focus()
  }, [open, preview])

  if (!open) return null

  function back(): void {
    request.current += 1
    running.current = false
    setBusy(false)
    setPreview(null)
    setError('')
    setCopied(false)
  }

  async function choose(result: Result): Promise<void> {
    if (running.current) return
    if (result.kind === 'tool') {
      if (onOpenTool) onOpenTool(result.tool.id)
      else openTool(result.tool.id)
      setOpen(false)
      return
    }
    const currentRequest = ++request.current
    running.current = true
    setBusy(true)
    setError('')
    setCopied(false)
    try {
      const text = await executeAction(result.match, async () => {
        try { return await navigator.clipboard.readText() }
        catch { throw new Error(t('app.command.clipboardFailed')) }
      }, { t, zone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC' })
      if (request.current === currentRequest) {
        setPreview({ title: t(result.match.action.titleKey), text })
      }
    } catch (reason) {
      if (request.current === currentRequest) setError(reason instanceof Error ? reason.message : String(reason))
    } finally {
      if (request.current === currentRequest) {
        running.current = false
        setBusy(false)
      }
    }
  }

  async function copyResult(): Promise<void> {
    if (!preview || running.current) return
    const currentRequest = ++request.current
    running.current = true
    setBusy(true)
    setError('')
    try {
      await navigator.clipboard.writeText(preview.text)
      if (request.current === currentRequest) setCopied(true)
    } catch {
      if (request.current === currentRequest) setError(t('app.command.copyFailed'))
    } finally {
      if (request.current === currentRequest) {
        running.current = false
        setBusy(false)
      }
    }
  }

  function handleKeyDown(event: KeyboardEvent<HTMLElement>): void {
    if (event.nativeEvent.isComposing) return
    if (event.key === 'Escape') {
      event.preventDefault()
      event.stopPropagation()
      if (preview) back()
      else setOpen(false)
    } else if (event.key === 'Tab') {
      const focusable = Array.from(dialogRef.current?.querySelectorAll<HTMLElement>('input, button:not([disabled]):not([tabindex="-1"]), textarea') ?? [])
      const first = focusable[0]
      const last = focusable[focusable.length - 1]
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
    } else if (preview && event.key === 'Enter' && event.target === dialogRef.current?.querySelector('[data-command-copy]')) {
      event.preventDefault()
      if (!event.repeat) void copyResult()
    } else if (!preview && event.target === inputRef.current) {
      if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
        event.preventDefault()
        setSelectedIndex((index) => Math.max(0, Math.min(index + (event.key === 'ArrowDown' ? 1 : -1), results.length - 1)))
      } else if (event.key === 'Enter') {
        event.preventDefault()
        if (!event.repeat && results[selectedIndex]) void choose(results[selectedIndex])
      }
    }
  }

  return createPortal(
    <div className="command-palette-backdrop" role="presentation" onMouseDown={() => setOpen(false)}>
      <section ref={dialogRef} className="command-palette" role="dialog" aria-modal="true"
        aria-label={t('app.search.title')} aria-busy={busy}
        onKeyDown={handleKeyDown} onMouseDown={(event) => event.stopPropagation()}>
        <div className="command-palette__search">
          {preview ? <button className="icon-ghost" type="button" aria-label={t('app.command.back')} onClick={back}><ArrowLeft size={17} /></button> : <Search size={17} />}
          {preview ? <strong>{preview.title}</strong> : <input ref={inputRef} value={query}
            placeholder={t('app.search.placeholder')} aria-label={t('app.search.placeholder')}
            role="combobox" aria-expanded="true" aria-autocomplete="list" aria-controls={listId}
            aria-activedescendant={results.length ? `${listId}-${selectedIndex}` : undefined}
            onChange={(event) => {
              request.current += 1
              running.current = false
              setBusy(false)
              setError('')
              setQuery(event.target.value)
              setSelectedIndex(0)
            }} />}
          <button className="icon-ghost" type="button" aria-label={t('app.search.close')} onClick={() => setOpen(false)}><X size={16} /></button>
        </div>
        {preview ? <div className="command-palette__preview">
          <textarea readOnly value={preview.text} aria-label={t('app.command.preview')} spellCheck={false} />
          <div className="command-palette__preview-actions">
            <span role="status">{copied ? t('app.command.copied') : ''}</span>
            <button className="dialog-button" type="button" data-command-copy aria-disabled={busy} onClick={() => void copyResult()}><Copy size={14} />{t('app.command.copy')}</button>
          </div>
        </div> : <div className="command-palette__results" id={listId} role="listbox" aria-label={t('app.search.title')}>
          {results.length === 0 ? <div className="command-palette__empty">{t('app.search.empty')}</div> : results.map((result, index) => {
            const action = result.kind === 'action' ? result.match.action : null
            const tool = result.kind === 'tool' ? result.tool : null
            const Icon = tool?.icon ?? WandSparkles
            const group = tool && tool.groupId !== 'home' ? toolGroups.find((item) => item.id === tool.groupId) : null
            const detail = action ? t(action.input === 'none' ? 'app.command.noInput' : result.kind === 'action' && result.match.argument !== undefined ? 'app.command.inline' : 'app.command.clipboard')
              : group ? t(group.titleKey) : ''
            return <button className={index === selectedIndex ? 'command-result command-result--selected' : 'command-result'}
              type="button" role="option" aria-selected={index === selectedIndex} tabIndex={-1}
              data-command-kind={result.kind} data-command-id={action?.id ?? tool!.id}
              id={`${listId}-${index}`} key={action ? `action-${action.id}` : `tool-${tool!.id}`}
              onMouseEnter={() => setSelectedIndex(index)} onClick={() => void choose(result)}>
              <Icon size={17} />
              <span className="command-result__label"><span>{t(action?.titleKey ?? tool!.titleKey)}</span><small>{detail}</small></span>
              <small>{t(action ? 'app.command.action' : 'app.command.tool')}</small>
            </button>
          })}
        </div>}
        {error && <p className="command-palette__error" role="alert">{error}</p>}
        <footer className="command-palette__hint" role="status">{busy ? t('common.loading') : t(preview ? 'app.command.previewHint' : 'app.command.hint')}</footer>
      </section>
    </div>, document.body
  )
}
