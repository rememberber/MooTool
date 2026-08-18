import {
  Check,
  CheckCircle2,
  Clipboard,
  Contrast,
  Copy,
  Dices,
  Monitor,
  Palette,
  Pipette,
  TriangleAlert,
  X
} from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import {
  useLocalizedMessages,
  type LocalizedMessageKey,
  type MessageValues
} from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { imageApi } from '../../platform/api/imageApi'
import { nativeDesktopApi } from '../../platform/api/nativeDesktopApi'
import type { ImageAsset } from '../../platform/contracts/image'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import {
  bestTextColor,
  ColorToolError,
  colorFormats,
  contrastRatio,
  createColorScale,
  parseHexColor,
  randomColor
} from './colorTools'
import { colorMessages } from './colorMessages'

type ColorMessageKey = LocalizedMessageKey<typeof colorMessages>
type Notice = { key: ColorMessageKey; values?: MessageValues } | { raw: string }

class ColorLocalizedError extends Error {
  constructor(readonly key: ColorMessageKey, readonly values?: MessageValues) {
    super(key)
  }
}

export function ColorSurface() {
  const { t } = useLocalizedMessages(colorMessages)
  const [hex, setHex] = useState('#2F6FED')
  const [contrastHex, setContrastHex] = useState('#FFFFFF')
  const [notice, setNotice] = useState<Notice>({ key: 'notice.ready' })
  const [failed, setFailed] = useState(false)
  const [copied, setCopied] = useState('')
  const [sampling, setSampling] = useState(false)
  const [screenAssets, setScreenAssets] = useState<ImageAsset[]>([])
  const parsed = useMemo(() => {
    try {
      return parseHexColor(hex)
    } catch {
      return parseHexColor('#2F6FED')
    }
  }, [hex])
  const comparison = useMemo(() => {
    try {
      return parseHexColor(contrastHex)
    } catch {
      return parseHexColor('#FFFFFF')
    }
  }, [contrastHex])
  const formats = useMemo(() => colorFormats(parsed), [parsed])
  const scale = useMemo(() => createColorScale(parsed), [parsed])
  const ratio = useMemo(() => contrastRatio(parsed, comparison), [comparison, parsed])
  const noticeText = 'raw' in notice ? notice.raw : t(notice.key, notice.values)
  const session = useMemo(() => ({
    digest: JSON.stringify({
      hexHash: contentFingerprint(hex),
      contrastHash: contentFingerprint(contrastHex),
      ratio: ratio.toFixed(2)
    }),
    summary: t('session.summary', { color: formats.hex, ratio: ratio.toFixed(2) })
  }), [contrastHex, formats.hex, hex, ratio, t])
  const { sessionId, reportError } = useToolSessionReport('color', session.digest, session.summary)

  function updateHex(value: string): void {
    setHex(value.toUpperCase())
    try {
      parseHexColor(value)
      succeed('notice.updated')
    } catch (cause) {
      fail(cause)
    }
  }

  async function copy(value: string): Promise<void> {
    try {
      await clipboardApi.writeText(value)
      setCopied(value)
      succeed('notice.copied', { value })
      window.setTimeout(() => setCopied(''), 1200)
    } catch {
      failMessage('error.copy')
    }
  }

  function randomize(): void {
    const value = randomColor()
    setHex(value)
    succeed('notice.random', { value })
  }

  async function sampleScreen(): Promise<void> {
    setSampling(true)
    succeed('notice.capturing')
    try {
      let value: string
      if (window.__TAURI_INTERNALS__) {
        const result = await nativeDesktopApi.captureDisplays()
        const captured = await Promise.all(result.assets.map((asset) => imageApi.read(asset.name)))
        setScreenAssets(captured)
        succeed('notice.displaysCaptured', { count: captured.length })
        return
      } else {
        const EyeDropper = (window as typeof window & {
          EyeDropper?: new () => { open(): Promise<{ sRGBHex: string }> }
        }).EyeDropper
        if (!EyeDropper) throw new ColorLocalizedError('error.eyeDropper')
        value = (await new EyeDropper().open()).sRGBHex
      }
      updateHex(value)
      succeed('notice.screenColor', { value: value.toUpperCase() })
    } catch (cause) {
      fail(cause)
    } finally {
      setSampling(false)
    }
  }

  async function closeScreenPicker() {
    const names = screenAssets.map((asset) => asset.name)
    setScreenAssets([])
    try {
      if (names.length) await imageApi.delete(names)
      succeed('notice.cancelled')
    } catch (cause) {
      fail(cause)
    }
  }

  async function confirmScreenColor(sample: ScreenPixelSample) {
    const names = screenAssets.map((asset) => asset.name)
    setScreenAssets([])
    try {
      if (names.length) await imageApi.delete(names)
      updateHex(sample.hex)
      succeed('notice.screenColorPosition', { value: sample.hex, x: sample.x, y: sample.y })
    } catch (cause) {
      fail(cause)
    }
  }

  function succeed(key: ColorMessageKey, values?: MessageValues) { setNotice({ key, values }); setFailed(false) }
  function failMessage(key: ColorMessageKey, values?: MessageValues) { setNotice({ key, values }); setFailed(true) }
  function fail(cause: unknown) {
    if (cause instanceof ColorToolError) setNotice({ key: 'error.invalidHex' })
    else if (cause instanceof ColorLocalizedError) setNotice({ key: cause.key, values: cause.values })
    else setNotice({ raw: cause instanceof Error ? cause.message : String(cause) })
    setFailed(true)
  }

  return (
    <main className="utility-workbench color-workbench">
      <header className="utility-header">
        <div>
          <span className="eyebrow">TAURI COLOR WORKBENCH</span>
          <h1>{t('title')}</h1>
        </div>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>

      <section className="color-grid">
        <section className="color-hero-card">
          <div className="color-hero" style={{ background: formats.hex, color: bestTextColor(parsed) }}>
            <Palette />
            <strong>{formats.hex}</strong>
            <span>{t('hero.subtitle')}</span>
          </div>
          <div className="color-picker-row">
            <input
              aria-label={t('picker.label')}
              type="color"
              value={formats.hex.slice(0, 7)}
              onChange={(event) => updateHex(event.target.value)}
            />
            <label>HEX
              <input value={hex} onChange={(event) => updateHex(event.target.value)} />
            </label>
            <button className="secondary-button" type="button" onClick={randomize}><Dices />{t('action.random')}</button>
            <button className="secondary-button" type="button" disabled={sampling} onClick={() => void sampleScreen()}>
              <Pipette />{t(sampling ? 'action.waiting' : 'action.pickScreen')}
            </button>
          </div>
        </section>

        <section className="color-format-card">
          <header><strong>{t('formats.title')}</strong><span>{t('formats.copyHint')}</span></header>
          {Object.entries(formats).map(([name, value]) => (
            <button type="button" key={name} onClick={() => void copy(value)}>
              <span>{name.toUpperCase()}</span>
              <code>{value}</code>
              {copied === value ? <Check /> : <Copy />}
            </button>
          ))}
        </section>

        <section className="color-scale-card">
          <header><strong>{t('scale.title')}</strong><span>96 → 12% lightness</span></header>
          <div className="color-scale">
            {scale.map((value, index) => (
              <button
                type="button"
                key={`${value}-${index}`}
                style={{ background: value, color: bestTextColor(parseHexColor(value)) }}
                onClick={() => void copy(value)}
                title={t('action.copyColor', { value })}
              >
                <span>{(index + 1) * 100}</span>
                <code>{value}</code>
              </button>
            ))}
          </div>
        </section>

        <section className="color-contrast-card">
          <header><Contrast /><strong>{t('contrast.title')}</strong></header>
          <div
            className="color-contrast-preview"
            style={{ background: colorFormats(comparison).hex, color: formats.hex }}
          >
            MooTool Aa
          </div>
          <div className="color-contrast-controls">
            <label>{t('contrast.foreground')} <code>{formats.hex}</code></label>
            <label>{t('contrast.background')}
              <input
                type="color"
                value={colorFormats(comparison).hex}
                onChange={(event) => setContrastHex(event.target.value.toUpperCase())}
              />
              <input value={contrastHex} onChange={(event) => setContrastHex(event.target.value.toUpperCase())} />
            </label>
          </div>
          <div className="color-contrast-score">
            <strong>{ratio.toFixed(2)}:1</strong>
            <Badge pass={ratio >= 4.5} label={t('contrast.aaText')} />
            <Badge pass={ratio >= 3} label={t('contrast.aaLarge')} />
            <Badge pass={ratio >= 7} label={t('contrast.aaaText')} />
          </div>
        </section>
      </section>

      <footer className={failed ? 'utility-status utility-status--error' : 'utility-status'}>
        <span>{failed ? <TriangleAlert /> : <CheckCircle2 />}{noticeText}</span>
        <span>{t('footer.capabilities')}</span>
        <code>{session.summary}</code>
      </footer>
      {screenAssets.length > 0 && (
        <ScreenColorEditor
          assets={screenAssets}
          onCancel={() => void closeScreenPicker()}
          onConfirm={(sample) => void confirmScreenColor(sample)}
        />
      )}
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
    </main>
  )
}

