import { CheckCircle2, CircleAlert, Eraser, FolderOpen, History, LoaderCircle, Play, Settings, SlidersHorizontal, Square, WandSparkles } from 'lucide-react'
import { useEffect, useMemo, useReducer, useRef } from 'react'
import { HistoryDialog } from '@/features/history/HistoryDialog'
import { useSettings } from '@/features/settings/SettingsProvider'
import { Dialog } from '@/shared/components/Dialog'
import { Tooltip } from '@/shared/components/Tooltip'
import type { RuntimeStatus } from '@/shared/contracts/app'
import type { CodeRuntimeId, RuntimeExecutionResult } from '@/shared/contracts/runtime'
import type { RuntimeRunOption } from '@/shared/contracts/settings'
import { useToast } from '@/shared/feedback/ToastProvider'
import { useI18n } from '@/shared/i18n/I18nProvider'
import type { MessageKey } from '@/shared/i18n/messages'
import { formatRuntimeSource, parseRuntimeArguments, runtimeDisplayName } from './runtimeTools'

type PrimaryRuntime = 'java' | 'python' | 'node'

type RuntimeUiState = {
  activeTab: PrimaryRuntime
  javaMode: 'java' | 'groovy'
  codes: Record<CodeRuntimeId, string>
  stdout: string
  stderr: string
  running: boolean
  requestId: string
  result: RuntimeExecutionResult | null
  statuses: RuntimeStatus[]
  detecting: boolean
  historyOpen: boolean
  optionsOpen: boolean
  runOptions: Record<CodeRuntimeId, RuntimeRunOption>
}

const samples: Record<CodeRuntimeId, string> = {
  java: `public class Main {
    public static void main(String[] args) {
        System.out.println("Hello from MooTool");
    }
}`,
  groovy: `def tools = ['Java', 'Python', 'Node.js']
tools.eachWithIndex { tool, index ->
    println "\${index + 1}. \${tool}"
}`,
  python: `tools = ["Java", "Python", "Node.js"]
for index, tool in enumerate(tools, start=1):
    print(f"{index}. {tool}")`,
  node: `const tools = ['Java', 'Python', 'Node.js']
tools.forEach((tool, index) => {
  console.log(\`\${index + 1}. \${tool}\`)
})`
}

const initialState: RuntimeUiState = {
  activeTab: 'java',
  javaMode: 'java',
  codes: samples,
  stdout: '',
  stderr: '',
  running: false,
  requestId: '',
  result: null,
  statuses: [],
  detecting: true,
  historyOpen: false,
  optionsOpen: false,
  runOptions: {
    java: { arguments: '', workingDirectory: '' },
    groovy: { arguments: '', workingDirectory: '' },
    python: { arguments: '', workingDirectory: '' },
    node: { arguments: '', workingDirectory: '' }
  }
}

function updateState(state: RuntimeUiState, patch: Partial<RuntimeUiState>): RuntimeUiState {
  return { ...state, ...patch }
}

