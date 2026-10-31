import { Children, useEffect, useLayoutEffect, useRef, useState, type ReactElement } from 'react'
import { useI18n } from '../app/i18n'
import { useSettings } from '../features/settings/SettingsProvider'
import { normalizePaneSizeList, storedPaneSizes } from '../platform/contracts/settings'

interface Props {
  id: string
  legacyId?: string
  className?: string
  initialLeft: number
  initialRight: number
  defaultSizes?: [number, number, number]
  minLeft?: number
  minCenter?: number
  minRight?: number
  children: [ReactElement, ReactElement, ReactElement]
}

export function ResizableThreeColumns({ id, legacyId, className = '', initialLeft, initialRight, defaultSizes, minLeft = 170, minCenter = 320, minRight = 190, children }: Props) {
  const { t } = useI18n()
  const { settings, save } = useSettings()
  const hostRef = useRef<HTMLElement>(null)
  const stored = storedPaneSizes(settings.layout.paneSizes, id, legacyId, 3)
  const [ratios, setRatios] = useState<number[] | null>(stored)
  const panels = Children.toArray(children)

  useEffect(() => {
    const next = storedPaneSizes(settings.layout.paneSizes, id, legacyId, 3)
    if (next) setRatios(next)
  }, [id, legacyId, settings.layout.paneSizes])

  useLayoutEffect(() => {
    if (ratios || storedPaneSizes(settings.layout.paneSizes, id, legacyId, 3)) return
    if (defaultSizes) {
      setRatios(normalizePaneSizeList(defaultSizes))
      return
    }
    const width = hostRef.current?.getBoundingClientRect().width ?? 0
    if (width < minLeft + minCenter + minRight + 12) return
    const center = Math.max(minCenter, width - initialLeft - initialRight - 12)
    setRatios(normalizePaneSizeList([initialLeft, center, initialRight]))
  }, [defaultSizes, id, initialLeft, initialRight, legacyId, minCenter, minLeft, minRight, ratios, settings.layout.paneSizes])

  function persist(next: number[]) {
    setRatios(next)
    const current = settings.layout.paneSizes[id]
    if (current && current.join(',') === next.join(',')) return
    void save((value) => ({
      ...value,
      layout: { ...value.layout, paneSizes: { ...value.layout.paneSizes, [id]: next } }
    }))
  }

  function widths(nextLeft: number, nextRight: number): number[] {
    const width = hostRef.current?.getBoundingClientRect().width ?? nextLeft + nextRight + minCenter + 12
    const available = Math.max(minLeft + minCenter + minRight, width - 12)
    const left = Math.round(Math.max(minLeft, Math.min(nextLeft, available - minCenter - minRight)))
    const right = Math.round(Math.max(minRight, Math.min(nextRight, available - left - minCenter)))
    return normalizePaneSizeList([left, available - left - right, right])
  }

  const currentLeft = ratios ? ratios[0] : 0
  const currentRight = ratios ? ratios[2] : 0
  const columns = ratios
    ? `minmax(${minLeft}px, ${ratios[0]}fr) 6px minmax(${minCenter}px, ${ratios[1]}fr) 6px minmax(${minRight}px, ${ratios[2]}fr)`
    : defaultSizes
      ? `minmax(${minLeft}px, ${defaultSizes[0]}fr) 6px minmax(${minCenter}px, ${defaultSizes[1]}fr) 6px minmax(${minRight}px, ${defaultSizes[2]}fr)`
      : `${initialLeft}px 6px minmax(${minCenter}px, 1fr) 6px ${initialRight}px`

  return (
    <section ref={hostRef} className={`resizable-columns resizable-three-columns ${className}`.trim()} style={{ gridTemplateColumns: columns }}>
      {panels[0]}
      <ResizeHandle
        label={t('layout.resizePanels')}
        value={ratios ? Math.round(currentLeft * 1000) : initialLeft}
        minimum={minLeft}
        onPreview={(clientX) => {
          const bounds = hostRef.current?.getBoundingClientRect()
          if (!bounds) return
          const right = ratios ? ratios[2] * Math.max(0, bounds.width - 12) : initialRight
          setRatios(widths(clientX - bounds.left, right))
        }}
        onCommit={(clientX) => {
          const bounds = hostRef.current?.getBoundingClientRect()
          if (!bounds) return
          const right = ratios ? ratios[2] * Math.max(0, bounds.width - 12) : initialRight
          persist(widths(clientX - bounds.left, right))
        }}
        onReset={() => {
          if (defaultSizes) {
            persist(normalizePaneSizeList(defaultSizes))
            return
          }
          const width = hostRef.current?.getBoundingClientRect().width ?? initialLeft + initialRight + minCenter + 12
          persist(normalizePaneSizeList([initialLeft, Math.max(minCenter, width - initialLeft - initialRight - 12), initialRight]))
        }}
        onKeyboard={(direction) => {
          const bounds = hostRef.current?.getBoundingClientRect()
          const available = Math.max(minLeft + minCenter + minRight, (bounds?.width ?? 0) - 12)
          const left = (ratios?.[0] ?? initialLeft / available) * available
          const right = (ratios?.[2] ?? initialRight / available) * available
          persist(widths(left + direction, right))
        }}
      />
      {panels[1]}
      <ResizeHandle
        label={t('layout.resizePanels')}
        value={ratios ? Math.round(currentRight * 1000) : initialRight}
        minimum={minRight}
        onPreview={(clientX) => {
          const bounds = hostRef.current?.getBoundingClientRect()
          if (!bounds) return
          const left = ratios ? ratios[0] * Math.max(0, bounds.width - 12) : initialLeft
          setRatios(widths(left, bounds.right - clientX))
        }}
        onCommit={(clientX) => {
          const bounds = hostRef.current?.getBoundingClientRect()
          if (!bounds) return
          const left = ratios ? ratios[0] * Math.max(0, bounds.width - 12) : initialLeft
          persist(widths(left, bounds.right - clientX))
        }}
        onReset={() => {
          if (defaultSizes) {
            persist(normalizePaneSizeList(defaultSizes))
            return
          }
          const width = hostRef.current?.getBoundingClientRect().width ?? initialLeft + initialRight + minCenter + 12
          persist(normalizePaneSizeList([initialLeft, Math.max(minCenter, width - initialLeft - initialRight - 12), initialRight]))
        }}
        onKeyboard={(direction) => {
          const bounds = hostRef.current?.getBoundingClientRect()
          const available = Math.max(minLeft + minCenter + minRight, (bounds?.width ?? 0) - 12)
          const left = (ratios?.[0] ?? initialLeft / available) * available
          const right = (ratios?.[2] ?? initialRight / available) * available
          persist(widths(left, right - direction))
        }}
      />
      {panels[2]}
    </section>
  )
}

function ResizeHandle({ label, value, minimum, onPreview, onCommit, onReset, onKeyboard }: { label: string; value: number; minimum: number; onPreview(clientX: number): void; onCommit(clientX: number): void; onReset(): void; onKeyboard(direction: number): void }) {
  return <div className="resize-separator" role="separator" aria-label={label} aria-orientation="vertical" aria-valuemin={minimum} aria-valuenow={value} tabIndex={0} onDoubleClick={onReset} onPointerDown={(event) => event.currentTarget.setPointerCapture(event.pointerId)} onPointerMove={(event) => { if (event.currentTarget.hasPointerCapture(event.pointerId)) onPreview(event.clientX) }} onPointerUp={(event) => { onCommit(event.clientX); if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId) }} onKeyDown={(event) => { if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') { event.preventDefault(); onKeyboard(event.key === 'ArrowLeft' ? -16 : 16) } }} />
}
