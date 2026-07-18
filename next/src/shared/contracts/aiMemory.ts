export const aiMemoryKinds = ['userPreference', 'projectFact', 'technicalDecision', 'taskSummary', 'agentPrivate', 'temporary'] as const
export type AiMemoryKind = (typeof aiMemoryKinds)[number]

export const aiMemoryScopes = ['task', 'branch', 'directory', 'project', 'agentProfile', 'user'] as const
export type AiMemoryScope = (typeof aiMemoryScopes)[number]

export const aiMemorySensitivities = ['public', 'internal', 'private', 'restricted'] as const
export type AiMemorySensitivity = (typeof aiMemorySensitivities)[number]

export const aiMemorySourceKinds = ['user', 'taskSummary', 'decisionRecord', 'document', 'clientAdapter', 'import'] as const
export type AiMemorySourceKind = (typeof aiMemorySourceKinds)[number]

export type AiMemory = {
  id: string
  kind: AiMemoryKind
  scope: AiMemoryScope
  scopeValue?: string
  content: string
  sourceKind: AiMemorySourceKind
  sourceRef?: string
  confidence: number
  sensitivity: AiMemorySensitivity
  createdBy: 'user' | 'import' | 'agentCandidate'
  createdAt: string
  updatedAt: string
  lastUsedAt?: string
  expiresAt?: string
  archivedAt?: string
  supersededBy?: string
  fingerprint: string
}

export type AiMemoryCandidate = {
  id: string
  kind: AiMemoryKind
  proposedScope: AiMemoryScope
  proposedScopeValue?: string
  content: string
  sourceKind: Exclude<AiMemorySourceKind, 'clientAdapter'>
  sourceRef: string
  evidenceSummary: string
  confidence: number
  sensitivity: AiMemorySensitivity
  status: 'pending' | 'approved' | 'rejected'
  createdAt: string
  reviewedAt?: string
  approvedMemoryId?: string
}

export type AiMemoryInjectionEvent = {
  id: string
  memoryId: string
  targetTaskRef: string
  selectionReason: string
  tokenCount: number
  injectedAt: string
}

export type AiMemoryListInput = {
  keyword?: string
  kind?: AiMemoryKind
  scope?: AiMemoryScope
  includeArchived?: boolean
}

export type AiMemorySaveInput = {
  id?: string
  kind: AiMemoryKind
  scope: AiMemoryScope
  scopeValue?: string
  content: string
  sourceKind: AiMemorySourceKind
  sourceRef?: string
  confidence: number
  sensitivity: AiMemorySensitivity
  expiresAt?: string
}

export type AiMemoryCandidateSaveInput = {
  kind: AiMemoryKind
  proposedScope: AiMemoryScope
  proposedScopeValue?: string
  content: string
  sourceKind: Exclude<AiMemorySourceKind, 'clientAdapter'>
  sourceRef: string
  evidenceSummary: string
  confidence: number
  sensitivity: AiMemorySensitivity
}

export type AiMemoryCandidateReviewInput = {
  candidateId: string
  action: 'approve' | 'reject'
}

export type AiMemoryPreviewInput = {
  projectRoot?: string
  targetPath?: string
  branch?: string
  agentProfileId?: string
  taskRef?: string
  query?: string
  tokenBudget: number
  maxItems: number
}

export type AiEffectiveMemory = {
  memory: AiMemory
  estimatedTokens: number
  reason: 'taskScope' | 'branchScope' | 'directoryScope' | 'projectScope' | 'agentProfileScope' | 'userScope'
  rank: number
  semanticScore?: number
}

export type AiMemoryPreview = {
  memories: AiEffectiveMemory[]
  totalEstimatedTokens: number
  omittedByBudget: number
}

export type AiMemoryStats = {
  active: number
  pendingCandidates: number
  archived: number
  expiringSoon: number
}

export type AiMemorySnapshot = {
  memories: AiMemory[]
  candidates: AiMemoryCandidate[]
  stats: AiMemoryStats
}

