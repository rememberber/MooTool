package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitEditorFlushPolicyTest {
    @Test
    fun flushBeforePullCommitPushNotBeforeDiscard() {
        assertTrue(GitEditorFlushPolicy.shouldFlushEditor(GitVaultFlushAction.Pull))
        assertTrue(GitEditorFlushPolicy.shouldFlushEditor(GitVaultFlushAction.Commit))
        assertTrue(GitEditorFlushPolicy.shouldFlushEditor(GitVaultFlushAction.Push))
        assertFalse(GitEditorFlushPolicy.shouldFlushEditor(GitVaultFlushAction.Discard))
    }

    @Test
    fun vaultRefreshAfterPullNotAfterCommit() {
        assertTrue(GitEditorFlushPolicy.refreshesVaultAfterSuccess(GitVaultFlushAction.Pull))
        assertFalse(GitEditorFlushPolicy.refreshesVaultAfterSuccess(GitVaultFlushAction.Commit))
        assertFalse(GitEditorFlushPolicy.refreshesVaultAfterSuccess(GitVaultFlushAction.Push))
    }

    @Test
    fun fetchAndInitDoNotImplicitlyFlush() {
        assertFalse(GitEditorFlushPolicy.shouldFlushEditor(null))
    }
}
