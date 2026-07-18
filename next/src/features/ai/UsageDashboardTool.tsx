import { ChartColumn, CircleAlert, CloudDownload, Coins, Download, FileJson, Gauge, KeyRound, RefreshCw, Settings2, ShieldCheck, Trash2, Upload } from 'lucide-react'
import { useEffect, useState } from 'react'
import { AiMetric } from './AiManagerChrome'
import type { AiMoney, AiUsageBreakdown, AiUsageBudgetInput, AiUsageBudgetPeriod, AiUsageDashboard, AiUsageExportFormat, AiUsageImportPreview } from '@/shared/contracts/aiUsage'
import { Dialog } from '@/shared/components/Dialog'
import { ToolPageHeader } from '@/shared/components/ToolPage'
import { useI18n } from '@/shared/i18n/I18nProvider'
import type { SecretStatus } from '@/shared/contracts/settings'

const ranges = [1, 7, 30] as const

export function UsageDashboardTool() {
  const { t } = useI18n()
  const [rangeDays, setRangeDays] = useState<number>(7)
  const [dashboard, setDashboard] = useState<AiUsageDashboard | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [importPreview, setImportPreview] = useState<AiUsageImportPreview | null>(null)
  const [importBusy, setImportBusy] = useState(false)
  const [importError, setImportError] = useState('')
  const [budgetOpen, setBudgetOpen] = useState(false)
  const [budgetDraft, setBudgetDraft] = useState({ period: 'monthly' as AiUsageBudgetPeriod, tokenLimit: '', costLimit: '', enabled: true })
  const [budgetBusy, setBudgetBusy] = useState(false)
  const [exportFormat, setExportFormat] = useState<AiUsageExportFormat>('json')
  const [exportBusy, setExportBusy] = useState(false)
  const [exportMessage, setExportMessage] = useState('')
  const [providerOpen, setProviderOpen] = useState(false)
  const [providerStatus, setProviderStatus] = useState<SecretStatus | null>(null)
  const [providerKey, setProviderKey] = useState('')
  const [providerBusy, setProviderBusy] = useState(false)
  const [providerError, setProviderError] = useState('')
  const [providerMessage, setProviderMessage] = useState('')
  const timezoneOffsetMinutes = new Date().getTimezoneOffset()

  async function load(days = rangeDays): Promise<void> {
    setLoading(true)
    setError('')
    try {
      setDashboard(await window.mootool.getAiUsageDashboard({ rangeDays: days, timezoneOffsetMinutes }))
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : String(loadError))
    } finally {
      setLoading(false)
    }
  }

  async function chooseImport(): Promise<void> {
    const paths = await window.mootool.chooseAiUsageFiles()
    if (paths.length === 0) return
    setImportBusy(true)
    setImportError('')
    try {
      setImportPreview(await window.mootool.previewAiUsageImport({ paths }))
    } catch (previewError) {
      setImportError(previewError instanceof Error ? previewError.message : String(previewError))
      setImportPreview({ planId: '', expiresAt: '', files: [], events: 0, uniqueEvents: 0, duplicates: 0, fields: [], warnings: [] })
    } finally {
      setImportBusy(false)
    }
  }

  async function applyImport(): Promise<void> {
    if (!importPreview?.planId) return
    setImportBusy(true)
    setImportError('')
    try {
      const result = await window.mootool.applyAiUsageImport(importPreview.planId, timezoneOffsetMinutes)
      setDashboard(rangeDays === 30 ? result.dashboard : await window.mootool.getAiUsageDashboard({ rangeDays, timezoneOffsetMinutes }))
      setImportPreview(null)
    } catch (applyError) {
      setImportError(applyError instanceof Error ? applyError.message : String(applyError))
    } finally {
      setImportBusy(false)
    }
  }

  function openBudget(): void {
    const existing = dashboard?.budgets.find((item) => item.budget.period === 'monthly')?.budget
    setBudgetDraft({
      period: existing?.period ?? 'monthly',
      tokenLimit: existing?.tokenLimit?.toString() ?? '',
      costLimit: existing?.costLimit ? (existing.costLimit.micros / 1_000_000).toString() : '',
      enabled: existing?.enabled ?? true
    })
    setBudgetOpen(true)
  }

  async function saveBudget(): Promise<void> {
    const input: AiUsageBudgetInput = {
      period: budgetDraft.period,
      ...(budgetDraft.tokenLimit ? { tokenLimit: Number(budgetDraft.tokenLimit) } : {}),
      ...(budgetDraft.costLimit ? { costLimit: { currency: 'USD', micros: Math.round(Number(budgetDraft.costLimit) * 1_000_000) } } : {}),
      enabled: budgetDraft.enabled
    }
    setBudgetBusy(true)
    try {
      await window.mootool.saveAiUsageBudget(input)
      setBudgetOpen(false)
      await load()
    } finally {
      setBudgetBusy(false)
    }
  }

  async function clearUsage(): Promise<void> {
    if (!window.confirm(t('usage.clearConfirm'))) return
    await window.mootool.clearAiUsage()
    await load()
  }

  async function exportUsage(): Promise<void> {
    setExportBusy(true)
    setExportMessage('')
    try {
      const result = await window.mootool.exportAiUsage({ rangeDays, timezoneOffsetMinutes, format: exportFormat })
      if (result) setExportMessage(t('usage.export.done', { count: String(result.events), path: result.path }))
    } catch (exportError) {
      setError(exportError instanceof Error ? exportError.message : String(exportError))
    } finally {
      setExportBusy(false)
    }
  }

  async function openProvider(): Promise<void> {
    setProviderOpen(true)
    setProviderError('')
    setProviderMessage('')
    try { setProviderStatus(await window.mootool.getSecretStatus('openAiAdminApiKey')) } catch (statusError) {
      setProviderError(statusError instanceof Error ? statusError.message : String(statusError))
    }
  }

  async function saveProviderKey(): Promise<void> {
    if (!providerKey) return
    setProviderBusy(true)
    setProviderError('')
    try {
      setProviderStatus(await window.mootool.setSecret('openAiAdminApiKey', providerKey))
      setProviderKey('')
      setProviderMessage(t('usage.provider.keySaved'))
    } catch (saveError) {
      setProviderError(saveError instanceof Error ? saveError.message : String(saveError))
    } finally { setProviderBusy(false) }
  }

  async function clearProviderKey(): Promise<void> {
    setProviderBusy(true)
    setProviderError('')
    try {
      setProviderStatus(await window.mootool.clearSecret('openAiAdminApiKey'))
      setProviderKey('')
      setProviderMessage(t('usage.provider.keyCleared'))
    } catch (clearError) {
      setProviderError(clearError instanceof Error ? clearError.message : String(clearError))
    } finally { setProviderBusy(false) }
  }

  async function syncProvider(): Promise<void> {
    setProviderBusy(true)
    setProviderError('')
    setProviderMessage('')
    try {
      const result = await window.mootool.syncAiUsageProvider({ provider: 'openai', rangeDays, timezoneOffsetMinutes })
      setDashboard(result.dashboard)
      setProviderMessage(t('usage.provider.syncDone', { usage: String(result.usageEvents), costs: String(result.costEvents), imported: String(result.imported), unchanged: String(result.unchanged) }))
    } catch (syncError) {
      setProviderError(syncError instanceof Error ? syncError.message : String(syncError))
    } finally { setProviderBusy(false) }
  }

  useEffect(() => { void load(rangeDays) }, [rangeDays])

  const totals = dashboard?.totals
  const cost = preferredCost(totals?.billedCosts ?? [], totals?.estimatedCosts ?? [])
  return <section className="tool-page p5-tool ai-usage-dashboard">
    <ToolPageHeader title={t('usage.title')} actions={<><div className="ai-usage-range">{ranges.map((days) => <button className={rangeDays === days ? 'is-active' : ''} type="button" key={days} onClick={() => setRangeDays(days)}>{t(`usage.range.${days}` as 'usage.range.7')}</button>)}</div><button className="toolbar-button" type="button" onClick={() => { void openProvider() }}><CloudDownload size={14} />{t('usage.provider.action')}</button><button className="toolbar-button" type="button" onClick={() => { void chooseImport() }}><Upload size={14} />{t('usage.import.action')}</button><div className="ai-usage-export"><select aria-label={t('usage.export.format')} value={exportFormat} onChange={(event) => setExportFormat(event.target.value as AiUsageExportFormat)}><option value="json">JSON</option><option value="csv">CSV</option></select><button className="toolbar-button" type="button" disabled={exportBusy} onClick={() => { void exportUsage() }}><Download size={14} />{t('usage.export.action')}</button></div><button className="toolbar-button" type="button" onClick={openBudget}><Settings2 size={14} />{t('usage.budget.action')}</button><button className="toolbar-button" type="button" disabled={loading} onClick={() => { void load() }}><RefreshCw size={14} className={loading ? 'spin' : undefined} />{t('common.refresh')}</button></>} />
    <div className="local-tool-shell ai-usage-shell"><div className="ai-usage-scroll">
      <div className="ai-usage-privacy"><ShieldCheck size={15} /><span>{t('usage.privacy')}</span>{dashboard?.lastImportedAt && <time>{t('usage.lastImport', { time: new Date(dashboard.lastImportedAt).toLocaleString() })}</time>}</div>
      {exportMessage && <div className="ai-usage-export-message"><Download size={14} /><span>{exportMessage}</span></div>}
      {(error || (importError && !importPreview)) && <div className="ai-error" role="alert"><CircleAlert size={16} /><span>{error || importError}</span></div>}
      {loading && !dashboard ? <div className="history-empty">{t('usage.loading')}</div> : dashboard && <>
        <div className="ai-manager-metrics"><AiMetric label={t('usage.metric.total')} value={formatNumber(totals!.totalTokens)} /><AiMetric label={t('usage.metric.input')} value={formatNumber(totals!.inputTokens)} /><AiMetric label={t('usage.metric.output')} value={formatNumber(totals!.outputTokens)} /><AiMetric label={cost.billed ? t('usage.metric.billed') : t('usage.metric.estimated')} value={cost.label} /></div>

        <section className="ai-usage-token-split"><span><strong>{formatNumber(totals!.cachedInputTokens)}</strong>{t('usage.metric.cacheRead')}</span><span><strong>{formatNumber(totals!.cacheWriteTokens)}</strong>{t('usage.metric.cacheWrite')}</span><span><strong>{formatNumber(totals!.reasoningTokens)}</strong>{t('usage.metric.reasoning')}</span><span><strong>{formatNumber(totals!.requests)}</strong>{t('usage.metric.requests')}</span><span><strong>{formatNumber(totals!.events)}</strong>{t('usage.metric.events')}</span></section>

        {dashboard.anomalies.length > 0 && <section className="ai-usage-anomalies"><header><CircleAlert size={16} /><h2>{t('usage.anomaly.title')}</h2></header>{dashboard.anomalies.map((anomaly) => <article key={anomaly.date}><strong>{anomaly.date}</strong><span>{t('usage.anomaly.detail', { current: formatNumber(anomaly.totalTokens), baseline: formatNumber(anomaly.baselineAverageTokens), ratio: anomaly.ratio.toFixed(1) })}</span></article>)}</section>}

        <section className="ai-usage-trend"><header><div><h2>{t('usage.trend.title')}</h2><p>{t('usage.trend.description')}</p></div><ChartColumn size={17} /></header><Trend dashboard={dashboard} /></section>

        {dashboard.budgets.length > 0 && <section className="ai-usage-budgets"><header><h2>{t('usage.budget.title')}</h2></header><div>{dashboard.budgets.map((status) => <article key={status.budget.period}><header><strong>{t(`usage.budget.period.${status.budget.period}` as 'usage.budget.period.monthly')}</strong><span>{status.budget.enabled ? t('usage.budget.enabled') : t('usage.budget.disabled')}</span></header>{status.budget.tokenLimit && <BudgetProgress label={t('usage.budget.tokens')} used={status.usedTokens} limit={status.budget.tokenLimit} ratio={status.tokenRatio ?? 0} formatter={formatNumber} />}{status.budget.costLimit && <BudgetProgress label={t('usage.budget.cost')} used={status.usedCost?.micros ?? 0} limit={status.budget.costLimit.micros} ratio={status.costRatio ?? 0} formatter={(value) => formatMoney({ currency: status.budget.costLimit!.currency, micros: value })} />}</article>)}</div></section>}

        {totals!.events > 0 ? <div className="ai-usage-breakdowns"><Breakdown title={t('usage.breakdown.model')} items={dashboard.byModel} /><Breakdown title={t('usage.breakdown.client')} items={dashboard.byClient} /><Breakdown title={t('usage.breakdown.project')} items={dashboard.byProject} /></div> : <div className="ai-usage-empty"><Coins size={28} /><strong>{t('usage.empty.title')}</strong><p>{t('usage.empty.description')}</p></div>}

        {totals!.events > 0 && <div className="ai-usage-danger"><button className="toolbar-button" type="button" onClick={() => { void clearUsage() }}><Trash2 size={14} />{t('usage.clear')}</button><span>{t('usage.clearHint')}</span></div>}
      </>}
    </div></div>

    <Dialog title={t('usage.import.title')} open={Boolean(importPreview)} width={800} onClose={() => { if (!importBusy) setImportPreview(null) }} footer={<><button className="dialog-button" type="button" disabled={importBusy} onClick={() => setImportPreview(null)}>{t('common.cancel')}</button><button className="dialog-button dialog-button--primary" type="button" disabled={importBusy || !importPreview?.planId || (importPreview?.uniqueEvents ?? 0) === 0} onClick={() => { void applyImport() }}><Upload size={14} />{importBusy ? t('common.processing') : t('usage.import.apply')}</button></>}>
      {importPreview && <div className="ai-usage-import"><div className="ai-usage-import-summary"><span><strong>{importPreview.files.length}</strong>{t('usage.import.files')}</span><span><strong>{importPreview.events}</strong>{t('usage.import.events')}</span><span><strong>{importPreview.uniqueEvents}</strong>{t('usage.import.unique')}</span><span><strong>{importPreview.duplicates}</strong>{t('usage.import.duplicates')}</span></div><div className="ai-usage-import-safety"><ShieldCheck size={15} /><span>{t('usage.import.safety')}</span></div>{importError && <div className="ai-error" role="alert"><CircleAlert size={16} /><span>{importError}</span></div>}<div className="ai-usage-import-files">{importPreview.files.map((file) => <article key={file.path}><FileJson size={16} /><div><strong title={file.path}>{file.path}</strong><span>{file.clientId} · {file.models.join(', ') || '—'} · {formatBytes(file.sizeBytes)} · {t('usage.import.fileEvents', { count: String(file.events) })}</span><small>{file.fields.join(' · ') || t('usage.import.noFields')}</small>{file.warnings.map((warning) => <em key={warning}>{warning}</em>)}</div></article>)}</div>{importPreview.warnings.length > 0 && <div className="ai-usage-import-warnings">{importPreview.warnings.map((warning) => <span key={warning}><CircleAlert size={13} />{warning}</span>)}</div>}</div>}
    </Dialog>

    <Dialog title={t('usage.budget.title')} open={budgetOpen} width={520} onClose={() => { if (!budgetBusy) setBudgetOpen(false) }} footer={<><button className="dialog-button" type="button" disabled={budgetBusy} onClick={() => setBudgetOpen(false)}>{t('common.cancel')}</button><button className="dialog-button dialog-button--primary" type="button" disabled={budgetBusy || (!budgetDraft.tokenLimit && !budgetDraft.costLimit)} onClick={() => { void saveBudget() }}><Gauge size={14} />{budgetBusy ? t('common.processing') : t('common.save')}</button></>}>
      <div className="ai-usage-budget-editor"><label><span>{t('usage.budget.period')}</span><select value={budgetDraft.period} onChange={(event) => setBudgetDraft({ ...budgetDraft, period: event.target.value as AiUsageBudgetPeriod })}><option value="daily">{t('usage.budget.period.daily')}</option><option value="weekly">{t('usage.budget.period.weekly')}</option><option value="monthly">{t('usage.budget.period.monthly')}</option></select></label><label><span>{t('usage.budget.tokenLimit')}</span><input type="number" min="1" step="1000" value={budgetDraft.tokenLimit} onChange={(event) => setBudgetDraft({ ...budgetDraft, tokenLimit: event.target.value })} /></label><label><span>{t('usage.budget.costLimit')}</span><input type="number" min="0" step="0.01" value={budgetDraft.costLimit} onChange={(event) => setBudgetDraft({ ...budgetDraft, costLimit: event.target.value })} /></label><label className="ai-usage-budget-toggle"><input type="checkbox" checked={budgetDraft.enabled} onChange={(event) => setBudgetDraft({ ...budgetDraft, enabled: event.target.checked })} /><span>{t('usage.budget.enabled')}</span></label></div>
    </Dialog>

    <Dialog title={t('usage.provider.title')} open={providerOpen} width={620} onClose={() => { if (!providerBusy) setProviderOpen(false) }} footer={<><button className="dialog-button" type="button" disabled={providerBusy} onClick={() => setProviderOpen(false)}>{t('common.close')}</button><button className="dialog-button dialog-button--primary" type="button" disabled={providerBusy || !providerStatus?.stored} onClick={() => { void syncProvider() }}><CloudDownload size={14} />{providerBusy ? t('common.processing') : t('usage.provider.sync')}</button></>}>
      <div className="ai-usage-provider"><div className="ai-usage-provider-safety"><ShieldCheck size={15} /><span>{t('usage.provider.safety')}</span></div><article><header><div><strong>OpenAI Usage &amp; Costs API</strong><span>{providerStatus?.stored ? t('usage.provider.configured') : t('usage.provider.notConfigured')}</span></div><KeyRound size={17} /></header><p>{t('usage.provider.description', { days: String(rangeDays) })}</p><label><span>{t('usage.provider.adminKey')}</span><input type="password" autoComplete="new-password" value={providerKey} disabled={providerBusy || providerStatus?.encryptionAvailable === false} placeholder={providerStatus?.stored ? t('usage.provider.keyStored') : ''} onChange={(event) => setProviderKey(event.target.value)} /></label><div className="ai-usage-provider-actions"><button className="toolbar-button" type="button" disabled={providerBusy || !providerKey} onClick={() => { void saveProviderKey() }}>{t('usage.provider.saveKey')}</button>{providerStatus?.stored && <button className="toolbar-button" type="button" disabled={providerBusy} onClick={() => { void clearProviderKey() }}>{t('usage.provider.clearKey')}</button>}</div>{providerStatus?.encryptionAvailable === false && <div className="ai-error"><CircleAlert size={14} /><span>{t('usage.provider.secureStorageUnavailable')}</span></div>}{providerError && <div className="ai-error" role="alert"><CircleAlert size={14} /><span>{providerError}</span></div>}{providerMessage && <div className="ai-usage-provider-message"><ShieldCheck size={14} /><span>{providerMessage}</span></div>}</article></div>
    </Dialog>
  </section>

  function Breakdown({ title, items }: { title: string; items: AiUsageBreakdown[] }) {
    const maximum = Math.max(1, ...items.map((item) => item.totalTokens))
    return <section><h2>{title}</h2>{items.length > 0 ? <div>{items.slice(0, 8).map((item) => <article key={item.key}><header><strong title={item.label}>{item.label}</strong><span>{formatNumber(item.totalTokens)}</span></header><div><i style={{ width: `${Math.max(2, item.totalTokens / maximum * 100)}%` }} /></div><small>{t('usage.breakdown.detail', { input: formatNumber(item.inputTokens), output: formatNumber(item.outputTokens) })}</small></article>)}</div> : <div className="history-empty">—</div>}</section>
  }
}

