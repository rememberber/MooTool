package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JsonVaultFooterPresentationTest {
    @Test
    fun effectivePathPrefersSelectionThenOpenFile() {
        assertEquals("a.json", JsonVaultFooterPresentation.effectivePath("a.json", "b.json"))
        assertEquals("open.json", JsonVaultFooterPresentation.effectivePath("", "open.json"))
    }

    @Test
    fun duplicateOnlyForFiles() {
        assertTrue(JsonVaultFooterPresentation.canDuplicate(isDirectory = false))
        assertFalse(JsonVaultFooterPresentation.canDuplicate(isDirectory = true))
        assertFalse(JsonVaultFooterPresentation.canDuplicate(isDirectory = null))
    }

    @Test
    fun renameMatchesDuplicateRules() {
        assertTrue(JsonVaultFooterPresentation.canRename(isDirectory = false))
        assertFalse(JsonVaultFooterPresentation.canRename(isDirectory = true))
        assertFalse(JsonVaultFooterPresentation.canShowVaultActions("dir/", isDirectory = true))
        assertTrue(JsonVaultFooterPresentation.canShowVaultActions("a.json", isDirectory = false))
    }

    @Test
    fun gitFlushSkipsWhenNoFileOrClean() {
        assertTrue(JsonVaultFooterPresentation.gitFlushSkipsWhenClean("", editorDirty = true))
        assertTrue(JsonVaultFooterPresentation.gitFlushSkipsWhenClean("a.json", editorDirty = false))
        assertFalse(JsonVaultFooterPresentation.gitFlushSkipsWhenClean("a.json", editorDirty = true))
    }

    @Test
    fun gitUntitledBlockWhenDraftWithoutVaultFile() {
        assertEquals(
            "git.flush.untitled",
            JsonVaultFooterPresentation.gitUntitledBlockKey("", editorHasUserDraft = true),
        )
        assertNull(JsonVaultFooterPresentation.gitUntitledBlockKey("a.json", editorHasUserDraft = true))
        assertNull(JsonVaultFooterPresentation.gitUntitledBlockKey("", editorHasUserDraft = false))
    }
}
