package com.rememberber.mootool.next.compose.ui.components

import com.rememberber.mootool.next.compose.storage.VaultEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VaultTreeKeyTest {
    private val tree = buildVaultTree(
        listOf(
            VaultEntry("Work", "Work", true, 0),
            VaultEntry("Work/api.md", "api.md", false, 12),
            VaultEntry("readme.md", "readme.md", false, 8)
        )
    )

    @Test
    fun arrowsMoveAmongVisibleRowsAndEnterOpensFiles() {
        val expanded = mapOf("Work" to true)
        val down = applyVaultTreeKey("down", "Work", tree, expanded)
        assertEquals("Work/api.md", down.focusedPath)
        val enter = applyVaultTreeKey("enter", "Work/api.md", tree, expanded)
        assertEquals("Work/api.md", enter.openPath)
        val left = applyVaultTreeKey("left", "Work", tree, expanded)
        assertEquals(false, left.expanded?.get("Work"))
        val collapsed = mapOf("Work" to false)
        val right = applyVaultTreeKey("right", "Work", tree, collapsed)
        assertEquals(true, right.expanded?.get("Work"))
        val enterDir = applyVaultTreeKey("enter", "Work", tree, expanded)
        assertNull(enterDir.openPath)
        assertEquals("Work", enterDir.selectPath)
    }

    @Test
    fun visibleListHidesCollapsedChildren() {
        val hidden = visibleVaultEntries(tree, mapOf("Work" to false)).map { it.relativePath }
        assertEquals(listOf("Work", "readme.md"), hidden)
        val shown = visibleVaultEntries(tree, mapOf("Work" to true)).map { it.relativePath }
        assertEquals(listOf("Work", "Work/api.md", "readme.md"), shown)
    }

    @Test
    fun expandSelectionOpensAncestorsAndSelectedDirectory() {
        val items = listOf(
            VaultEntry("Work", "Work", true, 0),
            VaultEntry("Work/Nested", "Nested", true, 0),
            VaultEntry("Work/Nested/note.md", "note.md", false, 1)
        )
        val expanded = mutableMapOf("Work" to false, "Work/Nested" to false)
        expandVaultPathForSelection("Work/Nested", items, expanded)
        assertEquals(true, expanded["Work"])
        assertEquals(true, expanded["Work/Nested"])
        expandVaultPathForSelection("Work/Nested/note.md", items, expanded)
        assertEquals(true, expanded["Work"])
        assertEquals(true, expanded["Work/Nested"])
    }
}
