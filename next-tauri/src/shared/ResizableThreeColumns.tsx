import { Children, useEffect, useRef, useState, type ReactElement } from 'react'
import { useI18n } from '../app/i18n'
import { useSettings } from '../features/settings/SettingsProvider'

interface Props {
  id: string
  className?: string
  initialLeft: number
  initialRight: number
  minLeft?: number
  minCenter?: number
  minRight?: number
  children: [ReactElement, ReactElement, ReactElement]
}

export function ResizableThreeColumns({ id, className = '', initialLeft, initialRight, minLeft = 170, minCenter = 320, minRight = 190, children }: Props) {
  const { t } = useI18n()
  const { settings, save } = useSettings()
  const hostRef = useRef<HTMLElement>(null)
  const leftKey = `${id}-left`
  const rightKey = `${id}-right`
  const [left, setLeft] = useState(settings.layout.paneSizes[leftKey] ?? initialLeft)
  const [right, setRight] = useState(settings.layout.paneSizes[rightKey] ?? initialRight)
  const panels = Children.toArray(children)

  useEffect(() => setLeft(settings.layout.paneSizes[leftKey] ?? initialLeft), [initialLeft, leftKey, settings.layout.paneSizes])
  useEffect(() => setRight(settings.layout.paneSizes[rightKey] ?? initialRight), [initialRight, rightKey, settings.layout.paneSizes])

  function clampLeft(value: number) {
    const width = hostRef.current?.getBoundingClientRect().width ?? value + right + minCenter
    return Math.round(Math.max(minLeft, Math.min(value, width - right - minCenter - 12)))
  }
  function clampRight(value: number) {
    const width = hostRef.current?.getBoundingClientRect().width ?? value + left + minCenter
    return Math.round(Math.max(minRight, Math.min(value, width - left - minCenter - 12)))
  }
  function persist(key: string, value: number) {
    void save((current) => ({ ...current, layout: { ...current.layout, paneSizes: { ...current.layout.paneSizes, [key]: value } } }))
  }

  const calculate = (side: 'left' | 'right', clientX: number) => {
    const bounds = hostRef.current?.getBoundingClientRect()
    if (!bounds) return side === 'left' ? left : right
    return side === 'left' ? clampLeft(clientX - bounds.left) : clampRight(bounds.right - clientX)
  }

  return <section ref={hostRef} className={`resizable-columns resizable-three-columns ${className}`.trim()} style={{ gridTemplateColumns: `${left}px 6px minmax(${minCenter}px, 1fr) 6px ${right}px` }}>{panels[0]}<ResizeHandle label={t('layout.resizePanels')} value={left} minimum={minLeft} onPreview={(clientX) => setLeft(calculate('left', clientX))} onCommit={(clientX) => { const next = calculate('left', clientX); setLeft(next); persist(leftKey, next) }} onReset={() => { setLeft(initialLeft); persist(leftKey, initialLeft) }} onKeyboard={(direction) => { const next = clampLeft(left + direction); setLeft(next); persist(leftKey, next) }} />{panels[1]}<ResizeHandle label={t('layout.resizePanels')} value={right} minimum={minRight} onPreview={(clientX) => setRight(calculate('right', clientX))} onCommit={(clientX) => { const next = calculate('right', clientX); setRight(next); persist(rightKey, next) }} onReset={() => { setRight(initialRight); persist(rightKey, initialRight) }} onKeyboard={(direction) => { const next = clampRight(right - direction); setRight(next); persist(rightKey, next) }} />{panels[2]}</section>
}

function ResizeHandle({ label, value, minimum, onPreview, onCommit, onReset, onKeyboard }: { label: string; value: number; minimum: number; onPreview(clientX: number): void; onCommit(clientX: number): void; onReset(): void; onKeyboard(direction: number): void }) {
  return <div className="resize-separator" role="separator" aria-label={label} aria-orientation="vertical" aria-valuemin={minimum} aria-valuenow={value} tabIndex={0} onDoubleClick={onReset} onPointerDown={(event) => event.currentTarget.setPointerCapture(event.pointerId)} onPointerMove={(event) => { if (event.currentTarget.hasPointerCapture(event.pointerId)) onPreview(event.clientX) }} onPointerUp={(event) => { onCommit(event.clientX); if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId) }} onKeyDown={(event) => { if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') { event.preventDefault(); onKeyboard(event.key === 'ArrowLeft' ? -16 : 16) } }} />
}
