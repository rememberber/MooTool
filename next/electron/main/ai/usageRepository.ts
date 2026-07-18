import { createHash, randomUUID } from 'node:crypto'
import { mkdirSync } from 'node:fs'
import { dirname } from 'node:path'
import { DatabaseSync } from 'node:sqlite'
import type {
  AiMoney,
  AiUsageBreakdown,
  AiUsageBudget,
  AiUsageBudgetInput,
  AiUsageBudgetPeriod,
  AiUsageBudgetStatus,
  AiUsageDashboard,
  AiUsageDashboardInput,
  AiUsageEvent,
  AiUsageEventInput,
  AiUsageImportResult,
  AiUsageTotals,
  AiUsageTrendPoint
} from '../../../src/shared/contracts/aiUsage'
import { initializeAiUsageSchema } from './usageSchema'

type UsageImportCounts = Pick<AiUsageImportResult, 'imported' | 'updated' | 'unchanged'>

export class AiUsageRepository {
  private readonly database: DatabaseSync
  private readonly clock: () => Date

  constructor(databasePath: string, clock: () => Date = () => new Date()) {
    mkdirSync(dirname(databasePath), { recursive: true })
    this.database = new DatabaseSync(databasePath)
    this.database.exec('PRAGMA journal_mode = WAL; PRAGMA busy_timeout = 3000;')
    initializeAiUsageSchema(this.database)
    this.clock = clock
  }

  import(events: AiUsageEventInput[]): UsageImportCounts {
    const counts: UsageImportCounts = { imported: 0, updated: 0, unchanged: 0 }
    const now = this.clock().toISOString()
    this.database.exec('BEGIN IMMEDIATE')
    try {
      for (const input of events) {
        assertUsageEvent(input)
        const revision = sourceRevision(input)
        const existing = this.database.prepare('SELECT source_revision FROM ai_usage_events WHERE source_fingerprint = ?').get(input.sourceFingerprint) as { source_revision?: unknown } | undefined
        if (existing?.source_revision === revision) {
          counts.unchanged += 1
          continue
        }
        const id = existing ? undefined : randomUUID()
        this.database.prepare(`
          INSERT INTO ai_usage_events (
            id, source, provider, client_id, project_id, agent_profile_id, model_runtime_id, local_model_digest,
            session_id, model, started_at, input_tokens, output_tokens, cached_input_tokens, cache_write_tokens,
            reasoning_tokens, request_count, estimated_cost_micros, estimated_cost_currency, billed_cost_micros,
            billed_cost_currency, source_fingerprint, source_revision, imported_at
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          ON CONFLICT(source_fingerprint) DO UPDATE SET
            source = excluded.source, provider = excluded.provider, client_id = excluded.client_id,
            project_id = excluded.project_id, agent_profile_id = excluded.agent_profile_id,
            model_runtime_id = excluded.model_runtime_id, local_model_digest = excluded.local_model_digest,
            session_id = excluded.session_id, model = excluded.model, started_at = excluded.started_at,
            input_tokens = excluded.input_tokens, output_tokens = excluded.output_tokens,
            cached_input_tokens = excluded.cached_input_tokens, cache_write_tokens = excluded.cache_write_tokens,
            reasoning_tokens = excluded.reasoning_tokens, request_count = excluded.request_count,
            estimated_cost_micros = excluded.estimated_cost_micros, estimated_cost_currency = excluded.estimated_cost_currency,
            billed_cost_micros = excluded.billed_cost_micros, billed_cost_currency = excluded.billed_cost_currency,
            source_revision = excluded.source_revision, imported_at = excluded.imported_at
        `).run(
          id ?? randomUUID(), input.source, input.provider, input.clientId, nullable(input.projectId), nullable(input.agentProfileId),
          nullable(input.modelRuntimeId), nullable(input.localModelDigest), nullable(input.sessionId), input.model, input.startedAt,
          input.inputTokens, input.outputTokens, nullableNumber(input.cachedInputTokens), nullableNumber(input.cacheWriteTokens),
          nullableNumber(input.reasoningTokens), nullableNumber(input.requestCount), nullableNumber(input.estimatedCost?.micros),
          nullable(input.estimatedCost?.currency), nullableNumber(input.billedCost?.micros), nullable(input.billedCost?.currency),
          input.sourceFingerprint, revision, now
        )
        if (existing) counts.updated += 1
        else counts.imported += 1
      }
      this.database.exec('COMMIT')
      return counts
    } catch (error) {
      this.database.exec('ROLLBACK')
      throw error
    }
  }

