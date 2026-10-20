export interface QuickNoteGitChange {
  path: string
  originalPath?: string
  status: string
  conflict: boolean
}

export interface QuickNoteGitStatus {
  available: boolean
  repository: boolean
  branch: string
  remote: string
  ahead: number
  behind: number
  changes: QuickNoteGitChange[]
  conflicts: number
  merging: boolean
  operation: 'none' | 'merge' | 'rebase'
}

export interface QuickNoteGitCommit {
  hash: string
  shortHash: string
  author: string
  date: string
  message: string
}

export type QuickNoteGitAction = 'init' | 'configure-remote' | 'commit' | 'fetch' | 'pull' | 'push' | 'discard' | 'abort-merge' | 'resolve-conflict' | 'continue-operation'

export interface QuickNoteGitActionResult {
  success: boolean
  message: string
}

export type QuickNoteGitDiffPreview = 'text' | 'binary' | 'too-large'

export interface QuickNoteGitDiffFile {
  path: string
  originalPath?: string
  status: string
  before: string
  after: string
  preview: QuickNoteGitDiffPreview
}

export interface QuickNoteGitDiffResult {
  files: QuickNoteGitDiffFile[]
}
