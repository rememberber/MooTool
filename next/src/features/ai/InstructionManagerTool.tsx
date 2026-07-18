import { BookOpenCheck, CheckCircle2, CircleAlert, FileText, GitCompareArrows, ListOrdered, LocateFixed, RotateCcw, ShieldCheck, Sparkles } from 'lucide-react'
import { useState } from 'react'
import { AiManagerBody, AiManagerHeader, AiMetric } from './AiManagerChrome'
import { useAiInventory } from './useAiInventory'
import { aiClientIds, aiClientNames, type AiArtifact, type AiClientId } from '@/shared/contracts/ai'
import type { AiChangeApplyResult, AiChangePlan } from '@/shared/contracts/aiChanges'
import type { AiInstructionPreview } from '@/shared/contracts/aiInstructions'
import { Dialog } from '@/shared/components/Dialog'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function InstructionManagerTool() {
  const { t } = useI18n()
  const inventory = useAiInventory()
  const instructions = inventory.snapshot?.artifacts.filter((artifact) => artifact.kind === 'instruction') ?? []
  const tokenTotal = instructions.reduce((sum, instruction) => sum + metadataNumber(instruction, 'estimatedTokens'), 0)
  const diagnostics = inventory.snapshot?.diagnostics.filter((diagnostic) => diagnostic.code.startsWith('INSTRUCTION_')) ?? []
  const [plan, setPlan] = useState<AiChangePlan | null>(null)
  const [applied, setApplied] = useState<AiChangeApplyResult | null>(null)
  const [changeBusy, setChangeBusy] = useState(false)
  const [changeError, setChangeError] = useState('')
  const [effectiveClient, setEffectiveClient] = useState<'' | AiClientId>('')
  const [effectivePreview, setEffectivePreview] = useState<AiInstructionPreview | null>(null)
  const [effectiveBusy, setEffectiveBusy] = useState(false)
  const [effectiveError, setEffectiveError] = useState('')
  const hasProjectAgents = instructions.some((item) => item.clientId === 'codex' && item.scope === 'project' && item.name === 'AGENTS.md')
  const hasProjectClaude = instructions.some((item) => item.clientId === 'claudeCode' && item.scope === 'project' && item.name === 'CLAUDE.md')

  async function previewCompatibilityEntry(): Promise<void> {
    if (!inventory.snapshot?.projectRoot) return
    setChangeBusy(true)
    setChangeError('')
    setApplied(null)
    try {
      setPlan(await window.mootool.previewClaudeCompatibilityEntry(inventory.snapshot.projectRoot))
    } catch (error) {
      setChangeError(error instanceof Error ? error.message : String(error))
    } finally {
      setChangeBusy(false)
    }
  }

  async function applyCompatibilityEntry(): Promise<void> {
    if (!plan) return
    setChangeBusy(true)
    setChangeError('')
    try {
      setApplied(await window.mootool.applyClaudeCompatibilityEntry(plan.id))
      await inventory.scan()
    } catch (error) {
      setChangeError(error instanceof Error ? error.message : String(error))
    } finally {
      setChangeBusy(false)
    }
  }

  async function rollbackCompatibilityEntry(): Promise<void> {
    if (!applied) return
    setChangeBusy(true)
    setChangeError('')
    try {
      await window.mootool.rollbackClaudeCompatibilityEntry(applied.snapshotId)
      setApplied(null)
      setPlan(null)
      await inventory.scan()
    } catch (error) {
      setChangeError(error instanceof Error ? error.message : String(error))
    } finally {
      setChangeBusy(false)
    }
  }

  async function previewEffectiveInstructions(): Promise<void> {
    const projectRoot = inventory.snapshot?.projectRoot
    if (!projectRoot) return
    const targetPath = await window.mootool.chooseDirectory(projectRoot)
    if (!targetPath) return
    setEffectiveBusy(true)
    setEffectiveError('')
    try {
      setEffectivePreview(await window.mootool.previewEffectiveInstructions({
        projectRoot,
        targetPath,
        ...(effectiveClient ? { clientId: effectiveClient } : {})
      }))
    } catch (error) {
      setEffectiveError(error instanceof Error ? error.message : String(error))
    } finally {
      setEffectiveBusy(false)
    }
  }

  return (
    <section className="tool-page p5-tool ai-asset-manager">
      <AiManagerHeader title={t('instructionManager.title')} snapshot={inventory.snapshot} loading={inventory.loading} onChooseProject={() => { void inventory.chooseProject() }} onRefresh={() => { void inventory.scan() }} />
      <AiManagerBody snapshot={inventory.snapshot} loading={inventory.loading} error={inventory.error}>
        <div className="ai-manager-metrics">
          <AiMetric label={t('instructionManager.metric.total')} value={instructions.length} />
          <AiMetric label={t('instructionManager.metric.project')} value={instructions.filter((item) => item.scope === 'project').length} />
          <AiMetric label={t('instructionManager.metric.tokens')} value={tokenTotal.toLocaleString()} />
          <AiMetric label={t('instructionManager.metric.conflicts')} value={diagnostics.length} />
        </div>
        {inventory.snapshot?.projectRoot && <section className="ai-effective-card"><span><LocateFixed size={20} /></span><div><strong>{t('instructionManager.effective.title')}</strong><p>{t('instructionManager.effective.description')}</p>{effectiveError && <small>{effectiveError}</small>}</div><label><span>{t('instructionManager.effective.client')}</span><select aria-label={t('instructionManager.effective.client')} value={effectiveClient} onChange={(event) => setEffectiveClient(event.target.value as '' | AiClientId)}><option value="">{t('instructionManager.effective.allClients')}</option>{aiClientIds.map((clientId) => <option value={clientId} key={clientId}>{aiClientNames[clientId]}</option>)}</select></label><button className="toolbar-button" type="button" disabled={effectiveBusy} onClick={() => { void previewEffectiveInstructions() }}><ListOrdered size={14} />{effectiveBusy ? t('common.processing') : t('instructionManager.effective.chooseTarget')}</button></section>}
        {inventory.snapshot?.projectRoot && <section className="ai-compatibility-card"><span><GitCompareArrows size={20} /></span><div><strong>{t('instructionManager.compatibility.title')}</strong><p>{hasProjectClaude ? t('instructionManager.compatibility.exists') : hasProjectAgents ? t('instructionManager.compatibility.description') : t('instructionManager.compatibility.requiresAgents')}</p>{changeError && !plan && <small>{changeError}</small>}</div><button className="toolbar-button" type="button" disabled={changeBusy || !hasProjectAgents || hasProjectClaude} onClick={() => { void previewCompatibilityEntry() }}>{t('instructionManager.compatibility.preview')}</button></section>}
        {instructions.length > 0 ? <div className="ai-asset-list">{instructions.map((instruction) => <InstructionCard artifact={instruction} key={instruction.id} />)}</div> : <div className="history-empty">{t('instructionManager.empty')}</div>}
        {diagnostics.length > 0 && <section className="ai-manager-diagnostics"><h2>{t('ai.section.diagnostics')}</h2>{diagnostics.map((diagnostic) => <div key={diagnostic.id}><CircleAlert size={14} /><span>{t(`ai.diagnostic.${diagnostic.code}` as 'ai.diagnostic.INSTRUCTION_CONFLICT')}</span></div>)}</section>}
      </AiManagerBody>
      <Dialog
        title={t('instructionManager.change.title')}
        open={Boolean(plan)}
        width={780}
        onClose={() => { if (!changeBusy) { setPlan(null); setApplied(null); setChangeError('') } }}
        footer={applied ? (
          <><button className="dialog-button" type="button" disabled={changeBusy} onClick={() => { setPlan(null); setApplied(null) }}>{t('common.close')}</button><button className="dialog-button dialog-button--danger" type="button" disabled={changeBusy} onClick={() => { void rollbackCompatibilityEntry() }}><RotateCcw size={14} />{t('instructionManager.change.rollback')}</button></>
        ) : (
          <><button className="dialog-button" type="button" disabled={changeBusy} onClick={() => setPlan(null)}>{t('common.cancel')}</button><button className="dialog-button dialog-button--primary" type="button" disabled={changeBusy || !plan} onClick={() => { void applyCompatibilityEntry() }}><ShieldCheck size={14} />{changeBusy ? t('common.processing') : t('instructionManager.change.apply')}</button></>
        )}
      >
        <div className="ai-change-preview">
          <div className="ai-change-safety"><ShieldCheck size={16} /><span>{t('instructionManager.change.safety')}</span></div>
          {applied && <div className="ai-change-success"><CheckCircle2 size={16} /><span>{t('instructionManager.change.applied')}</span></div>}
          {changeError && <div className="ai-error" role="alert"><CircleAlert size={16} /><span>{changeError}</span></div>}
          {plan?.operations.map((operation) => <section key={operation.id}><header><strong>{operation.summary}</strong><code>{operation.targetPath}</code></header><pre>{operation.redactedDiff}</pre></section>)}
        </div>
      </Dialog>
      <Dialog
        title={t('instructionManager.effective.dialogTitle')}
        open={Boolean(effectivePreview)}
        width={780}
        onClose={() => setEffectivePreview(null)}
        footer={<button className="dialog-button" type="button" onClick={() => setEffectivePreview(null)}>{t('common.close')}</button>}
      >
        {effectivePreview && <div className="ai-effective-preview">
          <div className="ai-effective-summary"><div><span>{t('instructionManager.effective.target')}</span><code>{effectivePreview.targetPath}</code></div><strong>{t('instructionManager.effective.tokensTotal', { count: effectivePreview.totalEstimatedTokens.toLocaleString() })}</strong></div>
          {effectivePreview.instructions.length > 0 ? <ol>{effectivePreview.instructions.map((instruction) => <li key={`${instruction.clientId}:${instruction.path}`}><span>{instruction.order}</span><div><header><strong>{instruction.name}</strong><em>{aiClientNames[instruction.clientId]}</em><em>{t(`instructionManager.effective.reason.${instruction.reason}` as 'instructionManager.effective.reason.userScope')}</em></header><code>{instruction.path}</code><small>{t('instructionManager.tokens', { count: instruction.estimatedTokens.toLocaleString() })}</small></div></li>)}</ol> : <div className="history-empty">{t('instructionManager.effective.empty')}</div>}
        </div>}
      </Dialog>
    </section>
  )

  function InstructionCard({ artifact }: { artifact: AiArtifact }) {
    const appliesTo = metadataString(artifact, 'appliesTo')
    return (
      <article className="ai-asset-card">
        <span className="ai-asset-icon"><BookOpenCheck size={18} /></span>
        <div className="ai-asset-content">
          <header><strong>{artifact.name}</strong><span>{aiClientNames[artifact.clientId]}</span><span>{artifact.scope === 'project' ? t('ai.scope.project') : t('ai.scope.user')}</span></header>
          <p>{t('instructionManager.appliesTo', { path: appliesTo || '—' })}</p>
          <code title={artifact.path}>{artifact.path}</code>
          <footer>
            <span><FileText size={13} />{t('instructionManager.lines', { count: String(metadataNumber(artifact, 'lineCount')) })}</span>
            <span><Sparkles size={13} />{t('instructionManager.tokens', { count: metadataNumber(artifact, 'estimatedTokens').toLocaleString() })}</span>
          </footer>
        </div>
      </article>
    )
  }
}

function metadataString(artifact: AiArtifact, key: string): string {
  const value = artifact.metadata?.[key]
  return typeof value === 'string' ? value : ''
}

function metadataNumber(artifact: AiArtifact, key: string): number {
  const value = artifact.metadata?.[key]
  return typeof value === 'number' ? value : 0
}
