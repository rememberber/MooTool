import {
  CheckCircle2,
  Clipboard,
  ClipboardPaste,
  Copy,
  Download,
  FolderOpen,
  History,
  ImagePlus,
  QrCode,
  ScanLine,
  Trash2,
  TriangleAlert
} from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import {
  useLocalizedMessages,
  type LocalizedMessageKey,
  type MessageValues
} from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { historyApi } from '../../platform/api/historyApi'
import { userFilesApi } from '../../platform/api/userFilesApi'
import type { OperationHistory } from '../../platform/contracts/history'
import { CodeEditor } from '../../shared/CodeEditor'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import { parseOperationMetadata, useOperationRestore } from '../history/operationRestore'
import { useSettings } from '../settings/SettingsProvider'
import {
  decodeQrImage,
  defaultQrOptions,
  generateQrPngDataUrl,
  QrToolError,
  readQrLogo,
  type QrErrorCorrection,
  type QrOptions
} from './qrTools'
import { qrcodeMessages } from './qrcodeMessages'
import { qrcodeSessionDigest } from './qrcodeSession'

type QrcodeMessageKey = LocalizedMessageKey<typeof qrcodeMessages>
type Notice = { key: QrcodeMessageKey; values?: MessageValues } | { raw: string }
type QrTab = 'generate' | 'recognize' | 'history'

