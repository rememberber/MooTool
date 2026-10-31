import { useEffect, useRef, useState } from 'react'
import { ClipboardCopy, Download, ScanText, X } from 'lucide-react'
import { useLocalizedMessages, type LocalizedMessageKey } from '../../app/localizedMessages'
import { ocrApi } from '../../platform/api/ocrApi'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { userFilesApi } from '../../platform/api/userFilesApi'
import { ocrErrorCodes, type OcrLanguage, type OcrResult, type OcrSource, type OcrStatus } from '../../platform/contracts/ocr'
import { imageMessages } from './imageMessages'

type ImageMessageKey = LocalizedMessageKey<typeof imageMessages>

export function ImageOcrDialog({ source, status, onClose }: { source: OcrSource; status: OcrStatus | null; onClose: () => void }) {
  const { t } = useLocalizedMessages(imageMessages)
  const [language, setLanguage] = useState<OcrLanguage>('auto')
  const [result, setResult] = useState<OcrResult | null>(null)
  const [text, setText] = useState('')
  const [working, setWorking] = useState(false)
  const [cancelling, setCancelling] = useState(false)
  const [actionBusy, setActionBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const active = useRef(true)
  const requestId = useRef<string | null>(null)
  const editor = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    active.current = true
    return () => {
      active.current = false
      const id = requestId.current
      requestId.current = null
      if (id) void ocrApi.cancel(id).catch(() => undefined)
    }
  }, [])

  function failure(cause: unknown): string {
    const message = cause instanceof Error ? cause.message : String(cause)
    const code = ocrErrorCodes.find((item) => message.includes(`image.ocr.error.${item}`)) ?? 'failed'
    return t(`ocr.error.${code}` as ImageMessageKey)
  }

  async function recognize(): Promise<void> {
    if (requestId.current || working || actionBusy || !status?.available) return
    const id = crypto.randomUUID()
    requestId.current = id
    setWorking(true)
    setError('')
    setNotice('')
    setResult(null)
    setText('')
    try {
      const value = await ocrApi.recognize({ requestId: id, language, source: { name: source.name, dataUrl: source.dataUrl } })
      if (active.current && requestId.current === id) {
        setResult(value)
        setText(value.text)
        setNotice(value.lines.length ? t('ocr.complete') : t('ocr.empty'))
        requestAnimationFrame(() => editor.current?.focus())
      }
    } catch (cause) {
      if (active.current && requestId.current === id) setError(failure(cause))
    } finally {
      if (active.current && requestId.current === id) {
        requestId.current = null
        setWorking(false)
      }
    }
  }

  async function cancel(): Promise<void> {
    const id = requestId.current
    if (!id || cancelling) return
    requestId.current = null
    setCancelling(true)
    try { await ocrApi.cancel(id) }
    catch (cause) { if (active.current) setError(failure(cause)) }
    finally { if (active.current) { setWorking(false); setCancelling(false); setNotice(t('ocr.cancelled')) } }
  }

  async function copy(): Promise<void> {
    if (working || actionBusy || !text.trim()) return
    setActionBusy(true)
    setError('')
    setNotice('')
    try {
      await clipboardApi.writeText(text)
      if (active.current) setNotice(t('ocr.copied'))
    } catch { if (active.current) setError(t('ocr.error.failed')) }
    finally { if (active.current) setActionBusy(false) }
  }

  async function save(): Promise<void> {
    if (working || actionBusy || !text.trim()) return
    setActionBusy(true)
    setError('')
    setNotice('')
    try {
      const path = await userFilesApi.exportText('recognized-text.txt', text)
      if (active.current && path) setNotice(t('ocr.saved'))
    } catch { if (active.current) setError(t('ocr.error.failed')) }
    finally { if (active.current) setActionBusy(false) }
  }

  const languages = status?.languages.length ? status.languages : ['auto' as const]
  const languageLabel = (value: OcrLanguage) => t(`ocr.language.${value}` as ImageMessageKey)
  const unavailable = status && !status.available ? t(`ocr.unavailable.${status.reason ?? 'component'}` as ImageMessageKey) : ''

  return (
    <div className="image-ocr-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget && !working) onClose() }}>
      <section className="image-ocr-dialog" role="dialog" aria-modal="true" aria-label={t('ocr.title')}>
        <header>
          <strong>{t('ocr.title')}</strong>
          <button type="button" aria-label={t('ocr.close')} disabled={working} onClick={onClose}><X /></button>
        </header>
        <div className="image-ocr-options">
          <div className="image-ocr-controls">
            <label>
              <span>{t('ocr.language')}</span>
              <select aria-label={t('ocr.language')} value={language} disabled={working || actionBusy || !status?.available} onChange={(event) => setLanguage(event.target.value as OcrLanguage)}>
                {languages.map((item) => <option key={item} value={item}>{languageLabel(item)}</option>)}
              </select>
            </label>
            <span>{t('ocr.local')}</span>
          </div>
          <div className="image-ocr-workspace">
            <figure>
              <div><img src={source.dataUrl} alt={source.name} /></div>
              <figcaption title={source.name}>{source.name} · {source.width} × {source.height}</figcaption>
            </figure>
            <label className="image-ocr-editor">
              <span>{t('ocr.result')}</span>
              <textarea ref={editor} aria-label={t('ocr.result')} maxLength={100000} value={text} disabled={working || actionBusy} spellCheck={false} onChange={(event) => { setText(event.target.value); setNotice('') }} />
            </label>
          </div>
          {result && <p className="image-ocr-metadata">{t('ocr.engine.vision')} · {languageLabel(result.language)} · {t('ocr.metadata', { count: result.lines.length, time: result.durationMs })}</p>}
          <p className="image-ocr-help">{t(text ? 'ocr.review' : 'ocr.intro')}</p>
          {unavailable && <p className="image-ocr-error" role="alert">{unavailable}</p>}
          {error && <p className="image-ocr-error" role="alert">{error}</p>}
          <p className="image-ocr-notice" role="status">{working ? t('ocr.working') : notice}</p>
        </div>
        <footer>
          <button type="button" onClick={onClose}>{t('ocr.close')}</button>
          <button type="button" disabled={working || actionBusy || !text.trim()} onClick={() => void save()}><Download />{t('ocr.save')}</button>
          <button type="button" disabled={working || actionBusy || !text.trim()} onClick={() => void copy()}><ClipboardCopy />{t('ocr.copy')}</button>
          <button className="primary-button" type="button" disabled={actionBusy || cancelling || !status?.available} onClick={() => { if (working) void cancel(); else void recognize() }}>
            <ScanText />{t(cancelling ? 'ocr.cancelling' : working ? 'ocr.cancel' : result ? 'ocr.again' : 'ocr.start')}
          </button>
        </footer>
      </section>
    </div>
  )
}
