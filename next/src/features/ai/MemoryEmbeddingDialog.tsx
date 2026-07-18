import { BrainCircuit, CircleAlert, Database, Search, ShieldCheck, Square } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Dialog } from '@/shared/components/Dialog'
import { useI18n } from '@/shared/i18n/I18nProvider'
import type { AiMemoryEmbeddingProgressEvent, AiMemoryEmbeddingStatus, AiMemorySemanticPreview } from '@/shared/contracts/aiMemoryEmbedding'
import type { AiModelRuntimeId, AiModelRuntimeSnapshot } from '@/shared/contracts/aiModelRuntime'

type MemoryEmbeddingDialogProps = {
  open: boolean
  projectRoot: string
  defaultQuery: string
  onClose: () => void
}

export function MemoryEmbeddingDialog({ open, projectRoot, defaultQuery, onClose }: MemoryEmbeddingDialogProps) {
  const { t } = useI18n()
  const [runtimeSnapshot, setRuntimeSnapshot] = useState<AiModelRuntimeSnapshot | null>(null)
  const [status, setStatus] = useState<AiMemoryEmbeddingStatus | null>(null)
  const [runtimeId, setRuntimeId] = useState<AiModelRuntimeId>('ollama')
  const [model, setModel] = useState('')
  const [confirmed, setConfirmed] = useState(false)
  const [busy, setBusy] = useState(false)
  const [requestId, setRequestId] = useState('')
  const [events, setEvents] = useState<AiMemoryEmbeddingProgressEvent[]>([])
  const [error, setError] = useState('')
  const [semanticQuery, setSemanticQuery] = useState(defaultQuery)
  const [semanticPreview, setSemanticPreview] = useState<AiMemorySemanticPreview | null>(null)
  const [semanticBusy, setSemanticBusy] = useState(false)

  useEffect(() => window.mootool.onAiMemoryEmbeddingProgress((event) => {
    if (event.requestId === requestId) setEvents((current) => [...current.slice(-99), event])
  }), [requestId])

  useEffect(() => {
    if (!open) return
    setSemanticQuery(defaultQuery)
    setError('')
    void Promise.all([window.mootool.getAiModelRuntimeSnapshot(), window.mootool.getAiMemoryEmbeddingStatus()])
      .then(([runtimes, nextStatus]) => {
        setRuntimeSnapshot(runtimes)
        setStatus(nextStatus)
        const preferredRuntime = nextStatus.runtimeId && ['ollama', 'lmStudio'].includes(nextStatus.runtimeId) ? nextStatus.runtimeId : runtimes.runtimes.find((runtime) => ['ollama', 'lmStudio'].includes(runtime.id) && ['healthy', 'degraded'].includes(runtime.health))?.id ?? 'ollama'
        setRuntimeId(preferredRuntime)
        if (nextStatus.runtimeId === preferredRuntime && nextStatus.model) setModel(nextStatus.model)
      })
      .catch((loadError) => setError(message(loadError)))
  }, [open, defaultQuery])

  const runtimes = useMemo(() => (runtimeSnapshot?.runtimes ?? []).filter((runtime) => (runtime.id === 'ollama' || runtime.id === 'lmStudio') && ['healthy', 'degraded'].includes(runtime.health)), [runtimeSnapshot])
  const models = useMemo(() => {
    const all = runtimes.find((runtime) => runtime.id === runtimeId)?.models ?? []
    return [...all].sort((left, right) => Number(isLikelyEmbeddingModel(right)) - Number(isLikelyEmbeddingModel(left)) || left.name.localeCompare(right.name))
  }, [runtimes, runtimeId])

  useEffect(() => {
    if (!models.some((candidate) => candidate.name === model)) setModel(models[0]?.name ?? '')
  }, [models, model])

  async function rebuild(): Promise<void> {
    const nextRequestId = crypto.randomUUID()
    setRequestId(nextRequestId)
    setEvents([])
    setError('')
    setBusy(true)
    try {
      const result = await window.mootool.rebuildAiMemoryEmbeddings({ requestId: nextRequestId, runtimeId, model, confirmLocalProcessing: confirmed })
      if (result.status === 'failed') setError(result.message)
      setStatus(await window.mootool.getAiMemoryEmbeddingStatus())
    } catch (rebuildError) {
      setError(message(rebuildError))
    } finally {
      setBusy(false)
      setRequestId('')
    }
  }

  async function cancel(): Promise<void> {
    if (requestId) await window.mootool.cancelAiMemoryEmbedding(requestId)
  }

  async function runSemanticPreview(): Promise<void> {
    setSemanticBusy(true)
    setError('')
    try {
      setSemanticPreview(await window.mootool.previewAiMemoriesSemantic({
        requestId: crypto.randomUUID(), runtimeId, model, query: semanticQuery.trim(), confirmLocalProcessing: confirmed,
        ...(projectRoot ? { projectRoot, targetPath: projectRoot } : {}), tokenBudget: 1_000, maxItems: 20
      }))
    } catch (previewError) {
      setError(message(previewError))
      setSemanticPreview(null)
    } finally {
      setSemanticBusy(false)
    }
  }

  const latestProgress = events.at(-1)
  return <Dialog title={t('memoryManager.embedding.title')} open={open} width={860} onClose={() => { if (!busy && !semanticBusy) onClose() }} footer={<><button className="dialog-button" type="button" disabled={busy || semanticBusy} onClick={onClose}>{t('common.close')}</button>{busy ? <button className="dialog-button dialog-button--danger" type="button" onClick={() => { void cancel() }}><Square size={13} />{t('common.cancel')}</button> : <button className="dialog-button dialog-button--primary" type="button" disabled={!model || !confirmed} onClick={() => { void rebuild() }}><Database size={14} />{t('memoryManager.embedding.rebuild')}</button>}</>}>
    <div className="ai-memory-embedding">
      <div className="ai-native-memory-notice"><ShieldCheck size={15} /><span>{t('memoryManager.embedding.safety')}</span></div>
      {status && <div className="ai-memory-embedding-metrics"><Metric label={t('memoryManager.embedding.eligible')} value={status.eligible} /><Metric label={t('memoryManager.embedding.indexed')} value={status.indexed} /><Metric label={t('memoryManager.embedding.stale')} value={status.stale} /><Metric label={t('memoryManager.embedding.skipped')} value={status.skippedSensitive} /><Metric label={t('memoryManager.embedding.coverage')} value={`${Math.round(status.coverage * 100)}%`} /></div>}
      <div className="ai-memory-embedding-selectors"><label><span>{t('memoryManager.embedding.runtime')}</span><select value={runtimeId} disabled={busy} onChange={(event) => setRuntimeId(event.target.value as AiModelRuntimeId)}>{runtimes.map((runtime) => <option value={runtime.id} key={runtime.id}>{runtime.name} · {runtime.endpoint}</option>)}</select></label><label><span>{t('memoryManager.embedding.model')}</span><select value={model} disabled={busy} onChange={(event) => setModel(event.target.value)}>{models.map((item) => <option value={item.name} key={item.name}>{item.name}{isLikelyEmbeddingModel(item) ? ' · embedding' : ''}</option>)}</select></label></div>
      {runtimes.length === 0 && <div className="history-empty">{t('memoryManager.embedding.noRuntime')}</div>}
      <label className="ai-memory-embedding-confirm"><input type="checkbox" checked={confirmed} disabled={busy} onChange={(event) => setConfirmed(event.target.checked)} /><span>{t('memoryManager.embedding.confirm')}</span></label>
      {latestProgress && <div className="ai-memory-embedding-progress"><header><BrainCircuit size={14} /><strong>{latestProgress.message}</strong><span>{latestProgress.completed}/{latestProgress.total}</span></header><div><span style={{ width: `${latestProgress.total > 0 ? latestProgress.completed / latestProgress.total * 100 : 0}%` }} /></div></div>}
      {error && <div className="ai-error" role="alert"><CircleAlert size={16} /><span>{error}</span></div>}
      <section className="ai-memory-semantic"><header><div><Search size={15} /><strong>{t('memoryManager.embedding.semanticTitle')}</strong></div><small>{t('memoryManager.embedding.semanticHint')}</small></header><div><input value={semanticQuery} maxLength={2_000} placeholder={t('memoryManager.embedding.semanticPlaceholder')} onChange={(event) => setSemanticQuery(event.target.value)} /><button className="dialog-button" type="button" disabled={busy || semanticBusy || !confirmed || !model || !semanticQuery.trim()} onClick={() => { void runSemanticPreview() }}>{semanticBusy ? t('common.processing') : t('memoryManager.embedding.semanticRun')}</button></div>{semanticPreview && <><p>{t('memoryManager.embedding.semanticSummary', { count: String(semanticPreview.indexedCandidates) })}</p>{semanticPreview.memories.length > 0 ? <ol>{semanticPreview.memories.map((item) => <li key={item.memory.id}><span>{Math.round((item.semanticScore ?? 0) * 100)}%</span><div><strong>{item.memory.content}</strong><small>{item.reason} · ~{item.estimatedTokens} tokens</small></div></li>)}</ol> : <div className="history-empty">{t('memoryManager.preview.empty')}</div>}</>}</section>
    </div>
  </Dialog>
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return <div><span>{label}</span><strong>{value}</strong></div>
}

function isLikelyEmbeddingModel(model: { name: string; capabilities?: string[] }): boolean {
  return model.capabilities?.includes('embedding') === true || /embed|bge|e5(?:-|$)|gte|nomic/i.test(model.name)
}

function message(error: unknown): string {
  return error instanceof Error ? error.message : String(error)
}
