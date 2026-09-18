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
    fun inferSchemaAndDuplicatePathActionEnabled() {
        val analysis = JsonAnalysis(rootType = "object", nodes = 1, keys = 1, maxDepth = 1, bytes = 2)
        assertFalse(JsonInspectorPresentation.inferSchemaActionEnabled(null))
        assertTrue(JsonInspectorPresentation.inferSchemaActionEnabled(analysis))
        assertFalse(JsonInspectorPresentation.duplicatePathClickActionEnabled(""))
        assertTrue(JsonInspectorPresentation.duplicatePathClickActionEnabled("$.a"))
    }

    @Test
    fun showDuplicatePaths_whenCountPositive() {
        assertFalse(JsonInspectorPresentation.showDuplicatePaths(0))
        assertTrue(JsonInspectorPresentation.showDuplicatePaths(2))
        assertTrue(JsonInspectorPresentation.showDuplicatePathList(2))
    }

    @Test
    fun pathCopyEnabledRequiresNonBlankPath() {
        assertFalse(JsonInspectorPresentation.pathCopyEnabled(""))
        assertTrue(JsonInspectorPresentation.pathCopyEnabled("$.books[0]"))
        assertFalse(JsonInspectorPresentation.pathCopyActionEnabled("  "))
        assertTrue(JsonInspectorPresentation.pathCopyActionEnabled("$.books[0]"))
    }

    @Test
    fun pathQueryActionEnabledRequiresNonBlankTrimmedPath() {
        assertFalse(JsonInspectorPresentation.pathQueryActionEnabled(""))
        assertFalse(JsonInspectorPresentation.pathQueryActionEnabled("   "))
        assertTrue(JsonInspectorPresentation.pathQueryActionEnabled("$"))
        assertTrue(JsonInspectorPresentation.pathQueryActionEnabled(" $.books[0] "))
    }

    @Test
    fun formatAdvancedAndPathPickerOpenActionEnabled() {
        val analysis = JsonAnalysis(rootType = "object", nodes = 1, keys = 1, maxDepth = 1, bytes = 2)
        assertFalse(JsonInspectorPresentation.formatAdvancedActionEnabled(null))
        assertTrue(JsonInspectorPresentation.formatAdvancedActionEnabled(analysis))
        assertFalse(JsonInspectorPresentation.pathPickerOpenActionEnabled(0))
        assertTrue(JsonInspectorPresentation.pathPickerOpenActionEnabled(1))
    }

    @Test
    fun inspectorConvertActionEnabled() {
        val analysis = JsonAnalysis(rootType = "object", nodes = 1, keys = 1, maxDepth = 1, bytes = 2)
        assertFalse(JsonInspectorPresentation.jsonStructureConvertActionEnabled(null))
        assertTrue(JsonInspectorPresentation.jsonStructureConvertActionEnabled(analysis))
        assertFalse(JsonInspectorPresentation.editorTextConvertActionEnabled(""))
        assertTrue(JsonInspectorPresentation.editorTextConvertActionEnabled("{}"))
        assertTrue(JsonInspectorPresentation.conversionDialogActionEnabled())
    }

    @Test
    fun structurePanel_whenAnalysisPresent() {
        assertFalse(JsonInspectorPresentation.structurePanelVisible(null))
        assertTrue(
            JsonInspectorPresentation.structurePanelVisible(
                JsonAnalysis(rootType = "object", nodes = 1, keys = 1, maxDepth = 1, bytes = 2),
            ),
        )
    }

    @Test
    fun runCopyJsonPathEmptySkipsCopy() {
        assertTrue(
            JsonInspectorPresentation.runCopyJsonPath("  ", { true })
                is JsonInspectorPresentation.CopyJsonPathOutcome.Empty,
        )
    }

    @Test
    fun runCopyJsonPathSuccessAndFailure() {
        assertTrue(
            JsonInspectorPresentation.runCopyJsonPath("$.a", { true })
                is JsonInspectorPresentation.CopyJsonPathOutcome.Success,
        )
        assertTrue(
            JsonInspectorPresentation.runCopyJsonPath("$.a", { false })
                is JsonInspectorPresentation.CopyJsonPathOutcome.Failure,
        )
    }

    @Test
    fun shouldToastPathCopy() {
        assertTrue(JsonInspectorPresentation.shouldToastPathCopySuccess())
        assertTrue(JsonInspectorPresentation.shouldToastPathCopyFailure())
    }

    @Test
    fun shouldToastPathQueryFailure() {
        assertTrue(JsonInspectorPresentation.shouldToastPathQueryFailure())
    }

    @Test
    fun resultCopyEnabledRequiresNonBlankDisplay() {
        assertFalse(JsonInspectorPresentation.resultCopyEnabled(""))
        assertFalse(JsonInspectorPresentation.resultCopyEnabled("   "))
        assertTrue(JsonInspectorPresentation.resultCopyEnabled("""{"a":1}"""))
        assertFalse(JsonInspectorPresentation.resultCopyActionEnabled(""))
        assertTrue(JsonInspectorPresentation.resultCopyActionEnabled("""{"a":1}"""))
    }

    @Test
    fun runCopyResultTextEmptySkipsCopy() {
        assertTrue(
            JsonInspectorPresentation.runCopyResultText("  ", { true })
                is JsonInspectorPresentation.CopyJsonPathOutcome.Empty,
        )
    }

    @Test
    fun shouldToastResultCopy() {
        assertTrue(JsonInspectorPresentation.shouldToastResultCopySuccess())
        assertTrue(JsonInspectorPresentation.shouldToastResultCopyFailure())
    }
}
