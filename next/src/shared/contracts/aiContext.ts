import { aiClientIds, type AiClientId } from './ai'

export const aiContextCategories = ['instruction', 'skillMetadata', 'skillBody', 'memory', 'mcpSchema'] as const
export type AiContextCategory = (typeof aiContextCategories)[number]

export const aiContextLayers = ['resident', 'pathTriggered', 'onDemand', 'runtime'] as const
export type AiContextLayer = (typeof aiContextLayers)[number]

export type AiContextInspectorInput = {
  projectRoot: string
  targetPath: string
  clientId: AiClientId
  agentProfileId?: string
  branch?: string
  taskRef?: string
  selectedSkillNames: string[]
  memoryTokenBudget: number
  maxMemoryItems: number
  topN: number
}

export type AiContextItem = {
  id: string
  category: AiContextCategory
  layer: AiContextLayer
  name: string
  clientId?: AiClientId
  sourcePath?: string
  sourceToolId: 'instructionManager' | 'skillManager' | 'agentMemoryManager' | 'mcpManager'
  estimatedTokens: number
  reason: string
}

export type AiContextBreakdown = {
  category: AiContextCategory
  items: number
  estimatedTokens: number
}

export type AiContextDuplicateGroup = {
  category: 'instruction' | 'skillBody' | 'memory'
  itemIds: string[]
  names: string[]
  estimatedWasteTokens: number
}

export type AiContextRecommendationCode =
  | 'largeResidentContext'
  | 'largeInstruction'
  | 'largeSkillEntry'
  | 'duplicateContent'
  | 'unprobedMcp'
  | 'memoryBudgetExceeded'
  | 'profileProjectMismatch'

export type AiContextRecommendation = {
  code: AiContextRecommendationCode
  severity: 'info' | 'suggestion' | 'warning'
  sourceToolId: AiContextItem['sourceToolId'] | 'agentManager'
  itemId?: string
  message: string
}

export type AiContextInspectorSnapshot = {
  inspectedAt: string
  projectRoot: string
  targetPath: string
  clientId: AiClientId
  agentProfileId?: string
  items: AiContextItem[]
  topItems: AiContextItem[]
  breakdown: AiContextBreakdown[]
  totals: {
    estimatedTokens: number
    residentTokens: number
    pathTriggeredTokens: number
    onDemandTokens: number
    runtimeTokens: number
  }
  duplicates: AiContextDuplicateGroup[]
  recommendations: AiContextRecommendation[]
  mcpSchemas: {
    servers: number
    observedServers: number
    unknownServers: string[]
  }
  memory: {
    selected: number
    omittedByBudget: number
    budgetTokens: number
  }
  tokenizer: {
    id: 'heuristic-v1'
    label: string
    relativeErrorNotice: string
  }
}

export function isAiContextInspectorInput(value: unknown): value is AiContextInspectorInput {
  if (!isRecord(value)) return false
  return typeof value.projectRoot === 'string' && value.projectRoot.trim().length > 0 && value.projectRoot.length <= 4096
    && typeof value.targetPath === 'string' && value.targetPath.trim().length > 0 && value.targetPath.length <= 4096
    && aiClientIds.includes(value.clientId as AiClientId)
    && (value.agentProfileId === undefined || isUuid(value.agentProfileId))
    && optionalString(value.branch, 512)
    && optionalString(value.taskRef, 512)
    && Array.isArray(value.selectedSkillNames) && value.selectedSkillNames.length <= 200 && value.selectedSkillNames.every((item) => typeof item === 'string' && item.length <= 200)
    && integerInRange(value.memoryTokenBudget, 1, 100_000)
    && integerInRange(value.maxMemoryItems, 1, 200)
    && integerInRange(value.topN, 1, 100)
}

function optionalString(value: unknown, maximum: number): boolean {
  return value === undefined || (typeof value === 'string' && value.length <= maximum)
}

function integerInRange(value: unknown, minimum: number, maximum: number): boolean {
  return typeof value === 'number' && Number.isInteger(value) && value >= minimum && value <= maximum
}

function isUuid(value: unknown): boolean {
  return typeof value === 'string' && /^[0-9a-f-]{36}$/i.test(value)
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
