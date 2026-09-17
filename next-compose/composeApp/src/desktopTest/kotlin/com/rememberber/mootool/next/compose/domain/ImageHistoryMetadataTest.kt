package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ImageHistoryMetadataTest {
    @Test
    fun normalizeOperation_mapsSvgAlias() {
        assertEquals(ImageHistoryMetadata.OP_SVG, ImageHistoryMetadata.normalizeOperation("svg"))
        assertEquals(ImageHistoryMetadata.OP_PROCESS, ImageHistoryMetadata.normalizeOperation("process"))
        assertEquals(ImageHistoryMetadata.OP_PROCESS, ImageHistoryMetadata.normalizeOperation("  "))
    }
}
