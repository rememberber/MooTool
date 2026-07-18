import { createHash, randomUUID } from 'node:crypto'
import { lstat, readFile, realpath, stat } from 'node:fs/promises'
import { extname, isAbsolute, resolve } from 'node:path'
import type {
  AiMoney,
  AiUsageEventInput,
  AiUsageImportFilePreview,
  AiUsageImportPreview,
  AiUsageImportPreviewInput,
  AiUsageImportResult,
  AiUsageSource
} from '../../../src/shared/contracts/aiUsage'
import type { AiUsageRepository } from './usageRepository'

type UsageImportPlan = {
  id: string
  expiresAt: string
  files: Array<{ path: string; size: number; modifiedMs: number; hash: string }>
  events: AiUsageEventInput[]
  preview: AiUsageImportPreview
}

type ParsedUsageFile = {
  clientId: string
  events: AiUsageEventInput[]
  fields: string[]
  warnings: string[]
}

type UsageImportServiceOptions = {
  repository: AiUsageRepository
  clock?: () => Date
  planTtlMs?: number
  maximumFileBytes?: number
  maximumTotalBytes?: number
}

const allowedExtensions = new Set(['.json', '.jsonl', '.log'])
const maximumLines = 200_000

export class UsageImportService {
  private readonly repository: AiUsageRepository
  private readonly clock: () => Date
  private readonly planTtlMs: number
  private readonly maximumFileBytes: number
  private readonly maximumTotalBytes: number
  private readonly plans = new Map<string, UsageImportPlan>()

  constructor(options: UsageImportServiceOptions) {
    this.repository = options.repository
    this.clock = options.clock ?? (() => new Date())
    this.planTtlMs = options.planTtlMs ?? 10 * 60 * 1_000
    this.maximumFileBytes = options.maximumFileBytes ?? 50 * 1024 * 1024
    this.maximumTotalBytes = options.maximumTotalBytes ?? 100 * 1024 * 1024
  }

  async preview(input: AiUsageImportPreviewInput): Promise<AiUsageImportPreview> {
    this.purgeExpiredPlans()
    const files: UsageImportPlan['files'] = []
    const previews: AiUsageImportFilePreview[] = []
    const events: AiUsageEventInput[] = []
    let totalBytes = 0
    for (const requestedPath of [...new Set(input.paths)]) {
      if (!isAbsolute(requestedPath)) throw new Error('Usage import paths must be absolute')
      const requested = resolve(requestedPath)
      const linkInfo = await lstat(requested)
      if (linkInfo.isSymbolicLink()) throw new Error('Usage import does not follow symbolic links')
      if (!linkInfo.isFile()) throw new Error('Usage import source must be a file')
      if (!allowedExtensions.has(extname(requested).toLowerCase())) throw new Error('Usage import accepts JSON, JSONL, and log files only')
      if (linkInfo.size > this.maximumFileBytes) throw new Error('A Usage import file exceeds the 50 MB safety limit')
      totalBytes += linkInfo.size
      if (totalBytes > this.maximumTotalBytes) throw new Error('Usage import exceeds the 100 MB total safety limit')
      const path = await realpath(requested)
      const source = await readFile(path, 'utf8')
      const hash = sha256(source)
      const parsed = parseUsageFile(path, source)
      events.push(...parsed.events)
      const times = parsed.events.map((event) => event.startedAt).sort()
      previews.push({
        path,
        sizeBytes: linkInfo.size,
        clientId: parsed.clientId,
        events: parsed.events.length,
        models: [...new Set(parsed.events.map((event) => event.model))].sort(),
        ...(times[0] ? { from: times[0], to: times.at(-1) } : {}),
        fields: parsed.fields,
        warnings: parsed.warnings
      })
      files.push({ path, size: linkInfo.size, modifiedMs: linkInfo.mtimeMs, hash })
    }
    const unique = deduplicateEvents(events)
    const existing = this.repository.countExistingFingerprints(unique.map((event) => event.sourceFingerprint))
    const allTimes = unique.map((event) => event.startedAt).sort()
    const id = randomUUID()
    const expiresAt = new Date(this.clock().getTime() + this.planTtlMs).toISOString()
    const preview: AiUsageImportPreview = {
      planId: id,
      expiresAt,
      files: previews,
      events: events.length,
      uniqueEvents: unique.length,
      duplicates: events.length - unique.length + existing,
      ...(allTimes[0] ? { from: allTimes[0], to: allTimes.at(-1) } : {}),
      fields: [...new Set(previews.flatMap((file) => file.fields))].sort(),
      warnings: [...new Set(previews.flatMap((file) => file.warnings))]
    }
    this.plans.set(id, { id, expiresAt, files, events: unique, preview })
    return preview
  }

