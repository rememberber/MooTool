import {
  CheckCircle2,
  CircleAlert,
  Eraser,
  FolderOpen,
  Play,
  RefreshCw,
  Square,
  TriangleAlert,
  WandSparkles
} from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { codeRuntimeApi } from '../../platform/api/codeRuntimeApi'
import type { CodeRunResult, CodeRuntimeId, CodeRuntimeStatus } from '../../platform/contracts/codeRuntime'
import type { RuntimeRunOption } from '../../platform/contracts/settings'
import { CodeEditor } from '../../shared/CodeEditor'
import { ResizableColumns } from '../../shared/ResizableColumns'
import { contentFingerprint } from '../../shared/fingerprint'
import { parseOperationMetadata, useOperationRestore } from '../history/operationRestore'
import { useOperationHistory } from '../history/useOperationHistory'
import { useSettings } from '../settings/SettingsProvider'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { runtimeMessages } from './runtimeMessages'
import { formatRuntimeSource, parseRuntimeArguments, RuntimeToolError } from './runtimeTools'

type RuntimeMessageKey = LocalizedMessageKey<typeof runtimeMessages>
type RuntimeNotice = { key: RuntimeMessageKey; values?: MessageValues } | { raw: string }
type RuntimeOptions = Record<CodeRuntimeId, RuntimeRunOption>

const runtimeIds: CodeRuntimeId[] = ['java', 'groovy', 'python', 'node']
const samples: Record<CodeRuntimeId, string> = {
  java: 'public class Main {\n  public static void main(String[] args) {\n    System.out.println("Hello from MooTool Tauri");\n  }\n}',
  groovy: `['Java', 'Python', 'Node.js'].eachWithIndex { item, i -> println "\${i + 1}. \${item}" }`,
  python: 'tools = ["Java", "Python", "Node.js"]\nfor index, tool in enumerate(tools, 1):\n    print(f"{index}. {tool}")',
  node: "const tools = ['Java', 'Python', 'Node.js']\ntools.forEach((tool, index) => console.log(`\${index + 1}. \${tool}`))"
}

