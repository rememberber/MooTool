package com.rememberber.mootool.next.compose.features.json

import com.rememberber.mootool.next.compose.domain.JsonPathEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonInspectorPathUiTest {
    @Test
    fun pathPickerSelectionForOpen_follows_current_jsonPath() {
        val paths = listOf("$", "$.a", "$.b")
        assertEquals("$.b", jsonPathPickerSelectionForOpen("$.b", paths))
        assertEquals("$.b", jsonPathPickerSelectionForOpen("$.b", paths))
    }

    @Test
    fun quickPickerMenuLabel_indents_by_depth() {
        val entry = JsonPathEntry("$.store.book", "book", "{}", 2)
        assertEquals("· · book", jsonPathQuickPickerMenuLabel(entry))
    }

    @Test
    fun quickPickerButtonLabel_prefers_listed_entry() {
        val entries = listOf(
            JsonPathEntry("$", "root", "{}", 0),
            JsonPathEntry("$.name", "name", "x", 1),
        )
        assertEquals("· name", jsonPathQuickPickerButtonLabel("$.name", entries))
        assertEquals("$.custom", jsonPathQuickPickerButtonLabel("$.custom", entries))
        assertEquals("—", jsonPathQuickPickerButtonLabel("", entries))
    }
}
