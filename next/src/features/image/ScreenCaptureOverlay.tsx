import { Check, X } from 'lucide-react'
import { useEffect, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react'
import type { ScreenCaptureOverlayData, ScreenCaptureRect } from '@/shared/contracts/images'
import { useI18n } from '@/shared/i18n/I18nProvider'
import {
  captureRectFromPoints,
  moveCaptureRect,
  resizeCaptureRect,
  type CapturePoint,
  type CaptureResizeHandle
} from './captureSelection'

type DragState = {
  pointerId: number
  mode: 'create' | 'move' | CaptureResizeHandle
  start: CapturePoint
  rect: ScreenCaptureRect
}

export function ScreenCaptureOverlay() {
  const { t } = useI18n()
  const [capture, setCapture] = useState<ScreenCaptureOverlayData | null>(null)
  const [selection, setSelection] = useState<ScreenCaptureRect | null>(null)
  const dragRef = useRef<DragState | null>(null)
  const confirmingRef = useRef(false)

  useEffect(() => {
    let cancelled = false
    void window.mootool.getScreenCaptureOverlay().then((data) => {
      if (!cancelled) setCapture(data)
    })
    return () => { cancelled = true }
  }, [])

  useEffect(() => {
    const keyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault()
        void cancel()
      } else if (event.key === 'Enter' && selection) {
        event.preventDefault()
        void confirm()
      }
    }
    window.addEventListener('keydown', keyDown)
    return () => window.removeEventListener('keydown', keyDown)
  }, [selection])

  function capturePoint(event: Pick<ReactPointerEvent, 'clientX' | 'clientY'>): CapturePoint | null {
    if (!capture || window.innerWidth <= 0 || window.innerHeight <= 0) return null
    return {
      x: Math.min(capture.width, Math.max(0, event.clientX / window.innerWidth * capture.width)),
      y: Math.min(capture.height, Math.max(0, event.clientY / window.innerHeight * capture.height))
    }
  }

  function pointerDown(event: ReactPointerEvent<HTMLElement>): void {
    if (!capture || event.button !== 0 || (event.target as HTMLElement).closest('[data-capture-command]')) return
    const point = capturePoint(event)
    if (!point) return
    const target = (event.target as HTMLElement).closest<HTMLElement>('[data-capture-drag]')
    const mode = (target?.dataset.captureDrag ?? 'create') as DragState['mode']
    const startRect = selection ?? captureRectFromPoints(point, point, capture)
    dragRef.current = { pointerId: event.pointerId, mode, start: point, rect: startRect }
    event.currentTarget.setPointerCapture(event.pointerId)
    if (mode === 'create') setSelection(startRect)
    event.preventDefault()
  }

  function pointerMove(event: ReactPointerEvent<HTMLElement>): void {
    const drag = dragRef.current
    const point = capturePoint(event)
    if (!capture || !drag || drag.pointerId !== event.pointerId || !point) return
    const delta = { x: point.x - drag.start.x, y: point.y - drag.start.y }
    if (drag.mode === 'create') setSelection(captureRectFromPoints(drag.start, point, capture))
    else if (drag.mode === 'move') setSelection(moveCaptureRect(drag.rect, delta, capture))
    else setSelection(resizeCaptureRect(drag.rect, drag.mode, delta, capture))
  }

  function pointerUp(event: ReactPointerEvent<HTMLElement>): void {
    if (dragRef.current?.pointerId !== event.pointerId) return
    dragRef.current = null
    if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId)
  }

  async function confirm(): Promise<void> {
    if (!selection || confirmingRef.current) return
    confirmingRef.current = true
    await window.mootool.confirmScreenCapture(selection)
  }

  async function cancel(): Promise<void> {
    if (confirmingRef.current) return
    confirmingRef.current = true
    await window.mootool.cancelScreenCapture()
  }

  if (!capture) return <main className="screen-capture-overlay screen-capture-overlay--loading" />

  const selectionStyle = selection ? {
    left: `${selection.x / capture.width * 100}%`,
    top: `${selection.y / capture.height * 100}%`,
    width: `${selection.width / capture.width * 100}%`,
    height: `${selection.height / capture.height * 100}%`
  } : undefined

  return (
    <main
      className="screen-capture-overlay"
      onPointerDown={pointerDown}
      onPointerMove={pointerMove}
      onPointerUp={pointerUp}
      onPointerCancel={pointerUp}
      onContextMenu={(event) => { event.preventDefault(); void cancel() }}
    >
      <img className="screen-capture-overlay__image" src={capture.dataUrl} alt="" draggable={false} />
      {!selection && <div className="screen-capture-overlay__shade" />}
      {selection && selectionStyle && (
        <div className="screen-capture-overlay__selection" data-capture-drag="move" style={selectionStyle}>
          <span className="screen-capture-overlay__size">{selection.width} × {selection.height}</span>
          {(['nw', 'ne', 'se', 'sw'] as CaptureResizeHandle[]).map((handle) => (
            <span className={`screen-capture-overlay__handle screen-capture-overlay__handle--${handle}`} data-capture-drag={handle} key={handle} />
          ))}
          <div className="screen-capture-overlay__commands" data-capture-command>
            <button type="button" aria-label={t('common.cancel')} onClick={() => { void cancel() }}><X size={17} /></button>
            <button className="screen-capture-overlay__confirm" type="button" aria-label={t('image.captureConfirm')} onClick={() => { void confirm() }}><Check size={17} /></button>
          </div>
        </div>
      )}
      <div className="screen-capture-overlay__hint" data-capture-command>
        <strong>{t('image.captureOverlayHint')}</strong>
        <span>{t('image.captureOverlayKeys')}</span>
      </div>
    </main>
  )
}
