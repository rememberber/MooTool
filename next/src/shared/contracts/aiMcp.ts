import type { AiClientId, AiPrimaryClientId, AiScope } from './ai'
import type { AiChangeApplyResult, AiChangePlan, AiChangeRollbackResult } from './aiChanges'

export type AiMcpTransport = 'stdio' | 'streamableHttp' | 'legacySse' | 'unknown'
export type AiMcpReferenceSource = 'literal' | 'environment' | 'oauth'
export type AiMcpRisk = 'plaintextSecret' | 'sensitiveArgument' | 'shellLauncher' | 'insecureRemoteHttp' | 'legacyTransport' | 'unknownTransport'
export type AiMcpCopyWarning = 'environmentVariablesRequired' | 'timeoutNotPortable' | 'disabledNotPortable' | 'oauthReauthorizationRequired'

export type AiMcpNamedReference = {
  name: string
  source: AiMcpReferenceSource
  reference?: string
  sensitive: boolean
}

export type AiMcpServer = {
  id: string
  clientId: AiClientId
  scope: AiScope
  name: string
  configPath: string
  transport: AiMcpTransport
  enabled: boolean
  command?: string
  args: string[]
  url?: string
  environment: AiMcpNamedReference[]
  headers: AiMcpNamedReference[]
  startupTimeoutMs?: number
  toolTimeoutMs?: number
  risks: AiMcpRisk[]
}

export type AiMcpInventoryInput = {
  projectRoot?: string
}

export type AiMcpInventory = {
  scannedAt: string
  projectRoot?: string
  servers: AiMcpServer[]
  invalidConfigPaths: string[]
}

export type AiMcpCopyInput = {
  projectRoot?: string
  sourceServerId: string
  targetClientId: AiPrimaryClientId
  targetScope: AiScope
}

export type AiMcpSecretMapping = {
  field: string
  environmentVariable: string
}

export type AiMcpCopyPreview = {
  plan: AiChangePlan
  source: AiMcpServer
  targetClientId: AiPrimaryClientId
  targetScope: AiScope
  targetPath: string
  secretMappings: AiMcpSecretMapping[]
  warnings: AiMcpCopyWarning[]
}

export type AiMcpCopyApplyResult = AiChangeApplyResult
export type AiMcpCopyRollbackResult = AiChangeRollbackResult

export type AiMcpProbeInput = {
  requestId: string
  sourceServerId: string
  projectRoot?: string
  confirmCommand: boolean
}

export type AiMcpProbeResult = {
  requestId: string
  serverId: string
  status: 'healthy' | 'error' | 'cancelled'
  latencyMs: number
  protocolVersion?: string
  tools: number
  resources: number
  prompts: number
  toolSchemas: AiMcpToolSchemaSummary[]
  schemaEstimatedTokens: number
  executablePath?: string
  executableSha256?: string
  logs: string[]
  errorCode?: 'COMMAND_NOT_FOUND' | 'TIMEOUT' | 'PROTOCOL_ERROR' | 'CONNECTION_ERROR' | 'CANCELLED' | 'INSECURE_ENDPOINT' | 'CONFIRMATION_REQUIRED'
  message?: string
}

export type AiMcpToolSchemaSummary = {
  name: string
  estimatedTokens: number
}

export type AiMcpSchemaSnapshot = {
  serverId: string
  observedAt: string
  toolSchemas: AiMcpToolSchemaSummary[]
  schemaEstimatedTokens: number
}

export function isAiMcpInventoryInput(value: unknown): value is AiMcpInventoryInput {
  if (value === undefined || value === null) return true
  if (typeof value !== 'object' || Array.isArray(value)) return false
  const input = value as Record<string, unknown>
  return input.projectRoot === undefined || (typeof input.projectRoot === 'string' && input.projectRoot.length <= 4096)
}

export function isAiMcpCopyInput(value: unknown): value is AiMcpCopyInput {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) return false
  const input = value as Record<string, unknown>
  return typeof input.sourceServerId === 'string'
    && input.sourceServerId.length > 0
    && input.sourceServerId.length <= 512
    && (input.targetClientId === 'codex' || input.targetClientId === 'claudeCode')
    && (input.targetScope === 'user' || input.targetScope === 'project')
    && (input.projectRoot === undefined || (typeof input.projectRoot === 'string' && input.projectRoot.length <= 4096))
}

export function isAiMcpProbeInput(value: unknown): value is AiMcpProbeInput {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) return false
  const input = value as Record<string, unknown>
  return typeof input.requestId === 'string'
    && /^[0-9a-f-]{36}$/i.test(input.requestId)
    && typeof input.sourceServerId === 'string'
    && input.sourceServerId.length > 0
    && input.sourceServerId.length <= 512
    && typeof input.confirmCommand === 'boolean'
    && (input.projectRoot === undefined || (typeof input.projectRoot === 'string' && input.projectRoot.length <= 4096))
}
