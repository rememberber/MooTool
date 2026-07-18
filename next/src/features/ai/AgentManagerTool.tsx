import { Bot, Check, CircleAlert, Clipboard, Code2, Download, FolderOpen, Pencil, Play, Plus, RefreshCw, ShieldCheck, TerminalSquare, Trash2, Upload } from 'lucide-react'
import { useEffect, useState } from 'react'
import { AiMetric } from './AiManagerChrome'
import type {
  AiAgentCapabilityId,
  AiAgentClient,
  AiAgentLaunchPlan,
  AiAgentManagerSnapshot,
  AiAgentPermissionMode,
  AiAgentProfile,
  AiAgentProfileSaveInput
} from '@/shared/contracts/aiAgents'
import { aiClientNames, type AiPrimaryClientId } from '@/shared/contracts/ai'
import { aiModelRuntimeNames, type AiModelRuntimeId, type AiModelRuntimeSnapshot } from '@/shared/contracts/aiModelRuntime'
import { Dialog } from '@/shared/components/Dialog'
import { ToolPageHeader } from '@/shared/components/ToolPage'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { AgentTaskDialog } from './AgentTaskDialog'

type ProfileDraft = {
  id?: string
  name: string
  clientId: AiPrimaryClientId
  model: string
  modelRuntimeId: '' | AiModelRuntimeId
  localModelDigest: string
  workingDirectory: string
  configProfile: string
  permissionMode: AiAgentPermissionMode
  mcpServerNames: string
  skillNames: string
  environmentVariableRefs: string
  optionalFlags: string[]
}

const capabilities: AiAgentCapabilityId[] = ['instructions', 'skills', 'mcp', 'subagents', 'hooks', 'structuredOutput', 'usage', 'permissionModes']
const permissions: Record<AiPrimaryClientId, AiAgentPermissionMode[]> = {
  codex: ['readOnly', 'default', 'workspaceWrite'],
  claudeCode: ['default', 'plan', 'acceptEdits', 'dontAsk']
}
const flags: Record<AiPrimaryClientId, string[]> = {
  codex: ['--search', '--no-alt-screen'],
  claudeCode: ['--ide', '--no-chrome', '--disable-slash-commands']
}

