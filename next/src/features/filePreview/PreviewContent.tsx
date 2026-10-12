import { Copy, Minus, Plus, Search } from 'lucide-react'
import { lazy, Suspense, useEffect, useMemo, useRef, useState } from 'react'
import { TextCodeEditor, type TextCodeEditorHandle } from '@/shared/components/TextCodeEditor'
import { FindReplaceBar } from '@/shared/components/FindReplaceBar'
import { defaultFindReplaceOptions } from '@/shared/components/findReplace'
import { useToolActivity } from '@/shared/components/ToolActivity'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { decodePreviewText, jsonPreview, PreviewError, previewLanguage, readPreviewBytes, scanPreviewMatches, type PreviewKind, type PreviewStateProps, type TextEncoding } from './filePreview'

const PdfPreview = lazy(() => import('./PdfPreview').then(module => ({ default: module.PdfPreview })))

export function PreviewContent({ file, kind, ...stateProps }: { file: File; kind: PreviewKind } & PreviewStateProps) {
  const { t } = useI18n()
  if (kind === 'pdf') return <Suspense fallback={<p className="file-preview-empty" role="status">{t('common.loading')}</p>}><PdfPreview file={file} {...stateProps} /></Suspense>
  if (kind === 'image') return <ImagePreview file={file} {...stateProps} />
  return <TextPreview file={file} json={kind === 'json'} {...stateProps} />
}

function TextPreview({ file, json, initialState, onStateChange }: { file: File; json: boolean } & PreviewStateProps) {
  const { t } = useI18n()
  const actions = useToolActions('filePreview')
  const active = useToolActivity()
  const [bytes, setBytes] = useState<Uint8Array | null>(null)
  const [error, setError] = useState(false)
  const [encoding, setEncoding] = useState<TextEncoding>(initialState?.encoding ?? 'auto')
  const [formatted, setFormatted] = useState(initialState?.formatted ?? true)
  const [wrap, setWrap] = useState(initialState?.wrap ?? true)
  const [findOpen, setFindOpen] = useState(initialState?.findOpen ?? false)
  const [findQuery, setFindQuery] = useState(initialState?.findQuery ?? '')
  const [findOptions, setFindOptions] = useState(initialState?.findOptions ?? defaultFindReplaceOptions)
  const [findFocus, setFindFocus] = useState(0)
  const editor = useRef<TextCodeEditorHandle>(null)
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
  const matches = useMemo(() => scanPreviewMatches(value, findQuery, findOptions), [value, findQuery, findOptions])
  const ready = !!result && !result.error && !error
  function openFind(): void {
    setFindOpen(true); setFindFocus(value => value + 1); onStateChange({ findOpen: true })
  }
  function closeFind(): void { setFindOpen(false); onStateChange({ findOpen: false }); editor.current?.focus() }
  useEffect(() => {
    if (!active) return
    function handleKey(event: KeyboardEvent): void {
      if (event.defaultPrevented) return
      if ((event.metaKey || event.ctrlKey) && !event.altKey && !event.shiftKey && event.key.toLowerCase() === 'f' && ready) { event.preventDefault(); openFind() }
      else if (event.key === 'Escape' && findOpen && !event.metaKey && !event.ctrlKey && !event.altKey && !event.shiftKey) { event.preventDefault(); closeFind() }
    }
    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
  }, [active, ready, findOpen])
  function navigate(forward: boolean): void {
    if (!ready || !findQuery) return
    const selection = editor.current?.getSelection()
    const { match, wrapped } = scanPreviewMatches(value, findQuery, findOptions, forward ? selection?.end ?? 0 : selection?.start ?? 0, forward)
    if (!match) { actions.toast.info(t('findReplace.noMatches')); return }
    if (wrapped) actions.toast.info(t(forward ? 'findReplace.wrappedToStart' : 'findReplace.wrappedToEnd'))
    editor.current?.selectRange(match.start, match.end)
  }
  return <div className="file-preview-content" onKeyDownCapture={event => {
    if ((event.metaKey || event.ctrlKey) && !event.altKey && !event.shiftKey && event.key.toLowerCase() === 'f' && ready) { event.preventDefault(); event.stopPropagation(); openFind() }
    else if (event.key === 'Escape' && findOpen) { event.preventDefault(); event.stopPropagation(); closeFind() }
  }}>
    <div className="file-preview-options">
      <label>{t('filePreview.encoding')}<select aria-label={t('filePreview.encoding')} value={encoding} onChange={event => { const encoding = event.target.value as TextEncoding; setEncoding(encoding); onStateChange({ encoding }) }}>{(['auto', 'utf-8', 'utf-16le', 'utf-16be', 'gb18030'] as const).map(value => <option key={value} value={value}>{value === 'auto' ? t('filePreview.autoEncoding') : value.toUpperCase()}</option>)}</select></label>
      <label><input type="checkbox" checked={wrap} onChange={event => { setWrap(event.target.checked); onStateChange({ wrap: event.target.checked }) }} />{t('filePreview.wrap')}</label>
      {json && <label><input type="checkbox" checked={formatted} onChange={event => { setFormatted(event.target.checked); onStateChange({ formatted: event.target.checked }) }} />{t('filePreview.formatJson')}</label>}
      <button className="toolbar-button toolbar-button--icon" type="button" disabled={!ready} aria-label={t('findReplace.find')} title={t('filePreview.findShortcut')} onClick={openFind}><Search size={14} /></button>
      <button className="toolbar-button toolbar-button--icon" type="button" disabled={!result || !!result.error || error} aria-label={t('common.action.copy')} onClick={() => { void actions.copy(value) }}><Copy size={14} /></button>
    </div>
    {findOpen && <FindReplaceBar className="file-preview-find" findText={findQuery} replaceText="" showReplace={false} allowRegex={false} options={findOptions} matchCount={ready ? matches.count : 0} replacedCount={0} focusRequest={findFocus}
      onFindTextChange={query => { const findQuery = query.slice(0, 256); setFindQuery(findQuery); onStateChange({ findQuery }) }}
      onOptionsChange={options => { const findOptions = { ...options, regex: false }; setFindOptions(findOptions); onStateChange({ findOptions }) }}
      onFind={() => navigate(true)} onFindPrevious={() => navigate(false)} onFindNext={() => navigate(true)} onReplaceTextChange={() => {}} onReplace={() => {}} onReplaceAll={() => {}} onClose={closeFind} />}
    {error || result?.error ? <p className="file-preview-empty" role="status">{t(`filePreview.error.${error ? 'unreadable' : result!.error!}`)}</p>
      : !result ? <p className="file-preview-empty" role="status">{t('common.loading')}</p>
        : <><div className="file-preview-text-status" role="status">{result.encoding.toUpperCase()} · {json && result.parsed?.valid === false ? t('filePreview.invalidJson') : t('filePreview.readOnly')}</div><TextCodeEditor ref={editor} value={value} ariaLabel={t('filePreview.text')} readOnly language={previewLanguage(file.name)} wrap={wrap} initialViewState={initialState?.editor}
          onViewStateChange={editor => onStateChange({ editor })} searchQuery={findOpen ? findQuery : ''} searchOptions={findOptions} /></>}
  </div>
}

