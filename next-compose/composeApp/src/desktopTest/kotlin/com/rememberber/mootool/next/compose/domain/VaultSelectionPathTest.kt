package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.storage.VaultEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class VaultSelectionPathTest {
    @Test
    fun parentFromFileSelection() {
        val entries = listOf(
            VaultEntry("a/b.json", "b.json", false, 0),
            VaultEntry("dir", "dir", true, 0),
        )
        assertEquals("a", VaultSelectionPath.parentDirectory("a/b.json", entries))
    }

    @Test
    fun parentFromDirectorySelection() {
        val entries = listOf(VaultEntry("dir", "dir", true, 0))
        assertEquals("dir", VaultSelectionPath.parentDirectory("dir", entries))
    }

    @Test
    fun joinUnderParent() {
        assertEquals("dir/snippet.json", VaultSelectionPath.join("dir", "snippet.json"))
        assertEquals("snippet.json", VaultSelectionPath.join("", "snippet.json"))
    }

    @Test
    fun resolveSingleSegmentUnderSelectedDirectory() {
        val entries = listOf(
            VaultEntry("Work", "Work", true, 0),
            VaultEntry("Work/a.json", "a.json", false, 0),
        )
        assertEquals(
            "Work/new.json",
            VaultSelectionPath.resolveEntryPath("new.json", "Work", "", entries),
        )
        assertEquals("drafts/x.json", VaultSelectionPath.resolveEntryPath("drafts/x.json", "Work", "", entries))
    }
}
