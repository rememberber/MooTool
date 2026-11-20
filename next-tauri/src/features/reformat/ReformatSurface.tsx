import {
  CheckCircle2,
  Clipboard,
  Copy,
  Eraser,
  FileDown,
  FolderOpen,
  History,
  Paintbrush,
  Sparkles,
  TriangleAlert
} from 'lucide-react'
import { useMemo, useState, type KeyboardEvent } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { localDataApi } from '../../platform/api/localDataApi'
import { userFilesApi } from '../../platform/api/userFilesApi'
import { CodeEditor } from '../../shared/CodeEditor'
import { ResizableColumns } from '../../shared/ResizableColumns'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { FuncHistoryDialog } from '../history/FuncHistoryDialog'
import { useOperationHistory } from '../history/useOperationHistory'
import { parseOperationMetadata, useOperationRestore } from '../history/operationRestore'
import {
  formatCode,
  reformatSamples,
  reformatTypes,
  type ReformatType
} from './reformatTools'
import { reformatMessages } from './reformatMessages'

type ReformatMessageKey = LocalizedMessageKey<typeof reformatMessages>
type ReformatNotice = { key: ReformatMessageKey; values?: MessageValues } | { raw: string }
type ReformatTab = 'text' | 'file'

function typeLabel(type: ReformatType): string {
  return type === 'nginx' ? 'Nginx' : type.toUpperCase()
}

