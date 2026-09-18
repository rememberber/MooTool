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
    fun autoSelectPrefersEvidenceConflictFileAmongMany() {
        val many = listOf(
            "aaa.json" to true,
            GitMergeProductFlowPresentation.EVIDENCE_CONFLICT_FILE to true,
            "zzz.json" to true,
        )
        assertEquals(
            GitMergeProductFlowPresentation.EVIDENCE_CONFLICT_FILE,
            GitMergeProductFlowPresentation.autoSelectConflictPath(
                merging = true,
                conflicts = 3,
                changes = many,
                currentSelected = "",
            ),
        )
        assertEquals(
            GitMergeProductFlowPresentation.EVIDENCE_CONFLICT_FILE,
            GitMergeProductFlowPresentation.preferredConflictSelectionPath(
                listOf("aaa.json", GitMergeProductFlowPresentation.EVIDENCE_CONFLICT_FILE),
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
        assertEquals(
            "git.rebaseProductFlowSelect",
            GitMergeProductFlowPresentation.productFlowHintKey(
                true,
                1,
                selectedConflict = false,
                operation = "rebase",
            ),
        )
        assertEquals(
            "git.rebaseProductFlowResolve",
            GitMergeProductFlowPresentation.productFlowHintKey(
                true,
                1,
                selectedConflict = true,
                operation = "rebase",
            ),
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

    @Test
    fun mergeContinueRequiresResolvedConflictsAndIdle() {
        assertTrue(
            GitMergeProductFlowPresentation.mergeContinueActionEnabled(
                merging = true,
                conflicts = 0,
                busy = false,
            ),
        )
        assertFalse(
            GitMergeProductFlowPresentation.mergeContinueActionEnabled(
                merging = true,
                conflicts = 1,
                busy = false,
            ),
        )
        assertFalse(
            GitMergeProductFlowPresentation.mergeContinueActionEnabled(
                merging = true,
                conflicts = 0,
                busy = true,
            ),
        )
    }
}
