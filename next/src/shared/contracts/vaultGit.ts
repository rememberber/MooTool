export type VaultGitChange = {
  path: string
  originalPath?: string
  status: string
  conflict: boolean
}

export type VaultGitStatus = {
  available: boolean
  repository: boolean
  branch: string
  remote: string
  ahead: number
  behind: number
  changes: VaultGitChange[]
  conflicts: number
  merging: boolean
}

export type VaultGitCommit = {
  hash: string
  shortHash: string
  author: string
  date: string
  message: string
}

export type VaultGitAction = 'init' | 'configure-remote' | 'commit' | 'fetch' | 'pull' | 'push' | 'discard' | 'abort-merge' | 'resolve-conflict'

export type VaultGitActionInput = {
  action: VaultGitAction
  message?: string
  remote?: string
  path?: string
  strategy?: 'ours' | 'theirs'
}

export type VaultGitActionResult = {
  success: boolean
  message: string
}

export type VaultGitDiffInput = {
  path?: string
  commit?: string
}
