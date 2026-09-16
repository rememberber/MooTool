package com.rememberber.mootool.next.compose.ai

import com.rememberber.mootool.next.compose.app.ProductIdentity
import io.modelcontextprotocol.json.McpJsonDefaults
import io.modelcontextprotocol.server.McpServer
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider
import io.modelcontextprotocol.spec.McpSchema
import java.nio.file.Path

object McpBootstrap {
    fun run(args: Array<String>) {
        val accessFile = parseAccessFile(args)
        val mapper = McpJsonDefaults.getMapper()
        val transport = StdioServerTransportProvider(mapper)
        val builder = McpServer.sync(transport).serverInfo("MooTool", ProductIdentity.VERSION)
        McpToolCatalog.registrations().forEach { registration ->
            val tool = McpSchema.Tool.builder()
                .name(registration.name)
                .description(registration.description)
                .inputSchema(mapper, registration.inputSchemaJson)
                .annotations(registration.annotations())
                .build()
            builder.tool(tool) { _, arguments ->
                val name = registration.name
                val map = arguments?.let { parseArguments(it) } ?: emptyMap()
                val result = when {
                    VaultMcpTools.isVaultTool(name) -> VaultMcpTools.call(name, map, accessFile)
                    else -> MooToolMcpTools.call(name, map)
                }
                McpSchema.CallToolResult.builder()
                    .isError(result.isError)
                    .content(listOf(McpSchema.TextContent(result.text)))
                    .build()
            }
        }
        builder.build()
        Thread.currentThread().join()
    }

    fun parseAccessFile(args: Array<String>): Path? {
        val index = args.indexOf("--access-file")
        return if (index >= 0 && index + 1 < args.size) Path.of(args[index + 1]) else null
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseArguments(node: Any): Map<String, Any?> = when (node) {
        is Map<*, *> -> node as Map<String, Any?>
        else -> emptyMap()
    }
}
