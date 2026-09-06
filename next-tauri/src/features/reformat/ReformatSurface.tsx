import {
  CheckCircle2,
  Clipboard,
  Copy,
  Eraser,
  FileDown,
  FileUp,
  Paintbrush,
  Sparkles,
  TriangleAlert
} from 'lucide-react'
import { useMemo, useRef, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { userFilesApi } from '../../platform/api/userFilesApi'
import { CodeEditor } from '../../shared/CodeEditor'
import { ResizableColumns } from '../../shared/ResizableColumns'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import { parseOperationMetadata, useOperationRestore } from '../history/operationRestore'
import {
  formatCode,
  reformatSamples,
  reformatTypes,
  ReformatToolError,
  type ReformatType
} from './reformatTools'
import { reformatMessages } from './reformatMessages'

type ReformatMessageKey = LocalizedMessageKey<typeof reformatMessages>
type ReformatNotice = { key: ReformatMessageKey; values?: MessageValues } | { raw: string }

function typeLabel(type: ReformatType): string {
  return type === 'nginx' ? 'Nginx' : type.toUpperCase()
}

export function ReformatSurface() {
  const { t } = useLocalizedMessages(reformatMessages)
  const fileInput = useRef<HTMLInputElement>(null)
  const [type, setType] = useState<ReformatType>('nginx')
  const [indent, setIndent] = useState(4)
  const [source, setSource] = useState(reformatSamples.nginx)
  const [result, setResult] = useState('')
  const [fileName, setFileName] = useState('')
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState<ReformatNotice>({ key: 'notice.ready', values: { type: 'Nginx' } })
  const [failed, setFailed] = useState(false)
  const [copied, setCopied] = useState(false)
  const session = useMemo(() => ({
    digest: JSON.stringify({
      type,
      indent,
      length: source.length,
      hash: contentFingerprint(source),
      fileName
    }),
    summary: t('session.summary', { type: typeLabel(type), length: source.length, indent })
  }), [fileName, indent, source, t, type])
  const { sessionId, reportError } = useToolSessionReport(
    'reformat',
    session.digest,
    session.summary
  )
  const recordOperation = useOperationHistory('reformat')

  useOperationRestore('reformat', (entry) => {
    const metadata = parseOperationMetadata(entry)
    if (typeof metadata.type === 'string' && reformatTypes.includes(metadata.type as ReformatType)) setType(metadata.type as ReformatType)
    if (typeof metadata.indent === 'number') setIndent(metadata.indent)
    setFileName(typeof metadata.fileName === 'string' ? metadata.fileName : '')
    setSource(entry.inputText)
    setResult(entry.outputText)
    setFailed(false)
  })

  async function format(): Promise<void> {
    setBusy(true)
    setFailed(false)
    try {
      const output = await formatCode(source, type, indent)
      setResult(output)
      setNotice({ key: 'notice.formatted', values: { type: typeLabel(type) } })
      recordOperation(t('action.format'), `${typeLabel(type)} · ${output.length}`, 'success', {
        inputText: source, outputText: output, metadata: { type, indent, fileName }
      })
    } catch (cause) {
      setFailed(true)
      setNotice(cause instanceof ReformatToolError
        ? { key: `error.${cause.code}` }
        : { raw: cause instanceof Error ? cause.message : String(cause) })
      recordOperation(t('action.format'), `${typeLabel(type)} · ${cause instanceof Error ? cause.message : String(cause)}`, 'error', {
        inputText: source, metadata: { type, indent, fileName }
      })
    } finally {
      setBusy(false)
    }
  }

  function changeType(next: ReformatType): void {
    setType(next)
    if (!source.trim() || Object.values(reformatSamples).includes(source)) {
      setSource(reformatSamples[next])
      setResult('')
      setFileName('')
    }
    setNotice({ key: 'notice.ready', values: { type: typeLabel(next) } })
    setFailed(false)
  }

  async function importFile(file: File | undefined): Promise<void> {
    if (!file) return
    try {
      setSource(await file.text())
      setResult('')
      setFileName(file.name)
      setNotice({ key: 'notice.loaded', values: { file: file.name } })
      setFailed(false)
    } catch (cause) {
      setNotice({ raw: cause instanceof Error ? cause.message : String(cause) })
      setFailed(true)
    }
  }

  async function copy(): Promise<void> {
    try {
      await clipboardApi.writeText(result || source)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1200)
    } catch {
      setNotice({ key: 'notice.copyFailed' })
      setFailed(true)
    }
  }

  async function exportResult(): Promise<void> {
    if (!result) return
    const extension = type === 'nginx' ? 'conf' : type === 'html' ? 'html' : type
    const baseName = fileName ? fileName.replace(/\.[^.]+$/, '') : 'formatted'
    try {
      const path = await userFilesApi.exportText(`${baseName}.${extension}`, result)
      if (path) {
        setNotice({ key: 'notice.exported', values: { path } })
        setFailed(false)
      }
    } catch (cause) {
      setNotice({ raw: cause instanceof Error ? cause.message : String(cause) })
      setFailed(true)
    }
  }

  return (
    <main className="utility-workbench reformat-workbench">
      <header className="utility-header">
        <h1 className="visually-hidden">{t('title')}</h1>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>

      <section className="utility-toolbar" aria-label={t('toolbar.label')}>
        <div className="utility-segments" role="tablist" aria-label={t('type.label')}>
          {reformatTypes.map((item) => (
            <button
              className={type === item ? 'utility-segment utility-segment--active' : 'utility-segment'}
              type="button"
              role="tab"
              aria-selected={type === item}
              key={item}
              onClick={() => changeType(item)}
            >
              {typeLabel(item)}
            </button>
          ))}
        </div>
        <label className="utility-select">
          {t('indent.label')}
          <select value={indent} onChange={(event) => setIndent(Number(event.target.value))}>
            {[2, 3, 4, 5, 6, 8].map((value) => <option value={value} key={value}>{value}</option>)}
          </select>
        </label>
        <button
          className="primary-button"
          type="button"
          disabled={busy || !source.trim()}
          onClick={() => void format()}
        >
          <Paintbrush />{t(busy ? 'action.formatting' : 'action.format')}
        </button>
        <button className="secondary-button" type="button" onClick={() => fileInput.current?.click()}>
          <FileUp />{t('action.import')}
        </button>
        <input
          ref={fileInput}
          className="visually-hidden"
          type="file"
          accept=".conf,.nginx,.java,.xml,.html,.htm,text/*"
          onChange={(event) => void importFile(event.target.files?.[0])}
        />
        <button className="secondary-button" type="button" onClick={() => void copy()}>
          {copied ? <Clipboard /> : <Copy />}{t(copied ? 'action.copied' : 'action.copy')}
        </button>
        <button className="secondary-button" type="button" disabled={!result} onClick={() => void exportResult()}><FileDown />{t('action.export')}</button>
        <button
          className="icon-button"
          type="button"
          aria-label={t('action.clear')}
          onClick={() => {
            setSource('')
            setResult('')
            setFileName('')
            setNotice({ key: 'notice.cleared' })
          }}
        >
          <Eraser />
        </button>
      </section>

      <ResizableColumns id="reformat-result" className="reformat-editor-grid" initialPrimary={520} minPrimary={280} minSecondary={280}>
        <section className="utility-editor-card">
          <header><span>{t('editor.original')} · {fileName || t('editor.pending')}</span><code>{t('editor.stats', { lines: source.split('\n').length, length: source.length })}</code></header>
          <CodeEditor key={`${type}-source`} ariaLabel={t('editor.original')} value={source} onChange={(value) => { setSource(value); setResult('') }} className="utility-code-editor" />
        </section>
        <section className="utility-editor-card">
          <header><span>{t('editor.result')}</span><code>{t('editor.stats', { lines: result ? result.split('\n').length : 0, length: result.length })}</code></header>
          <CodeEditor key={`${type}-result`} ariaLabel={t('editor.result')} value={result} readOnly className="utility-code-editor" />
        </section>
      </ResizableColumns>

      <footer className={failed ? 'utility-status utility-status--error' : 'utility-status'}>
        <span>{failed ? <TriangleAlert /> : <CheckCircle2 />}{'raw' in notice ? notice.raw : t(notice.key, notice.values)}</span>
        <span><Sparkles />{t('status.local')}</span>
        <code>{session.summary}</code>
      </footer>
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
    </main>
  )
}
