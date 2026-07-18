import { CircleAlert, CloudDownload, Loader2, LockKeyhole, Play, ShieldAlert, Square } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import type { SecretStatus } from '@/shared/contracts/settings'
import type { AiModelRuntimeInstallation } from '@/shared/contracts/aiModelRuntime'
import type { AiModelRuntimeAction, AiModelRuntimeActionPlan, AiModelRuntimeActionProgressEvent, AiModelRuntimeActionResult } from '@/shared/contracts/aiModelRuntimeActions'
import { Dialog } from '@/shared/components/Dialog'
import { useI18n } from '@/shared/i18n/I18nProvider'

export type ModelRuntimeActionRequest = {
  id: string
  runtime: AiModelRuntimeInstallation
  action: AiModelRuntimeAction
  modelName?: string
}

type Props = {
  request: ModelRuntimeActionRequest | null
  onClose: () => void
  onCompleted: () => void
}

export function ModelRuntimeActionDialog({ request, onClose, onCompleted }: Props) {
  const { t } = useI18n()
  const [modelName, setModelName] = useState('')
  const [plan, setPlan] = useState<AiModelRuntimeActionPlan | null>(null)
  const [planning, setPlanning] = useState(false)
  const [executing, setExecuting] = useState(false)
  const [confirmAction, setConfirmAction] = useState(false)
  const [confirmDestructive, setConfirmDestructive] = useState(false)
  const [confirmRemote, setConfirmRemote] = useState(false)
  const [events, setEvents] = useState<AiModelRuntimeActionProgressEvent[]>([])
  const [result, setResult] = useState<AiModelRuntimeActionResult | null>(null)
  const [error, setError] = useState('')
  const [tokenStatus, setTokenStatus] = useState<SecretStatus | null>(null)
  const [token, setToken] = useState('')
  const [tokenSaving, setTokenSaving] = useState(false)
  const activeRequestId = useRef('')

  useEffect(() => window.mootool.onAiModelRuntimeActionProgress((event) => {
    if (event.requestId === activeRequestId.current) setEvents((current) => [...current.slice(-199), event])
  }), [])

  useEffect(() => {
    setModelName(request?.modelName ?? '')
    setPlan(null)
    setPlanning(false)
    setExecuting(false)
    setConfirmAction(false)
    setConfirmDestructive(false)
    setConfirmRemote(false)
    setEvents([])
    setResult(null)
    setError('')
    setToken('')
    activeRequestId.current = ''
    if (request?.runtime.id === 'lmStudio') void readTokenStatus()
    else setTokenStatus(null)
  }, [request?.id])

  async function readTokenStatus(): Promise<void> {
    try { setTokenStatus(await window.mootool.getSecretStatus('lmStudioApiToken')) } catch (statusError) {
      setError(statusError instanceof Error ? statusError.message : String(statusError))
    }
  }

  async function saveToken(): Promise<void> {
    if (!token.trim()) return
    setTokenSaving(true)
    setError('')
    try {
      setTokenStatus(await window.mootool.setSecret('lmStudioApiToken', token))
      setToken('')
      setPlan(null)
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : String(saveError))
    } finally { setTokenSaving(false) }
  }

  async function clearToken(): Promise<void> {
    setTokenSaving(true)
    setError('')
    try {
      setTokenStatus(await window.mootool.clearSecret('lmStudioApiToken'))
      setPlan(null)
    } catch (clearError) {
      setError(clearError instanceof Error ? clearError.message : String(clearError))
    } finally { setTokenSaving(false) }
  }

  async function preview(): Promise<void> {
    if (!request) return
    setPlanning(true)
    setError('')
    setResult(null)
    setEvents([])
    try {
      setPlan(await window.mootool.planAiModelRuntimeAction({ runtimeId: request.runtime.id, action: request.action, modelName }))
      setConfirmAction(false)
      setConfirmDestructive(false)
      setConfirmRemote(false)
    } catch (planError) {
      setError(planError instanceof Error ? planError.message : String(planError))
    } finally { setPlanning(false) }
  }

  async function execute(): Promise<void> {
    if (!plan) return
    const requestId = crypto.randomUUID()
    activeRequestId.current = requestId
    setExecuting(true)
    setError('')
    setResult(null)
    try {
      const actionResult = await window.mootool.executeAiModelRuntimeAction({
        requestId,
        planId: plan.planId,
        confirmAction,
        confirmDestructive,
        confirmRemoteEndpoint: confirmRemote
      })
      setResult(actionResult)
      if (actionResult.status === 'completed') onCompleted()
    } catch (executeError) {
      setError(executeError instanceof Error ? executeError.message : String(executeError))
    } finally {
      activeRequestId.current = ''
      setExecuting(false)
    }
  }

  async function cancel(): Promise<void> {
    if (activeRequestId.current) await window.mootool.cancelAiModelRuntimeAction(activeRequestId.current)
  }

  const canExecute = Boolean(plan && confirmAction && (!plan.destructive || confirmDestructive) && (!plan.requiresRemoteConfirmation || confirmRemote))
  const latestProgress = [...events].reverse().find((event) => event.percent !== undefined)?.percent

  return <Dialog
    title={t(`modelRuntime.action.title.${request?.action ?? 'pull'}` as 'modelRuntime.action.title.pull')}
    open={Boolean(request)}
    width={780}
    onClose={() => { if (!planning && !executing) onClose() }}
    footer={<>
      <button className="dialog-button" type="button" disabled={planning || executing} onClick={onClose}>{t('common.close')}</button>
      {executing
        ? <button className="dialog-button dialog-button--danger" type="button" onClick={() => { void cancel() }}><Square size={14} />{t('modelRuntime.action.cancel')}</button>
        : plan
          ? <button className="dialog-button dialog-button--primary" type="button" disabled={!canExecute || Boolean(result)} onClick={() => { void execute() }}><Play size={14} />{t('modelRuntime.action.execute')}</button>
          : <button className="dialog-button dialog-button--primary" type="button" disabled={planning || !modelName.trim()} onClick={() => { void preview() }}>{planning ? <Loader2 className="spin" size={14} /> : <ShieldAlert size={14} />}{t('modelRuntime.action.preview')}</button>}
    </>}
  >
    {request && <div className="ai-runtime-action">
      <div className="ai-runtime-action-runtime"><CloudDownload size={17} /><div><strong>{request.runtime.name}</strong><code>{request.runtime.endpoint}</code></div></div>
      <div className="ai-runtime-action-notice"><ShieldAlert size={16} /><span>{t('modelRuntime.action.safety')}</span></div>
      {error && <div className="ai-error" role="alert"><CircleAlert size={16} /><span>{error}</span></div>}
      {request.runtime.id === 'lmStudio' && <section className="ai-runtime-token"><header><div><LockKeyhole size={15} /><strong>{t('modelRuntime.action.token.title')}</strong></div><span>{tokenStatus?.stored ? t('modelRuntime.action.token.stored') : t('modelRuntime.action.token.optional')}</span></header>{tokenStatus?.encryptionAvailable === false ? <p>{t('modelRuntime.action.token.unavailable')}</p> : <div><input type="password" disabled={tokenSaving || executing} value={token} placeholder={t('modelRuntime.action.token.placeholder')} onChange={(event) => setToken(event.target.value)} /><button className="dialog-button" type="button" disabled={tokenSaving || !token.trim()} onClick={() => { void saveToken() }}>{t('modelRuntime.action.token.save')}</button>{tokenStatus?.stored && <button className="dialog-button" type="button" disabled={tokenSaving} onClick={() => { void clearToken() }}>{t('modelRuntime.action.token.clear')}</button>}</div>}</section>}
      <label className="ai-agent-field"><span>{t('modelRuntime.action.model')}</span><input autoFocus disabled={Boolean(plan) || planning || executing || Boolean(request.modelName)} value={modelName} placeholder={request.action === 'pull' ? t('modelRuntime.action.modelPullHint') : ''} onChange={(event) => setModelName(event.target.value)} /></label>
      {plan && <>
        <dl className="ai-runtime-action-plan"><div><dt>{t('modelRuntime.action.digest')}</dt><dd><code>{plan.modelDigest ?? '—'}</code></dd></div><div><dt>{t('modelRuntime.action.size')}</dt><dd>{formatOptionalBytes(plan.modelSizeBytes)}</dd></div><div><dt>{t('modelRuntime.action.available')}</dt><dd>{formatOptionalBytes(plan.modelDirectoryAvailableBytes)}</dd></div><div><dt>{t('modelRuntime.action.expires')}</dt><dd>{new Date(plan.expiresAt).toLocaleTimeString()}</dd></div></dl>
        {plan.affectedAgentProfiles.length > 0 && <section className="ai-runtime-action-affected"><strong>{t('modelRuntime.action.affected')}</strong>{plan.affectedAgentProfiles.map((profile) => <span key={profile.id}>{profile.name}</span>)}</section>}
        {plan.warnings.length > 0 && <div className="ai-agent-plan-warnings">{plan.warnings.map((warning) => <p key={warning}><CircleAlert size={14} />{warning}</p>)}</div>}
        <div className="ai-agent-task-confirmations"><label><input type="checkbox" disabled={executing || Boolean(result)} checked={confirmAction} onChange={(event) => setConfirmAction(event.target.checked)} /><span>{t('modelRuntime.action.confirm')}</span></label>{plan.requiresRemoteConfirmation && <label className="ai-agent-task-write"><input type="checkbox" disabled={executing || Boolean(result)} checked={confirmRemote} onChange={(event) => setConfirmRemote(event.target.checked)} /><span>{t('modelRuntime.action.confirmExposure')}</span></label>}{plan.destructive && <label className="ai-agent-task-write"><input type="checkbox" disabled={executing || Boolean(result)} checked={confirmDestructive} onChange={(event) => setConfirmDestructive(event.target.checked)} /><span>{t('modelRuntime.action.confirmDelete', { model: plan.modelName })}</span></label>}</div>
      </>}
      {(events.length > 0 || executing || result) && <section className="ai-runtime-action-progress"><header><strong>{t('modelRuntime.action.progress')}</strong>{executing && <span><Loader2 className="spin" size={14} />{latestProgress === undefined ? t('common.processing') : `${latestProgress.toFixed(1)}%`}</span>}{result && <span className={`ai-agent-task-status ai-agent-task-status--${result.status === 'completed' ? 'completed' : result.status === 'cancelled' ? 'cancelled' : 'failed'}`}>{t(`modelRuntime.action.status.${result.status}` as 'modelRuntime.action.status.completed')}</span>}</header>{latestProgress !== undefined && <div className="ai-runtime-action-progress-bar"><span style={{ width: `${latestProgress}%` }} /></div>}<div>{events.map((event, index) => <p key={`${event.timestamp}-${index}`}><time>{new Date(event.timestamp).toLocaleTimeString()}</time><span>{event.message}</span></p>)}</div>{result && <footer>{result.message} · {(result.durationMs / 1000).toFixed(1)} s</footer>}</section>}
    </div>}
  </Dialog>
}

function formatOptionalBytes(value?: number): string {
  if (value === undefined) return '—'
  if (value <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const index = Math.min(units.length - 1, Math.floor(Math.log(value) / Math.log(1024)))
  return `${(value / 1024 ** index).toFixed(index === 0 ? 0 : 1)} ${units[index]}`
}
