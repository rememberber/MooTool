import {
  CheckCircle2,
  ClipboardCopy,
  ClipboardPaste,
  Crop,
  Download,
  FileImage,
  FolderOpen,
  ImageDown,
  ImageUp,
  Maximize2,
  Minus,
  Monitor,
  Pencil,
  Plus,
  ScanLine,
  Shapes,
  Trash2,
  TriangleAlert,
  Type,
  Upload,
  X,
  ZoomIn
} from 'lucide-react'
import { Image as TauriImage } from '@tauri-apps/api/image'
import { getCurrentWebview } from '@tauri-apps/api/webview'
import { readImage as readClipboardImage, writeImage as writeClipboardImage } from '@tauri-apps/plugin-clipboard-manager'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  useLocalizedMessages,
  type LocalizedMessageKey,
  type MessageValues
} from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { imageApi } from '../../platform/api/imageApi'
import { userFilesApi } from '../../platform/api/userFilesApi'
import { nativeDesktopApi } from '../../platform/api/nativeDesktopApi'
import type { ImageAsset, ImageAssetSummary, ImageVectorizeOptions } from '../../platform/contracts/image'
import type { ReactNode } from 'react'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import { useDesktopDialog } from '../../shared/DesktopDialogProvider'
import {
  compressImage,
  ensureImageDataUrl,
  ImageToolError,
  imageDimensions,
  processedImageName,
  watermarkImage,
  type ImageFormat,
  type WatermarkPosition
} from './imageTools'
import { imageMessages } from './imageMessages'

type ImageMessageKey = LocalizedMessageKey<typeof imageMessages>
type ImageNotice = { key: ImageMessageKey; values?: MessageValues } | { raw: string }

class ImageLocalizedError extends Error {
  constructor(readonly key: ImageMessageKey, readonly values?: MessageValues) {
    super(key)
    this.name = 'ImageLocalizedError'
  }
}

