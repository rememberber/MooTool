import { ClipboardPaste, Copy, Download, FolderOpen, History, QrCode, ScanLine, Trash2 } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { useSettings } from '@/features/settings/SettingsProvider'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import { ToolPageHeader, ToolTabs } from '@/shared/components/ToolPage'
import { TextCodeEditor } from '@/shared/components/TextCodeEditor'
import type { FuncHistoryRecord } from '@/shared/contracts/history'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { generateQrDataUrl, normalizeQrSize, recognizeQrDataUrl, type QrErrorCorrection } from './qrTools'

type QrTab = 'generate' | 'recognize' | 'history'

export function QrCodeTool() {
  const { language, t } = useI18n()
  const actions = useToolActions('qrCode')
  const { settings, updateSettings } = useSettings()
  const [tab, setTab] = useState<QrTab>('generate')
  const [content, setContent] = useState('https://github.com/rememberber/MooTool')
  const [size, setSize] = useState(settings.tools.qrCodeSize)
  const [correction, setCorrection] = useState<QrErrorCorrection>(settings.tools.qrErrorCorrection)
  const [logoName, setLogoName] = useState('')
  const [logoDataUrl, setLogoDataUrl] = useState('')
  const [qrDataUrl, setQrDataUrl] = useState('')
  const [recognitionName, setRecognitionName] = useState('')
  const [recognitionDataUrl, setRecognitionDataUrl] = useState('')
  const [recognitionResult, setRecognitionResult] = useState('')
  const [busy, setBusy] = useState(false)
  const [history, setHistory] = useState<FuncHistoryRecord[]>([])

  const loadHistory = useCallback(async () => {
    setHistory(await window.mootool.listHistory({ funcType: 'qrCode' }))
  }, [])
  useEffect(() => { if (tab === 'history') void loadHistory() }, [loadHistory, tab])

  async function generate(): Promise<void> {
    setBusy(true)
    try {
      const normalizedSize = normalizeQrSize(size)
      const output = await generateQrDataUrl(content, normalizedSize, correction, logoDataUrl)
      setQrDataUrl(output)
      setSize(normalizedSize)
      await updateSettings({ tools: { qrCodeSize: normalizedSize, qrErrorCorrection: correction } })
      await actions.saveHistory(t('qrcode.history.generate'), content, output, JSON.stringify({ operation: 'generate', size: normalizedSize, correction }))
      actions.toast.success(t('qrcode.generated'))
    } catch (error) { actions.reportError(error) } finally { setBusy(false) }
  }

  async function chooseLogo(): Promise<void> {
    try {
      const file = await window.mootool.chooseImageFile()
      if (!file) return
      setLogoName(file.name)
      setLogoDataUrl(file.dataUrl)
    } catch (error) { actions.reportError(error) }
  }

  async function saveQr(): Promise<void> {
    if (!qrDataUrl) return
    try {
      await window.mootool.saveBinaryFile({ defaultName: 'mootool-qrcode.png', dataUrl: qrDataUrl, kind: 'image' })
      actions.toast.success(t('common.saved'))
    } catch (error) { actions.reportError(error) }
  }

  async function chooseRecognitionImage(): Promise<void> {
    try {
      const file = await window.mootool.chooseImageFile()
      if (!file) return
      setRecognitionName(file.name)
      setRecognitionDataUrl(file.dataUrl)
      await recognize(file.dataUrl, file.name)
    } catch (error) { actions.reportError(error) }
  }

  async function recognizeFromClipboard(): Promise<void> {
    try {
      const dataUrl = await window.mootool.readClipboardImage()
      if (!dataUrl) throw new Error(t('qrcode.clipboardEmpty'))
      setRecognitionName(t('qrcode.clipboard'))
      setRecognitionDataUrl(dataUrl)
      await recognize(dataUrl, t('qrcode.clipboard'))
    } catch (error) { actions.reportError(error) }
  }

  async function recognize(dataUrl = recognitionDataUrl, name = recognitionName): Promise<void> {
    if (!dataUrl) return
    setBusy(true)
    try {
      const result = await recognizeQrDataUrl(dataUrl)
      setRecognitionResult(result)
      await actions.saveHistory(t('qrcode.history.recognize'), name, result, JSON.stringify({ operation: 'recognize' }))
      actions.toast.success(t('qrcode.recognized'))
    } catch (error) { actions.reportError(error) } finally { setBusy(false) }
  }

  async function deleteHistory(id: number): Promise<void> {
    await window.mootool.deleteHistory(id)
    await loadHistory()
  }

  return (
    <section className="tool-page p4-tool">
      <ToolPageHeader title={t('qrcode.title')} />
      <div className="local-tool-shell qrcode-workspace">
        <ToolTabs tabs={[{ id: 'generate', label: t('qrcode.tab.generate') }, { id: 'recognize', label: t('qrcode.tab.recognize') }, { id: 'history', label: t('qrcode.tab.history') }]} active={tab} onChange={setTab} />
        {tab === 'generate' && <ResizableColumns className="qrcode-generate-layout" columns={2} defaultSizes={[1, 1]} minPaneWidths={[300, 280]} storageKey="qrcode-generate"><section className="qrcode-input-panel"><div className="qrcode-content-editor"><span>{t('qrcode.content')}</span><TextCodeEditor ariaLabel={t('qrcode.content')} value={content} onChange={setContent} /></div><div className="qrcode-options"><label><span>{t('qrcode.size')}</span><div><input type="number" min={120} max={2000} value={size} onChange={(event) => setSize(Number(event.target.value))} /><em>px</em></div></label><label><span>{t('qrcode.correction')}</span><select value={correction} onChange={(event) => setCorrection(event.target.value as QrErrorCorrection)}>{(['L', 'M', 'Q', 'H'] as const).map((level) => <option value={level} key={level}>{t(`qrcode.level.${level}` as 'qrcode.level.L')}</option>)}</select></label><label><span>{t('qrcode.logo')}</span><button className="dialog-button" type="button" onClick={() => { void chooseLogo() }}><FolderOpen size={14} />{logoName || t('qrcode.chooseLogo')}</button></label></div><button className="primary-command qrcode-generate-button" type="button" disabled={busy || !content.trim()} onClick={() => { void generate() }}><QrCode size={15} />{busy ? t('common.processing') : t('qrcode.generate')}</button></section><section className="qrcode-preview-panel">{qrDataUrl ? <img src={qrDataUrl} alt={t('qrcode.preview')} /> : <div className="image-placeholder"><QrCode size={42} /><span>{t('qrcode.preview')}</span></div>}<div className="qrcode-preview-actions"><button className="dialog-button" type="button" disabled={!qrDataUrl} onClick={() => { void saveQr() }}><Download size={14} />{t('common.save')}</button><button className="dialog-button" type="button" disabled={!qrDataUrl} onClick={() => { void window.mootool.writeClipboardImage(qrDataUrl); actions.toast.success(t('json.notice.copied')) }}><Copy size={14} />{t('common.action.copy')}</button></div></section></ResizableColumns>}
        {tab === 'recognize' && <ResizableColumns className="qrcode-recognize-layout" columns={2} defaultSizes={[1, 1]} minPaneWidths={[300, 280]} storageKey="qrcode-recognize"><section className="qrcode-recognize-source"><div className="p4-toolbar"><button className="dialog-button" type="button" onClick={() => { void chooseRecognitionImage() }}><FolderOpen size={14} />{t('qrcode.chooseImage')}</button><button className="dialog-button" type="button" onClick={() => { void recognizeFromClipboard() }}><ClipboardPaste size={14} />{t('qrcode.fromClipboard')}</button><button className="primary-command" type="button" disabled={!recognitionDataUrl || busy} onClick={() => { void recognize() }}><ScanLine size={14} />{t('qrcode.recognize')}</button></div>{recognitionDataUrl ? <img src={recognitionDataUrl} alt={recognitionName || t('qrcode.sourceImage')} /> : <div className="image-placeholder"><ScanLine size={42} /><span>{t('qrcode.chooseImage')}</span></div>}<span>{recognitionName}</span></section><div className="qrcode-result"><span>{t('qrcode.result')}</span><TextCodeEditor testId="qrcode-result-text" ariaLabel={t('qrcode.result')} value={recognitionResult} readOnly /><button className="toolbar-button" type="button" disabled={!recognitionResult} onClick={() => { void actions.copy(recognitionResult) }}><Copy size={14} />{t('common.action.copy')}</button></div></ResizableColumns>}
        {tab === 'history' && <div className="qrcode-history">{history.length === 0 ? <div className="history-empty">{t('history.empty')}</div> : history.map((record) => <article key={record.id}><button type="button" onClick={() => { let meta: { operation?: string } = {}; try { meta = JSON.parse(record.extraData ?? '{}') as { operation?: string } } catch { /* Legacy history. */ } if (meta.operation === 'generate') { setContent(record.inputText); setQrDataUrl(record.outputText); setTab('generate') } else { setRecognitionName(record.inputText); setRecognitionResult(record.outputText); setTab('recognize') } }}><History size={14} /><span><strong>{record.summary}</strong><small>{new Date(record.createTime.replace(' ', 'T')).toLocaleString(language)}</small><p>{record.inputText}</p></span></button><button className="icon-button" type="button" aria-label={t('history.delete')} onClick={() => { void deleteHistory(record.id) }}><Trash2 size={14} /></button></article>)}</div>}
      </div>
    </section>
  )
}
