package com.rememberber.mootool.next.compose.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AiClientConfigMergeTest {
    private val launch = McpLaunch(
        command = "C:\\Apps\\MooTool\\MooTool.exe",
        args = listOf("C:\\Apps\\MooTool\\mcp.js"),
        env = mapOf("ELECTRON_RUN_AS_NODE" to "1")
    )

    @Test
    fun mergeTomlPreservesExistingContent() {
        val source = "# keep this\nmodel = \"custom\"\n[mcp_servers.other]\ncommand = \"other\"\n"
        val next = AiClientConfigMerge.mergeToml(source, launch)
        assertTrue(next.startsWith(source))
        assertEquals(next, AiClientConfigMerge.mergeToml(next, launch))
    }

    @Test
    fun mergeJsonCreatesMcpServersEntry() {
        val source = "{}"
        val value = ClaudeMcpLaunch(command = launch.command, args = launch.args, env = launch.env)
        val next = AiClientConfigMerge.mergeJson(source, value, true)
        assertTrue(next.contains("mootool"))
        assertEquals(next, AiClientConfigMerge.mergeJson(next, value, true))
    }

    @Test
    fun mergeJsonPreservesJsoncComments() {
        val source = "{\n  // preserve me\n  \"custom\": true,\n  \"mcpServers\": {\"other\": {\"command\": \"existing\"},},\n}"
        val value = ClaudeMcpLaunch(command = launch.command, args = launch.args, env = launch.env)
        val next = AiClientConfigMerge.mergeJson(source, value, true)
        assertTrue(next.contains("// preserve me"))
        assertEquals(next, AiClientConfigMerge.mergeJson(next, value, true))
    }

    @Test
    fun mergeJsonRejectsMalformedAndDuplicateKeys() {
        val value = ClaudeMcpLaunch(command = launch.command, args = launch.args, env = launch.env)
        val source = "{\n  // preserve me\n  \"custom\": true,\n  \"mcpServers\": {\"other\": {\"command\": \"existing\"},},\n}"
        assertFailsWith<IllegalArgumentException> { AiClientConfigMerge.mergeJson("[]", value, true) }
        assertFailsWith<IllegalArgumentException> { AiClientConfigMerge.mergeJson("""{"mcpServers":null}""", value, true) }
        assertFailsWith<IllegalArgumentException> { AiClientConfigMerge.mergeJson("""{"mcpServers":1}""", value, true) }
        assertFailsWith<IllegalArgumentException> { AiClientConfigMerge.mergeJson("{bad}", value, true) }
        assertFailsWith<IllegalArgumentException> {
            AiClientConfigMerge.mergeJson("""{"mcpServers":{},"mcpServers":{}}""", value, true)
        }
        assertFailsWith<IllegalArgumentException> {
            AiClientConfigMerge.mergeJson("""{"nested":{"a":1,"a":2}}""", value, true)
        }
        assertFailsWith<IllegalArgumentException> { AiClientConfigMerge.mergeJson(source, value, false) }
        assertFailsWith<IllegalArgumentException> {
            AiClientConfigMerge.mergeJson("""{"mcpServers":{"mootool":{"command":"custom"}}}""", value, true)
        }
    }

    @Test
    fun manageServerUninstallPreservesUnrelatedToml() {
        val source = "# keep this\nmodel = \"custom\"\n[mcp_servers.other]\ncommand = \"other\"\n"
        val installed = AiClientConfigMerge.manageServer(source, true, launch, null)
        val receipt = ReceiptMcp(launch.command, launch.args, launch.env)
        val removed = AiClientConfigMerge.manageServer(installed, true, null, receipt)
        assertTrue(removed.startsWith(source))
        assertEquals(null, AiClientConfigMerge.readServer(removed, true))
    }

    @Test
    fun manageServerRejectsOutsideChanges() {
        val value = ClaudeMcpLaunch(command = launch.command, args = launch.args, env = launch.env)
        val receipt = ReceiptMcp(value.command, value.args, value.env, value.type)
        val tampered = AiClientConfigMerge.mergeJson(
            "{}",
            value.copy(command = "${value.command}-tampered"),
            true
        )
        assertFailsWith<IllegalArgumentException> {
            AiClientConfigMerge.manageServer(tampered, false, value, receipt)
        }
    }

    @Test
    fun readServerMatchesMergedClaudeLaunch() {
        val launch = ClaudeMcpLaunch(command = "/usr/bin/java", args = listOf("--mcp", "--access-file", "/tmp/access.json"))
        val merged = AiClientConfigMerge.mergeJson("{}", launch, true)
        val existing = AiClientConfigMerge.readServer(merged, false)
        assertTrue(AiClientConfigMerge.serverEquals(existing, launch, false))
    }
}
