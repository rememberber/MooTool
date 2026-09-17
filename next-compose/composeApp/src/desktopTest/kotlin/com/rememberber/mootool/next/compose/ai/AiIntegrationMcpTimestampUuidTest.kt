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
import kotlinx.serialization.json.Json

/** 子进程 stdio 调用 `mootool_timestamp` / `mootool_uuid`（对齐 Electron `timeTools` / `uuidTools`）。 */
class AiIntegrationMcpTimestampUuidTest {
    @Test
    fun subprocessTimestampAndUuid() {
        val productRoot = Files.createTempDirectory("mootool-mcp-ts-uuid-")
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
                val ts = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_timestamp")
                        .arguments(
                            mapOf(
                                "text" to "1970-01-01 08:00:00",
                                "direction" to "to-timestamp",
                                "zone" to "Asia/Shanghai",
                            ),
                        )
                        .build(),
                )
                assertTrue(!ts.isError)
                val tsText = ts.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                assertEquals("0", tsText)

                val uuids = client.callTool(
                    McpSchema.CallToolRequest.builder()
                        .name("mootool_uuid")
                        .arguments(mapOf("count" to 2))
                        .build(),
                )
                assertTrue(!uuids.isError)
                val uuidText = uuids.content.firstOrNull()?.let {
                    if (it is McpSchema.TextContent) it.text else null
                }.orEmpty()
                val list = Json.decodeFromString<List<String>>(uuidText)
                assertEquals(2, list.size)
                assertEquals(2, list.toSet().size)
            }
        } finally {
            productRoot.toFile().deleteRecursively()
        }
    }
}
