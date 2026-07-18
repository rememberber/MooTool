import { createHash } from 'node:crypto'
import type { AiUsageEventInput, AiUsageProviderSyncInput, AiUsageProviderSyncResult } from '../../../src/shared/contracts/aiUsage'
import type { AiUsageRepository } from './usageRepository'

type Fetcher = (input: string | URL | Request, init?: RequestInit) => Promise<Response>

type OpenAiUsageSyncServiceOptions = {
  repository: AiUsageRepository
  credentialProvider: () => string
  fetcher?: Fetcher
  baseUrl?: string
  clock?: () => Date
  requestTimeoutMs?: number
}

type ProviderBucket = {
  start_time?: unknown
  end_time?: unknown
  results?: unknown
}

export class OpenAiUsageSyncService {
  private readonly repository: AiUsageRepository
  private readonly credentialProvider: () => string
  private readonly fetcher: Fetcher
  private readonly baseUrl: string
  private readonly clock: () => Date
  private readonly requestTimeoutMs: number

  constructor(options: OpenAiUsageSyncServiceOptions) {
    this.repository = options.repository
    this.credentialProvider = options.credentialProvider
    this.fetcher = options.fetcher ?? fetch
    this.baseUrl = options.baseUrl ?? 'https://api.openai.com'
    this.clock = options.clock ?? (() => new Date())
    this.requestTimeoutMs = options.requestTimeoutMs ?? 15_000
  }

  async sync(input: AiUsageProviderSyncInput): Promise<AiUsageProviderSyncResult> {
    const apiKey = this.credentialProvider()
    if (!apiKey) throw new Error('OpenAI Admin API key is not configured')
    const endTime = Math.floor(this.clock().getTime() / 1000) + 1
    const startTime = endTime - input.rangeDays * 86_400
    const [usageBuckets, costBuckets] = await Promise.all([
      this.fetchPages('/v1/organization/usage/completions', apiKey, {
        start_time: String(startTime), end_time: String(endTime), bucket_width: '1d', limit: String(Math.min(31, input.rangeDays)),
        group_by: ['model', 'project_id']
      }),
      this.fetchPages('/v1/organization/costs', apiKey, {
        start_time: String(startTime), end_time: String(endTime), bucket_width: '1d', limit: String(Math.min(180, input.rangeDays)),
        group_by: ['project_id', 'line_item']
      })
    ])
    const usageEvents = normalizeUsageBuckets(usageBuckets)
    const costEvents = normalizeCostBuckets(costBuckets)
    const counts = this.repository.import([...usageEvents, ...costEvents])
    return {
      provider: 'openai',
      ...counts,
      usageEvents: usageEvents.length,
      costEvents: costEvents.length,
      dashboard: this.repository.dashboard(input)
    }
  }

  private async fetchPages(path: string, apiKey: string, parameters: Record<string, string | string[]>): Promise<ProviderBucket[]> {
    const buckets: ProviderBucket[] = []
    const seenPages = new Set<string>()
    let page = ''
    for (let pageIndex = 0; pageIndex < 100; pageIndex += 1) {
      const url = new URL(path, this.baseUrl)
      for (const [key, rawValue] of Object.entries(parameters)) {
        for (const value of Array.isArray(rawValue) ? rawValue : [rawValue]) url.searchParams.append(key, value)
      }
      if (page) url.searchParams.set('page', page)
      const response = await this.fetchResponse(url, apiKey)
      const data = Array.isArray(response.data) ? response.data.filter(isRecord) as ProviderBucket[] : []
      buckets.push(...data)
      if (response.has_more !== true) return buckets
      const nextPage = typeof response.next_page === 'string' ? response.next_page : ''
      if (!nextPage || nextPage.length > 4096 || seenPages.has(nextPage)) throw new Error('OpenAI Usage API returned an invalid pagination cursor')
      seenPages.add(nextPage)
      page = nextPage
    }
    throw new Error('OpenAI Usage API exceeded the pagination limit')
  }

