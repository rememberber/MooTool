package com.rememberber.mootool.next.compose.ui.workbench

import com.rememberber.mootool.next.compose.i18n.Translator
import com.rememberber.mootool.next.compose.model.AppLanguage
import com.rememberber.mootool.next.compose.model.ToolId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DetachPolicyTest {
    @Test
    fun openingDetachedToolRequestsExistingWindowFocus() {
        assertFalse(DetachPolicy.shouldRequestDetachedFocus(ToolId.Json, emptySet()))
        assertTrue(DetachPolicy.shouldRequestDetachedFocus(ToolId.Json, setOf(ToolId.Json)))
        assertFalse(DetachPolicy.shouldRequestDetachedFocus(ToolId.Mootool, setOf(ToolId.Json)))
        assertFalse(DetachPolicy.shouldRequestDetachedFocus(ToolId.Http, setOf(ToolId.Json)))
    }

    @Test
    fun sidebarWindowActionFollowsElectronHideTitlesRule() {
        assertTrue(DetachPolicy.showSidebarWindowAction(collapsed = false, hideTitles = false, detachable = true))
        assertFalse(DetachPolicy.showSidebarWindowAction(collapsed = true, hideTitles = false, detachable = true))
        assertFalse(DetachPolicy.showSidebarWindowAction(collapsed = false, hideTitles = true, detachable = true))
        assertFalse(DetachPolicy.showSidebarWindowAction(collapsed = false, hideTitles = false, detachable = false))
    }

    @Test
    fun recentListExcludesHomeAndDedupesCurrent() {
        assertEquals(listOf("json", "http"), DetachPolicy.recentToolIds(ToolId.Mootool, listOf("json", "http", "mootool")))
        assertEquals(listOf("json", "http"), DetachPolicy.recentToolIds(ToolId.Json, listOf("http", "json", "mootool")))
        assertEquals(
            listOf("http", "json", "host", "net", "encode"),
            DetachPolicy.recentToolIds(ToolId.Http, listOf("json", "host", "net", "encode", "crypto"))
        )
    }

    @Test
    fun detachedCopySubstitutesToolName() {
        val zh = Translator(AppLanguage.ZhCN)
        assertEquals("JSON 已在独立窗口中打开", zh.t("app.tool.detachedTitle", mapOf("tool" to "JSON")))
        val en = Translator(AppLanguage.EnUS)
        assertEquals("JSON is open in a separate window", en.t("app.tool.detachedTitle", mapOf("tool" to "JSON")))
        val ja = Translator(AppLanguage.JaJP)
        assertEquals("JSON は別ウィンドウで開いています", ja.t("app.tool.detachedTitle", mapOf("tool" to "JSON")))
    }
}
