import { ChevronLeft, ChevronRight, Files, FolderOpen, Trash2 } from 'lucide-react'
import { useRef, useState, type DragEvent } from 'react'
import { ToolPageHeader, WorkspaceDragZone } from '@/shared/components/ToolPage'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { classifyPreview, formatPreviewSize, previewBatchLimit, previewLimits, type PreviewViewState } from './filePreview'
import { PreviewContent } from './PreviewContent'
import './filePreview.css'

export function FilePreviewTool() {
  const { t } = useI18n()
  const [files, setFiles] = useState<File[]>([])
  const [index, setIndex] = useState(0)
  const [dragging, setDragging] = useState(false)
  const [notice, setNotice] = useState('')
  const [batch, setBatch] = useState(0)
  const views = useRef({ batch: 0, states: new Map<number, PreviewViewState>() })
  const input = useRef<HTMLInputElement>(null)
  const root = useRef<HTMLElement>(null)
  const file = files[index]
  const kind = file ? classifyPreview(file.name, file.type) : null

  function select(next: File[]): void {
    if (next.length > previewBatchLimit) { setNotice(t('filePreview.batchLimit')); return }
    if (!next.length) return
    views.current = { batch: views.current.batch + 1, states: new Map() }
    setFiles(next); setIndex(0); setNotice(''); setBatch(views.current.batch)
    root.current?.focus()
  }
  function drop(event: DragEvent): void {
    event.preventDefault(); setDragging(false)
    select(Array.from(event.dataTransfer.files))
  }
  return (
    <section ref={root} className={`tool-page file-preview-tool ${dragging ? 'file-preview-tool--dragging' : ''}`} tabIndex={-1}
      onDragOver={event => { event.preventDefault(); if (event.dataTransfer.types.includes('Files')) { event.dataTransfer.dropEffect = 'copy'; setDragging(true) } }}
      onDragLeave={event => { if (!event.currentTarget.contains(event.relatedTarget as Node | null)) setDragging(false) }} onDrop={drop}
      onKeyDown={event => {
        if (event.defaultPrevented || event.altKey || event.ctrlKey || event.metaKey || event.shiftKey || event.target instanceof Element && event.target.closest('input, select, textarea, .cm-editor, [contenteditable="true"]')) return
        if (event.key === 'ArrowLeft' && index > 0) { event.preventDefault(); setIndex(index - 1) }
        if (event.key === 'ArrowRight' && index < files.length - 1) { event.preventDefault(); setIndex(index + 1) }
      }}>
      <ToolPageHeader title={t('app.nav.filePreview')} />
      <input ref={input} className="visually-hidden" data-testid="preview-file-input" type="file" multiple tabIndex={-1} aria-label={t('filePreview.choose')}
        onChange={event => { select(Array.from(event.target.files ?? [])); event.target.value = '' }} />
      <div className="local-tool-shell file-preview-shell">
        <div className="file-preview-toolbar">
          <button className="toolbar-button" type="button" onClick={() => input.current?.click()}><FolderOpen size={15} />{t('filePreview.choose')}</button>
          <WorkspaceDragZone />
          <span className="file-preview-count">{files.length ? `${index + 1} / ${files.length}` : ''}</span>
          <button className="toolbar-button toolbar-button--icon" type="button" aria-label={t('filePreview.previous')} disabled={index === 0 || !file} onClick={() => setIndex(index - 1)}><ChevronLeft size={16} /></button>
          <button className="toolbar-button toolbar-button--icon" type="button" aria-label={t('filePreview.next')} disabled={index >= files.length - 1} onClick={() => setIndex(index + 1)}><ChevronRight size={16} /></button>
          <button className="toolbar-button toolbar-button--icon" type="button" aria-label={t('common.action.clear')} disabled={!files.length} onClick={() => { views.current = { batch: views.current.batch + 1, states: new Map() }; setBatch(views.current.batch); setFiles([]); setIndex(0); setNotice('') }}><Trash2 size={15} /></button>
        </div>
        {notice && <p className="file-preview-notice" role="alert">{notice}</p>}
        {file && kind ? <div className="file-preview-workspace">
          <nav className="file-preview-list" aria-label={t('filePreview.batch')}>
            {files.map((item, position) => <button key={position} type="button" className={index === position ? 'file-preview-item file-preview-item--active' : 'file-preview-item'} aria-current={position === index ? 'true' : undefined} onClick={() => setIndex(position)} title={item.name}>
              <span>{item.name}</span><small>{t(`filePreview.kind.${classifyPreview(item.name, item.type)}`)} · {formatPreviewSize(item.size)}</small>
            </button>)}
          </nav>
          <div className="file-preview-detail">
            <header className="file-preview-metadata"><strong title={file.name}>{file.name}</strong><span>{formatPreviewSize(file.size)} · {file.lastModified ? new Date(file.lastModified).toLocaleString() : '—'}</span></header>
            {kind === 'unsupported' ? <p className="file-preview-empty" role="status">{t('filePreview.unsupported')}</p>
              : file.size > previewLimits[kind] ? <p className="file-preview-empty" role="status">{t('filePreview.tooLarge', { limit: formatPreviewSize(previewLimits[kind]) })}</p>
                : <PreviewContent key={`${batch}:${index}`} file={file} kind={kind} initialState={views.current.states.get(index)} onStateChange={patch => {
                  if (views.current.batch === batch) views.current.states.set(index, { ...views.current.states.get(index), ...patch })
                }} />}
          </div>
        </div> : <button className="file-preview-empty file-preview-drop" type="button" onClick={() => input.current?.click()}><Files size={38} /><strong>{t('filePreview.empty')}</strong><span>{t('filePreview.hint')}</span></button>}
        <footer className="file-preview-footer">{t('filePreview.footer')}</footer>
      </div>
      {dragging && <div className="file-preview-drop-overlay" aria-hidden="true"><Files size={30} />{t('filePreview.drop')}</div>}
    </section>
  )
}
