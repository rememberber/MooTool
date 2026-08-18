import {
  CheckCircle2,
  Clipboard,
  Copy,
  Eraser,
  FileUp,
  Paintbrush,
  Sparkles,
  TriangleAlert
} from 'lucide-react'
import { useMemo, useRef, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { CodeEditor } from '../../shared/CodeEditor'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
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

  async function format(): Promise<void> {
    setBusy(true)
    setFailed(false)
    try {
      const output = await formatCode(source, type, indent)
      setSource(output)
      setNotice({ key: 'notice.formatted', values: { type: typeLabel(type) } })
    } catch (cause) {
      setFailed(true)
      setNotice(cause instanceof ReformatToolError
        ? { key: `error.${cause.code}` }
        : { raw: cause instanceof Error ? cause.message : String(cause) })
    } finally {
      setBusy(false)
    }
  }

  function changeType(next: ReformatType): void {
    setType(next)
    if (!source.trim() || Object.values(reformatSamples).includes(source)) {
      setSource(reformatSamples[next])
      setFileName('')
    }
    setNotice({ key: 'notice.ready', values: { type: typeLabel(next) } })
    setFailed(false)
  }

  async function importFile(file: File | undefined): Promise<void> {
    if (!file) return
    try {
      setSource(await file.text())
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
      await clipboardApi.writeText(source)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1200)
    } catch {
      setNotice({ key: 'notice.copyFailed' })
      setFailed(true)
    }
  }

  return (
    <main className="utility-workbench reformat-workbench">
      <header className="utility-header">
        <div>
          <span className="eyebrow">TAURI REFORMAT</span>
          <h1>{t('title')}</h1>
        </div>
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
        <button
          className="icon-button"
          type="button"
          aria-label={t('action.clear')}
          onClick={() => {
            setSource('')
            setFileName('')
            setNotice({ key: 'notice.cleared' })
          }}
        >
          <Eraser />
        </button>
      </section>

      <section className="utility-editor-card">
        <header>
          <span>{fileName || t('editor.pending')}</span>
          <code>{t('editor.stats', { lines: source.split('\n').length, length: source.length })}</code>
        </header>
        <CodeEditor
          key={type}
          ariaLabel={t('editor.label', { type: typeLabel(type) })}
          value={source}
          onChange={setSource}
          className="utility-code-editor"
        />
      </section>

      <footer className={failed ? 'utility-status utility-status--error' : 'utility-status'}>
        <span>{failed ? <TriangleAlert /> : <CheckCircle2 />}{'raw' in notice ? notice.raw : t(notice.key, notice.values)}</span>
        <span><Sparkles />{t('status.local')}</span>
        <code>{session.summary}</code>
      </footer>
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
    </main>
  )
}