  async apply(planId: string, timezoneOffsetMinutes: number): Promise<AiUsageImportResult> {
    const plan = this.plans.get(planId)
    if (!plan) throw new Error('Usage import plan no longer exists')
    if (Date.parse(plan.expiresAt) <= this.clock().getTime()) {
      this.plans.delete(planId)
      throw new Error('Usage import plan has expired')
    }
    for (const expected of plan.files) {
      const current = await stat(expected.path)
      if (!current.isFile() || current.size !== expected.size || current.mtimeMs !== expected.modifiedMs) throw new Error('Usage import source changed after preview')
      if (sha256(await readFile(expected.path, 'utf8')) !== expected.hash) throw new Error('Usage import source changed after preview')
    }
    const counts = this.repository.import(plan.events)
    this.plans.delete(planId)
    return { ...counts, dashboard: this.repository.dashboard({ rangeDays: 30, timezoneOffsetMinutes }) }
  }

  private purgeExpiredPlans(): void {
    const now = this.clock().getTime()
    for (const [id, plan] of this.plans) if (Date.parse(plan.expiresAt) <= now) this.plans.delete(id)
  }
}

function parseUsageFile(path: string, source: string): ParsedUsageFile {
  const json = tryParseJson(source)
  if (json !== undefined) {
    const normalized = parseNormalizedJson(path, json)
    if (normalized.events.length > 0) return normalized
  }
  const records: Record<string, unknown>[] = []
  let malformed = 0
  const lines = source.split(/\r?\n/)
  if (lines.length > maximumLines) throw new Error('Usage import file exceeds the 200,000 line safety limit')
  for (const line of lines) {
    if (!line.trim()) continue
    const value = tryParseJson(line)
    if (isRecord(value)) records.push(value)
    else malformed += 1
  }
  const codex = parseCodexRecords(path, records)
  if (codex.events.length > 0) return { ...codex, warnings: [...codex.warnings, ...(malformed ? [`${malformed} malformed line(s) skipped`] : [])] }
  const claude = parseClaudeRecords(path, records)
  if (claude.events.length > 0) return { ...claude, warnings: [...claude.warnings, ...(malformed ? [`${malformed} malformed line(s) skipped`] : [])] }
  const ollama = parseOllamaRecords(path, records)
  if (ollama.events.length > 0) return { ...ollama, warnings: [...ollama.warnings, ...(malformed ? [`${malformed} malformed line(s) skipped`] : [])] }
  return { clientId: 'unknown', events: [], fields: [], warnings: ['No supported Usage metadata was found', ...(malformed ? [`${malformed} malformed line(s) skipped`] : [])] }
}

function parseCodexRecords(path: string, records: Record<string, unknown>[]): ParsedUsageFile {
  let sessionId = ''
  let projectId: string | undefined
  let model = 'unknown'
  let startedAt = ''
  let usage: TokenCounts | undefined
  for (const record of records) {
    const payload = isRecord(record.payload) ? record.payload : {}
    const timestamp = isoString(record.timestamp) || isoString(payload.timestamp)
    if (!startedAt && timestamp) startedAt = timestamp
    if (record.type === 'session_meta') {
      sessionId = stringValue(payload.id) || sessionId
      projectId = stringValue(payload.cwd) || projectId
    } else if (record.type === 'turn_context') {
      model = stringValue(payload.model) || model
      projectId = stringValue(payload.cwd) || projectId
    } else if (record.type === 'event_msg' && payload.type === 'token_count') {
      const info = isRecord(payload.info) ? payload.info : {}
      const totals = isRecord(info.total_token_usage) ? info.total_token_usage : isRecord(info.last_token_usage) ? info.last_token_usage : undefined
      if (totals) usage = tokenCounts(totals, { reasoningKey: 'reasoning_output_tokens' })
    }
  }
  if (!usage) return emptyParsed('codex')
  const fields = usageFields(usage)
  return {
    clientId: 'codex',
    events: [{
      source: 'localLog', provider: 'openai', clientId: 'codex', projectId, sessionId: sessionId || undefined,
      model, startedAt: startedAt || new Date(0).toISOString(), ...usage,
      sourceFingerprint: sha256(`codex\0${path}\0${sessionId || 'single-session'}`)
    }],
    fields,
    warnings: model === 'unknown' ? ['Model name was not present in the Codex log'] : []
  }
}

