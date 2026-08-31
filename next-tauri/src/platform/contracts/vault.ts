export interface VaultFileEntry {
  relativePath: string
  sizeBytes: number
  modifiedAt: number
}

export interface VaultDocument {
  relativePath: string
  content: string
  fingerprint: string
  modifiedAt: number
}

export interface VaultSaveRequest {
  relativePath: string
  content: string
  expectedFingerprint: string | null
}

export interface VaultTrashResult {
  relativePath: string
  recoveryPath: string
}

export interface VaultGitStatus {
  available: boolean
  repository: boolean
  branch: string
  dirty: boolean
  changedFiles: number
  ahead: number
  behind: number
  remote: string
}

export interface VaultGitCommit {
  hash: string
  author: string
  timestamp: number
  subject: string
}

export interface VaultGitDetails {
  diff: string
  commits: VaultGitCommit[]
}

export interface VaultSnapshot {
  rootPath: string | null
  files: VaultFileEntry[]
  directories: string[]
  git: VaultGitStatus
}

export type VaultGitOperation = 'init' | 'pull' | 'commit' | 'push'

export interface VaultGitRequest {
  requestId: string
  operation: VaultGitOperation
  message?: string
  editorDirty: boolean
}

export interface VaultGitResult {
  operation: VaultGitOperation
  success: boolean
  exitCode: number | null
  stdout: string
  stderr: string
}

export interface VaultChangedEvent {
  reason: string
}

export interface VaultApi {
  chooseRootDirectory(): Promise<string | null>
  snapshot(): Promise<VaultSnapshot>
  configure(rootDirectory: string): Promise<VaultSnapshot>
  disconnect(): Promise<void>
  read(relativePath: string): Promise<VaultDocument>
  save(request: VaultSaveRequest): Promise<VaultDocument>
  createDirectory(relativePath: string): Promise<string>
  move(relativePath: string, destinationPath: string, expectedFingerprint: string | null): Promise<string>
  duplicate(relativePath: string, destinationPath: string, expectedFingerprint: string): Promise<VaultDocument>
  delete(relativePath: string, expectedFingerprint: string): Promise<VaultTrashResult>
  deleteEntry(relativePath: string, expectedFingerprint: string | null): Promise<VaultTrashResult>
  gitStatus(): Promise<VaultGitStatus>
  gitDetails(relativePath?: string): Promise<VaultGitDetails>
  configureGitRemote(remote: string): Promise<VaultGitStatus>
  runGit(request: VaultGitRequest): Promise<VaultGitResult>
  cancelGit(requestId: string): Promise<boolean>
  subscribe(listener: (event: VaultChangedEvent) => void): Promise<() => void>
}
