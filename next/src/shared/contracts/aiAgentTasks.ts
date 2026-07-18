import type { AiPrimaryClientId } from './ai'
import type { AiAgentPermissionMode } from './aiAgents'

export const aiAgentTaskStatuses = ['completed', 'failed', 'cancelled', 'timedOut', 'outputLimit'] as const
export type AiAgentTaskStatus = (typeof aiAgentTaskStatuses)[number]

export type AiAgentTaskStartInput = {
  requestId: string
  profileId: string
  prompt: string
  maxDurationSeconds: number
  maxTurns: number
  confirmExecution: boolean
  confirmWrite: boolean
}

export type AiAgentTaskOutputEvent = {
  requestId: string
  sequence: number
  stream: 'stdout' | 'stderr' | 'system'
  text: string
  timestamp: string
}

export type AiAgentTaskResult = {
  requestId: string
  profileId: string
  clientId: AiPrimaryClientId
  status: AiAgentTaskStatus
  executable: string
  args: string[]
  workingDirectory: string
  stdout: string
  stderr: string
  exitCode: number | null
  signal: string | null
  durationMs: number
  startedAt: string
  finishedAt: string
  truncated: boolean
  promptDeliveredVia: 'stdin'
}

export function isAiAgentTaskStartInput(value: unknown): value is AiAgentTaskStartInput {
  if (!isRecord(value)) return false
  return isAiAgentTaskRequestId(value.requestId)
    && isUuid(value.profileId)
    && typeof value.prompt === 'string'
    && value.prompt.trim().length > 0
    && utf8ByteLength(value.prompt) <= 64 * 1024
    && !value.prompt.includes('\0')
    && Number.isInteger(value.maxDurationSeconds)
    && Number(value.maxDurationSeconds) >= 1
    && Number(value.maxDurationSeconds) <= 3_600
    && Number.isInteger(value.maxTurns)
    && Number(value.maxTurns) >= 1
    && Number(value.maxTurns) <= 100
    && typeof value.confirmExecution === 'boolean'
    && typeof value.confirmWrite === 'boolean'
}

export function isAiAgentTaskRequestId(value: unknown): value is string {
  return isUuid(value)
}

export function requiresAiAgentTaskWriteConfirmation(profile: { clientId: AiPrimaryClientId; permissionMode: AiAgentPermissionMode }): boolean {
  return !(profile.clientId === 'codex' && profile.permissionMode === 'readOnly')
    && !(profile.clientId === 'claudeCode' && profile.permissionMode === 'plan')
}

function isUuid(value: unknown): value is string {
  return typeof value === 'string' && /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value)
}

function utf8ByteLength(value: string): number {
  return new TextEncoder().encode(value).byteLength
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
