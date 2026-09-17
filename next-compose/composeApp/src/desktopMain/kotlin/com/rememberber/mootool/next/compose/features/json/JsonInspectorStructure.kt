package com.rememberber.mootool.next.compose.features.json

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.domain.JsonAnalysis
import com.rememberber.mootool.next.compose.domain.JsonEngine
import com.rememberber.mootool.next.compose.ui.theme.MooColors
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.components.mooFocusClickable
import androidx.compose.ui.text.font.FontFamily

internal fun jsonInspectorDuplicateKeys(input: String, ignoreCase: Boolean): List<String> =
    runCatching { JsonEngine.findDuplicateKeys(input, ignoreCase) }.getOrDefault(emptyList())

internal fun jsonInspectorStructureMetricText(value: Int?): String = value?.toString() ?: "—"

internal fun jsonInspectorStructureMetricText(value: String?): String =
    if (value.isNullOrBlank()) "—" else value

internal fun jsonInspectorUtf8ByteLabel(bytes: Int?): String =
    if (bytes == null) "—" else "$bytes B"

/** Inspector「生成 JSON Schema」与 Electron 一致：仅结构解析成功时可用。 */
internal fun jsonInspectorInferSchemaEnabled(analysis: JsonAnalysis?): Boolean = analysis != null

@Composable
internal fun JsonInspectorStructurePanel(
    analysis: JsonAnalysis?,
    duplicates: List<String>,
    rootTypeLabel: String,
    nodesLabel: String,
    keysLabel: String,
    maxDepthLabel: String,
    duplicatesLabel: String,
    utf8Label: String,
    onDuplicatePathClick: (String) -> Unit = {},
) {
    val colors = MooTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        JsonInspectorStructureRow(
            rootTypeLabel,
            jsonInspectorStructureMetricText(analysis?.rootType),
            colors.textPrimary,
            colors,
        )
        JsonInspectorStructureRow(
            nodesLabel,
            jsonInspectorStructureMetricText(analysis?.nodes),
            colors.textPrimary,
            colors,
        )
        JsonInspectorStructureRow(
            keysLabel,
            jsonInspectorStructureMetricText(analysis?.keys),
            colors.textPrimary,
            colors,
        )
        JsonInspectorStructureRow(
            maxDepthLabel,
            jsonInspectorStructureMetricText(analysis?.maxDepth),
            colors.textPrimary,
            colors,
        )
        val duplicateCount = duplicates.size
        JsonInspectorStructureRow(
            duplicatesLabel,
            duplicateCount.toString(),
            if (duplicateCount > 0) colors.danger else colors.textPrimary,
            colors,
        )
        JsonInspectorStructureRow(
            utf8Label,
            jsonInspectorUtf8ByteLabel(analysis?.bytes),
            colors.textPrimary,
            colors,
        )
        if (duplicates.isNotEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                duplicates.forEach { path ->
                    Text(
                        path,
                        color = colors.danger,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.mooFocusClickable { onDuplicatePathClick(path) },
                    )
                }
            }
        }
    }
}

@Composable
private fun JsonInspectorStructureRow(
    label: String,
    value: String,
    valueColor: Color,
    colors: MooColors,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = colors.textSecondary, fontSize = 11.sp)
        Text(value, color = valueColor, fontSize = 11.sp)
    }
}
