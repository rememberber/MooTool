package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UaRegexTimeWiringPresentationTest {
    @Test
    fun uaParseRequiresSource() {
        assertFalse(UaWiringPresentation.canParse(""))
        assertTrue(UaWiringPresentation.canParse("Mozilla/5.0"))
    }

    @Test
    fun uaCopyRequiresResultText() {
        assertFalse(UaWiringPresentation.canCopyResult(""))
        assertTrue(UaWiringPresentation.canCopyResult("{}"))
    }

    @Test
    fun regexTestGuards() {
        assertFalse(RegexWiringPresentation.canRunTest("", running = false))
        assertFalse(RegexWiringPresentation.canRunTest("a", running = true))
        assertTrue(RegexWiringPresentation.showCancel(running = true))
    }

    @Test
    fun runMatchUsesRegexEngine() {
        val outcome = RegexWiringPresentation.runMatch("moo", "mootool", RegexOptions())
        assertTrue(outcome is RegexWiringPresentation.MatchOutcome.Success)
        assertTrue((outcome as RegexWiringPresentation.MatchOutcome.Success).matches.isNotEmpty())
    }

    @Test
    fun runMatchAlignsWithRegexEngineDirectMatch() {
        val options = RegexOptions(global = true, ignoreCase = true)
        val outcome = RegexWiringPresentation.runMatch("(moo)(\\d+)", "MOO1 moo22", options)
        val direct = RegexEngine.match("(moo)(\\d+)", "MOO1 moo22", options)
        assertTrue(outcome is RegexWiringPresentation.MatchOutcome.Success)
        assertEquals(direct, (outcome as RegexWiringPresentation.MatchOutcome.Success).matches)
    }

    @Test
    fun runMatchSurfacesInvalidPattern() {
        val outcome = RegexWiringPresentation.runMatch("(", "a", RegexOptions())
        assertTrue(outcome is RegexWiringPresentation.MatchOutcome.Failure)
        assertTrue((outcome as RegexWiringPresentation.MatchOutcome.Failure).error is RegexException)
    }

    @Test
    fun runParseUsesUaEngine() {
        val outcome = UaWiringPresentation.runParse(UaEngine.presets.first().second)
        assertTrue(outcome is UaWiringPresentation.ParseOutcome.Success)
    }

}
