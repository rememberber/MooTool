import { ChevronLeft, ChevronRight, FolderOpen, Minus, Plus, Search, Trash2, X } from 'lucide-react'
import { lazy, Suspense, useEffect, useMemo, useRef, useState, type DragEvent } from 'react'
import type { EditorView } from '@codemirror/view'
import { useLocalizedMessages, type LocalizedMessageKey } from '../../app/localizedMessages'
import { CodeEditor } from '../../shared/CodeEditor'
import { defaultFindReplaceOptions, type FindReplaceOptions } from '../../shared/findReplace'
import {
  classifyPreview,
  decodePreviewText,
  formatPreviewSize,
  jsonPreview,
  PreviewError,
  previewBatchLimit,
  previewLimits,
  readPreviewBytes,
  scanPreviewMatches,
  type PreviewKind,
  type PreviewViewState,
  type TextEncoding
} from './filePreview'
import { filePreviewMessages } from './filePreviewMessages'

const PdfPreview = lazy(() => import('./PdfPreview').then((module) => ({ default: module.PdfPreview })))

type PreviewMessageKey = LocalizedMessageKey<typeof filePreviewMessages>

export function FilePreviewSurface() {
  const { t } = useLocalizedMessages(filePreviewMessages)
  const [files, setFiles] = useState<File[]>([])
  const [index, setIndex] = useState(0)
  const [notice, setNotice] = useState<PreviewMessageKey | ''>('')
  const [batch, setBatch] = useState(0)
  const input = useRef<HTMLInputElement>(null)
  const root = useRef<HTMLElement>(null)
  const views = useRef({ batch: 0, states: new Map<number, PreviewViewState>() })
  const file = files[index]
  const kind = file ? classifyPreview(file.name, file.type) : null

  function select(next: File[]): void {
    if (next.length > previewBatchLimit) {
      setNotice('batchLimit')
      return
    }
    if (!next.length) return
    views.current = { batch: views.current.batch + 1, states: new Map() }
    setFiles(next)
    setIndex(0)
    setNotice('')
    setBatch(views.current.batch)
    root.current?.focus()
  }

  function clearFiles(): void {
    views.current = { batch: views.current.batch + 1, states: new Map() }
    setBatch(views.current.batch)
    setFiles([])
    setIndex(0)
    setNotice('')
  }

  function drop(event: DragEvent): void {
    event.preventDefault()
    select(Array.from(event.dataTransfer.files))
  }

  return (
    <main
      ref={root}
      className="utility-workbench file-preview-workbench"
      tabIndex={-1}
      onDragOver={(event) => event.preventDefault()}
      onDrop={drop}
      onKeyDown={(event) => {
        if (event.defaultPrevented || event.altKey || event.ctrlKey || event.metaKey || event.shiftKey) return
        if (event.target instanceof Element && event.target.closest('input, select, textarea, .cm-editor, [contenteditable="true"]')) return
        if (event.key === 'ArrowLeft' && index > 0) { event.preventDefault(); setIndex(index - 1) }
        if (event.key === 'ArrowRight' && index < files.length - 1) { event.preventDefault(); setIndex(index + 1) }
      }}
    >
      <header className="utility-header">
        <h1 className="visually-hidden">{t('title')}</h1>
      </header>
      <section className="utility-toolbar">
        <button className="secondary-button" type="button" onClick={() => input.current?.click()}><FolderOpen />{t('action.choose')}</button>
        <span>{files.length ? `${index + 1} / ${files.length}` : ''}</span>
        <button className="secondary-button" type="button" aria-label={t('action.previous')} disabled={index === 0 || !file} onClick={() => setIndex(index - 1)}><ChevronLeft />{t('action.previous')}</button>
        <button className="secondary-button" type="button" aria-label={t('action.next')} disabled={index >= files.length - 1} onClick={() => setIndex(index + 1)}><ChevronRight />{t('action.next')}</button>
        <button className="secondary-button" type="button" disabled={!files.length} onClick={clearFiles}><Trash2 />{t('action.clear')}</button>
      </section>
      <input
        ref={input}
        className="visually-hidden"
        type="file"
        multiple
        aria-label={t('action.choose')}
        onChange={(event) => {
          select(Array.from(event.target.files ?? []))
          event.target.value = ''
        }}
      />
      {notice && <p className="tool-surface-report-error" role="alert">{t(notice)}</p>}
      {file && kind ? (
        <section className="file-preview-layout">
          <nav aria-label={t('batch')}>
            {files.map((item, position) => (
              <button className={position === index ? 'utility-segment utility-segment--active' : 'utility-segment'} type="button" key={`${item.name}-${item.lastModified}-${position}`} onClick={() => setIndex(position)}>
                {item.name}
                <small>{t(`kind.${classifyPreview(item.name, item.type)}`)} · {formatPreviewSize(item.size)}</small>
              </button>
            ))}
          </nav>
          <PreviewPane
            key={`${batch}:${index}`}
            file={file}
            kind={kind}
            initialState={views.current.states.get(index)}
            onStateChange={(patch) => {
              if (views.current.batch !== batch) return
              views.current.states.set(index, { ...views.current.states.get(index), ...patch })
            }}
          />
        </section>
      ) : (
        <button className="file-preview-drop" type="button" onClick={() => input.current?.click()}>{t('empty')}</button>
      )}
    </main>
  )
}

