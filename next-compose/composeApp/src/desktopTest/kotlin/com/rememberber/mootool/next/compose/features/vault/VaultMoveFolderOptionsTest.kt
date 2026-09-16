package com.rememberber.mootool.next.compose.features.vault

import com.rememberber.mootool.next.compose.storage.VaultEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class VaultMoveFolderOptionsTest {
    @Test
    fun movingFileKeepsAncestorDirectories() {
        val entries = listOf(
            VaultEntry("Work", "Work", true, 0),
            VaultEntry("Work/Nested", "Nested", true, 0),
            VaultEntry("Work/Nested/note.json", "note.json", false, 1),
            VaultEntry("Other", "Other", true, 0),
        )
        val options = vaultMoveFolderOptions(entries, "Work/Nested/note.json", "root")
        assertEquals(listOf("", "Other", "Work", "Work/Nested"), options.map { it.first })
    }

    @Test
    fun movingDirectoryExcludesItselfAndDescendants() {
        val entries = listOf(
            VaultEntry("Work", "Work", true, 0),
            VaultEntry("Work/Nested", "Nested", true, 0),
            VaultEntry("Other", "Other", true, 0),
        )
        val options = vaultMoveFolderOptions(entries, "Work", "root")
        assertEquals(listOf("", "Other"), options.map { it.first })
    }
}
