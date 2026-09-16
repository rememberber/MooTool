package com.rememberber.mootool.next.compose.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TextFieldFindSeedTest {
    @Test
    fun selectedTextForFind_returns_non_empty_slice() {
        val value = TextFieldValue("127.0.0.1 localhost", TextRange(0, 9))
        assertEquals("127.0.0.1", value.selectedTextForFind())
    }

    @Test
    fun selectedTextForFind_null_when_collapsed() {
        assertNull(TextFieldValue("abc", TextRange(2)).selectedTextForFind())
    }
}
