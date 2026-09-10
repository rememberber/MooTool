package com.rememberber.mootool.next.compose.ui.workbench

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.ToolRegistry
import com.rememberber.mootool.next.compose.model.AppLanguage
import com.rememberber.mootool.next.compose.model.ToolGroupId
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.theme.MooTheme

@Composable
fun Sidebar(container: AppContainer, collapsed: Boolean, onToggle: () -> Unit) {
    val colors = MooTheme.colors
    val active by container.activeTool.collectAsState()
    val settings by container.settings.collectAsState()
    val hidden = settings.layout.hiddenNavigationToolIds.toSet()
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .width(if (collapsed) 84.dp else settings.layout.sidebarWidth.dp)
            .fillMaxHeight()
            .background(colors.sidebar)
            .border(width = 0.dp, color = colors.border)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (collapsed) "M" else container.t("app.name"),
                color = colors.textPrimary,
                fontSize = if (collapsed) 16.sp else 13.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                if (collapsed) "»" else "«",
                color = colors.textSecondary,
                modifier = Modifier.clickable(onClick = onToggle).padding(6.dp)
            )
        }
        Box(Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 8.dp, vertical = 4.dp)) {
                NavItem(container, ToolId.Mootool, collapsed, active == ToolId.Mootool && !container.showSettings.value)
                if (settings.layout.showRecent && settings.workspace.recentToolIds.isNotEmpty()) {
                    GroupLabel(container.t("app.nav.recent"), collapsed)
                    settings.workspace.recentToolIds.mapNotNull { ToolId.fromId(it) }.forEach { id ->
                        NavItem(container, id, collapsed, active == id)
                    }
                }
                ToolRegistry.groups.forEach { group ->
                    if (settings.layout.showSeparators) GroupLabel(container.t(group.titleKey), collapsed)
                    group.toolIds.filter { it.id !in hidden }.forEach { id ->
                        NavItem(container, id, collapsed, active == id && !container.showSettings.value)
                    }
                }
            }
            VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        }
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LanguageRow(container, collapsed)
            MooButton(container.t("app.nav.settings"), onClick = { container.openSettings(true) })
        }
    }
}

@Composable
private fun GroupLabel(text: String, collapsed: Boolean) {
    if (collapsed) {
        Spacer(Modifier.height(8.dp))
        return
    }
    Text(
        text,
        color = MooTheme.colors.textSecondary,
        fontSize = 11.sp,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 6.dp)
    )
}

@Composable
private fun NavItem(container: AppContainer, id: ToolId, collapsed: Boolean, selected: Boolean) {
    val tool = ToolRegistry.byId.getValue(id)
    val colors = MooTheme.colors
    val label = container.t(tool.titleKey)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.selected else colors.sidebar)
            .clickable { container.openTool(id) }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(tool.glyph, fontSize = 13.sp, color = if (selected) colors.accent else colors.textSecondary, modifier = Modifier.size(18.dp))
        if (!collapsed) {
            Spacer(Modifier.width(8.dp))
            Text(label, color = colors.textPrimary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun LanguageRow(container: AppContainer, collapsed: Boolean) {
    if (collapsed) return
    val current = AppLanguage.fromCode(container.settings.value.general.language)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(AppLanguage.ZhCN to "中", AppLanguage.EnUS to "EN", AppLanguage.JaJP to "日").forEach { (language, label) ->
            val selected = language == current
            Text(
                label,
                color = if (selected) MooTheme.colors.accent else MooTheme.colors.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        container.updateSettings { it.copy(general = it.general.copy(language = language.code)) }
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun StatusBar(container: AppContainer, extra: String) {
    val colors = MooTheme.colors
    val status by container.status.collectAsState()
    Row(
        modifier = Modifier.fillMaxWidth().height(26.dp).background(colors.toolbar)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(extra.ifBlank { status.ifBlank { container.t("app.status.ready") } }, color = colors.textSecondary, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Text(container.directories.dataRoot.toString(), color = colors.textSecondary, fontSize = 11.sp)
    }
}
