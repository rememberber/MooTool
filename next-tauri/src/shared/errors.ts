import { diagnosticsApi } from '../platform/api/diagnosticsApi'
import type { FrontendErrorReport } from '../platform/contracts/diagnostics'

export interface ProductError {
  code: string
  message: string
  retryable: boolean
  stack?: string
}

const reportedAt = new Map<string, number>()
const reportDeduplicationWindow = 5_000

export function toProductError(cause: unknown): ProductError {
  const candidate = parseCandidate(cause)
  const message = readString(candidate, 'message')
    ?? (cause instanceof Error ? cause.message : safeString(cause))
  return {
    code: normalizeCode(readString(candidate, 'code')),
    message: message.trim() || 'Unknown error',
    retryable: readBoolean(candidate, 'retryable') ?? false,
    stack: readString(candidate, 'stack') ?? (cause instanceof Error ? cause.stack : undefined)
  }
}

export function errorMessage(cause: unknown, context = 'frontend.operation'): string {
  const error = toProductError(cause)
  void reportProductError(error, context)
  return error.message
}

export async function reportProductError(
  error: ProductError,
  context = 'frontend.operation'
): Promise<void> {
  const report: FrontendErrorReport = {
    code: normalizeCode(error.code),
    message: limit(error.message, 16_384),
    context: limit(context.trim() || 'frontend.operation', 160),
    retryable: error.retryable,
    ...(error.stack ? { stack: limit(error.stack, 32_768) } : {})
  }
  const fingerprint = `${report.code}\u0000${report.context}\u0000${report.message}`
  const now = Date.now()
  if ((reportedAt.get(fingerprint) ?? 0) + reportDeduplicationWindow > now) return
  reportedAt.set(fingerprint, now)
  pruneReports(now)
  try {
    await diagnosticsApi.reportError(report)
  } catch {
    // Error reporting must never mask or recursively report the original failure.
  }
}

function parseCandidate(cause: unknown): object | undefined {
  if (cause && typeof cause === 'object') return cause
  if (typeof cause !== 'string') return undefined
  try {
    const parsed: unknown = JSON.parse(cause)
    return parsed && typeof parsed === 'object' ? parsed : undefined
  } catch {
    return undefined
  }
}

function readString(candidate: object | undefined, key: string): string | undefined {
  if (!candidate || !(key in candidate)) return undefined
  const value = (candidate as Record<string, unknown>)[key]
  return typeof value === 'string' ? value : undefined
}

function readBoolean(candidate: object | undefined, key: string): boolean | undefined {
  if (!candidate || !(key in candidate)) return undefined
  const value = (candidate as Record<string, unknown>)[key]
  return typeof value === 'boolean' ? value : undefined
}

function safeString(value: unknown): string {
  if (typeof value === 'string') return value
  try {
    return String(value)
  } catch {
    return 'Unknown error'
  }
}

function normalizeCode(value: string | undefined): string {
  const normalized = value?.trim().toLowerCase().replace(/[^a-z0-9_]+/g, '_')
  return normalized && normalized.length <= 80 ? normalized : 'frontend_error'
}

function limit(value: string, maximum: number): string {
  return value.length > maximum ? `${value.slice(0, maximum - 1)}…` : value
}

function pruneReports(now: number): void {
  for (const [fingerprint, timestamp] of reportedAt) {
    if (timestamp + reportDeduplicationWindow <= now) reportedAt.delete(fingerprint)
  }
}
