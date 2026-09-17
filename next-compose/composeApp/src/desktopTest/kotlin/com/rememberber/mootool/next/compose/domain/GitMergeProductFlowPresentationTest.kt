package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitMergeProductFlowPresentationTest {
    private val changes = listOf(
        "conflict.json" to true,
        "readme.md" to false,
    )

    @Test
    fun autoSelectPicksFirstConflictWhenNothingSelected() {
        assertEquals(
            "conflict.json",
            GitMergeProductFlowPresentation.autoSelectConflictPath(
                merging = true,
                conflicts = 1,
                changes = changes,
                currentSelected = "",
            ),
        )
    }

    @Test
    fun autoSelectSkipsWhenConflictAlreadySelected() {
        assertNull(
            GitMergeProductFlowPresentation.autoSelectConflictPath(
                merging = true,
                conflicts = 1,
                changes = changes,
                currentSelected = "conflict.json",
            ),
        )
    }

    @Test
    fun productFlowHintDependsOnSelection() {
        assertEquals(
            "git.mergeProductFlowSelect",
            GitMergeProductFlowPresentation.productFlowHintKey(true, 1, selectedConflict = false),
        )
        assertEquals(
            "git.mergeProductFlowResolve",
            GitMergeProductFlowPresentation.productFlowHintKey(true, 1, selectedConflict = true),
        )
    }

    @Test
    fun evidenceReadyMatchesScriptExpectations() {
        assertTrue(
            GitMergeProductFlowPresentation.evidenceReady(
                merging = true,
                conflicts = 1,
                unmergedPaths = listOf(GitMergeProductFlowPresentation.EVIDENCE_CONFLICT_FILE),
            ),
        )
        assertFalse(GitMergeProductFlowPresentation.evidenceReady(true, 0, listOf("a")))
    }

    @Test
    fun resolveActionsRequireSelectedConflictAndIdle() {
        assertTrue(
            GitMergeProductFlowPresentation.resolveActionsEnabled(
                merging = true,
                selectedConflict = true,
                busy = false,
            ),
        )
        assertFalse(
            GitMergeProductFlowPresentation.resolveActionsEnabled(
                merging = true,
                selectedConflict = false,
                busy = false,
            ),
        )
        assertFalse(
            GitMergeProductFlowPresentation.resolveActionsEnabled(
                merging = true,
                selectedConflict = true,
                busy = true,
            ),
        )
    }
}
