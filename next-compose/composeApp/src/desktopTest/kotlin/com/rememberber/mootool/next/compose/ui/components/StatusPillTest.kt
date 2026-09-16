package com.rememberber.mootool.next.compose.ui.components

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class StatusPillTest {
    @Test
    fun validAndErrorFillsMatchElectron() {
        assertEquals(Color(0xFFE8F3EE), statusPillFill(MooStatusKind.Valid, dark = false))
        assertEquals(Color(0xFF20352C), statusPillFill(MooStatusKind.Valid, dark = true))
        assertEquals(Color(0xFFF8EBE8), statusPillFill(MooStatusKind.Error, dark = false))
        assertEquals(Color(0xFF3A2523), statusPillFill(MooStatusKind.Error, dark = true))
        assertEquals(Color(0xFF35765B), statusPillContent(MooStatusKind.Valid, dark = false, fallback = Color.Black))
        assertEquals(Color(0xFFB25448), statusPillContent(MooStatusKind.Error, dark = false, fallback = Color.Black))
    }
}