function parseClaudeRecords(path: string, records: Record<string, unknown>[]): ParsedUsageFile {
  const sessions = new Map<string, { projectId?: string; model: string; startedAt: string; usage: TokenCounts; requests: number }>()
  for (const record of records) {
    const message = isRecord(record.message) ? record.message : {}
    const usage = isRecord(message.usage) ? message.usage : undefined
    if (!usage) continue
    const sessionId = stringValue(record.sessionId) || stringValue(record.session_id) || 'single-session'
    const current = sessions.get(sessionId) ?? { model: 'unknown', startedAt: '', usage: emptyTokenCounts(), requests: 0 }
    current.projectId = stringValue(record.cwd) || current.projectId
    current.model = stringValue(message.model) || current.model
    current.startedAt ||= isoString(record.timestamp)
    addCounts(current.usage, tokenCounts(usage, { cachedKey: 'cache_read_input_tokens', cacheWriteKey: 'cache_creation_input_tokens' }))
    current.requests += 1
    sessions.set(sessionId, current)
  }
  const events = [...sessions.entries()].map(([sessionId, session]) => ({
    source: 'localLog' as const,
    provider: 'anthropic',
    clientId: 'claudeCode',
    projectId: session.projectId,
    sessionId: sessionId === 'single-session' ? undefined : sessionId,
    model: session.model,
    startedAt: session.startedAt || new Date(0).toISOString(),
    ...session.usage,
    requestCount: session.requests,
    sourceFingerprint: sha256(`claudeCode\0${path}\0${sessionId}`)
  }))
  return { clientId: 'claudeCode', events, fields: [...new Set(events.flatMap(usageFields))], warnings: [] }
}

function parseOllamaRecords(path: string, records: Record<string, unknown>[]): ParsedUsageFile {
  const events = records.flatMap((record, index): AiUsageEventInput[] => {
    const inputTokens = nonNegativeInteger(record.prompt_eval_count)
    const outputTokens = nonNegativeInteger(record.eval_count)
    const model = stringValue(record.model)
    if (inputTokens === undefined || outputTokens === undefined || !model) return []
    return [{
      source: 'localRuntime', provider: 'ollama', clientId: 'ollama', model,
      startedAt: isoString(record.created_at) || new Date(0).toISOString(), inputTokens, outputTokens, requestCount: 1,
      sourceFingerprint: sha256(`ollama\0${path}\0${stringValue(record.id) || index}`)
    }]
  })
  return { clientId: 'ollama', events, fields: ['inputTokens', 'outputTokens', 'requestCount'], warnings: [] }
}

function parseNormalizedJson(path: string, value: unknown): ParsedUsageFile {
  const candidates = Array.isArray(value) ? value : isRecord(value) && Array.isArray(value.events) ? value.events : [value]
  const events = candidates.flatMap((candidate, index): AiUsageEventInput[] => {
    if (!isRecord(candidate)) return []
    const inputTokens = nonNegativeInteger(candidate.inputTokens)
    const outputTokens = nonNegativeInteger(candidate.outputTokens)
    const model = stringValue(candidate.model)
    const startedAt = isoString(candidate.startedAt)
    if (inputTokens === undefined || outputTokens === undefined || !model || !startedAt) return []
    const source = validSource(candidate.source)
    return [{
      source,
      provider: stringValue(candidate.provider) || 'unknown',
      clientId: stringValue(candidate.clientId) || 'import',
      projectId: stringValue(candidate.projectId) || undefined,
      agentProfileId: stringValue(candidate.agentProfileId) || undefined,
      modelRuntimeId: stringValue(candidate.modelRuntimeId) || undefined,
      localModelDigest: stringValue(candidate.localModelDigest) || undefined,
      sessionId: stringValue(candidate.sessionId) || undefined,
      model,
      startedAt,
      inputTokens,
      outputTokens,
      cachedInputTokens: nonNegativeInteger(candidate.cachedInputTokens),
      cacheWriteTokens: nonNegativeInteger(candidate.cacheWriteTokens),
      reasoningTokens: nonNegativeInteger(candidate.reasoningTokens),
      requestCount: nonNegativeInteger(candidate.requestCount),
      estimatedCost: parseMoney(candidate.estimatedCost),
      billedCost: parseMoney(candidate.billedCost),
      sourceFingerprint: stringValue(candidate.sourceFingerprint) || sha256(`import\0${path}\0${index}`)
    }]
  })
  return { clientId: events[0]?.clientId ?? 'import', events, fields: [...new Set(events.flatMap(usageFields))], warnings: [] }
}

