import { ArrowUpRight, Brain, CircleAlert, FolderOpen, Gauge, Info, Layers3, RefreshCw, ScanSearch, Sparkles } from 'lucide-react'
import { useState } from 'react'
import { useAppStore } from '@/app/appStore'
import { AiMetric } from './AiManagerChrome'
import { aiClientIds, aiClientNames, type AiClientId, type AiDoctorSnapshot } from '@/shared/contracts/ai'
import type { AiAgentManagerSnapshot, AiAgentProfile } from '@/shared/contracts/aiAgents'
import type { AiContextCategory, AiContextInspectorSnapshot, AiContextItem, AiContextLayer } from '@/shared/contracts/aiContext'
import { ToolPageHeader } from '@/shared/components/ToolPage'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function ContextInspectorTool() {
  const { t } = useI18n()
  const openTool = useAppStore((state) => state.openTool)
  const [projectRoot, setProjectRoot] = useState('')
  const [targetPath, setTargetPath] = useState('')
  const [clientId, setClientId] = useState<AiClientId>('codex')
  const [agentProfileId, setAgentProfileId] = useState('')
  const [selectedSkills, setSelectedSkills] = useState<string[]>([])
  const [memoryBudget, setMemoryBudget] = useState(2000)
  const [inventory, setInventory] = useState<AiDoctorSnapshot | null>(null)
  const [agents, setAgents] = useState<AiAgentManagerSnapshot | null>(null)
  const [snapshot, setSnapshot] = useState<AiContextInspectorSnapshot | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function chooseProject(): Promise<void> {
    const selected = await window.mootool.chooseDirectory(projectRoot || undefined)
    if (!selected) return
    setProjectRoot(selected)
    setTargetPath(selected)
    setAgentProfileId('')
    setSelectedSkills([])
    await refreshOptions(selected, selected, clientId, '', [])
  }

  async function chooseTarget(): Promise<void> {
    const selected = await window.mootool.chooseDirectory(targetPath || projectRoot || undefined)
    if (!selected) return
    setTargetPath(selected)
    await inspect({ target: selected })
  }

  async function refreshOptions(root = projectRoot, target = targetPath, client = clientId, profileId = agentProfileId, skills = selectedSkills): Promise<void> {
    if (!root) return
    setLoading(true)
    setError('')
    try {
      const [nextInventory, nextAgents] = await Promise.all([
        window.mootool.scanAiEnvironment({ projectRoot: root }),
        window.mootool.getAiAgentManagerSnapshot({ projectRoot: root })
      ])
      setInventory(nextInventory)
      setAgents(nextAgents)
      const nextSnapshot = await window.mootool.inspectAiContext({
        projectRoot: root,
        targetPath: target || root,
        clientId: client,
        ...(profileId ? { agentProfileId: profileId } : {}),
        selectedSkillNames: skills,
        memoryTokenBudget: memoryBudget,
        maxMemoryItems: 20,
        topN: 10
      })
      setSnapshot(nextSnapshot)
    } catch (refreshError) {
      setError(refreshError instanceof Error ? refreshError.message : String(refreshError))
    } finally {
      setLoading(false)
    }
  }

  async function inspect(overrides: { target?: string; client?: AiClientId; profileId?: string; skills?: string[]; budget?: number } = {}): Promise<void> {
    if (!projectRoot) return
    setLoading(true)
    setError('')
    try {
      setSnapshot(await window.mootool.inspectAiContext({
        projectRoot,
        targetPath: overrides.target ?? targetPath ?? projectRoot,
        clientId: overrides.client ?? clientId,
        ...((overrides.profileId ?? agentProfileId) ? { agentProfileId: overrides.profileId ?? agentProfileId } : {}),
        selectedSkillNames: overrides.skills ?? selectedSkills,
        memoryTokenBudget: overrides.budget ?? memoryBudget,
        maxMemoryItems: 20,
        topN: 10
      }))
    } catch (inspectError) {
      setError(inspectError instanceof Error ? inspectError.message : String(inspectError))
    } finally {
      setLoading(false)
    }
  }

  function chooseProfile(id: string): void {
    const profile = agents?.profiles.find((candidate) => candidate.id === id)
    const nextClient = profile?.clientId ?? clientId
    const skills = profile?.skillNames ?? []
    setAgentProfileId(id)
    setClientId(nextClient)
    setSelectedSkills(skills)
    void inspect({ client: nextClient, profileId: id, skills })
  }

  function chooseClient(value: AiClientId): void {
    setClientId(value)
    setAgentProfileId('')
    setSelectedSkills([])
    void inspect({ client: value, profileId: '', skills: [] })
  }

  function toggleSkill(name: string, checked: boolean): void {
    const next = checked ? [...selectedSkills, name] : selectedSkills.filter((item) => item !== name)
    setSelectedSkills(next)
    void inspect({ skills: next })
  }

  const profiles = agents?.profiles ?? []
  const skills = availableSkills(inventory, clientId)
  return <section className="tool-page p5-tool ai-context-inspector">
    <ToolPageHeader title={t('contextInspector.title')} actions={<><button className="toolbar-button" type="button" onClick={() => { void chooseProject() }}><FolderOpen size={14} />{projectRoot ? t('ai.changeProject') : t('ai.chooseProject')}</button><button className="toolbar-button" type="button" disabled={loading || !projectRoot} onClick={() => { void refreshOptions() }}><RefreshCw size={14} className={loading ? 'spin' : undefined} />{t('common.refresh')}</button></>} />
    <div className="local-tool-shell ai-context-shell"><div className="ai-context-scroll">
      {!projectRoot ? <div className="ai-context-empty"><ScanSearch size={32} /><strong>{t('contextInspector.empty.title')}</strong><p>{t('contextInspector.empty.description')}</p><button className="toolbar-button" type="button" onClick={() => { void chooseProject() }}><FolderOpen size={14} />{t('ai.chooseProject')}</button></div> : <>
        <section className="ai-context-controls"><div className="ai-context-control-grid"><label><span>{t('contextInspector.project')}</span><code title={projectRoot}>{projectRoot}</code></label><label><span>{t('contextInspector.target')}</span><button type="button" onClick={() => { void chooseTarget() }}><code title={targetPath}>{targetPath}</code><FolderOpen size={13} /></button></label><label><span>{t('contextInspector.client')}</span><select value={clientId} onChange={(event) => chooseClient(event.target.value as AiClientId)}>{aiClientIds.map((candidate) => <option value={candidate} key={candidate}>{aiClientNames[candidate]}</option>)}</select></label><label><span>{t('contextInspector.profile')}</span><select value={agentProfileId} onChange={(event) => chooseProfile(event.target.value)}><option value="">{t('contextInspector.noProfile')}</option>{profiles.map((profile) => <option value={profile.id} key={profile.id}>{profile.name}</option>)}</select></label><label><span>{t('contextInspector.memoryBudget')}</span><input type="number" min="1" max="100000" value={memoryBudget} onChange={(event) => setMemoryBudget(Math.max(1, Number(event.target.value) || 1))} onBlur={() => { void inspect({ budget: memoryBudget }) }} /></label></div>
          <fieldset><legend>{t('contextInspector.skills')}</legend>{skills.length > 0 ? skills.map((name) => <label key={name}><input type="checkbox" checked={selectedSkills.includes(name)} onChange={(event) => toggleSkill(name, event.target.checked)} /><span>{name}</span></label>) : <small>{t('contextInspector.skillsEmpty')}</small>}</fieldset>
        </section>
        {error && <div className="ai-error" role="alert"><CircleAlert size={16} /><span>{error}</span></div>}
        {loading && !snapshot ? <div className="history-empty">{t('contextInspector.loading')}</div> : snapshot && <>
          <div className="ai-context-estimate"><Info size={15} /><span>{t('contextInspector.estimateNotice')}</span><code>{snapshot.tokenizer.label}</code></div>
          <div className="ai-manager-metrics"><AiMetric label={t('contextInspector.metric.total')} value={formatTokens(snapshot.totals.estimatedTokens)} /><AiMetric label={t('contextInspector.metric.resident')} value={formatTokens(snapshot.totals.residentTokens)} /><AiMetric label={t('contextInspector.metric.onDemand')} value={formatTokens(snapshot.totals.onDemandTokens)} /><AiMetric label={t('contextInspector.metric.mcpUnknown')} value={snapshot.mcpSchemas.unknownServers.length} /></div>

          <section className="ai-context-breakdown"><header><div><h2>{t('contextInspector.breakdown.title')}</h2><p>{t('contextInspector.breakdown.description')}</p></div><Gauge size={18} /></header><div>{snapshot.breakdown.map((entry) => <article key={entry.category}><header><span>{t(`contextInspector.category.${entry.category}` as 'contextInspector.category.instruction')}</span><strong>{formatTokens(entry.estimatedTokens)}</strong></header><div><i style={{ width: `${snapshot.totals.estimatedTokens ? Math.max(2, entry.estimatedTokens / snapshot.totals.estimatedTokens * 100) : 0}%` }} /></div><small>{t('contextInspector.items', { count: String(entry.items) })}</small></article>)}</div></section>

          {snapshot.recommendations.length > 0 && <section className="ai-context-recommendations"><header><h2>{t('contextInspector.recommendations')}</h2><Sparkles size={17} /></header>{snapshot.recommendations.map((recommendation, index) => <article className={`ai-context-recommendation ai-context-recommendation--${recommendation.severity}`} key={`${recommendation.code}-${index}`}><CircleAlert size={15} /><div><strong>{t(`contextInspector.recommendation.${recommendation.code}` as 'contextInspector.recommendation.largeResidentContext')}</strong><p>{recommendation.message}</p></div><button className="icon-button" type="button" aria-label={t('contextInspector.openSource')} onClick={() => openTool(recommendation.sourceToolId)}><ArrowUpRight size={14} /></button></article>)}</section>}

          <div className="ai-context-content-grid"><section className="ai-context-top"><header><h2>{t('contextInspector.top.title')}</h2><Layers3 size={17} /></header><div>{snapshot.topItems.map((item, index) => <ContextRow item={item} rank={index + 1} key={item.id} />)}</div></section><section className="ai-context-layers"><header><h2>{t('contextInspector.layers.title')}</h2><Brain size={17} /></header>{(['resident', 'pathTriggered', 'onDemand', 'runtime'] as AiContextLayer[]).map((layer) => { const layerItems = snapshot.items.filter((item) => item.layer === layer); return <article key={layer}><header><strong>{t(`contextInspector.layer.${layer}` as 'contextInspector.layer.resident')}</strong><span>{formatTokens(layerItems.reduce((sum, item) => sum + item.estimatedTokens, 0))}</span></header><p>{t(`contextInspector.layerDescription.${layer}` as 'contextInspector.layerDescription.resident')}</p><small>{t('contextInspector.items', { count: String(layerItems.length) })}</small></article> })}</section></div>

          {snapshot.duplicates.length > 0 && <section className="ai-context-duplicates"><h2>{t('contextInspector.duplicates')}</h2>{snapshot.duplicates.map((group, index) => <article key={`${group.category}-${index}`}><strong>{t(`contextInspector.category.${group.category}` as 'contextInspector.category.instruction')}</strong><span>{group.names.join(' · ')}</span><em>{t('contextInspector.duplicateWaste', { tokens: formatTokens(group.estimatedWasteTokens) })}</em></article>)}</section>}
        </>}
      </>}
    </div></div>
  </section>

  function ContextRow({ item, rank }: { item: AiContextItem; rank: number }) {
    return <article><span>{rank}</span><div><header><strong>{item.name}</strong><em>{t(`contextInspector.category.${item.category}` as 'contextInspector.category.instruction')}</em></header><p>{item.reason}</p>{item.sourcePath && <code title={item.sourcePath}>{item.sourcePath}</code>}</div><strong>{formatTokens(item.estimatedTokens)}</strong><button className="icon-button" type="button" aria-label={t('contextInspector.openSource')} onClick={() => openTool(item.sourceToolId)}><ArrowUpRight size={14} /></button></article>
  }
}

function availableSkills(inventory: AiDoctorSnapshot | null, clientId: AiClientId): string[] {
  return [...new Set((inventory?.artifacts ?? []).filter((artifact) => artifact.kind === 'skill' && artifact.clientId === clientId).map((artifact) => {
    const declared = artifact.metadata?.declaredName
    return typeof declared === 'string' && declared ? declared : artifact.name
  }))].sort()
}

function formatTokens(value: number): string {
  return `≈ ${value.toLocaleString()}`
}
