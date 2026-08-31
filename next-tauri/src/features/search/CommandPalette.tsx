import { Search, X } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { homeTool, navigationToolCatalog, type ToolId } from '../../app/toolCatalog'
import { useI18n } from '../../app/i18n'
import { useSettings } from '../settings/SettingsProvider'

interface CommandPaletteProps {
  open: boolean
  onClose(): void
  onOpenTool(toolId: ToolId): void
}

export function CommandPalette({ open, onClose, onOpenTool }: CommandPaletteProps) {
  const { t, toolTitle, groupTitle } = useI18n()
  const { settings } = useSettings()
  const inputRef = useRef<HTMLInputElement>(null)
  const [query, setQuery] = useState('')
  const [selectedIndex, setSelectedIndex] = useState(0)

  const results = useMemo(() => {
    const normalized = query.trim().toLocaleLowerCase()
    const tools = [
      {
        id: homeTool.id,
        icon: homeTool.icon,
        label: 'MooTool',
        groupLabel: '',
        keywords: ['home', 'mootool']
      },
      ...navigationToolCatalog.filter((tool) => !settings.layout.hiddenTools.includes(tool.id)).map((tool) => ({
        id: tool.id,
        icon: tool.icon,
        label: toolTitle(tool),
        groupLabel: groupTitle(tool.group),
        keywords: tool.keywords
      }))
    ]
    if (!normalized) return tools
    return tools.filter((tool) => [
      tool.id,
      tool.label,
      ...tool.keywords
    ].some((value) => value.toLocaleLowerCase().includes(normalized)))
  }, [groupTitle, query, settings.layout.hiddenTools, toolTitle])

  useEffect(() => {
    if (!open) return
    setQuery('')
    setSelectedIndex(0)
    window.requestAnimationFrame(() => inputRef.current?.focus())
  }, [open])

  useEffect(() => {
    setSelectedIndex((current) => Math.min(current, Math.max(results.length - 1, 0)))
  }, [results.length])

  useEffect(() => {
    if (!open) return
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [onClose, open])

  if (!open) return null

  function choose(toolId: ToolId): void {
    onOpenTool(toolId)
    onClose()
  }

  return createPortal(
    <div className="command-palette-backdrop" role="presentation" onMouseDown={onClose}>
      <section
        className="command-palette"
        role="dialog"
        aria-modal="true"
        aria-label={t('shell.search')}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="command-palette__search">
          <Search aria-hidden="true" />
          <input
            ref={inputRef}
            value={query}
            placeholder={t('shell.search')}
            aria-label={t('shell.search')}
            aria-controls="command-palette-results"
            aria-activedescendant={results[selectedIndex] ? `command-result-${results[selectedIndex].id}` : undefined}
            onChange={(event) => {
              setQuery(event.target.value)
              setSelectedIndex(0)
            }}
            onKeyDown={(event) => {
              if (event.key === 'ArrowDown') {
                event.preventDefault()
                setSelectedIndex((current) => Math.min(current + 1, results.length - 1))
              } else if (event.key === 'ArrowUp') {
                event.preventDefault()
                setSelectedIndex((current) => Math.max(current - 1, 0))
              } else if (event.key === 'Enter' && results[selectedIndex]) {
                event.preventDefault()
                choose(results[selectedIndex].id)
              }
            }}
          />
          <kbd>Esc</kbd>
          <button className="icon-button" type="button" aria-label={t('settings.close')} onClick={onClose}>
            <X />
          </button>
        </div>
        <div id="command-palette-results" className="command-palette__results" role="listbox">
          {results.length === 0
            ? <p className="command-palette__empty">{t('shell.searchEmpty')}</p>
            : results.map((tool, index) => {
                const Icon = tool.icon
                return (
                  <button
                    id={`command-result-${tool.id}`}
                    className={index === selectedIndex ? 'command-result command-result--selected' : 'command-result'}
                    type="button"
                    role="option"
                    aria-selected={index === selectedIndex}
                    key={tool.id}
                    onMouseEnter={() => setSelectedIndex(index)}
                    onClick={() => choose(tool.id)}
                  >
                    <Icon aria-hidden="true" />
                    <span>{tool.label}</span>
                    <small>{tool.groupLabel}</small>
                  </button>
                )
              })}
        </div>
      </section>
    </div>,
    document.body
  )
}
