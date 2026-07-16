import { useEffect, useLayoutEffect, useRef, useState, type PointerEvent as ReactPointerEvent, type ReactNode } from 'react'
import { useSettings } from '@/features/settings/SettingsProvider'
import { useI18n } from '@/shared/i18n/I18nProvider'

type ResizableColumnsProps = {
  children: ReactNode
  className: string
  columns: number
  defaultSizes: readonly number[]
  minPaneWidths: readonly number[]
  minimumWidth?: number
  paneSelector?: string
  storageKey: string
}

type DragState = {
  dividerIndex: number
  startX: number
  widths: number[]
}

function normalizeSizes(sizes: readonly number[], columns: number): number[] {
  const usable = sizes.length === columns && sizes.every((size) => Number.isFinite(size) && size > 0)
    ? [...sizes]
    : Array.from({ length: columns }, () => 1)
  const total = usable.reduce((sum, size) => sum + size, 0)
  return usable.map((size) => size / total)
}

export function ResizableColumns({
  children,
  className,
  columns,
  defaultSizes,
  minPaneWidths,
  minimumWidth = 720,
  paneSelector,
  storageKey
}: ResizableColumnsProps) {
  const { t } = useI18n()
  const { settings, updateSettings } = useSettings()
  const containerRef = useRef<HTMLDivElement>(null)
  const dragRef = useRef<DragState | null>(null)
  const ratiosRef = useRef<number[]>(normalizeSizes(settings.layout.paneSizes[storageKey] ?? defaultSizes, columns))
  const [ratios, setRatios] = useState(ratiosRef.current)
  const [dividerPositions, setDividerPositions] = useState<number[]>([])
  const [enabled, setEnabled] = useState(false)
  const [activeDivider, setActiveDivider] = useState<number | null>(null)
  const defaultSizesKey = defaultSizes.join(',')
  const storedSizesKey = settings.layout.paneSizes[storageKey]?.join(',') ?? ''

  function applyRatios(nextRatios: number[]): void {
    ratiosRef.current = nextRatios
    setRatios(nextRatios)
  }

  function paneElements(): HTMLElement[] {
    if (!containerRef.current) return []
    if (paneSelector) {
      return paneSelector.split(',').flatMap((selector) => Array.from(
        containerRef.current!.querySelectorAll(`:scope > ${selector.trim()}`)
      )).slice(0, columns) as HTMLElement[]
    }
    return Array.from(containerRef.current.children)
      .filter((element) => !element.classList.contains('pane-resizer'))
      .slice(0, columns) as HTMLElement[]
  }

  function persist(nextRatios = ratiosRef.current): void {
    void updateSettings({
      layout: { paneSizes: { ...settings.layout.paneSizes, [storageKey]: nextRatios } }
    }).catch(() => undefined)
  }

  function resizeAdjacent(dividerIndex: number, delta: number, widths: number[]): number[] {
    const nextWidths = [...widths]
    const leftMinimum = minPaneWidths[dividerIndex] ?? 120
    const rightMinimum = minPaneWidths[dividerIndex + 1] ?? 120
    const pairWidth = widths[dividerIndex] + widths[dividerIndex + 1]
    const leftWidth = Math.min(pairWidth - rightMinimum, Math.max(leftMinimum, widths[dividerIndex] + delta))
    nextWidths[dividerIndex] = leftWidth
    nextWidths[dividerIndex + 1] = pairWidth - leftWidth
    return normalizeSizes(nextWidths, columns)
  }

  function startResize(dividerIndex: number, event: ReactPointerEvent<HTMLDivElement>): void {
    const widths = paneElements().map((pane) => pane.getBoundingClientRect().width)
    if (!enabled || widths.length !== columns) return
    event.preventDefault()
    event.currentTarget.setPointerCapture(event.pointerId)
    dragRef.current = { dividerIndex, startX: event.clientX, widths }
    setActiveDivider(dividerIndex)
  }

  function continueResize(event: ReactPointerEvent<HTMLDivElement>): void {
    const drag = dragRef.current
    if (!drag) return
    applyRatios(resizeAdjacent(drag.dividerIndex, event.clientX - drag.startX, drag.widths))
  }

  function finishResize(event: ReactPointerEvent<HTMLDivElement>): void {
    if (!dragRef.current) return
    if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId)
    dragRef.current = null
    setActiveDivider(null)
    persist()
  }

  function adjustWithKeyboard(dividerIndex: number, delta: number): void {
    const widths = paneElements().map((pane) => pane.getBoundingClientRect().width)
    if (widths.length !== columns) return
    const nextRatios = resizeAdjacent(dividerIndex, delta, widths)
    applyRatios(nextRatios)
    persist(nextRatios)
  }

  useEffect(() => {
    if (dragRef.current) return
    applyRatios(normalizeSizes(settings.layout.paneSizes[storageKey] ?? defaultSizesKey.split(',').map(Number), columns))
  }, [columns, defaultSizesKey, storageKey, storedSizesKey])

  useLayoutEffect(() => {
    const container = containerRef.current
    if (!container) return

    const measure = () => {
      const isEnabled = columns > 1 && container.getBoundingClientRect().width >= minimumWidth
      setEnabled(isEnabled)
      if (!isEnabled) {
        setDividerPositions([])
        return
      }
      const containerLeft = container.getBoundingClientRect().left
      const panes = paneElements()
      setDividerPositions(Array.from({ length: columns - 1 }, (_, index) => {
        const leftEdge = panes[index]?.getBoundingClientRect().right ?? 0
        const rightEdge = panes[index + 1]?.getBoundingClientRect().left ?? leftEdge
        return ((leftEdge + rightEdge) / 2) - containerLeft
      }))
    }

    const observer = new ResizeObserver(measure)
    observer.observe(container)
    paneElements().forEach((pane) => observer.observe(pane))
    measure()
    return () => observer.disconnect()
  }, [columns, minimumWidth, paneSelector, ratios])

  const gridTemplateColumns = enabled
    ? ratios.map((ratio, index) => `minmax(${minPaneWidths[index] ?? 120}px, ${ratio}fr)`).join(' ')
    : undefined

  return (
    <div ref={containerRef} className={`${className} resizable-columns`} style={{ gridTemplateColumns }}>
      {children}
      {enabled && dividerPositions.map((position, index) => (
        <div
          className={activeDivider === index ? 'pane-resizer pane-resizer--active' : 'pane-resizer'}
          role="separator"
          aria-label={t('common.resizePane')}
          aria-orientation="vertical"
          key={index}
          style={{ left: position }}
          tabIndex={0}
          onDoubleClick={() => {
            const nextRatios = normalizeSizes(defaultSizes, columns)
            applyRatios(nextRatios)
            persist(nextRatios)
          }}
          onKeyDown={(event) => {
            if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return
            event.preventDefault()
            adjustWithKeyboard(index, event.key === 'ArrowLeft' ? -16 : 16)
          }}
          onPointerDown={(event) => startResize(index, event)}
          onPointerMove={continueResize}
          onPointerUp={finishResize}
          onPointerCancel={finishResize}
        />
      ))}
    </div>
  )
}