  dashboard(input: AiUsageDashboardInput): AiUsageDashboard {
    const now = this.clock()
    const range = dateRange(input.rangeDays, input.timezoneOffsetMinutes, now)
    const events = this.listBetween(range.from, range.to)
    const budgets = this.listBudgets().map((budget) => this.budgetStatus(budget, input.timezoneOffsetMinutes, now))
    const trend = buildTrend(events, input.rangeDays, input.timezoneOffsetMinutes, now)
    const lastImport = this.database.prepare('SELECT max(imported_at) AS value FROM ai_usage_events').get() as { value?: unknown }
    return {
      generatedAt: now.toISOString(),
      range: { ...range, days: input.rangeDays },
      totals: aggregate(events),
      trend,
      byModel: breakdown(events, (event) => event.model || 'unknown'),
      byClient: breakdown(events, (event) => event.clientId || 'unknown'),
      byProject: breakdown(events, (event) => event.projectId || 'Unassigned'),
      budgets,
      anomalies: detectAnomalies(trend),
      ...(typeof lastImport.value === 'string' ? { lastImportedAt: lastImport.value } : {})
    }
  }

  events(input: AiUsageDashboardInput): AiUsageEvent[] {
    const range = dateRange(input.rangeDays, input.timezoneOffsetMinutes, this.clock())
    return this.listBetween(range.from, range.to)
  }

  saveBudget(input: AiUsageBudgetInput): AiUsageBudget {
    const now = this.clock().toISOString()
    this.database.prepare(`
      INSERT INTO ai_usage_budgets(period, token_limit, cost_limit_micros, cost_currency, enabled, updated_at)
      VALUES (?, ?, ?, ?, ?, ?)
      ON CONFLICT(period) DO UPDATE SET token_limit = excluded.token_limit,
        cost_limit_micros = excluded.cost_limit_micros, cost_currency = excluded.cost_currency,
        enabled = excluded.enabled, updated_at = excluded.updated_at
    `).run(input.period, nullableNumber(input.tokenLimit), nullableNumber(input.costLimit?.micros), nullable(input.costLimit?.currency), input.enabled ? 1 : 0, now)
    this.database.prepare('DELETE FROM ai_usage_budget_notifications WHERE period = ?').run(input.period)
    return this.listBudgets().find((budget) => budget.period === input.period)!
  }

  claimBudgetNotification(period: AiUsageBudgetPeriod, periodKey: string, threshold: 50 | 80 | 100): boolean {
    const existing = this.database.prepare(`
      SELECT max(threshold) AS threshold FROM ai_usage_budget_notifications WHERE period = ? AND period_key = ?
    `).get(period, periodKey) as { threshold?: unknown }
    if (Number(existing.threshold ?? 0) >= threshold) return false
    const result = this.database.prepare(`
      INSERT OR IGNORE INTO ai_usage_budget_notifications(period, period_key, threshold, notified_at) VALUES (?, ?, ?, ?)
    `).run(period, periodKey, threshold, this.clock().toISOString())
    return Number(result.changes) > 0
  }

  countExistingFingerprints(fingerprints: string[]): number {
    if (fingerprints.length === 0) return 0
    let count = 0
    const query = this.database.prepare('SELECT 1 FROM ai_usage_events WHERE source_fingerprint = ?')
    for (const fingerprint of new Set(fingerprints)) if (query.get(fingerprint)) count += 1
    return count
  }

  clear(): number {
    return Number(this.database.prepare('DELETE FROM ai_usage_events').run().changes)
  }

  close(): void {
    if (this.database.isOpen) this.database.close()
  }

  private listBetween(from: string, to: string): AiUsageEvent[] {
    return this.database.prepare('SELECT * FROM ai_usage_events WHERE started_at >= ? AND started_at < ? ORDER BY started_at').all(from, to).map(mapUsageEvent)
  }

  private listBudgets(): AiUsageBudget[] {
    return this.database.prepare('SELECT * FROM ai_usage_budgets ORDER BY CASE period WHEN \'daily\' THEN 1 WHEN \'weekly\' THEN 2 ELSE 3 END').all().map((row) => {
      const record = row as Record<string, unknown>
      return {
        period: record.period as AiUsageBudgetPeriod,
        ...(record.token_limit == null ? {} : { tokenLimit: Number(record.token_limit) }),
        ...(record.cost_limit_micros == null || typeof record.cost_currency !== 'string' ? {} : { costLimit: { micros: Number(record.cost_limit_micros), currency: record.cost_currency } }),
        enabled: Boolean(record.enabled),
        updatedAt: String(record.updated_at)
      }
    })
  }

