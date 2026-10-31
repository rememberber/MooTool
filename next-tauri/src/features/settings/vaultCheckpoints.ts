import { quickNoteGitApi } from '../../platform/api/quickNoteGitApi'
import { vaultApi } from '../../platform/api/vaultApi'
import { defaultAppSettings, type AppSettings } from '../../platform/contracts/settings'
import type { VaultGitStatus } from '../../platform/contracts/vault'
import { VaultGitCheckpointScheduler, type VaultGitCheckpointResult } from '../../shared/vaultGitCheckpointScheduler'

const state = {
  settings: defaultAppSettings(),
  quickNoteDirty: false,
  jsonDirty: false
}

let pullTimer = 0
let windowBound = false

const quickNoteScheduler = new VaultGitCheckpointScheduler({
  enabled: () => state.settings.vault.autoCommit,
  hasUnsavedEditorChanges: () => state.quickNoteDirty,
  idleMilliseconds: () => state.settings.vault.autoCommitIdleSeconds * 1_000,
  inactiveMilliseconds: () => state.settings.vault.autoCommitInactiveSeconds * 1_000,
  checkpoint: (message) => checkpointQuickNote(message)
})

const jsonScheduler = new VaultGitCheckpointScheduler({
  enabled: () => state.settings.vault.autoCommit,
  hasUnsavedEditorChanges: () => state.jsonDirty,
  idleMilliseconds: () => state.settings.vault.autoCommitIdleSeconds * 1_000,
  inactiveMilliseconds: () => state.settings.vault.autoCommitInactiveSeconds * 1_000,
  checkpoint: (message) => checkpointJson(message)
})

export function configureVaultCheckpoints(settings: AppSettings): void {
  state.settings = settings
  quickNoteScheduler.start()
  jsonScheduler.start()
  bindWindowActivity()
  window.clearInterval(pullTimer)
  pullTimer = 0
  if (!settings.vault.autoPullMinutes || !desktop()) return
  pullTimer = window.setInterval(() => {
    void pullQuickNotes()
    void pullJson()
  }, settings.vault.autoPullMinutes * 60_000)
}

export function recordQuickNoteActivity(message: string): void {
  quickNoteScheduler.recordActivity(message)
}

export function recordJsonVaultActivity(message: string): void {
  jsonScheduler.recordActivity(message)
}

export function setQuickNoteEditorDirty(dirty: boolean): void {
  state.quickNoteDirty = dirty
}

export function setJsonVaultEditorDirty(dirty: boolean): void {
  state.jsonDirty = dirty
}

function desktop(): boolean {
  return typeof window !== 'undefined' && Boolean(window.__TAURI_INTERNALS__)
}

function bindWindowActivity(): void {
  if (windowBound || typeof window === 'undefined') return
  windowBound = true
  const update = () => {
    const active = !document.hidden && document.hasFocus()
    quickNoteScheduler.setWindowActive(active)
    jsonScheduler.setWindowActive(active)
  }
  window.addEventListener('focus', update)
  window.addEventListener('blur', update)
  document.addEventListener('visibilitychange', update)
}

async function checkpointQuickNote(message: string): Promise<VaultGitCheckpointResult> {
  if (!desktop()) return { success: true, message: 'Git checkpoint skipped' }
  return quickNoteGitApi.action({ action: 'automatic-checkpoint', message })
}

async function checkpointJson(message: string): Promise<VaultGitCheckpointResult> {
  if (!desktop()) return { success: true, message: 'Git checkpoint skipped' }
  try {
    let status = await vaultApi.gitStatus()
    if (!status.available) return { success: true, message: 'Git is unavailable; checkpoint skipped' }
    if (!status.repository) {
      const initialized = await vaultApi.runGit({ requestId: crypto.randomUUID(), operation: 'init', editorDirty: false })
      if (!initialized.success) return { success: false, message: initialized.stderr || initialized.stdout }
      status = await vaultApi.gitStatus()
    }
    if (status.merging || status.conflicts > 0) {
      return { success: true, message: 'Merge/rebase in progress; checkpoint skipped' }
    }
    return commitAndPush(status, message)
  } catch (cause) {
    return { success: false, message: cause instanceof Error ? cause.message : 'JSON Vault checkpoint failed' }
  }
}

async function commitAndPush(status: VaultGitStatus, message: string): Promise<VaultGitCheckpointResult> {
  let result: VaultGitCheckpointResult = { success: true, message: 'No changes to commit' }
  if (status.dirty) {
    const committed = await vaultApi.runGit({
      requestId: crypto.randomUUID(),
      operation: 'commit',
      message,
      editorDirty: false
    })
    if (!committed.success) return { success: false, message: committed.stderr || committed.stdout }
    result = { success: true, message: committed.stdout || 'Done' }
  }
  if (status.remote && (status.dirty || status.ahead > 0)) {
    const pushed = await vaultApi.runGit({ requestId: crypto.randomUUID(), operation: 'push', editorDirty: false })
    return { success: pushed.success, message: pushed.stderr || pushed.stdout || result.message }
  }
  return result
}

async function pullQuickNotes(): Promise<void> {
  if (state.quickNoteDirty) return
  try {
    const status = await quickNoteGitApi.status()
    if (!status.repository || !status.remote || status.merging || status.conflicts > 0 || status.changes.length > 0) return
    await quickNoteGitApi.action({ action: 'pull' })
  } catch {
    /* The next interval retries a transient Git failure. */
  }
}

async function pullJson(): Promise<void> {
  if (state.jsonDirty) return
  try {
    const status = await vaultApi.gitStatus()
    if (!status.repository || !status.remote || status.merging || status.conflicts > 0 || status.dirty) return
    await vaultApi.runGit({ requestId: crypto.randomUUID(), operation: 'pull', editorDirty: false })
  } catch {
    /* JSON Vault may be disconnected. */
  }
}
