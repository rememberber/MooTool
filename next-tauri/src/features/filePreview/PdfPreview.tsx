import { ChevronLeft, ChevronRight, Minus, Plus } from 'lucide-react'
import { getDocument, PDFWorker, type PDFDocumentProxy, type RenderTask } from 'pdfjs-dist'
import PdfWorker from 'pdfjs-dist/build/pdf.worker.mjs?worker'
import { useEffect, useRef, useState } from 'react'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { filePreviewMessages } from './filePreviewMessages'
import { PreviewError, formatPreviewSize, previewLimits, readPreviewBytes } from './filePreview'

export function PdfPreview({ file, initialPage = 1, initialZoom = 1, onViewChange }: {
  file: File
  initialPage?: number
  initialZoom?: number
  onViewChange?: (patch: { pdfPage?: number; pdfZoom?: number }) => void
}) {
  const { t } = useLocalizedMessages(filePreviewMessages)
  const [document, setDocument] = useState<PDFDocumentProxy | null>(null)
  const [error, setError] = useState<'pdf' | 'password' | 'tooLarge' | null>(null)
  const [page, setPage] = useState(initialPage)
  const [zoom, setZoom] = useState(initialZoom)
  const report = useRef(onViewChange)
  report.current = onViewChange

  function changePage(next: number): void {
    setPage(next)
    report.current?.({ pdfPage: next })
  }

  function changeZoom(next: number): void {
    setZoom(next)
    report.current?.({ pdfZoom: next })
  }

  useEffect(() => {
    let active = true
    const port = new PdfWorker()
    const worker = PDFWorker.create({ port })
    let task: ReturnType<typeof getDocument> | undefined
    let disposed = false
    function dispose(): void {
      if (disposed) return
      disposed = true
      void (task?.destroy() ?? Promise.resolve()).catch(() => {}).finally(() => { worker.destroy(); port.terminate() })
    }
    void worker.promise.catch(() => {})
    setDocument(null)
    setError(null)
    const assets = new URL('./pdfjs/', window.location.href).href
    void readPreviewBytes(file, 'pdf').then((bytes) => {
      if (!active) return
      task = getDocument({
        data: bytes,
        worker,
        cMapUrl: `${assets}cmaps/`,
        cMapPacked: true,
        standardFontDataUrl: `${assets}standard_fonts/`,
        wasmUrl: `${assets}wasm/`,
        useWorkerFetch: false,
        enableXfa: false,
        maxImageSize: 32_000_000,
        canvasMaxAreaInBytes: 32_000_000
      })
      return task.promise
    }).then((pdf) => {
      if (active && pdf) {
        const nextPage = Math.max(1, Math.min(initialPage, pdf.numPages))
        setPage(nextPage)
        report.current?.({ pdfPage: nextPage, pdfZoom: initialZoom })
        setDocument(pdf)
      }
    }).catch((cause: unknown) => {
      if (!active) return
      if (cause instanceof PreviewError && cause.code === 'tooLarge') setError('tooLarge')
      else setError(cause instanceof Error && cause.name === 'PasswordException' ? 'password' : 'pdf')
      dispose()
    })
    return () => {
      active = false
      dispose()
    }
  }, [file])

  return (
    <div className="file-preview-content">
      <div className="file-preview-options">
        <button type="button" disabled={!document || page <= 1} aria-label={t('previousPage')} onClick={() => changePage(page - 1)}><ChevronLeft size={15} /></button>
        <label>{t('page')}<input type="number" aria-label={t('page')} min={1} max={document?.numPages ?? 1} value={page} disabled={!document} onChange={(event) => { const value = Number(event.target.value); if (Number.isInteger(value) && value >= 1 && value <= (document?.numPages ?? 1)) changePage(value) }} /></label>
        <span>/ {document?.numPages ?? '—'}</span>
        <button type="button" disabled={!document || page >= document.numPages} aria-label={t('nextPage')} onClick={() => changePage(page + 1)}><ChevronRight size={15} /></button>
        <button type="button" onClick={() => changeZoom(1)}>{t('pdf.fit')}</button>
        <button type="button" disabled={zoom <= 0.25} aria-label={t('image.zoomOut')} onClick={() => changeZoom(Math.max(0.25, zoom / 1.25))}><Minus size={14} /></button>
        <button type="button" disabled={zoom >= 3} aria-label={t('image.zoomIn')} onClick={() => changeZoom(Math.min(3, zoom * 1.25))}><Plus size={14} /></button>
        <span>{Math.round(zoom * 100)}%</span>
      </div>
      {error === 'tooLarge' ? <p className="file-preview-empty" role="status">{t('tooLarge', { limit: formatPreviewSize(previewLimits.pdf) })}</p>
        : error ? <p className="file-preview-empty" role="status">{t(`error.${error}`)}</p>
          : document ? <PdfPage document={document} page={page} zoom={zoom} /> : <p className="file-preview-empty" role="status">{t('loading')}</p>}
    </div>
  )
}

function PdfPage({ document, page, zoom }: { document: PDFDocumentProxy; page: number; zoom: number }) {
  const { t } = useLocalizedMessages(filePreviewMessages)
  const container = useRef<HTMLDivElement>(null)
  const [width, setWidth] = useState(0)
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading')
  useEffect(() => {
    const element = container.current
    if (!element) return
    const observer = new ResizeObserver((entries) => setWidth(Math.floor(entries[0].contentRect.width)))
    observer.observe(element)
    return () => observer.disconnect()
  }, [])
  useEffect(() => {
    if (width <= 0) return
    let active = true
    let render: RenderTask | undefined
    const host = container.current
    if (!host) return
    const canvas = window.document.createElement('canvas')
    canvas.setAttribute('aria-label', t('pdfPage', { page: String(page) }))
    canvas.dataset.page = String(page)
    host.replaceChildren(canvas)
    setStatus('loading')
    void document.getPage(page).then(async (pdfPage) => {
      if (!active) return
      const base = pdfPage.getViewport({ scale: 1 })
      const scale = Math.max(0.01, (Math.max(100, width - 48) / base.width) * zoom)
      const viewport = pdfPage.getViewport({ scale })
      const ratio = Math.min(window.devicePixelRatio || 1, 2, Math.sqrt(8_000_000 / (viewport.width * viewport.height)), 8192 / viewport.width, 8192 / viewport.height)
      canvas.width = Math.max(1, Math.floor(viewport.width * ratio))
      canvas.height = Math.max(1, Math.floor(viewport.height * ratio))
      canvas.style.width = `${viewport.width}px`
      canvas.style.height = `${viewport.height}px`
      render = pdfPage.render({ canvas, viewport, transform: [ratio, 0, 0, ratio, 0, 0] })
      await render.promise
      if (active) {
        canvas.dataset.rendered = 'true'
        setStatus('ready')
        pdfPage.cleanup()
      }
    }).catch(() => { if (active) setStatus('error') })
    return () => { active = false; render?.cancel(); canvas.remove(); canvas.width = 0; canvas.height = 0 }
  }, [document, page, t, width, zoom])
  return (
    <div className="file-preview-pdf-area">
      {status !== 'ready' && <p className="file-preview-pdf-status" role="status">{t(status === 'error' ? 'error.pdf' : 'loading')}</p>}
      <div className="file-preview-pdf" ref={container} />
    </div>
  )
}
