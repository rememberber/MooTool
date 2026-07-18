import { aiClientIds, type AiClientId, type AiDiagnostic } from './ai'

export type AiInstructionPreviewInput = {
  projectRoot: string
  targetPath: string
  clientId?: AiClientId
}

export type AiEffectiveInstruction = {
  artifactId: string
  name: string
  path: string
  clientId: AiClientId
  scope: 'user' | 'project'
  appliesTo: string
  estimatedTokens: number
  reason: 'userScope' | 'directoryAncestor' | 'projectScope' | 'pathPattern'
  order: number
}

export type AiInstructionPreview = {
  projectRoot: string
  targetPath: string
  instructions: AiEffectiveInstruction[]
  totalEstimatedTokens: number
  diagnostics: AiDiagnostic[]
}

export function isAiInstructionPreviewInput(value: unknown): value is AiInstructionPreviewInput {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) return false
  const input = value as Record<string, unknown>
  return typeof input.projectRoot === 'string'
    && input.projectRoot.length > 0
    && input.projectRoot.length <= 4096
    && typeof input.targetPath === 'string'
    && input.targetPath.length > 0
    && input.targetPath.length <= 4096
    && (input.clientId === undefined || aiClientIds.includes(input.clientId as AiClientId))
}
