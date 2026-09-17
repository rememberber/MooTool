package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiffHistoryMetadataTest {
    @Test
    fun roundTripsIgnoreWhitespaceAndHighlightMode() {
        val encoded = DiffHistoryMetadata.encode(ignoreWhitespace = true, highlightMode = "line")
        val parsed = DiffHistoryMetadata.decode(encoded)
        assertTrue(parsed.ignoreWhitespace)
        assertEquals("line", parsed.highlightMode)
    }

    @Test
    fun decodesLegacyTabSeparatedOptions() {
        val parsed = DiffHistoryMetadata.decode("false\tcharacter")
        assertFalse(parsed.ignoreWhitespace)
        assertEquals("character", parsed.highlightMode)
    }
}
