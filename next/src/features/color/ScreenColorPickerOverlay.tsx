import { useEffect, useRef, useState, type MouseEvent as ReactMouseEvent, type PointerEvent as ReactPointerEvent } from 'react'
import type { ScreenCaptureOverlayData } from '@/shared/contracts/images'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { sampleScreenColor, type SampledScreenColor } from './screenColorPicker'

type ColorPreview = SampledScreenColor & {
  clientX: number
  clientY: number
}

export function ScreenColorPickerOverlay() {
  const { t } = useI18n()
  const [capture, setCapture] = useState<ScreenCaptureOverlayData | null>(null)
  const [preview, setPreview] = useState<ColorPreview | null>(null)
  const imageDataRef = useRef<ImageData | null>(null)
  const confirmingRef = useRef(false)

  useEffect(() => {
    let cancelled = false
    void window.mootool.getScreenColorOverlay().then((data) => {
      if (!cancelled) setCapture(data)
    })
    return () => { cancelled = true }
  }, [])

  useEffect(() => {
    if (!capture) return
    let cancelled = false
    const image = new Image()
    image.onload = () => {
      if (cancelled) return
      const canvas = document.createElement('canvas')
      canvas.width = capture.width
      canvas.height = capture.height
      const context = canvas.getContext('2d', { willReadFrequently: true })
      if (!context) return
      context.drawImage(image, 0, 0, capture.width, capture.height)
      imageDataRef.current = context.getImageData(0, 0, capture.width, capture.height)
    }
    image.src = capture.dataUrl
    return () => {
      cancelled = true
      imageDataRef.current = null
    }
  }, [capture])

  useEffect(() => {
    const keyDown = (event: KeyboardEvent) => {
      if (event.key !== 'Escape') return
      event.preventDefault()
      void cancel()
    }
    window.addEventListener('keydown', keyDown)
    return () => window.removeEventListener('keydown', keyDown)
  }, [])

  function colorAt(clientX: number, clientY: number): ColorPreview | null {
    if (!capture || !imageDataRef.current || window.innerWidth <= 0 || window.innerHeight <= 0) return null
    const sampled = sampleScreenColor(
      imageDataRef.current.data,
      capture.width,
      capture.height,
      clientX / window.innerWidth * capture.width,
      clientY / window.innerHeight * capture.height
    )
    return sampled ? { ...sampled, clientX, clientY } : null
  }

  function pointerMove(event: ReactPointerEvent<HTMLElement>): void {
    const sampled = colorAt(event.clientX, event.clientY)
    if (sampled) setPreview(sampled)
  }

  function selectColor(event: ReactMouseEvent<HTMLElement>): void {
    if (event.button !== 0 || confirmingRef.current) return
    const sampled = colorAt(event.clientX, event.clientY)
    if (!sampled) return
    setPreview(sampled)
    confirmingRef.current = true
    event.preventDefault()
    void window.mootool.confirmScreenColor(sampled.hex)
  }

  async function cancel(): Promise<void> {
    if (confirmingRef.current) return
    confirmingRef.current = true
    await window.mootool.cancelScreenColor()
  }

  if (!capture) return <main className="screen-color-picker-overlay screen-color-picker-overlay--loading" />

  const previewStyle = preview ? {
    left: Math.min(window.innerWidth - 150, Math.max(8, preview.clientX + 18)),
    top: Math.min(window.innerHeight - 58, Math.max(8, preview.clientY + 18))
  } : undefined

  return (
    <main
      className="screen-color-picker-overlay"
      onPointerMove={pointerMove}
      onClick={selectColor}
      onContextMenu={(event) => { event.preventDefault(); void cancel() }}
    >
      <img className="screen-color-picker-overlay__image" src={capture.dataUrl} alt="" draggable={false} />
      {preview && (
        <>
          <span className="screen-color-picker-overlay__crosshair" style={{ left: preview.clientX, top: preview.clientY }} />
          <div className="screen-color-picker-overlay__preview" style={previewStyle}>
            <span className="screen-color-picker-overlay__swatch" style={{ background: preview.hex }} />
            <span><strong>{preview.hex}</strong><small>RGB {preview.r}, {preview.g}, {preview.b}</small></span>
          </div>
        </>
      )}
      <div className="screen-color-picker-overlay__hint">
        <strong>{t('color.pickerOverlayHint')}</strong>
        <span>{t('color.pickerOverlayKeys')}</span>
      </div>
    </main>
  )
}
