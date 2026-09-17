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

    fun pullEnabled(persistedRemote: String, merging: Boolean): Boolean =
        GitOperationPresentation.pullEnabled(networkRemotePresent(persistedRemote), merging)

    fun pushEnabled(persistedRemote: String, merging: Boolean): Boolean =
        GitOperationPresentation.pushEnabled(networkRemotePresent(persistedRemote), merging)
}
