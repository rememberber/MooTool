package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitVaultRemotePresentationTest {
    @Test
    fun detectsUnsavedDraft() {
        assertTrue(GitVaultRemotePresentation.unsavedRemoteDraft("https://new.git", "https://old.git"))
        assertFalse(GitVaultRemotePresentation.unsavedRemoteDraft("https://same.git", "https://same.git"))
    }

    @Test
    fun pullRequiresPersistedRemote() {
        assertFalse(GitVaultRemotePresentation.pullEnabled(persistedRemote = "", merging = false))
        assertTrue(GitVaultRemotePresentation.pullEnabled(persistedRemote = "https://x.git", merging = false))
        assertFalse(GitVaultRemotePresentation.pullEnabled(persistedRemote = "https://x.git", merging = true))
    }
}
