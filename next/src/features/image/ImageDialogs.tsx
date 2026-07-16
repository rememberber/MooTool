import { useEffect, useState } from 'react'
import { Dialog } from '@/shared/components/Dialog'
import type { ScreenCapture } from '@/shared/contracts/images'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { cropImage, type CompressImageOptions, type ImageOutputMode, type WatermarkImageOptions } from './imageTools'

export function ImageBase64Dialog({ open, mode, value, onClose, onImport }: { open: boolean; mode: 'import' | 'export'; value: string; onClose: () => void; onImport: (value: string) => void }) {
  const { t } = useI18n()
  const [text, setText] = useState(value)
  useEffect(() => { if (open) setText(value) }, [open, value])
  return <Dialog title={mode === 'import' ? t('image.base64Import') : t('image.base64Export')} open={open} width={720} onClose={onClose} footer={<><button className="dialog-button" type="button" onClick={onClose}>{t('common.cancel')}</button>{mode === 'import' && <button className="dialog-button dialog-button--primary" type="button" disabled={!text.trim()} onClick={() => onImport(text)}>{t('common.import')}</button>}</>}><textarea className="base64-dialog-text" value={text} readOnly={mode === 'export'} spellCheck={false} onChange={(event) => setText(event.target.value)} /></Dialog>
}

export function ImageCompressDialog({ open, count, onClose, onConfirm }: { open: boolean; count: number; onClose: () => void; onConfirm: (options: CompressImageOptions, mode: ImageOutputMode) => void }) {
  const { t } = useI18n()
  const [quality, setQuality] = useState(80)
  const [scale, setScale] = useState(100)
  const [format, setFormat] = useState<CompressImageOptions['format']>('auto')
  const [mode, setMode] = useState<ImageOutputMode>('keep')
  return <Dialog title={t('image.compressTitle')} open={open} width={520} onClose={onClose} footer={<><button className="dialog-button" type="button" onClick={onClose}>{t('common.cancel')}</button><button className="dialog-button dialog-button--primary" type="button" onClick={() => onConfirm({ quality: quality / 100, scale: scale / 100, format }, mode)}>{t('image.startProcess')}</button></>}><div className="image-options"><p>{t('image.selectedCount', { count: String(count) })}</p><label><span>{t('image.quality')} · {quality}%</span><input type="range" min={10} max={100} value={quality} onChange={(event) => setQuality(Number(event.target.value))} /></label><label><span>{t('image.scale')} · {scale}%</span><input type="range" min={10} max={100} value={scale} onChange={(event) => setScale(Number(event.target.value))} /></label><label><span>{t('image.outputFormat')}</span><select value={format} onChange={(event) => setFormat(event.target.value as CompressImageOptions['format'])}><option value="auto">{t('image.format.auto')}</option><option value="png">PNG</option><option value="jpeg">JPEG</option></select></label><OutputMode value={mode} onChange={setMode} /></div></Dialog>
}

