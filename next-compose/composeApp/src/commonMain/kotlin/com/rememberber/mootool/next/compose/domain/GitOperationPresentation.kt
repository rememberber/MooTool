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
}
