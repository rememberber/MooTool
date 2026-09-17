package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class EnvSessionMetadataTest {
    @Test
    fun normalizesTabAndScopeWireIds() {
        assertEquals(EnvSessionMetadata.TAB_RUNTIME, EnvSessionMetadata.normalizeTab("Runtime"))
        assertEquals(EnvSessionMetadata.TAB_ENVIRONMENT, EnvSessionMetadata.normalizeTab("unknown"))
        assertEquals(EnvSessionMetadata.SCOPE_USER, EnvSessionMetadata.normalizeScope("User"))
        assertEquals(EnvSessionMetadata.SCOPE_PROCESS, EnvSessionMetadata.normalizeScope(""))
    }
}
