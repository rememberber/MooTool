import { Children, useEffect, useLayoutEffect, useRef, useState, type ReactElement } from 'react'
import { useSettings } from '../features/settings/SettingsProvider'
import { useI18n } from '../app/i18n'
import { normalizePaneSizeList, storedPaneSizes } from '../platform/contracts/settings'

interface ResizableColumnsProps {
  id: string
  legacyId?: string
  className?: string
  initialPrimary: number
  defaultSizes?: [number, number]
  minPrimary?: number
  minSecondary?: number
  children: [ReactElement, ReactElement]
}

export function ResizableColumns({
  id,
  legacyId,
  className = '',
  initialPrimary,
  defaultSizes,
  minPrimary = 180,
  minSecondary = 280,
  children
}: ResizableColumnsProps) {
  const { settings, save } = useSettings()
  const { t } = useI18n()
  const containerRef = useRef<HTMLElement>(null)
  const [dragging, setDragging] = useState(false)
  const stored = storedPaneSizes(settings.layout.paneSizes, id, legacyId, 2)
  const [ratios, setRatios] = useState<number[] | null>(stored)
  const panels = Children.toArray(children)

  useEffect(() => {
    if (dragging) return
    const next = storedPaneSizes(settings.layout.paneSizes, id, legacyId, 2)
    if (next) setRatios(next)
  }, [dragging, id, legacyId, settings.layout.paneSizes])

  useLayoutEffect(() => {
    if (ratios || storedPaneSizes(settings.layout.paneSizes, id, legacyId, 2)) return
    if (defaultSizes) {
      setRatios(normalizePaneSizeList(defaultSizes))
      return
    }
    const width = containerRef.current?.getBoundingClientRect().width ?? 0
    if (width < minPrimary + minSecondary + 6) return
    const secondary = Math.max(minSecondary, width - initialPrimary - 6)
    setRatios(normalizePaneSizeList([initialPrimary, secondary]))
  }, [defaultSizes, id, initialPrimary, legacyId, minPrimary, minSecondary, ratios, settings.layout.paneSizes])

  function widthRatios(primary: number): number[] {
    const width = containerRef.current?.getBoundingClientRect().width ?? primary + minSecondary + 6
    const available = Math.max(minPrimary + minSecondary, width - 6)
    const nextPrimary = Math.round(Math.max(minPrimary, Math.min(primary, available - minSecondary)))
    return normalizePaneSizeList([nextPrimary, available - nextPrimary])
  }

  function persist(next: number[]): void {
    setRatios(next)
    const current = settings.layout.paneSizes[id]
    if (current && current.join(',') === next.join(',')) return
    void save((value) => ({
      ...value,
      layout: {
        ...value.layout,
        paneSizes: { ...value.layout.paneSizes, [id]: next }
      }
    }))
  }

  const columns = ratios
    ? `minmax(${minPrimary}px, ${ratios[0]}fr) 6px minmax(${minSecondary}px, ${ratios[1]}fr)`
    : defaultSizes
      ? `minmax(${minPrimary}px, ${defaultSizes[0]}fr) 6px minmax(${minSecondary}px, ${defaultSizes[1]}fr)`
      : `${initialPrimary}px 6px minmax(${minSecondary}px, 1fr)`

  return (
    <section
      ref={containerRef}
      className={`resizable-columns ${dragging ? 'resizable-columns--dragging' : ''} ${className}`.trim()}
      style={{ gridTemplateColumns: columns }}
    >
      {panels[0]}
      <div
        className="resize-separator"
        role="separator"
        aria-label={t('layout.resizePanels')}
        aria-orientation="vertical"
        aria-valuemin={minPrimary}
        aria-valuenow={ratios ? Math.round(ratios[0] * 1000) : initialPrimary}
        tabIndex={0}
        onDoubleClick={() => {
          if (defaultSizes) {
            persist(normalizePaneSizeList(defaultSizes))
            return
          }
          const width = containerRef.current?.getBoundingClientRect().width ?? initialPrimary + minSecondary + 6
          const secondary = Math.max(minSecondary, width - initialPrimary - 6)
          persist(normalizePaneSizeList([initialPrimary, secondary]))
        }}
        onPointerDown={(event) => {
          event.currentTarget.setPointerCapture(event.pointerId)
          setDragging(true)
        }}
        onPointerMove={(event) => {
          if (!event.currentTarget.hasPointerCapture(event.pointerId)) return
          const left = containerRef.current?.getBoundingClientRect().left ?? 0
          setRatios(widthRatios(event.clientX - left))
        }}
        onPointerUp={(event) => {
          if (event.currentTarget.hasPointerCapture(event.pointerId)) {
            event.currentTarget.releasePointerCapture(event.pointerId)
          }
          setDragging(false)
          const left = containerRef.current?.getBoundingClientRect().left ?? 0
          persist(widthRatios(event.clientX - left))
        }}
        onKeyDown={(event) => {
          if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return
          event.preventDefault()
          const width = containerRef.current?.getBoundingClientRect().width ?? initialPrimary + minSecondary + 6
          const available = Math.max(minPrimary + minSecondary, width - 6)
          const current = (ratios?.[0] ?? initialPrimary / available) * available
          persist(widthRatios(current + (event.key === 'ArrowLeft' ? -16 : 16)))
        }}
      />
      {panels[1]}
    </section>
  )
}
