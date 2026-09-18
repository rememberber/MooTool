package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UaWiringPresentationTest {
    @Test
    fun canParseRequiresSource() {
        assertFalse(UaWiringPresentation.canParse(""))
        assertTrue(UaWiringPresentation.canParse("Mozilla/5.0"))
        assertFalse(UaWiringPresentation.parseActionEnabled(""))
        assertTrue(UaWiringPresentation.parseActionEnabled("Mozilla/5.0"))
        assertFalse(UaWiringPresentation.copyResultActionEnabled(""))
        assertTrue(UaWiringPresentation.copyResultActionEnabled("{}"))
    }

    @Test
    fun runParseSuccess() {
        val outcome = UaWiringPresentation.runParse("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        assertTrue(outcome is UaWiringPresentation.ParseOutcome.Success)
    }

    @Test
    fun shouldToastParseFailure() {
        assertTrue(UaWiringPresentation.shouldToastParseFailure(UaException("empty", "")))
    }
}
