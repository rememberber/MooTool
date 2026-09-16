package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.ToolId

/**
 * Electron stores resizable column sizes as normalized ratios under storage keys.
 * Compose stores absolute dp widths per [ToolId] and pane index.
 */
object ElectronPaneSizeImport {
    private const val REFERENCE_CONTENT_WIDTH_DP = 1320f
    private const val REFERENCE_CONTENT_HEIGHT_DP = 900f

    fun mergeElectronIntoCompose(
        electron: Map<String, List<Float>>,
        current: Map<String, List<Float>>
    ): Map<String, List<Float>> {
        if (electron.isEmpty()) return current
        val result = current.toMutableMap()

        electron["json-three-pane"]?.let { ratios ->
            if (ratios.size >= 3) {
                val total = ratios.sum().takeIf { it > 0f } ?: return@let
                val vault = (ratios[0] / total * REFERENCE_CONTENT_WIDTH_DP).coerceIn(200f, 320f)
                val inspector = (ratios[2] / total * REFERENCE_CONTENT_WIDTH_DP).coerceIn(240f, 340f)
                result[ToolId.Json.id] = listOf(vault, inspector)
            }
        }
        electron["json-two-pane"]?.let { ratios ->
            if (ratios.size >= 2) {
                val total = ratios.sum().takeIf { it > 0f } ?: return@let
                val vault = (ratios[0] / total * REFERENCE_CONTENT_WIDTH_DP).coerceIn(200f, 320f)
                val inspector = existingSlot(result, current, ToolId.Json.id, 1, 280f)
                result[ToolId.Json.id] = listOf(vault, inspector)
            }
        }

        mapTwoColumnWorkspace(electron["http-workspace"], ToolId.Http.id, 180f, 320f, 220f, result, current)
        mapTwoColumnWorkspace(electron["host-workspace"], ToolId.Host.id, 170f, 360f, 220f, result, current)
        mapTwoColumnWorkspace(electron["image-library"], ToolId.Image.id, 180f, 360f, 240f, result, current)
        mapTwoColumnWorkspace(electron["translation-words"], ToolId.Translation.id, 170f, 360f, 220f, result, current)
        mapTwoColumnWorkspace(electron["ua-parser"], ToolId.UaParse.id, 300f, 480f, 360f, result, current)
        mapTwoColumnWorkspace(electron["text-diff"], ToolId.TextDiff.id, 240f, 900f, 240f, result, current)
        mapTwoColumnWorkspace(electron["network-workspace"], ToolId.Net.id, 340f, 900f, 320f, result, current)
        mapTwoColumnWorkspace(electron["regex-test"], ToolId.Regex.id, 320f, 900f, 290f, result, current)
        mapTwoColumnWorkspace(electron["reformat-file"], ToolId.Reformat.id, 260f, 900f, 260f, result, current)
        mapTwoColumnWorkspace(electron["calculator-panels"], ToolId.Calculator.id, 360f, 900f, 320f, result, current)
        mapTwoColumnWorkspace(electron["cron-builder"], ToolId.Cron.id, 460f, 900f, 340f, result, current)
        mapIoThreePane(electron["encode-panes"], ToolId.Encode.id, 240f, 700f, 120f, 280f, result)
        mapIoThreePane(electron["config-convert"], "config-convert", 240f, 700f, 110f, 280f, result)
        mapIoThreePane(electron["config-validate"], "config-validate", 240f, 700f, 110f, 280f, result)
        mapTwoColumnWorkspace(electron["settings-page"], "settings-page", 180f, 360f, 420f, result, current)
        mapTwoColumnWorkspace(electron["color-board"], ToolId.ColorBoard.id, 240f, 700f, 420f, result, current)
        mapTwoColumnWorkspace(electron["translation-editor"], "translation-editor", 280f, 900f, 280f, result, current)
        mapTwoColumnWorkspace(electron["protobuf-json"], "protobuf-json", 240f, 700f, 420f, result, current)
        mapIoThreePane(electron["protobuf-wire"], "protobuf-wire", 240f, 700f, 110f, 280f, result)
        mapIoThreePane(electron["protobuf-convert"], "protobuf-convert", 240f, 700f, 110f, 280f, result)
        mapIoThreePane(electron["crypto-symmetric"], "crypto-symmetric", 240f, 700f, 110f, 280f, result)
        mapIoThreePane(electron["crypto-base"], "crypto-base", 240f, 700f, 110f, 280f, result)
        mapTwoColumnWorkspace(electron["crypto-key-pair"], "crypto-key-pair", 260f, 900f, 260f, result, current)

        mapTwoColumnWorkspace(electron["runtime-editor-output"], "runtime-editor-output", 300f, 900f, 300f, result, current)

        electron["quick-note-tree-replace"]?.let { ratios ->
            if (ratios.size >= 3) {
                val total = ratios.sum().takeIf { it > 0f } ?: return@let
                val vault = (ratios[0] / total * REFERENCE_CONTENT_WIDTH_DP).coerceIn(200f, 320f)
                val replace = (ratios[2] / total * REFERENCE_CONTENT_WIDTH_DP).coerceIn(200f, 340f)
                result["quick-note-tree-replace"] = listOf(vault, replace)
            }
        }
        electron["quick-note-tree-no-replace"]?.let { ratios ->
            if (ratios.size >= 2) {
                val total = ratios.sum().takeIf { it > 0f } ?: return@let
                val vault = (ratios[0] / total * REFERENCE_CONTENT_WIDTH_DP).coerceIn(200f, 320f)
                val replace = existingSlot(result, current, "quick-note-tree-no-replace", 1, 240f)
                result["quick-note-tree-no-replace"] = listOf(vault, replace)
            }
        }
        electron["quick-note-no-tree-replace"]?.let { ratios ->
            if (ratios.size >= 2) {
                val total = ratios.sum().takeIf { it > 0f } ?: return@let
                val editor = (ratios[0] / total * REFERENCE_CONTENT_WIDTH_DP).coerceIn(320f, 900f)
                val replace = (ratios[1] / total * REFERENCE_CONTENT_WIDTH_DP).coerceIn(200f, 340f)
                result["quick-note-no-tree-replace"] = listOf(editor, replace)
            }
        }
        mapTwoColumnWorkspace(electron["quick-note-editor-preview"], "quick-note-editor-preview", 260f, 900f, 260f, result, current)
        mapTwoColumnWorkspace(electron["qrcode-generate"], "qrcode-generate", 300f, 900f, 280f, result, current)
        mapTwoColumnWorkspace(electron["qrcode-recognize"], "qrcode-recognize", 300f, 900f, 280f, result, current)

        return result
    }

