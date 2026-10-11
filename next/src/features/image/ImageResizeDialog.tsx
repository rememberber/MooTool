import { useState } from 'react'
import { Dialog } from '@/shared/components/Dialog'
import type { ImageAssetSummary } from '@/shared/contracts/images'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { OutputMode } from './ImageDialogs'
import { resizeGeometry, type ImageOutputMode, type ResizeImageOptions } from './imageTools'

const presets = { small: [640, 480], medium: [1280, 720], large: [1920, 1080] } as const
type Preset = keyof typeof presets | 'custom'

export function ImageResizeDialog({ open, assets, onClose, onConfirm }: {
  open: boolean
  assets: ImageAssetSummary[]
  onClose: () => void
  onConfirm: (options: ResizeImageOptions, mode: ImageOutputMode) => void
}) {
  const { t } = useI18n()
  const [preset, setPreset] = useState<Preset>('medium')
  const [width, setWidth] = useState('1280')
  const [height, setHeight] = useState('720')
  const [fit, setFit] = useState<ResizeImageOptions['fit']>('fit')
  const [shrinkOnly, setShrinkOnly] = useState(true)
  const [format, setFormat] = useState<ResizeImageOptions['format']>('auto')
  const [mode, setMode] = useState<ImageOutputMode>('keep')
  if (!open) return null

  const options: ResizeImageOptions = { width: width === '' ? null : Number(width), height: height === '' ? null : Number(height), fit, shrinkOnly, format }
  const rows = assets.map(asset => {
    try { return { asset, output: resizeGeometry(asset.width, asset.height, options), error: null } }
    catch (error) { return { asset, output: null, error: error instanceof Error && error.message === 'image.resize.tooLarge' ? t('image.resize.tooLarge') : t('image.resize.invalid') } }
  })
  const valid = rows.length > 0 && rows.every(row => row.output)
  const error = rows.find(row => row.error)?.error

  function selectPreset(value: Preset) {
    setPreset(value)
    if (value !== 'custom') { setWidth(String(presets[value][0])); setHeight(String(presets[value][1])) }
  }

  return <Dialog title={t('image.resize.title')} open={open} width={720} onClose={onClose} footer={<>
    <button className="dialog-button" type="button" onClick={onClose}>{t('common.cancel')}</button>
    <button className="dialog-button dialog-button--primary" type="button" disabled={!valid} onClick={() => { if (valid) onConfirm(options, mode) }}>{t('image.startProcess')}</button>
  </>}>
    <div className="image-options image-resize-options">
      <p>{t('image.selectedCount', { count: String(assets.length) })}</p>
      <div className="image-option-grid">
        <label><span>{t('image.resize.preset')}</span><select aria-label={t('image.resize.preset')} value={preset} onChange={event => selectPreset(event.target.value as Preset)}>
          <option value="small">{t('image.resize.small')} · 640 × 480</option>
          <option value="medium">{t('image.resize.medium')} · 1280 × 720</option>
          <option value="large">{t('image.resize.large')} · 1920 × 1080</option>
          <option value="custom">{t('image.resize.custom')}</option>
        </select></label>
        <label><span>{t('image.resize.width')}</span><input type="number" min={1} max={16384} step={1} placeholder={t('image.resize.auto')} value={width} onChange={event => { setWidth(event.target.value); setPreset('custom') }} /></label>
        <label><span>{t('image.resize.height')}</span><input type="number" min={1} max={16384} step={1} placeholder={t('image.resize.auto')} value={height} onChange={event => { setHeight(event.target.value); setPreset('custom') }} /></label>
      </div>
      <p>{t('image.resize.dimensionsHint')}</p>
      <div className="image-option-grid image-resize-mode-grid">
        <label><span>{t('image.resize.mode')}</span><select aria-label={t('image.resize.mode')} value={fit} onChange={event => setFit(event.target.value as ResizeImageOptions['fit'])}>
          <option value="fit">{t('image.resize.fit')}</option>
          <option value="fill">{t('image.resize.fill')}</option>
          <option value="stretch">{t('image.resize.stretch')}</option>
        </select></label>
        <label><span>{t('image.outputFormat')}</span><select aria-label={t('image.outputFormat')} value={format} onChange={event => setFormat(event.target.value as ResizeImageOptions['format'])}>
          <option value="auto">{t('image.resize.formatAuto')}</option><option value="png">PNG</option><option value="jpeg">JPEG</option>
        </select></label>
      </div>
      <label className="checkbox-row"><input type="checkbox" checked={shrinkOnly} onChange={event => setShrinkOnly(event.target.checked)} />{t('image.resize.shrinkOnly')}</label>
      <p>{t(fit === 'fit' ? 'image.resize.fitHint' : fit === 'fill' ? 'image.resize.fillHint' : 'image.resize.stretchHint')}</p>
      {fit === 'fill' && shrinkOnly && <p>{t('image.resize.shrinkFillHint')}</p>}
      <OutputMode value={mode} onChange={setMode} />
      <p>{t('image.resize.libraryHint')}</p>
      <div className="image-resize-preview" role="region" aria-label={t('image.resize.preview')} tabIndex={0}>
        <table><caption>{t('image.resize.preview')}</caption><thead><tr><th>{t('image.resize.file')}</th><th>{t('image.resize.source')}</th><th>{t('image.resize.output')}</th></tr></thead><tbody>
          {rows.map(row => <tr key={row.asset.name}><td title={row.asset.name}>{row.asset.name}</td><td>{row.asset.width} × {row.asset.height}</td><td>{row.output ? `${row.output.width} × ${row.output.height}` : '—'}</td></tr>)}
        </tbody></table>
      </div>
      {error && <p className="image-resize-error" role="alert">{error}</p>}
    </div>
  </Dialog>
}
