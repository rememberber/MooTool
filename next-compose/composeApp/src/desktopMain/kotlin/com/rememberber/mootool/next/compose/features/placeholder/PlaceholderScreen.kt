package com.rememberber.mootool.next.compose.features.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.ToolRegistry
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.theme.MooTheme

@Composable
fun PlaceholderScreen(container: AppContainer, toolId: ToolId) {
    val tool = ToolRegistry.byId.getValue(toolId)
    Column(
        modifier = Modifier.fillMaxSize().background(MooTheme.colors.workspace).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(container.t(tool.titleKey), fontSize = 18.sp, color = MooTheme.colors.textPrimary)
        Text(container.t("app.tool.unimplemented"), color = MooTheme.colors.warning)
        Text("Tool ID: ${toolId.id}", color = MooTheme.colors.textSecondary)
    }
}

@Composable
fun DetachedNotice(container: AppContainer, toolId: ToolId) {
    val tool = ToolRegistry.byId.getValue(toolId)
    Column(
        modifier = Modifier.fillMaxSize().background(MooTheme.colors.workspace).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(container.t(tool.titleKey), fontSize = 18.sp, color = MooTheme.colors.textPrimary)
        Text(container.t("app.tool.detach"), color = MooTheme.colors.textSecondary)
        MooButton(container.t("app.tool.reattach"), primary = true, onClick = { container.sessionManager.reattach(toolId) })
    }
}
