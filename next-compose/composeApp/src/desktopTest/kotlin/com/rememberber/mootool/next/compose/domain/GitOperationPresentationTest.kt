package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitOperationPresentationTest {
    @Test
    fun inProgressMessageKeyFollowsOperation() {
        assertNull(GitOperationPresentation.inProgressMessageKey("none", merging = false))
        assertNull(GitOperationPresentation.inProgressMessageKey("merge", merging = false))
        assertEquals("git.operationMerge", GitOperationPresentation.inProgressMessageKey("merge", merging = true))
        assertEquals("git.operationRebase", GitOperationPresentation.inProgressMessageKey("rebase", merging = true))
        assertEquals("git.operationInProgress", GitOperationPresentation.inProgressMessageKey("unknown", merging = true))
    }

    @Test
    fun abortPresentationFollowsOperation() {
        assertEquals("git.abortMergeOnly", GitOperationPresentation.abortButtonKey("merge"))
        assertEquals("git.abortRebase", GitOperationPresentation.abortButtonKey("rebase"))
        assertEquals("git.abortMerge", GitOperationPresentation.abortButtonKey("none"))
        assertEquals("git.confirmAbortMerge", GitOperationPresentation.confirmAbortKey("merge"))
        assertEquals("git.confirmAbortRebase", GitOperationPresentation.confirmAbortKey("rebase"))
        assertEquals("git.confirmAbort", GitOperationPresentation.confirmAbortKey("unknown"))
    }

    @Test
    fun pullEnabledRequiresCleanRemoteState() {
        assertTrue(GitOperationPresentation.pullEnabled(remotePresent = true, merging = false, conflicts = 0))
        assertFalse(GitOperationPresentation.pullEnabled(remotePresent = false, merging = false, conflicts = 0))
        assertFalse(GitOperationPresentation.pullEnabled(remotePresent = true, merging = true, conflicts = 0))
        assertFalse(GitOperationPresentation.pullEnabled(remotePresent = true, merging = false, conflicts = 1))
    }

    @Test
    fun pushEnabledMatchesElectronVaultGitDialog() {
        assertTrue(GitOperationPresentation.pushEnabled(remotePresent = true, merging = false))
        assertFalse(GitOperationPresentation.pushEnabled(remotePresent = false, merging = false))
        assertFalse(GitOperationPresentation.pushEnabled(remotePresent = true, merging = true))
        // 未解决冲突但非 merging：面板仍可点 push，引擎层拒绝（见 GitPushGuardTest）
        assertTrue(GitOperationPresentation.pushEnabled(remotePresent = true, merging = false))
    }

    @Test
    fun commitAndRemoteHelpersMatchElectronVaultGitDialog() {
        assertTrue(
            GitOperationPresentation.commitEnabled(
                merging = false,
                conflicts = 0,
                hasChanges = true,
                messageTrimmed = "msg",
            ),
        )
        assertFalse(
            GitOperationPresentation.commitEnabled(
                merging = true,
                conflicts = 0,
                hasChanges = true,
                messageTrimmed = "msg",
            ),
        )
        assertTrue(GitOperationPresentation.configureRemoteEnabled(repository = true, draftRemoteTrimmed = "", statusRemote = "origin"))
        assertFalse(GitOperationPresentation.configureRemoteEnabled(repository = false, draftRemoteTrimmed = "x", statusRemote = ""))
        assertTrue(GitOperationPresentation.continueOperationEnabled(merging = true, conflicts = 0))
        assertFalse(GitOperationPresentation.continueOperationEnabled(merging = true, conflicts = 1))
    }
}
