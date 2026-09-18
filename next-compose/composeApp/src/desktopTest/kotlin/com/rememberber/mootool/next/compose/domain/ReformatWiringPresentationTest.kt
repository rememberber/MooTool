package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReformatWiringPresentationTest {
    @Test
    fun shouldToastFormatFailure() {
        assertTrue(ReformatWiringPresentation.shouldToastFormatFailure(IllegalStateException()))
    }

    @Test
    fun canRunFormatRequiresInputAndNotBusy() {
        assertFalse(ReformatWiringPresentation.canRunFormat(busy = true, inputNotBlank = true))
        assertFalse(ReformatWiringPresentation.canRunFormat(busy = false, inputNotBlank = false))
        assertTrue(ReformatWiringPresentation.canRunFormat(busy = false, inputNotBlank = true))
    }

    @Test
    fun defaultSaveFileNameMatchesSaveResult() {
        assertEquals(
            "App.java",
            ReformatWiringPresentation.defaultSaveFileName("App.java", ReformatType.Java),
        )
        assertEquals(
            "formatted.conf",
            ReformatWiringPresentation.defaultSaveFileName("", ReformatType.Nginx),
        )
    }

    @Test
    fun runFormatUsesReformatEngine() {
        val outcome = ReformatWiringPresentation.runFormat(
            "server { listen 80; }",
            ReformatType.Nginx,
            2,
        )
        assertTrue(outcome is ReformatWiringPresentation.FormatRunOutcome.Success)
        outcome as ReformatWiringPresentation.FormatRunOutcome.Success
        assertTrue(outcome.output.contains("listen 80"))
    }

    @Test
    fun formatErrorMessageUsesLocatedKeyWhenLinePresent() {
        val message = ReformatWiringPresentation.formatErrorMessage(
            ReformatException("bad", "xml", line = 2, column = 3),
        )
        assertEquals("reformat.error.located", message.key)
        assertEquals("2", message.params["line"])
    }

    @Test
    fun inferTypeFromFileNameMatchesExtensions() {
        assertEquals(ReformatType.Java, ReformatWiringPresentation.inferTypeFromFileName("App.java"))
        assertEquals(ReformatType.Nginx, ReformatWiringPresentation.inferTypeFromFileName("site.conf"))
        assertEquals(ReformatType.Html, ReformatWiringPresentation.inferTypeFromFileName("index.htm"))
        assertEquals(null, ReformatWiringPresentation.inferTypeFromFileName("readme.txt"))
    }

    @Test
    fun shouldToastFailures() {
        assertTrue(ReformatWiringPresentation.shouldToastFormatFailure(IllegalStateException()))
        assertTrue(ReformatWiringPresentation.shouldToastIoFailure(IllegalStateException()))
    }

    @Test
    fun runReadSourceFileReturnsContentAndInferredType() {
        val file = kotlin.io.path.createTempFile(suffix = ".xml").toFile()
        try {
            file.writeText("<root/>")
            val outcome = ReformatWiringPresentation.runReadSourceFile(file)
            assertTrue(outcome is ReformatWiringPresentation.ReadSourceOutcome.Success)
            outcome as ReformatWiringPresentation.ReadSourceOutcome.Success
            assertEquals("<root/>", outcome.content)
            assertEquals(ReformatType.Xml, outcome.inferredType)
        } finally {
            file.delete()
        }
    }
}
