package com.rememberber.mootool.next.compose.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class McpToolCatalogTest {
    @Test
    fun registersElevenToolsWithSchemasAndNamesAlignedToHandlers() {
        val registrations = McpToolCatalog.registrations()
        assertEquals(12, registrations.size)
        assertEquals(
            MooToolMcpTools.toolNames().toSet() + VaultMcpTools.toolNames.toSet(),
            registrations.map { it.name }.toSet(),
        )
        registrations.forEach { reg ->
            assertTrue(reg.description.length > reg.name.length)
            assertTrue(reg.inputSchemaJson.contains("\"type\":\"object\""))
            assertTrue(reg.annotations().readOnlyHint() == true)
            assertTrue(reg.annotations().destructiveHint() == false)
            assertTrue(reg.annotations().openWorldHint() == false)
        }
        assertEquals(4, registrations.count { it.idempotentHint })
        assertEquals(8, registrations.count { !it.idempotentHint })
    }
}
