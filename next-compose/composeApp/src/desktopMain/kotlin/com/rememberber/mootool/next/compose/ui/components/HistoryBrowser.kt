package com.rememberber.mootool.next.compose.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.ui.theme.MooTheme

@Composable
fun HistoryBrowser(
    container: AppContainer,
    toolId: String,
    title: String,
    onRestore: (HistoryRecord) -> Unit,
    onDismiss: () -> Unit,
    detail: @Composable (HistoryRecord) -> Unit = { item ->
        val snippet = listOf(item.input, item.output).filter { it.isNotBlank() }.joinToString(" → ")
        if (snippet.isNotBlank()) {
            Text(
                snippet,
                color = MooTheme.colors.textMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 7.dp)
            )
        }
    }
) {
    var query by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(container.history.list(toolId)) }
    fun reload() {
        items = container.history.list(toolId, query)
    }
    LaunchedEffect(query, toolId) { reload() }
    val colors = MooTheme.colors
    MooOverlay(onDismiss = onDismiss) {
        Column(
            Modifier.width(560.dp).height(460.dp).mooDialogSurface().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MooPageTitle(title)
            MooTextField(
                query,
                { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = container.t("history.searchPlaceholder")
            )
            if (items.isEmpty()) {
                Text(container.t("json.history.empty"), color = colors.textMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(items, key = { it.id }) { item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(
                                Modifier.weight(1f).mooFocusClickable { onRestore(item) }
                            ) {
                                Text(
                                    item.summary.ifBlank { item.operation },
                                    color = colors.textBody,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(item.createdAt, color = colors.textMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
                                detail(item)
                            }
                            MooGhostButton(container.t("common.delete"), onClick = {
                                container.history.delete(item.id)
                                reload()
                            }, size = 28.dp) {
                                Text("×", color = colors.textMuted, fontSize = 16.sp)
                            }
                        }
                        androidx.compose.foundation.layout.Box(
                            Modifier.fillMaxWidth().height(1.dp).background(colors.borderSoft)
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.clear"), danger = true, onClick = {
                    container.history.clear(toolId)
                    reload()
                })
                MooButton(container.t("common.close"), onClick = onDismiss)
            }
        }
    }
}
