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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 对照 Electron `server.test.ts` 首条用例：stdio 协商、`listTools` 与 `mootool_json_format` 成功/失败。 */
class AiIntegrationMcpStdioNegotiationTest {
    @Test
    fun subprocessNegotiatesListToolsAndJsonFormatRoundtrip() {
        val productRoot = Files.createTempDirectory("mootool-mcp-negotiation-")
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
                val init = client.initialize()
                assertEquals("MooTool", init.serverInfo()?.name())
                var tools = client.listTools().tools
                assertEquals(12, tools.size)
                assertTrue(tools.all { it.inputSchema().type() == "object" })
                assertTrue(tools.all { it.annotations()?.readOnlyHint() == true })

                val formatted = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_json_format")
                        .arguments(
                            mapOf(
                                "text" to """{"b":2,"a":1}""",
                                "sortKeys" to true,
                                "spaces" to 0,
                            ),
                        )
                        .build(),
                )
                assertTrue(!formatted.isError)
                val formattedText = formatted.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }
                assertEquals("""{"a":1,"b":2}""", formattedText)

                val failure = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_json_format")
                        .arguments(mapOf("text" to "{bad}"))
                        .build(),
                )
                assertTrue(failure.isError)

                tools = client.listTools().tools
                assertEquals(12, tools.size)
            }
        } finally {
            productRoot.toFile().deleteRecursively()
        }
    }
}