export function ImageSurface() {
  const dialog = useDesktopDialog()
  const { t } = useLocalizedMessages(imageMessages)
  const fileInput = useRef<HTMLInputElement>(null)
  const [assets, setAssets] = useState<ImageAssetSummary[]>([])
  const [current, setCurrent] = useState<ImageAsset>()
  const [selected, setSelected] = useState<string[]>([])
  const [zoom, setZoom] = useState(1)
  const [fit, setFit] = useState(true)
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState<ImageNotice>({ key: 'notice.ready' })
  const [failed, setFailed] = useState(false)
  const [panel, setPanel] = useState<'compress' | 'watermark' | 'base64-import' | 'base64-export' | 'svg' | null>(null)
  const [base64Input, setBase64Input] = useState('')
  const [base64DataUrl, setBase64DataUrl] = useState(true)
  const [quality, setQuality] = useState(0.82)
  const [scale, setScale] = useState(1)
  const [format, setFormat] = useState<ImageFormat>('auto')
  const [watermarkText, setWatermarkText] = useState('MooTool')
  const [watermarkOpacity, setWatermarkOpacity] = useState(0.36)
  const [watermarkColor, setWatermarkColor] = useState('#FFFFFF')
  const [watermarkSize, setWatermarkSize] = useState(36)
  const [watermarkPosition, setWatermarkPosition] = useState<WatermarkPosition>('bottom-right')
  const [watermarkDiagonal, setWatermarkDiagonal] = useState(false)
  const [vectorizeOptions, setVectorizeOptions] = useState<ImageVectorizeOptions>({
    preset: 'poster',
    colorCount: 16,
    detail: 'medium',
    filterSpeckle: 8
  })
  const [nativeDragActive, setNativeDragActive] = useState(false)
  const [captureAssets, setCaptureAssets] = useState<ImageAsset[]>([])
  const noticeText = 'raw' in notice ? notice.raw : t(notice.key, notice.values)
  const base64ExportValue = current
    ? base64DataUrl ? current.dataUrl : current.dataUrl.slice(current.dataUrl.indexOf(',') + 1)
    : ''

  const load = useCallback(async (preferredName?: string) => {
    try {
      const next = await imageApi.list()
      setAssets(next)
      const name = preferredName ?? current?.name ?? next[0]?.name
      if (!name) {
        setCurrent(undefined)
        setSelected([])
        return
      }
      const available = next.find((item) => item.name === name) ?? next[0]
      if (available) {
        setCurrent(await imageApi.read(available.name))
        setSelected((values) => values.length ? values.filter((value) => next.some((item) => item.name === value)) : [available.name])
      }
    } catch (cause) { fail(cause) }
  }, [current?.name])

  useEffect(() => { void load() }, [])

  const processingNames = useMemo(() => selected.length ? selected : current ? [current.name] : [], [current, selected])
  const session = useMemo(() => ({
    digest: JSON.stringify({ names: assets.map((item) => item.name), current: current?.name, zoom, fit, panel }),
    summary: current
      ? `${current.name} · ${current.width}×${current.height} · ${formatBytes(current.sizeBytes)}`
      : t('session.library', { count: assets.length })
  }), [assets, current, fit, panel, t, zoom])
  const { sessionId, reportError } = useToolSessionReport('image', session.digest, session.summary)
  const recordOperation = useOperationHistory('image')

  useEffect(() => {
    if (!window.__TAURI_INTERNALS__) return
    let disposed = false
    let unlisten: (() => void) | undefined
    void getCurrentWebview().onDragDropEvent((event) => {
      if (event.payload.type === 'enter' || event.payload.type === 'over') {
        setNativeDragActive(true)
        return
      }
      setNativeDragActive(false)
      if (event.payload.type !== 'drop' || !event.payload.paths.length) return
      setBusy(true)
      void imageApi.importPaths(event.payload.paths)
        .then(async (imported) => {
          if (disposed) return
          await load(imported[0]?.name)
          if (disposed) return
          succeed('notice.imported', { count: imported.length })
          recordOperation(t('operation.dropImport'), t('operation.imageCount', { count: imported.length }), 'success')
        })
        .catch((cause: unknown) => {
          if (!disposed) fail(cause)
        })
        .finally(() => {
          if (!disposed) setBusy(false)
        })
    }).then((dispose) => {
      if (disposed) dispose()
      else unlisten = dispose
    }).catch((cause: unknown) => {
      if (!disposed) fail(cause)
    })
    return () => {
      disposed = true
      unlisten?.()
    }
  }, [load, recordOperation, t])

  async function selectAsset(name: string) {
    try {
      setCurrent(await imageApi.read(name))
      setZoom(1)
      setFit(true)
      if (!selected.includes(name)) setSelected([name])
    } catch (cause) { fail(cause) }
  }

  async function importFiles(files: File[]) {
    if (!files.length) return
    setBusy(true)
    let first = ''
    try {
      for (const file of files.slice(0, 50)) {
        if (!/^image\/(png|jpeg|webp|gif)$/i.test(file.type)) {
          throw new ImageLocalizedError('error.unsupportedFormat', { file: file.name })
        }
        if (file.size > 20 * 1024 * 1024) {
          throw new ImageLocalizedError('error.fileLimit', { file: file.name })
        }
        const dataUrl = await fileToDataUrl(file)
        const dimensions = await imageDimensions(dataUrl)
        const saved = await imageApi.save({ name: file.name, dataUrl, ...dimensions })
        first ||= saved.name
      }
      await load(first)
      const count = Math.min(files.length, 50)
      succeed('notice.imported', { count })
      recordOperation(t('operation.import'), t('operation.imageCount', { count }), 'success')
    } catch (cause) { fail(cause) } finally { setBusy(false) }
  }

  async function importClipboard() {
    try {
      if (window.__TAURI_INTERNALS__) {
        const clipboardImage = await readClipboardImage()
        try {
          const [{ width, height }, rgba] = await Promise.all([clipboardImage.size(), clipboardImage.rgba()])
          if (!width || !height || width > 50_000 || height > 50_000 || rgba.byteLength > 80 * 1024 * 1024) {
            throw new ImageLocalizedError('error.clipboardDimensions')
          }
          const dataUrl = rgbaToPngDataUrl(rgba, width, height)
          const saved = await imageApi.save({ name: `Clipboard-${timestamp()}.png`, dataUrl, width, height })
          await load(saved.name)
          succeed('notice.clipboardImported')
          recordOperation(t('operation.clipboardImport'), `${width}×${height}`, 'success')
          return
        } finally {
          await clipboardImage.close()
        }
      }
      if (!navigator.clipboard.read) throw new ImageLocalizedError('error.clipboardRead')
      const items = await navigator.clipboard.read()
      for (const item of items) {
        const mimeType = item.types.find((type) => type.startsWith('image/'))
        if (!mimeType) continue
        const blob = await item.getType(mimeType)
        await importFiles([new File([blob], `Clipboard-${timestamp()}.${mimeExtension(mimeType)}`, { type: mimeType })])
        return
      }
      throw new ImageLocalizedError('error.clipboardEmpty')
    } catch (cause) { fail(cause) }
  }

  async function captureScreen() {
    setBusy(true)
    if (window.__TAURI_INTERNALS__) {
      try {
        succeed('notice.capturing')
        const result = await nativeDesktopApi.captureDisplays()
        const captured = await Promise.all(result.assets.map((asset) => imageApi.read(asset.name)))
        setCaptureAssets(captured)
        succeed('notice.displaysCaptured', { count: result.monitorCount })
      } catch (cause) {
        fail(cause)
      } finally {
        setBusy(false)
      }
      return
    }
    let stream: MediaStream | undefined
    try {
      stream = await navigator.mediaDevices.getDisplayMedia({ video: true, audio: false })
      const video = document.createElement('video')
      video.srcObject = stream
      video.muted = true
      await video.play()
      await new Promise((resolve) => window.setTimeout(resolve, 180))
      const canvas = document.createElement('canvas')
      canvas.width = video.videoWidth
      canvas.height = video.videoHeight
      canvas.getContext('2d')?.drawImage(video, 0, 0)
      const dataUrl = canvas.toDataURL('image/png')
      const saved = await imageApi.save({ name: `Screenshot-${timestamp()}.png`, dataUrl, width: canvas.width, height: canvas.height })
      await load(saved.name)
      succeed('notice.screenshotSaved')
      recordOperation(t('operation.screenshot'), `${canvas.width} × ${canvas.height}`, 'success')
    } catch (cause) { fail(cause) } finally {
      stream?.getTracks().forEach((track) => track.stop())
      setBusy(false)
    }
  }

  async function importBase64() {
    setBusy(true)
    try {
      const dataUrl = ensureImageDataUrl(base64Input)
      const dimensions = await imageDimensions(dataUrl)
      const mimeType = dataUrl.match(/^data:([^;]+)/)?.[1] ?? 'image/png'
      const saved = await imageApi.save({ name: `Base64-${timestamp()}.${mimeExtension(mimeType)}`, dataUrl, ...dimensions })
      setPanel(null)
      setBase64Input('')
      await load(saved.name)
      succeed('notice.base64Imported')
    } catch (cause) { fail(cause) } finally { setBusy(false) }
  }

  async function copyBase64() {
    if (!base64ExportValue) return
    try {
      await clipboardApi.writeText(base64ExportValue)
      succeed('notice.base64Copied')
    } catch (cause) { fail(cause) }
  }

  async function saveBase64() {
    if (!current || !base64ExportValue) return
    try {
      const stem = current.name.replace(/\.[^.]+$/, '')
      const path = await userFilesApi.exportText(`${stem}.base64.txt`, base64ExportValue)
      if (path) succeed('notice.base64Exported', { path })
    } catch (cause) { fail(cause) }
  }

  async function vectorizeSelected() {
    if (!processingNames.length) return
    setBusy(true)
    try {
      const paths = await imageApi.vectorize(processingNames, vectorizeOptions)
      if (!paths) return
      setPanel(null)
      succeed(paths.length === 1 ? 'notice.svgExportedOne' : 'notice.svgExported',
        paths.length === 1 ? { path: paths[0] ?? '' } : { count: paths.length })
      recordOperation(t('operation.vectorize'), t('operation.imageCount', { count: paths.length }), 'success')
    } catch (cause) { fail(cause) } finally { setBusy(false) }
  }

  async function processSelected(mode: 'compress' | 'watermark') {
    if (!processingNames.length) return
    setBusy(true)
    let first = ''
    try {
      for (const name of processingNames) {
        const asset = await imageApi.read(name)
        const dataUrl = mode === 'compress'
          ? await compressImage(asset.dataUrl, { quality, scale, format })
          : await watermarkImage(asset.dataUrl, { text: watermarkText, opacity: watermarkOpacity, color: watermarkColor, fontSize: watermarkSize, position: watermarkPosition, diagonal: watermarkDiagonal })
        const dimensions = await imageDimensions(dataUrl)
        const outputName = processedImageName(asset.name, mode === 'compress' ? 'compressed' : 'watermarked', mode === 'compress' ? format : 'auto')
        const saved = await imageApi.save({ name: outputName, dataUrl, ...dimensions })
        first ||= saved.name
      }
      setPanel(null)
      await load(first)
      succeed('notice.processed', { count: processingNames.length })
      recordOperation(
        t(mode === 'compress' ? 'operation.compress' : 'operation.watermark'),
        t('operation.imageCount', { count: processingNames.length }),
        'success'
      )
    } catch (cause) { fail(cause) } finally { setBusy(false) }
  }

  async function copyImage() {
    if (!current) return
    try {
      if (window.__TAURI_INTERNALS__) {
        const { rgba, width, height } = await dataUrlToRgba(current.dataUrl)
        const nativeImage = await TauriImage.new(rgba, width, height)
        try {
          await writeClipboardImage(nativeImage)
        } finally {
          await nativeImage.close()
        }
        succeed('notice.nativeCopied')
        return
      }
      const blob = await fetch(current.dataUrl).then((response) => response.blob())
      if (typeof ClipboardItem !== 'undefined') await navigator.clipboard.write([new ClipboardItem({ [blob.type]: blob })])
      else await clipboardApi.writeText(current.dataUrl)
      succeed('notice.copied')
    } catch (cause) { fail(cause) }
  }

  async function renameCurrent() {
    if (!current) return
    const nextName = await dialog.prompt(t('prompt.rename'), current.name)
    if (!nextName || nextName === current.name) return
    try {
      const renamed = await imageApi.rename(current.name, nextName)
      await load(renamed.name)
      succeed('notice.renamed')
    } catch (cause) { fail(cause) }
  }

  async function deleteSelected() {
    if (!processingNames.length || !await dialog.confirm(t('confirm.delete', { count: processingNames.length }), { dangerous: true })) return
    try {
      await imageApi.delete(processingNames)
      setCurrent(undefined)
      setSelected([])
      await load()
      succeed('notice.deleted')
    } catch (cause) { fail(cause) }
  }

  async function exportSelected() {
    try {
      const exported = await imageApi.export(processingNames)
      if (exported) {
        succeed(exported.length === 1 ? 'notice.exportedOne' : 'notice.exported',
          exported.length === 1 ? { path: exported[0] ?? '' } : { count: exported.length })
      }
    } catch (cause) { fail(cause) }
  }

  async function cancelCapture() {
    const names = captureAssets.map((asset) => asset.name)
    setCaptureAssets([])
    try {
      if (names.length) await imageApi.delete(names)
      succeed('notice.captureCancelled')
    } catch (cause) { fail(cause) }
  }

  async function keepFullCapture(asset: ImageAsset) {
    setBusy(true)
    try {
      const discard = captureAssets.filter((item) => item.name !== asset.name).map((item) => item.name)
      if (discard.length) await imageApi.delete(discard)
      setCaptureAssets([])
      await load(asset.name)
      succeed('notice.fullSaved', { width: asset.width, height: asset.height })
      recordOperation(
        t('operation.screenshot'),
        t('operation.fullSize', { width: asset.width, height: asset.height }),
        'success'
      )
    } catch (cause) { fail(cause) } finally { setBusy(false) }
  }

  async function saveCaptureRegion(asset: ImageAsset, selection: CropRegion) {
    setBusy(true)
    try {
      const dataUrl = await cropDataUrl(asset.dataUrl, selection)
      const saved = await imageApi.save({
        name: `Screenshot-${timestamp()}.png`,
        dataUrl,
        width: selection.width,
        height: selection.height
      })
      const discard = captureAssets.map((item) => item.name)
      if (discard.length) await imageApi.delete(discard)
      setCaptureAssets([])
      await load(saved.name)
      succeed('notice.regionSaved', { width: selection.width, height: selection.height })
      recordOperation(t('operation.regionScreenshot'), `${selection.width}×${selection.height}`, 'success')
    } catch (cause) { fail(cause) } finally { setBusy(false) }
  }

  function toggleSelection(name: string, checked: boolean) {
    setSelected((values) => checked ? [...new Set([...values, name])] : values.filter((value) => value !== name))
  }

  function fail(cause: unknown) {
    setFailed(true)
    if (cause instanceof ImageLocalizedError) {
      setNotice({ key: cause.key, values: cause.values })
      return
    }
    if (cause instanceof ImageToolError) {
      const key: ImageMessageKey = `error.${cause.code}`
      setNotice({ key })
      return
    }
    setNotice({ raw: cause instanceof Error ? cause.message : String(cause) })
  }

  function succeed(key: ImageMessageKey, values?: MessageValues) {
    setFailed(false)
    setNotice({ key, values })
  }

  return (
    <main
      className={nativeDragActive ? 'utility-workbench image-workbench image-workbench--drag-active' : 'utility-workbench image-workbench'}
      onDragOver={window.__TAURI_INTERNALS__ ? undefined : (event) => event.preventDefault()}
      onDrop={window.__TAURI_INTERNALS__ ? undefined : (event) => { event.preventDefault(); void importFiles([...event.dataTransfer.files]) }}
    >
      <header className="utility-header">
        <h1 className="visually-hidden">{t('title')}</h1>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>
      <div className="image-toolbar">
        <button type="button" disabled={busy} onClick={() => void captureScreen()}><ScanLine />{t('action.screenshot')}</button>
        <button type="button" disabled={busy} onClick={() => void importClipboard()}><ClipboardPaste />{t('action.clipboard')}</button>
        <button type="button" disabled={busy} onClick={() => fileInput.current?.click()}><FolderOpen />{t('action.importImages')}</button>
        <button type="button" onClick={() => setPanel('base64-import')}><ImageDown />{t('action.importBase64')}</button>
        <input ref={fileInput} hidden multiple type="file" accept="image/png,image/jpeg,image/webp,image/gif" onChange={(event) => { void importFiles([...event.target.files ?? []]); event.target.value = '' }} />
        <span />
        <button type="button" disabled={!processingNames.length || busy} onClick={() => setPanel('compress')}><Minus />{t('action.compress')}</button>
        <button type="button" disabled={!processingNames.length || busy} onClick={() => setPanel('watermark')}><Type />{t('action.watermark')}</button>
        <button type="button" disabled={!processingNames.length || busy} onClick={() => setPanel('svg')}><Shapes />{t('action.vectorize')}</button>
        <button type="button" disabled={!current} onClick={() => void copyImage()}><ClipboardCopy />{t('action.copy')}</button>
        <button type="button" disabled={!current} onClick={() => setPanel('base64-export')}><ImageUp />{t('action.exportBase64')}</button>
        <button type="button" disabled={!processingNames.length} onClick={() => void exportSelected()}><Download />{t('action.export')}</button>
      </div>
      <section className="image-layout">
        {nativeDragActive && <div className="image-drop-overlay"><Upload /><strong>{t('drop.title')}</strong><span>{t('drop.hint')}</span></div>}
        <aside className="image-library">
          <header><strong>{t('library.title')}</strong><span>{assets.length}</span></header>
          <div>
            {assets.length ? assets.map((asset) => (
              <article className={current?.name === asset.name ? 'image-library-item image-library-item--active' : 'image-library-item'} key={asset.name}>
                <input type="checkbox" checked={selected.includes(asset.name)} onChange={(event) => toggleSelection(asset.name, event.target.checked)} />
                <button type="button" onClick={() => void selectAsset(asset.name)}>
                  <FileImage />
                  <span><strong>{asset.name}</strong><small>{asset.width}×{asset.height} · {formatBytes(asset.sizeBytes)}</small></span>
                </button>
              </article>
            )) : <div className="image-empty">{t('library.empty')}</div>}
          </div>
          <footer>
            <button type="button" disabled={!current} title={t('action.rename')} onClick={() => void renameCurrent()}><Pencil /></button>
            <button type="button" disabled={!processingNames.length} title={t('action.delete')} onClick={() => void deleteSelected()}><Trash2 /></button>
          </footer>
        </aside>
        <section className="image-preview">
          <div className={fit ? 'image-preview-canvas image-preview-canvas--fit' : 'image-preview-canvas'}>
            {current
              ? <img src={current.dataUrl} alt={current.name} style={fit ? undefined : { width: `${current.width * zoom}px`, height: `${current.height * zoom}px` }} />
              : <div className="image-empty"><FileImage />{t('preview.empty')}</div>}
          </div>
          <footer>
            <button type="button" title={t('action.zoomOut')} disabled={!current} onClick={() => { setFit(false); setZoom((value) => Math.max(0.1, value * 0.9)) }}><Minus /></button>
            <button type="button" title={t('action.zoomIn')} disabled={!current} onClick={() => { setFit(false); setZoom((value) => Math.min(5, value * 1.1)) }}><Plus /></button>
            <button type="button" title={t('action.actualSize')} disabled={!current} onClick={() => { setFit(false); setZoom(1) }}><ZoomIn /></button>
            <button type="button" title={t('action.fit')} disabled={!current} onClick={() => setFit(true)}><Maximize2 /></button>
            <span>{current ? `${current.width} × ${current.height} · ${formatBytes(current.sizeBytes)} · ${fit ? t('preview.fit') : `${Math.round(zoom * 100)}%`}` : ''}</span>
          </footer>
        </section>
        {panel && (
          <ProcessingPanel mode={panel} onClose={() => setPanel(null)}>
            {panel === 'base64-import' ? <>
              <textarea value={base64Input} placeholder={t('base64.placeholder')} onChange={(event) => setBase64Input(event.target.value)} />
              <button className="primary-button" type="button" disabled={!base64Input.trim() || busy} onClick={() => void importBase64()}><Upload />{t('action.import')}</button>
            </> : panel === 'base64-export' ? <>
              <label className="image-base64-mode"><input type="checkbox" checked={base64DataUrl} onChange={(event) => setBase64DataUrl(event.target.checked)} />{t('base64.includePrefix')}</label>
              <textarea readOnly value={base64ExportValue} aria-label={t('base64.output')} />
              <div className="image-processing-actions">
                <button type="button" disabled={!base64ExportValue} onClick={() => void copyBase64()}><ClipboardCopy />{t('base64.copy')}</button>
                <button className="primary-button" type="button" disabled={!base64ExportValue} onClick={() => void saveBase64()}><Download />{t('base64.save')}</button>
              </div>
            </> : panel === 'svg' ? <>
              <label>{t('svg.preset')} <select value={vectorizeOptions.preset} onChange={(event) => setVectorizeOptions((value) => ({ ...value, preset: event.target.value as ImageVectorizeOptions['preset'] }))}><option value="poster">{t('svg.poster')}</option><option value="photo">{t('svg.photo')}</option><option value="bw">{t('svg.bw')}</option></select></label>
              <label>{t('svg.colors')} <input type="number" min="2" max="64" disabled={vectorizeOptions.preset === 'bw'} value={vectorizeOptions.colorCount} onChange={(event) => setVectorizeOptions((value) => ({ ...value, colorCount: Math.min(64, Math.max(2, Number(event.target.value) || 2)) }))} /></label>
              <label>{t('svg.detail')} <select value={vectorizeOptions.detail} onChange={(event) => setVectorizeOptions((value) => ({ ...value, detail: event.target.value as ImageVectorizeOptions['detail'] }))}><option value="low">{t('svg.low')}</option><option value="medium">{t('svg.medium')}</option><option value="high">{t('svg.high')}</option></select></label>
              <label>{t('svg.speckle')} <input type="number" min="0" max="128" value={vectorizeOptions.filterSpeckle} onChange={(event) => setVectorizeOptions((value) => ({ ...value, filterSpeckle: Math.min(128, Math.max(0, Number(event.target.value) || 0)) }))} /></label>
              <p className="image-vectorize-hint">{t('svg.hint')}</p>
              <button className="primary-button" type="button" disabled={busy} onClick={() => void vectorizeSelected()}><Shapes />{t('svg.start')}</button>
            </> : panel === 'compress' ? <>
              <label>{t('compress.quality')} <input type="range" min="5" max="100" value={Math.round(quality * 100)} onChange={(event) => setQuality(Number(event.target.value) / 100)} /><span>{Math.round(quality * 100)}%</span></label>
              <label>{t('compress.scale')} <input type="range" min="10" max="100" value={Math.round(scale * 100)} onChange={(event) => setScale(Number(event.target.value) / 100)} /><span>{Math.round(scale * 100)}%</span></label>
              <label>{t('compress.format')} <select value={format} onChange={(event) => setFormat(event.target.value as ImageFormat)}><option value="auto">{t('compress.auto')}</option><option value="png">PNG</option><option value="jpeg">JPEG</option></select></label>
              <button className="primary-button" type="button" disabled={busy} onClick={() => void processSelected('compress')}>{t('action.startCompress')}</button>
            </> : <>
              <label>{t('watermark.text')} <input value={watermarkText} onChange={(event) => setWatermarkText(event.target.value)} /></label>
              <label>{t('watermark.color')} <input type="color" value={watermarkColor} onChange={(event) => setWatermarkColor(event.target.value)} /></label>
              <label>{t('watermark.opacity')} <input type="range" min="1" max="100" value={Math.round(watermarkOpacity * 100)} onChange={(event) => setWatermarkOpacity(Number(event.target.value) / 100)} /><span>{Math.round(watermarkOpacity * 100)}%</span></label>
              <label>{t('watermark.fontSize')} <input type="number" min="12" max="200" value={watermarkSize} onChange={(event) => setWatermarkSize(Number(event.target.value))} /></label>
              <label>{t('watermark.position')} <select value={watermarkPosition} onChange={(event) => setWatermarkPosition(event.target.value as WatermarkPosition)}><option value="bottom-right">{t('watermark.bottomRight')}</option><option value="bottom-left">{t('watermark.bottomLeft')}</option><option value="top-right">{t('watermark.topRight')}</option><option value="top-left">{t('watermark.topLeft')}</option><option value="center">{t('watermark.center')}</option><option value="tile">{t('watermark.tile')}</option></select></label>
              <label><input type="checkbox" checked={watermarkDiagonal} onChange={(event) => setWatermarkDiagonal(event.target.checked)} />{t('watermark.diagonal')}</label>
              <button className="primary-button" type="button" disabled={busy || !watermarkText.trim()} onClick={() => void processSelected('watermark')}>{t('action.addWatermark')}</button>
            </>}
          </ProcessingPanel>
        )}
      </section>
      <footer className={failed ? 'utility-status utility-status--error' : 'utility-status'}><span>{failed ? <TriangleAlert /> : <CheckCircle2 />}{noticeText}</span><span>{t('footer.capabilities')}</span><code>{session.summary}</code></footer>
      {captureAssets.length > 0 && (
        <CaptureEditor
          assets={captureAssets}
          busy={busy}
          onCancel={() => void cancelCapture()}
          onKeepFull={(asset) => void keepFullCapture(asset)}
          onSave={(asset, selection) => void saveCaptureRegion(asset, selection)}
        />
      )}
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
    </main>
  )
}

