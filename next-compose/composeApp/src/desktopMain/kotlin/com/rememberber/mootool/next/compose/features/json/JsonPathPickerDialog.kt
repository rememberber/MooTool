package com.rememberber.mootool.next.compose.features.json

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.JsonEngine
import com.rememberber.mootool.next.compose.domain.JsonTranslator
import com.rememberber.mootool.next.compose.sessions.JsonSession
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooOverlay
import com.rememberber.mootool.next.compose.ui.components.mooDialogSurface
import com.rememberber.mootool.next.compose.ui.theme.MooTheme

@Composable
internal fun JsonPathPickerDialog(container: AppContainer, session: JsonSession, onChanged: () -> Unit) {
    val colors = MooTheme.colors
    val translator = JsonTranslator { key, params -> container.t(key, params) }
    val entries = runCatching { JsonEngine.listPaths(session.editor.text, translator) }.getOrDefault(emptyList())
    val entryPaths = entries.map { it.path }
    LaunchedEffect(session.pathPickerOpen, entryPaths) {
        if (!session.pathPickerOpen) return@LaunchedEffect
        val initial = jsonPathPickerInitialSelection(session.jsonPath, entryPaths)
        if (session.pathPickerSelection.isBlank() || session.pathPickerSelection !in entryPaths) {
            session.pathPickerSelection = initial
            onChanged()
        }
    }
    val selected = session.pathPickerSelection
    val current = entries.find { it.path == selected }
    val canUse = current != null
    val previewPath = current?.path ?: selected
    val previewText = remember(session.editor.revision, previewPath) {
        jsonPathNodePreview(session.editor.text, previewPath, translator, current?.preview.orEmpty())
    }
    MooOverlay(onDismiss = { session.pathPickerOpen = false; onChanged() }) {
        Column(
            Modifier.width(760.dp).height(480.dp).mooDialogSurface().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(container.t("json.pathPicker.title"), color = colors.textPrimary)
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .heightIn(min = 400.dp)
                    .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp)),
            ) {
                LazyColumn(
                    Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                        .padding(6.dp),
                ) {
                    items(entries) { entry ->
                        JsonPathListRow(
                            entry = entry,
                            selected = entry.path == selected,
                            onTap = { session.pathPickerSelection = entry.path; onChanged() },
                            onDoubleTap = {
                                session.applyPathPickerChoice(entry.path, container.t("json.notice.pathApplied"))
                                session.pathPickerSelection = entry.path
                                onChanged()
                            },
                        )
                    }
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(colors.border))
                Column(
                    Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(container.t("json.pathPicker.path"), color = colors.textMuted, fontSize = 10.sp)
                    Text(
                        current?.path ?: selected,
                        color = colors.textPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(container.t("json.pathPicker.preview"), color = colors.textMuted, fontSize = 10.sp)
                    Text(
                        previewText,
                        color = colors.textPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.cancel"), onClick = { session.pathPickerOpen = false; onChanged() })
                MooButton(
                    container.t("json.pathPicker.use"),
                    prominent = true,
                    enabled = canUse,
                    onClick = {
                        val entry = current ?: return@MooButton
                        session.applyPathPickerChoice(entry.path, container.t("json.notice.pathApplied"))
                        session.pathPickerSelection = entry.path
                        onChanged()
                    },
                    modifier = Modifier.semantics {
                        contentDescription = container.t("json.pathPicker.use")
                    },
                )
            }
        }
    }
}
