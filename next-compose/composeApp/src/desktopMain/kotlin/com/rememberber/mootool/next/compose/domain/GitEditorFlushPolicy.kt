package com.rememberber.mootool.next.compose.domain

enum class GitVaultFlushAction {
    Pull,
    Discard,
    AbortMerge,
    ResolveConflict,
    ContinueOperation,
    Commit,
    Push,
}

object GitEditorFlushPolicy {
    /**
     * Electron `prepareGitAction` saves before pull/continue only.
     * Compose additionally flushes before commit/push so staged files match the editor buffer.
     */
    fun shouldFlushEditor(action: GitVaultFlushAction?): Boolean = when (action) {
        GitVaultFlushAction.Discard,
        GitVaultFlushAction.AbortMerge,
        GitVaultFlushAction.ResolveConflict -> false
        GitVaultFlushAction.Pull,
        GitVaultFlushAction.ContinueOperation,
        GitVaultFlushAction.Commit,
        GitVaultFlushAction.Push -> true
        null -> false
    }

    fun refreshesVaultAfterSuccess(action: GitVaultFlushAction?): Boolean =
        action in setOf(
            GitVaultFlushAction.Pull,
            GitVaultFlushAction.Discard,
            GitVaultFlushAction.AbortMerge,
            GitVaultFlushAction.ResolveConflict,
            GitVaultFlushAction.ContinueOperation,
        )
}
