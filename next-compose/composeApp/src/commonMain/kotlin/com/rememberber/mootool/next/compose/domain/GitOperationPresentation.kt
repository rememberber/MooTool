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

    /** 对齐 Electron `VaultGitDialog` pull：`remote` 且非 merge/rebase 进行中（未解决冲突仍由 `git pull` 结果处理）。 */
    fun pullEnabled(remotePresent: Boolean, merging: Boolean): Boolean =
        remotePresent && !merging

    /** 对齐 Electron `VaultGitDialog` push：`remote` 且非 merge/rebase 进行中（冲突未解决时仍由引擎拒绝）。 */
    fun pushEnabled(remotePresent: Boolean, merging: Boolean): Boolean =
        remotePresent && !merging

    fun fetchEnabled(remotePresent: Boolean): Boolean = remotePresent

    fun continueOperationEnabled(merging: Boolean, conflicts: Int): Boolean =
        merging && conflicts == 0

    /** 对齐 `VaultGitDialog` continue 钮：`busy` 时禁用，其余同 [continueOperationEnabled]。 */
    fun continueActionEnabled(busy: Boolean, merging: Boolean, conflicts: Int): Boolean =
        !busy && continueOperationEnabled(merging, conflicts)

    /** 对齐 Electron commit：`!merging && conflicts==0 && changes && message.trim()`。 */
    fun commitEnabled(
        merging: Boolean,
        conflicts: Int,
        hasChanges: Boolean,
        messageTrimmed: String,
    ): Boolean = !merging && conflicts == 0 && hasChanges && messageTrimmed.isNotEmpty()

    /** 对齐 `VaultGitDialog` 提交钮：`busy` 时禁用，其余同 [commitEnabled]。 */
    fun commitActionEnabled(
        busy: Boolean,
        merging: Boolean,
        conflicts: Int,
        hasChanges: Boolean,
        messageTrimmed: String,
    ): Boolean = !busy && commitEnabled(merging, conflicts, hasChanges, messageTrimmed)

    /** 对齐 Electron configure-remote：`repository && (draftRemote || statusRemote)`。 */
    fun configureRemoteEnabled(repository: Boolean, draftRemoteTrimmed: String, statusRemote: String): Boolean =
        repository && (draftRemoteTrimmed.isNotEmpty() || statusRemote.isNotBlank())

    fun initEnabled(available: Boolean, busy: Boolean): Boolean = !busy && available

    /** 对齐 `VaultGitDialog` 初始化仓库钮：同 [initEnabled]。 */
    fun initActionEnabled(available: Boolean, busy: Boolean): Boolean = initEnabled(available, busy)

    fun discardEnabled(busy: Boolean): Boolean = !busy

    /** 对齐 `VaultGitDialog` 刷新钮：同 [refreshEnabled]。 */
    fun refreshActionEnabled(busy: Boolean): Boolean = refreshEnabled(busy)

    /** 对齐 `VaultGitDialog` 丢弃变更钮：同 [discardEnabled]。 */
    fun discardActionEnabled(busy: Boolean): Boolean = discardEnabled(busy)

    /** 对齐 `VaultGitDialog` 中止 merge/rebase 确认钮：同 [abortConfirmEnabled]。 */
    fun abortActionEnabled(busy: Boolean): Boolean = abortConfirmEnabled(busy)

    /** 对齐 `VaultGitDialog` ours/theirs：同 [resolveConflictEnabled]。 */
    fun resolveConflictActionEnabled(busy: Boolean): Boolean = resolveConflictEnabled(busy)

    /** 对齐 Electron `VaultGitDialog`：merge/rebase 或存在未解决冲突时显示 abort。 */
    fun showAbortAction(merging: Boolean, conflicts: Int): Boolean = merging || conflicts > 0

    /** 对齐 Electron：merge/rebase 进行中且无未解决冲突时显示 continue。 */
    fun showContinueAction(merging: Boolean, conflicts: Int): Boolean =
        merging && conflicts == 0

    /** 对齐 DIFF-515：仅在 merge/rebase/冲突上下文显示变更计数。 */
    fun showChangeCounts(merging: Boolean, conflicts: Int): Boolean =
        merging || conflicts > 0

    fun resolveConflictEnabled(busy: Boolean): Boolean = !busy

    fun refreshEnabled(busy: Boolean): Boolean = !busy

    fun abortConfirmEnabled(busy: Boolean): Boolean = !busy
}
