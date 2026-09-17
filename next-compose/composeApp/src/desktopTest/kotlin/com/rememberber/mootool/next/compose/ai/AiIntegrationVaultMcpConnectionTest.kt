package com.rememberber.mootool.next.compose.ai

import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.transport.ServerParameters
import io.modelcontextprotocol.client.transport.StdioClientTransport
import io.modelcontextprotocol.json.McpJsonDefaults
import io.modelcontextprotocol.spec.McpSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
    fun subprocessNotesReadHonorsOffsetAndLength() {
        val notesRoot = Files.createTempDirectory("mootool-mcp-vault-notes-read-page-")
        val productRoot = Files.createTempDirectory("mootool-mcp-vault-notes-read-page-product-")
        try {
            notesRoot.resolve("note.md").writeText(
                "---\ntitle: 分页\nsyntax: text/markdown\n---\nMooTool fixture 🐮",
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
                val first = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_notes_read")
                        .arguments(mapOf("path" to "note.md", "offset" to 0, "length" to 7))
                        .build(),
                )
                assertFalse(first.isError)
                val firstJson = first.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                } ?: ""
                val firstBody = Json.parseToJsonElement(firstJson).jsonObject
                val nextOffset = firstBody["nextOffset"]?.jsonPrimitive?.content?.toInt()
                assertTrue(nextOffset != null && nextOffset > 0)
                val second = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_notes_read")
                        .arguments(mapOf("path" to "note.md", "offset" to nextOffset, "length" to 50))
                        .build(),
                )
                assertFalse(second.isError)
                val secondJson = second.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                } ?: ""
                val secondBody = Json.parseToJsonElement(secondJson).jsonObject
                val combined = firstBody["content"]!!.jsonPrimitive.content +
                    secondBody["content"]!!.jsonPrimitive.content
                assertTrue(combined.contains("MooTool fixture"))
                assertTrue(combined.contains("🐮"))
            }
        } finally {
            notesRoot.toFile().deleteRecursively()
            productRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun subprocessListToolsIncludesVaultReadToolsWithNotesGrant() {
        val notesRoot = Files.createTempDirectory("mootool-mcp-vault-list-notes-")
        val productRoot = Files.createTempDirectory("mootool-mcp-vault-list-notes-product-")
        try {
            notesRoot.resolve("note.md").writeText("---\ntitle: t\nsyntax: text/markdown\n---\nbody")
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
                val tools = client.listTools().tools.map { it.name() }.toSet()
                assertTrue("mootool_notes_search" in tools)
                assertTrue("mootool_notes_read" in tools)
                val read = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_notes_read")
                        .arguments(mapOf("path" to "note.md", "offset" to 0, "length" to 200))
                        .build(),
                )
                assertFalse(read.isError)
                assertTrue(
                    read.content.firstOrNull()?.let { if (it is McpSchema.TextContent) it.text else null }.orEmpty()
                        .contains("body"),
                )
            }
        } finally {
            notesRoot.toFile().deleteRecursively()
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

    @Test
    fun subprocessNotesSearchHonorsSearchOffset() {
        val notesRoot = Files.createTempDirectory("mootool-mcp-vault-notes-search-offset-")
        val productRoot = Files.createTempDirectory("mootool-mcp-vault-notes-search-offset-product-")
        try {
            notesRoot.resolve("alpha.md").writeText(
                "---\ntitle: a\nsyntax: text/markdown\n---\n566-alpha",
            )
            notesRoot.resolve("beta.md").writeText(
                "---\ntitle: b\nsyntax: text/markdown\n---\n566-beta",
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
                val page = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_notes_search")
                        .arguments(mapOf("query" to "566", "limit" to 1, "offset" to 1))
                        .build(),
                )
                assertFalse(page.isError)
                val text = page.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                } ?: ""
                assertTrue(text.contains("beta.md") || text.contains("566-beta"))
                assertFalse(text.contains("alpha.md"))
            }
        } finally {
            notesRoot.toFile().deleteRecursively()
            productRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun subprocessDualVaultNotesSearchHonorsOffsetWhenJsonGranted() {
        val notesRoot = Files.createTempDirectory("mootool-mcp-vault-dual-notes-offset-")
        val jsonRoot = Files.createTempDirectory("mootool-mcp-vault-dual-notes-offset-json-")
        val productRoot = Files.createTempDirectory("mootool-mcp-vault-dual-notes-offset-product-")
        try {
            notesRoot.resolve("alpha.md").writeText(
                "---\ntitle: a\nsyntax: text/markdown\n---\n567-alpha",
            )
            notesRoot.resolve("beta.md").writeText(
                "---\ntitle: b\nsyntax: text/markdown\n---\n567-beta",
            )
            jsonRoot.resolve("data.json").writeText("""{"tag":"567-json"}""")
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
                val page = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_notes_search")
                        .arguments(mapOf("query" to "567", "limit" to 1, "offset" to 1))
                        .build(),
                )
                assertFalse(page.isError)
                val text = page.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                } ?: ""
                assertTrue(text.contains("beta.md") || text.contains("567-beta"))
                assertFalse(text.contains("alpha.md"))
            }
        } finally {
            notesRoot.toFile().deleteRecursively()
            jsonRoot.toFile().deleteRecursively()
            productRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun subprocessDualVaultJsonSearchHonorsOffsetWhenNotesGranted() {
        val notesRoot = Files.createTempDirectory("mootool-mcp-vault-dual-json-offset-notes-")
        val jsonRoot = Files.createTempDirectory("mootool-mcp-vault-dual-json-offset-json-")
        val productRoot = Files.createTempDirectory("mootool-mcp-vault-dual-json-offset-product-")
        try {
            notesRoot.resolve("note.md").writeText(
                "---\ntitle: n\nsyntax: text/markdown\n---\n568-notes",
            )
            jsonRoot.resolve("alpha.json").writeText("""{"tag":"568-alpha"}""")
            jsonRoot.resolve("beta.json").writeText("""{"tag":"568-beta"}""")
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
                val page = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_json_documents_search")
                        .arguments(mapOf("query" to "568", "limit" to 1, "offset" to 1))
                        .build(),
                )
                assertFalse(page.isError)
                val text = page.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                } ?: ""
                assertTrue(text.contains("beta.json") || text.contains("568-beta"))
                assertFalse(text.contains("alpha.json"))
            }
        } finally {
            notesRoot.toFile().deleteRecursively()
            jsonRoot.toFile().deleteRecursively()
            productRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun subprocessDualVaultJsonReadHonorsOffsetWhenNotesGranted() {
        val notesRoot = Files.createTempDirectory("mootool-mcp-vault-dual-json-read-notes-")
        val jsonRoot = Files.createTempDirectory("mootool-mcp-vault-dual-json-read-json-")
        val productRoot = Files.createTempDirectory("mootool-mcp-vault-dual-json-read-product-")
        try {
            notesRoot.resolve("note.md").writeText(
                "---\ntitle: n\nsyntax: text/markdown\n---\n569-notes",
            )
            jsonRoot.resolve("page.json").writeText("""{"head":"skip","tail":"569"}""")
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
                assertTrue(body.contains("569"))
                assertFalse(body.contains("skip"))
            }
        } finally {
            notesRoot.toFile().deleteRecursively()
            jsonRoot.toFile().deleteRecursively()
            productRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun subprocessDualVaultNotesReadHonorsOffsetWhenJsonGranted() {
        val notesRoot = Files.createTempDirectory("mootool-mcp-vault-dual-notes-read-notes-")
        val jsonRoot = Files.createTempDirectory("mootool-mcp-vault-dual-notes-read-json-")
        val productRoot = Files.createTempDirectory("mootool-mcp-vault-dual-notes-read-product-")
        try {
            notesRoot.resolve("page.md").writeText(
                "---\ntitle: t\nsyntax: text/markdown\n---\nskip570tail",
            )
            jsonRoot.resolve("doc.json").writeText("""{"tag":"570-json"}""")
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
                val read = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_notes_read")
                        .arguments(mapOf("path" to "page.md", "offset" to 4, "length" to 7))
                        .build(),
                )
                assertFalse(read.isError)
                val body = read.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                } ?: ""
                assertTrue(body.contains("570tail"))
                assertFalse(body.contains("skip"))
            }
        } finally {
            notesRoot.toFile().deleteRecursively()
            jsonRoot.toFile().deleteRecursively()
            productRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun subprocessDualVaultJsonSearchThenReadHonorsBodyOffset() {
        val notesRoot = Files.createTempDirectory("mootool-mcp-vault-dual-search-read-notes-")
        val jsonRoot = Files.createTempDirectory("mootool-mcp-vault-dual-search-read-json-")
        val productRoot = Files.createTempDirectory("mootool-mcp-vault-dual-search-read-product-")
        try {
            notesRoot.resolve("note.md").writeText(
                "---\ntitle: n\nsyntax: text/markdown\n---\n571-notes",
            )
            jsonRoot.resolve("alpha.json").writeText("""{"tag":"571-alpha","body":"skip571"}""")
            jsonRoot.resolve("beta.json").writeText("""{"head":"skip571","tail":"571tail"}""")
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
                val search = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_json_documents_search")
                        .arguments(mapOf("query" to "571", "limit" to 1, "offset" to 1))
                        .build(),
                )
                assertFalse(search.isError)
                val searchText = search.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                assertTrue(searchText.contains("beta.json") || searchText.contains("571-beta"))
                val read = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_json_documents_read")
                        .arguments(mapOf("path" to "beta.json", "offset" to 18, "length" to 20))
                        .build(),
                )
                assertFalse(read.isError)
                val body = read.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                assertTrue(body.contains("571tail"))
                assertFalse(body.contains("skip571"))
            }
        } finally {
            notesRoot.toFile().deleteRecursively()
            jsonRoot.toFile().deleteRecursively()
            productRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun subprocessDualVaultNotesSearchThenReadHonorsBodyOffset() {
        val notesRoot = Files.createTempDirectory("mootool-mcp-vault-dual-notes-search-read-")
        val jsonRoot = Files.createTempDirectory("mootool-mcp-vault-dual-notes-search-read-json-")
        val productRoot = Files.createTempDirectory("mootool-mcp-vault-dual-notes-search-read-product-")
        try {
            notesRoot.resolve("alpha.md").writeText(
                "---\ntitle: a\nsyntax: text/markdown\n---\nskip572alpha",
            )
            notesRoot.resolve("beta.md").writeText(
                "---\ntitle: b\nsyntax: text/markdown\n---\nskip572572tail",
            )
            jsonRoot.resolve("doc.json").writeText("""{"tag":"572-json"}""")
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
                val search = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_notes_search")
                        .arguments(mapOf("query" to "572", "limit" to 1, "offset" to 1))
                        .build(),
                )
                assertFalse(search.isError)
                val searchText = search.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                assertTrue(searchText.contains("beta.md") || searchText.contains("572tail"))
                val read = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_notes_read")
                        .arguments(mapOf("path" to "beta.md", "offset" to 7, "length" to 7))
                        .build(),
                )
                assertFalse(read.isError)
                val body = read.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                assertTrue(body.contains("572tail"))
                assertFalse(body.contains("skip572"))
            }
        } finally {
            notesRoot.toFile().deleteRecursively()
            jsonRoot.toFile().deleteRecursively()
            productRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun subprocessDualVaultJsonSearchThenNotesReadHonorsBodyOffset() {
        val notesRoot = Files.createTempDirectory("mootool-mcp-vault-dual-json-notes-read-notes-")
        val jsonRoot = Files.createTempDirectory("mootool-mcp-vault-dual-json-notes-read-json-")
        val productRoot = Files.createTempDirectory("mootool-mcp-vault-dual-json-notes-read-product-")
        try {
            notesRoot.resolve("alpha.md").writeText(
                "---\ntitle: a\nsyntax: text/markdown\n---\nskip573alpha",
            )
            notesRoot.resolve("beta.md").writeText(
                "---\ntitle: b\nsyntax: text/markdown\n---\nskip573573tail",
            )
            jsonRoot.resolve("alpha.json").writeText("""{"tag":"573-alpha"}""")
            jsonRoot.resolve("beta.json").writeText("""{"tag":"573-beta","body":"573-json"}""")
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
                val search = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_json_documents_search")
                        .arguments(mapOf("query" to "573", "limit" to 1, "offset" to 1))
                        .build(),
                )
                assertFalse(search.isError)
                val searchText = search.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                assertTrue(searchText.contains("beta.json") || searchText.contains("573-beta"))
                val read = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_notes_read")
                        .arguments(mapOf("path" to "beta.md", "offset" to 7, "length" to 7))
                        .build(),
                )
                assertFalse(read.isError)
                val body = read.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                assertTrue(body.contains("573tail"))
                assertFalse(body.contains("skip573"))
            }
        } finally {
            notesRoot.toFile().deleteRecursively()
            jsonRoot.toFile().deleteRecursively()
            productRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun subprocessDualVaultNotesSearchThenJsonReadHonorsBodyOffset() {
        val notesRoot = Files.createTempDirectory("mootool-mcp-vault-dual-notes-json-read-notes-")
        val jsonRoot = Files.createTempDirectory("mootool-mcp-vault-dual-notes-json-read-json-")
        val productRoot = Files.createTempDirectory("mootool-mcp-vault-dual-notes-json-read-product-")
        try {
            notesRoot.resolve("alpha.md").writeText(
                "---\ntitle: a\nsyntax: text/markdown\n---\nskip574alpha",
            )
            notesRoot.resolve("beta.md").writeText(
                "---\ntitle: b\nsyntax: text/markdown\n---\nskip574574tail",
            )
            jsonRoot.resolve("alpha.json").writeText("""{"tag":"574-alpha","body":"skip574"}""")
            jsonRoot.resolve("beta.json").writeText("""{"head":"skip574","tail":"574-json"}""")
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
                val search = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_notes_search")
                        .arguments(mapOf("query" to "574", "limit" to 1, "offset" to 1))
                        .build(),
                )
                assertFalse(search.isError)
                val searchText = search.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                assertTrue(searchText.contains("beta.md") || searchText.contains("574tail"))
                val read = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_json_documents_read")
                        .arguments(mapOf("path" to "beta.json", "offset" to 18, "length" to 20))
                        .build(),
                )
                assertFalse(read.isError)
                val body = read.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                assertTrue(body.contains("574-json"))
                assertFalse(body.contains("skip574"))
            }
        } finally {
            notesRoot.toFile().deleteRecursively()
            jsonRoot.toFile().deleteRecursively()
            productRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun subprocessDualVaultJsonReadThenNotesSearchHonorsBodyOffset() {
        val notesRoot = Files.createTempDirectory("mootool-mcp-vault-dual-json-read-notes-search-notes-")
        val jsonRoot = Files.createTempDirectory("mootool-mcp-vault-dual-json-read-notes-search-json-")
        val productRoot = Files.createTempDirectory("mootool-mcp-vault-dual-json-read-notes-search-product-")
        try {
            notesRoot.resolve("alpha.md").writeText(
                "---\ntitle: a\nsyntax: text/markdown\n---\n575beta-first",
            )
            notesRoot.resolve("beta.md").writeText(
                "---\ntitle: b\nsyntax: text/markdown\n---\n575beta-second",
            )
            jsonRoot.resolve("hint.json").writeText("""{"needle":"575beta","pad":"skip575"}""")
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
                val jsonRead = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_json_documents_read")
                        .arguments(mapOf("path" to "hint.json", "offset" to 11, "length" to 7))
                        .build(),
                )
                assertFalse(jsonRead.isError)
                val needle = jsonRead.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                assertTrue(needle.contains("575beta"))
                val search = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_notes_search")
                        .arguments(mapOf("query" to "575beta", "limit" to 1, "offset" to 1))
                        .build(),
                )
                assertFalse(search.isError)
                val searchText = search.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                assertTrue(searchText.contains("beta.md") || searchText.contains("575beta-second"))
                assertFalse(searchText.contains("575beta-first"))
            }
        } finally {
            notesRoot.toFile().deleteRecursively()
            jsonRoot.toFile().deleteRecursively()
            productRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun subprocessDualVaultNotesReadThenJsonSearchHonorsBodyOffset() {
        val notesRoot = Files.createTempDirectory("mootool-mcp-vault-dual-notes-read-json-search-notes-")
        val jsonRoot = Files.createTempDirectory("mootool-mcp-vault-dual-notes-read-json-search-json-")
        val productRoot = Files.createTempDirectory("mootool-mcp-vault-dual-notes-read-json-search-product-")
        try {
            notesRoot.resolve("hint.md").writeText(
                "---\ntitle: hint\nsyntax: text/markdown\n---\n576needle-pad",
            )
            jsonRoot.resolve("alpha.json").writeText("""{"tag":"576needle-first"}""")
            jsonRoot.resolve("beta.json").writeText("""{"tag":"576needle-second"}""")
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
                val notesRead = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_notes_read")
                        .arguments(mapOf("path" to "hint.md", "offset" to 0, "length" to 9))
                        .build(),
                )
                assertFalse(notesRead.isError)
                val needle = notesRead.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                assertTrue(needle.contains("576needle"))
                val search = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_json_documents_search")
                        .arguments(mapOf("query" to "576needle", "limit" to 1, "offset" to 1))
                        .build(),
                )
                assertFalse(search.isError)
                val searchText = search.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                assertTrue(searchText.contains("beta.json") || searchText.contains("576needle-second"))
                assertFalse(searchText.contains("576needle-first"))
            }
        } finally {
            notesRoot.toFile().deleteRecursively()
            jsonRoot.toFile().deleteRecursively()
            productRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun subprocessDualVaultNotesReadThenJsonReadHonorsBodyOffset() {
        val notesRoot = Files.createTempDirectory("mootool-mcp-vault-dual-notes-read-json-read-notes-")
        val jsonRoot = Files.createTempDirectory("mootool-mcp-vault-dual-notes-read-json-read-json-")
        val productRoot = Files.createTempDirectory("mootool-mcp-vault-dual-notes-read-json-read-product-")
        try {
            notesRoot.resolve("pointer.md").writeText(
                "---\ntitle: pointer\nsyntax: text/markdown\n---\n577json-doc.json",
            )
            jsonRoot.resolve("577json-doc.json").writeText("""{"prefix":"skip577","value":"577-json-read"}""")
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
                val notesRead = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_notes_read")
                        .arguments(mapOf("path" to "pointer.md", "offset" to 0, "length" to 16))
                        .build(),
                )
                assertFalse(notesRead.isError)
                val pathHint = notesRead.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                assertTrue(pathHint.contains("577json-doc.json"))
                val jsonRead = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_json_documents_read")
                        .arguments(mapOf("path" to "577json-doc.json", "offset" to 29, "length" to 13))
                        .build(),
                )
                assertFalse(jsonRead.isError)
                val body = jsonRead.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                assertTrue(body.contains("577-json-read"))
                assertFalse(body.contains("skip577"))
            }
        } finally {
            notesRoot.toFile().deleteRecursively()
            jsonRoot.toFile().deleteRecursively()
            productRoot.toFile().deleteRecursively()
        }
    }

    @Test
    fun subprocessDualVaultReadAfterSearch() {
        val notesRoot = Files.createTempDirectory("mootool-mcp-vault-dual-read-notes-")
        val jsonRoot = Files.createTempDirectory("mootool-mcp-vault-dual-read-json-")
        val productRoot = Files.createTempDirectory("mootool-mcp-vault-dual-read-product-")
        try {
            notesRoot.resolve("note.md").writeText(
                "---\ntitle: dual-read\nsyntax: text/markdown\n---\nnotes-body-566",
            )
            jsonRoot.resolve("doc.json").writeText("""{"read":"566-json"}""")
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
                val notesRead = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_notes_read")
                        .arguments(mapOf("path" to "note.md", "offset" to 0, "length" to 200))
                        .build(),
                )
                assertFalse(notesRead.isError)
                val jsonRead = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_json_documents_read")
                        .arguments(mapOf("path" to "doc.json", "offset" to 0, "length" to 200))
                        .build(),
                )
                assertFalse(jsonRead.isError)
                val notesText = notesRead.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                val jsonText = jsonRead.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                assertTrue(notesText.contains("notes-body-566"))
                assertTrue(jsonText.contains("566-json"))
            }
        } finally {
            notesRoot.toFile().deleteRecursively()
            jsonRoot.toFile().deleteRecursively()
            productRoot.toFile().deleteRecursively()
        }
    }
}
