package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class CodeRunHistoryMetadataTest {
    @Test
    fun encodesRuntimeAndRunOptions() {
        val raw = CodeRunHistoryMetadata.encode(CodeRuntime.Python, "--verbose", "/tmp/work")
        val meta = CodeRunHistoryMetadata.decode(raw)
        assertEquals("--verbose", meta?.arguments)
        assertEquals("/tmp/work", meta?.workingDirectory)
        assertEquals("python", meta?.runtime)
    }
}
