package com.rememberber.mootool.next.compose.ai

import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.nio.file.Files

class VaultMcpAccessTest {
    @Test
    fun readRejectsOversizedAccessFile() {
        val file = Files.createTempFile("mootool-access-", ".json")
        try {
            file.writeText("""{"version":1,"notes":null,"json":null}""" + " ".repeat(20_000))
            assertFailsWith<IllegalArgumentException> {
                VaultMcpAccess.read(file)
            }
        } finally {
            file.toFile().delete()
        }
    }

    @Test
    fun readReturnsEmptyWhenMissing() {
        assertEquals(VaultMcpAccessFile(), VaultMcpAccess.read(null))
    }

    @Test
    fun readRejectsUnsupportedSchemaVersion() {
        val file = Files.createTempFile("mootool-access-ver-", ".json")
        try {
            file.writeText("""{"version":2,"notes":null,"json":null}""")
            assertFailsWith<IllegalArgumentException> {
                VaultMcpAccess.read(file)
            }
        } finally {
            file.toFile().delete()
        }
    }

    @Test
    fun readRejectsUnknownJsonProperties() {
        val file = Files.createTempFile("mootool-access-extra-", ".json")
        try {
            file.writeText("""{"version":1,"notes":null,"json":null,"extra":true}""")
            assertFailsWith<IllegalArgumentException> {
                VaultMcpAccess.read(file)
            }
        } finally {
            file.toFile().delete()
        }
    }

    @Test
    fun readRejectsMalformedJson() {
        val file = Files.createTempFile("mootool-access-bad-", ".json")
        try {
            file.writeText("{not json")
            assertFailsWith<IllegalArgumentException> {
                VaultMcpAccess.read(file)
            }
        } finally {
            file.toFile().delete()
        }
    }

    @Test
    fun readRejectsSymlinkAccessFile() {
        if (!supportsSymlinks()) return
        val dir = Files.createTempDirectory("mootool-access-symlink-")
        try {
            val real = dir.resolve("real.json")
            real.writeText("""{"version":1,"notes":null,"json":null}""")
            val link = dir.resolve("link.json")
            Files.createSymbolicLink(link, real)
            assertFailsWith<IllegalArgumentException> {
                VaultMcpAccess.read(link)
            }
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    private fun supportsSymlinks(): Boolean =
        runCatching {
            val dir = Files.createTempDirectory("mootool-symlink-probe-")
            try {
                val target = dir.resolve("t.txt")
                target.writeText("x")
                Files.createSymbolicLink(dir.resolve("l.txt"), target)
                true
            } finally {
                dir.toFile().deleteRecursively()
            }
        }.getOrDefault(false)
}
