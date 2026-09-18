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
    fun runTestActionEnabledMatchesGuards() {
        assertFalse(RegexWiringPresentation.runTestActionEnabled("", running = false))
        assertFalse(RegexWiringPresentation.runTestActionEnabled("a", running = true))
        assertTrue(RegexWiringPresentation.runTestActionEnabled("a", running = false))
        assertTrue(RegexWiringPresentation.cancelTestActionEnabled(running = true))
        assertFalse(RegexWiringPresentation.cancelTestActionEnabled(running = false))
    }

    @Test
    fun runMatchSuccess() {
        val outcome = RegexWiringPresentation.runMatch("a+", "aaa", RegexOptions())
        assertTrue(outcome is RegexWiringPresentation.MatchOutcome.Success)
    }
}
