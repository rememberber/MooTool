package com.rememberber.mootool.next.compose.domain

/** Vault Git 面板文案：merge/rebase 进行中（对齐 Electron `VaultGitStatus.operation`）。 */
object GitOperationPresentation {
    fun inProgressMessageKey(operation: String, merging: Boolean): String? {
        if (!merging) return null
        return when (operation) {
            "merge" -> "git.operationMerge"
            "rebase" -> "git.operationRebase"
            else -> "git.operationInProgress"
        }
    }

    fun abortButtonKey(operation: String): String =
        when (operation) {
            "rebase" -> "git.abortRebase"
            "merge" -> "git.abortMergeOnly"
            else -> "git.abortMerge"
        }

    fun confirmAbortKey(operation: String): String =
        when (operation) {
            "rebase" -> "git.confirmAbortRebase"
            "merge" -> "git.confirmAbortMerge"
            else -> "git.confirmAbort"
        }

    fun pullEnabled(remotePresent: Boolean, merging: Boolean, conflicts: Int): Boolean =
        remotePresent && !merging && conflicts == 0

    /** 对齐 Electron `VaultGitDialog` push：`remote` 且非 merge/rebase 进行中（冲突未解决时仍由引擎拒绝）。 */
    fun pushEnabled(remotePresent: Boolean, merging: Boolean): Boolean =
        remotePresent && !merging
}
