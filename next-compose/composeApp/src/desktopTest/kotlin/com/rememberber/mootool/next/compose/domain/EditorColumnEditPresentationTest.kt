package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertTrue

class EditorColumnEditPresentationTest {
    @Test
    fun imeEvidenceSampleNamesMatchPrepScript() {
        assertTrue(EditorColumnEditPresentation.JSON_IME_SAMPLE.endsWith(".json"))
        assertTrue(EditorColumnEditPresentation.QUICK_NOTE_IME_SAMPLE.endsWith(".md"))
        assertTrue(EditorColumnEditPresentation.commandPaletteKeywords.any { it == "列编辑" })
    }
}
