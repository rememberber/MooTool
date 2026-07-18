import { CircleAlert, Loader2, Play, ShieldAlert, Square, TerminalSquare } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import type { AiAgentProfile } from '@/shared/contracts/aiAgents'
import { requiresAiAgentTaskWriteConfirmation, type AiAgentTaskOutputEvent, type AiAgentTaskResult } from '@/shared/contracts/aiAgentTasks'
import { aiClientNames } from '@/shared/contracts/ai'
import { Dialog } from '@/shared/components/Dialog'
import { useI18n } from '@/shared/i18n/I18nProvider'

type AgentTaskDialogProps = {
  profile: AiAgentProfile | null
  onClose: () => void
}

export function AgentTaskDialog({ profile, onClose }: AgentTaskDialogProps) {
  const { t } = useI18n()
  const [prompt, setPrompt] = useState('')
  const [maxDurationSeconds, setMaxDurationSeconds] = useState(300)
  const [maxTurns, setMaxTurns] = useState(12)
  const [confirmExecution, setConfirmExecution] = useState(false)
  const [confirmWrite, setConfirmWrite] = useState(false)
  const [running, setRunning] = useState(false)
  const [log, setLog] = useState('')
  const [error, setError] = useState('')
  const [result, setResult] = useState<AiAgentTaskResult | null>(null)
  const activeRequestId = useRef('')

  useEffect(() => window.mootool.onAiAgentTaskOutput((event) => appendOutput(event)), [])
  useEffect(() => {
    setPrompt('')
    setMaxDurationSeconds(300)
    setMaxTurns(12)
    setConfirmExecution(false)
    setConfirmWrite(false)
    setRunning(false)
    setLog('')
    setError('')
    setResult(null)
    activeRequestId.current = ''
  }, [profile?.id])

  function appendOutput(event: AiAgentTaskOutputEvent): void {
    if (event.requestId !== activeRequestId.current) return
    const prefix = event.stream === 'system' ? '[MooTool] ' : event.stream === 'stderr' ? '[stderr] ' : ''
    setLog((current) => `${current}${prefix}${event.text}`)
  }

  async function run(): Promise<void> {
    if (!profile) return
    const requestId = crypto.randomUUID()
    activeRequestId.current = requestId
    setRunning(true)
    setLog('')
    setError('')
    setResult(null)
    try {
      setResult(await window.mootool.runAiAgentTask({
        requestId,
        profileId: profile.id,
        prompt,
        maxDurationSeconds,
        maxTurns,
        confirmExecution,
        confirmWrite
      }))
    } catch (runError) {
      setError(runError instanceof Error ? runError.message : String(runError))
    } finally {
      activeRequestId.current = ''
      setRunning(false)
    }
  }

  async function cancel(): Promise<void> {
    if (activeRequestId.current) await window.mootool.cancelAiAgentTask(activeRequestId.current)
  }

  const writeConfirmationRequired = profile ? requiresAiAgentTaskWriteConfirmation(profile) : false
  const canRun = Boolean(profile && prompt.trim() && confirmExecution && (!writeConfirmationRequired || confirmWrite))

  return <Dialog
    title={t('agentManager.task.title')}
    open={Boolean(profile)}
    width={860}
    onClose={() => { if (!running) onClose() }}
    footer={<>
      <button className="dialog-button" type="button" disabled={running} onClick={onClose}>{t('common.close')}</button>
      {running
        ? <button className="dialog-button dialog-button--danger" type="button" onClick={() => { void cancel() }}><Square size={14} />{t('agentManager.task.cancel')}</button>
        : <button className="dialog-button dialog-button--primary" type="button" disabled={!canRun} onClick={() => { void run() }}><Play size={14} />{t('agentManager.task.run')}</button>}
    </>}
  >
    {profile && <div className="ai-agent-task">
      <div className="ai-agent-task-profile"><TerminalSquare size={17} /><div><strong>{profile.name}</strong><span>{aiClientNames[profile.clientId]} · {profile.workingDirectory}</span></div></div>
      <div className="ai-agent-task-notice"><ShieldAlert size={16} /><span>{t('agentManager.task.safety')}</span></div>
      {error && <div className="ai-error" role="alert"><CircleAlert size={16} /><span>{error}</span></div>}
      <label className="ai-agent-field"><span>{t('agentManager.task.prompt')}</span><textarea autoFocus disabled={running} value={prompt} maxLength={65536} placeholder={t('agentManager.task.promptHint')} onChange={(event) => setPrompt(event.target.value)} /></label>
      <div className="ai-agent-editor-grid">
        <label className="ai-agent-field"><span>{t('agentManager.task.duration')}</span><input type="number" min="1" max="3600" disabled={running} value={maxDurationSeconds} onChange={(event) => setMaxDurationSeconds(Math.min(3600, Math.max(1, Math.round(Number(event.target.value) || 1))))} /></label>
        <label className="ai-agent-field"><span>{t('agentManager.task.turns')}</span><input type="number" min="1" max="100" disabled={running || profile.clientId === 'codex'} value={maxTurns} onChange={(event) => setMaxTurns(Math.min(100, Math.max(1, Math.round(Number(event.target.value) || 1))))} /><small>{profile.clientId === 'codex' ? t('agentManager.task.turnsCodex') : t('agentManager.task.turnsClaude')}</small></label>
      </div>
      <div className="ai-agent-task-confirmations">
        <label><input type="checkbox" disabled={running} checked={confirmExecution} onChange={(event) => setConfirmExecution(event.target.checked)} /><span>{t('agentManager.task.confirmExecution')}</span></label>
        {writeConfirmationRequired && <label className="ai-agent-task-write"><input type="checkbox" disabled={running} checked={confirmWrite} onChange={(event) => setConfirmWrite(event.target.checked)} /><span>{t('agentManager.task.confirmWrite')}</span></label>}
      </div>
      {(running || log || result) && <section className="ai-agent-task-output"><header><strong>{t('agentManager.task.output')}</strong>{running && <span><Loader2 className="spin" size={14} />{t('agentManager.task.running')}</span>}{result && <span className={`ai-agent-task-status ai-agent-task-status--${result.status}`}>{t(`agentManager.task.status.${result.status}` as 'agentManager.task.status.completed')}</span>}</header><pre>{log || t('agentManager.task.waiting')}</pre></section>}
      {result && <dl className="ai-agent-task-result"><div><dt>{t('agentManager.task.durationResult')}</dt><dd>{(result.durationMs / 1000).toFixed(1)} s</dd></div><div><dt>{t('agentManager.task.exitCode')}</dt><dd>{result.exitCode ?? '—'}</dd></div><div><dt>{t('agentManager.task.transport')}</dt><dd>stdin</dd></div></dl>}
    </div>}
  </Dialog>
}
