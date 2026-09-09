package com.rememberber.mootool.next.compose.app

import com.rememberber.mootool.next.compose.model.ToolId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToolRegistryTest {
    @Test
    fun registryHasHomeAndTwentyFiveTools() {
        assertEquals(26, ToolRegistry.tools.size)
        assertEquals(ToolId.ordered, ToolRegistry.tools.map { it.id })
        assertEquals(6, ToolRegistry.groups.size)
        assertTrue(ToolRegistry.byId.getValue(ToolId.Json).status.name == "Available")
        assertTrue(ToolRegistry.byId.getValue(ToolId.TimeConvert).status.name == "Available")
        assertTrue(ToolRegistry.byId.getValue(ToolId.Calculator).status.name == "Available")
        assertTrue(ToolRegistry.byId.getValue(ToolId.Encode).status.name == "Available")
        assertTrue(ToolRegistry.byId.getValue(ToolId.UaParse).status.name == "Available")
        assertTrue(ToolRegistry.byId.getValue(ToolId.Regex).status.name == "Available")
        assertTrue(ToolRegistry.byId.getValue(ToolId.Cron).status.name == "Available")
        assertTrue(ToolRegistry.byId.getValue(ToolId.TextDiff).status.name == "Available")
    }

    @Test
    fun searchMatchesLocalizedKeywordsAndHiddenToolsStillSearchable() {
        val results = ToolRegistry.search("jsonpath") { it }
        assertEquals(listOf(ToolId.Json), results.map { it.id })
        val home = ToolRegistry.search("首页") { key -> if (key == "app.nav.home") "主页" else key }
        assertTrue(home.any { it.id == ToolId.Mootool })
    }
}
