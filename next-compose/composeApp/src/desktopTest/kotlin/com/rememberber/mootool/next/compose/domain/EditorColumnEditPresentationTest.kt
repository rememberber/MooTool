package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorColumnEditPresentationTest {
    @Test
    fun columnNoticeKeyWrapWhenLatchAndWrap() {
        assertEquals("quickNote.columnEdit.wrap", EditorColumnEditPresentation.columnNoticeKey(columnLatch = true, wrap = true))
        assertEquals(null, EditorColumnEditPresentation.columnNoticeKey(columnLatch = false, wrap = true))
    }

    @Test
    fun imeEvidenceSampleNamesMatchPrepScript() {
        assertTrue(EditorColumnEditPresentation.JSON_IME_SAMPLE.endsWith(".json"))
        assertTrue(EditorColumnEditPresentation.QUICK_NOTE_IME_SAMPLE.endsWith(".md"))
        assertTrue(EditorColumnEditPresentation.commandPaletteKeywords.any { it == "列编辑" })
    }
}
