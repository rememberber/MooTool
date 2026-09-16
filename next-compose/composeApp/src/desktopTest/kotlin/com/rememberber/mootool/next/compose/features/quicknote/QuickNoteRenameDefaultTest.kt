package com.rememberber.mootool.next.compose.features.quicknote

import kotlin.test.Test
import kotlin.test.assertEquals

class QuickNoteRenameDefaultTest {
    @Test
    fun fileUsesMetadataTitleWhenPresent() {
        assertEquals(
            "My Title",
            quickNoteRenameDefault("Work/note.md", isDirectory = false, metadataTitle = "My Title"),
        )
    }

    @Test
    fun fileFallsBackToFilenameWithoutExtension() {
        assertEquals(
            "note",
            quickNoteRenameDefault("Work/note.md", isDirectory = false, metadataTitle = ""),
        )
    }

    @Test
    fun directoryUsesLeafName() {
        assertEquals("Nested", quickNoteRenameDefault("Work/Nested", isDirectory = true))
    }
}
