import type { AiUsageEvent, AiUsageExportFormat, AiUsageExportInput } from '../../../src/shared/contracts/aiUsage'
import { AiUsageRepository } from './usageRepository'

export type AiUsageExportDocument = {
  format: AiUsageExportFormat
  extension: AiUsageExportFormat
  content: string
  events: number
}

export class UsageExportService {
  constructor(private readonly repository: AiUsageRepository) {}

  create(input: AiUsageExportInput): AiUsageExportDocument {
    const events = this.repository.events(input)
    return {
      format: input.format,
      extension: input.format,
      content: input.format === 'json' ? JSON.stringify({ schemaVersion: 1, exportedAt: new Date().toISOString(), events: events.map(exportEvent) }, null, 2) : toCsv(events),
      events: events.length
    }
  }
}

function exportEvent(event: AiUsageEvent): Record<string, unknown> {
  return {
    source: event.source,
    provider: event.provider,
    clientId: event.clientId,
    ...(event.projectId ? { projectId: event.projectId } : {}),
    ...(event.agentProfileId ? { agentProfileId: event.agentProfileId } : {}),
    ...(event.modelRuntimeId ? { modelRuntimeId: event.modelRuntimeId } : {}),
    ...(event.localModelDigest ? { localModelDigest: event.localModelDigest } : {}),
    ...(event.sessionId ? { sessionId: event.sessionId } : {}),
    model: event.model,
    startedAt: event.startedAt,
    inputTokens: event.inputTokens,
    outputTokens: event.outputTokens,
    ...(event.cachedInputTokens === undefined ? {} : { cachedInputTokens: event.cachedInputTokens }),
    ...(event.cacheWriteTokens === undefined ? {} : { cacheWriteTokens: event.cacheWriteTokens }),
    ...(event.reasoningTokens === undefined ? {} : { reasoningTokens: event.reasoningTokens }),
    ...(event.requestCount === undefined ? {} : { requestCount: event.requestCount }),
    ...(event.estimatedCost ? { estimatedCost: event.estimatedCost } : {}),
    ...(event.billedCost ? { billedCost: event.billedCost } : {}),
    sourceFingerprint: event.sourceFingerprint,
    importedAt: event.importedAt
  }
}

function toCsv(events: AiUsageEvent[]): string {
  const headers = [
    'source', 'provider', 'clientId', 'projectId', 'agentProfileId', 'modelRuntimeId', 'localModelDigest', 'sessionId', 'model', 'startedAt',
    'inputTokens', 'outputTokens', 'cachedInputTokens', 'cacheWriteTokens', 'reasoningTokens', 'requestCount',
    'estimatedCostCurrency', 'estimatedCostMicros', 'billedCostCurrency', 'billedCostMicros', 'sourceFingerprint', 'importedAt'
  ]
  const rows = events.map((event) => [
    event.source, event.provider, event.clientId, event.projectId, event.agentProfileId, event.modelRuntimeId, event.localModelDigest, event.sessionId,
    event.model, event.startedAt, event.inputTokens, event.outputTokens, event.cachedInputTokens, event.cacheWriteTokens, event.reasoningTokens,
    event.requestCount, event.estimatedCost?.currency, event.estimatedCost?.micros, event.billedCost?.currency, event.billedCost?.micros,
    event.sourceFingerprint, event.importedAt
  ].map(csvCell).join(','))
  return `\uFEFF${headers.join(',')}\r\n${rows.join('\r\n')}${rows.length ? '\r\n' : ''}`
}

function csvCell(value: unknown): string {
  const text = value === undefined || value === null ? '' : String(value)
  return /[",\r\n]/.test(text) ? `"${text.replaceAll('"', '""')}"` : text
}
