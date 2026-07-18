import type { AiAgentProfile, AiAgentProfileSaveInput } from './aiAgents'
import { isAiAgentProfileSaveInput } from './aiAgents'

export const aiAgentProfileShareFormat = 'mootool.agent-profile' as const

export type AiAgentProfileShareDocument = {
  format: typeof aiAgentProfileShareFormat
  version: 1
  exportedAt: string
  profile: Omit<AiAgentProfileSaveInput, 'id' | 'workingDirectory'>
}

export function createAiAgentProfileShareDocument(profile: AiAgentProfile, exportedAt = new Date().toISOString()): AiAgentProfileShareDocument {
  return {
    format: aiAgentProfileShareFormat,
    version: 1,
    exportedAt,
    profile: {
      name: profile.name,
      clientId: profile.clientId,
      model: profile.model,
      modelRuntimeId: profile.modelRuntimeId,
      localModelDigest: profile.localModelDigest,
      configProfile: profile.configProfile,
      permissionMode: profile.permissionMode,
      mcpServerNames: [...profile.mcpServerNames],
      skillNames: [...profile.skillNames],
      environmentVariableRefs: [...profile.environmentVariableRefs],
      optionalFlags: [...profile.optionalFlags]
    }
  }
}

export function isAiAgentProfileShareDocument(value: unknown): value is AiAgentProfileShareDocument {
  if (!isRecord(value)
    || value.format !== aiAgentProfileShareFormat
    || value.version !== 1
    || typeof value.exportedAt !== 'string'
    || !Number.isFinite(Date.parse(value.exportedAt))
    || !isRecord(value.profile)) return false
  return isAiAgentProfileSaveInput({ ...value.profile, workingDirectory: '/mootool-share-validation' })
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