interface CropRegion { x: number; y: number; width: number; height: number }

function CaptureEditor({ assets, busy, onCancel, onKeepFull, onSave }: {
  assets: ImageAsset[]
  busy: boolean
  onCancel: () => void
  onKeepFull: (asset: ImageAsset) => void
  onSave: (asset: ImageAsset, selection: CropRegion) => void
}) {
  const { t } = useLocalizedMessages(imageMessages)
  const imageRef = useRef<HTMLImageElement>(null)
  const drag = useRef<{ pointerId: number; x: number; y: number } | undefined>(undefined)
  const [index, setIndex] = useState(0)
  const [selection, setSelection] = useState<CropRegion>()
  const asset = assets[Math.min(index, assets.length - 1)]

  useEffect(() => {
    const handleKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !busy) onCancel()
    }
    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
  }, [busy, onCancel])

  function point(event: React.PointerEvent<HTMLImageElement>) {
    const rect = event.currentTarget.getBoundingClientRect()
    return {
      x: Math.max(0, Math.min(asset.width, Math.round((event.clientX - rect.left) * asset.width / rect.width))),
      y: Math.max(0, Math.min(asset.height, Math.round((event.clientY - rect.top) * asset.height / rect.height)))
    }
  }

  function updateSelection(event: React.PointerEvent<HTMLImageElement>) {
    const start = drag.current
    if (!start || start.pointerId !== event.pointerId) return
    const current = point(event)
    setSelection({
      x: Math.min(start.x, current.x),
      y: Math.min(start.y, current.y),
      width: Math.abs(current.x - start.x),
      height: Math.abs(current.y - start.y)
    })
  }

  return (
    <section className="capture-editor" role="dialog" aria-modal="true" aria-label={t('capture.dialog')}>
      <header>
        <div><Crop /><strong>{t('capture.dialog')}</strong><span>{t('capture.instructions')}</span></div>
        <button type="button" disabled={busy} aria-label={t('capture.cancel')} onClick={onCancel}><X /></button>
      </header>
      {assets.length > 1 && <nav>{assets.map((item, itemIndex) => <button className={itemIndex === index ? 'capture-monitor capture-monitor--active' : 'capture-monitor'} type="button" key={item.name} onClick={() => { setIndex(itemIndex); setSelection(undefined) }}><Monitor />{t('capture.monitor', { number: itemIndex + 1 })}<span>{item.width}×{item.height}</span></button>)}</nav>}
      <div className="capture-stage">
        <div className="capture-image-wrap">
          <img
            ref={imageRef}
            src={asset.dataUrl}
            alt={t('capture.alt', { number: index + 1 })}
            draggable={false}
            onPointerDown={(event) => {
              const start = point(event)
              drag.current = { pointerId: event.pointerId, ...start }
              event.currentTarget.setPointerCapture(event.pointerId)
              setSelection({ ...start, width: 0, height: 0 })
            }}
            onPointerMove={updateSelection}
            onPointerUp={(event) => { updateSelection(event); drag.current = undefined }}
            onPointerCancel={() => { drag.current = undefined }}
          />
          {selection && <span className="capture-selection" style={{ left: `${selection.x / asset.width * 100}%`, top: `${selection.y / asset.height * 100}%`, width: `${selection.width / asset.width * 100}%`, height: `${selection.height / asset.height * 100}%` }}><em>{selection.width} × {selection.height}</em></span>}
        </div>
      </div>
      <footer>
        <span>{selection && selection.width >= 2 && selection.height >= 2 ? t('capture.selection', { width: selection.width, height: selection.height }) : t('capture.drag')}</span>
        <button type="button" disabled={busy} onClick={() => onKeepFull(asset)}>{t('capture.keepFull')}</button>
        <button className="primary-button" type="button" disabled={busy || !selection || selection.width < 2 || selection.height < 2} onClick={() => selection && onSave(asset, selection)}><Crop />{t('capture.saveRegion')}</button>
      </footer>
    </section>
  )
}

