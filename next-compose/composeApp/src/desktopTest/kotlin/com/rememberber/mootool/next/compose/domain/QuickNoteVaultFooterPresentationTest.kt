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
}
