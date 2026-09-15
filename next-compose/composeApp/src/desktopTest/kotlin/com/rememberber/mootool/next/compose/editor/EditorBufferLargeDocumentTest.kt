package com.rememberber.mootool.next.compose.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EditorBufferLargeDocumentTest {
    @Test
    fun holdsFiveMebibytesOfMixedTextAndExposesIme() {
        val unit = "汉字ABC😀\n"
        val repeats = (EditorLimits.LARGE_DOCUMENT_BYTES / EditorLimits.utf8Size(unit)) + 1
        val text = unit.repeat(repeats)
        assertTrue(EditorLimits.exceedsLargeDocument(text))
        val buffer = EditorBuffer("")
        buffer.setText(text, recordUndo = false)
        assertEquals(text.length, buffer.text.length)
        assertNotNull(buffer.area.getInputMethodRequests())
    }
}
