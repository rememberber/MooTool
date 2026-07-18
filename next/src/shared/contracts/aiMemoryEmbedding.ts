import { aiModelRuntimeIds, type AiModelRuntimeId } from './aiModelRuntime'
import type { AiMemoryPreview, AiMemoryPreviewInput } from './aiMemory'

export type AiMemoryEmbeddingStatus = {
  available: boolean
  eligible: number
  indexed: number
  stale: number
  skippedSensitive: number
  coverage: number
  runtimeId?: AiModelRuntimeId
  model?: string
  modelVersion?: string
  dimensions?: number
  generatedAt?: string
}

export type AiMemoryEmbeddingRebuildInput = {
  requestId: string
  runtimeId: AiModelRuntimeId
  model: string
  confirmLocalProcessing: boolean
}

export type AiMemoryEmbeddingProgressEvent = {
  requestId: string
  status: 'preparing' | 'embedding' | 'completed' | 'cancelled' | 'failed'
  completed: number
  total: number
  skippedSensitive: number
  message: string
  timestamp: string
}

export type AiMemoryEmbeddingRebuildResult = {
  requestId: string
  status: 'completed' | 'cancelled' | 'failed'
  indexed: number
  eligible: number
  skippedSensitive: number
  dimensions?: number
  startedAt: string
  finishedAt: string
  message: string
}

export type AiMemorySemanticPreviewInput = AiMemoryPreviewInput & {
  requestId: string
  runtimeId: AiModelRuntimeId
  model: string
  query: string
  confirmLocalProcessing: boolean
}

export type AiMemorySemanticPreview = AiMemoryPreview & {
  mode: 'semantic'
  runtimeId: AiModelRuntimeId
  model: string
  indexedCandidates: number
}

export function isAiMemoryEmbeddingRebuildInput(value: unknown): value is AiMemoryEmbeddingRebuildInput {
  return isRecord(value)
    && isUuid(value.requestId)
    && aiModelRuntimeIds.includes(value.runtimeId as AiModelRuntimeId)
    && isModel(value.model)
    && typeof value.confirmLocalProcessing === 'boolean'
}

export function isAiMemorySemanticPreviewInput(value: unknown): value is AiMemorySemanticPreviewInput {
  return isRecord(value)
    && isUuid(value.requestId)
    && aiModelRuntimeIds.includes(value.runtimeId as AiModelRuntimeId)
    && isModel(value.model)
    && typeof value.query === 'string'
    && value.query.trim().length > 0
    && value.query.length <= 2_000
    && typeof value.confirmLocalProcessing === 'boolean'
    && optionalString(value.projectRoot, 4_096)
    && optionalString(value.targetPath, 4_096)
    && optionalString(value.branch, 512)
    && optionalString(value.agentProfileId, 512)
    && optionalString(value.taskRef, 512)
    && typeof value.tokenBudget === 'number'
    && Number.isInteger(value.tokenBudget)
    && value.tokenBudget >= 1
    && value.tokenBudget <= 100_000
    && typeof value.maxItems === 'number'
    && Number.isInteger(value.maxItems)
    && value.maxItems >= 1
    && value.maxItems <= 200
}

export function isAiMemoryEmbeddingRequestId(value: unknown): value is string {
  return isUuid(value)
}

function isModel(value: unknown): value is string {
  return typeof value === 'string' && value.trim().length > 0 && value.length <= 500 && !/[\r\n\0]/.test(value)
}

function optionalString(value: unknown, maximum: number): boolean {
  return value === undefined || (typeof value === 'string' && value.length <= maximum && !value.includes('\0'))
}

function isUuid(value: unknown): value is string {
  return typeof value === 'string' && /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value)
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