export function ReformatSurface() {
  const { t } = useLocalizedMessages(reformatMessages)
  const [tab, setTab] = useState<ReformatTab>('text')
  const [type, setType] = useState<ReformatType>('nginx')
  const [indent, setIndent] = useState(4)
  const [text, setText] = useState(reformatSamples.nginx)
  const [fileName, setFileName] = useState('')
  const [fileSource, setFileSource] = useState('')
  const [fileResult, setFileResult] = useState('')
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState<ReformatNotice>({ key: 'notice.ready', values: { type: 'Nginx' } })
  const [failed, setFailed] = useState(false)
  const [copied, setCopied] = useState(false)
  const [historyOpen, setHistoryOpen] = useState(false)
  const active = tab === 'text' ? text : fileSource
  const session = useMemo(() => ({
    digest: JSON.stringify({
      tab,
      type,
      indent,
      length: active.length,
      hash: contentFingerprint(active),
      fileName
    }),
    summary: t('session.summary', { type: typeLabel(type), length: active.length, indent })
  }), [active, fileName, indent, t, tab, type])
  const { sessionId, reportError } = useToolSessionReport('reformat', session.digest, session.summary)
  const recordOperation = useOperationHistory('reformat')

  useOperationRestore('reformat', (entry) => {
    const metadata = parseOperationMetadata(entry)
    if (typeof metadata.type === 'string' && reformatTypes.includes(metadata.type as ReformatType)) setType(metadata.type as ReformatType)
    if (typeof metadata.indent === 'number') setIndent(metadata.indent)
    if (metadata.mode === 'file') {
      setTab('file')
      setFileName(typeof metadata.fileName === 'string' ? metadata.fileName : '')
      setFileSource(entry.inputText)
      setFileResult(entry.outputText)
    } else {
      setTab('text')
      setText(entry.outputText || entry.inputText)
      setFileName('')
      setFileSource('')
      setFileResult('')
    }
    setFailed(false)
  })

  async function format(mode: ReformatTab): Promise<void> {
    const source = mode === 'text' ? text : fileSource
    setBusy(true)
    setFailed(false)
    try {
      const output = await formatCode(source, type, indent)
      if (mode === 'text') setText(output)
      else setFileResult(output)
      setNotice({ key: 'notice.formatted' })
      recordOperation(t('action.format'), `${typeLabel(type)} · ${output.length}`, 'success', {
        inputText: source, outputText: output, metadata: { type, indent, fileName: mode === 'file' ? fileName : '', mode }
      })
      void localDataApi.saveFuncHistory({
        funcType: 'reformat',
        summary: t('history.summary', { type: type.toUpperCase() }),
        inputText: source,
        outputText: output,
        extraData: mode === 'file' ? `${type}|file|${indent}|${fileName}` : `${type}|text|${indent}`
      })
    } catch (cause) {
      setFailed(true)
      setNotice({ raw: cause instanceof Error ? cause.message : String(cause) })
      recordOperation(t('action.format'), `${typeLabel(type)} · ${cause instanceof Error ? cause.message : String(cause)}`, 'error', {
        inputText: source, metadata: { type, indent, fileName: mode === 'file' ? fileName : '', mode }
      })
    } finally {
      setBusy(false)
    }
  }

  function changeType(next: ReformatType): void {
    setType(next)
    if (tab === 'text' && (!text.trim() || Object.values(reformatSamples).includes(text))) setText(reformatSamples[next])
  }

  async function chooseFile(): Promise<void> {
    try {
      const file = await userFilesApi.pickText('source')
      if (!file) return
      setFileName(file.name)
      setFileSource(file.content)
      setFileResult('')
      setNotice({ key: 'notice.loaded', values: { file: file.name } })
      setFailed(false)
    } catch (cause) {
      setNotice({ raw: cause instanceof Error ? cause.message : String(cause) })
      setFailed(true)
    }
  }

  async function copy(): Promise<void> {
    try {
      await clipboardApi.writeText(tab === 'text' ? text : fileResult)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1200)
    } catch {
      setNotice({ key: 'notice.copyFailed' })
      setFailed(true)
    }
  }

  async function exportResult(): Promise<void> {
    const content = tab === 'text' ? text : fileResult
    if (!content) return
    const extension = type === 'nginx' ? 'conf' : type
    const baseName = fileName ? fileName.replace(/\.[^.]+$/, '') : 'formatted'
    try {
      const path = await userFilesApi.exportText(`${baseName}.${extension}`, content)
      if (path) {
        setNotice({ key: 'notice.exported', values: { path } })
        setFailed(false)
      }
    } catch (cause) {
      setNotice({ raw: cause instanceof Error ? cause.message : String(cause) })
      setFailed(true)
    }
  }

  function formatShortcut(event: KeyboardEvent): void {
    if ((event.metaKey || event.ctrlKey) && event.shiftKey && event.key.toLowerCase() === 'f') {
      event.preventDefault()
      void format('text')
    }
  }

  return (
    <main className="utility-workbench reformat-workbench">
      <header className="utility-header">
        <h1 className="visually-hidden">{t('title')}</h1>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>

      <section className="utility-toolbar" aria-label={t('toolbar.label')}>
        <div className="utility-segments" role="tablist">
          {(['text', 'file'] as const).map((item) => (
            <button
              className={tab === item ? 'utility-segment utility-segment--active' : 'utility-segment'}
              type="button"
              role="tab"
              aria-selected={tab === item}
              key={item}
              onClick={() => setTab(item)}
            >
              {t(`tab.${item}`)}
            </button>
          ))}
        </div>
        <label className="utility-select">
          {t('type.label')}
          <select value={type} onChange={(event) => changeType(event.target.value as ReformatType)}>
            {reformatTypes.map((item) => <option value={item} key={item}>{typeLabel(item)}</option>)}
          </select>
        </label>
        <label className="utility-select">
          {t('indent.label')}
          <select value={indent} onChange={(event) => setIndent(Number(event.target.value))}>
            {[2, 3, 4, 5, 6].map((value) => <option value={value} key={value}>{value}</option>)}
          </select>
        </label>
        <button className="toolbar-button" type="button" onClick={() => setHistoryOpen(true)}><History size={14} />{t('action.history')}</button>
        <button
          className="primary-button"
          type="button"
          disabled={busy || !active.trim()}
          onClick={() => void format(tab)}
        >
          <Paintbrush />{t(busy ? 'action.formatting' : 'action.format')}
        </button>
        <button className="secondary-button" type="button" onClick={() => void copy()}>
          {copied ? <Clipboard /> : <Copy />}{t(copied ? 'action.copied' : 'action.copy')}
        </button>
        <button className="secondary-button" type="button" disabled={!(tab === 'text' ? text : fileResult)} onClick={() => void exportResult()}><FileDown />{t('action.export')}</button>
        <button
          className="icon-button"
          type="button"
          aria-label={t('action.clear')}
          onClick={() => {
            if (tab === 'text') setText('')
            else {
              setFileName('')
              setFileSource('')
              setFileResult('')
            }
            setNotice({ key: 'notice.cleared' })
          }}
        >
          <Eraser />
        </button>
      </section>

      {tab === 'text' ? (
        <section className="utility-editor-card" onKeyDown={formatShortcut}>
          <header><span>{t('editor.input')}</span><code>{t('editor.stats', { lines: text.split('\n').length, length: text.length })}</code></header>
          <CodeEditor key={`${type}-text`} ariaLabel={t('editor.input')} value={text} onChange={setText} className="utility-code-editor" />
        </section>
      ) : (
        <ResizableColumns id="reformat-file" legacyId="reformat-result" className="reformat-editor-grid" initialPrimary={520} defaultSizes={[1, 1]} minPrimary={260} minSecondary={260}>
          <section className="utility-editor-card">
            <header>
              <span>{t('editor.original')}</span>
              <button className="toolbar-button" type="button" onClick={() => void chooseFile()}><FolderOpen size={14} />{t('action.chooseFile')}</button>
              <code>{fileName || t('editor.noFile')}</code>
            </header>
            <CodeEditor key={`${type}-source`} ariaLabel={t('editor.original')} value={fileSource} onChange={setFileSource} className="utility-code-editor" />
          </section>
          <section className="utility-editor-card">
            <header><span>{t('editor.result')}</span><code>{t('editor.stats', { lines: fileResult ? fileResult.split('\n').length : 0, length: fileResult.length })}</code></header>
            <CodeEditor key={`${type}-result`} ariaLabel={t('editor.result')} value={fileResult} readOnly className="utility-code-editor" />
          </section>
        </ResizableColumns>
      )}

      <footer className={failed ? 'utility-status utility-status--error' : 'utility-status'}>
        <span>{failed ? <TriangleAlert /> : <CheckCircle2 />}{'raw' in notice ? notice.raw : t(notice.key, notice.values)}</span>
        <span><Sparkles />{t('status.local')}</span>
        <code>{session.summary}</code>
      </footer>
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
      {historyOpen && <FuncHistoryDialog funcType="reformat" onClose={() => setHistoryOpen(false)} onApply={(record) => {
        const [historyType, historyTab, historyIndent, historyName] = (record.extraData ?? '').split('|')
        if (reformatTypes.includes(historyType as ReformatType)) setType(historyType as ReformatType)
        setIndent(Number(historyIndent) || 4)
        if (historyTab === 'file') {
          setTab('file')
          setFileName(historyName ?? '')
          setFileSource(record.inputText)
          setFileResult(record.outputText)
        } else {
          setTab('text')
          setText(record.outputText || record.inputText)
        }
        setFailed(false)
        setHistoryOpen(false)
      }} />}
    </main>
  )
}
