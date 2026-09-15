package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GitDiffSelectionTest {
    @Test
    fun labelsRenameAndFallsBackToFirstFile() {
        val first = GitFileDiff(path = "a.md", status = "M", before = "old-a", after = "new-a")
        val renamed = GitFileDiff(path = "c.md", originalPath = "b.md", status = "R100", before = "old-b", after = "new-c")
        val files = listOf(first, renamed)
        assertEquals("M  a.md", GitDiffSelection.fileLabel(first))
        assertEquals("R100  b.md → c.md", GitDiffSelection.fileLabel(renamed))
        assertEquals("M  note.md", GitDiffSelection.fileLabel(GitFileDiff(path = "note.md")))
        assertEquals("c.md", GitDiffSelection.selected(files, "c.md")?.path)
        assertEquals("a.md", GitDiffSelection.selected(files, "missing.md")?.path)
        assertNull(GitDiffSelection.selected(emptyList(), "a.md"))
    }
}
