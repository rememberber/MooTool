package com.rememberber.mootool.next.compose.domain

/** Vault Git merge/rebase 冲突态 UI（对齐 Electron `VaultGitDialog` 变更行 `<em>` 与进行中提示）。 */
object GitMergeConflictPresentation {
    fun showConflictBadge(fileConflict: Boolean): Boolean = fileConflict

    /** merge/rebase 进行中且仍有未解决冲突时在状态区追加说明。 */
    fun unresolvedHintKey(merging: Boolean, conflicts: Int, operation: String): String? {
        if (!merging || conflicts <= 0) return null
        return when (operation) {
            "merge" -> "git.mergeUnresolvedHint"
            "rebase" -> "git.rebaseUnresolvedHint"
            else -> "git.unresolvedConflictsHint"
        }
    }
}
