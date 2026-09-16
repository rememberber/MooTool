package com.rememberber.mootool.next.compose.features.quicknote

import kotlin.test.Test
import kotlin.test.assertTrue

class QuickNoteDocumentInfoTest {
    @Test
    fun formatTimestampRendersIsoInstant() {
        val formatted = formatQuickNoteTimestamp("2024-06-01T12:00:00Z")
        assertTrue(formatted.contains("2024"))
        assertTrue(formatted.contains(":"))
    }
}
