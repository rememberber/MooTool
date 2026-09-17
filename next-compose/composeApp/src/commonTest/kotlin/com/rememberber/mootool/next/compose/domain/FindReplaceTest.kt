package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FindReplaceTest {
    @Test
    fun findsCaseInsensitiveAndWholeWordMatches() {
        val text = "JSON json Jsonify"
        val all = FindReplace.findAll(text, "json", FindReplaceOptions())
        assertEquals(3, all.size)
        val whole = FindReplace.findAll(text, "json", FindReplaceOptions(wholeWord = true))
        assertEquals(2, whole.size)
    }

    @Test
    fun replaceAllIsOneTransactionCount() {
        val (next, count) = FindReplace.replaceAll("a-a-a", "a", "b", FindReplaceOptions())
        assertEquals("b-b-b", next)
        assertEquals(3, count)
    }

    @Test
    fun replaceCurrent_prefers_exact_selection_match() {
        val (next, match) = FindReplace.replaceCurrent(
            "aa aa",
            "aa",
            "XX",
            FindReplaceOptions(),
            fromIndex = 2,
            selectionStart = 0,
            selectionEnd = 2,
        )
        assertEquals("XX aa", next)
        assertEquals(0, match?.start)
        assertEquals(2, match?.end)
    }

    /** 对照 Electron `findReplace.test.ts`。 */
    @Test
    fun mirrorsElectronFindReplaceVitestBasics() {
        val options = FindReplaceOptions()
        assertEquals(
            listOf(FindMatch(0, 3), FindMatch(4, 7), FindMatch(8, 11)),
            FindReplace.findAll("Foo foo FOO", "foo", options),
        )
        assertEquals(
            listOf(FindMatch(0, 3)),
            FindReplace.findAll("Foo foo FOO", "Foo", FindReplaceOptions(matchCase = true)),
        )
        assertEquals(
            listOf(FindMatch(0, 3), FindMatch(13, 16)),
            FindReplace.findAll("cat category cat", "cat", FindReplaceOptions(wholeWord = true)),
        )
        assertEquals(
            listOf(FindMatch(1, 2), FindMatch(4, 6), FindMatch(8, 9)),
            FindReplace.findAll("a1 b22 c3", "\\d+", FindReplaceOptions(regex = true)),
        )
        assertTrue(FindReplace.findAll("abc", "(", FindReplaceOptions(regex = true)).isEmpty())

        val wrapOptions = FindReplaceOptions()
        assertEquals(FindMatch(0, 3), FindReplace.findNext("one two one", "one", wrapOptions, 0, true))
        assertEquals(FindMatch(8, 11), FindReplace.findNext("one two one", "one", wrapOptions, 1, true))
        assertEquals(FindMatch(0, 3), FindReplace.findNext("one two one", "one", wrapOptions, 11, true))
        assertEquals(FindMatch(0, 3), FindReplace.findNext("one two one", "one", wrapOptions, 8, false))
        assertEquals(FindMatch(8, 11), FindReplace.findNext("one two one", "one", wrapOptions, 0, false))

        val (replacedOnce, onceMatch) = FindReplace.replaceCurrent(
            "hello world",
            "world",
            "moo",
            wrapOptions,
            fromIndex = 6,
            selectionStart = 6,
            selectionEnd = 11,
        )
        assertEquals("hello moo", replacedOnce)
        assertEquals(FindMatch(6, 9), onceMatch)

        val (replacedNext, _) = FindReplace.replaceCurrent(
            "foo bar foo",
            "foo",
            "x",
            wrapOptions,
            fromIndex = 0,
            selectionStart = 0,
            selectionEnd = 0,
        )
        assertEquals("x bar foo", replacedNext)

        val (captureAll, captureCount) = FindReplace.replaceAll(
            "a1 b2",
            "(\\w)(\\d)",
            "\$2\$1",
            FindReplaceOptions(regex = true),
        )
        assertEquals("1a 2b", captureAll)
        assertEquals(2, captureCount)

        assertEquals(
            listOf(FindMatch(1, 2), FindMatch(3, 4)),
            FindReplace.findAll("a\nb\nc", "\\n", FindReplaceOptions(regex = true)),
        )
        val (newlineSplit, newlineCount) = FindReplace.replaceAll(
            "a,b,c",
            ",",
            "\\n",
            FindReplaceOptions(regex = true),
        )
        assertEquals("a\nb\nc", newlineSplit)
        assertEquals(2, newlineCount)
        val (literalBackslashN, _) = FindReplace.replaceAll(
            "a,b",
            ",",
            "\\n",
            FindReplaceOptions(),
        )
        assertEquals("a\\nb", literalBackslashN)
        val (groupNewline, groupCount) = FindReplace.replaceAll(
            "a1",
            "(\\w)(\\d)",
            "\$2\\n\$1",
            FindReplaceOptions(regex = true),
        )
        assertEquals("1\na", groupNewline)
        assertEquals(1, groupCount)
        val (escapedBackslash, escapedCount) = FindReplace.replaceAll(
            "a",
            "a",
            "\\\\n",
            FindReplaceOptions(regex = true),
        )
        assertEquals("\\n", escapedBackslash)
        assertEquals(1, escapedCount)
    }
}
