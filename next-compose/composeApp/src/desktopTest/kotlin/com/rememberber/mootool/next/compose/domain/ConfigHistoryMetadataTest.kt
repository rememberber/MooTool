package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigHistoryMetadataTest {
    @Test
    fun encodeConvert_mapsDirection() {
        assertEquals(ConfigHistoryMetadata.TO_YAML, ConfigHistoryMetadata.encodeConvert(toYaml = true))
        assertEquals(ConfigHistoryMetadata.TO_PROPERTIES, ConfigHistoryMetadata.encodeConvert(toYaml = false))
    }

    @Test
    fun isConvert_recognizesConvertOptions() {
        assertTrue(ConfigHistoryMetadata.isConvert(ConfigHistoryMetadata.TO_YAML))
        assertTrue(ConfigHistoryMetadata.isConvert(ConfigHistoryMetadata.TO_PROPERTIES))
        assertFalse(ConfigHistoryMetadata.isConvert(ConfigHistoryMetadata.VALIDATE))
    }
}
