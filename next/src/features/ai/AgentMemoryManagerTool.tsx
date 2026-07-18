import { Archive, ArchiveRestore, BrainCircuit, Check, CircleAlert, Clock3, Eye, FileText, FolderOpen, Inbox, ListOrdered, Pencil, Plus, Search, ShieldCheck, Trash2, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { AiMetric } from './AiManagerChrome'
import type { AiMemory, AiMemoryKind, AiMemoryPreview, AiMemorySaveInput, AiMemoryScope, AiMemorySensitivity, AiMemorySnapshot } from '@/shared/contracts/aiMemory'
import { aiMemoryKinds, aiMemoryScopes, aiMemorySensitivities } from '@/shared/contracts/aiMemory'
import { Dialog } from '@/shared/components/Dialog'
import { ToolPageHeader } from '@/shared/components/ToolPage'
import { useI18n } from '@/shared/i18n/I18nProvider'
import type { AiNativeMemorySnapshot } from '@/shared/contracts/aiNativeMemory'
import { MemoryEmbeddingDialog } from './MemoryEmbeddingDialog'

const defaultTokenBudget = 1_000

export function AgentMemoryManagerTool() {
  const { t } = useI18n()
  const [snapshot, setSnapshot] = useState<AiMemorySnapshot | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [query, setQuery] = useState('')
  const [kindFilter, setKindFilter] = useState<'' | AiMemoryKind>('')
  const [includeArchived, setIncludeArchived] = useState(false)
  const [projectRoot, setProjectRoot] = useState('')
  const [editorOpen, setEditorOpen] = useState(false)
  const [draft, setDraft] = useState<AiMemorySaveInput>(() => emptyDraft(''))
  const [editorBusy, setEditorBusy] = useState(false)
  const [editorError, setEditorError] = useState('')
  const [preview, setPreview] = useState<AiMemoryPreview | null>(null)
  const [previewTarget, setPreviewTarget] = useState('')
  const [tokenBudget, setTokenBudget] = useState(defaultTokenBudget)
  const [previewBusy, setPreviewBusy] = useState(false)
  const [previewError, setPreviewError] = useState('')
  const [nativeMemory, setNativeMemory] = useState<AiNativeMemorySnapshot | null>(null)
  const [nativeMemoryBusy, setNativeMemoryBusy] = useState(false)
  const [nativeMemoryError, setNativeMemoryError] = useState('')
  const [embeddingOpen, setEmbeddingOpen] = useState(false)

  async function load(): Promise<void> {
    setLoading(true)
    setError('')
    try {
      setSnapshot(await window.mootool.getAiMemorySnapshot({
        ...(query.trim() ? { keyword: query.trim() } : {}),
        ...(kindFilter ? { kind: kindFilter } : {}),
        includeArchived
      }))
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : String(loadError))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    const timer = setTimeout(() => { void load() }, 180)
    return () => clearTimeout(timer)
  }, [query, kindFilter, includeArchived])

  async function chooseProject(): Promise<void> {
    const selected = await window.mootool.chooseDirectory(projectRoot || undefined)
    if (selected) setProjectRoot(selected)
  }

  function openNew(): void {
    setDraft(emptyDraft(projectRoot))
    setEditorError('')
    setEditorOpen(true)
  }

  function openEdit(memory: AiMemory): void {
    setDraft({
      id: memory.id,
      kind: memory.kind,
      scope: memory.scope,
      scopeValue: memory.scopeValue,
      content: memory.content,
      sourceKind: memory.sourceKind,
      sourceRef: memory.sourceRef,
      confidence: memory.confidence,
      sensitivity: memory.sensitivity,
      expiresAt: memory.expiresAt
    })
    setEditorError('')
    setEditorOpen(true)
  }

  async function saveMemory(): Promise<void> {
    setEditorBusy(true)
    setEditorError('')
    try {
      await window.mootool.saveAiMemory({ ...draft, content: draft.content.trim(), expiresAt: draft.expiresAt || undefined })
      setEditorOpen(false)
      await load()
    } catch (saveError) {
      setEditorError(saveError instanceof Error ? saveError.message : String(saveError))
    } finally {
      setEditorBusy(false)
    }
  }

  async function chooseScopePath(): Promise<void> {
    const selected = await window.mootool.chooseDirectory(draft.scopeValue || projectRoot || undefined)
    if (selected) setDraft((current) => ({ ...current, scopeValue: selected }))
  }

  async function archiveMemory(memory: AiMemory): Promise<void> {
    if (memory.archivedAt) await window.mootool.restoreAiMemory(memory.id)
    else await window.mootool.archiveAiMemory(memory.id)
    await load()
  }

  async function deleteMemory(memory: AiMemory): Promise<void> {
    if (!window.confirm(t('memoryManager.deleteConfirm'))) return
    await window.mootool.deleteAiMemory(memory.id)
    await load()
  }

  async function reviewCandidate(candidateId: string, action: 'approve' | 'reject'): Promise<void> {
    await window.mootool.reviewAiMemoryCandidate({ candidateId, action })
    await load()
  }

  async function openPreview(): Promise<void> {
    let targetPath = projectRoot
    if (projectRoot) {
      const selected = await window.mootool.chooseDirectory(projectRoot)
      if (!selected) return
      targetPath = selected
    }
    setPreviewTarget(targetPath)
    await runPreview(targetPath, tokenBudget)
  }

  async function runPreview(targetPath = previewTarget, budget = tokenBudget): Promise<void> {
    setPreviewBusy(true)
    setPreviewError('')
    try {
      setPreview(await window.mootool.previewAiMemories({
        ...(projectRoot ? { projectRoot } : {}),
        ...(targetPath ? { targetPath } : {}),
        ...(query.trim() ? { query: query.trim() } : {}),
        tokenBudget: budget,
        maxItems: 50
      }))
    } catch (previewFailure) {
      setPreviewError(previewFailure instanceof Error ? previewFailure.message : String(previewFailure))
      setPreview({ memories: [], totalEstimatedTokens: 0, omittedByBudget: 0 })
    } finally {
      setPreviewBusy(false)
    }
  }

  async function inspectNativeMemory(): Promise<void> {
    setNativeMemoryBusy(true)
    setNativeMemoryError('')
    setNativeMemory({ scannedAt: new Date().toISOString(), readOnly: true, roots: [], artifacts: [], diagnostics: [] })
    try { setNativeMemory(await window.mootool.getAiNativeMemorySnapshot()) } catch (nativeError) {
      setNativeMemoryError(nativeError instanceof Error ? nativeError.message : String(nativeError))
      setNativeMemory({ scannedAt: new Date().toISOString(), readOnly: true, roots: [], artifacts: [], diagnostics: [] })
    } finally { setNativeMemoryBusy(false) }
  }

  const memories = snapshot?.memories ?? []
  return <section className="tool-page p5-tool ai-memory-manager">
    <ToolPageHeader title={t('memoryManager.title')} actions={<><button className="toolbar-button" type="button" onClick={() => { void chooseProject() }}><FolderOpen size={14} />{projectRoot ? t('ai.changeProject') : t('ai.chooseProject')}</button><button className="toolbar-button" type="button" onClick={() => setEmbeddingOpen(true)}><BrainCircuit size={14} />{t('memoryManager.embedding.action')}</button><button className="toolbar-button" type="button" disabled={nativeMemoryBusy} onClick={() => { void inspectNativeMemory() }}><Eye size={14} />{t('memoryManager.native.action')}</button><button className="toolbar-button" type="button" disabled={loading} onClick={() => { void openPreview() }}><ListOrdered size={14} />{t('memoryManager.preview.action')}</button><button className="toolbar-button toolbar-button--primary" type="button" onClick={openNew}><Plus size={14} />{t('memoryManager.new')}</button></>} />
    <div className="local-tool-shell ai-memory-shell"><div className="ai-memory-scroll">
      <div className="ai-manager-scope"><span>{t('ai.project')}</span><strong title={projectRoot}>{projectRoot || t('memoryManager.allProjects')}</strong></div>
      <div className="ai-manager-metrics"><AiMetric label={t('memoryManager.metric.active')} value={snapshot?.stats.active ?? 0} /><AiMetric label={t('memoryManager.metric.pending')} value={snapshot?.stats.pendingCandidates ?? 0} /><AiMetric label={t('memoryManager.metric.expiring')} value={snapshot?.stats.expiringSoon ?? 0} /><AiMetric label={t('memoryManager.metric.archived')} value={snapshot?.stats.archived ?? 0} /></div>
      <div className="ai-memory-toolbar"><label><Search size={14} /><input aria-label={t('memoryManager.search')} value={query} placeholder={t('memoryManager.search')} onChange={(event) => setQuery(event.target.value)} /></label><select aria-label={t('memoryManager.filter.kind')} value={kindFilter} onChange={(event) => setKindFilter(event.target.value as '' | AiMemoryKind)}><option value="">{t('memoryManager.filter.allKinds')}</option>{aiMemoryKinds.map((kind) => <option value={kind} key={kind}>{kindLabel(kind)}</option>)}</select><label className="ai-memory-archive-toggle"><input type="checkbox" checked={includeArchived} onChange={(event) => setIncludeArchived(event.target.checked)} /><span>{t('memoryManager.showArchived')}</span></label></div>
      {(error || previewError) && <div className="ai-error" role="alert"><CircleAlert size={16} /><span>{error || previewError}</span></div>}
      {(snapshot?.candidates.length ?? 0) > 0 && <section className="ai-memory-inbox"><h2><Inbox size={15} />{t('memoryManager.inbox.title')}<span>{snapshot!.candidates.length}</span></h2>{snapshot!.candidates.map((candidate) => <article key={candidate.id}><div><header><strong>{kindLabel(candidate.kind)}</strong><span>{scopeLabel(candidate.proposedScope)}</span><span>{Math.round(candidate.confidence * 100)}%</span></header><p>{candidate.content}</p><small>{candidate.evidenceSummary} · {candidate.sourceRef}</small></div><button className="icon-button" type="button" aria-label={t('memoryManager.inbox.reject')} onClick={() => { void reviewCandidate(candidate.id, 'reject') }}><X size={14} /></button><button className="icon-button" type="button" aria-label={t('memoryManager.inbox.approve')} onClick={() => { void reviewCandidate(candidate.id, 'approve') }}><Check size={14} /></button></article>)}</section>}
      {loading && !snapshot ? <div className="history-empty">{t('ai.loading')}</div> : memories.length > 0 ? <div className="ai-memory-list">{memories.map((memory) => <MemoryCard memory={memory} key={memory.id} />)}</div> : <div className="history-empty">{t('memoryManager.empty')}</div>}
    </div></div>
    <Dialog title={draft.id ? t('memoryManager.edit') : t('memoryManager.new')} open={editorOpen} width={760} onClose={() => { if (!editorBusy) setEditorOpen(false) }} footer={<><button className="dialog-button" type="button" disabled={editorBusy} onClick={() => setEditorOpen(false)}>{t('common.cancel')}</button><button className="dialog-button dialog-button--primary" type="button" disabled={editorBusy || !draft.content.trim() || (draft.scope !== 'user' && !draft.scopeValue?.trim())} onClick={() => { void saveMemory() }}><ShieldCheck size={14} />{editorBusy ? t('common.processing') : t('common.save')}</button></>}>
      <div className="ai-memory-editor"><div className="ai-memory-editor-grid"><label><span>{t('memoryManager.field.kind')}</span><select value={draft.kind} onChange={(event) => setDraft({ ...draft, kind: event.target.value as AiMemoryKind })}>{aiMemoryKinds.map((kind) => <option value={kind} key={kind}>{kindLabel(kind)}</option>)}</select></label><label><span>{t('memoryManager.field.scope')}</span><select value={draft.scope} onChange={(event) => { const scope = event.target.value as AiMemoryScope; setDraft({ ...draft, scope, scopeValue: scope === 'user' ? undefined : scope === 'project' && projectRoot ? projectRoot : draft.scopeValue }) }}>{aiMemoryScopes.map((scope) => <option value={scope} key={scope}>{scopeLabel(scope)}</option>)}</select></label><label><span>{t('memoryManager.field.sensitivity')}</span><select value={draft.sensitivity} onChange={(event) => setDraft({ ...draft, sensitivity: event.target.value as AiMemorySensitivity })}>{aiMemorySensitivities.map((sensitivity) => <option value={sensitivity} key={sensitivity}>{sensitivityLabel(sensitivity)}</option>)}</select></label></div>{draft.scope !== 'user' && <label><span>{t('memoryManager.field.scopeValue')}</span><div className="ai-memory-path-field"><input value={draft.scopeValue ?? ''} onChange={(event) => setDraft({ ...draft, scopeValue: event.target.value })} /><button className="icon-button" type="button" aria-label={t('common.choose')} onClick={() => { void chooseScopePath() }}><FolderOpen size={14} /></button></div></label>}<label><span>{t('memoryManager.field.content')}</span><textarea rows={8} value={draft.content} onChange={(event) => setDraft({ ...draft, content: event.target.value })} /></label><div className="ai-memory-editor-grid"><label><span>{t('memoryManager.field.confidence')}</span><input type="number" min="0" max="1" step="0.05" value={draft.confidence} onChange={(event) => setDraft({ ...draft, confidence: Number(event.target.value) })} /></label><label><span>{t('memoryManager.field.expires')}</span><input type="datetime-local" value={toLocalDateTime(draft.expiresAt)} onChange={(event) => setDraft({ ...draft, expiresAt: event.target.value ? new Date(event.target.value).toISOString() : undefined })} /></label><label><span>{t('memoryManager.field.source')}</span><input value={draft.sourceRef ?? ''} onChange={(event) => setDraft({ ...draft, sourceRef: event.target.value })} /></label></div>{editorError && <div className="ai-error" role="alert"><CircleAlert size={16} /><span>{editorError}</span></div>}</div>
    </Dialog>
    <Dialog title={t('memoryManager.preview.title')} open={Boolean(preview)} width={780} onClose={() => setPreview(null)} footer={<button className="dialog-button" type="button" onClick={() => setPreview(null)}>{t('common.close')}</button>}>
      {preview && <div className="ai-memory-preview"><div className="ai-memory-preview-controls"><div><span>{t('memoryManager.preview.target')}</span><code>{previewTarget || t('memoryManager.allProjects')}</code></div><label><span>{t('memoryManager.preview.budget')}</span><input type="number" min="1" max="100000" value={tokenBudget} onChange={(event) => setTokenBudget(Number(event.target.value))} /></label><button className="dialog-button" type="button" disabled={previewBusy} onClick={() => { void runPreview(previewTarget, tokenBudget) }}>{previewBusy ? t('common.processing') : t('common.refresh')}</button></div><div className="ai-memory-preview-summary"><strong>{t('memoryManager.preview.total', { count: preview.totalEstimatedTokens.toLocaleString() })}</strong><span>{t('memoryManager.preview.omitted', { count: String(preview.omittedByBudget) })}</span></div>{preview.memories.length > 0 ? <ol>{preview.memories.map((item) => <li key={item.memory.id}><span>{item.rank}</span><div><header><strong>{kindLabel(item.memory.kind)}</strong><em>{t(`memoryManager.preview.reason.${item.reason}` as 'memoryManager.preview.reason.userScope')}</em><em>{t('instructionManager.tokens', { count: item.estimatedTokens.toLocaleString() })}</em></header><p>{item.memory.content}</p><small>{scopeLabel(item.memory.scope)}{item.memory.scopeValue ? ` · ${item.memory.scopeValue}` : ''}</small></div></li>)}</ol> : <div className="history-empty">{t('memoryManager.preview.empty')}</div>}</div>}
    </Dialog>
    <Dialog title={t('memoryManager.native.title')} open={Boolean(nativeMemory)} width={820} onClose={() => { if (!nativeMemoryBusy) setNativeMemory(null) }} footer={<button className="dialog-button" type="button" disabled={nativeMemoryBusy} onClick={() => setNativeMemory(null)}>{t('common.close')}</button>}>
      {nativeMemoryBusy ? <div className="history-empty">{t('memoryManager.native.loading')}</div> : nativeMemory && <div className="ai-native-memory"><div className="ai-native-memory-notice"><ShieldCheck size={15} /><span>{t('memoryManager.native.safety')}</span></div>{nativeMemoryError && <div className="ai-error" role="alert"><CircleAlert size={16} /><span>{nativeMemoryError}</span></div>}{nativeMemory.diagnostics.map((diagnostic, index) => <div className="ai-runtime-diagnostic ai-runtime-diagnostic--warning" key={`${diagnostic.message}-${index}`}><CircleAlert size={14} /><p>{diagnostic.message}</p></div>)}{nativeMemory.artifacts.length > 0 ? <div className="ai-native-memory-list">{nativeMemory.artifacts.map((artifact) => <article key={artifact.id}><header><div><FileText size={15} /><strong>{artifact.name}</strong>{artifact.entrypoint && <em>{t('memoryManager.native.entrypoint')}</em>}{artifact.sensitiveFindings > 0 && <em className="is-warning">{t('memoryManager.native.sensitive', { count: String(artifact.sensitiveFindings) })}</em>}</div><span>{t('instructionManager.tokens', { count: artifact.estimatedTokens.toLocaleString() })}</span></header><code title={artifact.path}>{artifact.path}</code><pre>{artifact.contentExcerpt}{artifact.excerptTruncated ? '\n…' : ''}</pre><footer><span>{artifact.projectKey}</span><span>{new Date(artifact.modifiedAt).toLocaleString()}</span></footer></article>)}</div> : <div className="history-empty">{t('memoryManager.native.empty')}</div>}</div>}
    </Dialog>
    <MemoryEmbeddingDialog open={embeddingOpen} projectRoot={projectRoot} defaultQuery={query} onClose={() => setEmbeddingOpen(false)} />
  </section>

  function MemoryCard({ memory }: { memory: AiMemory }) {
    return <article className={memory.archivedAt ? 'ai-memory-card ai-memory-card--archived' : 'ai-memory-card'}><header><strong>{kindLabel(memory.kind)}</strong><span>{scopeLabel(memory.scope)}</span><span>{sensitivityLabel(memory.sensitivity)}</span><em>{Math.round(memory.confidence * 100)}%</em></header><p>{memory.content}</p>{memory.scopeValue && <code>{memory.scopeValue}</code>}<footer><span><Clock3 size={12} />{new Date(memory.updatedAt).toLocaleString()}</span>{memory.expiresAt && <span>{t('memoryManager.expires', { date: new Date(memory.expiresAt).toLocaleString() })}</span>}<div><button className="icon-button" type="button" aria-label={t('common.edit')} onClick={() => openEdit(memory)}><Pencil size={13} /></button><button className="icon-button" type="button" aria-label={memory.archivedAt ? t('memoryManager.restore') : t('memoryManager.archive')} onClick={() => { void archiveMemory(memory) }}>{memory.archivedAt ? <ArchiveRestore size={13} /> : <Archive size={13} />}</button><button className="icon-button icon-button--danger" type="button" aria-label={t('common.action.delete')} onClick={() => { void deleteMemory(memory) }}><Trash2 size={13} /></button></div></footer></article>
  }

  function kindLabel(kind: AiMemoryKind): string { return t(`memoryManager.kind.${kind}` as 'memoryManager.kind.projectFact') }
  function scopeLabel(scope: AiMemoryScope): string { return t(`memoryManager.scope.${scope}` as 'memoryManager.scope.project') }
  function sensitivityLabel(value: AiMemorySensitivity): string { return t(`memoryManager.sensitivity.${value}` as 'memoryManager.sensitivity.internal') }
}

function emptyDraft(projectRoot: string): AiMemorySaveInput {
  return { kind: 'projectFact', scope: projectRoot ? 'project' : 'user', scopeValue: projectRoot || undefined, content: '', sourceKind: 'user', confidence: 1, sensitivity: 'internal' }
}

function toLocalDateTime(value?: string): string {
  if (!value) return ''
  const date = new Date(value)
  const offset = date.getTimezoneOffset() * 60_000
  return new Date(date.getTime() - offset).toISOString().slice(0, 16)
}
