import { Children, useEffect, useRef, useState, type ReactElement } from 'react'
import { useSettings } from '../features/settings/SettingsProvider'
import { useI18n } from '../app/i18n'

interface ResizableColumnsProps {
  id: string
  className?: string
  initialPrimary: number
  minPrimary?: number
  minSecondary?: number
  children: [ReactElement, ReactElement]
}

export function ResizableColumns({
  id,
  className = '',
  initialPrimary,
  minPrimary = 180,
  minSecondary = 280,
  children
}: ResizableColumnsProps) {
  const { settings, save } = useSettings()
  const { t } = useI18n()
  const containerRef = useRef<HTMLElement>(null)
  const configured = settings.layout.paneSizes[id] ?? initialPrimary
  const [primary, setPrimary] = useState(configured)
  const [dragging, setDragging] = useState(false)
  const panels = Children.toArray(children)

  useEffect(() => setPrimary(configured), [configured])

  function clamp(value: number): number {
    const width = containerRef.current?.getBoundingClientRect().width ?? value + minSecondary
    return Math.round(Math.max(minPrimary, Math.min(value, Math.max(minPrimary, width - minSecondary - 6))))
  }

  function persist(value: number): void {
    const next = clamp(value)
    setPrimary(next)
    if (settings.layout.paneSizes[id] === next) return
    void save((current) => ({
      ...current,
      layout: {
        ...current.layout,
        paneSizes: { ...current.layout.paneSizes, [id]: next }
      }
    }))
  }

  return (
    <section
      ref={containerRef}
      className={`resizable-columns ${dragging ? 'resizable-columns--dragging' : ''} ${className}`.trim()}
      style={{ gridTemplateColumns: `${primary}px 6px minmax(${minSecondary}px, 1fr)` }}
    >
      {panels[0]}
      <div
        className="resize-separator"
        role="separator"
        aria-label={t('layout.resizePanels')}
        aria-orientation="vertical"
        aria-valuemin={minPrimary}
        aria-valuenow={primary}
        tabIndex={0}
        onDoubleClick={() => persist(initialPrimary)}
        onPointerDown={(event) => {
          event.currentTarget.setPointerCapture(event.pointerId)
          setDragging(true)
        }}
        onPointerMove={(event) => {
          if (!event.currentTarget.hasPointerCapture(event.pointerId)) return
          const left = containerRef.current?.getBoundingClientRect().left ?? 0
          setPrimary(clamp(event.clientX - left))
        }}
        onPointerUp={(event) => {
          if (event.currentTarget.hasPointerCapture(event.pointerId)) {
            event.currentTarget.releasePointerCapture(event.pointerId)
          }
          setDragging(false)
          const left = containerRef.current?.getBoundingClientRect().left ?? 0
          persist(event.clientX - left)
        }}
        onPointerCancel={() => setDragging(false)}
        onKeyDown={(event) => {
          if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
            event.preventDefault()
            persist(primary + (event.key === 'ArrowLeft' ? -16 : 16))
          } else if (event.key === 'Home') {
            event.preventDefault()
            persist(initialPrimary)
          }
        }}
      />
      {panels[1]}
    </section>
  )
}
