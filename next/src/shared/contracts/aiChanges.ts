export type AiChangeOperationInput = {
  targetPath: string
  nextContent?: string
  nextContentBase64?: string
  summary: string
  expectedState?: 'missing' | 'existing' | 'any'
  mode?: 0o600 | 0o700
}

export type AiChangeOperationPreview = {
  id: string
  kind: 'create' | 'update'
  targetPath: string
  summary: string
  expectedHash?: string
  nextHash: string
  beforeSizeBytes: number
  afterSizeBytes: number
  redactedDiff: string
  binary: boolean
  executable: boolean
}

export type AiChangePlan = {
  id: string
  rootPath: string
  createdAt: string
  expiresAt: string
  state: 'pending'
  operations: AiChangeOperationPreview[]
}

export type AiChangeApplyResult = {
  planId: string
  snapshotId: string
  appliedAt: string
  operationCount: number
}

export type AiChangeRollbackResult = {
  snapshotId: string
  rolledBackAt: string
  operationCount: number
}