export function AgentManagerTool() {
  const { t } = useI18n()
  const [projectRoot, setProjectRoot] = useState('')
  const [snapshot, setSnapshot] = useState<AiAgentManagerSnapshot | null>(null)
  const [runtime, setRuntime] = useState<AiModelRuntimeSnapshot | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [draft, setDraft] = useState<ProfileDraft | null>(null)
  const [saving, setSaving] = useState(false)
  const [editorError, setEditorError] = useState('')
  const [plan, setPlan] = useState<AiAgentLaunchPlan | null>(null)
  const [planLoading, setPlanLoading] = useState(false)
  const [planError, setPlanError] = useState('')
  const [copied, setCopied] = useState(false)
  const [taskProfile, setTaskProfile] = useState<AiAgentProfile | null>(null)

  async function load(root = projectRoot): Promise<void> {
    setLoading(true)
    setError('')
    try {
      const [result, runtimeResult] = await Promise.all([
        window.mootool.getAiAgentManagerSnapshot(root ? { projectRoot: root } : {}),
        window.mootool.getAiModelRuntimeSnapshot()
      ])
      setSnapshot(result)
      setRuntime(runtimeResult)
      setProjectRoot(result.projectRoot ?? root)
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : String(loadError))
    } finally {
      setLoading(false)
    }
  }

  async function chooseProject(): Promise<void> {
    const selected = await window.mootool.chooseDirectory(projectRoot || undefined)
    if (selected) {
      setProjectRoot(selected)
      await load(selected)
    }
  }

  function newProfile(): void {
    setEditorError('')
    setDraft(emptyDraft(projectRoot))
  }

  function editProfile(profile: AiAgentProfile): void {
    setEditorError('')
    setDraft({
      id: profile.id,
      name: profile.name,
      clientId: profile.clientId,
      model: profile.model ?? '',
      modelRuntimeId: profile.modelRuntimeId ?? '',
      localModelDigest: profile.localModelDigest ?? '',
      workingDirectory: profile.workingDirectory,
      configProfile: profile.configProfile ?? '',
      permissionMode: profile.permissionMode,
      mcpServerNames: profile.mcpServerNames.join(', '),
      skillNames: profile.skillNames.join(', '),
      environmentVariableRefs: profile.environmentVariableRefs.join(', '),
      optionalFlags: profile.optionalFlags
    })
  }

  async function saveProfile(): Promise<void> {
    if (!draft) return
    const input: AiAgentProfileSaveInput = {
      ...(draft.id ? { id: draft.id } : {}),
      name: draft.name,
      clientId: draft.clientId,
      ...(draft.model.trim() ? { model: draft.model.trim() } : {}),
      ...(draft.modelRuntimeId ? { modelRuntimeId: draft.modelRuntimeId } : {}),
      ...(draft.modelRuntimeId && draft.localModelDigest ? { localModelDigest: draft.localModelDigest } : {}),
      workingDirectory: draft.workingDirectory,
      ...(draft.configProfile.trim() ? { configProfile: draft.configProfile.trim() } : {}),
      permissionMode: draft.permissionMode,
      mcpServerNames: parseList(draft.mcpServerNames),
      skillNames: parseList(draft.skillNames),
      environmentVariableRefs: parseList(draft.environmentVariableRefs),
      optionalFlags: draft.optionalFlags
    }
    setSaving(true)
    setEditorError('')
    try {
      await window.mootool.saveAiAgentProfile(input)
      setDraft(null)
      await load()
    } catch (saveError) {
      setEditorError(saveError instanceof Error ? saveError.message : String(saveError))
    } finally {
      setSaving(false)
    }
  }

  async function deleteProfile(profile: AiAgentProfile): Promise<void> {
    if (!window.confirm(t('agentManager.deleteConfirm', { name: profile.name }))) return
    try {
      await window.mootool.deleteAiAgentProfile(profile.id)
      await load()
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : String(deleteError))
    }
  }

  async function generatePlan(profile: AiAgentProfile): Promise<void> {
    setPlan(null)
    setPlanError('')
    setCopied(false)
    setPlanLoading(true)
    try {
      setPlan(await window.mootool.getAiAgentLaunchPlan(profile.id))
    } catch (launchError) {
      setPlanError(launchError instanceof Error ? launchError.message : String(launchError))
    } finally {
      setPlanLoading(false)
    }
  }

  async function copyPlan(): Promise<void> {
    if (!plan) return
    await navigator.clipboard.writeText(plan.displayCommand)
    setCopied(true)
  }

  async function importProfile(): Promise<void> {
    setError('')
    try {
      const document = await window.mootool.importAiAgentProfile()
      if (!document) return
      const workingDirectory = await window.mootool.chooseDirectory(projectRoot || undefined)
      if (!workingDirectory) return
      await window.mootool.saveAiAgentProfile({ ...document.profile, workingDirectory })
      setProjectRoot(projectRoot || workingDirectory)
      await load(projectRoot || workingDirectory)
    } catch (importError) {
      setError(importError instanceof Error ? importError.message : String(importError))
    }
  }

  async function exportProfile(profile: AiAgentProfile): Promise<void> {
    setError('')
    try {
      await window.mootool.exportAiAgentProfile(profile.id)
    } catch (exportError) {
      setError(exportError instanceof Error ? exportError.message : String(exportError))
    }
  }

  useEffect(() => { void load() }, [])

  const detected = snapshot?.clients.filter((client) => client.detected).length ?? 0
  const healthy = snapshot?.clients.filter((client) => client.health === 'healthy').length ?? 0
  const issues = snapshot?.clients.reduce((sum, client) => sum + client.diagnostics.length, 0) ?? 0
  const selectedRuntime = draft?.modelRuntimeId ? runtime?.runtimes.find((candidate) => candidate.id === draft.modelRuntimeId) : undefined

  return <section className="tool-page p5-tool ai-agent-manager">
    <ToolPageHeader title={t('agentManager.title')} actions={<><button className="toolbar-button" type="button" onClick={() => { void chooseProject() }}><FolderOpen size={14} />{projectRoot ? t('ai.changeProject') : t('ai.chooseProject')}</button><button className="toolbar-button" type="button" onClick={() => { void importProfile() }}><Upload size={14} />{t('agentManager.profile.import')}</button><button className="toolbar-button" type="button" onClick={newProfile}><Plus size={14} />{t('agentManager.new')}</button><button className="toolbar-button" type="button" disabled={loading} onClick={() => { void load() }}><RefreshCw size={14} className={loading ? 'spin' : undefined} />{t('common.refresh')}</button></>} />
    <div className="local-tool-shell ai-agent-shell"><div className="ai-agent-scroll">
      <div className="ai-agent-safety"><ShieldCheck size={15} /><span>{t('agentManager.safety')}</span><code title={projectRoot}>{projectRoot || t('ai.userScope')}</code></div>
      {error && <div className="ai-error" role="alert"><CircleAlert size={16} /><span>{error}</span></div>}
      {loading && !snapshot ? <div className="history-empty">{t('agentManager.loading')}</div> : snapshot && <>
        <div className="ai-manager-metrics"><AiMetric label={t('agentManager.metric.clients')} value={detected} /><AiMetric label={t('agentManager.metric.profiles')} value={snapshot.profiles.length} /><AiMetric label={t('agentManager.metric.healthy')} value={healthy} /><AiMetric label={t('agentManager.metric.issues')} value={issues} /></div>

        <section className="ai-agent-clients"><header><div><h2>{t('agentManager.clients.title')}</h2><p>{t('agentManager.clients.description')}</p></div><Bot size={18} /></header><div className="ai-agent-client-grid">{snapshot.clients.map((client) => <ClientCard client={client} key={client.id} />)}</div></section>

        <section className="ai-agent-profiles"><header><div><h2>{t('agentManager.profiles.title')}</h2><p>{t('agentManager.profiles.description')}</p></div><button className="toolbar-button" type="button" onClick={newProfile}><Plus size={14} />{t('agentManager.new')}</button></header>
          {snapshot.profiles.length > 0 ? <div className="ai-agent-profile-list">{snapshot.profiles.map((profile) => <article className="ai-agent-profile" key={profile.id}><span><Bot size={18} /></span><div><header><strong>{profile.name}</strong><em>{aiClientNames[profile.clientId]}</em>{profile.modelRuntimeId && <em title={profile.localModelDigest}>{aiModelRuntimeNames[profile.modelRuntimeId]}</em>}</header><p>{profile.model || t('agentManager.defaultModel')} · {t(`agentManager.permission.${profile.permissionMode}` as 'agentManager.permission.default')}</p><code title={profile.workingDirectory}>{profile.workingDirectory}</code><footer><span>{t('agentManager.dependencyCounts', { skills: String(profile.skillNames.length), mcp: String(profile.mcpServerNames.length), env: String(profile.environmentVariableRefs.length) })}</span></footer></div><div className="ai-agent-profile-actions"><button className="toolbar-button toolbar-button--primary" type="button" onClick={() => setTaskProfile(profile)}><Play size={14} />{t('agentManager.task.action')}</button><button className="toolbar-button" type="button" onClick={() => { void generatePlan(profile) }}><TerminalSquare size={14} />{t('agentManager.plan.action')}</button><button className="icon-button" type="button" aria-label={t('agentManager.profile.export')} onClick={() => { void exportProfile(profile) }}><Download size={14} /></button><button className="icon-button" type="button" aria-label={t('common.edit')} onClick={() => editProfile(profile)}><Pencil size={14} /></button><button className="icon-button" type="button" aria-label={t('agentManager.delete')} onClick={() => { void deleteProfile(profile) }}><Trash2 size={14} /></button></div></article>)}</div> : <div className="ai-agent-empty"><Bot size={28} /><strong>{t('agentManager.empty.title')}</strong><p>{t('agentManager.empty.description')}</p><button className="toolbar-button" type="button" onClick={newProfile}><Plus size={14} />{t('agentManager.new')}</button></div>}
        </section>
      </>}
    </div></div>

    <Dialog title={draft?.id ? t('agentManager.editor.editTitle') : t('agentManager.editor.newTitle')} open={Boolean(draft)} width={760} onClose={() => { if (!saving) setDraft(null) }} footer={<><button className="dialog-button" type="button" disabled={saving} onClick={() => setDraft(null)}>{t('common.cancel')}</button><button className="dialog-button dialog-button--primary" type="button" disabled={saving || !draft?.name.trim() || !draft.workingDirectory.trim() || Boolean(draft.modelRuntimeId && (!draft.model.trim() || !draft.localModelDigest))} onClick={() => { void saveProfile() }}><Check size={14} />{saving ? t('common.processing') : t('common.save')}</button></>}>
      {draft && <div className="ai-agent-editor">{editorError && <div className="ai-error" role="alert"><CircleAlert size={16} /><span>{editorError}</span></div>}<div className="ai-agent-editor-grid"><Field label={t('agentManager.field.name')}><input autoFocus value={draft.name} onChange={(event) => setDraft({ ...draft, name: event.target.value })} /></Field><Field label={t('agentManager.field.client')}><select value={draft.clientId} onChange={(event) => { const clientId = event.target.value as AiPrimaryClientId; setDraft({ ...draft, clientId, permissionMode: permissions[clientId][0], configProfile: '', optionalFlags: [] }) }}><option value="codex">Codex</option><option value="claudeCode">Claude Code</option></select></Field><Field label={t('agentManager.field.runtime')}><select value={draft.modelRuntimeId} onChange={(event) => { const modelRuntimeId = event.target.value as '' | AiModelRuntimeId; const nextRuntime = modelRuntimeId ? runtime?.runtimes.find((candidate) => candidate.id === modelRuntimeId) : undefined; const selectedModel = nextRuntime?.models.find((item) => item.name === draft.model) ?? nextRuntime?.models[0]; setDraft({ ...draft, modelRuntimeId, model: selectedModel?.name ?? (modelRuntimeId ? '' : draft.model), localModelDigest: selectedModel?.digest ?? '' }) }}><option value="">{t('agentManager.field.runtimeDefault')}</option>{runtime?.runtimes.map((candidate) => <option value={candidate.id} disabled={!candidate.detected && draft.modelRuntimeId !== candidate.id} key={candidate.id}>{candidate.name}</option>)}</select></Field><Field label={t('agentManager.field.model')}>{draft.modelRuntimeId ? <select value={draft.model} disabled={!selectedRuntime?.models.length && !draft.model} onChange={(event) => { const selectedModel = selectedRuntime?.models.find((item) => item.name === event.target.value); setDraft({ ...draft, model: selectedModel?.name ?? '', localModelDigest: selectedModel?.digest ?? '' }) }}><option value="">{t('agentManager.field.noLocalModels')}</option>{draft.model && !selectedRuntime?.models.some((model) => model.name === draft.model) && <option value={draft.model}>{draft.model}</option>}{selectedRuntime?.models.map((model) => <option value={model.name} key={model.digest}>{model.name}</option>)}</select> : <input value={draft.model} placeholder={t('agentManager.field.modelHint')} onChange={(event) => setDraft({ ...draft, model: event.target.value, localModelDigest: '' })} />}</Field><Field label={t('agentManager.field.permission')}><select value={draft.permissionMode} onChange={(event) => setDraft({ ...draft, permissionMode: event.target.value as AiAgentPermissionMode })}>{permissions[draft.clientId].map((mode) => <option value={mode} key={mode}>{t(`agentManager.permission.${mode}` as 'agentManager.permission.default')}</option>)}</select></Field>{draft.clientId === 'codex' && <Field label={t('agentManager.field.configProfile')}><input value={draft.configProfile} placeholder={t('agentManager.field.configProfileHint')} onChange={(event) => setDraft({ ...draft, configProfile: event.target.value })} /></Field>}</div>
        <Field label={t('agentManager.field.directory')}><div className="ai-agent-directory"><input value={draft.workingDirectory} onChange={(event) => setDraft({ ...draft, workingDirectory: event.target.value })} /><button className="icon-button" type="button" aria-label={t('common.choose')} onClick={async () => { const selected = await window.mootool.chooseDirectory(draft.workingDirectory || projectRoot || undefined); if (selected) setDraft({ ...draft, workingDirectory: selected }) }}><FolderOpen size={14} /></button></div></Field>
        <div className="ai-agent-editor-grid"><Field label={t('agentManager.field.skills')}><input value={draft.skillNames} placeholder={t('agentManager.field.listHint')} onChange={(event) => setDraft({ ...draft, skillNames: event.target.value })} /></Field><Field label={t('agentManager.field.mcp')}><input value={draft.mcpServerNames} placeholder={t('agentManager.field.listHint')} onChange={(event) => setDraft({ ...draft, mcpServerNames: event.target.value })} /></Field></div>
        <Field label={t('agentManager.field.env')} hint={t('agentManager.field.envHint')}><input value={draft.environmentVariableRefs} placeholder="OPENAI_API_KEY, HTTPS_PROXY" onChange={(event) => setDraft({ ...draft, environmentVariableRefs: event.target.value })} /></Field>
        <fieldset className="ai-agent-flags"><legend>{t('agentManager.field.flags')}</legend>{flags[draft.clientId].map((flag) => <label key={flag}><input type="checkbox" checked={draft.optionalFlags.includes(flag)} onChange={(event) => setDraft({ ...draft, optionalFlags: event.target.checked ? [...draft.optionalFlags, flag] : draft.optionalFlags.filter((item) => item !== flag) })} /><code>{flag}</code></label>)}</fieldset>
      </div>}
    </Dialog>

    <Dialog title={t('agentManager.plan.title')} open={planLoading || Boolean(plan) || Boolean(planError)} width={800} onClose={() => { if (!planLoading) { setPlan(null); setPlanError('') } }} footer={<><button className="dialog-button" type="button" disabled={planLoading} onClick={() => { setPlan(null); setPlanError('') }}>{t('common.close')}</button>{plan && <button className="dialog-button dialog-button--primary" type="button" onClick={() => { void copyPlan() }}>{copied ? <Check size={14} /> : <Clipboard size={14} />}{copied ? t('agentManager.plan.copied') : t('agentManager.plan.copy')}</button>}</>}>
      {planLoading ? <div className="history-empty">{t('agentManager.plan.loading')}</div> : planError ? <div className="ai-error" role="alert"><CircleAlert size={16} /><span>{planError}</span></div> : plan && <div className="ai-agent-plan"><div className="ai-agent-plan-notice"><ShieldCheck size={15} /><span>{t('agentManager.plan.safety')}</span></div><dl><Info label={t('agentManager.plan.executable')} value={plan.executable} /><Info label={t('agentManager.plan.directory')} value={plan.workingDirectory} /><Info label={t('agentManager.plan.arguments')} value={plan.args.join(' ') || '—'} /></dl><section><h3><Code2 size={15} />{t('agentManager.plan.command')}</h3><pre>{plan.displayCommand}</pre></section>{plan.requiredEnvironmentVariables.length > 0 && <section><h3>{t('agentManager.plan.env')}</h3><div className="ai-agent-plan-tags">{plan.requiredEnvironmentVariables.map((name) => <code key={name}>{name}</code>)}</div></section>}{plan.warnings.length > 0 && <div className="ai-agent-plan-warnings">{plan.warnings.map((warning) => <p key={warning}><CircleAlert size={14} />{warning}</p>)}</div>}</div>}
    </Dialog>
    <AgentTaskDialog profile={taskProfile} onClose={() => setTaskProfile(null)} />
  </section>

  function ClientCard({ client }: { client: AiAgentClient }) {
    return <article className={`ai-agent-client ai-agent-client--${client.health}`}><header><div><strong>{client.name}</strong><span className={`ai-health ai-health--${client.health}`}>{t(`ai.status.${client.health}` as 'ai.status.healthy')}</span></div><code title={client.binaryPath ?? client.configRoot}>{client.binaryPath ?? t('agentManager.binaryMissing')}</code></header>{client.configurationChanged && <div className="ai-agent-config-changed"><CircleAlert size={13} />{t('agentManager.configurationChanged')}</div>}<table><tbody>{capabilities.map((capability) => { const support = client.capabilities.find((item) => item.id === capability)?.support ?? 'none'; return <tr key={capability}><th>{t(`agentManager.capability.${capability}` as 'agentManager.capability.instructions')}</th><td><span className={`ai-agent-support ai-agent-support--${support}`}>{t(`agentManager.support.${support}` as 'agentManager.support.full')}</span></td></tr> })}</tbody></table><footer><span>{t('agentManager.artifacts', { count: String(client.artifactCount) })}</span><code title={client.configurationFingerprint}>{client.configurationFingerprint.slice(0, 20)}…</code></footer>{client.diagnostics.map((diagnostic) => <p key={diagnostic}><CircleAlert size={13} />{diagnostic}</p>)}</article>
  }
}

function Field({ label, hint, children }: { label: string; hint?: string; children: React.ReactNode }) {
  return <label className="ai-agent-field"><span>{label}</span>{children}{hint && <small>{hint}</small>}</label>
}

function Info({ label, value }: { label: string; value: string }) {
  return <div><dt>{label}</dt><dd><code title={value}>{value}</code></dd></div>
}

function emptyDraft(projectRoot: string): ProfileDraft {
  return { name: '', clientId: 'codex', model: '', modelRuntimeId: '', localModelDigest: '', workingDirectory: projectRoot, configProfile: '', permissionMode: 'readOnly', mcpServerNames: '', skillNames: '', environmentVariableRefs: '', optionalFlags: [] }
}

function parseList(value: string): string[] {
  return [...new Set(value.split(/[\n,]/).map((item) => item.trim()).filter(Boolean))]
}