  private async fetchResponse(url: URL, apiKey: string): Promise<Record<string, unknown>> {
    const controller = new AbortController()
    const timer = setTimeout(() => controller.abort(), this.requestTimeoutMs)
    try {
      const response = await this.fetcher(url, { headers: { Authorization: `Bearer ${apiKey}`, Accept: 'application/json' }, signal: controller.signal })
      if (!response.ok) throw new Error(`OpenAI Usage API returned HTTP ${response.status}`)
      const source = await response.text()
      if (Buffer.byteLength(source, 'utf8') > 2 * 1024 * 1024) throw new Error('OpenAI Usage API response exceeded 2 MB')
      const parsed = JSON.parse(source) as unknown
      if (!isRecord(parsed)) throw new Error('OpenAI Usage API returned an invalid response')
      return parsed
    } catch (error) {
      if (error instanceof Error && error.name === 'AbortError') throw new Error('OpenAI Usage API request timed out')
      throw error
    } finally {
      clearTimeout(timer)
    }
  }
}

function normalizeUsageBuckets(buckets: ProviderBucket[]): AiUsageEventInput[] {
  return buckets.flatMap((bucket) => normalizeResults(bucket).map((result) => ({
    source: 'providerApi' as const,
    provider: 'openai',
    clientId: 'openaiApi',
    ...(safeString(result.project_id) ? { projectId: safeString(result.project_id) } : {}),
    model: safeString(result.model) || 'openai-unattributed',
    startedAt: bucketTimestamp(bucket),
    inputTokens: safeInteger(result.input_tokens),
    outputTokens: safeInteger(result.output_tokens),
    ...(safeInteger(result.input_cached_tokens) > 0 ? { cachedInputTokens: safeInteger(result.input_cached_tokens) } : {}),
    ...(safeInteger(result.num_model_requests) > 0 ? { requestCount: safeInteger(result.num_model_requests) } : {}),
    sourceFingerprint: fingerprint('usage', bucket, result)
  })))
}

function normalizeCostBuckets(buckets: ProviderBucket[]): AiUsageEventInput[] {
  return buckets.flatMap((bucket) => normalizeResults(bucket).flatMap((result) => {
    const amount = isRecord(result.amount) ? result.amount : {}
    const currency = safeString(amount.currency).toUpperCase()
    const value = typeof amount.value === 'number' && Number.isFinite(amount.value) && amount.value >= 0 ? amount.value : NaN
    if (!/^[A-Z]{3}$/.test(currency) || !Number.isFinite(value)) return []
    return [{
      source: 'providerApi' as const,
      provider: 'openai',
      clientId: 'openaiApi',
      ...(safeString(result.project_id) ? { projectId: safeString(result.project_id) } : {}),
      model: safeString(result.line_item) || 'OpenAI costs',
      startedAt: bucketTimestamp(bucket),
      inputTokens: 0,
      outputTokens: 0,
      requestCount: 0,
      billedCost: { currency, micros: Math.round(value * 1_000_000) },
      sourceFingerprint: fingerprint('cost', bucket, result)
    }]
  }))
}

function normalizeResults(bucket: ProviderBucket): Array<Record<string, unknown>> {
  return Array.isArray(bucket.results) ? bucket.results.filter(isRecord) : []
}

function bucketTimestamp(bucket: ProviderBucket): string {
  const seconds = safeInteger(bucket.start_time)
  if (seconds <= 0) throw new Error('OpenAI Usage API returned an invalid bucket timestamp')
  return new Date(seconds * 1000).toISOString()
}

function fingerprint(kind: string, bucket: ProviderBucket, result: Record<string, unknown>): string {
  const identity = {
    kind, start: safeInteger(bucket.start_time), end: safeInteger(bucket.end_time),
    projectId: safeString(result.project_id), model: safeString(result.model), lineItem: safeString(result.line_item),
    apiKeyId: safeString(result.api_key_id), userId: safeString(result.user_id), serviceTier: safeString(result.service_tier), batch: result.batch === true
  }
  return `openai:${kind}:${createHash('sha256').update(JSON.stringify(identity)).digest('hex')}`
}

function safeInteger(value: unknown): number {
  return typeof value === 'number' && Number.isSafeInteger(value) && value >= 0 ? value : 0
}

function safeString(value: unknown): string {
  return typeof value === 'string' && value.length <= 4096 && !/[\r\n\0]/.test(value) ? value : ''
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
