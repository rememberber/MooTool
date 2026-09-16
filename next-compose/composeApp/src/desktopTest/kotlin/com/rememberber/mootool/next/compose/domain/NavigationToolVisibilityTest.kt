package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.ToolId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NavigationToolVisibilityTest {
    @Test
    fun navigationToolIdsMatchTwentyFiveToolsExcludingHome() {
        val ids = NavigationToolVisibility.navigationToolIds
        assertEquals(25, ids.size)
        assertTrue(ToolId.Json.id in ids)
        assertTrue(ToolId.Mootool.id !in ids)
    }

    @Test
    fun showAllClearsHiddenList() {
        assertEquals(emptyList(), NavigationToolVisibility.showAll())
    }

    @Test
    fun hideAllListsEveryNavigationTool() {
        assertEquals(NavigationToolVisibility.navigationToolIds, NavigationToolVisibility.hideAll())
    }

    @Test
    fun normalizeHiddenNavigationToolIdsMatchesElectronContract() {
        val normalized = NavigationToolVisibility.normalizeHiddenNavigationToolIds(
            listOf("json", "json", ToolId.Mootool.id, "not-a-tool", "qrCode")
        )
        assertEquals(listOf("json", "qrCode"), normalized)
    }

    @Test
    fun visibleNavigationToolCountIgnoresDuplicateHiddenEntries() {
        val hidden = listOf("json", "json", "qrCode")
        assertEquals(23, NavigationToolVisibility.visibleNavigationToolCount(hidden))
    }
}