export function RuntimeSurface() {
  const { t } = useLocalizedMessages(runtimeMessages)
  const { settings, ready, save } = useSettings()
  const [runtime, setRuntime] = useState<CodeRuntimeId>('node')
  const [codes, setCodes] = useState<Record<CodeRuntimeId, string>>(() => resolveDrafts(settings.runtime.drafts))
  const [options, setOptions] = useState<RuntimeOptions>(settings.runtime.options)
  const [timeoutSeconds, setTimeoutSeconds] = useState(settings.runtime.timeoutSeconds)
  const [hydrated, setHydrated] = useState(false)
  const [statuses, setStatuses] = useState<CodeRuntimeStatus[]>([])
  const [stdout, setStdout] = useState('')
  const [stderr, setStderr] = useState('')
  const [running, setRunning] = useState(false)
  const [requestId, setRequestId] = useState('')
  const [result, setResult] = useState<CodeRunResult>()
  const [notice, setNotice] = useState<RuntimeNotice>({ key: 'notice.detecting' })
  const [failed, setFailed] = useState(false)
  const stdoutRef = useRef('')
  const stderrRef = useRef('')
  const code = codes[runtime]
  const option = options[runtime]
  const status = statuses.find((item) => item.id === runtime)
  const noticeText = 'raw' in notice ? notice.raw : t(notice.key, notice.values)

  const session = useMemo(() => ({
    digest: JSON.stringify({ runtime, codeHash: contentFingerprint(code), running, exitCode: result?.exitCode }),
    summary: `${runtimeName(runtime)} · ${running
      ? t('session.running')
      : result
        ? t('session.exit', { code: result.exitCode ?? '-' })
        : status?.available
          ? t('session.available')
          : t('session.missing')}`
  }), [code, result, running, runtime, status, t])
  const { sessionId, reportError } = useToolSessionReport('runtime', session.digest, session.summary)
  const recordOperation = useOperationHistory('runtime')

  useEffect(() => {
    if (!ready || hydrated) return
    setCodes(resolveDrafts(settings.runtime.drafts))
    setOptions(settings.runtime.options)
    setTimeoutSeconds(settings.runtime.timeoutSeconds)
    setHydrated(true)
  }, [hydrated, ready, settings.runtime])

  useEffect(() => {
    if (!hydrated) return
    const unchanged = runtimeIds.every((id) => (
      settings.runtime.drafts[id] === codes[id]
      && settings.runtime.options[id].argumentsText === options[id].argumentsText
      && settings.runtime.options[id].workingDirectory === options[id].workingDirectory
    )) && settings.runtime.timeoutSeconds === timeoutSeconds
    if (unchanged) return
    const timer = window.setTimeout(() => {
      void save((current) => ({
        ...current,
        runtime: {
          ...current.runtime,
          drafts: codes,
          options,
          timeoutSeconds
        }
      })).catch(fail)
    }, 500)
    return () => window.clearTimeout(timer)
  }, [codes, hydrated, options, save, settings.runtime, timeoutSeconds])

  useOperationRestore('runtime', (entry) => {
    const metadata = parseOperationMetadata(entry)
    const nextRuntime = metadata.runtime
    if (!isRuntimeId(nextRuntime)) return
    setRuntime(nextRuntime)
    setCodes((current) => ({ ...current, [nextRuntime]: entry.inputText }))
    setOptions((current) => ({
      ...current,
      [nextRuntime]: {
        argumentsText: typeof metadata.argumentsText === 'string' ? metadata.argumentsText : '',
        workingDirectory: typeof metadata.workingDirectory === 'string' ? metadata.workingDirectory : ''
      }
    }))
    const [restoredStdout = '', restoredStderr = ''] = entry.outputText.split('\n\u0000stderr\u0000\n')
    setStdout(restoredStdout)
    setStderr(restoredStderr)
    setFailed(false)
  })

  useEffect(() => { void detect() }, [])
  useEffect(() => {
    const listener = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key === 'Enter' && !running) {
        event.preventDefault()
        void run()
      }
    }
    addEventListener('keydown', listener)
    return () => removeEventListener('keydown', listener)
  })

  async function detect() {
    try {
      const loaded = await codeRuntimeApi.detect()
      setStatuses(loaded)
      setNotice({ key: 'notice.detected', values: { count: loaded.filter((item) => item.available).length } })
      setFailed(false)
    } catch (cause) { fail(cause) }
  }

  async function run() {
    if (running) return
    if (status && !status.available) {
      setNotice({ key: 'error.runtimeMissing', values: { runtime: runtimeName(runtime) } })
      setFailed(true)
      return
    }
    let args: string[]
    try { args = parseRuntimeArguments(option.argumentsText) } catch (cause) { fail(cause); return }
    const id = crypto.randomUUID()
    setRequestId(id)
    setRunning(true)
    setResult(undefined)
    stdoutRef.current = ''
    stderrRef.current = ''
    setStdout('')
    setStderr('')
    setFailed(false)
    try {
      const completed = await codeRuntimeApi.run({
        requestId: id,
        runtime,
        code,
        timeoutMs: timeoutSeconds * 1_000,
        arguments: args,
        workingDirectory: option.workingDirectory
      }, (event) => {
        if (event.stream === 'stdout') {
          stdoutRef.current += event.text
          setStdout(stdoutRef.current)
        } else {
          stderrRef.current += event.text
          setStderr(stderrRef.current)
        }
      })
      setResult(completed)
      setStdout(completed.stdout)
      setStderr(completed.stderr)
      setNotice(completed.timedOut
        ? { key: 'notice.timeout' }
        : completed.cancelled
          ? { key: 'notice.cancelled' }
          : { key: 'notice.finished', values: { code: completed.exitCode ?? '-' } })
      setFailed(completed.exitCode !== 0)
      recordOperation(t('operation.run'), `${runtimeName(runtime)} · exit ${completed.exitCode ?? '-'} · ${completed.durationMs} ms`, completed.exitCode === 0 ? 'success' : 'error', {
        inputText: code,
        outputText: `${completed.stdout}\n\u0000stderr\u0000\n${completed.stderr}`,
        metadata: { runtime, ...option, exitCode: completed.exitCode, durationMs: completed.durationMs }
      })
    } catch (cause) {
      fail(cause)
      recordOperation(t('operation.run'), `${runtimeName(runtime)} · ${runtimeErrorText(cause, t)}`, 'error', {
        inputText: code,
        metadata: { runtime, ...option }
      })
    } finally {
      setRunning(false)
      setRequestId('')
    }
  }

  async function stop() {
    if (!requestId) return
    await codeRuntimeApi.cancel(requestId)
    setNotice({ key: 'notice.stopping' })
  }

  async function formatSource() {
    try {
      const formatted = await formatRuntimeSource(code, runtime)
      setCodes((current) => ({ ...current, [runtime]: formatted }))
      setNotice({ key: 'notice.formatted' })
      setFailed(false)
    } catch (cause) { fail(cause) }
  }

  async function chooseWorkingDirectory() {
    if (!window.__TAURI_INTERNALS__) return
    try {
      const { open } = await import('@tauri-apps/plugin-dialog')
      const selection = await open({
        directory: true,
        multiple: false,
        defaultPath: option.workingDirectory || undefined,
        title: t('action.chooseDirectory')
      })
      const path = Array.isArray(selection) ? selection[0] : selection
      if (path) patchOption({ workingDirectory: path })
    } catch (cause) { fail(cause) }
  }

  function patchOption(patch: Partial<RuntimeRunOption>) {
    setOptions((current) => ({ ...current, [runtime]: { ...current[runtime], ...patch } }))
  }

  function fail(cause: unknown) {
    setNotice(cause instanceof RuntimeToolError
      ? { key: `error.${cause.code}` }
      : { raw: cause instanceof Error ? cause.message : String(cause) })
    setFailed(true)
  }

  return (
    <main className="utility-workbench code-runtime-workbench">
      <header className="utility-header">
        <h1 className="visually-hidden">{t('title')}</h1>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>
      <section className="code-runtime-toolbar">
        <div className="utility-segments">
          {runtimeIds.map((item) => (
            <button className={runtime === item ? 'utility-segment utility-segment--active' : 'utility-segment'} type="button" disabled={running} key={item} onClick={() => setRuntime(item)}>
              {runtimeName(item)}<i className={statuses.find((entry) => entry.id === item)?.available ? 'runtime-dot runtime-dot--ok' : 'runtime-dot'} />
            </button>
          ))}
        </div>
        <label>{t('field.arguments')}<input value={option.argumentsText} placeholder="--name 'Moo Tool'" onChange={(event) => patchOption({ argumentsText: event.target.value })} /></label>
        <label className="runtime-working-directory">{t('field.workingDirectory')}<input value={option.workingDirectory} placeholder={t('placeholder.workingDirectory')} onChange={(event) => patchOption({ workingDirectory: event.target.value })} /><button type="button" aria-label={t('action.chooseDirectory')} title={t('action.chooseDirectory')} onClick={() => void chooseWorkingDirectory()}><FolderOpen /></button></label>
        <label className="runtime-timeout">{t('field.timeout')}<input type="number" min="1" max="300" value={timeoutSeconds} onChange={(event) => setTimeoutSeconds(Math.min(300, Math.max(1, event.target.valueAsNumber || 30)))} /><span>s</span></label>
        <button className="secondary-button" type="button" disabled={running} onClick={() => void detect()}><RefreshCw />{t('action.detect')}</button>
        {running
          ? <button className="primary-button runtime-stop" type="button" onClick={() => void stop()}><Square />{t('action.stop')}</button>
          : <button className="primary-button" type="button" onClick={() => void run()}><Play />{t('action.run')}</button>}
      </section>
      {status && <section className={status.available ? 'runtime-status-line runtime-status-line--ok' : 'runtime-status-line'}>{status.available ? <CheckCircle2 /> : <CircleAlert />}<strong>{runtimeName(runtime)}</strong><code>{status.command}</code><span>{status.version || t('status.notInstalled')}</span></section>}
      <ResizableColumns id="runtime-source" className="code-runtime-layout" initialPrimary={520} minPrimary={320} minSecondary={300}>
        <section className="utility-editor-card">
          <header>
            <span>{t('pane.source', { runtime: runtimeName(runtime) })}</span>
            <div>
              <button className="utility-copy" type="button" onClick={() => void formatSource()}><WandSparkles />{t('action.format')}</button>
              <button className="utility-copy" type="button" onClick={() => setCodes((current) => ({ ...current, [runtime]: samples[runtime] }))}><Eraser />{t('action.restore')}</button>
            </div>
          </header>
          <CodeEditor ariaLabel={t('aria.source')} value={code} onChange={(value) => setCodes((current) => ({ ...current, [runtime]: value }))} className="utility-code-editor" lineWrapping={false} />
        </section>
        <section className="runtime-output-card">
          <header><strong>{t('pane.output')}</strong>{result && <span>{result.durationMs} ms · exit {result.exitCode ?? '-'}</span>}</header>
          <div>{stdout && <pre className="runtime-stdout">{stdout}</pre>}{stderr && <pre className="runtime-stderr">{stderr}</pre>}{!stdout && !stderr && <p>{t(running ? 'output.running' : 'output.empty')}</p>}</div>
          {result && <footer title={result.command}><code>{result.command}</code></footer>}
        </section>
      </ResizableColumns>
      <footer className={failed ? 'utility-status utility-status--error' : 'utility-status'}>
        <span>{failed ? <TriangleAlert /> : <CheckCircle2 />}{noticeText}</span>
        <span>{t('footer.capabilities', { seconds: timeoutSeconds })}</span>
        <code>{session.summary}</code>
      </footer>
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
    </main>
  )
}

function resolveDrafts(drafts: Record<CodeRuntimeId, string>): Record<CodeRuntimeId, string> {
  return Object.fromEntries(runtimeIds.map((id) => [id, drafts[id] || samples[id]])) as Record<CodeRuntimeId, string>
}

function isRuntimeId(value: unknown): value is CodeRuntimeId {
  return typeof value === 'string' && runtimeIds.includes(value as CodeRuntimeId)
}

function runtimeName(value: CodeRuntimeId) {
  return { java: 'Java', groovy: 'Groovy', python: 'Python', node: 'Node.js' }[value]
}

function runtimeErrorText(cause: unknown, t: (key: RuntimeMessageKey) => string): string {
  return cause instanceof RuntimeToolError ? t(`error.${cause.code}`) : cause instanceof Error ? cause.message : String(cause)
}
