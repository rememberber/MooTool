package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class QuickReplaceEngineTest {
    @Test
    fun normalizesLineOrientedValues() {
        assertEquals(" a \n b ", QuickReplaceEngine.run(" a \n\n b ", QuickReplaceAction.RemoveBlankLines))
        assertEquals("a\t2\nb\t1", QuickReplaceEngine.run("a\na\nb", QuickReplaceAction.DeduplicateWithCount))
        assertEquals("a\nb", QuickReplaceEngine.run("b\na", QuickReplaceAction.SortAscending))
        assertEquals("a\nb", QuickReplaceEngine.run(" a \n b ", QuickReplaceAction.Trim))
        assertEquals("ab", QuickReplaceEngine.run("a\tb", QuickReplaceAction.RemoveTabs))
    }

    @Test
    fun convertsIdentifiersAndNumberFormats() {
        assertEquals("helloWorld", QuickReplaceEngine.run("hello_world", QuickReplaceAction.UnderscoreToCamel))
        assertEquals("hello_world", QuickReplaceEngine.run("helloWorld", QuickReplaceAction.CamelToUnderscore))
        assertEquals("1250", QuickReplaceEngine.run("1.25e3", QuickReplaceAction.ScientificToNormal))
        assertEquals("1,234,567.5", QuickReplaceEngine.run("1234567.5", QuickReplaceAction.NormalToThousands))
        assertEquals("1234567.5", QuickReplaceEngine.run("1,234,567.5", QuickReplaceAction.ThousandsToNormal))
        assertEquals("1.2345675e+6", QuickReplaceEngine.run("1234567.5", QuickReplaceAction.NormalToScientific))
    }

    @Test
    fun convertsListDelimitersAndEscapesText() {
        assertEquals("'a','b'", QuickReplaceEngine.run("a\nb", QuickReplaceAction.LinesToSingleQuoted))
        assertEquals("a\nb", QuickReplaceEngine.run("\"a\", b", QuickReplaceAction.CommaToLines))
        val escaped = QuickReplaceEngine.run("a\nb", QuickReplaceAction.Escape)
        assertEquals("a\\nb", escaped)
        assertEquals("a\nb", QuickReplaceEngine.run(escaped, QuickReplaceAction.Unescape))
        assertFails { QuickReplaceEngine.run("\\", QuickReplaceAction.Unescape) }
        val (chars, words, lines) = QuickReplaceEngine.stats("hello world\nnext")
        assertEquals(16, chars)
        assertEquals(3, words)
        assertEquals(2, lines)
    }
}
