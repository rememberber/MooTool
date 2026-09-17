package com.rememberber.mootool.next.compose.ai

import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.transport.ServerParameters
import io.modelcontextprotocol.client.transport.StdioClientTransport
import io.modelcontextprotocol.json.McpJsonDefaults
import io.modelcontextprotocol.spec.McpSchema
import java.nio.file.Files
import java.time.Duration
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Vault 只读 MCP 经 stdio 子进程端到端（`McpBootstrap` + `VaultMcpTools`）。 */
class AiIntegrationVaultMcpConnectionTest {
    @Test
    fun subprocessSearchAndReadNotesVault() {
        val notesRoot = Files.createTempDirectory("mootool-mcp-vault-notes-")
        val productRoot = Files.createTempDirectory("mootool-mcp-vault-product-")
        try {
            notesRoot.resolve("note.md").writeText(
                "---\ntitle: 端到端\nsyntax: text/markdown\n---\nMooTool MCP subprocess",
            )
            val directories = com.rememberber.mootool.next.compose.app.AppPaths
                .resolve(productRoot.toString())
                .also { it.ensureCreated() }
            val accessFile = McpLaunchResolver.accessFile(directories).also { path ->
                path.parent.createDirectories()
                path.writeText(
                    """{"version":1,"notes":"${notesRoot.toRealPath()}","json":null}""",
                )
            }
            val launch = desktopTestMcpLaunch(accessFile)
            val transport = StdioClientTransport(
                ServerParameters.builder(launch.command).args(launch.args).build(),
                McpJsonDefaults.getMapper(),
            )
            McpClient.sync(transport).requestTimeout(Duration.ofSeconds(15)).build().use { client ->
                client.initialize()
                val search = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_notes_search")
                        .arguments(mapOf("query" to "subprocess", "limit" to 10, "offset" to 0))
                        .build(),
                )
                assertFalse(search.isError)
                val searchText = search.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                } ?: ""
                assertTrue(searchText.contains("note.md"))
                assertTrue(searchText.contains("subprocess"))
                val read = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_notes_read")
                        .arguments(mapOf("path" to "note.md", "offset" to 0, "length" to 200))
                        .build(),
                )
                assertFalse(read.isError)
                val body = read.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                } ?: ""
                assertTrue(body.contains("MooTool MCP subprocess"))
            }
        } finally {
            notesRoot.toFile().deleteRecursively()
            productRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun subprocessSearchAndReadJsonVault() {
        val jsonRoot = Files.createTempDirectory("mootool-mcp-vault-json-")
        val productRoot = Files.createTempDirectory("mootool-mcp-vault-json-product-")
        try {
            jsonRoot.resolve("doc.json").writeText("""{"fixture":"mcp","value":42}""")
            val directories = com.rememberber.mootool.next.compose.app.AppPaths
                .resolve(productRoot.toString())
                .also { it.ensureCreated() }
            val accessFile = McpLaunchResolver.accessFile(directories).also { path ->
                path.parent.createDirectories()
                path.writeText(
                    """{"version":1,"notes":null,"json":"${jsonRoot.toRealPath()}"}""",
                )
            }
            val launch = desktopTestMcpLaunch(accessFile)
            val transport = StdioClientTransport(
                ServerParameters.builder(launch.command).args(launch.args).build(),
                McpJsonDefaults.getMapper(),
            )
            McpClient.sync(transport).requestTimeout(Duration.ofSeconds(15)).build().use { client ->
                client.initialize()
                val search = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_json_documents_search")
                        .arguments(mapOf("query" to "42", "limit" to 5, "offset" to 0))
                        .build(),
                )
                assertFalse(search.isError)
                val searchText = search.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                } ?: ""
                assertTrue(searchText.contains("doc.json"))
                val read = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_json_documents_read")
                        .arguments(mapOf("path" to "doc.json", "offset" to 0, "length" to 500))
                        .build(),
                )
                assertFalse(read.isError)
                val body = read.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                } ?: ""
                assertTrue(body.contains("42"))
            }
        } finally {
            jsonRoot.toFile().deleteRecursively()
            productRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun subprocessReadJsonVaultHonorsOffsetAndLength() {
        val jsonRoot = Files.createTempDirectory("mootool-mcp-vault-read-offset-")
        val productRoot = Files.createTempDirectory("mootool-mcp-vault-read-offset-product-")
        try {
            jsonRoot.resolve("page.json").writeText("""{"head":"skip","tail":"563"}""")
            val directories = com.rememberber.mootool.next.compose.app.AppPaths
                .resolve(productRoot.toString())
                .also { it.ensureCreated() }
            val accessFile = McpLaunchResolver.accessFile(directories).also { path ->
                path.parent.createDirectories()
                path.writeText(
                    """{"version":1,"notes":null,"json":"${jsonRoot.toRealPath()}"}""",
                )
            }
            val launch = desktopTestMcpLaunch(accessFile)
            val transport = StdioClientTransport(
                ServerParameters.builder(launch.command).args(launch.args).build(),
                McpJsonDefaults.getMapper(),
            )
            McpClient.sync(transport).requestTimeout(Duration.ofSeconds(15)).build().use { client ->
                client.initialize()
                val read = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_json_documents_read")
                        .arguments(mapOf("path" to "page.json", "offset" to 18, "length" to 20))
                        .build(),
                )
                assertFalse(read.isError)
                val body = read.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                } ?: ""
                assertTrue(body.contains("563"))
                assertFalse(body.contains("skip"))
            }
        } finally {
            jsonRoot.toFile().deleteRecursively()
            productRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun subprocessSearchJsonVaultHonorsSearchOffset() {
        val jsonRoot = Files.createTempDirectory("mootool-mcp-vault-search-offset-")
        val productRoot = Files.createTempDirectory("mootool-mcp-vault-search-offset-product-")
        try {
            jsonRoot.resolve("alpha.json").writeText("""{"tag":"564-alpha"}""")
            jsonRoot.resolve("beta.json").writeText("""{"tag":"564-beta"}""")
            val directories = com.rememberber.mootool.next.compose.app.AppPaths
                .resolve(productRoot.toString())
                .also { it.ensureCreated() }
            val accessFile = McpLaunchResolver.accessFile(directories).also { path ->
                path.parent.createDirectories()
                path.writeText(
                    """{"version":1,"notes":null,"json":"${jsonRoot.toRealPath()}"}""",
                )
            }
            val launch = desktopTestMcpLaunch(accessFile)
            val transport = StdioClientTransport(
                ServerParameters.builder(launch.command).args(launch.args).build(),
                McpJsonDefaults.getMapper(),
            )
            McpClient.sync(transport).requestTimeout(Duration.ofSeconds(15)).build().use { client ->
                client.initialize()
                val page = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_json_documents_search")
                        .arguments(mapOf("query" to "564", "limit" to 1, "offset" to 1))
                        .build(),
                )
                assertFalse(page.isError)
                val text = page.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                } ?: ""
                assertTrue(text.contains("beta.json") || text.contains("564-beta"))
                assertFalse(text.contains("alpha.json"))
            }
        } finally {
            jsonRoot.toFile().deleteRecursively()
            productRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun subprocessSearchBothVaultKindsInOneAccessPolicy() {
        val notesRoot = Files.createTempDirectory("mootool-mcp-vault-dual-notes-")
        val jsonRoot = Files.createTempDirectory("mootool-mcp-vault-dual-json-")
        val productRoot = Files.createTempDirectory("mootool-mcp-vault-dual-product-")
        try {
            notesRoot.resolve("note.md").writeText(
                "---\ntitle: dual\nsyntax: text/markdown\n---\nnotes vault dual",
            )
            jsonRoot.resolve("data.json").writeText("""{"dual":true,"tag":"562"}""")
            val directories = com.rememberber.mootool.next.compose.app.AppPaths
                .resolve(productRoot.toString())
                .also { it.ensureCreated() }
            val accessFile = McpLaunchResolver.accessFile(directories).also { path ->
                path.parent.createDirectories()
                path.writeText(
                    """{"version":1,"notes":"${notesRoot.toRealPath()}","json":"${jsonRoot.toRealPath()}"}""",
                )
            }
            val launch = desktopTestMcpLaunch(accessFile)
            val transport = StdioClientTransport(
                ServerParameters.builder(launch.command).args(launch.args).build(),
                McpJsonDefaults.getMapper(),
            )
            McpClient.sync(transport).requestTimeout(Duration.ofSeconds(15)).build().use { client ->
                client.initialize()
                val notesSearch = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_notes_search")
                        .arguments(mapOf("query" to "dual", "limit" to 5, "offset" to 0))
                        .build(),
                )
                assertFalse(notesSearch.isError)
                val jsonSearch = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_json_documents_search")
                        .arguments(mapOf("query" to "562", "limit" to 5, "offset" to 0))
                        .build(),
                )
                assertFalse(jsonSearch.isError)
                val jsonText = jsonSearch.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                } ?: ""
                assertTrue(jsonText.contains("data.json"))
            }
        } finally {
            notesRoot.toFile().deleteRecursively()
            jsonRoot.toFile().deleteRecursively()
            productRoot.toFile().deleteRecursively()
        }
    }
}
