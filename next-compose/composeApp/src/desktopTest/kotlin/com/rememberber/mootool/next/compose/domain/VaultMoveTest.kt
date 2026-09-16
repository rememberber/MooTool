package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VaultMoveTest {
    @Test
    fun rejectsNoOpAndSelfNesting() {
        assertFalse(VaultMove.canMoveToDirectory("note.md", ""))
        assertFalse(VaultMove.canMoveToDirectory("Work", "Work"))
        assertFalse(VaultMove.canMoveToDirectory("Work", "Work/nested"))
        assertTrue(VaultMove.canMoveToDirectory("Work/note.md", ""))
        assertTrue(VaultMove.canMoveToDirectory("a.md", "Work"))
        assertEquals("Work", VaultMove.parentDirectory("Work/note.md"))
        assertEquals("", VaultMove.parentDirectory("note.md"))
        assertEquals("", VaultMove.parentDirectory("Work"))
        assertEquals("Work", VaultMove.parentDirectory("Work/Nested"))
        assertEquals("Work/api.md", VaultMove.retargetAfterMove("Work/api.md", "readme.md", "docs/readme.md"))
        assertEquals("Archive/note.md", VaultMove.retargetAfterMove("Work/note.md", "Work", "Archive"))
        assertEquals("renamed.json", VaultMove.retargetAfterMove("old.json", "old.json", "renamed.json"))
    }

    @Test
    fun moveAffectsOpenPathMatchesElectronAffectsSelection() {
        assertFalse(VaultMove.moveAffectsOpenPath("", "Work/note.md"))
        assertFalse(VaultMove.moveAffectsOpenPath("a.json", "b.json"))
        assertTrue(VaultMove.moveAffectsOpenPath("a.json", "a.json"))
        assertTrue(VaultMove.moveAffectsOpenPath("Work/a.json", "Work"))
        assertFalse(VaultMove.moveAffectsOpenPath("Other/a.json", "Work"))
    }

    @Test
    fun retargetVaultPathsUpdatesSelectionAndOpenFile() {
        val (file, selected) = VaultMove.retargetVaultPaths(
            currentFile = "Work/note.md",
            vaultSelectedPath = "Work/Nested",
            from = "Work",
            next = "Archive",
        )
        assertEquals("Archive/note.md", file)
        assertEquals("Archive/Nested", selected)
    }

    @Test
    fun clearPathsAfterDeleteRemovesSelectionUnderFolder() {
        val (file, selected) = VaultMove.clearVaultPathsAfterDelete(
            deleted = "Work",
            currentFile = "Work/a.json",
            vaultSelectedPath = "Work/Nested",
        )
        assertEquals("", file)
        assertEquals("", selected)
        assertEquals("Other.json", VaultMove.clearPathIfDeleted("Work", "Other.json"))
    }
}
