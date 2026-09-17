package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegexHistoryMetadataTest {
    @Test
    fun encodeDecode_roundTripsFlags() {
        val options = RegexOptions(global = true, ignoreCase = true, multiline = false, dotAll = true)
        val wire = RegexHistoryMetadata.encode(options)
        val restored = RegexHistoryMetadata.decode(wire)
        assertTrue(restored.global)
        assertTrue(restored.ignoreCase)
        assertTrue(restored.dotAll)
        assertEquals(false, restored.multiline)
    }

    @Test
    fun decode_blankReturnsFallback() {
        val fallback = RegexOptions(global = false, ignoreCase = true)
        assertEquals(fallback.ignoreCase, RegexHistoryMetadata.decode("", fallback).ignoreCase)
    }
}
