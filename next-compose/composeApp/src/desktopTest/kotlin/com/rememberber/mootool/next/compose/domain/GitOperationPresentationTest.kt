package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GitOperationPresentationTest {
    @Test
    fun inProgressMessageKeyFollowsOperation() {
        assertNull(GitOperationPresentation.inProgressMessageKey("none", merging = false))
        assertNull(GitOperationPresentation.inProgressMessageKey("merge", merging = false))
        assertEquals("git.operationMerge", GitOperationPresentation.inProgressMessageKey("merge", merging = true))
        assertEquals("git.operationRebase", GitOperationPresentation.inProgressMessageKey("rebase", merging = true))
        assertEquals("git.operationInProgress", GitOperationPresentation.inProgressMessageKey("unknown", merging = true))
    }
}
