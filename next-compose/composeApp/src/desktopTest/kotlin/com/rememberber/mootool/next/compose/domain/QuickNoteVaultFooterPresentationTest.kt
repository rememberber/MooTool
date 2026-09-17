package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuickNoteVaultFooterPresentationTest {
    @Test
    fun effectivePathPrefersTreeSelection() {
        assertEquals("notes/a.md", QuickNoteVaultFooterPresentation.effectivePath("notes/a.md", "notes/b.md"))
    }

    @Test
    fun footerDirtyOnlyWhenOpenFileDirty() {
        assertTrue(
            QuickNoteVaultFooterPresentation.footerDirty("a.md", "a.md", documentDirty = true),
        )
        assertFalse(
            QuickNoteVaultFooterPresentation.footerDirty("a.md", "b.md", documentDirty = true),
        )
    }

    @Test
    fun gitFlushSkipsWhenNoFileOrClean() {
        assertTrue(QuickNoteVaultFooterPresentation.gitFlushSkipsWhenClean("", documentDirty = true))
        assertTrue(QuickNoteVaultFooterPresentation.gitFlushSkipsWhenClean("a.md", documentDirty = false))
        assertFalse(QuickNoteVaultFooterPresentation.gitFlushSkipsWhenClean("a.md", documentDirty = true))
    }

    @Test
    fun gitUntitledBlockKey() {
        assertEquals(
            "git.flush.untitled",
            QuickNoteVaultFooterPresentation.gitUntitledBlockKey("", "draft", sampleText = "sample"),
        )
        assertEquals(null, QuickNoteVaultFooterPresentation.gitUntitledBlockKey("", "sample", sampleText = "sample"))
        assertEquals(null, QuickNoteVaultFooterPresentation.gitUntitledBlockKey("a.md", "draft", sampleText = "sample"))
    }
}
