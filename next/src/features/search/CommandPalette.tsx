import { Search, X } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useAppStore } from '@/app/appStore'
import { toolGroups, toolRegistry, type ToolDefinition } from '@/app/toolRegistry'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function CommandPalette() {
  const { t } = useI18n()
  const open = useAppStore((state) => state.searchOpen)
  const setOpen = useAppStore((state) => state.setSearchOpen)
  const openTool = useAppStore((state) => state.openTool)
  const inputRef = useRef<HTMLInputElement>(null)
  const [query, setQuery] = useState('')
  const [selectedIndex, setSelectedIndex] = useState(0)

  const results = useMemo(() => {
    const normalized = query.trim().toLocaleLowerCase()
    if (!normalized) {
      return toolRegistry
    }
    return toolRegistry.filter((tool) => {
      const label = t(tool.titleKey).toLocaleLowerCase()
      return label.includes(normalized)
        || tool.id.toLocaleLowerCase().includes(normalized)
        || tool.keywords.some((keyword) => keyword.toLocaleLowerCase().includes(normalized))
    })
  }, [query, t])

  useEffect(() => {
    if (!open) {
      return
    }
    setQuery('')
    setSelectedIndex(0)
    window.requestAnimationFrame(() => inputRef.current?.focus())
  }, [open])

  useEffect(() => {
    setSelectedIndex((current) => Math.min(current, Math.max(results.length - 1, 0)))
  }, [results.length])

  if (!open) {
    return null
  }

  function choose(tool: ToolDefinition): void {
    openTool(tool.id)
  }

  return createPortal(
    <div className="command-palette-backdrop" role="presentation" onMouseDown={() => setOpen(false)}>
      <section
        className="command-palette"
        role="dialog"
        aria-modal="true"
        aria-label={t('app.search.title')}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="command-palette__search">
          <Search size={17} />
          <input
            ref={inputRef}
            value={query}
            placeholder={t('app.search.placeholder')}
            aria-label={t('app.search.placeholder')}
            onChange={(event) => {
              setQuery(event.target.value)
              setSelectedIndex(0)
            }}
            onKeyDown={(event) => {
              if (event.key === 'Escape') {
                setOpen(false)
              } else if (event.key === 'ArrowDown') {
                event.preventDefault()
                setSelectedIndex((index) => Math.min(index + 1, results.length - 1))
              } else if (event.key === 'ArrowUp') {
                event.preventDefault()
                setSelectedIndex((index) => Math.max(index - 1, 0))
              } else if (event.key === 'Enter' && results[selectedIndex]) {
                choose(results[selectedIndex])
              }
            }}
          />
          <button className="icon-ghost" type="button" aria-label={t('app.search.close')} onClick={() => setOpen(false)}>
            <X size={16} />
          </button>
        </div>

        <div className="command-palette__results" role="listbox">
          {results.length === 0 ? (
            <div className="command-palette__empty">{t('app.search.empty')}</div>
          ) : results.map((tool, index) => {
            const Icon = tool.icon
            const group = tool.groupId === 'home' ? null : toolGroups.find((item) => item.id === tool.groupId)
            return (
              <button
                className={index === selectedIndex ? 'command-result command-result--selected' : 'command-result'}
                type="button"
                role="option"
                aria-selected={index === selectedIndex}
                key={tool.id}
                onMouseEnter={() => setSelectedIndex(index)}
                onClick={() => choose(tool)}
              >
                <Icon size={17} />
                <span>{t(tool.titleKey)}</span>
                <small>{group ? t(group.titleKey) : ''}</small>
              </button>
            )
          })}
        </div>
      </section>
    </div>,
    document.body
  )
}
