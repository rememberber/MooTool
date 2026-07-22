import { ClipboardCopy, ClipboardPaste, Download, FileImage, FolderOpen, ImageDown, ImagePlus, List, Maximize2, Minimize2, Minus, Pencil, Plus, Save, ScanLine, Trash2, Type, Upload, ZoomIn } from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { ToolPageHeader } from '@/shared/components/ToolPage'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import type { ImageAsset, ImageAssetSummary } from '@/shared/contracts/images'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { ImageBase64Dialog, ImageCompressDialog, ImageWatermarkDialog } from './ImageDialogs'
import { compressImage, ensureImageDataUrl, processedImageName, watermarkImage, type CompressImageOptions, type ImageOutputMode, type WatermarkImageOptions } from './imageTools'

export function ImageTool() {
  const { t } = useI18n()
  const actions = useToolActions('image')
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
    const name = window.prompt(t('image.saveName'), current.name)
    if (!name) return
    try { const saved = await window.mootool.saveImageAsset({ name, dataUrl: current.dataUrl }); await loadAssets(saved.name); actions.toast.success(t('common.saved')) } catch (error) { actions.reportError(error) }
  }

  async function renameCurrent(): Promise<void> {
    if (!current) return
    const nextName = window.prompt(t('image.renamePrompt'), current.name)
    if (!nextName || nextName === current.name) return
    try { const renamed = await window.mootool.renameImageAsset({ name: current.name, nextName }); await loadAssets(renamed.name) } catch (error) { actions.reportError(error) }
  }

  async function deleteSelected(): Promise<void> {
    if (!processingNames.length || !window.confirm(t('image.confirmDelete', { count: String(processingNames.length) }))) return
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
        <div className="image-main-toolbar"><button className="toolbar-button toolbar-button--icon" type="button" aria-label={t('image.toggleList')} onClick={() => setListVisible((value) => !value)}><List size={14} /></button><button className="toolbar-button" type="button" disabled={busy} onClick={() => { void capture() }}><ScanLine size={14} />{t('image.screenshot')}</button><button className="toolbar-button" type="button" onClick={() => { void importClipboard() }}><ClipboardPaste size={14} />{t('image.fromClipboard')}</button><button className="toolbar-button" type="button" onClick={() => { void importImages() }}><FolderOpen size={14} />{t('image.import')}</button><button className="toolbar-button" type="button" onClick={() => setBase64Mode('import')}><ImageDown size={14} />{t('image.fromBase64')}</button><span className="p4-toolbar__spacer" /><button className="toolbar-button" type="button" disabled={!processingNames.length || busy} onClick={() => setCompressOpen(true)}><Minimize2 size={14} />{t('image.compress')}</button><button className="toolbar-button" type="button" disabled={!processingNames.length || busy} onClick={() => setWatermarkOpen(true)}><Type size={14} />{t('image.watermark')}</button><button className="toolbar-button" type="button" disabled={!current} onClick={() => { void saveCurrent() }}><Save size={14} />{t('common.save')}</button><button className="toolbar-button" type="button" disabled={!current} onClick={() => { void copyImage() }}><ClipboardCopy size={14} />{t('image.copy')}</button><button className="toolbar-button" type="button" disabled={!current} onClick={() => setBase64Mode('export')}><Upload size={14} />{t('image.toBase64')}</button></div>
        <ResizableColumns className={listVisible ? 'image-layout' : 'image-layout image-layout--collapsed'} columns={listVisible ? 2 : 1} defaultSizes={listVisible ? [230, 770] : [1]} minPaneWidths={listVisible ? [180, 360] : [360]} storageKey="image-library">
          {listVisible && <aside className="image-library"><header><span>{t('image.library')}</span><button className="icon-button" type="button" aria-label={t('image.import')} onClick={() => { void importImages() }}><ImagePlus size={14} /></button></header><div className="image-list">{assets.length === 0 ? <div className="history-empty">{t('image.empty')}</div> : assets.map((asset) => <div className={current?.name === asset.name ? 'image-list-item image-list-item--active' : 'image-list-item'} key={asset.name}><input type="checkbox" aria-label={`${t('image.select')} ${asset.name}`} checked={selectedNames.includes(asset.name)} onChange={(event) => toggleAsset(asset.name, event.target.checked)} /><button type="button" onClick={() => { void selectAsset(asset.name) }}><FileImage size={15} /><span><strong>{asset.name}</strong><small>{asset.width} × {asset.height} · {formatBytes(asset.size)}</small></span></button></div>)}</div><footer><button className="icon-button" type="button" disabled={!current} aria-label={t('common.rename')} onClick={() => { void renameCurrent() }}><Pencil size={14} /></button><button className="icon-button" type="button" disabled={!processingNames.length} aria-label={t('common.export')} onClick={() => { void exportSelected() }}><Download size={14} /></button><button className="icon-button icon-button--danger" type="button" disabled={!processingNames.length} aria-label={t('common.action.delete')} onClick={() => { void deleteSelected() }}><Trash2 size={14} /></button></footer></aside>}
          <main className="image-canvas-panel" onDoubleClick={() => { if (current) void window.mootool.openImageAsset(current.name) }}><div className={fit ? 'image-canvas image-canvas--fit' : 'image-canvas'}>{current ? <img src={current.dataUrl} alt={current.name} style={fit ? undefined : { width: `${current.width * zoom}px`, height: `${current.height * zoom}px` }} /> : <div className="image-placeholder"><FileImage size={48} /><span>{t('image.emptyPreview')}</span></div>}</div><div className="image-zoom-toolbar"><button className="icon-button" type="button" aria-label={t('image.zoomIn')} disabled={!current} onClick={() => { setFit(false); setZoom((value) => Math.min(5, value * 1.1)) }}><Plus size={14} /></button><button className="icon-button" type="button" aria-label={t('image.zoomOut')} disabled={!current} onClick={() => { setFit(false); setZoom((value) => Math.max(0.1, value * 0.9)) }}><Minus size={14} /></button><button className="icon-button" type="button" aria-label={t('image.original')} disabled={!current} onClick={() => { setFit(false); setZoom(1) }}><ZoomIn size={14} /></button><button className="icon-button" type="button" aria-label={t('image.fit')} disabled={!current} onClick={() => setFit(true)}><Maximize2 size={14} /></button><span>{current ? `${current.width} × ${current.height} · ${formatBytes(current.size)} · ${fit ? t('image.fit') : `${Math.round(zoom * 100)}%`}` : ''}</span></div></main>
        </ResizableColumns>
      </div>
      <ImageBase64Dialog open={base64Mode !== null} mode={base64Mode ?? 'import'} value={base64Mode === 'export' ? current?.dataUrl ?? '' : ''} onClose={() => setBase64Mode(null)} onImport={(value) => { void importBase64(value) }} />
      <ImageCompressDialog open={compressOpen} count={processingNames.length} onClose={() => setCompressOpen(false)} onConfirm={(options, mode) => { void processCompression(options, mode) }} />
      <ImageWatermarkDialog open={watermarkOpen} count={processingNames.length} onClose={() => setWatermarkOpen(false)} onConfirm={(options, mode) => { void processWatermark(options, mode) }} />
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