type TokenCounts = {
  inputTokens: number
  outputTokens: number
  cachedInputTokens?: number
  cacheWriteTokens?: number
  reasoningTokens?: number
}

function tokenCounts(value: Record<string, unknown>, keys: { cachedKey?: string; cacheWriteKey?: string; reasoningKey?: string } = {}): TokenCounts {
  return {
    inputTokens: nonNegativeInteger(value.input_tokens) ?? 0,
    outputTokens: nonNegativeInteger(value.output_tokens) ?? 0,
    cachedInputTokens: nonNegativeInteger(value[keys.cachedKey ?? 'cached_input_tokens']),
    cacheWriteTokens: nonNegativeInteger(value[keys.cacheWriteKey ?? 'cache_write_tokens']),
    reasoningTokens: nonNegativeInteger(value[keys.reasoningKey ?? 'reasoning_tokens'])
  }
}

function emptyTokenCounts(): TokenCounts {
  return { inputTokens: 0, outputTokens: 0 }
}

function addCounts(target: TokenCounts, value: TokenCounts): void {
  target.inputTokens += value.inputTokens
  target.outputTokens += value.outputTokens
  target.cachedInputTokens = (target.cachedInputTokens ?? 0) + (value.cachedInputTokens ?? 0)
  target.cacheWriteTokens = (target.cacheWriteTokens ?? 0) + (value.cacheWriteTokens ?? 0)
  target.reasoningTokens = (target.reasoningTokens ?? 0) + (value.reasoningTokens ?? 0)
}

function usageFields(value: Partial<AiUsageEventInput>): string[] {
  return ['inputTokens', 'outputTokens', 'cachedInputTokens', 'cacheWriteTokens', 'reasoningTokens', 'requestCount', 'estimatedCost', 'billedCost']
    .filter((key) => value[key as keyof AiUsageEventInput] !== undefined)
}

function deduplicateEvents(events: AiUsageEventInput[]): AiUsageEventInput[] {
  const unique = new Map<string, AiUsageEventInput>()
  for (const event of events) unique.set(event.sourceFingerprint, event)
  return [...unique.values()]
}

function parseMoney(value: unknown): AiMoney | undefined {
  if (!isRecord(value)) return undefined
  const currency = stringValue(value.currency).toUpperCase()
  const micros = nonNegativeInteger(value.micros)
  return /^[A-Z]{3}$/.test(currency) && micros !== undefined ? { currency, micros } : undefined
}

function validSource(value: unknown): AiUsageSource {
  return value === 'localLog' || value === 'cli' || value === 'providerApi' || value === 'localRuntime' ? value : 'import'
}

function tryParseJson(value: string): unknown {
  try { return JSON.parse(value) as unknown } catch { return undefined }
}

function isoString(value: unknown): string {
  return typeof value === 'string' && Number.isFinite(Date.parse(value)) ? value : ''
}

function stringValue(value: unknown): string {
  return typeof value === 'string' ? value.slice(0, 4096) : ''
}

function nonNegativeInteger(value: unknown): number | undefined {
  return typeof value === 'number' && Number.isSafeInteger(value) && value >= 0 ? value : undefined
}

function emptyParsed(clientId: string): ParsedUsageFile {
  return { clientId, events: [], fields: [], warnings: [] }
}

function sha256(value: string): string {
  return createHash('sha256').update(value).digest('hex')
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
