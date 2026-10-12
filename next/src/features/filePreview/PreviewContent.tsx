import { Copy, Minus, Plus } from 'lucide-react'
import { lazy, Suspense, useEffect, useMemo, useState } from 'react'
import { TextCodeEditor } from '@/shared/components/TextCodeEditor'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { decodePreviewText, jsonPreview, PreviewError, previewLanguage, readPreviewBytes, type PreviewKind, type TextEncoding } from './filePreview'

const PdfPreview = lazy(() => import('./PdfPreview').then(module => ({ default: module.PdfPreview })))

export function PreviewContent({ file, kind }: { file: File; kind: PreviewKind }) {
  const { t } = useI18n()
  if (kind === 'pdf') return <Suspense fallback={<p className="file-preview-empty" role="status">{t('common.loading')}</p>}><PdfPreview file={file} /></Suspense>
  if (kind === 'image') return <ImagePreview file={file} />
  return <TextPreview file={file} json={kind === 'json'} />
}

function TextPreview({ file, json }: { file: File; json: boolean }) {
  const { t } = useI18n()
  const actions = useToolActions('filePreview')
  const [bytes, setBytes] = useState<Uint8Array | null>(null)
  const [error, setError] = useState(false)
  const [encoding, setEncoding] = useState<TextEncoding>('auto')
  const [formatted, setFormatted] = useState(true)
  const [wrap, setWrap] = useState(true)
  useEffect(() => {
    let active = true
    setBytes(null); setError(false)
    readPreviewBytes(file, json ? 'json' : 'text').then(value => { if (active) setBytes(value) }, () => { if (active) setError(true) })
    return () => { active = false }
  }, [file, json])
  const result = useMemo(() => {
    if (!bytes) return null
    try {
      const decoded = decodePreviewText(bytes, encoding)
      const parsed = json ? jsonPreview(decoded.text, file.name) : null
      return { ...decoded, parsed, error: null }
    } catch (error) { return { text: '', encoding, parsed: null, error: error instanceof PreviewError ? error.code : 'unreadable' as const } }
  }, [bytes, encoding, file.name, json])
  const value = result?.parsed && formatted ? result.parsed.formatted : result?.text ?? ''
  return <div className="file-preview-content">
    <div className="file-preview-options">
      <label>{t('filePreview.encoding')}<select aria-label={t('filePreview.encoding')} value={encoding} onChange={event => setEncoding(event.target.value as TextEncoding)}>{(['auto', 'utf-8', 'utf-16le', 'utf-16be', 'gb18030'] as const).map(value => <option key={value} value={value}>{value === 'auto' ? t('filePreview.autoEncoding') : value.toUpperCase()}</option>)}</select></label>
      <label><input type="checkbox" checked={wrap} onChange={event => setWrap(event.target.checked)} />{t('filePreview.wrap')}</label>
      {json && <label><input type="checkbox" checked={formatted} onChange={event => setFormatted(event.target.checked)} />{t('filePreview.formatJson')}</label>}
      <button className="toolbar-button toolbar-button--icon" type="button" disabled={!result || !!result.error || error} aria-label={t('common.action.copy')} onClick={() => { void actions.copy(value) }}><Copy size={14} /></button>
    </div>
    {error || result?.error ? <p className="file-preview-empty" role="status">{t(`filePreview.error.${error ? 'unreadable' : result!.error!}`)}</p>
      : !result ? <p className="file-preview-empty" role="status">{t('common.loading')}</p>
        : <><div className="file-preview-text-status" role="status">{result.encoding.toUpperCase()} · {json && result.parsed?.valid === false ? t('filePreview.invalidJson') : t('filePreview.readOnly')}</div><TextCodeEditor value={value} ariaLabel={t('filePreview.text')} readOnly language={previewLanguage(file.name)} wrap={wrap} /></>}
  </div>
}

function ImagePreview({ file }: { file: File }) {
  const { t } = useI18n()
  const [url, setUrl] = useState('')
  const [zoom, setZoom] = useState<number | null>(null)
  const [dimensions, setDimensions] = useState<{ width: number; height: number } | null>(null)
  const [error, setError] = useState(false)
  useEffect(() => {
    const url = URL.createObjectURL(file)
    setUrl(url); setZoom(null); setDimensions(null); setError(false)
    return () => URL.revokeObjectURL(url)
  }, [file])
  return <div className="file-preview-content">
    <div className="file-preview-options">
      <button className="toolbar-button" type="button" onClick={() => setZoom(null)} aria-pressed={zoom === null}>{t('filePreview.fitImage')}</button>
      <button className="toolbar-button" type="button" onClick={() => setZoom(1)} aria-pressed={zoom === 1}>100%</button>
      <button className="toolbar-button toolbar-button--icon" type="button" aria-label={t('filePreview.zoomOut')} onClick={() => setZoom(value => Math.max(.1, (value ?? 1) / 1.25))}><Minus size={14} /></button>
      <button className="toolbar-button toolbar-button--icon" type="button" aria-label={t('filePreview.zoomIn')} onClick={() => setZoom(value => Math.min(4, (value ?? 1) * 1.25))}><Plus size={14} /></button>
      <span>{zoom !== null && `${Math.round(zoom * 100)}%`} {dimensions && `${dimensions.width} × ${dimensions.height}`}</span>
    </div>
    {error ? <p className="file-preview-empty" role="status">{t('filePreview.error.image')}</p> : <div className={`file-preview-image ${zoom === null ? 'file-preview-image--fit' : ''}`}>
      {url && <img src={url} alt={file.name} style={zoom !== null && dimensions ? { width: dimensions.width * zoom, height: dimensions.height * zoom } : undefined}
        onLoad={event => setDimensions({ width: event.currentTarget.naturalWidth, height: event.currentTarget.naturalHeight })} onError={() => setError(true)} />}
    </div>}
  </div>
}