interface ScreenPixelSample {
  hex: string
  red: number
  green: number
  blue: number
  x: number
  y: number
  clientX: number
  clientY: number
}

function ScreenColorEditor({ assets, onCancel, onConfirm }: {
  assets: ImageAsset[]
  onCancel: () => void
  onConfirm: (sample: ScreenPixelSample) => void
}) {
  const { t } = useLocalizedMessages(colorMessages)
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const [index, setIndex] = useState(0)
  const [preview, setPreview] = useState<ScreenPixelSample>()
  const asset = assets[Math.min(index, assets.length - 1)]

  useEffect(() => {
    setPreview(undefined)
    const canvas = canvasRef.current
    if (!canvas) return
    const context = canvas.getContext('2d', { willReadFrequently: true })
    if (!context) return
    const image = new Image()
    image.onload = () => {
      canvas.width = asset.width
      canvas.height = asset.height
      context.drawImage(image, 0, 0, asset.width, asset.height)
    }
    image.src = asset.dataUrl
  }, [asset])

  useEffect(() => {
    const handleKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onCancel()
    }
    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
  }, [onCancel])

  function sample(event: React.PointerEvent<HTMLImageElement> | React.MouseEvent<HTMLImageElement>): ScreenPixelSample | undefined {
    const canvas = canvasRef.current
    const context = canvas?.getContext('2d', { willReadFrequently: true })
    if (!canvas || !context || !canvas.width || !canvas.height) return
    const rect = event.currentTarget.getBoundingClientRect()
    const x = Math.max(0, Math.min(asset.width - 1, Math.floor((event.clientX - rect.left) * asset.width / rect.width)))
    const y = Math.max(0, Math.min(asset.height - 1, Math.floor((event.clientY - rect.top) * asset.height / rect.height)))
    const [red, green, blue] = context.getImageData(x, y, 1, 1).data
    return {
      hex: `#${red.toString(16).padStart(2, '0')}${green.toString(16).padStart(2, '0')}${blue.toString(16).padStart(2, '0')}`.toUpperCase(),
      red,
      green,
      blue,
      x,
      y,
      clientX: event.clientX,
      clientY: event.clientY
    }
  }

  return (
    <section className="screen-color-editor" role="dialog" aria-modal="true" aria-label={t('screen.dialog')}>
      <header><div><Pipette /><strong>{t('screen.dialog')}</strong><span>{t('screen.instructions')}</span></div><button type="button" aria-label={t('screen.cancel')} onClick={onCancel}><X /></button></header>
      {assets.length > 1 && <nav>{assets.map((item, itemIndex) => <button className={itemIndex === index ? 'capture-monitor capture-monitor--active' : 'capture-monitor'} type="button" key={item.name} onClick={() => setIndex(itemIndex)}><Monitor />{t('screen.monitor', { number: itemIndex + 1 })}<span>{item.width}×{item.height}</span></button>)}</nav>}
      <div className="screen-color-stage">
        <img src={asset.dataUrl} alt={t('screen.alt', { number: index + 1 })} draggable={false} onPointerMove={(event) => setPreview(sample(event))} onClick={(event) => { const value = sample(event); if (value) onConfirm(value) }} />
        {preview && <div className="screen-color-preview" style={{ left: Math.min(window.innerWidth - 178, preview.clientX + 18), top: Math.min(window.innerHeight - 74, preview.clientY + 18) }}><span style={{ background: preview.hex }} /><strong>{preview.hex}</strong><small>RGB {preview.red}, {preview.green}, {preview.blue}</small></div>}
      </div>
      <canvas ref={canvasRef} hidden />
    </section>
  )
}

function Badge({ pass, label }: { pass: boolean; label: string }) {
  const { t } = useLocalizedMessages(colorMessages)
  return <span className={pass ? 'color-badge color-badge--pass' : 'color-badge'}>{t(pass ? 'badge.pass' : 'badge.fail')} {label}</span>
}
