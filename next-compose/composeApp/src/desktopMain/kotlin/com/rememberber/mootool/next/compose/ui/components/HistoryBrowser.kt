package com.rememberber.mootool.next.compose.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
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
        Text(item.createdAt, color = MooTheme.colors.textSecondary, fontSize = 11.sp)
        val snippet = listOf(item.input, item.output).filter { it.isNotBlank() }.joinToString(" → ")
        if (snippet.isNotBlank()) {
            Text(snippet.take(240), color = MooTheme.colors.textSecondary, fontSize = 11.sp, maxLines = 2)
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
            Modifier.width(560.dp).height(460.dp)
                .background(colors.workspace, RoundedCornerShape(12.dp))
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, color = colors.textPrimary)
            MooTextField(
                query,
                { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = container.t("history.searchPlaceholder")
            )
            if (items.isEmpty()) {
                Text(container.t("json.history.empty"), color = colors.textSecondary, modifier = Modifier.weight(1f))
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(items, key = { it.id }) { item ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(
                                Modifier.weight(1f).clickable { onRestore(item) }.padding(4.dp)
                            ) {
                                Text(item.summary.ifBlank { item.operation }, color = colors.textPrimary, fontSize = 13.sp)
                                detail(item)
                            }
                            MooButton(container.t("common.delete"), onClick = {
                                container.history.delete(item.id)
                                reload()
                            })
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.clear"), onClick = {
                    container.history.clear(toolId)
                    reload()
                })
                MooButton(container.t("common.close"), onClick = onDismiss)
            }
        }
    }
}