function ImagePreview({ file, initialState, onStateChange }: { file: File } & PreviewStateProps) {
  const { t } = useI18n()
  const [url, setUrl] = useState('')
  const [zoom, setZoom] = useState<number | null>(initialState?.imageZoom ?? null)
  function changeZoom(zoom: number | null): void { setZoom(zoom); onStateChange({ imageZoom: zoom }) }
  const [dimensions, setDimensions] = useState<{ width: number; height: number } | null>(null)
  const [error, setError] = useState(false)
  useEffect(() => {
    const url = URL.createObjectURL(file)
    setUrl(url); setDimensions(null); setError(false)
    return () => URL.revokeObjectURL(url)
  }, [file])
  return <div className="file-preview-content">
    <div className="file-preview-options">
      <button className="toolbar-button" type="button" onClick={() => changeZoom(null)} aria-pressed={zoom === null}>{t('filePreview.fitImage')}</button>
      <button className="toolbar-button" type="button" onClick={() => changeZoom(1)} aria-pressed={zoom === 1}>100%</button>
      <button className="toolbar-button toolbar-button--icon" type="button" aria-label={t('filePreview.zoomOut')} onClick={() => changeZoom(Math.max(.1, (zoom ?? 1) / 1.25))}><Minus size={14} /></button>
      <button className="toolbar-button toolbar-button--icon" type="button" aria-label={t('filePreview.zoomIn')} onClick={() => changeZoom(Math.min(4, (zoom ?? 1) * 1.25))}><Plus size={14} /></button>
      <span>{zoom !== null && `${Math.round(zoom * 100)}%`} {dimensions && `${dimensions.width} × ${dimensions.height}`}</span>
    </div>
    {error ? <p className="file-preview-empty" role="status">{t('filePreview.error.image')}</p> : <div className={`file-preview-image ${zoom === null ? 'file-preview-image--fit' : ''}`}>
      {url && <img src={url} alt={file.name} style={zoom !== null && dimensions ? { width: dimensions.width * zoom, height: dimensions.height * zoom } : undefined}
        onLoad={event => setDimensions({ width: event.currentTarget.naturalWidth, height: event.currentTarget.naturalHeight })} onError={() => setError(true)} />}
    </div>}
  </div>
}
