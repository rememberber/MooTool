package com.rememberber.mootool.next.compose.ui.components

import com.rememberber.mootool.next.compose.storage.VaultEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VaultTreeTest {
    @Test
    fun nestsFilesUnderDirectories() {
        val tree = buildVaultTree(
            listOf(
                VaultEntry("Work", "Work", true, 0),
                VaultEntry("Work/api.md", "api.md", false, 12),
                VaultEntry("readme.md", "readme.md", false, 8)
            )
        )
        assertEquals(2, tree.size)
        val work = tree.first { it.entry.directory }
        assertEquals("Work", work.entry.relativePath)
        assertEquals(listOf("Work/api.md"), work.children.map { it.entry.relativePath })
        assertTrue(tree.any { it.entry.relativePath == "readme.md" })
    }

    @Test
    fun vaultTreeExpandForMode_smart_expandsRootOnly() {
        val items = listOf(
            VaultEntry("Work", "Work", true, 0),
            VaultEntry("Work/nested", "nested", true, 0),
        )
        val map = vaultTreeExpandForMode("smart", items)
        assertEquals(true, map["Work"])
        assertEquals(false, map["Work/nested"])
    }

    @Test
    fun contextMenuHidesFileOnlyActionsOnDirectories() {
        val actions = listOf(
            VaultContextAction(VaultContextId.Rename, "Rename"),
            VaultContextAction(VaultContextId.Duplicate, "Duplicate", filesOnly = true),
            VaultContextAction(VaultContextId.Export, "Export", filesOnly = true),
            VaultContextAction(VaultContextId.Delete, "Delete")
        )
        val files = visibleVaultContextActions(actions, directory = false).map { it.id }
        val folders = visibleVaultContextActions(actions, directory = true).map { it.id }
        assertEquals(listOf(VaultContextId.Rename, VaultContextId.Duplicate, VaultContextId.Export, VaultContextId.Delete), files)
        assertEquals(listOf(VaultContextId.Rename, VaultContextId.Delete), folders)
    }
}
