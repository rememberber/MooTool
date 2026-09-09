package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.services.RegexWorkerClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RegexEngineTest {
    @Test
    fun returnsMatchPositionsAndCaptureGroups() {
        val matches = RegexEngine.match(
            "(moo)(\\d+)",
            "moo1 moo22",
            RegexOptions(global = true)
        )
        assertEquals(2, matches.size)
        assertEquals(RegexMatch(0, "moo1", listOf("moo", "1")), matches[0])
        assertEquals(RegexMatch(5, "moo22", listOf("moo", "22")), matches[1])
    }

    @Test
    fun handlesZeroWidthGlobalExpressionsWithoutLooping() {
        val matches = RegexEngine.match("(?=a)", "aa", RegexOptions(global = true))
        assertEquals(2, matches.size)
        assertEquals(0, matches[0].index)
        assertEquals(1, matches[1].index)
        assertEquals("", matches[0].value)
    }

    @Test
    fun keepsJavaCommonPatternCatalogAndNamedGroups() {
        assertEquals(21, RegexEngine.commonRegexes.size)
        val named = RegexEngine.match("(?<word>moo)(?<n>\\d+)", "moo9", RegexOptions(global = false)).single()
        assertEquals("moo", named.named["word"])
        assertEquals("9", named.named["n"])
        val emoji = RegexEngine.match("😀", "hi 😀", RegexOptions())
        assertEquals(1, emoji.size)
        assertEquals(3, emoji[0].index)
    }

    @Test
    fun rejectsInvalidPatternsAndHonorsDotAll() {
        assertFailsWith<RegexException> { RegexEngine.match("(", "a", RegexOptions()) }
        val withoutDotAll = RegexEngine.match("a.b", "a\nb", RegexOptions(dotAll = false))
        assertTrue(withoutDotAll.isEmpty())
        val withDotAll = RegexEngine.match("a.b", "a\nb", RegexOptions(dotAll = true))
        assertEquals("a\nb", withDotAll.single().value)
    }

    @Test
    fun emptyPatternDoesNotMatchAndHtmlIdLookbehindWorks() {
        assertTrue(RegexEngine.match("", "abc", RegexOptions(global = true)).isEmpty())
        val html = RegexEngine.match(
            RegexEngine.commonRegexes.first { it.id == "htmlId" }.pattern,
            """<div id="panel-1">""",
            RegexOptions()
        ).single()
        assertEquals("panel-1", html.value)
        val ignoreCase = RegexEngine.match("moo", "MOO", RegexOptions(ignoreCase = true, global = false)).single()
        assertEquals("MOO", ignoreCase.value)
    }
}

class RegexWorkerClientTest {
    @Test
    fun workerMatchesAndKillsCatastrophicBacktracking() {
        val client = RegexWorkerClient(timeoutMs = 800)
        val ok = client.match("(moo)(\\d+)", "moo1 moo22", RegexOptions(global = true))
        assertTrue(ok.ok, "worker failed: code=${ok.code} error=${ok.error}")
        assertEquals(2, ok.matches.size)
        assertEquals(RegexEngine.ENGINE_NAME, ok.engine)
        val started = System.currentTimeMillis()
        val timedOut = client.match("(.*a){28}", "a".repeat(28), RegexOptions())
        val elapsed = System.currentTimeMillis() - started
        assertEquals("timeout", timedOut.code)
        assertTrue(elapsed < 8_000, "worker was not killed in time: ${elapsed}ms")
        val invalid = client.match("(", "a", RegexOptions())
        assertEquals("invalid", invalid.code)
        assertTrue(!invalid.ok)
    }
}
