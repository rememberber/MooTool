package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditorColumnEditPresentationTest {
    @Test
    fun columnNoticeKeyWrapWhenLatchAndWrap() {
        assertEquals("quickNote.columnEdit.wrap", EditorColumnEditPresentation.columnNoticeKey(columnLatch = true, wrap = true))
        assertEquals("quickNote.columnEdit.hint", EditorColumnEditPresentation.columnNoticeKey(columnLatch = true, wrap = false))
        assertEquals(null, EditorColumnEditPresentation.columnNoticeKey(columnLatch = false, wrap = true))
    }

    @Test
    fun columnGestureMatchesElectronAltOrLatch() {
        assertTrue(EditorColumnEditPresentation.columnGestureActive(altDown = true, columnLatch = false))
        assertTrue(EditorColumnEditPresentation.columnGestureActive(altDown = false, columnLatch = true))
        assertFalse(EditorColumnEditPresentation.columnGestureActive(altDown = false, columnLatch = false))
        assertTrue(EditorColumnEditPresentation.columnDragWithoutAlt(columnLatch = true))
    }

    @Test
    fun imeEvidenceSampleNamesMatchPrepScript() {
        assertTrue(EditorColumnEditPresentation.JSON_IME_SAMPLE.endsWith(".json"))
        assertTrue(EditorColumnEditPresentation.QUICK_NOTE_IME_SAMPLE.endsWith(".md"))
        assertTrue(EditorColumnEditPresentation.commandPaletteKeywords.any { it == "列编辑" })
        assertTrue(
            EditorColumnEditPresentation.matchesEvidenceJsonImeSample(
                EditorColumnEditPresentation.evidenceJsonImeSampleBody,
            ),
        )
        assertTrue(
            EditorColumnEditPresentation.matchesEvidenceWalkthroughPath(
                EditorColumnEditPresentation.JSON_IME_SAMPLE,
            ),
        )
    }
}
