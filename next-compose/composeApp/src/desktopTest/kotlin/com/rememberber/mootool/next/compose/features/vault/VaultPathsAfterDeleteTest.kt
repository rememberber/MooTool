package com.rememberber.mootool.next.compose.features.vault

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VaultPathsAfterDeleteTest {
    @Test
    fun marksOpenFileClearedWhenDeleted() {
        val result = vaultPathsAfterDelete("a.json", "a.json", "a.json")
        assertEquals("", result.currentFile)
        assertEquals("", result.vaultSelectedPath)
        assertTrue(result.clearedOpenFile)
    }

    @Test
    fun keepsSiblingPaths() {
        val result = vaultPathsAfterDelete("other.json", "a.json", "Work")
        assertEquals("a.json", result.currentFile)
        assertEquals("Work", result.vaultSelectedPath)
        assertEquals(false, result.clearedOpenFile)
    }
}
