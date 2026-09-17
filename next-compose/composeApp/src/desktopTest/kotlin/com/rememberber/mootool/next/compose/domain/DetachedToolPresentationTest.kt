package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.ToolId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DetachedToolPresentationTest {
    @Test
    fun windowTitle_matchesElectronPattern() {
        assertEquals("JSON · MooTool Next Compose", DetachedToolPresentation.windowTitle("JSON", "MooTool Next Compose"))
    }

    @Test
    fun showMainWindowPlaceholder_respectsDetachable() {
        assertFalse(DetachedToolPresentation.showMainWindowPlaceholder(ToolId.Mootool, setOf(ToolId.Json)))
        assertTrue(DetachedToolPresentation.showMainWindowPlaceholder(ToolId.Json, setOf(ToolId.Json)))
        assertFalse(DetachedToolPresentation.showMainWindowPlaceholder(ToolId.Json, emptySet()))
    }
}
