import { invoke } from '@tauri-apps/api/core'
import type { QuickNoteGitAction, QuickNoteGitActionResult, QuickNoteGitCommit, QuickNoteGitDiffResult, QuickNoteGitStatus } from '../contracts/quickNoteGit'

export interface QuickNoteGitApi {
  status(): Promise<QuickNoteGitStatus>
  history(): Promise<QuickNoteGitCommit[]>
  diff(input: { path?: string; commit?: string }): Promise<QuickNoteGitDiffResult>
  action(input: { action: QuickNoteGitAction; message?: string; remote?: string; path?: string; strategy?: 'ours' | 'theirs' }): Promise<QuickNoteGitActionResult>
}

const emptyStatus = (): QuickNoteGitStatus => ({
  available: false,
  repository: false,
  branch: '',
  remote: '',
  ahead: 0,
  behind: 0,
  changes: [],
  conflicts: 0,
  merging: false,
  operation: 'none'
})

const desktopApi: QuickNoteGitApi = {
  status: () => invoke<QuickNoteGitStatus>('quick_note_git_status'),
  history: () => invoke<QuickNoteGitCommit[]>('quick_note_git_history'),
  diff: (input) => invoke<QuickNoteGitDiffResult>('quick_note_git_diff', { input }),
  action: (input) => invoke<QuickNoteGitActionResult>('quick_note_git_action', { input })
}

const browserApi: QuickNoteGitApi = {
  status: async () => emptyStatus(),
  history: async () => [],
  diff: async () => ({ files: [] }),
  action: async () => { throw new Error('Quick Note Git requires the Tauri desktop app') }
}

export const quickNoteGitApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__ ? desktopApi : browserApi
