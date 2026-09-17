package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UaRegexTimeWiringPresentationTest {
    @Test
    fun uaParseRequiresSource() {
        assertFalse(UaWiringPresentation.canParse(""))
        assertTrue(UaWiringPresentation.canParse("Mozilla/5.0"))
    }

    @Test
    fun regexTestGuards() {
        assertFalse(RegexWiringPresentation.canRunTest("", running = false))
        assertFalse(RegexWiringPresentation.canRunTest("a", running = true))
        assertTrue(RegexWiringPresentation.showCancel(running = true))
    }

    @Test
    fun timeConvertGuards() {
        assertFalse(TimeWiringPresentation.canConvertTimestamp(""))
        assertTrue(TimeWiringPresentation.canConvertLocal("2026-01-01"))
    }
}
