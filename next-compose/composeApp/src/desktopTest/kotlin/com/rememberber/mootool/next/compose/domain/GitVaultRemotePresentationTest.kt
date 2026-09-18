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
    fun fetchActionEnabledRespectsBusyAndRemote() {
        val remote = "https://x.git"
        assertTrue(GitVaultRemotePresentation.fetchActionEnabled(remote, busy = false))
        assertFalse(GitVaultRemotePresentation.fetchActionEnabled(remote, busy = true))
        assertFalse(GitVaultRemotePresentation.fetchActionEnabled("", busy = false))
    }

    @Test
    fun fetchStaysEnabledDuringMergeWithConflicts() {
        val remote = "https://x.git"
        assertTrue(
            GitVaultRemotePresentation.fetchActionEnabled(remote, busy = false),
            "fetch stays enabled during merge/rebase conflicts (Electron VaultGitDialog)",
        )
        assertFalse(
            GitVaultRemotePresentation.pullActionEnabled(
                persistedRemote = remote,
                merging = true,
                busy = false,
                conflicts = 2,
            ),
        )
    }

    @Test
    fun pullRequiresPersistedRemote() {
        assertFalse(GitVaultRemotePresentation.pullEnabled(persistedRemote = "", merging = false))
        assertTrue(GitVaultRemotePresentation.pullEnabled(persistedRemote = "https://x.git", merging = false))
        assertFalse(GitVaultRemotePresentation.pullEnabled(persistedRemote = "https://x.git", merging = true))
    }

    @Test
    fun pushPullActionRespectsBusyAndMerge() {
        val remote = "https://x.git"
        assertFalse(GitVaultRemotePresentation.pushActionEnabled(remote, merging = true, busy = false))
        assertFalse(GitVaultRemotePresentation.pullActionEnabled(remote, merging = true, busy = false))
        assertTrue(GitVaultRemotePresentation.pushActionEnabled(remote, merging = false, busy = false))
        assertFalse(GitVaultRemotePresentation.pushActionEnabled(remote, merging = false, busy = true))
    }

    @Test
    fun pushDisabledWhileUnresolvedConflicts() {
        val remote = "https://x.git"
        assertFalse(
            GitVaultRemotePresentation.pushActionEnabled(
                persistedRemote = remote,
                merging = false,
                busy = false,
                conflicts = 1,
            ),
        )
        assertTrue(
            GitVaultRemotePresentation.pushActionEnabled(
                persistedRemote = remote,
                merging = false,
                busy = false,
                conflicts = 0,
            ),
        )
    }

    @Test
    fun pullDisabledWhileUnresolvedConflicts() {
        val remote = "https://x.git"
        assertFalse(
            GitVaultRemotePresentation.pullActionEnabled(
                persistedRemote = remote,
                merging = false,
                busy = false,
                conflicts = 1,
            ),
        )
        assertTrue(
            GitVaultRemotePresentation.pullActionEnabled(
                persistedRemote = remote,
                merging = false,
                busy = false,
                conflicts = 0,
            ),
        )
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