function ProcessingPanel({ mode, onClose, children }: { mode: 'compress' | 'watermark' | 'base64-import' | 'base64-export' | 'svg'; onClose: () => void; children: ReactNode }) {
  const { t } = useLocalizedMessages(imageMessages)
  const title = mode === 'compress' ? 'panel.compress' : mode === 'watermark' ? 'panel.watermark' : mode === 'base64-import' ? 'panel.base64Import' : mode === 'base64-export' ? 'panel.base64Export' : 'panel.svg'
  return <aside className="image-processing"><header><strong>{t(title)}</strong><button type="button" aria-label={t('panel.close')} onClick={onClose}>×</button></header>{children}</aside>
}

function fileToDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result))
    reader.onerror = () => reject(reader.error ?? new ImageLocalizedError('error.fileRead'))
    reader.readAsDataURL(file)
  })
}

function rgbaToPngDataUrl(rgba: Uint8Array, width: number, height: number): string {
  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const context = canvas.getContext('2d')
  if (!context) throw new ImageLocalizedError('error.imageCanvas')
  const pixels = new Uint8ClampedArray(rgba.byteLength)
  pixels.set(rgba)
  context.putImageData(new ImageData(pixels, width, height), 0, 0)
  return canvas.toDataURL('image/png')
}

async function dataUrlToRgba(dataUrl: string): Promise<{ rgba: Uint8Array; width: number; height: number }> {
  const image = await loadImage(dataUrl)
  const canvas = document.createElement('canvas')
  canvas.width = image.naturalWidth
  canvas.height = image.naturalHeight
  const context = canvas.getContext('2d')
  if (!context) throw new ImageLocalizedError('error.imageCanvas')
  context.drawImage(image, 0, 0)
  const pixels = context.getImageData(0, 0, canvas.width, canvas.height)
  return { rgba: new Uint8Array(pixels.data), width: canvas.width, height: canvas.height }
}

async function cropDataUrl(dataUrl: string, region: CropRegion): Promise<string> {
  const image = await loadImage(dataUrl)
  const canvas = document.createElement('canvas')
  canvas.width = region.width
  canvas.height = region.height
  const context = canvas.getContext('2d')
  if (!context) throw new ImageLocalizedError('error.captureCanvas')
  context.drawImage(image, region.x, region.y, region.width, region.height, 0, 0, region.width, region.height)
  return canvas.toDataURL('image/png')
}

function loadImage(dataUrl: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new ImageLocalizedError('error.decode'))
    image.src = dataUrl
  })
}

function mimeExtension(mimeType: string): string {
  if (mimeType === 'image/jpeg') return 'jpg'
  if (mimeType === 'image/webp') return 'webp'
  if (mimeType === 'image/gif') return 'gif'
  return 'png'
}

function timestamp(): string {
  return new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19)
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(1)} KiB`
  return `${(bytes / 1024 ** 2).toFixed(2)} MiB`
}
