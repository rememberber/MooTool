package com.rememberber.mootool.next.compose.features.json

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.domain.JsonPathEntry
import com.rememberber.mootool.next.compose.ui.theme.MooTheme

/** 检查器内联路径树最多展示节点数，避免超大 JSON 拖垮侧栏。 */
const val JSON_INSPECTOR_INLINE_PATH_LIMIT = 80

internal fun jsonInspectorVisiblePathEntries(
    paths: List<JsonPathEntry>,
    limit: Int = JSON_INSPECTOR_INLINE_PATH_LIMIT,
): List<JsonPathEntry> = paths.take(limit)

internal fun jsonInspectorPathTreeShowsTruncationHint(
    paths: List<JsonPathEntry>,
    limit: Int = JSON_INSPECTOR_INLINE_PATH_LIMIT,
): Boolean = paths.size > limit

@Composable
internal fun JsonPathListRow(
    entry: JsonPathEntry,
    selected: Boolean,
    modifier: Modifier = Modifier,
    showInlinePreview: Boolean = false,
    inlinePreviewText: String? = null,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit = onTap,
) {
    val colors = MooTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = entry.path }
            .clip(RoundedCornerShape(5.dp))
            .background(if (selected) colors.control else Color.Transparent)
            .pointerInput(entry.path) {
                detectTapGestures(onTap = { onTap() }, onDoubleTap = { onDoubleTap() })
            }
            .padding(start = (10 + entry.depth * 18).dp, top = 4.dp, end = 8.dp, bottom = 4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                entry.label,
                color = if (selected) colors.accent else colors.textPrimary,
                fontSize = 12.sp,
                modifier = Modifier.widthIn(min = 70.dp)
            )
            Text(
                entry.path,
                color = colors.textMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        if (showInlinePreview && selected) {
            Text(
                inlinePreviewText ?: entry.preview,
                color = colors.textPrimary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
