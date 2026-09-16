package com.rememberber.mootool.next.compose.features.json

import com.rememberber.mootool.next.compose.domain.JsonEngine
import com.rememberber.mootool.next.compose.domain.JsonTranslator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonPathNodePreviewTest {
    private val t = JsonTranslator { key, _ -> key }

    @Test
    fun preview_uses_pretty_json_for_objects() {
        val input = """{"store":{"books":[{"title":"One"}]}}"""
        val preview = jsonPathNodePreview(input, "$.store", t)
        assertTrue(preview.contains("books"))
        assertTrue(preview.contains("\n"))
    }

    @Test
    fun preview_falls_back_when_json_invalid() {
        assertEquals("fallback", jsonPathNodePreview("{", "$", t, fallback = "fallback"))
    }

    @Test
    fun preview_string_scalar_matches_electron_formatPreview() {
        val input = """{"store":{"books":[{"title":"One"}]}}"""
        assertEquals("One", jsonPathNodePreview(input, "$.store.books[0].title", t))
    }

    @Test
    fun pathPickerPreview_empty_when_path_missing() {
        val input = """{"a":1}"""
        assertEquals("", JsonEngine.pathPickerPreview(input, "$.missing", t))
    }
}
