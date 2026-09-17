package com.rememberber.mootool.next.compose.ai

import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.transport.ServerParameters
import io.modelcontextprotocol.client.transport.StdioClientTransport
import io.modelcontextprotocol.json.McpJsonDefaults
import java.nio.file.Files
import java.time.Duration
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 对照 Electron `server.test.ts`：`listTools` 12 项 MooTool 工具且 `readOnlyHint` + `inputSchema.type === object`。 */
class AiIntegrationMcpListToolsTest {
    @Test
    fun subprocessListsToolsWithSchemasAndReadOnlyHints() {
        val productRoot = Files.createTempDirectory("mootool-mcp-list-tools-")
        try {
            val directories = com.rememberber.mootool.next.compose.app.AppPaths
                .resolve(productRoot.toString())
                .also { it.ensureCreated() }
            val accessFile = McpLaunchResolver.accessFile(directories).also { path ->
                path.parent.createDirectories()
                path.writeText("""{"version":1}""")
            }
            val launch = desktopTestMcpLaunch(accessFile)
            val transport = StdioClientTransport(
                ServerParameters.builder(launch.command).args(launch.args).build(),
                McpJsonDefaults.getMapper(),
            )
            McpClient.sync(transport).requestTimeout(Duration.ofSeconds(15)).build().use { client ->
                client.initialize()
                val tools = client.listTools().tools
                assertEquals(12, tools.size)
                assertEquals(McpToolCatalog.registrations().map { it.name }.toSet(), tools.map { it.name }.toSet())
                tools.forEach { tool ->
                    assertTrue(tool.description().length > tool.name().length)
                    assertEquals("object", tool.inputSchema().type())
                    assertTrue(tool.annotations()?.readOnlyHint() == true)
                }
            }
        } finally {
            productRoot.toFile().deleteRecursively()
        }
    }
}
