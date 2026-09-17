package com.rememberber.mootool.next.compose.ui.workbench

import com.rememberber.mootool.next.compose.model.ToolId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkbenchNavPresentationTest {
    @Test
    fun showDetachedPlaceholder_delegatesToDetachedToolPresentation() {
        assertTrue(WorkbenchNavPresentation.showDetachedPlaceholder(ToolId.Http, setOf(ToolId.Http)))
    }

    @Test
    fun recentToolIds_matchesDetachPolicy() {
        assertEquals(
            DetachPolicy.recentToolIds(ToolId.Json, listOf("http", "json")),
            WorkbenchNavPresentation.recentToolIds(ToolId.Json, listOf("http", "json")),
        )
    }
}
