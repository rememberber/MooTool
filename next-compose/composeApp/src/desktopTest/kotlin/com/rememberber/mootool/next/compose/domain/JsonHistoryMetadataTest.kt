package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonHistoryMetadataTest {
    @Test
    fun encodesEditorAndPathQueryKinds() {
        assertFalse(JsonHistoryMetadata.isPathQuery(JsonHistoryMetadata.encodeEditor()))
        assertTrue(JsonHistoryMetadata.isPathQuery(JsonHistoryMetadata.encodePathQuery()))
        assertEquals(JsonHistoryMetadata.KIND_EDITOR, JsonHistoryMetadata.decode("").kind)
    }
}
