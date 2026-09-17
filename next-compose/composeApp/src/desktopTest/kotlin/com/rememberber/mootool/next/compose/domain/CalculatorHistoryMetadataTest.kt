package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class CalculatorHistoryMetadataTest {
    @Test
    fun encodeDecode_marker() {
        assertEquals(CalculatorHistoryMetadata.MARKER, CalculatorHistoryMetadata.encode())
        assertEquals(CalculatorHistoryMetadata.MARKER, CalculatorHistoryMetadata.decode(""))
        assertEquals(CalculatorHistoryMetadata.MARKER, CalculatorHistoryMetadata.decode("calc"))
    }
}