export function RuntimeTool() {
  const { t } = useI18n()
  const { settings, updateSettings } = useSettings()
  const toast = useToast()
  const [state, update] = useReducer(updateState, settings.runtime, (runtimeSettings): RuntimeUiState => ({
    ...initialState,
    codes: Object.fromEntries((['java', 'groovy', 'python', 'node'] as CodeRuntimeId[]).map((id) => [id, runtimeSettings.drafts[id] || samples[id]])) as Record<CodeRuntimeId, string>,
    runOptions: runtimeSettings.options
  }))
  const requestIdRef = useRef('')
  const stdoutRef = useRef('')
  const stderrRef = useRef('')
  const lineNumbersRef = useRef<HTMLPreElement>(null)
  const runtime: CodeRuntimeId = state.activeTab === 'java' ? state.javaMode : state.activeTab
  const code = state.codes[runtime]
  const status = state.statuses.find((item) => item.id === runtime)
  const availableCount = state.statuses.filter((item) => item.available).length
  const statusText = state.detecting
    ? t('common.loading')
    : t('runtime.detected', { count: String(availableCount) })
  const outputState = useMemo(() => runtimeResultLabel(state.result, state.running, runtime, t), [runtime, state.result, state.running, t])
  const runOption = state.runOptions[runtime]

  useEffect(() => {
    void detectRuntimes()
  }, [])

  useEffect(() => {
    if (sameRuntimePersistence(settings.runtime.drafts, state.codes) && sameRuntimePersistence(settings.runtime.options, state.runOptions)) return
    const timer = window.setTimeout(() => {
      void updateSettings({ runtime: { drafts: state.codes, options: state.runOptions } })
    }, 500)
    return () => window.clearTimeout(timer)
  }, [settings.runtime.drafts, settings.runtime.options, state.codes, state.runOptions, updateSettings])

  useEffect(() => window.mootool.onRuntimeOutput((event) => {
    if (event.requestId !== requestIdRef.current) return
    if (event.stream === 'stdout') stdoutRef.current += event.text
    else stderrRef.current += event.text
    update({ stdout: stdoutRef.current, stderr: stderrRef.current })
  }), [])

  useEffect(() => {
    const handleShortcut = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key === 'Enter') {
        event.preventDefault()
        if (!state.running) void runCode()
      }
    }
    window.addEventListener('keydown', handleShortcut)
    return () => window.removeEventListener('keydown', handleShortcut)
  })

  async function detectRuntimes(): Promise<void> {
    update({ detecting: true })
    try {
      update({ statuses: await window.mootool.detectRuntimes(), detecting: false })
    } catch (error) {
      update({ detecting: false })
      toast.error(errorMessage(error))
    }
  }

  function setCode(nextCode: string): void {
    update({ codes: { ...state.codes, [runtime]: nextCode } })
  }

  async function runCode(): Promise<void> {
    if (state.running) return
    const currentStatus = state.statuses.find((item) => item.id === runtime)
    if (currentStatus && !currentStatus.available) {
      toast.error(t('runtime.configure', { name: runtimeDisplayName(runtime) }))
      return
    }
    const requestId = `run-${crypto.randomUUID()}`
    requestIdRef.current = requestId
    stdoutRef.current = ''
    stderrRef.current = ''
    update({ running: true, requestId, stdout: '', stderr: '', result: null })
    try {
      const result = await window.mootool.runCode({
        requestId,
        runtime,
        code,
        timeoutMs: settings.network.requestTimeoutMs,
        arguments: parseRuntimeArguments(runOption.arguments),
        workingDirectory: runOption.workingDirectory
      })
      stdoutRef.current = result.stdout
      stderrRef.current = result.stderr
      update({ running: false, requestId: '', stdout: result.stdout, stderr: result.stderr, result })
      await window.mootool.saveHistory({
        funcType: `runtime-${runtime}`,
        summary: `${runtimeDisplayName(runtime)} · ${result.exitCode ?? '-'}`,
        inputText: code,
        outputText: [result.stdout, result.stderr].filter(Boolean).join('\n'),
        extraData: JSON.stringify(runOption)
      })
    } catch (error) {
      update({ running: false, requestId: '' })
      toast.error(errorMessage(error))
    } finally {
      requestIdRef.current = ''
    }
  }

  async function stopCode(): Promise<void> {
    if (!state.requestId) return
    await window.mootool.cancelCodeRun(state.requestId)
  }

  async function formatSource(): Promise<void> {
    try {
      setCode(await formatRuntimeSource(code, runtime))
    } catch (error) {
      toast.error(errorMessage(error))
    }
  }

  function switchTab(tab: PrimaryRuntime): void {
    if (state.running) return
    update({ activeTab: tab, stdout: '', stderr: '', result: null })
    stdoutRef.current = ''
    stderrRef.current = ''
  }

  function patchRunOption(patch: Partial<RuntimeRunOption>): void {
    update({ runOptions: { ...state.runOptions, [runtime]: { ...runOption, ...patch } } })
  }

  return (
    <section className="tool-page runtime-tool">
      <div className="tool-page__header">
        <h1>{t('runtime.title')}</h1>
        <div className={availableCount > 0 ? 'status-pill status-pill--valid' : 'status-pill status-pill--error'}>
          {state.detecting ? <LoaderCircle className="spin" size={14} /> : availableCount > 0 ? <CheckCircle2 size={14} /> : <CircleAlert size={14} />}
          {statusText}
        </div>
      </div>

      <div className="runtime-shell">
        <div className="runtime-tabs" role="tablist">
          {(['java', 'python', 'node'] as const).map((tab) => (
            <button className={state.activeTab === tab ? 'runtime-tab runtime-tab--active' : 'runtime-tab'} type="button" role="tab" aria-selected={state.activeTab === tab} key={tab} disabled={state.running} onClick={() => switchTab(tab)}>
              {t(`runtime.tab.${tab}` as MessageKey)}
            </button>
          ))}
          <span />
          <Tooltip content={t('runtime.detect')} side="bottom">
            <button className="quick-note-icon-button" type="button" aria-label={t('runtime.detect')} disabled={state.detecting || state.running} onClick={() => { void detectRuntimes() }}><Settings size={14} /></button>
          </Tooltip>
        </div>

        <div className="runtime-toolbar">
          {state.activeTab === 'java' && (
            <div className="segmented" role="tablist">
              {(['java', 'groovy'] as const).map((mode) => (
                <button className={state.javaMode === mode ? 'segmented__item segmented__item--active' : 'segmented__item'} type="button" role="tab" aria-selected={state.javaMode === mode} key={mode} disabled={state.running} onClick={() => update({ javaMode: mode, stdout: '', stderr: '', result: null })}>
                  {t(`runtime.mode.${mode}` as MessageKey)}
                </button>
              ))}
            </div>
          )}
          <button type="button" disabled={state.running} onClick={() => { void formatSource() }}><WandSparkles size={13} />{t('runtime.format')}</button>
          <button className="runtime-run-button" type="button" disabled={state.running || status?.available === false} onClick={() => { void runCode() }}><Play size={13} />{t('runtime.run')}</button>
          <button type="button" disabled={!state.running} onClick={() => { void stopCode() }}><Square size={12} />{t('runtime.stop')}</button>
          <button type="button" onClick={() => { stdoutRef.current = ''; stderrRef.current = ''; update({ stdout: '', stderr: '', result: null }) }}><Eraser size={13} />{t('runtime.clear')}</button>
          <button type="button" disabled={state.running} onClick={() => update({ historyOpen: true })}><History size={13} />{t('runtime.history')}</button>
          <button type="button" disabled={state.running} onClick={() => update({ optionsOpen: true })}><SlidersHorizontal size={13} />{t('runtime.options')}</button>
          <span className="runtime-toolbar__spacer" />
          <code title={status?.version || ''}>{status?.available ? status.command : t('runtime.missing', { name: runtimeDisplayName(runtime) })}</code>
        </div>

        {status?.available === false && (
          <div className="runtime-missing-banner">
            <CircleAlert size={15} />
            <span>{t('runtime.configure', { name: runtimeDisplayName(runtime) })}</span>
            <button type="button" onClick={() => window.mootool.openSettings()}>{t('runtime.openSettings')}</button>
          </div>
        )}

        <div className="runtime-workspace">
          <section className="runtime-pane runtime-code-pane">
            <header>{t('runtime.editor')}<span>{runtimeDisplayName(runtime)}</span></header>
            <div className="runtime-editor-wrap">
              <pre ref={lineNumbersRef} className="runtime-line-numbers" aria-hidden="true">{lineNumbers(code)}</pre>
              <textarea
                aria-label={t('runtime.editor')}
                spellCheck={false}
                value={code}
                onChange={(event) => setCode(event.target.value)}
                onScroll={(event) => { if (lineNumbersRef.current) lineNumbersRef.current.scrollTop = event.currentTarget.scrollTop }}
              />
            </div>
          </section>
          <section className="runtime-pane runtime-output-pane">
            <header>{t('runtime.output')}<span className={`runtime-output-state runtime-output-state--${outputState.tone}`}>{outputState.label}</span></header>
            <div className="runtime-output-scroll">
              {!state.stdout && !state.stderr && !state.running && <div className="runtime-output-empty">{t('runtime.ready')}</div>}
              {state.stdout && <pre className="runtime-stdout">{state.stdout}</pre>}
              {state.stderr && <pre className="runtime-stderr">{state.stderr}</pre>}
              {state.running && <div className="runtime-running"><LoaderCircle className="spin" size={14} />{t('runtime.running', { name: runtimeDisplayName(runtime) })}</div>}
            </div>
            {state.result && (
              <footer className="runtime-result-footer">
                <span title={state.result.command}>{t('runtime.command')}: <code>{state.result.command}</code></span>
                <span>{t('runtime.exitCode', { code: String(state.result.exitCode ?? '-') })}</span>
                <span>{t('runtime.duration', { duration: String(state.result.durationMs) })}</span>
              </footer>
            )}
          </section>
        </div>
      </div>

      <HistoryDialog
        funcType={`runtime-${runtime}`}
        open={state.historyOpen}
        onClose={() => update({ historyOpen: false })}
        onApply={(value) => { setCode(value); update({ historyOpen: false }) }}
      />
      <Dialog
        title={`${t('runtime.options')} · ${runtimeDisplayName(runtime)}`}
        open={state.optionsOpen}
        onClose={() => update({ optionsOpen: false })}
        footer={<button className="dialog-button dialog-button--primary" type="button" onClick={() => update({ optionsOpen: false })}>{t('common.close')}</button>}
      >
        <div className="runtime-options-form">
          <label>
            <span>{t('runtime.arguments')}</span>
            <input value={runOption.arguments} placeholder={t('runtime.argumentsPlaceholder')} onChange={(event) => patchRunOption({ arguments: event.target.value })} />
          </label>
          <label>
            <span>{t('runtime.workingDirectory')}</span>
            <div>
              <input value={runOption.workingDirectory} placeholder={t('runtime.defaultWorkingDirectory')} onChange={(event) => patchRunOption({ workingDirectory: event.target.value })} />
              <button className="icon-button" type="button" aria-label={t('settings.chooseDirectory')} onClick={() => { void window.mootool.chooseDirectory(runOption.workingDirectory).then((path) => { if (path) patchRunOption({ workingDirectory: path }) }) }}><FolderOpen size={15} /></button>
            </div>
          </label>
        </div>
      </Dialog>
    </section>
  )
}

function runtimeResultLabel(
  result: RuntimeExecutionResult | null,
  running: boolean,
  runtime: CodeRuntimeId,
  t: (key: MessageKey, params?: Record<string, string>) => string
): { label: string; tone: 'idle' | 'success' | 'error' } {
  if (running) return { label: t('runtime.running', { name: runtimeDisplayName(runtime) }), tone: 'idle' }
  if (!result) return { label: t('runtime.ready'), tone: 'idle' }
  if (result.truncated) return { label: t('runtime.truncated'), tone: 'error' }
  if (result.timedOut) return { label: t('runtime.timeout'), tone: 'error' }
  if (result.cancelled) return { label: t('runtime.cancelled'), tone: 'error' }
  if (result.exitCode === 0) return { label: t('runtime.completed'), tone: 'success' }
  return { label: t('runtime.failed'), tone: 'error' }
}

function lineNumbers(code: string): string {
  return Array.from({ length: Math.max(1, code.split(/\r?\n/).length) }, (_value, index) => String(index + 1)).join('\n')
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error)
}

function sameRuntimePersistence(left: unknown, right: unknown): boolean {
  return JSON.stringify(left) === JSON.stringify(right)
}
