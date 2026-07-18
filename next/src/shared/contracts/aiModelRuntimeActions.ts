import { aiModelRuntimeIds, type AiModelRuntimeExposure, type AiModelRuntimeId } from './aiModelRuntime'

export const aiModelRuntimeActions = ['pull', 'load', 'unload', 'delete'] as const
export type AiModelRuntimeAction = (typeof aiModelRuntimeActions)[number]

export type AiModelRuntimeActionPlanInput = {
  runtimeId: AiModelRuntimeId
  action: AiModelRuntimeAction
  modelName: string
}

export type AiModelRuntimeActionPlan = {
  planId: string
  createdAt: string
  expiresAt: string
  runtimeId: AiModelRuntimeId
  runtimeName: string
  action: AiModelRuntimeAction
  modelName: string
  endpoint: string
  exposure: AiModelRuntimeExposure
  destructive: boolean
  modelExists: boolean
  modelRunning: boolean
  modelDigest?: string
  modelSizeBytes?: number
  modelDirectoryAvailableBytes?: number
  runtimeInstanceId?: string
  affectedAgentProfiles: Array<{ id: string; name: string }>
  authenticationConfigured: boolean
  requiresRemoteConfirmation: boolean
  warnings: string[]
}

export type AiModelRuntimeActionExecuteInput = {
  requestId: string
  planId: string
  confirmAction: boolean
  confirmDestructive: boolean
  confirmRemoteEndpoint: boolean
}

export type AiModelRuntimeActionProgressEvent = {
  requestId: string
  planId: string
  runtimeId: AiModelRuntimeId
  action: AiModelRuntimeAction
  status: 'preparing' | 'downloading' | 'loading' | 'unloading' | 'deleting' | 'verifying' | 'completed' | 'failed' | 'cancelled'
  message: string
  completedBytes?: number
  totalBytes?: number
  percent?: number
  timestamp: string
}

export type AiModelRuntimeActionResult = {
  requestId: string
  planId: string
  runtimeId: AiModelRuntimeId
  action: AiModelRuntimeAction
  modelName: string
  status: 'completed' | 'failed' | 'cancelled'
  startedAt: string
  finishedAt: string
  durationMs: number
  message: string
}

export function supportedAiModelRuntimeActions(runtimeId: AiModelRuntimeId): AiModelRuntimeAction[] {
  if (runtimeId === 'ollama') return ['pull', 'load', 'unload', 'delete']
  if (runtimeId === 'lmStudio') return ['pull', 'load', 'unload']
  return []
}

export function isAiModelRuntimeActionPlanInput(value: unknown): value is AiModelRuntimeActionPlanInput {
  return isRecord(value)
    && aiModelRuntimeIds.includes(value.runtimeId as AiModelRuntimeId)
    && aiModelRuntimeActions.includes(value.action as AiModelRuntimeAction)
    && isModelName(value.modelName)
}

export function isAiModelRuntimeActionExecuteInput(value: unknown): value is AiModelRuntimeActionExecuteInput {
  return isRecord(value)
    && isUuid(value.requestId)
    && isUuid(value.planId)
    && typeof value.confirmAction === 'boolean'
    && typeof value.confirmDestructive === 'boolean'
    && typeof value.confirmRemoteEndpoint === 'boolean'
}

export function isAiModelRuntimeActionRequestId(value: unknown): value is string {
  return isUuid(value)
}

function isModelName(value: unknown): value is string {
  return typeof value === 'string'
    && value.trim().length > 0
    && value.length <= 1_000
    && new TextEncoder().encode(value).byteLength <= 4_096
    && !/[\r\n\0]/.test(value)
}

function isUuid(value: unknown): value is string {
  return typeof value === 'string' && /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value)
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
