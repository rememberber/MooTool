package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EncodeEngineTest {
    @Test
    fun roundTripsUnicodeAndUtf8Hex() {
        assertEquals("Moo 工具 🚀", EncodeEngine.fromUnicode(EncodeEngine.toUnicode("Moo 工具 🚀")))
        assertEquals("你好 Moo", EncodeEngine.hexToText(EncodeEngine.textToHex("你好 Moo")))
        assertTrue(EncodeEngine.toUnicode("🚀").contains("\\u"))
    }

    @Test
    fun roundTripsUrlTextInUtf8AndGb2312() {
        assertEquals("你好 a/b", EncodeEngine.urlDecode(EncodeEngine.urlEncode("你好 a/b", UrlCharset.Utf8), UrlCharset.Utf8))
        assertEquals("编码测试", EncodeEngine.urlDecode(EncodeEngine.urlEncode("编码测试", UrlCharset.Gb2312), UrlCharset.Gb2312))
        assertTrue(EncodeEngine.urlEncode("a b", UrlCharset.Utf8).contains("%20"))
        assertEquals("a b", EncodeEngine.urlDecode("a+b", UrlCharset.Utf8))
    }

    @Test
    fun convertsCodePointsToDecimalOrHexLists() {
        assertEquals("65 20013", EncodeEngine.textToAscii("A中", AsciiFormat.Decimal))
        assertEquals("41 4E2D", EncodeEngine.textToAscii("A中", AsciiFormat.Hex))
        assertEquals("A中", EncodeEngine.asciiToText("65 4E2D"))
        assertEquals("", EncodeEngine.asciiToText("  "))
    }

    @Test
    fun rejectsBrokenHexTruncationAndUnmappableGb2312() {
        assertFailsWith<EncodeException> { EncodeEngine.hexToText("zzz") }
        assertFailsWith<EncodeException> { EncodeEngine.hexToText("abc") }
        assertFailsWith<EncodeException> { EncodeEngine.hexToText("ff") }
        assertFailsWith<EncodeException> { EncodeEngine.urlEncode("🚀", UrlCharset.Gb2312) }
        assertFailsWith<EncodeException> { EncodeEngine.asciiToText("1114112") }
    }

    @Test
    fun preservesNewlinesAndPercentInUrlRoundTrip() {
        val source = "line1\nline2 % path"
        assertEquals(source, EncodeEngine.urlDecode(EncodeEngine.urlEncode(source, UrlCharset.Utf8), UrlCharset.Utf8))
        assertEquals("MooTool 编码转换", EncodeEngine.fromUnicode(EncodeEngine.toUnicode("MooTool 编码转换")))
    }
}
