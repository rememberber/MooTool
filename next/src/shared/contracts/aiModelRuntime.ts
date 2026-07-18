export const aiModelRuntimeIds = ['ollama', 'lmStudio', 'llamaCpp', 'vllm', 'localAi'] as const
export type AiModelRuntimeId = (typeof aiModelRuntimeIds)[number]

export const aiModelRuntimeNames: Record<AiModelRuntimeId, string> = {
  ollama: 'Ollama',
  lmStudio: 'LM Studio',
  llamaCpp: 'llama.cpp',
  vllm: 'vLLM',
  localAi: 'LocalAI'
}

export type AiModelRuntimeHealth = 'healthy' | 'degraded' | 'stopped' | 'notInstalled'
export type AiModelRuntimeExposure = 'loopback' | 'allInterfaces' | 'localNetwork' | 'remote'

export type AiModelRuntimeDiagnosticCode =
  | 'RUNTIME_NOT_INSTALLED'
  | 'RUNTIME_SERVICE_STOPPED'
  | 'RUNTIME_ENDPOINT_EXPOSED'
  | 'RUNTIME_REMOTE_HTTP'
  | 'RUNTIME_API_INVALID'
  | 'RUNTIME_MODEL_DIRECTORY_UNREADABLE'

export type AiModelRuntimeDiagnostic = {
  code: AiModelRuntimeDiagnosticCode
  severity: 'info' | 'warning' | 'error'
  message: string
}

export type AiLocalModelRuntimeModel = {
  name: string
  digest: string
  sizeBytes: number
  modifiedAt?: string
  format?: string
  family?: string
  parameterSize?: string
  quantization?: string
  running: boolean
  loadedSizeBytes?: number
  vramSizeBytes?: number
  contextLength?: number
  expiresAt?: string
  capabilities?: string[]
  runtimeInstanceIds?: string[]
}

export type AiModelRuntimeResources = {
  platform: NodeJS.Platform
  architecture: string
  cpuModel: string
  totalMemoryBytes: number
  freeMemoryBytes: number
  cpuOnly: boolean
  modelDirectory: string
  modelDirectoryExists: boolean
  modelDirectoryAvailableBytes?: number
}

export type AiModelRuntimeInstallation = {
  id: AiModelRuntimeId
  name: string
  detected: boolean
  health: AiModelRuntimeHealth
  binaryPath?: string
  cliVersion?: string
  apiVersion?: string
  endpoint: string
  exposure: AiModelRuntimeExposure
  protocols: Array<'ollamaNative' | 'lmStudioNative' | 'openAICompatible' | 'anthropicCompatible'>
  responseTimeMs?: number
  modelDirectory?: string
  modelDirectoryExists?: boolean
  modelDirectoryAvailableBytes?: number
  models: AiLocalModelRuntimeModel[]
  diagnostics: AiModelRuntimeDiagnostic[]
}

export type AiModelRuntimeSnapshot = {
  scannedAt: string
  readOnly: true
  runtime: AiModelRuntimeInstallation
  runtimes: AiModelRuntimeInstallation[]
  resources: AiModelRuntimeResources
  stats: {
    models: number
    runningModels: number
    totalModelBytes: number
    loadedBytes: number
    vramBytes: number
  }
}

export type AiModelRuntimeDetailInput = {
  runtimeId: AiModelRuntimeId
  modelName: string
}

export type AiModelRuntimeModelDetail = {
  runtimeId: AiModelRuntimeId
  modelName: string
  modifiedAt?: string
  format?: string
  family?: string
  parameterSize?: string
  quantization?: string
  contextLength?: number
  capabilities: string[]
  parameterText?: string
  licenseExcerpt?: string
}

export function isAiModelRuntimeDetailInput(value: unknown): value is AiModelRuntimeDetailInput {
  return isRecord(value)
    && aiModelRuntimeIds.includes(value.runtimeId as AiModelRuntimeId)
    && typeof value.modelName === 'string'
    && value.modelName.trim().length > 0
    && value.modelName.length <= 500
    && !/[\r\n\0]/.test(value.modelName)
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
