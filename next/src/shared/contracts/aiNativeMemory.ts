export type AiNativeMemoryArtifact = {
  id: string
  clientId: 'claudeCode'
  source: 'claudeAutoMemory'
  projectKey: string
  path: string
  root: string
  name: string
  entrypoint: boolean
  sizeBytes: number
  modifiedAt: string
  estimatedTokens: number
  contentExcerpt: string
  excerptTruncated: boolean
  sensitiveFindings: number
}

export type AiNativeMemorySnapshot = {
  scannedAt: string
  readOnly: true
  roots: string[]
  artifacts: AiNativeMemoryArtifact[]
  diagnostics: Array<{ severity: 'info' | 'warning'; message: string }>
}