  private budgetStatus(budget: AiUsageBudget, offset: number, now: Date): AiUsageBudgetStatus {
    const from = periodStart(budget.period, offset, now)
    const totals = aggregate(this.listBetween(from, new Date(now.getTime() + 1).toISOString()))
    const usedCost = budget.costLimit ? totals.billedCosts.find((money) => money.currency === budget.costLimit!.currency)
      ?? totals.estimatedCosts.find((money) => money.currency === budget.costLimit!.currency) : undefined
    return {
      budget,
      usedTokens: totals.totalTokens,
      ...(budget.tokenLimit ? { tokenRatio: totals.totalTokens / budget.tokenLimit } : {}),
      ...(usedCost ? { usedCost, costRatio: budget.costLimit && budget.costLimit.micros > 0 ? usedCost.micros / budget.costLimit.micros : undefined } : {})
    }
  }
}

function mapUsageEvent(row: Record<string, unknown>): AiUsageEvent {
  return {
    id: String(row.id),
    source: row.source as AiUsageEvent['source'],
    provider: String(row.provider),
    clientId: String(row.client_id),
    ...(row.project_id == null ? {} : { projectId: String(row.project_id) }),
    ...(row.agent_profile_id == null ? {} : { agentProfileId: String(row.agent_profile_id) }),
    ...(row.model_runtime_id == null ? {} : { modelRuntimeId: String(row.model_runtime_id) }),
    ...(row.local_model_digest == null ? {} : { localModelDigest: String(row.local_model_digest) }),
    ...(row.session_id == null ? {} : { sessionId: String(row.session_id) }),
    model: String(row.model),
    startedAt: String(row.started_at),
    inputTokens: Number(row.input_tokens),
    outputTokens: Number(row.output_tokens),
    ...(row.cached_input_tokens == null ? {} : { cachedInputTokens: Number(row.cached_input_tokens) }),
    ...(row.cache_write_tokens == null ? {} : { cacheWriteTokens: Number(row.cache_write_tokens) }),
    ...(row.reasoning_tokens == null ? {} : { reasoningTokens: Number(row.reasoning_tokens) }),
    ...(row.request_count == null ? {} : { requestCount: Number(row.request_count) }),
    ...(money(row.estimated_cost_currency, row.estimated_cost_micros) ? { estimatedCost: money(row.estimated_cost_currency, row.estimated_cost_micros)! } : {}),
    ...(money(row.billed_cost_currency, row.billed_cost_micros) ? { billedCost: money(row.billed_cost_currency, row.billed_cost_micros)! } : {}),
    sourceFingerprint: String(row.source_fingerprint),
    importedAt: String(row.imported_at)
  }
}

function aggregate(events: AiUsageEvent[]): AiUsageTotals {
  const totals: AiUsageTotals = emptyTotals()
  const estimated = new Map<string, number>()
  const billed = new Map<string, number>()
  for (const event of events) {
    totals.events += 1
    totals.requests += event.requestCount ?? 1
    totals.inputTokens += event.inputTokens
    totals.outputTokens += event.outputTokens
    totals.cachedInputTokens += event.cachedInputTokens ?? 0
    totals.cacheWriteTokens += event.cacheWriteTokens ?? 0
    totals.reasoningTokens += event.reasoningTokens ?? 0
    if (event.estimatedCost) estimated.set(event.estimatedCost.currency, (estimated.get(event.estimatedCost.currency) ?? 0) + event.estimatedCost.micros)
    if (event.billedCost) billed.set(event.billedCost.currency, (billed.get(event.billedCost.currency) ?? 0) + event.billedCost.micros)
  }
  totals.totalTokens = totals.inputTokens + totals.outputTokens
  totals.estimatedCosts = mapMoney(estimated)
  totals.billedCosts = mapMoney(billed)
  return totals
}

function breakdown(events: AiUsageEvent[], keyOf: (event: AiUsageEvent) => string): AiUsageBreakdown[] {
  const groups = new Map<string, AiUsageEvent[]>()
  for (const event of events) {
    const key = keyOf(event)
    groups.set(key, [...(groups.get(key) ?? []), event])
  }
  return [...groups.entries()].map(([key, values]) => ({ key, label: key, ...aggregate(values) }))
    .sort((left, right) => right.totalTokens - left.totalTokens || left.label.localeCompare(right.label)).slice(0, 50)
}

