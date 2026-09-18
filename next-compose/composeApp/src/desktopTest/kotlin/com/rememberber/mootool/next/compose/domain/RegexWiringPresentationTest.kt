package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegexWiringPresentationTest {
    @Test
    fun shouldToastWorkerErrorSkipsCancelled() {
        assertFalse(RegexWiringPresentation.shouldToastWorkerError("cancelled"))
        assertTrue(RegexWiringPresentation.shouldToastWorkerError("timeout"))
    }

    @Test
    fun runMatchSuccess() {
        val outcome = RegexWiringPresentation.runMatch("a+", "aaa", RegexOptions())
        assertTrue(outcome is RegexWiringPresentation.MatchOutcome.Success)
    }
}