function PreviewPane({ file, kind, initialState, onStateChange }: {
  file: File
  kind: PreviewKind
  initialState?: PreviewViewState
  onStateChange: (patch: Partial<PreviewViewState>) => void
}) {
  const { t } = useLocalizedMessages(filePreviewMessages)
  const [encoding, setEncoding] = useState<TextEncoding>(initialState?.encoding ?? 'auto')
  const [formatted, setFormatted] = useState(initialState?.formatted ?? true)
  const [wrap, setWrap] = useState(initialState?.wrap ?? true)
  const [text, setText] = useState('')
  const [error, setError] = useState('')
  const [imageUrl, setImageUrl] = useState('')
  const [jsonValid, setJsonValid] = useState(true)
  const [encodingName, setEncodingName] = useState('utf-8')
  const [findQuery, setFindQuery] = useState(initialState?.findQuery ?? '')
  const [findOpen, setFindOpen] = useState(initialState?.findOpen ?? false)
  const [findOptions, setFindOptions] = useState<FindReplaceOptions>(initialState?.findOptions ?? defaultFindReplaceOptions)
  const [findNotice, setFindNotice] = useState<PreviewMessageKey | ''>('')
  const [zoom, setZoom] = useState<number | null>(initialState?.imageZoom ?? null)
  const [dimensions, setDimensions] = useState<{ width: number; height: number } | null>(null)
  const editor = useRef<EditorView | null>(null)

  useEffect(() => {
    let active = true
    setText('')
    setError('')
    setJsonValid(true)
    if (kind === 'unsupported') return
    if (kind === 'pdf') return
    if (file.size > previewLimits[kind]) {
      setError(t('tooLarge', { limit: formatPreviewSize(previewLimits[kind]) }))
      return
    }
    if (kind === 'image') {
      const url = URL.createObjectURL(file)
      setImageUrl(url)
      return () => {
        active = false
        URL.revokeObjectURL(url)
      }
    }
    void readPreviewBytes(file, kind).then((bytes) => {
      if (!active) return
      try {
        const decoded = decodePreviewText(bytes, encoding)
        setEncodingName(decoded.encoding)
        if (kind === 'json') {
          const parsed = jsonPreview(decoded.text, file.name)
          setJsonValid(parsed.valid)
          setText(formatted && parsed.valid ? parsed.formatted : decoded.text)
        } else {
          setText(decoded.text)
        }
      } catch (cause) {
        setError(cause instanceof PreviewError ? t(`error.${cause.code === 'tooLarge' ? 'unreadable' : cause.code}`) : t('error.unreadable'))
      }
    }).catch(() => {
      if (active) setError(t('error.unreadable'))
    })
    return () => { active = false }
  }, [encoding, file, formatted, kind, t])

  const encodings = useMemo(() => ['auto', 'utf-8', 'utf-16le', 'utf-16be', 'gb18030'] as const, [])

  if (kind === 'unsupported') return <p role="status">{t('unsupported')}</p>
  if (kind === 'pdf') {
    return (
      <Suspense fallback={<p className="file-preview-empty" role="status">{t('loading')}</p>}>
        <PdfPreview file={file} initialPage={initialState?.pdfPage ?? 1} initialZoom={initialState?.pdfZoom ?? 1} onViewChange={onStateChange} />
      </Suspense>
    )
  }
  const matches = scanPreviewMatches(text, findQuery, findOptions)
  const previewOptions = { ...findOptions, regex: false as const }

  function openFind(): void {
    setFindOpen(true)
    setFindNotice('')
    onStateChange({ findOpen: true })
  }

  function jump(forward: boolean): void {
    if (!findQuery) return
    const view = editor.current
    const from = view ? (forward ? view.state.selection.main.to : view.state.selection.main.from) : 0
    const found = scanPreviewMatches(text, findQuery, previewOptions, from, forward)
    if (!found.match || !view) {
      setFindNotice('find.noMatches')
      return
    }
    setFindNotice(found.wrapped ? (forward ? 'find.wrappedStart' : 'find.wrappedEnd') : '')
    view.dispatch({
      selection: { anchor: found.match.start, head: found.match.end },
      scrollIntoView: true
    })
    view.focus()
  }

  if (kind === 'image') {
    return (
      <section className="file-preview-image">
        <div className="utility-toolbar">
          <button type="button" aria-pressed={zoom === null} onClick={() => { setZoom(null); onStateChange({ imageZoom: null }) }}>{t('image.fit')}</button>
          <button type="button" aria-pressed={zoom === 1} onClick={() => { setZoom(1); onStateChange({ imageZoom: 1 }) }}>{t('image.actual')}</button>
          <button type="button" aria-label={t('image.zoomOut')} onClick={() => { const next = Math.max(0.1, (zoom ?? 1) / 1.25); setZoom(next); onStateChange({ imageZoom: next }) }}><Minus /></button>
          <button type="button" aria-label={t('image.zoomIn')} onClick={() => { const next = Math.min(4, (zoom ?? 1) * 1.25); setZoom(next); onStateChange({ imageZoom: next }) }}><Plus /></button>
          <span>{zoom !== null ? `${Math.round(zoom * 100)}%` : ''} {dimensions ? `${dimensions.width} × ${dimensions.height}` : ''}</span>
        </div>
        {imageUrl && (
          <img
            alt={file.name}
            src={imageUrl}
            style={zoom !== null && dimensions ? { width: dimensions.width * zoom, height: dimensions.height * zoom } : undefined}
            onLoad={(event) => setDimensions({ width: event.currentTarget.naturalWidth, height: event.currentTarget.naturalHeight })}
          />
        )}
      </section>
    )
  }
  return (
    <section
      className="file-preview-text"
      onKeyDownCapture={(event) => {
        if ((event.metaKey || event.ctrlKey) && !event.altKey && !event.shiftKey && event.key.toLowerCase() === 'f' && !error) {
          event.preventDefault()
          event.stopPropagation()
          openFind()
        } else if (event.key === 'Escape' && findOpen) {
          event.preventDefault()
          event.stopPropagation()
          setFindOpen(false)
          setFindNotice('')
          onStateChange({ findOpen: false })
        }
      }}
    >
      <div className="utility-toolbar">
        <label>{t('encoding')}
          <select aria-label={t('encoding')} value={encoding} onChange={(event) => { const next = event.target.value as TextEncoding; setEncoding(next); onStateChange({ encoding: next }) }}>
            {encodings.map((value) => <option key={value} value={value}>{value === 'auto' ? t('encoding.auto') : value.toUpperCase()}</option>)}
          </select>
        </label>
        <label><input type="checkbox" checked={wrap} onChange={(event) => { setWrap(event.target.checked); onStateChange({ wrap: event.target.checked }) }} />{t('wrap')}</label>
        {kind === 'json' && <label><input type="checkbox" checked={formatted} onChange={(event) => { setFormatted(event.target.checked); onStateChange({ formatted: event.target.checked }) }} />{t('formatJson')}</label>}
        <button type="button" disabled={Boolean(error)} onClick={openFind}><Search />{t('action.find')}</button>
        <span>{encodingName.toUpperCase()} · {kind === 'json' && !jsonValid ? t('invalidJson') : t('status.readOnly')}</span>
      </div>
      {findOpen && (
        <div className="file-preview-find">
          <input aria-label={t('action.find')} autoFocus value={findQuery} placeholder={t('action.find')} onChange={(event) => { const findQuery = event.target.value.slice(0, 256); setFindNotice(''); setFindQuery(findQuery); onStateChange({ findQuery }) }} onKeyDown={(event) => { if (event.key === 'Enter' && !event.nativeEvent.isComposing) { event.preventDefault(); jump(!event.shiftKey) } }} />
          <button type="button" disabled={!findQuery} onClick={() => jump(false)}>{t('find.previous')}</button>
          <button type="button" disabled={!findQuery} onClick={() => jump(true)}>{t('find.next')}</button>
          <span>{findQuery ? t('find.count', { count: matches.count }) : ''}</span>
          <label><input type="checkbox" checked={findOptions.matchCase} onChange={(event) => { const next = { ...findOptions, matchCase: event.target.checked, regex: false }; setFindOptions(next); onStateChange({ findOptions: next }) }} />{t('find.matchCase')}</label>
          <label><input type="checkbox" checked={findOptions.wholeWord} onChange={(event) => { const next = { ...findOptions, wholeWord: event.target.checked, regex: false }; setFindOptions(next); onStateChange({ findOptions: next }) }} />{t('find.wholeWord')}</label>
          {findNotice && <span role="status">{t(findNotice)}</span>}
          <button type="button" aria-label={t('find.close')} onClick={() => { setFindOpen(false); setFindNotice(''); onStateChange({ findOpen: false }) }}><X /></button>
        </div>
      )}
      {error ? <p role="alert">{error}</p> : <CodeEditor ariaLabel={file.name} value={text} readOnly lineWrapping={wrap} searchQuery={findOpen ? findQuery : ''} searchOptions={previewOptions} onReady={(view) => { editor.current = view }} />}
    </section>
  )
}
