package com.rememberber.mootool.next.compose.features.json

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonToolbarFormatPolicyTest {
    @Test
    fun quick_format_spaces_match_electron_toolbar() {
        assertEquals(2, JsonToolbarFormatPolicy.QUICK_FORMAT_SPACES)
    }
}
