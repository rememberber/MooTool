import { ClipboardCopy, ClipboardPaste, Download, FileImage, FolderOpen, ImageDown, ImagePlus, List, Maximize2, Minimize2, Minus, Pencil, Plus, Save, ScanLine, Shapes, Trash2, Type, Upload } from 'lucide-react'
import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { ToolPageHeader, WorkspaceDragZone } from '@/shared/components/ToolPage'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import type { ImageAsset, ImageAssetSummary, ImageVectorizeOptions } from '@/shared/contracts/images'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useDesktopDialog } from '@/shared/feedback/DesktopDialogProvider'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { ImageBase64Dialog, ImageCompressDialog, ImageSvgDialog, ImageWatermarkDialog } from './ImageDialogs'
import { compressImage, ensureImageDataUrl, processedImageName, watermarkImage, type CompressImageOptions, type ImageOutputMode, type WatermarkImageOptions } from './imageTools'

export function ImageTool() {
  const { t } = useI18n()
  const actions = useToolActions('image')
  const desktopDialog = useDesktopDialog()
  const [assets, setAssets] = useState<ImageAssetSummary[]>([])
  const [selectedNames, setSelectedNames] = useState<string[]>([])
  const [current, setCurrent] = useState<ImageAsset | null>(null)
  const [listVisible, setListVisible] = useState(true)
  const [zoom, setZoom] = useState(1)
  const [fit, setFit] = useState(true)
  const [busy, setBusy] = useState(false)
  const [base64Mode, setBase64Mode] = useState<'import' | 'export' | null>(null)
  const [compressOpen, setCompressOpen] = useState(false)
  const [watermarkOpen, setWatermarkOpen] = useState(false)
  const [svgOpen, setSvgOpen] = useState(false)
  const [canvasSize, setCanvasSize] = useState({ width: 0, height: 0 })
  const canvasRef = useRef<HTMLDivElement>(null)
  const zoomAnchorRef = useRef<ZoomAnchor | null>(null)
  const effectiveZoomRef = useRef(1)

  const loadAssets = useCallback(async (preferredName?: string) => {
    const next = await window.mootool.listImageAssets()
    setAssets(next)
    const name = preferredName ?? next[0]?.name
    if (!name) { setCurrent(null); setSelectedNames([]); return }
    const found = next.find((asset) => asset.name === name) ?? next[0]
    if (found) {
      setCurrent(await window.mootool.readImageAsset(found.name))
      setSelectedNames((selection) => selection.length ? selection.filter((item) => next.some((asset) => asset.name === item)) : [found.name])
      setFit(true)
      setZoom(1)
    }
  }, [])

  useEffect(() => { void loadAssets() }, [loadAssets])
  const processingNames = useMemo(() => selectedNames.length > 0 ? selectedNames : current ? [current.name] : [], [current, selectedNames])
  const fitZoom = useMemo(() => current ? calculateFitZoom(current.width, current.height, canvasSize.width, canvasSize.height) : 1, [canvasSize, current])
  const effectiveZoom = fit ? fitZoom : zoom
  const maximumZoom = Math.max(5, fitZoom * 4)
  effectiveZoomRef.current = effectiveZoom

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const measure = () => {
      const style = window.getComputedStyle(canvas)
      const width = Math.max(0, canvas.clientWidth - parseFloat(style.paddingLeft) - parseFloat(style.paddingRight))
      const height = Math.max(0, canvas.clientHeight - parseFloat(style.paddingTop) - parseFloat(style.paddingBottom))
      setCanvasSize((value) => value.width === width && value.height === height ? value : { width, height })
    }
    measure()
    const observer = new ResizeObserver(measure)
    observer.observe(canvas)
    return () => observer.disconnect()
  }, [])

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const handleWheel = (event: WheelEvent) => {
      if (!current || !event.ctrlKey) return
      event.preventDefault()
      rememberZoomAnchor(canvas, event.clientX, event.clientY, zoomAnchorRef)
      const nextZoom = clampZoom(effectiveZoomRef.current * Math.exp(-event.deltaY * 0.01), maximumZoom)
      if (Math.abs(nextZoom - effectiveZoomRef.current) < 0.001) {
        zoomAnchorRef.current = null
        return
      }
      effectiveZoomRef.current = nextZoom
      setFit(false)
      setZoom(nextZoom)
    }
    canvas.addEventListener('wheel', handleWheel, { passive: false })
    return () => canvas.removeEventListener('wheel', handleWheel)
  }, [current, maximumZoom])

  useLayoutEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    if (fit) {
      zoomAnchorRef.current = null
      canvas.scrollLeft = 0
      canvas.scrollTop = 0
      return
    }
    const anchor = zoomAnchorRef.current
    const image = canvas.querySelector('img')
    if (!anchor || !image) return
    const imageRect = image.getBoundingClientRect()
    canvas.scrollLeft += imageRect.left + imageRect.width * anchor.xRatio - anchor.clientX
    canvas.scrollTop += imageRect.top + imageRect.height * anchor.yRatio - anchor.clientY
    zoomAnchorRef.current = null
  }, [current, fit, zoom])

  function changeZoom(factor: number): void {
    const canvas = canvasRef.current
    if (!current || !canvas) return
    const canvasRect = canvas.getBoundingClientRect()
    rememberZoomAnchor(canvas, canvasRect.left + canvasRect.width / 2, canvasRect.top + canvasRect.height / 2, zoomAnchorRef)
    const nextZoom = clampZoom(effectiveZoomRef.current * factor, maximumZoom)
    effectiveZoomRef.current = nextZoom
    setFit(false)
    setZoom(nextZoom)
  }

  function showOriginalSize(): void {
    const canvas = canvasRef.current
    if (!current || !canvas) return
    const canvasRect = canvas.getBoundingClientRect()
    rememberZoomAnchor(canvas, canvasRect.left + canvasRect.width / 2, canvasRect.top + canvasRect.height / 2, zoomAnchorRef)
    effectiveZoomRef.current = 1
    setFit(false)
    setZoom(1)
  }

  async function selectAsset(name: string): Promise<void> {
    try {
      setCurrent(await window.mootool.readImageAsset(name))
      setFit(true)
      setZoom(1)
      if (!selectedNames.includes(name)) setSelectedNames([name])
    } catch (error) { actions.reportError(error) }
  }

  function toggleAsset(name: string, selected: boolean): void {
    setSelectedNames((currentSelection) => selected ? [...new Set([...currentSelection, name])] : currentSelection.filter((item) => item !== name))
  }

  async function importImages(): Promise<void> {
    try {
      const imported = await window.mootool.importImageAssets()
      if (imported.length) await loadAssets(imported[0].name)
    } catch (error) { actions.reportError(error) }
  }

  async function importClipboard(): Promise<void> {
    try {
      const dataUrl = await window.mootool.readClipboardImage()
      if (!dataUrl) throw new Error(t('image.clipboardEmpty'))
      const saved = await window.mootool.saveImageAsset({ name: `Untitled-${timestamp()}.png`, dataUrl })
      await loadAssets(saved.name)
      actions.toast.success(t('image.imported'))
    } catch (error) { actions.reportError(error) }
  }

  async function capture(): Promise<void> {
    if (busy) return
    setBusy(true)
    try {
      const result = await window.mootool.captureScreenRegion()
      if (result) await saveCapture(result.dataUrl)
    } catch (error) { actions.reportError(error) } finally { setBusy(false) }
  }

  async function saveCapture(dataUrl: string): Promise<void> {
    try {
      const saved = await window.mootool.saveImageAsset({ name: `Screenshot-${timestamp()}.png`, dataUrl })
      await loadAssets(saved.name)
    } catch (error) { actions.reportError(error) }
  }

  async function importBase64(value: string): Promise<void> {
    try {
      const saved = await window.mootool.saveImageAsset({ name: `Base64-${timestamp()}.png`, dataUrl: ensureImageDataUrl(value) })
      setBase64Mode(null)
      await loadAssets(saved.name)
    } catch (error) { actions.reportError(error) }
  }

  async function copyImage(): Promise<void> {
    if (!current) return
    try { await window.mootool.writeClipboardImage(current.dataUrl); actions.toast.success(t('json.notice.copied')) } catch (error) { actions.reportError(error) }
  }

  async function saveCurrent(): Promise<void> {
    if (!current) return
    const name = await desktopDialog.prompt(t('image.saveName'), { defaultValue: current.name, confirmLabel: t('common.save') })
    if (!name) return
    try { const saved = await window.mootool.saveImageAsset({ name, dataUrl: current.dataUrl }); await loadAssets(saved.name); actions.toast.success(t('common.saved')) } catch (error) { actions.reportError(error) }
  }

  async function renameCurrent(): Promise<void> {
    if (!current) return
    const nextName = await desktopDialog.prompt(t('image.renamePrompt'), { defaultValue: current.name, confirmLabel: t('common.rename') })
    if (!nextName || nextName === current.name) return
    try { const renamed = await window.mootool.renameImageAsset({ name: current.name, nextName }); await loadAssets(renamed.name) } catch (error) { actions.reportError(error) }
  }

  async function deleteSelected(): Promise<void> {
    if (!processingNames.length || !await desktopDialog.confirm(t('image.confirmDelete', { count: String(processingNames.length) }), { confirmLabel: t('common.action.delete'), danger: true })) return
    try { await window.mootool.deleteImageAssets(processingNames); setCurrent(null); setSelectedNames([]); await loadAssets(); actions.toast.success(t('favorite.deleted')) } catch (error) { actions.reportError(error) }
  }

  async function exportSelected(): Promise<void> {
    if (!processingNames.length) return
    try { const directory = await window.mootool.exportImageAssets(processingNames); if (directory) actions.toast.success(t('image.exported', { directory })) } catch (error) { actions.reportError(error) }
  }

  async function processCompression(options: CompressImageOptions, mode: ImageOutputMode): Promise<void> {
    setCompressOpen(false)
    await processImages(async (asset) => compressImage(asset.dataUrl, options), 'compressed', mode, options.format)
  }

  async function processWatermark(options: WatermarkImageOptions, mode: ImageOutputMode): Promise<void> {
    setWatermarkOpen(false)
    await processImages(async (asset) => watermarkImage(asset.dataUrl, options), 'watermarked', mode, 'auto')
  }

  async function processSvg(options: ImageVectorizeOptions): Promise<void> {
    setSvgOpen(false)
    if (!processingNames.length) return
    setBusy(true)
    try {
      const result = await window.mootool.vectorizeImageAssets(processingNames, options)
      if (result) actions.toast.success(t('image.svgComplete', { count: String(result.files.length), path: result.outputPath }))
    } catch (error) { actions.reportError(error) } finally { setBusy(false) }
  }

  async function processImages(transform: (asset: ImageAsset) => Promise<string>, suffix: 'compressed' | 'watermarked', mode: ImageOutputMode, format: CompressImageOptions['format']): Promise<void> {
    if (!processingNames.length) return
    setBusy(true)
    let preferred = ''
    try {
      for (const name of processingNames) {
        const asset = await window.mootool.readImageAsset(name)
        const dataUrl = await transform(asset)
        const outputName = mode === 'overwrite' ? overwriteName(asset.name, format) : processedImageName(asset.name, suffix, format)
        const saved = await window.mootool.saveImageAsset({ name: outputName, dataUrl })
        if (mode === 'overwrite' && saved.name !== asset.name) await window.mootool.deleteImageAssets([asset.name])
        preferred ||= saved.name
      }
      await loadAssets(preferred)
      actions.toast.success(t('image.processComplete', { count: String(processingNames.length) }))
    } catch (error) { actions.reportError(error) } finally { setBusy(false) }
  }

  return (
    <section className="tool-page p4-tool image-tool-page">
      <ToolPageHeader title={t('image.title')} />
      <div className="local-tool-shell image-workspace">
        <div className="image-main-toolbar"><button className="toolbar-button toolbar-button--icon" type="button" aria-label={t('image.toggleList')} onClick={() => setListVisible((value) => !value)}><List size={14} /></button><button className="toolbar-button" type="button" disabled={busy} onClick={() => { void capture() }}><ScanLine size={14} />{t('image.screenshot')}</button><button className="toolbar-button" type="button" onClick={() => { void importClipboard() }}><ClipboardPaste size={14} />{t('image.fromClipboard')}</button><button className="toolbar-button" type="button" onClick={() => { void importImages() }}><FolderOpen size={14} />{t('image.import')}</button><button className="toolbar-button" type="button" onClick={() => setBase64Mode('import')}><ImageDown size={14} />{t('image.fromBase64')}</button><WorkspaceDragZone className="p4-toolbar__spacer" /><button className="toolbar-button" type="button" disabled={!processingNames.length || busy} onClick={() => setSvgOpen(true)}><Shapes size={14} />{t('image.toSvg')}</button><button className="toolbar-button" type="button" disabled={!processingNames.length || busy} onClick={() => setCompressOpen(true)}><Minimize2 size={14} />{t('image.compress')}</button><button className="toolbar-button" type="button" disabled={!processingNames.length || busy} onClick={() => setWatermarkOpen(true)}><Type size={14} />{t('image.watermark')}</button><button className="toolbar-button" type="button" disabled={!current} onClick={() => { void saveCurrent() }}><Save size={14} />{t('common.save')}</button><button className="toolbar-button" type="button" disabled={!current} onClick={() => { void copyImage() }}><ClipboardCopy size={14} />{t('image.copy')}</button><button className="toolbar-button" type="button" disabled={!current} onClick={() => setBase64Mode('export')}><Upload size={14} />{t('image.toBase64')}</button></div>
        <ResizableColumns className={listVisible ? 'image-layout' : 'image-layout image-layout--collapsed'} columns={listVisible ? 2 : 1} defaultSizes={listVisible ? [230, 770] : [1]} minPaneWidths={listVisible ? [180, 360] : [360]} storageKey="image-library">
          {listVisible && <aside className="image-library"><header><span>{t('image.library')}</span><button className="icon-button" type="button" aria-label={t('image.import')} onClick={() => { void importImages() }}><ImagePlus size={14} /></button></header><div className="image-list">{assets.length === 0 ? <div className="history-empty">{t('image.empty')}</div> : assets.map((asset) => <div className={current?.name === asset.name ? 'image-list-item image-list-item--active' : 'image-list-item'} key={asset.name}><input type="checkbox" aria-label={`${t('image.select')} ${asset.name}`} checked={selectedNames.includes(asset.name)} onChange={(event) => toggleAsset(asset.name, event.target.checked)} /><button type="button" onClick={() => { void selectAsset(asset.name) }}><FileImage size={15} /><span><strong>{asset.name}</strong><small>{asset.width} × {asset.height} · {formatBytes(asset.size)}</small></span></button></div>)}</div><footer><button className="icon-button" type="button" disabled={!current} aria-label={t('common.rename')} onClick={() => { void renameCurrent() }}><Pencil size={14} /></button><button className="icon-button" type="button" disabled={!processingNames.length} aria-label={t('common.export')} onClick={() => { void exportSelected() }}><Download size={14} /></button><button className="icon-button icon-button--danger" type="button" disabled={!processingNames.length} aria-label={t('common.action.delete')} onClick={() => { void deleteSelected() }}><Trash2 size={14} /></button></footer></aside>}
          <main className="image-canvas-panel" onDoubleClick={() => { if (current) void window.mootool.openImageAsset(current.name) }}><div ref={canvasRef} className={fit ? 'image-canvas image-canvas--fit' : 'image-canvas'}>{current ? <img src={current.dataUrl} alt={current.name} style={{ width: `${current.width * effectiveZoom}px`, height: `${current.height * effectiveZoom}px` }} /> : <div className="image-placeholder"><FileImage size={48} /><span>{t('image.emptyPreview')}</span></div>}</div><div className="image-zoom-toolbar" onDoubleClick={(event) => event.stopPropagation()}><button className="icon-button" type="button" aria-label={t('image.zoomIn')} disabled={!current} onClick={() => changeZoom(1.1)}><Plus size={14} /></button><button className="icon-button" type="button" aria-label={t('image.zoomOut')} disabled={!current} onClick={() => changeZoom(1 / 1.1)}><Minus size={14} /></button><button className="icon-button image-actual-size-button" type="button" aria-label={t('image.original')} aria-pressed={!fit && Math.abs(zoom - 1) < 0.001} title={t('image.original')} disabled={!current} onClick={showOriginalSize}>1:1</button><button className="icon-button" type="button" aria-label={t('image.fit')} aria-pressed={fit} title={t('image.fit')} disabled={!current} onClick={() => setFit(true)}><Maximize2 size={14} /></button><span>{current ? `${current.width} × ${current.height} · ${formatBytes(current.size)} · ${fit ? `${t('image.fit')} · ` : ''}${Math.round(effectiveZoom * 100)}%` : ''}</span></div></main>
        </ResizableColumns>
      </div>
      <ImageBase64Dialog open={base64Mode !== null} mode={base64Mode ?? 'import'} value={base64Mode === 'export' ? current?.dataUrl ?? '' : ''} onClose={() => setBase64Mode(null)} onImport={(value) => { void importBase64(value) }} />
      <ImageCompressDialog open={compressOpen} count={processingNames.length} onClose={() => setCompressOpen(false)} onConfirm={(options, mode) => { void processCompression(options, mode) }} />
      <ImageWatermarkDialog open={watermarkOpen} count={processingNames.length} onClose={() => setWatermarkOpen(false)} onConfirm={(options, mode) => { void processWatermark(options, mode) }} />
      <ImageSvgDialog open={svgOpen} count={processingNames.length} onClose={() => setSvgOpen(false)} onConfirm={(options) => { void processSvg(options) }} />
    </section>
  )
}

