package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.model.ToolId
import kotlin.test.Test
import kotlin.test.assertEquals

class ElectronPaneSizeImportTest {
    @Test
    fun convertsJsonThreePaneRatiosToComposeVaultAndInspectorWidths() {
        val merged = ElectronPaneSizeImport.mergeElectronIntoCompose(
            mapOf("json-three-pane" to listOf(190f, 520f, 280f)),
            emptyMap()
        )
        val json = merged[ToolId.Json.id]!!
        assertEquals(253.33333f, json[0], 0.01f)
        assertEquals(340f, json[1], 0.01f)
    }

    @Test
    fun convertsHttpHostAndQuickNoteWorkspaceRatios() {
        val merged = ElectronPaneSizeImport.mergeElectronIntoCompose(
            mapOf(
                "http-workspace" to listOf(230f, 770f),
                "host-workspace" to listOf(220f, 780f),
                "quick-note-tree-replace" to listOf(220f, 520f, 240f)
            ),
            mapOf(ToolId.Http.id to listOf(210f, 300f))
        )
        assertEquals(303.6f, merged[ToolId.Http.id]!![0], 0.5f)
        assertEquals(300f, merged[ToolId.Http.id]!![1], 0.01f)
        assertEquals(290.4f, merged[ToolId.Host.id]!![0], 0.5f)
        val quick = merged["quick-note-tree-replace"]!!
        assertEquals(296.33f, quick[0], 0.5f)
        assertEquals(323.67f, quick[1], 0.5f)
    }

    @Test
    fun convertsTextDiffAndNetworkWorkspaceRatios() {
        val merged = ElectronPaneSizeImport.mergeElectronIntoCompose(
            mapOf(
                "text-diff" to listOf(1f, 1f),
                "network-workspace" to listOf(1.15f, 0.85f)
            ),
            emptyMap()
        )
        assertEquals(660f, merged[ToolId.TextDiff.id]!![0], 0.5f)
        assertEquals(759f, merged[ToolId.Net.id]!![0], 0.5f)
    }

    @Test
    fun convertsRegexAndReformatFileRatios() {
        val merged = ElectronPaneSizeImport.mergeElectronIntoCompose(
            mapOf(
                "regex-test" to listOf(710f, 290f),
                "reformat-file" to listOf(1f, 1f)
            ),
            emptyMap()
        )
        assertEquals(900f, merged[ToolId.Regex.id]!![0], 0.5f)
        assertEquals(660f, merged[ToolId.Reformat.id]!![0], 0.5f)
    }

    @Test
    fun convertsCalculatorAndCronBuilderRatios() {
        val merged = ElectronPaneSizeImport.mergeElectronIntoCompose(
            mapOf(
                "calculator-panels" to listOf(1f, 1f),
                "cron-builder" to listOf(660f, 340f)
            ),
            emptyMap()
        )
        assertEquals(660f, merged[ToolId.Calculator.id]!![0], 0.5f)
        assertEquals(871.2f, merged[ToolId.Cron.id]!![0], 0.5f)
    }

    @Test
    fun convertsEncodeThreePaneRatios() {
        val merged = ElectronPaneSizeImport.mergeElectronIntoCompose(
            mapOf("encode-panes" to listOf(1f, 0.32f, 1f)),
            emptyMap()
        )
        val encode = merged[ToolId.Encode.id]!!
        assertEquals(568.9655f, encode[0], 0.5f)
        assertEquals(182.069f, encode[1], 0.5f)
    }

    @Test
    fun convertsSettingsPageAndConfigPaneRatios() {
        val merged = ElectronPaneSizeImport.mergeElectronIntoCompose(
            mapOf(
                "settings-page" to listOf(220f, 780f),
                "config-convert" to listOf(1f, 0.28f, 1f),
                "config-validate" to listOf(1f, 0.32f, 0.6f)
            ),
            emptyMap()
        )
        assertEquals(290.4f, merged["settings-page"]!![0], 0.5f)
        val convert = merged["config-convert"]!!
        assertEquals(578.947f, convert[0], 0.5f)
        assertEquals(162.105f, convert[1], 0.5f)
        val validate = merged["config-validate"]!!
        assertEquals(687.5f, validate[0], 0.5f)
        assertEquals(220f, validate[1], 0.5f)
    }

    @Test
    fun convertsColorBoardAndTranslationEditorRatios() {
        val merged = ElectronPaneSizeImport.mergeElectronIntoCompose(
            mapOf(
                "color-board" to listOf(0.34f, 0.66f),
                "translation-editor" to listOf(1f, 1f)
            ),
            emptyMap()
        )
        assertEquals(448.8f, merged[ToolId.ColorBoard.id]!![0], 0.5f)
        assertEquals(660f, merged["translation-editor"]!![0], 0.5f)
    }

    @Test
    fun convertsRuntimeEditorOutputRatios() {
        val merged = ElectronPaneSizeImport.mergeElectronIntoCompose(
            mapOf("runtime-editor-output" to listOf(1f, 1f)),
            emptyMap()
        )
        assertEquals(660f, merged["runtime-editor-output"]!![0], 0.5f)
    }

    @Test
    fun convertsQrcodeAndQuickNoteEditorPreviewRatios() {
        val merged = ElectronPaneSizeImport.mergeElectronIntoCompose(
            mapOf(
                "qrcode-generate" to listOf(1f, 1f),
                "quick-note-editor-preview" to listOf(0.4f, 0.6f)
            ),
            emptyMap()
        )
        assertEquals(660f, merged["qrcode-generate"]!![0], 0.5f)
        assertEquals(528f, merged["quick-note-editor-preview"]!![0], 0.5f)
    }

    @Test
    fun convertsProtobufAndCryptoPaneRatios() {
        val merged = ElectronPaneSizeImport.mergeElectronIntoCompose(
            mapOf(
                "protobuf-json" to listOf(0.34f, 0.66f),
                "protobuf-wire" to listOf(1f, 0.28f, 1f),
                "crypto-symmetric" to listOf(1f, 0.28f, 1f),
                "crypto-key-pair" to listOf(1f, 1f)
            ),
            emptyMap()
        )
        assertEquals(448.8f, merged["protobuf-json"]!![0], 0.5f)
        assertEquals(578.947f, merged["protobuf-wire"]!![0], 0.5f)
        assertEquals(660f, merged["crypto-key-pair"]!![0], 0.5f)
    }

    @Test
    fun mergeIntoSettingsUsesConvertedJsonPaneWidths() {
        val imported = AppSettings.Default.copy(
            layout = AppSettings.Default.layout.copy(
                paneSizes = mapOf("json-three-pane" to listOf(0.19f, 0.52f, 0.28f))
            )
        )
        val merged = ElectronNextSettingsImport.mergeInto(AppSettings.Default, imported)
        val json = merged.layout.paneSizes[ToolId.Json.id]!!
        assertEquals(253.33333f, json[0], 0.01f)
        assertEquals(340f, json[1], 0.01f)
    }
}
