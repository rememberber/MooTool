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

/** 子进程 stdio 调用 `mootool_encode` url 往返（对齐 F13 / DIFF-549 进程内单测）。 */
class AiIntegrationMcpEncodeTest {
    @Test
    fun subprocessEncodesAndDecodesUrlText() {
        val productRoot = Files.createTempDirectory("mootool-mcp-encode-")
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
                val encoded = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_encode")
                        .arguments(
                            mapOf(
                                "text" to "a b",
                                "format" to "url",
                                "direction" to "encode",
                                "charset" to "utf-8",
                            ),
                        )
                        .build(),
                )
                assertTrue(!encoded.isError)
                val encodedText = encoded.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                assertTrue(encodedText.isNotBlank(), encodedText)

                val decoded = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_encode")
                        .arguments(
                            mapOf(
                                "text" to encodedText,
                                "format" to "url",
                                "direction" to "decode",
                                "charset" to "utf-8",
                            ),
                        )
                        .build(),
                )
                assertTrue(!decoded.isError)
                val plain = decoded.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                assertEquals("a b", plain)
            }
        } finally {
            productRoot.toFile().deleteRecursively()
        }
    }
}
