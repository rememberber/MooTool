package com.rememberber.mootool.next.compose.features.json

import com.rememberber.mootool.next.compose.domain.JsonStatus
import com.rememberber.mootool.next.compose.sessions.JsonSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonInspectorResultTest {
    private val pathTitle = "JSON Path"
    private val pathApplied = "JSONPath applied"
    private val ok = JsonStatus(JsonStatus.Kind.Valid, "OK")

    @Test
    fun performInlinePathTreeDoubleTapQuery_sets_path_result_and_panel_title() {
        val session = JsonSession()
        val input = """{"store":{"books":[{"title":"One"}]}}"""
        val translator = com.rememberber.mootool.next.compose.domain.JsonTranslator { key, _ -> key }
        session.performInlinePathTreeDoubleTapQuery(
            input = input,
            path = "$.store.books[0].title",
            previewFallback = "",
            pathAppliedNotice = pathApplied,
            queryPanelTitle = pathTitle,
            translator = translator,
        )
        assertEquals("$.store.books[0].title", session.jsonPath)
        assertEquals("\"One\"", session.pathResult)
        assertEquals(pathTitle, session.notice)
    }

    @Test
    fun performInlinePathTreeDoubleTapQuery_clears_result_on_invalid_path() {
        val session = JsonSession()
        val translator = com.rememberber.mootool.next.compose.domain.JsonTranslator { key, _ -> key }
        session.performInlinePathTreeDoubleTapQuery(
            input = "{}",
            path = "$[?(function(){return true})]",
            previewFallback = "",
            pathAppliedNotice = pathApplied,
            queryPanelTitle = pathTitle,
            translator = translator,
        )
        assertEquals("", session.pathResult)
        assertTrue(session.notice.isNotBlank())
    }

    @Test
    fun applyInlinePathTreePreview_sets_path_preview_and_notice() {
        val session = JsonSession()
        session.applyInlinePathTreePreview("$.a", "\"x\"", pathApplied)
        assertEquals("$.a", session.jsonPath)
        assertEquals("\"x\"", session.pathResult)
        assertEquals(pathApplied, session.notice)
    }

    @Test
    fun applyPathPickerChoice_sets_path_and_notice_without_query_result() {
        val session = JsonSession().apply {
            jsonPath = "$"
            pathResult = """["stale"]"""
            pathPickerOpen = true
            notice = "old"
        }
        session.applyPathPickerChoice("$.store", pathApplied)
        assertEquals("$.store", session.jsonPath)
        assertEquals("", session.pathResult)
        assertEquals(pathApplied, session.notice)
        assertFalse(session.pathPickerOpen)
    }

    @Test
    fun applyInspectorJsonPathInput_clears_stale_path_result() {
        val session = JsonSession().apply {
            jsonPath = "$.a"
            pathResult = """["x"]"""
            notice = pathApplied
        }
        session.applyInspectorJsonPathInput("$.b", pathApplied)
        assertEquals("$.b", session.jsonPath)
        assertEquals("", session.pathResult)
        assertEquals("", session.notice)
    }

    @Test
    fun prefers_pathResult_for_query_and_path_applied() {
        val query = jsonInspectorResultDisplay(
            pathResult = """["a"]""",
            notice = pathTitle,
            status = ok,
            pathAppliedNotice = pathApplied,
            jsonPathPanelTitle = pathTitle,
        )
        assertEquals("""["a"]""", query.text)

        val applied = jsonInspectorResultDisplay(
            pathResult = "42",
            notice = pathApplied,
            status = ok,
            pathAppliedNotice = pathApplied,
            jsonPathPanelTitle = pathTitle,
        )
        assertEquals("42", applied.text)
    }

    @Test
    fun prefers_notice_when_unrelated_to_jsonpath() {
        val display = jsonInspectorResultDisplay(
            pathResult = """["stale"]""",
            notice = "Saved",
            status = ok,
            pathAppliedNotice = pathApplied,
            jsonPathPanelTitle = pathTitle,
        )
        assertEquals("Saved", display.text)
    }

    @Test
    fun error_color_when_no_path_result_and_invalid() {
        val invalid = JsonStatus(JsonStatus.Kind.Error, "Invalid JSON")
        val display = jsonInspectorResultDisplay(
            pathResult = "",
            notice = "",
            status = invalid,
            pathAppliedNotice = pathApplied,
            jsonPathPanelTitle = pathTitle,
        )
        assertEquals("Invalid JSON", display.text)
        assertTrue(display.isError)

        val withPath = jsonInspectorResultDisplay(
            pathResult = "1",
            notice = "",
            status = invalid,
            pathAppliedNotice = pathApplied,
            jsonPathPanelTitle = pathTitle,
        )
        assertFalse(withPath.isError)
    }
}
