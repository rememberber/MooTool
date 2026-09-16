package com.rememberber.mootool.next.compose.features.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.ToolRegistry
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.theme.MooTheme

@Composable
fun PlaceholderScreen(container: AppContainer, toolId: ToolId) {
    val tool = ToolRegistry.byId.getValue(toolId)
    Column(
        modifier = Modifier.fillMaxSize().background(MooTheme.colors.workspace).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MooPageTitle(container.t(tool.titleKey), large = true)
        Text(container.t("app.tool.unimplemented"), color = MooTheme.colors.warning)
        Text("Tool ID: ${toolId.id}", color = MooTheme.colors.textSecondary)
    }
}

@Composable
fun DetachedNotice(container: AppContainer, toolId: ToolId) {
    val tool = ToolRegistry.byId.getValue(toolId)
    val label = container.t(tool.titleKey)
    Column(
        modifier = Modifier.fillMaxSize().background(MooTheme.colors.workspace).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MooPageTitle(container.t("app.tool.detachedTitle", mapOf("tool" to label)), large = true)
        Text(container.t("app.tool.detachedDescription"), color = MooTheme.colors.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MooButton(container.t("app.tool.focus"), prominent = true, onClick = { container.requestFocusDetached(toolId) })
            MooButton(container.t("app.tool.reattach"), onClick = { container.sessionManager.reattach(toolId) })
        }
    }
}