    private fun mapIoThreePane(
        ratios: List<Float>?,
        paneKey: String,
        minLeft: Float,
        maxLeft: Float,
        minMiddle: Float,
        maxMiddle: Float,
        result: MutableMap<String, List<Float>>
    ) {
        ratios ?: return
        if (ratios.size < 3) return
        val total = ratios.sum().takeIf { it > 0f } ?: return
        val left = (ratios[0] / total * REFERENCE_CONTENT_WIDTH_DP).coerceIn(minLeft, maxLeft)
        val middle = (ratios[1] / total * REFERENCE_CONTENT_WIDTH_DP).coerceIn(minMiddle, maxMiddle)
        result[paneKey] = listOf(left, middle)
    }

    private fun mapTwoColumnWorkspace(
        ratios: List<Float>?,
        paneKey: String,
        minFirst: Float,
        maxFirst: Float,
        defaultSecond: Float,
        result: MutableMap<String, List<Float>>,
        current: Map<String, List<Float>>
    ) {
        ratios ?: return
        if (ratios.size < 2) return
        val total = ratios.sum().takeIf { it > 0f } ?: return
        val first = (ratios[0] / total * REFERENCE_CONTENT_WIDTH_DP).coerceIn(minFirst, maxFirst)
        val second = existingSlot(result, current, paneKey, 1, defaultSecond)
        result[paneKey] = listOf(first, second)
    }

    private fun existingSlot(
        result: Map<String, List<Float>>,
        current: Map<String, List<Float>>,
        paneKey: String,
        index: Int,
        fallback: Float
    ): Float = result[paneKey]?.getOrNull(index)
        ?: current[paneKey]?.getOrNull(index)
        ?: fallback
}
