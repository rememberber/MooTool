package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ColorHistoryMetadataTest {
    @Test
    fun encodesFormatAndOperationJson() {
        val encoded = ColorHistoryMetadata.encode(ColorFormat.RGB, ColorOperation.Invert.name)
        val meta = ColorHistoryMetadata.decode(encoded)
        assertNotNull(meta)
        assertEquals(ColorFormat.RGB.name, meta.format)
        assertEquals(ColorOperation.Invert.name, meta.operation)
    }

    @Test
    fun decodesLegacyPlainFormatOption() {
        val meta = ColorHistoryMetadata.decode(ColorFormat.HEX_LOWER.name)
        assertNotNull(meta)
        assertEquals(ColorFormat.HEX_LOWER.name, meta.format)
        assertEquals("pick", meta.operation)
    }
}
