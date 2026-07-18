import { Cable, CheckCircle2, CircleAlert, CopyPlus, KeyRound, Network, RotateCcw, Server, ShieldCheck, Square, TerminalSquare, TestTube2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import { AiManagerBody, AiManagerHeader, AiMetric } from './AiManagerChrome'
import { useAiInventory } from './useAiInventory'
import type { AiChangeApplyResult } from '@/shared/contracts/aiChanges'
import { aiClientNames, type AiPrimaryClientId, type AiScope } from '@/shared/contracts/ai'
import type { AiMcpCopyPreview, AiMcpInventory, AiMcpProbeResult, AiMcpRisk, AiMcpServer } from '@/shared/contracts/aiMcp'
import { Dialog } from '@/shared/components/Dialog'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function McpManagerTool() {
  const { t } = useI18n()
  const aiInventory = useAiInventory()
  const [inventory, setInventory] = useState<AiMcpInventory | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [copySource, setCopySource] = useState<AiMcpServer | null>(null)
  const [targetClientId, setTargetClientId] = useState<AiPrimaryClientId>('claudeCode')
  const [targetScope, setTargetScope] = useState<AiScope>('user')
  const [preview, setPreview] = useState<AiMcpCopyPreview | null>(null)
  const [applied, setApplied] = useState<AiChangeApplyResult | null>(null)
  const [copyBusy, setCopyBusy] = useState(false)
  const [copyError, setCopyError] = useState('')
  const [confirmedMappings, setConfirmedMappings] = useState(false)
  const [probeServer, setProbeServer] = useState<AiMcpServer | null>(null)
  const [probeConfirmed, setProbeConfirmed] = useState(false)
  const [probeResult, setProbeResult] = useState<AiMcpProbeResult | null>(null)
  const [probeBusy, setProbeBusy] = useState(false)
  const [probeError, setProbeError] = useState('')
  const [probeRequestId, setProbeRequestId] = useState('')

  async function loadMcp(projectRoot = aiInventory.snapshot?.projectRoot): Promise<void> {
    setLoading(true)
    setError('')
    try {
      setInventory(await window.mootool.getMcpInventory(projectRoot ? { projectRoot } : undefined))
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : String(loadError))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (aiInventory.snapshot) void loadMcp(aiInventory.snapshot.projectRoot)
  }, [aiInventory.snapshot?.scannedAt])

  function openCopy(server: AiMcpServer): void {
    setCopySource(server)
    setTargetClientId(server.clientId === 'codex' ? 'claudeCode' : 'codex')
    setTargetScope(aiInventory.snapshot?.projectRoot ? 'project' : 'user')
    setPreview(null)
    setApplied(null)
    setCopyError('')
    setConfirmedMappings(false)
  }

  function closeCopy(): void {
    setCopySource(null)
    setPreview(null)
    setApplied(null)
    setCopyError('')
    setConfirmedMappings(false)
  }

  async function previewCopy(): Promise<void> {
    if (!copySource) return
    setCopyBusy(true)
    setCopyError('')
    try {
      setPreview(await window.mootool.previewMcpCopy({
        sourceServerId: copySource.id,
        targetClientId,
        targetScope,
        ...(aiInventory.snapshot?.projectRoot ? { projectRoot: aiInventory.snapshot.projectRoot } : {})
      }))
    } catch (copyFailure) {
      setCopyError(copyFailure instanceof Error ? copyFailure.message : String(copyFailure))
    } finally {
      setCopyBusy(false)
    }
  }

  async function applyCopy(): Promise<void> {
    if (!preview) return
    setCopyBusy(true)
    setCopyError('')
    try {
      setApplied(await window.mootool.applyMcpCopy(preview.plan.id))
      await Promise.all([loadMcp(), aiInventory.scan()])
    } catch (copyFailure) {
      setCopyError(copyFailure instanceof Error ? copyFailure.message : String(copyFailure))
    } finally {
      setCopyBusy(false)
    }
  }

  async function rollbackCopy(): Promise<void> {
    if (!applied) return
    setCopyBusy(true)
    setCopyError('')
    try {
      await window.mootool.rollbackMcpCopy(applied.snapshotId)
      closeCopy()
      await Promise.all([loadMcp(), aiInventory.scan()])
    } catch (copyFailure) {
      setCopyError(copyFailure instanceof Error ? copyFailure.message : String(copyFailure))
    } finally {
      setCopyBusy(false)
    }
  }

  function openProbe(server: AiMcpServer): void {
    setProbeServer(server)
    setProbeConfirmed(false)
    setProbeResult(null)
    setProbeError('')
    setProbeRequestId('')
  }

  function closeProbe(): void {
    setProbeServer(null)
    setProbeResult(null)
    setProbeError('')
    setProbeRequestId('')
    setProbeConfirmed(false)
  }

  async function runProbe(): Promise<void> {
    if (!probeServer) return
    const requestId = crypto.randomUUID()
    setProbeRequestId(requestId)
    setProbeBusy(true)
    setProbeError('')
    setProbeResult(null)
    try {
      setProbeResult(await window.mootool.probeMcpServer({
        requestId,
        sourceServerId: probeServer.id,
        confirmCommand: probeServer.transport !== 'stdio' || probeConfirmed,
        ...(aiInventory.snapshot?.projectRoot ? { projectRoot: aiInventory.snapshot.projectRoot } : {})
      }))
    } catch (probeFailure) {
      setProbeError(probeFailure instanceof Error ? probeFailure.message : String(probeFailure))
    } finally {
      setProbeBusy(false)
    }
  }

  async function cancelProbe(): Promise<void> {
    if (!probeRequestId) return
    await window.mootool.cancelMcpProbe(probeRequestId)
  }

  const servers = inventory?.servers ?? []
  const riskCount = servers.filter((server) => server.risks.length > 0).length + (inventory?.invalidConfigPaths.length ?? 0)
  return (
    <section className="tool-page p5-tool ai-asset-manager ai-mcp-manager">
      <AiManagerHeader title={t('mcpManager.title')} snapshot={aiInventory.snapshot} loading={aiInventory.loading || loading} onChooseProject={() => { void aiInventory.chooseProject() }} onRefresh={() => { void aiInventory.scan().then(() => loadMcp()) }} />
      <AiManagerBody snapshot={aiInventory.snapshot} loading={aiInventory.loading || loading} error={aiInventory.error || error}>
        <div className="ai-manager-metrics">
          <AiMetric label={t('mcpManager.metric.total')} value={servers.length} />
          <AiMetric label={t('mcpManager.metric.stdio')} value={servers.filter((server) => server.transport === 'stdio').length} />
          <AiMetric label={t('mcpManager.metric.http')} value={servers.filter((server) => server.transport === 'streamableHttp').length} />
          <AiMetric label={t('mcpManager.metric.risks')} value={riskCount} />
        </div>
        {servers.length > 0 ? <div className="ai-asset-list">{servers.map((server) => <McpCard server={server} key={server.id} onCopy={openCopy} onProbe={openProbe} />)}</div> : <div className="history-empty">{t('mcpManager.empty')}</div>}
        {inventory?.invalidConfigPaths.map((path) => <div className="ai-error" role="alert" key={path}><CircleAlert size={15} /><span>{t('mcpManager.invalidConfig', { path })}</span></div>)}
      </AiManagerBody>
      <Dialog
        title={t('mcpManager.copy.title')}
        open={Boolean(copySource)}
        width={820}
        onClose={() => { if (!copyBusy) closeCopy() }}
        footer={applied ? (
          <><button className="dialog-button" type="button" disabled={copyBusy} onClick={closeCopy}>{t('common.close')}</button><button className="dialog-button dialog-button--danger" type="button" disabled={copyBusy} onClick={() => { void rollbackCopy() }}><RotateCcw size={14} />{t('mcpManager.copy.rollback')}</button></>
        ) : preview ? (
          <><button className="dialog-button" type="button" disabled={copyBusy} onClick={() => { setPreview(null); setConfirmedMappings(false) }}>{t('mcpManager.copy.back')}</button><button className="dialog-button dialog-button--primary" type="button" disabled={copyBusy || (preview.secretMappings.length > 0 && !confirmedMappings)} onClick={() => { void applyCopy() }}><ShieldCheck size={14} />{copyBusy ? t('common.processing') : t('mcpManager.copy.apply')}</button></>
        ) : (
          <><button className="dialog-button" type="button" disabled={copyBusy} onClick={closeCopy}>{t('common.cancel')}</button><button className="dialog-button dialog-button--primary" type="button" disabled={copyBusy || !copySource || (targetScope === 'project' && !aiInventory.snapshot?.projectRoot)} onClick={() => { void previewCopy() }}><CopyPlus size={14} />{copyBusy ? t('common.processing') : t('mcpManager.copy.preview')}</button></>
        )}
      >
        <div className="ai-mcp-copy">
          {!preview && copySource && <div className="ai-mcp-copy-fields"><label><span>{t('mcpManager.copy.source')}</span><strong>{aiClientNames[copySource.clientId]} · {copySource.name}</strong></label><label><span>{t('mcpManager.copy.client')}</span><select value={targetClientId} onChange={(event) => setTargetClientId(event.target.value as AiPrimaryClientId)}><option value="codex">Codex</option><option value="claudeCode">Claude Code</option></select></label><label><span>{t('mcpManager.copy.scope')}</span><select value={targetScope} onChange={(event) => setTargetScope(event.target.value as AiScope)}><option value="user">{t('ai.scope.user')}</option><option value="project" disabled={!aiInventory.snapshot?.projectRoot}>{t('ai.scope.project')}</option></select></label></div>}
          {preview && <>
            <div className="ai-change-safety"><ShieldCheck size={16} /><span>{t('mcpManager.copy.safety')}</span></div>
            {applied && <div className="ai-change-success"><CheckCircle2 size={16} /><span>{t('mcpManager.copy.applied')}</span></div>}
            {preview.secretMappings.length > 0 && <section className="ai-mcp-mappings"><header><KeyRound size={15} /><strong>{t('mcpManager.copy.mappings')}</strong></header>{preview.secretMappings.map((mapping) => <div key={`${mapping.field}:${mapping.environmentVariable}`}><code>{mapping.field}</code><span>→</span><code>{mapping.environmentVariable}</code></div>)}{!applied && <label><input type="checkbox" checked={confirmedMappings} onChange={(event) => setConfirmedMappings(event.target.checked)} /><span>{t('mcpManager.copy.confirmMappings')}</span></label>}</section>}
            {preview.warnings.map((warning) => <div className="ai-warning" key={warning}><CircleAlert size={15} /><span>{t(`mcpManager.copy.warning.${warning}` as 'mcpManager.copy.warning.environmentVariablesRequired')}</span></div>)}
            {preview.plan.operations.map((operation) => <section className="ai-mcp-diff" key={operation.id}><header><strong>{operation.summary}</strong><code>{operation.targetPath}</code></header><pre>{operation.redactedDiff}</pre></section>)}
          </>}
          {copyError && <div className="ai-error" role="alert"><CircleAlert size={16} /><span>{copyError}</span></div>}
        </div>
      </Dialog>
      <Dialog
        title={t('mcpManager.probe.title')}
        open={Boolean(probeServer)}
        width={720}
        onClose={() => { if (!probeBusy) closeProbe() }}
        footer={probeBusy ? (
          <button className="dialog-button dialog-button--danger" type="button" onClick={() => { void cancelProbe() }}><Square size={13} />{t('common.stop')}</button>
        ) : probeResult ? (
          <><button className="dialog-button" type="button" onClick={closeProbe}>{t('common.close')}</button><button className="dialog-button" type="button" onClick={() => { void runProbe() }}><TestTube2 size={14} />{t('mcpManager.probe.retry')}</button></>
        ) : (
          <><button className="dialog-button" type="button" onClick={closeProbe}>{t('common.cancel')}</button><button className="dialog-button dialog-button--primary" type="button" disabled={!probeServer || (probeServer.transport === 'stdio' && !probeConfirmed)} onClick={() => { void runProbe() }}><TestTube2 size={14} />{t('mcpManager.probe.start')}</button></>
        )}
      >
        {probeServer && <div className="ai-mcp-probe">
          <section className="ai-mcp-probe-target"><span>{probeServer.transport === 'stdio' ? <TerminalSquare size={18} /> : <Network size={18} />}</span><div><strong>{probeServer.name}</strong><code>{probeServer.transport === 'stdio' ? [probeServer.command, ...probeServer.args].filter(Boolean).join(' ') : probeServer.url}</code></div></section>
          <div className="ai-change-safety"><ShieldCheck size={16} /><span>{t('mcpManager.probe.safety')}</span></div>
          {probeServer.transport === 'stdio' && !probeResult && <label className="ai-mcp-probe-confirm"><input type="checkbox" checked={probeConfirmed} onChange={(event) => setProbeConfirmed(event.target.checked)} /><span>{t('mcpManager.probe.confirmCommand')}</span></label>}
          {probeBusy && <div className="history-empty">{t('mcpManager.probe.running')}</div>}
          {probeResult && <section className={probeResult.status === 'healthy' ? 'ai-mcp-probe-result ai-mcp-probe-result--healthy' : 'ai-mcp-probe-result ai-mcp-probe-result--error'}><header>{probeResult.status === 'healthy' ? <CheckCircle2 size={17} /> : <CircleAlert size={17} />}<strong>{probeResult.status === 'healthy' ? t('mcpManager.probe.healthy') : probeResult.status === 'cancelled' ? t('mcpManager.probe.cancelled') : t('mcpManager.probe.failed')}</strong><span>{probeResult.latencyMs} ms</span></header>{probeResult.status === 'healthy' && <div className="ai-mcp-capabilities"><span><strong>{probeResult.tools}</strong>{t('mcpManager.probe.tools')}</span><span><strong>{probeResult.resources}</strong>{t('mcpManager.probe.resources')}</span><span><strong>{probeResult.prompts}</strong>{t('mcpManager.probe.prompts')}</span></div>}{probeResult.protocolVersion && <p>{t('mcpManager.probe.protocol', { version: probeResult.protocolVersion })}</p>}{probeResult.executablePath && <p><code>{probeResult.executablePath}</code></p>}{probeResult.executableSha256 && <p><code>SHA-256 {probeResult.executableSha256}</code></p>}{probeResult.message && <p>{probeResult.message}</p>}{probeResult.logs.length > 0 && <pre>{probeResult.logs.join('\n')}</pre>}</section>}
          {probeError && <div className="ai-error" role="alert"><CircleAlert size={16} /><span>{probeError}</span></div>}
        </div>}
      </Dialog>
    </section>
  )

  function McpCard({ server, onCopy, onProbe }: { server: AiMcpServer; onCopy: (server: AiMcpServer) => void; onProbe: (server: AiMcpServer) => void }) {
    const endpoint = server.transport === 'stdio' ? [server.command, ...server.args].filter(Boolean).join(' ') : server.url
    return <article className="ai-asset-card ai-mcp-card"><span className="ai-asset-icon">{server.transport === 'stdio' ? <TerminalSquare size={18} /> : server.transport === 'streamableHttp' ? <Network size={18} /> : <Server size={18} />}</span><div className="ai-asset-content"><header><strong>{server.name}</strong><span>{aiClientNames[server.clientId]}</span><span>{server.scope === 'project' ? t('ai.scope.project') : t('ai.scope.user')}</span><span>{transportLabel(server.transport)}</span></header><code title={endpoint}>{endpoint || '—'}</code><code title={server.configPath}>{server.configPath}</code><footer>{server.environment.length > 0 && <span><KeyRound size={13} />{t('mcpManager.envCount', { count: String(server.environment.length) })}</span>}{server.headers.length > 0 && <span><Cable size={13} />{t('mcpManager.headerCount', { count: String(server.headers.length) })}</span>}{!server.enabled && <span>{t('mcpManager.disabled')}</span>}<span className="ai-mcp-card-actions"><button className="ai-asset-copy" type="button" disabled={server.transport !== 'stdio' && server.transport !== 'streamableHttp'} onClick={() => onProbe(server)}><TestTube2 size={12} />{t('mcpManager.probe.action')}</button><button className="ai-asset-copy" type="button" onClick={() => onCopy(server)}><CopyPlus size={12} />{t('mcpManager.copy.action')}</button></span></footer>{server.risks.length > 0 && <div className="ai-mcp-risks">{server.risks.map((risk) => <span key={risk}><CircleAlert size={11} />{riskLabel(risk)}</span>)}</div>}</div></article>
  }

  function transportLabel(transport: AiMcpServer['transport']): string {
    return t(`mcpManager.transport.${transport}` as 'mcpManager.transport.stdio')
  }

  function riskLabel(risk: AiMcpRisk): string {
    return t(`mcpManager.risk.${risk}` as 'mcpManager.risk.plaintextSecret')
  }
}
