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
        assertEquals("Work/api.md", VaultMove.retargetAfterMove("Work/api.md", "readme.md", "docs/readme.md"))
        assertEquals("Archive/note.md", VaultMove.retargetAfterMove("Work/note.md", "Work", "Archive"))
        assertEquals("renamed.json", VaultMove.retargetAfterMove("old.json", "old.json", "renamed.json"))
    }
}
