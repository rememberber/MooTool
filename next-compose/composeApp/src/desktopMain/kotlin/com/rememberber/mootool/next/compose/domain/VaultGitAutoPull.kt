package com.rememberber.mootool.next.compose.domain

/**
 * Electron `pullJsonVault` / `pullQuickNoteVault`：编辑器干净且 Git 工作区无本地变更、无冲突、非 merge 中才自动 pull。
 * 编辑器脏由 [com.rememberber.mootool.next.compose.domain.VaultGitPullScheduler] 的 `hasUnsavedEditorChanges` 单独判断。
 */
object VaultGitAutoPull {
    fun mayPullCleanWorkingTree(status: GitStatus): Boolean {
        if (!status.available || !status.repository || status.remote.isBlank()) return false
        if (status.merging || status.conflicts > 0) return false
        if (status.changes.isNotEmpty()) return false
        return true
    }

    /** Electron `pull*Vault`：`broadcast('*-vault:changed')` after success or merge/conflict state. */
    fun shouldNotifyVaultAfterPull(pullResult: GitActionResult, afterStatus: GitStatus): Boolean =
        pullResult.success || afterStatus.merging || afterStatus.conflicts > 0
}
