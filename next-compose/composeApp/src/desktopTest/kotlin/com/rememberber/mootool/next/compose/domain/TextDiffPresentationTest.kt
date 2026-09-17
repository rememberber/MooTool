package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextDiffPresentationTest {
    @Test
    fun debounceMatchesScreen() {
        assertEquals(160L, TextDiffPresentation.AUTO_COMPARE_DEBOUNCE_MS)
    }

    @Test
    fun nextNavIndexMatchesDiffScreenNavigate() {
        assertEquals(0, TextDiffPresentation.nextNavIndex(-1, 1, 3))
        assertEquals(1, TextDiffPresentation.nextNavIndex(0, 1, 3))
        assertEquals(-1, TextDiffPresentation.nextNavIndex(0, 1, 0))
    }

    @Test
    fun navigationAndCompareGuards() {
        assertFalse(TextDiffPresentation.canNavigateDiffs(0))
        assertTrue(TextDiffPresentation.canNavigateDiffs(2))
        assertFalse(TextDiffPresentation.canManualCompare("", ""))
        assertTrue(TextDiffPresentation.canManualCompare("a", ""))
    }

    @Test
    fun runCompareUsesDiffEngine() {
        val result = TextDiffPresentation.runCompare("a", "b", ignoreWhitespace = false)
        assertTrue(result.segments.isNotEmpty())
    }

    @Test
    fun runReadImportFileReturnsContent() {
        val file = kotlin.io.path.createTempFile(suffix = ".txt").toFile()
        try {
            file.writeText("line-a\nline-b")
            val outcome = TextDiffPresentation.runReadImportFile(file)
            assertTrue(outcome is TextDiffPresentation.ImportOutcome.Success)
            assertEquals("line-a\nline-b", (outcome as TextDiffPresentation.ImportOutcome.Success).content)
        } finally {
            file.delete()
        }
    }

    @Test
    fun runReadImportFileMissingFileFails() {
        val file = java.io.File("/nonexistent/mootool-diff-import-${System.nanoTime()}.txt")
        val outcome = TextDiffPresentation.runReadImportFile(file)
        assertTrue(outcome is TextDiffPresentation.ImportOutcome.Failure)
    }
}
