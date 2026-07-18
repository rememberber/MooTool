import { CircleAlert, FolderOpen, RefreshCw, ShieldCheck } from 'lucide-react'
import type { ReactNode } from 'react'
import { ToolPageHeader } from '@/shared/components/ToolPage'
import type { AiDoctorSnapshot } from '@/shared/contracts/ai'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function AiManagerHeader({ title, snapshot, loading, onChooseProject, onRefresh, extraActions }: {
  title: string
  snapshot: AiDoctorSnapshot | null
  loading: boolean
  onChooseProject: () => void
  onRefresh: () => void
  extraActions?: ReactNode
}) {
  const { t } = useI18n()
  return (
    <ToolPageHeader title={title} actions={(
      <>
        {extraActions}
        <span className="ai-readonly-badge"><ShieldCheck size={14} />{t('ai.readOnly')}</span>
        <button className="toolbar-button" type="button" disabled={loading} onClick={onChooseProject}><FolderOpen size={14} />{snapshot?.projectRoot ? t('ai.changeProject') : t('ai.chooseProject')}</button>
        <button className="toolbar-button" type="button" disabled={loading} onClick={onRefresh}><RefreshCw size={14} className={loading ? 'spin' : undefined} />{t('common.refresh')}</button>
      </>
    )} />
  )
}

export function AiManagerBody({ snapshot, loading, error, children }: {
  snapshot: AiDoctorSnapshot | null
  loading: boolean
  error: string
  children: ReactNode
}) {
  const { t } = useI18n()
  return (
    <div className="local-tool-shell ai-manager-shell">
      <div className="ai-manager-scroll">
        <div className="ai-manager-scope"><span>{t('ai.project')}</span><strong title={snapshot?.projectRoot}>{snapshot?.projectRoot ?? t('ai.userScope')}</strong></div>
        {error && <div className="ai-error" role="alert"><CircleAlert size={16} /><span>{t('ai.scanFailed')}: {error}</span></div>}
        {loading && !snapshot ? <div className="history-empty">{t('ai.loading')}</div> : snapshot ? children : null}
      </div>
    </div>
  )
}

export function AiMetric({ label, value }: { label: string; value: string | number }) {
  return <article className="ai-manager-metric"><strong>{value}</strong><span>{label}</span></article>
}

export function formatAssetBytes(bytes: number): string {
  if (bytes < 1_000) return `${bytes} B`
  if (bytes < 1_000_000) return `${(bytes / 1_000).toFixed(1)} KB`
  return `${(bytes / 1_000_000).toFixed(1)} MB`
}