function buildTrend(events: AiUsageEvent[], days: number, offset: number, now: Date): AiUsageTrendPoint[] {
  const groups = new Map<string, AiUsageEvent[]>()
  for (const event of events) {
    const date = localDate(new Date(event.startedAt), offset)
    groups.set(date, [...(groups.get(date) ?? []), event])
  }
  const points: AiUsageTrendPoint[] = []
  const today = localDate(now, offset)
  const cursor = new Date(`${today}T00:00:00.000Z`)
  cursor.setUTCDate(cursor.getUTCDate() - days + 1)
  for (let index = 0; index < days; index += 1) {
    const date = cursor.toISOString().slice(0, 10)
    points.push({ date, ...aggregate(groups.get(date) ?? []) })
    cursor.setUTCDate(cursor.getUTCDate() + 1)
  }
  return points
}

function detectAnomalies(trend: AiUsageTrendPoint[]): AiUsageDashboard['anomalies'] {
  const anomalies: AiUsageDashboard['anomalies'] = []
  for (let index = 3; index < trend.length; index += 1) {
    const baseline = trend.slice(Math.max(0, index - 7), index).map((point) => point.totalTokens).filter((tokens) => tokens > 0)
    if (baseline.length < 3) continue
    const average = baseline.reduce((sum, tokens) => sum + tokens, 0) / baseline.length
    const ratio = average > 0 ? trend[index].totalTokens / average : 0
    if (trend[index].totalTokens < 10_000 || ratio < 2) continue
    anomalies.push({ date: trend[index].date, totalTokens: trend[index].totalTokens, baselineAverageTokens: Math.round(average), ratio })
  }
  return anomalies.sort((left, right) => right.ratio - left.ratio || right.date.localeCompare(left.date))
}

function dateRange(days: number, offset: number, now: Date): { from: string; to: string } {
  const today = localDate(now, offset)
  const start = new Date(`${today}T00:00:00.000Z`)
  start.setUTCDate(start.getUTCDate() - days + 1)
  start.setUTCMinutes(start.getUTCMinutes() + offset)
  return { from: start.toISOString(), to: new Date(now.getTime() + 1).toISOString() }
}

function periodStart(period: AiUsageBudgetPeriod, offset: number, now: Date): string {
  const today = localDate(now, offset)
  const start = new Date(`${today}T00:00:00.000Z`)
  if (period === 'weekly') {
    const day = start.getUTCDay()
    start.setUTCDate(start.getUTCDate() - (day === 0 ? 6 : day - 1))
  } else if (period === 'monthly') {
    start.setUTCDate(1)
  }
  start.setUTCMinutes(start.getUTCMinutes() + offset)
  return start.toISOString()
}

function localDate(value: Date, offset: number): string {
  return new Date(value.getTime() - offset * 60_000).toISOString().slice(0, 10)
}

function emptyTotals(): AiUsageTotals {
  return { events: 0, requests: 0, inputTokens: 0, outputTokens: 0, cachedInputTokens: 0, cacheWriteTokens: 0, reasoningTokens: 0, totalTokens: 0, estimatedCosts: [], billedCosts: [] }
}

function mapMoney(values: Map<string, number>): AiMoney[] {
  return [...values.entries()].map(([currency, micros]) => ({ currency, micros })).sort((left, right) => left.currency.localeCompare(right.currency))
}

function money(currency: unknown, micros: unknown): AiMoney | undefined {
  return typeof currency === 'string' && typeof micros === 'number' ? { currency, micros } : undefined
}

function sourceRevision(input: AiUsageEventInput): string {
  return createHash('sha256').update(JSON.stringify(input)).digest('hex')
}

function assertUsageEvent(input: AiUsageEventInput): void {
  const strings = [input.provider, input.clientId, input.model, input.sourceFingerprint]
  if (strings.some((value) => !value.trim() || value.length > 4096)) throw new Error('Usage event contains an invalid required field')
  if (!Number.isFinite(Date.parse(input.startedAt))) throw new Error('Usage event has an invalid timestamp')
  const numbers = [input.inputTokens, input.outputTokens, input.cachedInputTokens, input.cacheWriteTokens, input.reasoningTokens, input.requestCount]
  if (numbers.some((value) => value !== undefined && (!Number.isSafeInteger(value) || value < 0))) throw new Error('Usage event contains an invalid token count')
  for (const cost of [input.estimatedCost, input.billedCost]) {
    if (cost && (!/^[A-Z]{3}$/.test(cost.currency) || !Number.isSafeInteger(cost.micros) || cost.micros < 0)) throw new Error('Usage event contains an invalid cost')
  }
}

function nullable(value?: string): string | null {
  return value?.trim() || null
}

function nullableNumber(value?: number): number | null {
  return value ?? null
}
