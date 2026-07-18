import { CheckCircle2, CircleAlert, CopyPlus, FileCode2, FolderInput, FolderTree, RotateCcw, ShieldCheck, Sparkles, TerminalSquare } from 'lucide-react'
import { useState } from 'react'
import { AiManagerBody, AiManagerHeader, AiMetric, formatAssetBytes } from './AiManagerChrome'
import { useAiInventory } from './useAiInventory'
import { aiClientNames, type AiArtifact, type AiPrimaryClientId } from '@/shared/contracts/ai'
import type { AiChangeApplyResult } from '@/shared/contracts/aiChanges'
import type { AiSkillInstallPreview, AiSkillInstallScope } from '@/shared/contracts/aiSkills'
import { Dialog } from '@/shared/components/Dialog'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function SkillManagerTool() {
  const { t } = useI18n()
  const inventory = useAiInventory()
  const skills = inventory.snapshot?.artifacts.filter((artifact) => artifact.kind === 'skill') ?? []
  const tokenTotal = skills.reduce((sum, skill) => sum + metadataNumber(skill, 'estimatedTokens'), 0)
  const relevantDiagnostics = inventory.snapshot?.diagnostics.filter((diagnostic) => diagnostic.code.startsWith('SKILL_') || (diagnostic.code === 'PLAINTEXT_SECRET_RISK' && skills.some((skill) => diagnostic.path?.startsWith(skill.path)))) ?? []
  const [installerOpen, setInstallerOpen] = useState(false)
  const [sourceDirectory, setSourceDirectory] = useState('')
  const [targetClientId, setTargetClientId] = useState<AiPrimaryClientId>('codex')
  const [targetScope, setTargetScope] = useState<AiSkillInstallScope>('user')
  const [preview, setPreview] = useState<AiSkillInstallPreview | null>(null)
  const [applied, setApplied] = useState<AiChangeApplyResult | null>(null)
  const [confirmRisks, setConfirmRisks] = useState(false)
  const [installBusy, setInstallBusy] = useState(false)
  const [installError, setInstallError] = useState('')

  async function openInstaller(artifact?: AiArtifact): Promise<void> {
    const source = artifact?.path ?? await window.mootool.chooseDirectory()
    if (!source) return
    setSourceDirectory(source)
    setTargetClientId(artifact?.clientId === 'codex' ? 'claudeCode' : 'codex')
    setTargetScope(artifact?.scope === 'project' && inventory.snapshot?.projectRoot ? 'project' : 'user')
    setPreview(null)
    setApplied(null)
    setConfirmRisks(false)
    setInstallError('')
    setInstallerOpen(true)
  }

  async function inspectInstall(): Promise<void> {
    setInstallBusy(true)
    setInstallError('')
    setPreview(null)
    setConfirmRisks(false)
    try {
      setPreview(await window.mootool.previewSkillInstall({
        sourceDirectory,
        targetClientId,
        scope: targetScope,
        projectRoot: targetScope === 'project' ? inventory.snapshot?.projectRoot : undefined
      }))
    } catch (error) {
      setInstallError(error instanceof Error ? error.message : String(error))
    } finally {
      setInstallBusy(false)
    }
  }

  async function applyInstall(): Promise<void> {
    if (!preview) return
    setInstallBusy(true)
    setInstallError('')
    try {
      setApplied(await window.mootool.applySkillInstall({ planId: preview.plan.id, confirmRisks }))
      await inventory.scan()
    } catch (error) {
      setInstallError(error instanceof Error ? error.message : String(error))
    } finally {
      setInstallBusy(false)
    }
  }

  async function rollbackInstall(): Promise<void> {
    if (!applied) return
    setInstallBusy(true)
    setInstallError('')
    try {
      await window.mootool.rollbackSkillInstall(applied.snapshotId)
      setInstallerOpen(false)
      resetInstaller()
      await inventory.scan()
    } catch (error) {
      setInstallError(error instanceof Error ? error.message : String(error))
    } finally {
      setInstallBusy(false)
    }
  }

  function resetInstaller(): void {
    setPreview(null)
    setApplied(null)
    setConfirmRisks(false)
    setInstallError('')
  }

  return (
    <section className="tool-page p5-tool ai-asset-manager">
      <AiManagerHeader title={t('skillManager.title')} snapshot={inventory.snapshot} loading={inventory.loading} onChooseProject={() => { void inventory.chooseProject() }} onRefresh={() => { void inventory.scan() }} extraActions={<button className="toolbar-button" type="button" disabled={inventory.loading} onClick={() => { void openInstaller() }}><FolderInput size={14} />{t('skillManager.install')}</button>} />
      <AiManagerBody snapshot={inventory.snapshot} loading={inventory.loading} error={inventory.error}>
        <div className="ai-manager-metrics">
          <AiMetric label={t('skillManager.metric.total')} value={skills.length} />
          <AiMetric label={t('skillManager.metric.project')} value={skills.filter((skill) => skill.scope === 'project').length} />
          <AiMetric label={t('skillManager.metric.tokens')} value={tokenTotal.toLocaleString()} />
          <AiMetric label={t('skillManager.metric.risks')} value={relevantDiagnostics.length} />
        </div>
        {skills.length > 0 ? <div className="ai-asset-list">{skills.map((skill) => <SkillCard artifact={skill} key={skill.id} />)}</div> : <div className="history-empty">{t('skillManager.empty')}</div>}
        {relevantDiagnostics.length > 0 && <section className="ai-manager-diagnostics"><h2>{t('ai.section.diagnostics')}</h2>{relevantDiagnostics.map((diagnostic) => <div key={diagnostic.id}><CircleAlert size={14} /><span>{t(`ai.diagnostic.${diagnostic.code}` as 'ai.diagnostic.SKILL_ENTRY_INVALID')}</span></div>)}</section>}
      </AiManagerBody>
      <Dialog
        title={t('skillManager.install.title')}
        open={installerOpen}
        width={820}
        onClose={() => { if (!installBusy) { setInstallerOpen(false); resetInstaller() } }}
        footer={applied ? (
          <><button className="dialog-button" type="button" disabled={installBusy} onClick={() => { setInstallerOpen(false); resetInstaller() }}>{t('common.close')}</button><button className="dialog-button dialog-button--danger" type="button" disabled={installBusy} onClick={() => { void rollbackInstall() }}><RotateCcw size={14} />{t('skillManager.install.rollback')}</button></>
        ) : preview ? (
          <><button className="dialog-button" type="button" disabled={installBusy} onClick={() => { setPreview(null); setInstallError('') }}>{t('skillManager.install.back')}</button><button className="dialog-button dialog-button--primary" type="button" disabled={installBusy || (preview.requiresRiskConfirmation && !confirmRisks)} onClick={() => { void applyInstall() }}><ShieldCheck size={14} />{installBusy ? t('common.processing') : t('skillManager.install.apply')}</button></>
        ) : (
          <><button className="dialog-button" type="button" disabled={installBusy} onClick={() => setInstallerOpen(false)}>{t('common.cancel')}</button><button className="dialog-button dialog-button--primary" type="button" disabled={installBusy || !sourceDirectory || (targetScope === 'project' && !inventory.snapshot?.projectRoot)} onClick={() => { void inspectInstall() }}><ShieldCheck size={14} />{installBusy ? t('common.processing') : t('skillManager.install.inspect')}</button></>
        )}
      >
        <div className="ai-skill-installer">
          {!preview && <div className="ai-skill-install-fields">
            <label><span>{t('skillManager.install.source')}</span><code title={sourceDirectory}>{sourceDirectory}</code></label>
            <label><span>{t('skillManager.install.client')}</span><select value={targetClientId} onChange={(event) => setTargetClientId(event.target.value as AiPrimaryClientId)}><option value="codex">Codex</option><option value="claudeCode">Claude Code</option></select></label>
            <label><span>{t('skillManager.install.scope')}</span><select value={targetScope} onChange={(event) => setTargetScope(event.target.value as AiSkillInstallScope)}><option value="user">{t('ai.scope.user')}</option><option value="project" disabled={!inventory.snapshot?.projectRoot}>{t('ai.scope.project')}</option></select></label>
          </div>}
          {installError && <div className="ai-error" role="alert"><CircleAlert size={16} /><span>{installError}</span></div>}
          {preview && <>
            <div className="ai-skill-install-summary"><Sparkles size={18} /><div><strong>{preview.name}</strong><p>{preview.description}</p><code title={preview.targetDirectory}>{preview.targetDirectory}</code></div><span>{preview.files.length} · {formatAssetBytes(preview.totalSizeBytes)}</span></div>
            {applied && <div className="ai-change-success"><CheckCircle2 size={16} /><span>{t('skillManager.install.applied')}</span></div>}
            {preview.findings.length > 0 && <div className="ai-skill-install-risks">{preview.findings.map((finding, index) => <div key={`${finding.code}-${finding.relativePath}-${index}`}><CircleAlert size={14} /><span>{t(`ai.diagnostic.${finding.code}` as 'ai.diagnostic.SKILL_DANGEROUS_PATTERN')}{finding.relativePath ? ` · ${finding.relativePath}` : ''}</span></div>)}</div>}
            <div className="ai-skill-install-files">{preview.files.map((file, index) => <details key={file.relativePath}><summary><span>{file.relativePath}</span><small>{file.binary ? t('skillManager.install.binary') : formatAssetBytes(file.sizeBytes)}{file.executable ? ` · ${t('skillManager.install.executable')}` : ''}</small></summary>{preview.plan.operations[index] && <pre>{preview.plan.operations[index].redactedDiff}</pre>}</details>)}</div>
            {preview.requiresRiskConfirmation && !applied && <label className="ai-skill-risk-confirm"><input type="checkbox" checked={confirmRisks} onChange={(event) => setConfirmRisks(event.target.checked)} /><span>{t('skillManager.install.confirmRisks')}</span></label>}
          </>}
        </div>
      </Dialog>
    </section>
  )

  function SkillCard({ artifact }: { artifact: AiArtifact }) {
    const fileCount = metadataNumber(artifact, 'fileCount')
    const size = metadataNumber(artifact, 'totalSizeBytes')
    const tokens = metadataNumber(artifact, 'estimatedTokens')
    const description = metadataString(artifact, 'description')
    return (
      <article className="ai-asset-card">
        <span className="ai-asset-icon"><Sparkles size={18} /></span>
        <div className="ai-asset-content">
          <header><strong>{artifact.name}</strong><span>{aiClientNames[artifact.clientId]}</span><span>{artifact.scope === 'project' ? t('ai.scope.project') : t('ai.scope.user')}</span>{artifact.source === 'legacy' && <span>{t('skillManager.legacy')}</span>}</header>
          <p>{description || t('skillManager.noDescription')}</p>
          <code title={artifact.path}>{artifact.path}</code>
          <footer>
            <span><FolderTree size={13} />{t('skillManager.files', { count: String(fileCount) })}</span>
            <span><FileCode2 size={13} />{formatAssetBytes(size)}</span>
            <span><Sparkles size={13} />{t('skillManager.tokens', { count: tokens.toLocaleString() })}</span>
            {artifact.metadata?.hasScripts === true && <span><TerminalSquare size={13} />{t('skillManager.scripts')}</span>}
            <button className="ai-asset-copy" type="button" onClick={() => { void openInstaller(artifact) }}><CopyPlus size={13} />{t('skillManager.copy')}</button>
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
