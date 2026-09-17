package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitMergeConflictPresentationTest {
    @Test
    fun showConflictBadgeOnlyWhenFileConflict() {
        assertTrue(GitMergeConflictPresentation.showConflictBadge(fileConflict = true))
        assertFalse(GitMergeConflictPresentation.showConflictBadge(fileConflict = false))
    }

    @Test
    fun unresolvedHintDuringMergeOrRebaseWithConflicts() {
        assertEquals(
            "git.mergeUnresolvedHint",
            GitMergeConflictPresentation.unresolvedHintKey(merging = true, conflicts = 1, operation = "merge"),
        )
        assertEquals(
            "git.rebaseUnresolvedHint",
            GitMergeConflictPresentation.unresolvedHintKey(merging = true, conflicts = 2, operation = "rebase"),
        )
        assertNull(GitMergeConflictPresentation.unresolvedHintKey(merging = true, conflicts = 0, operation = "merge"))
        assertNull(GitMergeConflictPresentation.unresolvedHintKey(merging = false, conflicts = 1, operation = "merge"))
    }
}
