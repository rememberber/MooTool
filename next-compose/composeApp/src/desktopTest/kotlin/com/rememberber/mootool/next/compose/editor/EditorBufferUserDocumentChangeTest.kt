package com.rememberber.mootool.next.compose.editor

import kotlin.test.Test
import kotlin.test.assertEquals

class EditorBufferUserDocumentChangeTest {
    @Test
    fun setText_does_not_fire_user_document_change() {
        val buffer = EditorBuffer("")
        var count = 0
        buffer.onUserDocumentChange = { count += 1 }
        buffer.setText("{\"a\":1}", recordUndo = false)
        assertEquals(0, count)
    }

    @Test
    fun replaceRange_fires_user_document_change() {
        val buffer = EditorBuffer("{\"a\":1}")
        var count = 0
        buffer.onUserDocumentChange = { count += 1 }
        buffer.replaceRange(1, 1, "x")
        assertEquals(1, count)
    }
}