export function isAiMemoryListInput(value: unknown): value is AiMemoryListInput {
  if (value === undefined || value === null) return true
  if (!isRecord(value)) return false
  return optionalString(value.keyword, 200)
    && (value.kind === undefined || aiMemoryKinds.includes(value.kind as AiMemoryKind))
    && (value.scope === undefined || aiMemoryScopes.includes(value.scope as AiMemoryScope))
    && (value.includeArchived === undefined || typeof value.includeArchived === 'boolean')
}

export function isAiMemorySaveInput(value: unknown): value is AiMemorySaveInput {
  if (!isRecord(value)) return false
  return optionalUuid(value.id)
    && aiMemoryKinds.includes(value.kind as AiMemoryKind)
    && aiMemoryScopes.includes(value.scope as AiMemoryScope)
    && optionalString(value.scopeValue, 4096)
    && typeof value.content === 'string'
    && value.content.trim().length > 0
    && value.content.length <= 32768
    && aiMemorySourceKinds.includes(value.sourceKind as AiMemorySourceKind)
    && optionalString(value.sourceRef, 4096)
    && typeof value.confidence === 'number'
    && Number.isFinite(value.confidence)
    && value.confidence >= 0
    && value.confidence <= 1
    && aiMemorySensitivities.includes(value.sensitivity as AiMemorySensitivity)
    && optionalIsoDate(value.expiresAt)
}

export function isAiMemoryCandidateSaveInput(value: unknown): value is AiMemoryCandidateSaveInput {
  if (!isRecord(value)) return false
  return aiMemoryKinds.includes(value.kind as AiMemoryKind)
    && aiMemoryScopes.includes(value.proposedScope as AiMemoryScope)
    && optionalString(value.proposedScopeValue, 4096)
    && typeof value.content === 'string'
    && value.content.trim().length > 0
    && value.content.length <= 32768
    && (value.sourceKind === 'user' || value.sourceKind === 'taskSummary' || value.sourceKind === 'decisionRecord' || value.sourceKind === 'document' || value.sourceKind === 'import')
    && typeof value.sourceRef === 'string'
    && value.sourceRef.trim().length > 0
    && value.sourceRef.length <= 4096
    && typeof value.evidenceSummary === 'string'
    && value.evidenceSummary.length <= 4096
    && typeof value.confidence === 'number'
    && Number.isFinite(value.confidence)
    && value.confidence >= 0
    && value.confidence <= 1
    && aiMemorySensitivities.includes(value.sensitivity as AiMemorySensitivity)
}

export function isAiMemoryCandidateReviewInput(value: unknown): value is AiMemoryCandidateReviewInput {
  return isRecord(value) && isUuid(value.candidateId) && (value.action === 'approve' || value.action === 'reject')
}

export function isAiMemoryPreviewInput(value: unknown): value is AiMemoryPreviewInput {
  if (!isRecord(value)) return false
  return optionalString(value.projectRoot, 4096)
    && optionalString(value.targetPath, 4096)
    && optionalString(value.branch, 512)
    && optionalString(value.agentProfileId, 512)
    && optionalString(value.taskRef, 512)
    && optionalString(value.query, 500)
    && typeof value.tokenBudget === 'number'
    && Number.isInteger(value.tokenBudget)
    && value.tokenBudget >= 1
    && value.tokenBudget <= 100000
    && typeof value.maxItems === 'number'
    && Number.isInteger(value.maxItems)
    && value.maxItems >= 1
    && value.maxItems <= 200
}

export function isAiMemoryId(value: unknown): value is string {
  return isUuid(value)
}

function optionalString(value: unknown, maximum: number): boolean {
  return value === undefined || (typeof value === 'string' && value.length <= maximum)
}

function optionalIsoDate(value: unknown): boolean {
  return value === undefined || (typeof value === 'string' && value.length <= 64 && Number.isFinite(Date.parse(value)))
}

function optionalUuid(value: unknown): boolean {
  return value === undefined || isUuid(value)
}

function isUuid(value: unknown): value is string {
  return typeof value === 'string' && /^[0-9a-f-]{36}$/i.test(value)
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
