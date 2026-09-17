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

    @Test
    fun saveRemoteEnabledMatchesConfigureRemoteAndBusy() {
        assertFalse(
            GitVaultRemotePresentation.saveRemoteEnabled(
                busy = true,
                repository = true,
                draftRemoteTrimmed = "https://x.git",
                statusRemote = "",
            ),
        )
        assertTrue(
            GitVaultRemotePresentation.saveRemoteEnabled(
                busy = false,
                repository = true,
                draftRemoteTrimmed = "",
                statusRemote = "https://x.git",
            ),
        )
        assertFalse(
            GitVaultRemotePresentation.saveRemoteEnabled(
                busy = false,
                repository = false,
                draftRemoteTrimmed = "https://x.git",
                statusRemote = "",
            ),
        )
    }
}
