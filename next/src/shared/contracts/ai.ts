export const aiClientIds = ['codex', 'claudeCode', 'cursor', 'geminiCli', 'githubCopilot'] as const
export type AiClientId = (typeof aiClientIds)[number]

export const aiPrimaryClientIds = ['codex', 'claudeCode'] as const
export type AiPrimaryClientId = (typeof aiPrimaryClientIds)[number]

export const aiClientNames: Record<AiClientId, string> = {
  codex: 'Codex',
  claudeCode: 'Claude Code',
  cursor: 'Cursor',
  geminiCli: 'Gemini CLI',
  githubCopilot: 'GitHub Copilot'
}

export const aiRuntimeIds = ['ollama'] as const
export type AiRuntimeId = (typeof aiRuntimeIds)[number]

export const aiArtifactKinds = ['clientConfig', 'instruction', 'skill', 'mcpServer'] as const
export type AiArtifactKind = (typeof aiArtifactKinds)[number]

export type AiScope = 'user' | 'project'
export type AiHealthStatus = 'healthy' | 'warning' | 'missing' | 'error'
export type AiDiagnosticSeverity = 'info' | 'warning' | 'error'

export type AiDiagnosticCode =
  | 'PROJECT_NOT_SELECTED'
  | 'CLIENT_CONFIG_WITHOUT_BINARY'
  | 'OLLAMA_NOT_RUNNING'
  | 'SKILL_MISSING_ENTRY'
  | 'SYMLINK_SKIPPED'
  | 'UNREADABLE_PATH'
  | 'SCAN_LIMIT_REACHED'
  | 'MCP_CONFIG_INVALID'
  | 'PLAINTEXT_SECRET_RISK'
  | 'SKILL_ENTRY_INVALID'
  | 'SKILL_REFERENCE_MISSING'
  | 'SKILL_DANGEROUS_PATTERN'
  | 'SKILL_ENTRY_TOO_LARGE'
  | 'INSTRUCTION_DUPLICATE'
  | 'INSTRUCTION_CONFLICT'
  | 'INSTRUCTION_TOO_LARGE'

export type AiDiscoveryInput = {
  projectRoot?: string
}

export type AiArtifact = {
  id: string
  clientId: AiClientId
  kind: AiArtifactKind
  scope: AiScope
  name: string
  path: string
  source: 'standard' | 'legacy'
  sizeBytes?: number
  modifiedAt?: string
  metadata?: Record<string, string | number | boolean>
}

export type AiClientInstallation = {
  id: AiClientId
  name: string
  detected: boolean
  status: AiHealthStatus
  binaryPath?: string
  configRoot: string
  artifactCount: number
}

export type AiLocalModel = {
  name: string
  digest: string
  sizeBytes: number
  parameterSize?: string
  quantization?: string
  family?: string
  running: boolean
  contextLength?: number
}

export type AiRuntimeInstallation = {
  id: AiRuntimeId
  name: string
  detected: boolean
  status: AiHealthStatus
  binaryPath?: string
  endpoint: string
  protocols: Array<'ollama' | 'openaiCompatible'>
  version?: string
  models: AiLocalModel[]
}

export type AiDiagnostic = {
  id: string
  code: AiDiagnosticCode
  severity: AiDiagnosticSeverity
  message: string
  path?: string
  clientId?: AiClientId
  runtimeId?: AiRuntimeId
}

export type AiInventorySummary = {
  detectedClients: number
  detectedRuntimes: number
  artifacts: number
  instructions: number
  skills: number
  mcpServers: number
  diagnostics: number
}

export type AiDoctorSnapshot = {
  scannedAt: string
  projectRoot?: string
  readOnly: true
  clients: AiClientInstallation[]
  runtimes: AiRuntimeInstallation[]
  artifacts: AiArtifact[]
  diagnostics: AiDiagnostic[]
  summary: AiInventorySummary
}

export function isAiDiscoveryInput(value: unknown): value is AiDiscoveryInput {
  if (value == null) return true
  if (typeof value !== 'object' || Array.isArray(value)) return false
  const projectRoot = (value as { projectRoot?: unknown }).projectRoot
  return projectRoot == null || (typeof projectRoot === 'string' && projectRoot.length <= 4096)
}