export function ImageWatermarkDialog({ open, count, onClose, onConfirm }: { open: boolean; count: number; onClose: () => void; onConfirm: (options: WatermarkImageOptions, mode: ImageOutputMode) => void }) {
  const { t } = useI18n()
  const [text, setText] = useState('MooTool')
  const [opacity, setOpacity] = useState(50)
  const [color, setColor] = useState('#FFFFFF')
  const [position, setPosition] = useState<WatermarkImageOptions['position']>('bottom-right')
  const [fontSize, setFontSize] = useState<WatermarkImageOptions['fontSize']>('auto')
  const [diagonal, setDiagonal] = useState(false)
  const [mode, setMode] = useState<ImageOutputMode>('keep')
  return <Dialog title={t('image.watermarkTitle')} open={open} width={540} onClose={onClose} footer={<><button className="dialog-button" type="button" onClick={onClose}>{t('common.cancel')}</button><button className="dialog-button dialog-button--primary" type="button" disabled={!text.trim()} onClick={() => onConfirm({ text, opacity: opacity / 100, color, position, fontSize, diagonal }, mode)}>{t('image.startProcess')}</button></>}><div className="image-options"><p>{t('image.selectedCount', { count: String(count) })}</p><label><span>{t('image.watermarkText')}</span><input value={text} onChange={(event) => setText(event.target.value)} /></label><label><span>{t('image.opacity')} · {opacity}%</span><input type="range" min={5} max={100} value={opacity} onChange={(event) => setOpacity(Number(event.target.value))} /></label><div className="image-option-grid"><label><span>{t('image.position')}</span><select value={position} onChange={(event) => setPosition(event.target.value as WatermarkImageOptions['position'])}>{['bottom-right', 'bottom-left', 'top-right', 'top-left', 'center', 'tile'].map((value) => <option key={value} value={value}>{t(`image.position.${value}` as 'image.position.bottom-right')}</option>)}</select></label><label><span>{t('image.fontSize')}</span><select value={fontSize} onChange={(event) => setFontSize(event.target.value as WatermarkImageOptions['fontSize'])}>{['auto', 'small', 'medium', 'large'].map((value) => <option key={value} value={value}>{t(`image.font.${value}` as 'image.font.auto')}</option>)}</select></label><label><span>{t('image.color')}</span><input type="color" value={color} onChange={(event) => setColor(event.target.value)} /></label></div><label className="checkbox-row"><input type="checkbox" checked={diagonal} onChange={(event) => setDiagonal(event.target.checked)} />{t('image.diagonal')}</label><OutputMode value={mode} onChange={setMode} /></div></Dialog>
}

export function ScreenCaptureDialog({ open, sources, onClose, onConfirm }: { open: boolean; sources: ScreenCapture[]; onClose: () => void; onConfirm: (dataUrl: string, sourceName: string) => void }) {
  const { t } = useI18n()
  const [selectedId, setSelectedId] = useState('')
  const selected = sources.find((source) => source.id === selectedId) ?? sources[0]
  const [rect, setRect] = useState({ x: 0, y: 0, width: 1, height: 1 })
  const [busy, setBusy] = useState(false)
  useEffect(() => {
    if (!open || sources.length === 0) return
    setSelectedId(sources[0].id)
    setRect({ x: 0, y: 0, width: sources[0].width, height: sources[0].height })
  }, [open, sources])
  function changeSource(id: string): void {
    setSelectedId(id)
    const next = sources.find((source) => source.id === id)
    if (next) setRect({ x: 0, y: 0, width: next.width, height: next.height })
  }
  async function confirm(): Promise<void> {
    if (!selected) return
    setBusy(true)
    try { onConfirm(await cropImage(selected.dataUrl, rect), selected.name) } finally { setBusy(false) }
  }
  return <Dialog title={t('image.screenshot')} open={open} width={900} onClose={onClose} footer={<><button className="dialog-button" type="button" onClick={onClose}>{t('common.cancel')}</button><button className="dialog-button dialog-button--primary" type="button" disabled={!selected || busy} onClick={() => { void confirm() }}>{busy ? t('common.processing') : t('image.saveCapture')}</button></>}><div className="capture-dialog"><div className="capture-sources">{sources.map((source) => <button className={source.id === selected?.id ? 'capture-source capture-source--active' : 'capture-source'} type="button" key={source.id} onClick={() => changeSource(source.id)}><img src={source.dataUrl} alt={source.name} /><span>{source.name}</span></button>)}</div>{selected && <><div className="capture-preview"><img src={selected.dataUrl} alt={selected.name} /></div><div className="capture-crop-fields">{(['x', 'y', 'width', 'height'] as const).map((key) => <label key={key}><span>{t(`image.crop.${key}` as 'image.crop.x')}</span><input type="number" min={0} value={rect[key]} onChange={(event) => setRect((current) => ({ ...current, [key]: Number(event.target.value) }))} /></label>)}</div></>}</div></Dialog>
}

function OutputMode({ value, onChange }: { value: ImageOutputMode; onChange: (value: ImageOutputMode) => void }) {
  const { t } = useI18n()
  return <fieldset className="output-mode"><legend>{t('image.outputMode')}</legend><label><input type="radio" name="image-output-mode" value="keep" checked={value === 'keep'} onChange={() => onChange('keep')} />{t('image.keepOriginal')}</label><label><input type="radio" name="image-output-mode" value="overwrite" checked={value === 'overwrite'} onChange={() => onChange('overwrite')} />{t('image.overwrite')}</label></fieldset>
}
