import { aiPrimaryClientIds, type AiClientId, type AiPrimaryClientId } from './ai'
import { aiModelRuntimeIds, type AiModelRuntimeId } from './aiModelRuntime'

export const aiAgentPermissionModes = ['readOnly', 'default', 'workspaceWrite', 'plan', 'acceptEdits', 'dontAsk'] as const
export type AiAgentPermissionMode = (typeof aiAgentPermissionModes)[number]

export type AiAgentCapabilityId = 'instructions' | 'skills' | 'mcp' | 'subagents' | 'hooks' | 'structuredOutput' | 'usage' | 'permissionModes'
export type AiAgentCapabilitySupport = 'full' | 'partial' | 'none'

export type AiAgentClientCapability = {
  id: AiAgentCapabilityId
  support: AiAgentCapabilitySupport
}

export type AiAgentClient = {
  id: AiClientId
  name: string
  detected: boolean
  health: 'healthy' | 'warning' | 'missing'
  binaryPath?: string
  configRoot: string
  artifactCount: number
  configurationFingerprint: string
  previousConfigurationFingerprint?: string
  configurationChanged: boolean
  capabilities: AiAgentClientCapability[]
  diagnostics: string[]
}

export type AiAgentProfile = {
  id: string
  name: string
  clientId: AiPrimaryClientId
  model?: string
  modelRuntimeId?: AiModelRuntimeId
  localModelDigest?: string
  workingDirectory: string
  configProfile?: string
  permissionMode: AiAgentPermissionMode
  mcpServerNames: string[]
  skillNames: string[]
  environmentVariableRefs: string[]
  optionalFlags: string[]
  createdAt: string
  updatedAt: string
}

export type AiAgentProfileSaveInput = Omit<AiAgentProfile, 'id' | 'createdAt' | 'updatedAt'> & { id?: string }

export type AiAgentManagerInput = {
  projectRoot?: string
}

export type AiAgentManagerSnapshot = {
  scannedAt: string
  projectRoot?: string
  clients: AiAgentClient[]
  profiles: AiAgentProfile[]
}

export type AiAgentLaunchPlan = {
  profileId: string
  clientId: AiClientId
  executable: string
  args: string[]
  workingDirectory: string
  requiredEnvironmentVariables: string[]
  displayCommand: string
  executes: false
  warnings: string[]
}

export function isAiAgentManagerInput(value: unknown): value is AiAgentManagerInput {
  return value == null || (isRecord(value) && (value.projectRoot === undefined || (typeof value.projectRoot === 'string' && value.projectRoot.length <= 4096)))
}

export function isAiAgentProfileSaveInput(value: unknown): value is AiAgentProfileSaveInput {
  if (!isRecord(value)) return false
  return (value.id === undefined || isUuid(value.id))
    && typeof value.name === 'string' && value.name.trim().length > 0 && value.name.length <= 200
    && aiPrimaryClientIds.includes(value.clientId as AiPrimaryClientId)
    && optionalSafeValue(value.model, 500)
    && (value.modelRuntimeId === undefined || aiModelRuntimeIds.includes(value.modelRuntimeId as AiModelRuntimeId))
    && optionalSafeValue(value.localModelDigest, 500)
    && (value.localModelDigest === undefined || value.modelRuntimeId !== undefined)
    && (value.modelRuntimeId === undefined || (typeof value.model === 'string' && value.model.trim().length > 0 && typeof value.localModelDigest === 'string' && value.localModelDigest.trim().length > 0))
    && typeof value.workingDirectory === 'string' && value.workingDirectory.trim().length > 0 && value.workingDirectory.length <= 4096
    && optionalSafeValue(value.configProfile, 200)
    && aiAgentPermissionModes.includes(value.permissionMode as AiAgentPermissionMode)
    && isStringArray(value.mcpServerNames, 200, 200)
    && isStringArray(value.skillNames, 200, 200)
    && Array.isArray(value.environmentVariableRefs) && value.environmentVariableRefs.length <= 100 && value.environmentVariableRefs.every((item) => typeof item === 'string' && /^[A-Za-z_][A-Za-z0-9_]{0,127}$/.test(item))
    && isStringArray(value.optionalFlags, 20, 100)
}

export function isAiAgentProfileId(value: unknown): value is string {
  return isUuid(value)
}

function optionalSafeValue(value: unknown, maximum: number): boolean {
  return value === undefined || (typeof value === 'string' && value.length <= maximum && !/[\r\n\0]/.test(value))
}

function isStringArray(value: unknown, maximumItems: number, maximumLength: number): boolean {
  return Array.isArray(value) && value.length <= maximumItems && value.every((item) => typeof item === 'string' && item.length <= maximumLength && !/[\r\n\0]/.test(item))
}

function isUuid(value: unknown): value is string {
  return typeof value === 'string' && /^[0-9a-f-]{36}$/i.test(value)
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
