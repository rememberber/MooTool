import { Check, CircleAlert, FlaskConical, Plus, RefreshCw, Save, ShieldCheck, Square, Trash2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import { AiMetric } from './AiManagerChrome'
import type { AiPromptLabCase, AiPromptLabRunResult, AiPromptLabSuite, AiPromptLabSuiteSaveInput } from '@/shared/contracts/aiPromptLab'
import type { AiModelRuntimeId, AiModelRuntimeSnapshot } from '@/shared/contracts/aiModelRuntime'
import { ToolPageHeader } from '@/shared/components/ToolPage'
import { useI18n } from '@/shared/i18n/I18nProvider'

type Draft = AiPromptLabSuiteSaveInput

export function PromptLabTool() {
  const { t } = useI18n()
  const [suites, setSuites] = useState<AiPromptLabSuite[]>([])
  const [runtimes, setRuntimes] = useState<AiModelRuntimeSnapshot | null>(null)
  const [draft, setDraft] = useState<Draft>(() => emptyDraft())
  const [runtimeId, setRuntimeId] = useState<AiModelRuntimeId>('ollama')
  const [model, setModel] = useState('')
  const [temperature, setTemperature] = useState(0.2)
  const [maxTokens, setMaxTokens] = useState(512)
  const [confirmNetwork, setConfirmNetwork] = useState(false)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [running, setRunning] = useState(false)
  const [requestId, setRequestId] = useState('')
  const [result, setResult] = useState<AiPromptLabRunResult | null>(null)
  const [error, setError] = useState('')

  async function load(): Promise<void> {
    setLoading(true)
    setError('')
    try {
      const [suiteList, runtimeSnapshot] = await Promise.all([
        window.mootool.listAiPromptLabSuites(),
        window.mootool.getAiModelRuntimeSnapshot()
      ])
      setSuites(suiteList)
      setRuntimes(runtimeSnapshot)
      const runnable = runtimeSnapshot.runtimes.filter((item) => ['healthy', 'degraded'].includes(item.health) && item.models.length > 0)
      const selected = runnable.find((item) => item.id === runtimeId) ?? runnable[0]
      if (selected) {
        setRuntimeId(selected.id)
        setModel((current) => selected.models.some((item) => item.name === current) ? current : selected.models[0]?.name ?? '')
      }
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : String(loadError))
    } finally {
      setLoading(false)
    }
  }

  function selectSuite(suite: AiPromptLabSuite): void {
    setDraft({ id: suite.id, name: suite.name, systemPrompt: suite.systemPrompt, promptTemplate: suite.promptTemplate, testCases: suite.testCases.map((item) => ({ ...item })) })
    setResult(null)
    setError('')
  }

  function newSuite(): void {
    setDraft(emptyDraft())
    setResult(null)
    setError('')
  }

  async function saveSuite(): Promise<void> {
    setSaving(true)
    setError('')
    try {
      const saved = await window.mootool.saveAiPromptLabSuite(draft)
      setDraft({ id: saved.id, name: saved.name, systemPrompt: saved.systemPrompt, promptTemplate: saved.promptTemplate, testCases: saved.testCases.map((item) => ({ ...item })) })
      setSuites(await window.mootool.listAiPromptLabSuites())
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : String(saveError))
    } finally {
      setSaving(false)
    }
  }

  async function deleteSuite(): Promise<void> {
    if (!draft.id || !window.confirm(`${t('promptLab.delete')} ${draft.name}?`)) return
    setSaving(true)
    setError('')
    try {
      await window.mootool.deleteAiPromptLabSuite(draft.id)
      setSuites(await window.mootool.listAiPromptLabSuites())
      newSuite()
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : String(deleteError))
    } finally {
      setSaving(false)
    }
  }

  async function run(): Promise<void> {
    const nextRequestId = crypto.randomUUID()
    setRequestId(nextRequestId)
    setRunning(true)
    setResult(null)
    setError('')
    try {
      setResult(await window.mootool.runAiPromptLab({
        requestId: nextRequestId,
        runtimeId,
        model,
        systemPrompt: draft.systemPrompt,
        promptTemplate: draft.promptTemplate,
        testCases: draft.testCases,
        temperature,
        maxTokens,
        confirmNetworkEndpoint: confirmNetwork
      }))
    } catch (runError) {
      setError(runError instanceof Error ? runError.message : String(runError))
    } finally {
      setRunning(false)
      setRequestId('')
    }
  }

  async function cancel(): Promise<void> {
    if (requestId) await window.mootool.cancelAiPromptLab(requestId)
  }

  function updateCase(id: string, patch: Partial<AiPromptLabCase>): void {
    setDraft({ ...draft, testCases: draft.testCases.map((item) => item.id === id ? { ...item, ...patch } : item) })
  }

  function chooseRuntime(id: AiModelRuntimeId): void {
    const selected = runtimes?.runtimes.find((item) => item.id === id)
    setRuntimeId(id)
    setModel(selected?.models[0]?.name ?? '')
    setConfirmNetwork(false)
  }

  useEffect(() => { void load() }, [])

  const runtime = runtimes?.runtimes.find((item) => item.id === runtimeId)
  const runnable = runtimes?.runtimes.filter((item) => ['healthy', 'degraded'].includes(item.health) && item.models.length > 0) ?? []
  const needsNetworkConfirmation = runtime?.exposure === 'remote' || runtime?.exposure === 'localNetwork'
  const canRun = !running && Boolean(model) && draft.promptTemplate.trim().length > 0 && draft.testCases.length > 0 && draft.testCases.every((item) => item.name.trim()) && (!needsNetworkConfirmation || confirmNetwork)
  const canSave = !saving && draft.name.trim().length > 0 && draft.promptTemplate.trim().length > 0 && draft.testCases.length > 0 && draft.testCases.every((item) => item.name.trim())

  return <section className="tool-page p5-tool ai-prompt-lab">
    <ToolPageHeader title={t('promptLab.title')} actions={<><button className="toolbar-button" type="button" disabled={loading} onClick={() => { void load() }}><RefreshCw size={14} className={loading ? 'spin' : undefined} />{t('common.refresh')}</button><button className="toolbar-button" type="button" disabled={!canSave} onClick={() => { void saveSuite() }}><Save size={14} />{saving ? t('common.processing') : t('common.save')}</button>{running ? <button className="toolbar-button toolbar-button--danger" type="button" onClick={() => { void cancel() }}><Square size={13} />{t('promptLab.stop')}</button> : <button className="toolbar-button" type="button" disabled={!canRun} onClick={() => { void run() }}><FlaskConical size={14} />{t('promptLab.run')}</button>}</>} />
    <div className="local-tool-shell ai-prompt-lab-shell">
      <aside className="ai-prompt-suite-sidebar"><header><strong>{t('promptLab.suites')}</strong><button className="icon-button" type="button" aria-label={t('promptLab.new')} onClick={newSuite}><Plus size={14} /></button></header>{suites.length > 0 ? suites.map((suite) => <button type="button" className={draft.id === suite.id ? 'is-active' : ''} onClick={() => selectSuite(suite)} key={suite.id}><strong>{suite.name}</strong><small>{suite.testCases.length} · {new Date(suite.updatedAt).toLocaleDateString()}</small></button>) : <p>{t('promptLab.emptySuites')}</p>}</aside>
      <main className="ai-prompt-editor">
        <div className="ai-change-safety"><ShieldCheck size={16} /><span>{t('promptLab.safety')}</span></div>
        {error && <div className="ai-error" role="alert"><CircleAlert size={16} /><span>{error}</span></div>}
        <div className="ai-prompt-fields"><label><span>{t('promptLab.name')}</span><input value={draft.name} onChange={(event) => setDraft({ ...draft, name: event.target.value })} /></label><label><span>{t('promptLab.system')}</span><textarea rows={3} value={draft.systemPrompt} onChange={(event) => setDraft({ ...draft, systemPrompt: event.target.value })} /></label><label><span>{t('promptLab.template')}</span><textarea rows={5} value={draft.promptTemplate} onChange={(event) => setDraft({ ...draft, promptTemplate: event.target.value })} /><small>{t('promptLab.templateHint')}</small></label></div>
        <section className="ai-prompt-run-config"><label><span>{t('promptLab.runtime')}</span><select value={runtimeId} onChange={(event) => chooseRuntime(event.target.value as AiModelRuntimeId)}>{runnable.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}</select></label><label><span>{t('promptLab.model')}</span><select value={model} onChange={(event) => setModel(event.target.value)}>{runtime?.models.map((item) => <option value={item.name} key={item.digest}>{item.name}</option>)}</select></label><label><span>{t('promptLab.temperature')}</span><input type="number" min="0" max="2" step="0.1" value={temperature} onChange={(event) => setTemperature(Math.min(2, Math.max(0, Number(event.target.value) || 0)))} /></label><label><span>{t('promptLab.maxTokens')}</span><input type="number" min="1" max="32768" value={maxTokens} onChange={(event) => setMaxTokens(Math.min(32768, Math.max(1, Number(event.target.value) || 1)))} /></label></section>
        {runnable.length === 0 && <div className="history-empty">{t('promptLab.noRuntime')}</div>}
        {needsNetworkConfirmation && <label className="ai-prompt-network-confirm"><input type="checkbox" checked={confirmNetwork} onChange={(event) => setConfirmNetwork(event.target.checked)} /><span>{t('promptLab.confirmNetwork')}</span></label>}
        <section className="ai-prompt-cases"><header><div><h2>{t('promptLab.cases')}</h2><small>{draft.testCases.length} / 20</small></div><button className="toolbar-button" type="button" disabled={draft.testCases.length >= 20} onClick={() => setDraft({ ...draft, testCases: [...draft.testCases, newCase(draft.testCases.length + 1)] })}><Plus size={13} />{t('promptLab.addCase')}</button></header>{draft.testCases.map((testCase, index) => <article key={testCase.id}><header><span>{index + 1}</span><input aria-label={t('promptLab.caseName')} value={testCase.name} placeholder={t('promptLab.caseName')} onChange={(event) => updateCase(testCase.id, { name: event.target.value })} /><button className="icon-button" type="button" disabled={draft.testCases.length === 1} aria-label={t('promptLab.delete')} onClick={() => setDraft({ ...draft, testCases: draft.testCases.filter((item) => item.id !== testCase.id) })}><Trash2 size={13} /></button></header><div><label><span>{t('promptLab.caseInput')}</span><textarea rows={3} value={testCase.input} onChange={(event) => updateCase(testCase.id, { input: event.target.value })} /></label><label><span>{t('promptLab.expected')}</span><input value={testCase.expectedContains} onChange={(event) => updateCase(testCase.id, { expectedContains: event.target.value })} /></label></div></article>)}</section>
        {result && <section className="ai-prompt-results"><header><div><h2>{t('promptLab.results')}</h2><code>{result.runtimeName} · {result.model}</code></div>{result.cancelled && <span>{t('promptLab.stop')}</span>}</header><div className="ai-manager-metrics"><AiMetric label={t('promptLab.metric.completed')} value={`${result.summary.completed}/${result.summary.cases}`} /><AiMetric label={t('promptLab.metric.passRate')} value={result.summary.passRate === undefined ? '—' : `${Math.round(result.summary.passRate * 100)}%`} /><AiMetric label={t('promptLab.metric.tokens')} value={result.summary.totalTokens.toLocaleString()} /><AiMetric label={t('promptLab.metric.duration')} value={`${result.summary.durationMs} ms`} /></div>{result.results.map((item) => <article className={item.error ? 'is-error' : item.passed === true ? 'is-passed' : item.passed === false ? 'is-failed' : ''} key={item.caseId}><header><strong>{item.name}</strong><span>{item.error ? item.error : item.passed === true ? <><Check size={12} />{t('promptLab.passed')}</> : item.passed === false ? t('promptLab.failed') : t('promptLab.unscored')}</span><small>{item.durationMs} ms · {item.totalTokens ?? 0} Token</small></header><details><summary>{t('promptLab.output')}</summary><pre>{item.output}</pre></details></article>)}</section>}
      </main>
    </div>
  </section>
}

function emptyDraft(): Draft {
  return { name: '', systemPrompt: '', promptTemplate: '{{input}}', testCases: [newCase(1)] }
}

function newCase(index: number): AiPromptLabCase {
  return { id: crypto.randomUUID(), name: `Case ${index}`, input: '', expectedContains: '' }
}
