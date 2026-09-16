package com.rememberber.mootool.next.compose.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.nio.file.Files
import java.nio.file.Path

class VaultMcpReadServiceTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun searchReadAndPaging() {
        val home = Files.createTempDirectory("mootool-vault-mcp-")
        try {
            val notes = home.resolve("notes")
            val jsonVault = home.resolve("json")
            val policy = home.resolve("access.json")
            notes.createDirectories()
            jsonVault.createDirectories()
            policy.writeText("""{"version":1,"notes":"${notes.accessPolicyPath()}","json":"${jsonVault.accessPolicyPath()}"}""")
            notes.resolve("note.md").writeText(
                "---\ntitle: 我的笔记\nsyntax: text/markdown\n---\nMooTool fixture 🐮"
            )
            jsonVault.resolve("document.json").writeText("""{"fixture":"mootool","value":42}""")
            val service = VaultMcpReadService(policy)
            val found = service.search(VaultMcpKind.Notes, "我的笔记", 10, 0)
            assertEquals(listOf("note.md"), found.entries.map { it.path })
            assertEquals("MooTool fixture 🐮", found.entries.first().excerpt)
            val first = service.read(VaultMcpKind.Notes, "note.md", 0, 7)
            val second = service.read(VaultMcpKind.Notes, "note.md", first.nextOffset!!, 50)
            assertEquals("MooTool fixture 🐮", first.content + second.content)
            val original = notes.resolve("note.md").readText()
            service.read(VaultMcpKind.Notes, "note.md", 0, 100)
            assertEquals(original, notes.resolve("note.md").readText())
            assertEquals(listOf("note.md"), notes.listDirectoryEntries().map { it.fileName.toString() }.sorted())
            val jsonHit = service.search(VaultMcpKind.Json, "42", 10, 0)
            assertEquals("document.json", jsonHit.entries.first().path)
            val body = service.read(VaultMcpKind.Json, "document.json", 0, 100)
            assertEquals(42, json.parseToJsonElement(body.content).jsonObject["value"]!!.toString().toInt())
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun revokesAccessWhenPolicyCleared() {
        val home = Files.createTempDirectory("mootool-vault-mcp-")
        try {
            val notes = home.resolve("notes")
            notes.createDirectories()
            val policy = home.resolve("access.json")
            policy.writeText("""{"version":1,"notes":"${notes.accessPolicyPath()}","json":null}""")
            notes.resolve("note.md").writeText("body")
            val tool = VaultMcpTools.call("mootool_notes_read", mapOf("path" to "note.md"), policy)
            assertFalse(tool.isError)
            val jsonVault = home.resolve("json")
            jsonVault.createDirectories()
            jsonVault.resolve("document.json").writeText("""{"value":1}""")
            policy.writeText(
                """{"version":1,"notes":"${notes.accessPolicyPath()}","json":"${jsonVault.accessPolicyPath()}"}""",
            )
            assertFalse(
                VaultMcpTools.call(
                    "mootool_json_documents_read",
                    mapOf("path" to "document.json", "length" to 50_000),
                    policy,
                ).isError,
            )
            policy.writeText("""{"version":1,"notes":null,"json":null}""")
            val denied = VaultMcpTools.call("mootool_notes_read", mapOf("path" to "note.md"), policy)
            assertTrue(denied.isError)
            assertTrue(
                VaultMcpTools.call(
                    "mootool_json_documents_read",
                    mapOf("path" to "document.json", "length" to 50_000),
                    policy,
                ).isError,
            )
            assertFailsWith<IllegalArgumentException> {
                VaultMcpReadService(null).search(VaultMcpKind.Notes, "", 10, 0)
            }
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun rejectsUnsafePathsAndGitignoredFiles() {
        val home = Files.createTempDirectory("mootool-vault-mcp-")
        try {
            val notes = home.resolve("notes")
            notes.createDirectories()
            val policy = home.resolve("access.json")
            policy.writeText("""{"version":1,"notes":"${notes.accessPolicyPath()}","json":null}""")
            notes.resolve(".gitignore").writeText("private/\nignored.md\n")
            notes.resolve("note.md").writeText("visible")
            notes.resolve("private").createDirectories()
            notes.resolve("private/secret.md").writeText("secret")
            notes.resolve("ignored.md").writeText("ignored")
            notes.resolve(".secret.md").writeText("hidden")
            notes.resolve("large.md").writeText("x".repeat(2_000_001))
            val service = VaultMcpReadService(policy)
            home.resolve("outside.md").writeText("outside secret")
            for (path in listOf(
                "../outside.md",
                "/etc/hosts",
                "C:/outside.md",
                "..\\outside.md",
                ".secret.md",
                "private/secret.md",
                "ignored.md",
                "large.md",
            )) {
                assertFailsWith<Exception> { service.read(VaultMcpKind.Notes, path, 0, 100) }
            }
            if (supportsSymlinks()) {
                java.nio.file.Files.createSymbolicLink(notes.resolve("link.md"), home.resolve("outside.md"))
                assertFailsWith<Exception> { service.read(VaultMcpKind.Notes, "link.md", 0, 100) }
            }
            assertEquals(emptyList(), service.search(VaultMcpKind.Notes, "secret", 20, 0).entries)
            assertEquals(listOf("note.md"), service.search(VaultMcpKind.Notes, "", 20, 0).entries.map { it.path })
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun refusesDocumentAccessWhenGitignoreCannotBeReadSafely() {
        val home = Files.createTempDirectory("mootool-vault-mcp-gitignore-")
        try {
            val notes = home.resolve("notes")
            notes.createDirectories()
            val policy = home.resolve("access.json")
            policy.writeText("""{"version":1,"notes":"${notes.accessPolicyPath()}","json":null}""")
            notes.resolve("note.md").writeText("body")
            val service = VaultMcpReadService(policy)
            notes.resolve(".gitignore").writeText("#".repeat(100_001))
            assertFailsWith<IllegalArgumentException> {
                service.search(VaultMcpKind.Notes, "", 10, 0)
            }
            assertFailsWith<IllegalArgumentException> {
                service.read(VaultMcpKind.Notes, "note.md", 0, 100)
            }
            notes.resolve(".gitignore").writeBytes(byteArrayOf(0xff.toByte()))
            assertFailsWith<Exception> {
                service.read(VaultMcpKind.Notes, "note.md", 0, 100)
            }
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun rejectsRelativeVaultRootGrant() {
        val home = Files.createTempDirectory("mootool-vault-mcp-relative-root-")
        try {
            val notes = home.resolve("notes")
            notes.createDirectories()
            notes.resolve("note.md").writeText("body")
            val policy = home.resolve("access.json")
            policy.writeText("""{"version":1,"notes":"notes","json":null}""")
            val service = VaultMcpReadService(policy)
            assertFailsWith<IllegalArgumentException> {
                service.search(VaultMcpKind.Notes, "", 10, 0)
            }
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun rejectsSymlinkVaultRootGrant() {
        if (!supportsSymlinks()) return
        val home = Files.createTempDirectory("mootool-vault-mcp-root-link-")
        try {
            val vault = home.resolve("vault")
            vault.createDirectories()
            vault.resolve("note.md").writeText("body")
            val link = home.resolve("notes-link")
            Files.createSymbolicLink(link, vault)
            val policy = home.resolve("access.json")
            policy.writeText("""{"version":1,"notes":"${link.toAbsolutePath()}","json":null}""")
            val service = VaultMcpReadService(policy)
            assertFailsWith<IllegalArgumentException> {
                service.search(VaultMcpKind.Notes, "", 10, 0)
            }
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    @Test
    fun pagesSearchResultsWithoutDroppingMatches() {
        val home = Files.createTempDirectory("mootool-vault-mcp-page-")
        try {
            val jsonVault = home.resolve("json")
            jsonVault.createDirectories()
            val policy = home.resolve("access.json")
            policy.writeText("""{"version":1,"notes":null,"json":"${jsonVault.accessPolicyPath()}"}""")
            for (index in 0..3) {
                jsonVault.resolve("file-$index.json").writeText("""{"index":$index}""")
            }
            val service = VaultMcpReadService(policy)
            val first = service.search(VaultMcpKind.Json, "index", 2, 0)
            val second = service.search(VaultMcpKind.Json, "index", 2, first.nextOffset!!)
            val paths = first.entries + second.entries
            assertEquals(
                listOf("file-0.json", "file-1.json", "file-2.json", "file-3.json"),
                paths.map { it.path },
            )
            assertEquals(null, second.nextOffset)
        } finally {
            home.toFile().deleteRecursively()
        }
    }

    /** 与 `AiIntegrationService.setDataAccess` 一致：写入 canonical `realpath`。 */
    private fun Path.accessPolicyPath(): String = toRealPath().toString()

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
