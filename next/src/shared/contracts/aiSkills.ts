import type { AiPrimaryClientId } from './ai'
import type { AiChangeApplyResult, AiChangePlan, AiChangeRollbackResult } from './aiChanges'

export type AiSkillInstallScope = 'user' | 'project'

export type AiSkillInstallInput = {
  sourceDirectory: string
  targetClientId: AiPrimaryClientId
  scope: AiSkillInstallScope
  projectRoot?: string
}

export type AiSkillInstallFile = {
  relativePath: string
  sizeBytes: number
  binary: boolean
  executable: boolean
}

export type AiSkillInstallFinding = {
  code: string
  severity: 'warning' | 'error'
  relativePath: string
}

export type AiSkillInstallPreview = {
  plan: AiChangePlan
  name: string
  description: string
  sourceDirectory: string
  targetDirectory: string
  targetClientId: AiPrimaryClientId
  scope: AiSkillInstallScope
  files: AiSkillInstallFile[]
  findings: AiSkillInstallFinding[]
  totalSizeBytes: number
  estimatedTokens: number
  requiresRiskConfirmation: boolean
}

export type AiSkillInstallApplyInput = {
  planId: string
  confirmRisks: boolean
}

export type AiSkillInstallApplyResult = AiChangeApplyResult
export type AiSkillInstallRollbackResult = AiChangeRollbackResult

export function isAiSkillInstallInput(value: unknown): value is AiSkillInstallInput {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) return false
  const input = value as Record<string, unknown>
  return typeof input.sourceDirectory === 'string'
    && input.sourceDirectory.length > 0
    && input.sourceDirectory.length <= 4096
    && (input.targetClientId === 'codex' || input.targetClientId === 'claudeCode')
    && (input.scope === 'user' || input.scope === 'project')
    && (input.projectRoot === undefined || (typeof input.projectRoot === 'string' && input.projectRoot.length <= 4096))
}

export function isAiSkillInstallApplyInput(value: unknown): value is AiSkillInstallApplyInput {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) return false
  const input = value as Record<string, unknown>
  return typeof input.planId === 'string' && typeof input.confirmRisks === 'boolean'
}
