import { FolderOpen, Minus, Plus, Search, Trash2 } from 'lucide-react'
import { useEffect, useMemo, useRef, useState, type DragEvent } from 'react'
import type { EditorView } from '@codemirror/view'
import { useLocalizedMessages, type LocalizedMessageKey } from '../../app/localizedMessages'
import { CodeEditor } from '../../shared/CodeEditor'
import { defaultFindReplaceOptions } from '../../shared/findReplace'
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
  type TextEncoding
} from './filePreview'
import { filePreviewMessages } from './filePreviewMessages'

type PreviewMessageKey = LocalizedMessageKey<typeof filePreviewMessages>

export function FilePreviewSurface() {
  const { t } = useLocalizedMessages(filePreviewMessages)
  const [files, setFiles] = useState<File[]>([])
  const [index, setIndex] = useState(0)
  const [notice, setNotice] = useState<PreviewMessageKey | ''>('')
  const input = useRef<HTMLInputElement>(null)
  const file = files[index]
  const kind = file ? classifyPreview(file.name, file.type) : null

  function select(next: File[]): void {
    if (next.length > previewBatchLimit) {
      setNotice('batchLimit')
      return
    }
    if (!next.length) return
    setFiles(next)
    setIndex(0)
    setNotice('')
  }

  function drop(event: DragEvent): void {
    event.preventDefault()
    select(Array.from(event.dataTransfer.files))
  }

  return (
    <main className="utility-workbench file-preview-workbench" onDragOver={(event) => event.preventDefault()} onDrop={drop}>
      <header className="utility-header">
        <h1 className="visually-hidden">{t('title')}</h1>
      </header>
      <section className="utility-toolbar">
        <button className="secondary-button" type="button" onClick={() => input.current?.click()}><FolderOpen />{t('action.choose')}</button>
        <button className="secondary-button" type="button" disabled={!files.length} onClick={() => { setFiles([]); setIndex(0); setNotice('') }}><Trash2 />{t('action.clear')}</button>
        <span>{files.length ? `${index + 1} / ${files.length}` : ''}</span>
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
          <nav aria-label={t('title')}>
            {files.map((item, position) => (
              <button className={position === index ? 'utility-segment utility-segment--active' : 'utility-segment'} type="button" key={`${item.name}-${item.lastModified}-${position}`} onClick={() => setIndex(position)}>
                {item.name}
                <small>{t(`kind.${classifyPreview(item.name, item.type)}`)} · {formatPreviewSize(item.size)}</small>
              </button>
            ))}
          </nav>
          <PreviewPane file={file} kind={kind} />
        </section>
      ) : (
        <button className="file-preview-drop" type="button" onClick={() => input.current?.click()}>{t('empty')}</button>
      )}
    </main>
  )
}

function PreviewPane({ file, kind }: { file: File; kind: PreviewKind }) {
  const { t } = useLocalizedMessages(filePreviewMessages)
  const [encoding, setEncoding] = useState<TextEncoding>('auto')
  const [formatted, setFormatted] = useState(true)
  const [text, setText] = useState('')
  const [error, setError] = useState('')
  const [imageUrl, setImageUrl] = useState('')
  const [jsonValid, setJsonValid] = useState(true)
  const [encodingName, setEncodingName] = useState('utf-8')
  const [findQuery, setFindQuery] = useState('')
  const [zoom, setZoom] = useState<number | null>(null)
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
  if (kind === 'pdf') return <p role="status">{t('pdf')}</p>
  const matches = scanPreviewMatches(text, findQuery, defaultFindReplaceOptions)

  function jump(forward: boolean): void {
    const view = editor.current
    const from = view ? (forward ? view.state.selection.main.to : view.state.selection.main.from) : 0
    const found = scanPreviewMatches(text, findQuery, defaultFindReplaceOptions, from, forward)
    if (!found.match || !view) return
    view.dispatch({
      selection: { anchor: found.match.start, head: found.match.end },
      scrollIntoView: true
    })
  }

  if (kind === 'image') {
    return (
      <section className="file-preview-image">
        <div className="utility-toolbar">
          <button type="button" aria-pressed={zoom === null} onClick={() => setZoom(null)}>{t('image.fit')}</button>
          <button type="button" aria-pressed={zoom === 1} onClick={() => setZoom(1)}>{t('image.actual')}</button>
          <button type="button" aria-label={t('image.zoomOut')} onClick={() => setZoom(Math.max(0.1, (zoom ?? 1) / 1.25))}><Minus /></button>
          <button type="button" aria-label={t('image.zoomIn')} onClick={() => setZoom(Math.min(4, (zoom ?? 1) * 1.25))}><Plus /></button>
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
    <section className="file-preview-text">
      <div className="utility-toolbar">
        <label>{t('encoding')}
          <select aria-label={t('encoding')} value={encoding} onChange={(event) => setEncoding(event.target.value as TextEncoding)}>
            {encodings.map((value) => <option key={value} value={value}>{value === 'auto' ? t('encoding.auto') : value.toUpperCase()}</option>)}
          </select>
        </label>
        {kind === 'json' && <label><input type="checkbox" checked={formatted} onChange={(event) => setFormatted(event.target.checked)} />{t('formatJson')}</label>}
        <label>{t('action.find')}<Search /><input aria-label={t('action.find')} value={findQuery} onChange={(event) => setFindQuery(event.target.value.slice(0, 256))} /></label>
        <button type="button" onClick={() => jump(false)}>{t('find.previous')}</button>
        <button type="button" onClick={() => jump(true)}>{t('find.next')}</button>
        <span>{t('find.count', { count: matches.count })}</span>
        <span>{encodingName.toUpperCase()} · {kind === 'json' && !jsonValid ? t('invalidJson') : t('status.readOnly')}</span>
      </div>
      {error ? <p role="alert">{error}</p> : <CodeEditor ariaLabel={file.name} value={text} readOnly lineWrapping onReady={(view) => { editor.current = view }} />}
    </section>
  )
}
