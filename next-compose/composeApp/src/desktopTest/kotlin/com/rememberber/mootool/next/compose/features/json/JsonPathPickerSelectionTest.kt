package com.rememberber.mootool.next.compose.features.json

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonPathPickerSelectionTest {
    @Test
    fun initialSelection_prefers_current_path_when_listed() {
        assertEquals("$.a", jsonPathPickerInitialSelection("$.a", listOf("$", "$.a", "$.b")))
    }

    @Test
    fun initialSelection_falls_back_to_root_or_first() {
        assertEquals("$", jsonPathPickerInitialSelection("$.missing", listOf("$", "$.a")))
        assertEquals("$.custom", jsonPathPickerInitialSelection("$.custom", emptyList()))
    }
}