function Trend({ dashboard }: { dashboard: AiUsageDashboard }) {
  const maximum = Math.max(1, ...dashboard.trend.map((point) => point.totalTokens))
  return <div className="ai-usage-chart">{dashboard.trend.map((point) => <div key={point.date} title={`${point.date}: ${point.totalTokens.toLocaleString()}`}><span><i style={{ height: `${Math.max(point.totalTokens ? 4 : 0, point.totalTokens / maximum * 100)}%` }} /></span><small>{point.date.slice(5)}</small></div>)}</div>
}

function BudgetProgress({ label, used, limit, ratio, formatter }: { label: string; used: number; limit: number; ratio: number; formatter: (value: number) => string }) {
  const tone = ratio >= 1 ? 'danger' : ratio >= 0.8 ? 'warning' : ratio >= 0.5 ? 'notice' : 'normal'
  return <div className={`ai-usage-budget-progress ai-usage-budget-progress--${tone}`}><header><span>{label}</span><strong>{formatter(used)} / {formatter(limit)}</strong></header><div><i style={{ width: `${Math.min(100, ratio * 100)}%` }} /></div><small>{Math.round(ratio * 100)}%</small></div>
}

function preferredCost(billed: AiMoney[], estimated: AiMoney[]): { label: string; billed: boolean } {
  if (billed.length > 0) return { label: billed.map(formatMoney).join(' · '), billed: true }
  if (estimated.length > 0) return { label: estimated.map(formatMoney).join(' · '), billed: false }
  return { label: '—', billed: false }
}

function formatMoney(value: AiMoney): string {
  try { return new Intl.NumberFormat(undefined, { style: 'currency', currency: value.currency }).format(value.micros / 1_000_000) } catch { return `${value.currency} ${(value.micros / 1_000_000).toFixed(2)}` }
}

function formatNumber(value: number): string {
  return value.toLocaleString()
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 ** 2).toFixed(1)} MB`
}
