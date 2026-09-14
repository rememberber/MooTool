package com.rememberber.mootool.next.compose.editor

import com.rememberber.mootool.next.compose.domain.ColumnEditEngine
import com.rememberber.mootool.next.compose.domain.ColumnRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorBufferColumnEditTest {
    @Test
    fun columnInsertIsOneUndoStep() {
        val buffer = EditorBuffer("")
        buffer.setText("aa\nbb", recordUndo = false)
        buffer.replaceAllText(ColumnEditEngine.replace(buffer.text, ColumnRange(0, 1, 0, 0), "x", buffer.area.tabSize))
        assertEquals("xaa\nxbb", buffer.text)
        assertTrue(buffer.canUndo())
        buffer.undo()
        assertEquals("aa\nbb", buffer.text)
    }
}
