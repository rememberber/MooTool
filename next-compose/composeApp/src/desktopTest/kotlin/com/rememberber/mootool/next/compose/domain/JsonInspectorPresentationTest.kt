package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonInspectorPresentationTest {
    @Test
    fun inferSchemaEnabled_requiresAnalysis() {
        assertFalse(JsonInspectorPresentation.inferSchemaEnabled(null))
        assertTrue(
            JsonInspectorPresentation.inferSchemaEnabled(
                JsonAnalysis(rootType = "object", nodes = 1, keys = 1, maxDepth = 1, bytes = 2),
            ),
        )
    }

    @Test
    fun showDuplicatePaths_whenCountPositive() {
        assertFalse(JsonInspectorPresentation.showDuplicatePaths(0))
        assertTrue(JsonInspectorPresentation.showDuplicatePaths(2))
    }
}
