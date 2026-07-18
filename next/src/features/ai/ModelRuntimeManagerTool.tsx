import { Box, Boxes, CircleAlert, CloudDownload, Cpu, Database, Gauge, HardDrive, MemoryStick, Network, Power, PowerOff, RefreshCw, ShieldAlert, ShieldCheck, Trash2, ZoomIn } from 'lucide-react'
import { useEffect, useState } from 'react'
import { AiMetric } from './AiManagerChrome'
import { aiModelRuntimeNames, type AiLocalModelRuntimeModel, type AiModelRuntimeHealth, type AiModelRuntimeId, type AiModelRuntimeModelDetail, type AiModelRuntimeSnapshot } from '@/shared/contracts/aiModelRuntime'
import { Dialog } from '@/shared/components/Dialog'
import { ToolPageHeader } from '@/shared/components/ToolPage'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { supportedAiModelRuntimeActions, type AiModelRuntimeAction } from '@/shared/contracts/aiModelRuntimeActions'
import { ModelRuntimeActionDialog, type ModelRuntimeActionRequest } from './ModelRuntimeActionDialog'

export function ModelRuntimeManagerTool() {
  const { t } = useI18n()
  const [snapshot, setSnapshot] = useState<AiModelRuntimeSnapshot | null>(null)
  const [selectedRuntimeId, setSelectedRuntimeId] = useState<AiModelRuntimeId>('ollama')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [detailModel, setDetailModel] = useState('')
  const [detail, setDetail] = useState<AiModelRuntimeModelDetail | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailError, setDetailError] = useState('')
  const [actionRequest, setActionRequest] = useState<ModelRuntimeActionRequest | null>(null)

  async function scan(): Promise<void> {
    setLoading(true)
    setError('')
    try {
      setSnapshot(await window.mootool.getAiModelRuntimeSnapshot())
    } catch (scanError) {
      setError(scanError instanceof Error ? scanError.message : String(scanError))
    } finally {
      setLoading(false)
    }
  }

  async function inspectModel(model: AiLocalModelRuntimeModel): Promise<void> {
    setDetailModel(model.name)
    setDetail(null)
    setDetailError('')
    setDetailLoading(true)
    try {
      setDetail(await window.mootool.inspectAiModelRuntimeModel({ runtimeId: selectedRuntimeId, modelName: model.name }))
    } catch (inspectError) {
      setDetailError(inspectError instanceof Error ? inspectError.message : String(inspectError))
    } finally {
      setDetailLoading(false)
    }
  }

  function openAction(action: AiModelRuntimeAction, modelName?: string): void {
    if (!runtime) return
    setActionRequest({ id: crypto.randomUUID(), runtime, action, ...(modelName ? { modelName } : {}) })
  }

  useEffect(() => { void scan() }, [])

  const runtime = snapshot?.runtimes.find((candidate) => candidate.id === selectedRuntimeId) ?? snapshot?.runtime
  const stats = summarizeRuntimeModels(runtime?.models ?? [])

  return <section className="tool-page p5-tool ai-model-runtime-manager">
    <ToolPageHeader title={t('modelRuntime.title')} actions={<><span className="ai-readonly-badge"><ShieldCheck size={14} />{t('modelRuntime.action.controlled')}</span><button className="toolbar-button" type="button" disabled={loading} onClick={() => { void scan() }}><RefreshCw size={14} className={loading ? 'spin' : undefined} />{t('common.refresh')}</button></>} />
    <div className="local-tool-shell ai-model-runtime-shell"><div className="ai-model-runtime-scroll">
      {error && <div className="ai-error" role="alert"><CircleAlert size={16} /><span>{error}</span></div>}
      {loading && !snapshot ? <div className="history-empty">{t('modelRuntime.loading')}</div> : snapshot && runtime && <>
        <nav className="ai-runtime-selector" aria-label={t('modelRuntime.title')}>{snapshot.runtimes.map((candidate) => <button type="button" className={candidate.id === runtime.id ? 'is-active' : ''} onClick={() => setSelectedRuntimeId(candidate.id)} key={candidate.id}><span>{aiModelRuntimeNames[candidate.id]}</span><Health health={candidate.health} /></button>)}</nav>
        <section className={`ai-runtime-overview ai-runtime-overview--${runtime.health}`}>
          <span><Boxes size={24} /></span><div><header><strong>{runtime.name}{runtime.apiVersion ? ` ${runtime.apiVersion}` : ''}</strong><Health health={runtime.health} /></header><code>{runtime.endpoint}</code><small>{runtime.binaryPath ?? t('modelRuntime.binaryMissing')}{runtime.cliVersion ? ` · CLI ${runtime.cliVersion}` : ''}</small></div>
        </section>

        <div className="ai-manager-metrics"><AiMetric label={t('modelRuntime.metric.models')} value={stats.models} /><AiMetric label={t('modelRuntime.metric.running')} value={stats.runningModels} /><AiMetric label={t('modelRuntime.metric.disk')} value={formatBytes(stats.totalModelBytes)} /><AiMetric label={t('modelRuntime.metric.available')} value={formatOptionalBytes(runtime.modelDirectoryAvailableBytes)} /></div>

        <div className="ai-model-runtime-grid">
          <section className="ai-runtime-info-card"><h2><Cpu size={15} />{t('modelRuntime.machine.title')}</h2><dl><Info label={t('modelRuntime.machine.cpu')} value={snapshot.resources.cpuModel} /><Info label={t('modelRuntime.machine.arch')} value={`${snapshot.resources.platform} · ${snapshot.resources.architecture}`} /><Info label={t('modelRuntime.machine.memory')} value={`${formatBytes(snapshot.resources.freeMemoryBytes)} / ${formatBytes(snapshot.resources.totalMemoryBytes)}`} /><Info label={t('modelRuntime.machine.modelDirectory')} value={runtime.modelDirectory ?? '—'} code /></dl>{snapshot.resources.cpuOnly && <div className="ai-runtime-notice"><Gauge size={15} /><span>{t('modelRuntime.machine.intelCpuOnly')}</span></div>}</section>
          <section className="ai-runtime-info-card"><h2><Network size={15} />{t('modelRuntime.endpoint.title')}</h2><dl><Info label={t('modelRuntime.endpoint.address')} value={runtime.endpoint} code /><Info label={t('modelRuntime.endpoint.exposure')} value={t(`modelRuntime.exposure.${runtime.exposure}` as 'modelRuntime.exposure.loopback')} /><Info label={t('modelRuntime.endpoint.protocols')} value={runtime.protocols.map((protocol) => protocolLabel(protocol)).join(' · ')} /><Info label={t('modelRuntime.endpoint.latency')} value={runtime.responseTimeMs === undefined ? '—' : `${runtime.responseTimeMs} ms`} /></dl></section>
        </div>

        {runtime.diagnostics.length > 0 && <section className="ai-runtime-diagnostics">{runtime.diagnostics.map((diagnostic, index) => <div className={`ai-runtime-diagnostic ai-runtime-diagnostic--${diagnostic.severity}`} key={`${diagnostic.code}-${index}`}>{diagnostic.severity === 'info' ? <Box size={15} /> : <ShieldAlert size={15} />}<div><strong>{t(`modelRuntime.diagnostic.${diagnostic.code}` as 'modelRuntime.diagnostic.RUNTIME_NOT_INSTALLED')}</strong><p>{diagnostic.message}</p></div></div>)}</section>}

        <section className="ai-runtime-models"><header><div><h2>{t('modelRuntime.models.title')}</h2><p>{t('modelRuntime.models.description')}</p></div><div className="ai-runtime-model-toolbar"><span><MemoryStick size={14} />{formatBytes(stats.loadedBytes)}{stats.vramBytes > 0 ? ` · VRAM ${formatBytes(stats.vramBytes)}` : ''}</span>{supportedAiModelRuntimeActions(runtime.id).includes('pull') && ['healthy', 'degraded'].includes(runtime.health) && <button className="toolbar-button toolbar-button--primary" type="button" onClick={() => openAction('pull')}><CloudDownload size={14} />{t('modelRuntime.action.pull')}</button>}</div></header>
          {runtime.models.length > 0 ? <div className="ai-runtime-model-list">{runtime.models.map((model) => <ModelCard model={model} key={`${model.name}-${model.digest}`} />)}</div> : <div className="history-empty">{runtime.health === 'notInstalled' ? t('modelRuntime.notInstalledGuide') : runtime.health === 'stopped' ? t('modelRuntime.stoppedGuide') : t('modelRuntime.models.empty')}</div>}
        </section>
      </>}
    </div></div>

    <Dialog title={t('modelRuntime.detail.title', { model: detailModel })} open={Boolean(detailModel)} width={720} onClose={() => { if (!detailLoading) setDetailModel('') }} footer={<button className="dialog-button" type="button" disabled={detailLoading} onClick={() => setDetailModel('')}>{t('common.close')}</button>}>
      {detailLoading ? <div className="history-empty">{t('modelRuntime.detail.loading')}</div> : detailError ? <div className="ai-error" role="alert"><CircleAlert size={16} /><span>{detailError}</span></div> : detail && <div className="ai-runtime-detail"><div className="ai-runtime-detail-grid"><Info label={t('modelRuntime.detail.format')} value={detail.format ?? '—'} /><Info label={t('modelRuntime.detail.family')} value={detail.family ?? '—'} /><Info label={t('modelRuntime.detail.parameters')} value={detail.parameterSize ?? '—'} /><Info label={t('modelRuntime.detail.quantization')} value={detail.quantization ?? '—'} /><Info label={t('modelRuntime.detail.context')} value={detail.contextLength?.toLocaleString() ?? '—'} /><Info label={t('modelRuntime.detail.modified')} value={detail.modifiedAt ? new Date(detail.modifiedAt).toLocaleString() : '—'} /></div><section><h3>{t('modelRuntime.detail.capabilities')}</h3><div className="ai-runtime-capabilities">{detail.capabilities.length > 0 ? detail.capabilities.map((capability) => <span key={capability}>{capability}</span>) : <span>—</span>}</div></section>{detail.parameterText && <section><h3>{t('modelRuntime.detail.parameterText')}</h3><pre>{detail.parameterText}</pre></section>}{detail.licenseExcerpt && <section><h3>{t('modelRuntime.detail.license')}</h3><pre>{detail.licenseExcerpt}</pre></section>}</div>}
    </Dialog>
    <ModelRuntimeActionDialog request={actionRequest} onClose={() => setActionRequest(null)} onCompleted={() => { void scan() }} />
  </section>

  function ModelCard({ model }: { model: AiLocalModelRuntimeModel }) {
    const actions = runtime ? supportedAiModelRuntimeActions(runtime.id) : []
    return <article className={model.running ? 'ai-runtime-model-card ai-runtime-model-card--running' : 'ai-runtime-model-card'}><span className="ai-runtime-model-icon"><Database size={18} /></span><div><header><strong>{model.name}</strong>{model.running && <em>{t('modelRuntime.models.running')}</em>}</header><p>{[model.parameterSize, model.quantization, model.format, model.family].filter(Boolean).join(' · ') || t('modelRuntime.models.metadataUnknown')}</p><code title={model.digest}>{shortDigest(model.digest)}</code><footer><span><HardDrive size={12} />{formatBytes(model.sizeBytes)}</span>{model.contextLength && <span>{t('modelRuntime.models.context', { count: model.contextLength.toLocaleString() })}</span>}{model.expiresAt && <span>{t('modelRuntime.models.unloads', { time: new Date(model.expiresAt).toLocaleTimeString() })}</span>}</footer></div><div className="ai-runtime-model-actions">{model.running && actions.includes('unload') ? <button className="icon-button" type="button" aria-label={t('modelRuntime.action.unload')} title={t('modelRuntime.action.unload')} onClick={() => openAction('unload', model.name)}><PowerOff size={14} /></button> : actions.includes('load') && <button className="icon-button" type="button" aria-label={t('modelRuntime.action.load')} title={t('modelRuntime.action.load')} onClick={() => openAction('load', model.name)}><Power size={14} /></button>}{actions.includes('delete') && <button className="icon-button" type="button" aria-label={t('modelRuntime.action.delete')} title={t('modelRuntime.action.delete')} onClick={() => openAction('delete', model.name)}><Trash2 size={14} /></button>}<button className="icon-button" type="button" aria-label={t('modelRuntime.models.inspect', { model: model.name })} onClick={() => { void inspectModel(model) }}><ZoomIn size={14} /></button></div></article>
  }

  function Health({ health }: { health: AiModelRuntimeHealth }) {
    return <span className={`ai-health ai-health--${health === 'healthy' ? 'healthy' : health === 'notInstalled' ? 'missing' : 'warning'}`}>{t(`modelRuntime.health.${health}` as 'modelRuntime.health.healthy')}</span>
  }

  function protocolLabel(protocol: AiModelRuntimeSnapshot['runtime']['protocols'][number]): string {
    return t(`modelRuntime.protocol.${protocol}` as 'modelRuntime.protocol.ollamaNative')
  }
}

function Info({ label, value, code = false }: { label: string; value: string; code?: boolean }) {
  return <div><dt>{label}</dt>{code ? <dd><code title={value}>{value}</code></dd> : <dd title={value}>{value}</dd>}</div>
}

function formatBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const index = Math.min(units.length - 1, Math.floor(Math.log(bytes) / Math.log(1024)))
  return `${(bytes / 1024 ** index).toFixed(index === 0 ? 0 : 1)} ${units[index]}`
}

function formatOptionalBytes(bytes?: number): string {
  return bytes === undefined ? '—' : formatBytes(bytes)
}

function shortDigest(value: string): string {
  if (!value) return '—'
  return value.length > 24 ? `${value.slice(0, 12)}…${value.slice(-8)}` : value
}

function summarizeRuntimeModels(models: AiLocalModelRuntimeModel[]) {
  return {
    models: models.length,
    runningModels: models.filter((model) => model.running).length,
    totalModelBytes: models.reduce((sum, model) => sum + model.sizeBytes, 0),
    loadedBytes: models.reduce((sum, model) => sum + (model.loadedSizeBytes ?? 0), 0),
    vramBytes: models.reduce((sum, model) => sum + (model.vramSizeBytes ?? 0), 0)
  }
}
