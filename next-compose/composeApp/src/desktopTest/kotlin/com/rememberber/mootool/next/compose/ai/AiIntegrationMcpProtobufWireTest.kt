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
import kotlin.test.assertTrue

/** 子进程 stdio 调用 `mootool_protobuf_wire`（对齐 F07 Wire Tab / DIFF-554 MCP 登记）。 */
class AiIntegrationMcpProtobufWireTest {
    @Test
    fun subprocessDecodesProtobufWirePayload() {
        val productRoot = Files.createTempDirectory("mootool-mcp-protobuf-wire-")
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
                val response = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_protobuf_wire")
                        .arguments(
                            mapOf(
                                "text" to "08011203313232",
                                "format" to "hex",
                            ),
                        )
                        .build(),
                )
                assertTrue(!response.isError)
                val text = response.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                assertTrue(text.contains("field=1"), text)
            }
        } finally {
            productRoot.toFile().deleteRecursively()
        }
    }
}
