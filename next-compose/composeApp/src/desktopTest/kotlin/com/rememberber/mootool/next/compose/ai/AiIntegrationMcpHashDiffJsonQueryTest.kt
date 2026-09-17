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

/** 子进程 stdio 调用余下 `mootool_hash` / `mootool_diff` / `mootool_json_query`（对齐 `MooToolMcpToolsTest`）。 */
class AiIntegrationMcpHashDiffJsonQueryTest {
    @Test
    fun subprocessHashDiffAndJsonQuery() {
        val productRoot = Files.createTempDirectory("mootool-mcp-hash-diff-query-")
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
                val hash = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_hash")
                        .arguments(mapOf("text" to "abc"))
                        .build(),
                )
                assertTrue(!hash.isError)
                val hashText = hash.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                assertEquals(
                    "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                    hashText,
                )

                val diff = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_diff")
                        .arguments(mapOf("left" to "one\n", "right" to "two\n"))
                        .build(),
                )
                assertTrue(!diff.isError)
                val diffText = diff.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                assertTrue(diffText.contains("-") || diffText.contains("one") || diffText.isNotBlank(), diffText)

                val query = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_json_query")
                        .arguments(
                            mapOf(
                                "text" to """{"values":[1,2]}""",
                                "path" to "$.values[*]",
                            ),
                        )
                        .build(),
                )
                assertTrue(!query.isError)
                val queryText = query.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                assertTrue(queryText.contains("1") && queryText.contains("2"), queryText)
            }
        } finally {
            productRoot.toFile().deleteRecursively()
        }
    }
}
