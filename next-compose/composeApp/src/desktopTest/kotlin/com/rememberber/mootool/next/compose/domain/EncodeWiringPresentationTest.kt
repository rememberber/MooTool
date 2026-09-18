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

    @Test
    fun convertActionEnabledMatchesGuards() {
        assertFalse(EncodeWiringPresentation.forwardConvertActionEnabled(""))
        assertTrue(EncodeWiringPresentation.forwardConvertActionEnabled("x"))
        assertFalse(EncodeWiringPresentation.reverseConvertActionEnabled("  "))
        assertTrue(EncodeWiringPresentation.reverseConvertActionEnabled("y"))
    }

    @Test
    fun runConvertUsesEncodeEngine() {
        val outcome = EncodeWiringPresentation.runConvert(
            EncodeTab.Url,
            forward = true,
            input = "a b",
            charset = UrlCharset.Utf8,
            asciiFormat = AsciiFormat.Decimal,
        )
        assertTrue(outcome is EncodeWiringPresentation.ConvertOutcome.Success)
        assertTrue((outcome as EncodeWiringPresentation.ConvertOutcome.Success).output.contains("a"))
    }

    @Test
    fun shouldToastConvertFailure() {
        assertTrue(EncodeWiringPresentation.shouldToastConvertFailure(IllegalStateException()))
    }
}
