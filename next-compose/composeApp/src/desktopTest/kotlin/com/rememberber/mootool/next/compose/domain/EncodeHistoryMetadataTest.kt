package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class EncodeHistoryMetadataTest {
    @Test
    fun roundTripsTabDirectionCharsetAndAsciiFormat() {
        val encoded = EncodeHistoryMetadata.encode(EncodeTab.Url, forward = false, UrlCharset.Gb2312, AsciiFormat.Hex)
        val parsed = EncodeHistoryMetadata.decode(encoded)!!
        assertEquals("url", parsed.tab)
        assertEquals("reverse", parsed.direction)
        assertEquals("gb2312", parsed.charset)
        assertEquals("hex", parsed.asciiFormat)
    }

    @Test
    fun decodeReturnsNullForBlankOptions() {
        assertEquals(null, EncodeHistoryMetadata.decode("   "))
    }
}
