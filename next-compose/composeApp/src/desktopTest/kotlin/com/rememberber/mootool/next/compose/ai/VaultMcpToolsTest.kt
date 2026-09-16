package com.rememberber.mootool.next.compose.ai

import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 对照 Electron `vaultTools.ts` Zod schema 边界。 */
class VaultMcpToolsTest {
    @Test
    fun rejectsOutOfRangeSearchAndReadArguments() {
        val home = Files.createTempDirectory("mootool-vault-mcp-tools-")
        try {
            val notes = home.resolve("notes")
            notes.createDirectories()
            val policy = home.resolve("access.json")
            policy.writeText("""{"version":1,"notes":"${notes.toRealPath()}","json":null}""")
            notes.resolve("note.md").writeText("body")
            assertTrue(
                VaultMcpTools.call(
                    "mootool_notes_search",
                    mapOf("limit" to 51),
                    policy,
                ).isError,
            )
            assertTrue(
                VaultMcpTools.call(
                    "mootool_notes_search",
                    mapOf("query" to "x".repeat(201)),
                    policy,
                ).isError,
            )
            assertTrue(
                VaultMcpTools.call(
                    "mootool_notes_read",
                    mapOf("path" to "", "length" to 100),
                    policy,
                ).isError,
            )
            assertTrue(
                VaultMcpTools.call(
                    "mootool_notes_read",
                    mapOf("path" to "note.md", "length" to 50_001),
                    policy,
                ).isError,
            )
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun acceptsInRangeArguments() {
        val home = Files.createTempDirectory("mootool-vault-mcp-tools-ok-")
        try {
            val notes = home.resolve("notes")
            notes.createDirectories()
            val policy = home.resolve("access.json")
            policy.writeText("""{"version":1,"notes":"${notes.toRealPath()}","json":null}""")
            notes.resolve("note.md").writeText("hello")
            val read = VaultMcpTools.call(
                "mootool_notes_read",
                mapOf("path" to "note.md", "offset" to 0, "length" to 50_000),
                policy,
            )
            assertFalse(read.isError)
            assertTrue(read.text.contains("hello"))
        } finally {
            home.toFile().deleteRecursively()
        }
    }
}
