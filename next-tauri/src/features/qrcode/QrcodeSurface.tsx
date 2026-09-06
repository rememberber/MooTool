import {
  CheckCircle2,
  Clipboard,
  ClipboardPaste,
  Copy,
  Download,
  ImagePlus,
  ImageUp,
  QrCode,
  RefreshCw,
  ScanLine,
  TriangleAlert
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import {
  useLocalizedMessages,
  type LocalizedMessageKey,
  type MessageValues
} from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { userFilesApi } from '../../platform/api/userFilesApi'
import { CodeEditor } from '../../shared/CodeEditor'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import { parseOperationMetadata, useOperationRestore } from '../history/operationRestore'
import { useSettings } from '../settings/SettingsProvider'
import {
  decodeQrImage,
  defaultQrOptions,
  generateQrSvg,
  generateQrPngDataUrl,
  QrToolError,
  readQrLogo,
  svgDataUrl,
  type QrErrorCorrection,
  type QrOptions
} from './qrTools'
import { qrcodeMessages } from './qrcodeMessages'
import { qrcodeSessionDigest } from './qrcodeSession'

type QrcodeMessageKey = LocalizedMessageKey<typeof qrcodeMessages>
type Notice = { key: QrcodeMessageKey; values?: MessageValues } | { raw: string }

export function QrcodeSurface() {
  const { t } = useLocalizedMessages(qrcodeMessages)
  const { settings, ready, save } = useSettings()
  const [source, setSource] = useState('https://mootool.app/?product=next-tauri')
  const [options, setOptions] = useState<QrOptions>(() => ({ ...defaultQrOptions, size: settings.tools.qrCodeSize, errorCorrection: settings.tools.qrErrorCorrection }))
  const [svg, setSvg] = useState('')
  const [png, setPng] = useState('')
  const [outputFormat, setOutputFormat] = useState<'svg' | 'png'>('png')
  const [logoName, setLogoName] = useState('')
  const [logoDataUrl, setLogoDataUrl] = useState('')
  const [hydrated, setHydrated] = useState(false)
  const [decoded, setDecoded] = useState('')
  const [notice, setNotice] = useState<Notice>({ key: 'notice.ready' })
  const [failed, setFailed] = useState(false)
  const [copied, setCopied] = useState(false)
  const noticeText = 'raw' in notice ? notice.raw : t(notice.key, notice.values)
  const session = useMemo(() => ({
    digest: qrcodeSessionDigest(source, decoded, options),
    summary: t('session.summary', {
      generate: t(svg ? 'session.done' : 'session.waitGenerate'),
      decode: t(decoded ? 'session.done' : 'session.waitDecode')
    })
  }), [decoded, options, source, svg, t])
  const { sessionId, reportError } = useToolSessionReport('qrcode', session.digest, session.summary)
  const recordOperation = useOperationHistory('qrcode')
  useOperationRestore('qrcode', (entry) => {
    const metadata = parseOperationMetadata(entry)
    if (metadata.operation === 'decode') {
      setDecoded(entry.outputText)
    } else {
      setSource(entry.inputText)
      if (metadata.options && typeof metadata.options === 'object') setOptions({ ...defaultQrOptions, ...metadata.options as Partial<QrOptions> })
      setSvg(entry.outputText)
    }
    setFailed(false)
  })

  useEffect(() => {
    const handle = window.setTimeout(() => void renderQr(), 160)
    return () => window.clearTimeout(handle)
    // renderQr only uses state values listed below.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [logoDataUrl, source, options])

  useEffect(() => {
    if (!ready || hydrated) return
    setOptions((current) => ({ ...current, size: settings.tools.qrCodeSize, errorCorrection: settings.tools.qrErrorCorrection }))
    setHydrated(true)
  }, [hydrated, ready, settings.tools.qrCodeSize, settings.tools.qrErrorCorrection])

  useEffect(() => {
    if (!hydrated || (settings.tools.qrCodeSize === options.size && settings.tools.qrErrorCorrection === options.errorCorrection)) return
    const timer = window.setTimeout(() => void save((current) => ({ ...current, tools: { ...current.tools, qrCodeSize: options.size, qrErrorCorrection: options.errorCorrection } })).catch(fail), 500)
    return () => window.clearTimeout(timer)
  }, [hydrated, options.errorCorrection, options.size, save, settings.tools.qrCodeSize, settings.tools.qrErrorCorrection])

  async function renderQr(record = false): Promise<void> {
    try {
      const [output, pngOutput] = await Promise.all([
        generateQrSvg(source, options),
        generateQrPngDataUrl(source, options, logoDataUrl)
      ])
      setSvg(output)
      setPng(pngOutput)
      succeed('notice.generated')
      if (record) recordOperation(t('action.refresh'), `${source.length} · ${options.size}px`, 'success', {
        inputText: source, outputText: output, metadata: { operation: 'generate', options }
      })
    } catch (cause) {
      setSvg('')
      setPng('')
      fail(cause)
    }
  }

  async function decode(file?: File): Promise<void> {
    if (!file) return
    try {
      const value = await decodeQrImage(file)
      setDecoded(value)
      succeed('notice.decoded')
      recordOperation(t('scan.title'), `${file.name} · ${value.length}`, 'success', {
        outputText: value, metadata: { operation: 'decode', fileName: file.name }
      })
    } catch (cause) {
      fail(cause)
    }
  }

  async function download(): Promise<void> {
    if (!svg || !png) return
    try {
      const path = outputFormat === 'svg'
        ? await userFilesApi.exportText('mootool-qrcode.svg', svg)
        : await userFilesApi.exportDataUrl('mootool-qrcode.png', png)
      if (path) { succeed('notice.exported', { path }); recordOperation(t('action.export'), path, 'success') }
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

      <section className="qrcode-grid">
        <section className="utility-editor-card qrcode-compose">
          <header><span>{t('compose.title')}</span><QrCode /></header>
          <CodeEditor
            ariaLabel={t('compose.content')}
            value={source}
            onChange={setSource}
            className="utility-code-editor"
            lineWrapping
          />
          <div className="qrcode-options">
            <label>{t('option.correction')}
              <select
                value={options.errorCorrection}
                onChange={(event) => patchOptions({ errorCorrection: event.target.value as QrErrorCorrection })}
              >
                <option value="L">L · {t('option.approx', { percent: 7 })}</option>
                <option value="M">M · {t('option.approx', { percent: 15 })}</option>
                <option value="Q">Q · {t('option.approx', { percent: 25 })}</option>
                <option value="H">H · {t('option.approx', { percent: 30 })}</option>
              </select>
            </label>
            <label>{t('option.size')}
              <input
                type="number"
                min={160}
                max={2048}
                value={options.size}
                onChange={(event) => patchOptions({ size: Number(event.target.value) })}
              />
            </label>
            <label>{t('option.margin')}
              <input
                type="number"
                min={0}
                max={16}
                value={options.margin}
                onChange={(event) => patchOptions({ margin: Number(event.target.value) })}
              />
            </label>
            <label>{t('option.foreground')}
              <input type="color" value={options.dark} onChange={(event) => patchOptions({ dark: event.target.value })} />
            </label>
            <label>{t('option.background')}
              <input type="color" value={options.light} onChange={(event) => patchOptions({ light: event.target.value })} />
            </label>
            <label className="qrcode-logo-input">{t('option.logo')}
              <span className="secondary-button"><ImagePlus />{logoName || t('action.chooseLogo')}<input type="file" accept="image/*" onChange={(event) => { void chooseLogo(event.target.files?.[0]); event.target.value = '' }} /></span>
            </label>
            {logoDataUrl && <button className="secondary-button" type="button" onClick={() => { setLogoDataUrl(''); setLogoName('') }}>{t('action.removeLogo')}</button>}
          </div>
        </section>

        <section className="qrcode-preview-card">
          <div className="qrcode-preview">
            {svg
              ? <img src={svgDataUrl(svg)} alt={t('preview.alt')} />
              : <QrCode />}
          </div>
          <div className="qrcode-preview-actions">
            <button className="secondary-button" type="button" onClick={() => void renderQr(true)}>
              <RefreshCw />{t('action.refresh')}
            </button>
            <select aria-label={t('option.outputFormat')} value={outputFormat} onChange={(event) => setOutputFormat(event.target.value as 'svg' | 'png')}><option value="png">PNG</option><option value="svg">SVG</option></select>
            <button className="secondary-button" type="button" disabled={!png} onClick={() => void copyQrImage()}><Copy />{t('action.copyImage')}</button>
            <button className="primary-button" type="button" disabled={!svg || !png} onClick={() => void download()}>
              <Download />{t('action.exportFormat', { format: outputFormat.toUpperCase() })}
            </button>
          </div>
          <small>{t('preview.hint')}</small>
        </section>

        <section className="qrcode-scan-card">
          <header>
            <div><ScanLine /><strong>{t('scan.title')}</strong></div>
            <label className="secondary-button qrcode-upload">
              <ImageUp />{t('action.chooseImage')}
              <input
                type="file"
                accept="image/*"
                onChange={(event) => void decode(event.target.files?.[0])}
              />
            </label>
            <button className="secondary-button" type="button" onClick={() => void decodeClipboard()}><ClipboardPaste />{t('action.fromClipboard')}</button>
          </header>
          <div className="qrcode-decoded">
            <CodeEditor
              ariaLabel={t('scan.result')}
              value={decoded}
              onChange={setDecoded}
              className="utility-code-editor"
              lineWrapping
            />
            <button className="utility-copy" type="button" disabled={!decoded} onClick={() => void copyDecoded()}>
              {copied ? <Clipboard /> : <Copy />}{t(copied ? 'action.copied' : 'action.copy')}
            </button>
          </div>
        </section>
      </section>

      <footer className={failed ? 'utility-status utility-status--error' : 'utility-status'}>
        <span>{failed ? <TriangleAlert /> : <CheckCircle2 />}{noticeText}</span>
        <span>{t('footer.capabilities')}</span>
        <code>{session.summary}</code>
      </footer>
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
    </main>
  )
}
