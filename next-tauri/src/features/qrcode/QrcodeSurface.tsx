import {
  CheckCircle2,
  Clipboard,
  Copy,
  Download,
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
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import {
  decodeQrImage,
  defaultQrOptions,
  generateQrSvg,
  QrToolError,
  svgDataUrl,
  type QrErrorCorrection,
  type QrOptions
} from './qrTools'
import { qrcodeMessages } from './qrcodeMessages'

type QrcodeMessageKey = LocalizedMessageKey<typeof qrcodeMessages>
type Notice = { key: QrcodeMessageKey; values?: MessageValues } | { raw: string }

export function QrcodeSurface() {
  const { t } = useLocalizedMessages(qrcodeMessages)
  const [source, setSource] = useState('https://mootool.app/?product=next-tauri')
  const [options, setOptions] = useState<QrOptions>(defaultQrOptions)
  const [svg, setSvg] = useState('')
  const [decoded, setDecoded] = useState('')
  const [notice, setNotice] = useState<Notice>({ key: 'notice.ready' })
  const [failed, setFailed] = useState(false)
  const [copied, setCopied] = useState(false)
  const noticeText = 'raw' in notice ? notice.raw : t(notice.key, notice.values)
  const session = useMemo(() => ({
    digest: JSON.stringify({
      sourceHash: contentFingerprint(source),
      decodedHash: contentFingerprint(decoded),
      options,
      generated: Boolean(svg)
    }),
    summary: t('session.summary', {
      generate: t(svg ? 'session.done' : 'session.waitGenerate'),
      decode: t(decoded ? 'session.done' : 'session.waitDecode')
    })
  }), [decoded, options, source, svg, t])
  const { sessionId, reportError } = useToolSessionReport('qrcode', session.digest, session.summary)
  const recordOperation = useOperationHistory('qrcode')

  useEffect(() => {
    const handle = window.setTimeout(() => void renderQr(), 160)
    return () => window.clearTimeout(handle)
    // renderQr only uses state values listed below.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [source, options])

  async function renderQr(record = false): Promise<void> {
    try {
      setSvg(await generateQrSvg(source, options))
      succeed('notice.generated')
      if (record) recordOperation(t('action.refresh'), `${source.length} · ${options.size}px`, 'success')
    } catch (cause) {
      setSvg('')
      fail(cause)
    }
  }

  async function decode(file?: File): Promise<void> {
    if (!file) return
    try {
      const value = await decodeQrImage(file)
      setDecoded(value)
      succeed('notice.decoded')
      recordOperation(t('scan.title'), `${file.name} · ${value.length}`, 'success')
    } catch (cause) {
      fail(cause)
    }
  }

  async function download(): Promise<void> {
    if (!svg) return
    try {
      const path = await userFilesApi.exportText('mootool-qrcode.svg', svg)
      if (path) { succeed('notice.exported', { path }); recordOperation(t('action.export'), path, 'success') }
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
        <div>
          <span className="eyebrow">TAURI QR CODE STUDIO</span>
          <h1>{t('title')}</h1>
        </div>
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
            <button className="primary-button" type="button" disabled={!svg} onClick={() => void download()}>
              <Download />{t('action.export')}
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
