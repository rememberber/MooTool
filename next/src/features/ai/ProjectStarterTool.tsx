import { CheckCircle2, CircleAlert, FileDiff, FolderOpen, PackagePlus, RotateCcw, ShieldCheck } from 'lucide-react'
import { useState } from 'react'
import type { AiChangeApplyResult } from '@/shared/contracts/aiChanges'
import { aiProjectStarterItems, type AiProjectStarterItem, type AiProjectStarterPreview } from '@/shared/contracts/aiProjectStarter'
import { ToolPageHeader } from '@/shared/components/ToolPage'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function ProjectStarterTool() {
  const { t } = useI18n()
  const [projectRoot, setProjectRoot] = useState('')
  const [items, setItems] = useState<AiProjectStarterItem[]>([...aiProjectStarterItems])
  const [preview, setPreview] = useState<AiProjectStarterPreview | null>(null)
  const [applied, setApplied] = useState<AiChangeApplyResult | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function chooseProject(): Promise<void> {
    const selected = await window.mootool.chooseDirectory(projectRoot || undefined)
    if (!selected) return
    setProjectRoot(selected)
    setPreview(null)
    setApplied(null)
    setError('')
  }

  async function buildPreview(): Promise<void> {
    setBusy(true)
    setError('')
    setPreview(null)
    setApplied(null)
    try {
      setPreview(await window.mootool.previewAiProjectStarter({ projectRoot, items }))
    } catch (previewError) {
      setError(previewError instanceof Error ? previewError.message : String(previewError))
    } finally {
      setBusy(false)
    }
  }

  async function apply(): Promise<void> {
    if (!preview) return
    setBusy(true)
    setError('')
    try {
      setApplied(await window.mootool.applyAiProjectStarter(preview.plan.id))
    } catch (applyError) {
      setError(applyError instanceof Error ? applyError.message : String(applyError))
    } finally {
      setBusy(false)
    }
  }

  async function rollback(): Promise<void> {
    if (!applied) return
    setBusy(true)
    setError('')
    try {
      await window.mootool.rollbackAiProjectStarter(applied.snapshotId)
      setPreview(null)
      setApplied(null)
    } catch (rollbackError) {
      setError(rollbackError instanceof Error ? rollbackError.message : String(rollbackError))
    } finally {
      setBusy(false)
    }
  }

  function toggleItem(item: AiProjectStarterItem, checked: boolean): void {
    setItems(checked ? [...items, item] : items.filter((candidate) => candidate !== item))
    setPreview(null)
    setApplied(null)
  }

  return <section className="tool-page p5-tool ai-project-starter">
    <ToolPageHeader title={t('projectStarter.title')} actions={<button className="toolbar-button" type="button" onClick={() => { void chooseProject() }}><FolderOpen size={14} />{projectRoot ? t('ai.changeProject') : t('ai.chooseProject')}</button>} />
    <div className="local-tool-shell ai-project-starter-shell"><div className="ai-project-starter-scroll">
      <section className="ai-project-starter-intro"><span><PackagePlus size={24} /></span><div><h2>{t('projectStarter.title')}</h2><p>{t('projectStarter.description')}</p></div></section>
      <div className="ai-change-safety"><ShieldCheck size={16} /><span>{t('projectStarter.safety')}</span></div>
      {error && <div className="ai-error" role="alert"><CircleAlert size={16} /><span>{error}</span></div>}
      <section className="ai-project-starter-config"><label><span>{t('projectStarter.project')}</span><button type="button" onClick={() => { void chooseProject() }}><code title={projectRoot}>{projectRoot || t('ai.chooseProject')}</code><FolderOpen size={14} /></button></label><fieldset>{aiProjectStarterItems.map((item) => <label key={item}><input type="checkbox" checked={items.includes(item)} disabled={Boolean(applied)} onChange={(event) => toggleItem(item, event.target.checked)} /><span>{t(`projectStarter.item.${item}` as 'projectStarter.item.instructions')}</span></label>)}</fieldset><button className="dialog-button dialog-button--primary" type="button" disabled={busy || !projectRoot || items.length === 0 || Boolean(applied)} onClick={() => { void buildPreview() }}><FileDiff size={14} />{busy ? t('common.processing') : t('projectStarter.preview')}</button></section>
      {preview && <section className="ai-project-starter-preview">
        {preview.skipped.length > 0 && <div className="ai-project-starter-skipped"><strong>{t('projectStarter.skipped')}</strong>{preview.skipped.map((item) => <p key={`${item.item}:${item.path}`}><span>{t(`projectStarter.item.${item.item}` as 'projectStarter.item.instructions')}</span><code title={item.path}>{item.path}</code><em>{t(`projectStarter.reason.${item.reason}` as 'projectStarter.reason.alreadyExists')}</em></p>)}</div>}
        {applied && <div className="ai-change-success"><CheckCircle2 size={16} /><span>{t('projectStarter.applied')}</span></div>}
        {preview.plan.operations.map((operation) => <article key={operation.id}><header><strong>{operation.summary}</strong><code>{operation.targetPath}</code></header><pre>{operation.redactedDiff}</pre></article>)}
        <footer>{applied ? <button className="dialog-button dialog-button--danger" type="button" disabled={busy} onClick={() => { void rollback() }}><RotateCcw size={14} />{t('projectStarter.rollback')}</button> : <button className="dialog-button dialog-button--primary" type="button" disabled={busy} onClick={() => { void apply() }}><ShieldCheck size={14} />{busy ? t('common.processing') : t('projectStarter.apply')}</button>}</footer>
      </section>}
    </div></div>
  </section>
}
