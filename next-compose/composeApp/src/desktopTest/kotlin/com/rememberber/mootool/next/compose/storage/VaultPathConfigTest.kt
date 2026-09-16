package com.rememberber.mootool.next.compose.storage

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class VaultPathConfigTest {
    @Test
    fun emptyUsesDefaultSentinel() {
        assertEquals("", VaultPathConfig.normalizedCustomRoot(""))
        assertEquals("", VaultPathConfig.normalizedCustomRoot("   "))
    }

    @Test
    fun rejectsRelativePaths() {
        assertNull(VaultPathConfig.normalizedCustomRoot("relative/path"))
        assertEquals("", VaultPathConfig.effectiveCustomRoot("relative/path"))
        assertFailsWith<IllegalArgumentException> {
            VaultPathConfig.resolveCustomRoot("notes")
        }
    }

    @Test
    fun acceptsAbsolutePaths() {
        val dir = Files.createTempDirectory("mootool-vault-path-")
        try {
            val normalized = VaultPathConfig.normalizedCustomRoot(dir.toString())
            assertEquals(dir.toAbsolutePath().normalize().toString(), normalized)
            assertEquals(dir.toAbsolutePath().normalize(), VaultPathConfig.resolveCustomRoot(dir.toString()))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
