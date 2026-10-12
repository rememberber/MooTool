import { useEffect, useRef, useState } from 'react'
import { ClipboardCopy, Download, ScanText } from 'lucide-react'
import { Dialog } from '@/shared/components/Dialog'
import { ocrErrorCodes, type OcrLanguage, type OcrResult, type OcrSource, type OcrStatus } from '@/shared/contracts/ocr'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function ImageOcrDialog({ source, status, onClose }: { source: OcrSource; status: OcrStatus | null; onClose: () => void }) {
  const { t } = useI18n()
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
      if (id) void window.mootool.cancelImageTextRecognition(id).catch(() => {})
    }
  }, [])

  function failure(error: unknown): string {
    const code = ocrErrorCodes.find(code => error instanceof Error && error.message.includes(`image.ocr.error.${code}`)) ?? 'failed'
    return t(`image.ocr.error.${code}`)
  }

  async function recognize() {
    if (requestId.current || working || actionBusy || !status?.available) return
    const id = crypto.randomUUID()
    requestId.current = id
    setWorking(true); setError(''); setNotice(''); setResult(null); setText('')
    try {
      const value = await window.mootool.recognizeImageText({ requestId: id, language, source: { name: source.name, dataUrl: source.dataUrl } })
      if (active.current && requestId.current === id) {
        setResult(value); setText(value.text)
        setNotice(value.lines.length ? t('image.ocr.complete') : t('image.ocr.empty'))
        requestAnimationFrame(() => editor.current?.focus())
      }
    } catch (error) { if (active.current && requestId.current === id) setError(failure(error)) }
    finally { if (active.current && requestId.current === id) { requestId.current = null; setWorking(false) } }
  }

  async function cancel() {
    const id = requestId.current
    if (!id || cancelling) return
    requestId.current = null
    setCancelling(true)
    try { await window.mootool.cancelImageTextRecognition(id) }
    catch (error) { if (active.current) setError(failure(error)) }
    finally { if (active.current) { setWorking(false); setCancelling(false); setNotice(t('image.ocr.cancelled')) } }
  }

  async function copy() {
    if (working || actionBusy || !text.trim()) return
    setActionBusy(true); setError(''); setNotice('')
    try {
      await navigator.clipboard.writeText(text)
      if (active.current) setNotice(t('image.ocr.copied'))
    } catch { if (active.current) setError(t('app.command.copyFailed')) }
    finally { if (active.current) setActionBusy(false) }
  }

  async function save() {
    if (working || actionBusy || !text.trim()) return
    setActionBusy(true); setError(''); setNotice('')
    try {
      const path = await window.mootool.saveTextFile({ kind: 'text', defaultName: 'recognized-text.txt', content: text })
      if (active.current && path) setNotice(t('app.command.saved'))
    } catch { if (active.current) setError(t('app.command.saveFailed')) }
    finally { if (active.current) setActionBusy(false) }
  }

  const languages = status?.languages.length ? status.languages : ['auto'] as const
  const languageLabel = (language: OcrLanguage) => t(status?.engine === 'pp-ocrv6-small' && language === 'auto' ? 'image.ocr.language.multilingual' : `image.ocr.language.${language}`)
  const unavailable = status && !status.available ? t(`image.ocr.unavailable.${status.reason ?? 'component'}`) : ''
  return <Dialog title={t('image.ocr.title')} open onClose={onClose} width={840} footer={<>
    <button className="dialog-button" type="button" onClick={onClose}>{t('common.close')}</button>
    <button className="dialog-button" type="button" disabled={working || actionBusy || !text.trim()} onClick={() => void save()}><Download size={14} />{t('image.ocr.save')}</button>
    <button className="dialog-button" type="button" disabled={working || actionBusy || !text.trim()} onClick={() => void copy()}><ClipboardCopy size={14} />{t('image.ocr.copy')}</button>
    <button className="dialog-button dialog-button--primary" type="button" disabled={actionBusy || cancelling || !status?.available} onClick={() => { if (working) void cancel(); else void recognize() }}>
      <ScanText size={14} />{t(cancelling ? 'image.batch.cancelling' : working ? 'common.cancel' : result ? 'image.ocr.again' : 'image.ocr.start')}
    </button>
  </>}>
    <div className="image-ocr-options">
      <div className="image-ocr-controls">
        <label><span>{t('image.ocr.language')}</span><select aria-label={t('image.ocr.language')} value={language} disabled={working || actionBusy || !status?.available} onChange={event => setLanguage(event.target.value as OcrLanguage)}>
          {languages.map(language => <option key={language} value={language}>{languageLabel(language)}</option>)}
        </select></label>
        <span>{t('image.ocr.local')}</span>
      </div>
      <div className="image-ocr-workspace">
        <figure><div><img src={source.dataUrl} alt={source.name} /></div><figcaption title={source.name}>{source.name} · {source.width} × {source.height}</figcaption></figure>
        <label className="image-ocr-editor"><span>{t('image.ocr.result')}</span><textarea ref={editor} aria-label={t('image.ocr.result')} maxLength={100000} value={text} disabled={working || actionBusy} spellCheck={false} onChange={event => { setText(event.target.value); setNotice('') }} /></label>
      </div>
      {result && <p className="image-ocr-metadata">{t(result.engine === 'apple-vision' ? 'image.ocr.engine.vision' : 'image.ocr.engine.ppocr')} · {languageLabel(result.language)} · {t('image.ocr.metadata', { count: String(result.lines.length), time: String(result.durationMs) })}</p>}
      <p className="image-ocr-help">{t(text ? 'image.ocr.review' : 'image.ocr.intro')}</p>
      {unavailable && <p className="image-ocr-error" role="alert">{unavailable}</p>}
      {error && <p className="image-ocr-error" role="alert">{error}</p>}
      <p className="image-ocr-notice" role="status">{working ? t('image.ocr.working') : notice}</p>
    </div>
  </Dialog>
}
