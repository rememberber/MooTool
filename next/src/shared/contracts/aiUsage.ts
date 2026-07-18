export const aiUsageSources = ['localLog', 'cli', 'providerApi', 'import', 'localRuntime'] as const
export type AiUsageSource = (typeof aiUsageSources)[number]

export type AiMoney = {
  currency: string
  micros: number
}

export type AiUsageEventInput = {
  source: AiUsageSource
  provider: string
  clientId: string
  projectId?: string
  agentProfileId?: string
  modelRuntimeId?: string
  localModelDigest?: string
  sessionId?: string
  model: string
  startedAt: string
  inputTokens: number
  outputTokens: number
  cachedInputTokens?: number
  cacheWriteTokens?: number
  reasoningTokens?: number
  requestCount?: number
  estimatedCost?: AiMoney
  billedCost?: AiMoney
  sourceFingerprint: string
}

export type AiUsageEvent = AiUsageEventInput & {
  id: string
  importedAt: string
}

export type AiUsageDashboardInput = {
  rangeDays: number
  timezoneOffsetMinutes: number
}

export type AiUsageTotals = {
  events: number
  requests: number
  inputTokens: number
  outputTokens: number
  cachedInputTokens: number
  cacheWriteTokens: number
  reasoningTokens: number
  totalTokens: number
  estimatedCosts: AiMoney[]
  billedCosts: AiMoney[]
}

export type AiUsageTrendPoint = AiUsageTotals & {
  date: string
}

export type AiUsageBreakdown = AiUsageTotals & {
  key: string
  label: string
}

export type AiUsageBudgetPeriod = 'daily' | 'weekly' | 'monthly'

export type AiUsageBudget = {
  period: AiUsageBudgetPeriod
  tokenLimit?: number
  costLimit?: AiMoney
  enabled: boolean
  updatedAt: string
}

export type AiUsageBudgetInput = {
  period: AiUsageBudgetPeriod
  tokenLimit?: number
  costLimit?: AiMoney
  enabled: boolean
}

export type AiUsageBudgetStatus = {
  budget: AiUsageBudget
  usedTokens: number
  tokenRatio?: number
  usedCost?: AiMoney
  costRatio?: number
}

export type AiUsageAnomaly = {
  date: string
  totalTokens: number
  baselineAverageTokens: number
  ratio: number
}

export type AiUsageDashboard = {
  generatedAt: string
  range: { from: string; to: string; days: number }
  totals: AiUsageTotals
  trend: AiUsageTrendPoint[]
  byModel: AiUsageBreakdown[]
  byClient: AiUsageBreakdown[]
  byProject: AiUsageBreakdown[]
  budgets: AiUsageBudgetStatus[]
  anomalies: AiUsageAnomaly[]
  lastImportedAt?: string
}

export type AiUsageImportPreviewInput = {
  paths: string[]
}

export type AiUsageImportFilePreview = {
  path: string
  sizeBytes: number
  clientId: string
  events: number
  models: string[]
  from?: string
  to?: string
  fields: string[]
  warnings: string[]
}

export type AiUsageImportPreview = {
  planId: string
  expiresAt: string
  files: AiUsageImportFilePreview[]
  events: number
  uniqueEvents: number
  duplicates: number
  from?: string
  to?: string
  fields: string[]
  warnings: string[]
}

export type AiUsageImportResult = {
  imported: number
  updated: number
  unchanged: number
  dashboard: AiUsageDashboard
}

export type AiUsageExportFormat = 'json' | 'csv'

export type AiUsageExportInput = AiUsageDashboardInput & {
  format: AiUsageExportFormat
}

export type AiUsageExportResult = {
  path: string
  format: AiUsageExportFormat
  events: number
}

export type AiUsageProviderSyncInput = AiUsageDashboardInput & {
  provider: 'openai'
}

export type AiUsageProviderSyncResult = {
  provider: 'openai'
  imported: number
  updated: number
  unchanged: number
  usageEvents: number
  costEvents: number
  dashboard: AiUsageDashboard
}

export function isAiUsageDashboardInput(value: unknown): value is AiUsageDashboardInput {
  return isRecord(value)
    && typeof value.rangeDays === 'number'
    && Number.isInteger(value.rangeDays)
    && value.rangeDays >= 1
    && value.rangeDays <= 365
    && typeof value.timezoneOffsetMinutes === 'number'
    && Number.isInteger(value.timezoneOffsetMinutes)
    && value.timezoneOffsetMinutes >= -840
    && value.timezoneOffsetMinutes <= 840
}

export function isAiUsageImportPreviewInput(value: unknown): value is AiUsageImportPreviewInput {
  return isRecord(value)
    && Array.isArray(value.paths)
    && value.paths.length >= 1
    && value.paths.length <= 100
    && value.paths.every((path) => typeof path === 'string' && path.trim().length > 0 && path.length <= 4096)
}

export function isAiUsageBudgetInput(value: unknown): value is AiUsageBudgetInput {
  return isRecord(value)
    && (value.period === 'daily' || value.period === 'weekly' || value.period === 'monthly')
    && (value.tokenLimit === undefined || isPositiveInteger(value.tokenLimit))
    && (value.costLimit === undefined || isMoney(value.costLimit))
    && typeof value.enabled === 'boolean'
    && (value.tokenLimit !== undefined || value.costLimit !== undefined)
}

export function isAiUsageExportInput(value: unknown): value is AiUsageExportInput {
  if (!isRecord(value) || !isAiUsageDashboardInput(value)) return false
  const format = (value as Record<string, unknown>).format
  return format === 'json' || format === 'csv'
}

export function isAiUsageProviderSyncInput(value: unknown): value is AiUsageProviderSyncInput {
  return isRecord(value) && value.provider === 'openai' && isAiUsageDashboardInput(value)
}

export function isAiUsagePlanId(value: unknown): value is string {
  return typeof value === 'string' && /^[0-9a-f-]{36}$/i.test(value)
}

function isMoney(value: unknown): value is AiMoney {
  return isRecord(value)
    && typeof value.currency === 'string'
    && /^[A-Z]{3}$/.test(value.currency)
    && typeof value.micros === 'number'
    && Number.isSafeInteger(value.micros)
    && value.micros >= 0
}

function isPositiveInteger(value: unknown): boolean {
  return typeof value === 'number' && Number.isSafeInteger(value) && value > 0
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
