package com.rememberber.mootool.next.compose.domain

/** Vault Git 远程 URL 草稿 vs 已持久化 remote（对齐 Electron pull/fetch 只读 `status.remote`）。 */
object GitVaultRemotePresentation {
    fun unsavedRemoteDraft(draftTrimmed: String, persistedRemote: String): Boolean {
        val draft = draftTrimmed.trim()
        if (draft.isEmpty()) return false
        return draft != persistedRemote.trim()
    }

    fun networkRemotePresent(persistedRemote: String): Boolean = persistedRemote.isNotBlank()

    fun fetchEnabled(persistedRemote: String): Boolean =
        GitOperationPresentation.fetchEnabled(networkRemotePresent(persistedRemote))

    /** 对齐 `VaultGitDialog` 顶栏 fetch：`busy` 时禁用；冲突/merge 期间仍可用（Electron）。 */
    fun fetchActionEnabled(persistedRemote: String, busy: Boolean): Boolean =
        !busy && fetchEnabled(persistedRemote)

    fun pullEnabled(persistedRemote: String, merging: Boolean, conflicts: Int = 0): Boolean =
        GitOperationPresentation.pullEnabled(networkRemotePresent(persistedRemote), merging) &&
            conflicts == 0

    fun pushEnabled(persistedRemote: String, merging: Boolean, conflicts: Int = 0): Boolean =
        GitOperationPresentation.pushEnabled(networkRemotePresent(persistedRemote), merging) &&
            conflicts == 0

    /** 对齐 `VaultGitDialog` 顶栏 push/pull：`busy` 时禁用；未解决冲突时 push 禁用（DIFF-516，引擎亦拒绝）。 */
    fun pushActionEnabled(
        persistedRemote: String,
        merging: Boolean,
        busy: Boolean,
        conflicts: Int = 0,
    ): Boolean = !busy && pushEnabled(persistedRemote, merging, conflicts)

    /** 未解决冲突时 pull 禁用（DIFF-520，与 push/commit 一致；fetch 仍可用）。 */
    fun pullActionEnabled(
        persistedRemote: String,
        merging: Boolean,
        busy: Boolean,
        conflicts: Int = 0,
    ): Boolean = !busy && pullEnabled(persistedRemote, merging, conflicts)

    fun saveRemoteEnabled(
        busy: Boolean,
        repository: Boolean,
        draftRemoteTrimmed: String,
        statusRemote: String,
    ): Boolean =
        !busy &&
            GitOperationPresentation.configureRemoteEnabled(
                repository = repository,
                draftRemoteTrimmed = draftRemoteTrimmed,
                statusRemote = statusRemote,
            )
}