function overwriteName(name: string, format: CompressImageOptions['format']): string {
  if (format === 'auto') return name
  const base = name.replace(/\.[^.]+$/, '')
  return `${base}.${format === 'jpeg' ? 'jpg' : 'png'}`
}

function timestamp(): string {
  return new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19)
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

type ZoomAnchor = {
  clientX: number
  clientY: number
  xRatio: number
  yRatio: number
}

function calculateFitZoom(imageWidth: number, imageHeight: number, viewportWidth: number, viewportHeight: number): number {
  if (imageWidth <= 0 || imageHeight <= 0 || viewportWidth <= 0 || viewportHeight <= 0) return 1
  return Math.min(viewportWidth / imageWidth, viewportHeight / imageHeight)
}

function clampZoom(value: number, maximum: number): number {
  return Math.min(maximum, Math.max(0.1, value))
}

function rememberZoomAnchor(canvas: HTMLDivElement, clientX: number, clientY: number, anchorRef: { current: ZoomAnchor | null }): void {
  const image = canvas.querySelector('img')
  const imageRect = image?.getBoundingClientRect()
  if (!imageRect || imageRect.width <= 0 || imageRect.height <= 0) {
    anchorRef.current = null
    return
  }
  const pointerInsideImage = clientX >= imageRect.left && clientX <= imageRect.right && clientY >= imageRect.top && clientY <= imageRect.bottom
  anchorRef.current = {
    clientX: pointerInsideImage ? clientX : imageRect.left + imageRect.width / 2,
    clientY: pointerInsideImage ? clientY : imageRect.top + imageRect.height / 2,
    xRatio: pointerInsideImage ? (clientX - imageRect.left) / imageRect.width : 0.5,
    yRatio: pointerInsideImage ? (clientY - imageRect.top) / imageRect.height : 0.5
  }
}
