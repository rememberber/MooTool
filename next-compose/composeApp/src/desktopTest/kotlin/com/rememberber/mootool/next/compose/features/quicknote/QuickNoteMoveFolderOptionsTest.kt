package com.rememberber.mootool.next.compose.features.quicknote

import com.rememberber.mootool.next.compose.storage.VaultEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class QuickNoteMoveFolderOptionsTest {
    @Test
    fun movingFileKeepsAncestorDirectories() {
        val entries = listOf(
            VaultEntry("Work", "Work", true, 0),
            VaultEntry("Work/Nested", "Nested", true, 0),
            VaultEntry("Work/Nested/note.md", "note.md", false, 1),
            VaultEntry("Other", "Other", true, 0),
        )
        val options = quickNoteMoveFolderOptions(entries, "Work/Nested/note.md", "root")
        assertEquals(listOf("", "Other", "Work", "Work/Nested"), options.map { it.first })
    }

    @Test
    fun movingDirectoryExcludesItselfAndDescendants() {
        val entries = listOf(
            VaultEntry("Work", "Work", true, 0),
            VaultEntry("Work/Nested", "Nested", true, 0),
            VaultEntry("Other", "Other", true, 0),
        )
        val options = quickNoteMoveFolderOptions(entries, "Work", "root")
        assertEquals(listOf("", "Other"), options.map { it.first })
    }
}
