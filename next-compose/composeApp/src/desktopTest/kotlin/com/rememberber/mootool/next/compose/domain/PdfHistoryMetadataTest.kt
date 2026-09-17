package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class PdfHistoryMetadataTest {
    @Test
    fun normalizeOperation_prefersMergeWhenExplicit() {
        assertEquals(PdfHistoryMetadata.OP_MERGE, PdfHistoryMetadata.normalizeOperation("merge"))
        assertEquals(PdfHistoryMetadata.OP_SPLIT, PdfHistoryMetadata.normalizeOperation("split"))
        assertEquals(PdfHistoryMetadata.OP_SPLIT, PdfHistoryMetadata.normalizeOperation("unknown"))
    }
}
