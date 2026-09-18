package com.rememberber.mootool.next.compose.domain

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuickNoteWiringPresentationTest {
    @Test
    fun exportEnabledRequiresOpenFile() {
        assertFalse(QuickNoteWiringPresentation.exportEnabled(""))
        assertTrue(QuickNoteWiringPresentation.exportEnabled("notes/a.md"))
    }

    @Test
    fun runWriteExportTextRoundTrip() {
        val file = Files.createTempFile("quicknote-export-", ".txt")
        try {
            val outcome = QuickNoteWiringPresentation.runWriteExportText(file, "hello 580")
            assertTrue(outcome is QuickNoteWiringPresentation.WriteExportOutcome.Success)
            assertEquals("hello 580", Files.readString(file))
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun runWriteExportTextMissingParentFails() {
        val file = Files.createTempDirectory("qn-parent-").resolve("missing/out.txt")
        val outcome = QuickNoteWiringPresentation.runWriteExportText(file, "x")
        assertTrue(outcome is QuickNoteWiringPresentation.WriteExportOutcome.Failure)
    }

    @Test
    fun operationFailureMessagePrefersThrowableMessage() {
        val t: (String) -> String = { key -> "fb:$key" }
        assertEquals("disk", QuickNoteWiringPresentation.operationFailureMessage("quickNote.saveFailed", IllegalStateException("disk"), t))
        assertEquals("fb:quickNote.saveFailed", QuickNoteWiringPresentation.operationFailureMessage("quickNote.saveFailed", IllegalStateException(), t))
    }

    @Test
    fun shouldToastFailures() {
        assertTrue(QuickNoteWiringPresentation.shouldToastOperationFailure())
        assertTrue(QuickNoteWiringPresentation.shouldToastSaveFailure())
        assertTrue(QuickNoteWiringPresentation.shouldToastIoFailure(IllegalStateException()))
    }
}
