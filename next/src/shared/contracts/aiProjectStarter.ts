import type { AiChangeApplyResult, AiChangePlan, AiChangeRollbackResult } from './aiChanges'

export const aiProjectStarterItems = ['instructions', 'projectSkill', 'mcpManifest', 'gitignore'] as const
export type AiProjectStarterItem = (typeof aiProjectStarterItems)[number]

export type AiProjectStarterPreviewInput = {
  projectRoot: string
  items: AiProjectStarterItem[]
}

export type AiProjectStarterSkipped = {
  item: AiProjectStarterItem
  path: string
  reason: 'alreadyExists' | 'alreadyConfigured'
}

export type AiProjectStarterPreview = {
  plan: AiChangePlan
  skipped: AiProjectStarterSkipped[]
}

export type AiProjectStarterApplyResult = AiChangeApplyResult
export type AiProjectStarterRollbackResult = AiChangeRollbackResult

export function isAiProjectStarterPreviewInput(value: unknown): value is AiProjectStarterPreviewInput {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) return false
  const input = value as Record<string, unknown>
  return typeof input.projectRoot === 'string'
    && input.projectRoot.trim().length > 0
    && input.projectRoot.length <= 4096
    && Array.isArray(input.items)
    && input.items.length > 0
    && input.items.length <= aiProjectStarterItems.length
    && input.items.every((item) => aiProjectStarterItems.includes(item as AiProjectStarterItem))
    && new Set(input.items).size === input.items.length
}
