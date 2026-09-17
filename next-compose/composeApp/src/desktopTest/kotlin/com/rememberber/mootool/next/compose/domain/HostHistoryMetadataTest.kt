package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class HostHistoryMetadataTest {
    @Test
    fun encodeApplyBackup_trimsPath() {
        assertEquals("/tmp/hosts.bak", HostHistoryMetadata.encodeApplyBackup("  /tmp/hosts.bak  "))
        assertEquals("", HostHistoryMetadata.decodeBackup("   "))
    }
}
