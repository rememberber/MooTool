package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VaultConflictPresentationTest {
    @Test
    fun reloadHiddenWhenFileDeletedExternally() {
        assertFalse(VaultConflictPresentation.showReloadAction(deleted = true))
        assertTrue(VaultConflictPresentation.showReloadAction(deleted = false))
    }

    @Test
    fun hintKeyFollowsDeletedState() {
        assertEquals("vault.conflict.hintDeleted", VaultConflictPresentation.hintMessageKey(deleted = true))
        assertEquals("vault.conflict.hint", VaultConflictPresentation.hintMessageKey(deleted = false))
    }

    @Test
    fun previewTextPrefersDeletedMessageThenDiff() {
        assertEquals(
            "deleted",
            VaultConflictPresentation.previewText(
                deleted = true,
                deletedMessage = "deleted",
                noDiffMessage = "none",
                unifiedDiff = "diff",
            ),
        )
        assertEquals(
            "diff",
            VaultConflictPresentation.previewText(
                deleted = false,
                deletedMessage = "deleted",
                noDiffMessage = "none",
                unifiedDiff = "diff",
            ),
        )
        assertEquals(
            "none",
            VaultConflictPresentation.previewText(
                deleted = false,
                deletedMessage = "deleted",
                noDiffMessage = "none",
                unifiedDiff = "",
            ),
        )
    }
}
