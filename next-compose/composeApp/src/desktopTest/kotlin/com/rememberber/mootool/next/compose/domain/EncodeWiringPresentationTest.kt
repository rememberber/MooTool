package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EncodeWiringPresentationTest {
    @Test
    fun convertRequiresNonBlankSource() {
        assertFalse(EncodeWiringPresentation.canConvert("   "))
        assertTrue(EncodeWiringPresentation.canConvert("abc"))
    }
}