export function QrcodeSurface() {
  const { t } = useLocalizedMessages(qrcodeMessages)
  const { settings, ready, save } = useSettings()
  const [source, setSource] = useState('https://github.com/rememberber/MooTool')
  const [options, setOptions] = useState<QrOptions>(() => ({ ...defaultQrOptions, size: settings.tools.qrCodeSize, errorCorrection: settings.tools.qrErrorCorrection }))
  const [tab, setTab] = useState<QrTab>('generate')
  const [png, setPng] = useState('')
  const [busy, setBusy] = useState(false)
  const [logoName, setLogoName] = useState('')
  const [logoDataUrl, setLogoDataUrl] = useState('')
  const recognitionFile = useRef<File | null>(null)
  const [recognitionName, setRecognitionName] = useState('')
  const [recognitionUrl, setRecognitionUrl] = useState('')
  const [history, setHistory] = useState<OperationHistory[]>([])
  const [hydrated, setHydrated] = useState(false)
  const [decoded, setDecoded] = useState('')
  const [notice, setNotice] = useState<Notice>({ key: 'notice.ready' })
  const [failed, setFailed] = useState(false)
  const [copied, setCopied] = useState(false)
  const noticeText = 'raw' in notice ? notice.raw : t(notice.key, notice.values)
  const session = useMemo(() => ({
    digest: qrcodeSessionDigest(source, decoded, options),
    summary: t('session.summary', {
      generate: t(png ? 'session.done' : 'session.waitGenerate'),
      decode: t(decoded ? 'session.done' : 'session.waitDecode')
    })
  }), [decoded, options, png, source, t])
  const { sessionId, reportError } = useToolSessionReport('qrcode', session.digest, session.summary)
  const recordOperation = useOperationHistory('qrcode')
  useOperationRestore('qrcode', (entry) => {
    const metadata = parseOperationMetadata(entry)
    if (metadata.operation === 'decode') {
      setRecognitionName(entry.inputText)
      setDecoded(entry.outputText)
      setTab('recognize')
    } else {
      setSource(entry.inputText)
      if (metadata.options && typeof metadata.options === 'object') setOptions({ ...defaultQrOptions, ...metadata.options as Partial<QrOptions> })
      setPng(entry.outputText.startsWith('data:image') ? entry.outputText : '')
      setTab('generate')
    }
    setFailed(false)
  })

  useEffect(() => {
    if (!ready || hydrated) return
    setOptions((current) => ({ ...current, size: settings.tools.qrCodeSize, errorCorrection: settings.tools.qrErrorCorrection }))
    setHydrated(true)
  }, [hydrated, ready, settings.tools.qrCodeSize, settings.tools.qrErrorCorrection])

  useEffect(() => {
    if (tab !== 'history') return
    let active = true
    void historyApi.list(settings.data.historyLimit).then((items) => {
      if (active) setHistory(items.filter((item) => item.toolId === 'qrcode'))
    }).catch(() => undefined)
    return () => { active = false }
  }, [settings.data.historyLimit, tab])

  async function generate(): Promise<void> {
    setBusy(true)
    try {
      const output = await generateQrPngDataUrl(source, options, logoDataUrl)
      setPng(output)
      await save((current) => ({ ...current, tools: { ...current.tools, qrCodeSize: options.size, qrErrorCorrection: options.errorCorrection } }))
      await historyApi.record({
        id: crypto.randomUUID(),
        toolId: 'qrcode',
        action: t('action.generate'),
        summary: `${source.length} · ${options.size}px`,
        status: 'success',
        inputText: source,
        outputText: output,
        metadataJson: JSON.stringify({ operation: 'generate', options }),
        createdAt: Date.now()
      }, settings.data.historyLimit)
      succeed('notice.generated')
    } catch (cause) {
      setPng('')
      fail(cause)
    } finally {
      setBusy(false)
    }
  }

  async function decode(file?: File, name = file?.name ?? recognitionName): Promise<void> {
    const target = file ?? recognitionFile.current ?? undefined
    if (!target) return
    recognitionFile.current = target
    file = target
    setBusy(true)
    if (recognitionUrl) URL.revokeObjectURL(recognitionUrl)
    setRecognitionName(name || file.name)
    setRecognitionUrl(URL.createObjectURL(file))
    try {
      const value = await decodeQrImage(file)
      setDecoded(value)
      await historyApi.record({
        id: crypto.randomUUID(),
        toolId: 'qrcode',
        action: t('scan.title'),
        summary: name || file.name,
        status: 'success',
        inputText: name || file.name,
        outputText: value,
        metadataJson: JSON.stringify({ operation: 'decode', fileName: name || file.name }),
        createdAt: Date.now()
      }, settings.data.historyLimit)
      succeed('notice.decoded')
    } catch (cause) {
      setDecoded('')
      fail(cause)
    } finally {
      setBusy(false)
    }
  }

  async function download(): Promise<void> {
    if (!png) return
    try {
      const path = await userFilesApi.exportDataUrl('mootool-qrcode.png', png)
      if (path) { succeed('notice.exported', { path }); recordOperation(t('action.save'), path, 'success') }
    } catch (cause) { fail(cause) }
  }

  async function chooseLogo(file?: File): Promise<void> {
    if (!file) return
    try {
      setLogoDataUrl(await readQrLogo(file))
      setLogoName(file.name)
      succeed('notice.logoLoaded', { name: file.name })
    } catch (cause) { fail(cause) }
  }

  async function copyQrImage(): Promise<void> {
    if (!png) return
    try {
      await clipboardApi.writeImageDataUrl(png)
      succeed('notice.imageCopied')
    } catch (cause) { fail(cause) }
  }

  async function decodeClipboard(): Promise<void> {
    try {
      const file = await clipboardApi.readImageFile()
      if (!file) { failMessage('error.clipboardEmpty'); return }
      await decode(file)
    } catch (cause) { fail(cause) }
  }

  async function copyDecoded(): Promise<void> {
    try {
      await clipboardApi.writeText(decoded)
      setCopied(true)
      succeed('notice.copied')
      window.setTimeout(() => setCopied(false), 1200)
    } catch {
      failMessage('error.copy')
    }
  }

  function patchOptions(patch: Partial<QrOptions>): void {
    setOptions((current) => ({ ...current, ...patch }))
  }

  function succeed(key: QrcodeMessageKey, values?: MessageValues): void {
    setNotice({ key, values })
    setFailed(false)
  }

  function failMessage(key: QrcodeMessageKey, values?: MessageValues): void {
    setNotice({ key, values })
    setFailed(true)
  }

  function fail(cause: unknown): void {
    if (cause instanceof QrToolError) setNotice({ key: `error.${cause.code}`, values: cause.values })
    else setNotice({ raw: cause instanceof Error ? cause.message : String(cause) })
    setFailed(true)
  }

  return (
    <main className="utility-workbench qrcode-workbench">
      <header className="utility-header">
        <h1 className="visually-hidden">{t('title')}</h1>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>

      <div className="qrcode-tabs" role="tablist">
        {(['generate', 'recognize', 'history'] as const).map((id) => (
          <button key={id} type="button" role="tab" aria-selected={tab === id} onClick={() => setTab(id)}>{t(`tab.${id}`)}</button>
        ))}
      </div>
      {tab === 'generate' && (
        <section className="qrcode-grid qrcode-grid--generate">
          <section className="utility-editor-card qrcode-compose">
            <header><span>{t('compose.content')}</span><QrCode /></header>
            <CodeEditor ariaLabel={t('compose.content')} value={source} onChange={setSource} className="utility-code-editor" lineWrapping />
            <div className="qrcode-options">
              <label>{t('option.correction')}
                <select value={options.errorCorrection} onChange={(event) => patchOptions({ errorCorrection: event.target.value as QrErrorCorrection })}>
                  <option value="L">L · {t('option.approx', { percent: 7 })}</option>
                  <option value="M">M · {t('option.approx', { percent: 15 })}</option>
                  <option value="Q">Q · {t('option.approx', { percent: 25 })}</option>
                  <option value="H">H · {t('option.approx', { percent: 30 })}</option>
                </select>
              </label>
              <label>{t('option.size')}
                <input type="number" min={120} max={2000} value={options.size} onChange={(event) => patchOptions({ size: Number(event.target.value) })} />
              </label>
              <label className="qrcode-logo-input">{t('option.logo')}
                <span className="secondary-button"><ImagePlus />{logoName || t('action.chooseLogo')}<input type="file" accept="image/*" onChange={(event) => { void chooseLogo(event.target.files?.[0]); event.target.value = '' }} /></span>
              </label>
              {logoDataUrl && <button className="secondary-button" type="button" onClick={() => { setLogoDataUrl(''); setLogoName('') }}>{t('action.removeLogo')}</button>}
            </div>
            <button className="primary-button" type="button" disabled={busy || !source.trim()} onClick={() => void generate()}><QrCode />{busy ? t('session.waitGenerate') : t('action.generate')}</button>
          </section>
          <section className="qrcode-preview-card">
            <div className="qrcode-preview">{png ? <img src={png} alt={t('preview.alt')} /> : <QrCode />}</div>
            <div className="qrcode-preview-actions">
              <button className="secondary-button" type="button" disabled={!png} onClick={() => void download()}><Download />{t('action.save')}</button>
              <button className="secondary-button" type="button" disabled={!png} onClick={() => void copyQrImage()}><Copy />{t('action.copyImage')}</button>
            </div>
          </section>
        </section>
      )}
      {tab === 'recognize' && (
        <section className="qrcode-grid qrcode-grid--generate">
          <section className="qrcode-scan-card">
            <header>
              <label className="secondary-button qrcode-upload"><FolderOpen />{t('action.chooseImage')}<input type="file" accept="image/*" onChange={(event) => { void decode(event.target.files?.[0]); event.target.value = '' }} /></label>
              <button className="secondary-button" type="button" onClick={() => void decodeClipboard()}><ClipboardPaste />{t('action.fromClipboard')}</button>
              <button className="primary-button" type="button" disabled={!recognitionFile.current || busy} onClick={() => void decode()}><ScanLine />{t('scan.title')}</button>
            </header>
            {recognitionUrl ? <img src={recognitionUrl} alt={recognitionName} /> : <div className="qrcode-preview"><ScanLine /></div>}
            <span>{recognitionName}</span>
          </section>
          <section className="qrcode-decoded">
            <span>{t('scan.result')}</span>
            <CodeEditor ariaLabel={t('scan.result')} value={decoded} readOnly className="utility-code-editor" lineWrapping />
            <button className="utility-copy" type="button" disabled={!decoded} onClick={() => void copyDecoded()}>{copied ? <Clipboard /> : <Copy />}{t(copied ? 'action.copied' : 'action.copy')}</button>
          </section>
        </section>
      )}
      {tab === 'history' && (
        <section className="qrcode-history">
          {history.length === 0 ? <p role="status">{t('history.empty')}</p> : history.map((record) => (
            <article key={record.id}>
              <button type="button" onClick={() => {
                const metadata = parseOperationMetadata(record)
                if (metadata.operation === 'decode') {
                  setRecognitionName(record.inputText)
                  setDecoded(record.outputText)
                  setTab('recognize')
                } else {
                  setSource(record.inputText)
                  setPng(record.outputText.startsWith('data:image') ? record.outputText : '')
                  setTab('generate')
                }
              }}>
                <History /><span><strong>{record.action}</strong><small>{new Date(record.createdAt).toLocaleString()}</small><p>{record.inputText}</p></span>
              </button>
              <button type="button" aria-label={t('history.delete')} onClick={() => void historyApi.delete(record.id).then(() => setHistory((current) => current.filter((item) => item.id !== record.id)))}><Trash2 /></button>
            </article>
          ))}
        </section>
      )}

      <footer className={failed ? 'utility-status utility-status--error' : 'utility-status'}>
        <span>{failed ? <TriangleAlert /> : <CheckCircle2 />}{noticeText}</span>
        <span>{t('footer.capabilities')}</span>
        <code>{session.summary}</code>
      </footer>
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
    </main>
  )
}
