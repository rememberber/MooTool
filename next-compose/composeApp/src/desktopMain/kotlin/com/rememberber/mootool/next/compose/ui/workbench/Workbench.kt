package com.rememberber.mootool.next.compose.ui.workbench

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.ToolRegistry
import com.rememberber.mootool.next.compose.features.calculator.CalculatorScreen
import com.rememberber.mootool.next.compose.features.cron.CronScreen
import com.rememberber.mootool.next.compose.features.diff.TextDiffScreen
import com.rememberber.mootool.next.compose.features.encode.EncodeScreen
import com.rememberber.mootool.next.compose.features.home.HomeScreen
import com.rememberber.mootool.next.compose.features.json.JsonScreen
import com.rememberber.mootool.next.compose.features.regex.RegexScreen
import com.rememberber.mootool.next.compose.features.reformat.ReformatScreen
import com.rememberber.mootool.next.compose.features.placeholder.DetachedNotice
import com.rememberber.mootool.next.compose.features.placeholder.PlaceholderScreen
import com.rememberber.mootool.next.compose.features.settings.SettingsScreen
import com.rememberber.mootool.next.compose.features.time.TimeConvertScreen
import com.rememberber.mootool.next.compose.features.ua.UaParseScreen
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.theme.MooTheme

@Composable
fun Workbench(container: AppContainer, showSidebar: Boolean) {
    val settings by container.settings.collectAsState()
    val active by container.activeTool.collectAsState()
    val showSettings by container.showSettings.collectAsState()
    val searchOpen by container.searchOpen.collectAsState()
    val detached by container.sessionManager.detached.collectAsState()
    val collapsed = settings.layout.sidebarCollapsed || settings.layout.hideNavigationTitles
    Row(
        modifier = Modifier.fillMaxSize().background(MooTheme.colors.workspace).onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val meta = event.isMetaPressed || event.isCtrlPressed
            when {
                meta && event.key == Key.K -> {
                    container.setSearchOpen(true)
                    true
                }
                meta && event.key == Key.Comma -> {
                    container.openSettings(true)
                    true
                }
                event.key == Key.Escape -> {
                    when {
                        searchOpen -> { container.setSearchOpen(false); true }
                        showSettings -> { container.openSettings(false); true }
                        else -> false
                    }
                }
                else -> false
            }
        }
    ) {
        if (showSidebar) {
            Sidebar(container, collapsed) {
                container.updateSettings { it.copy(layout = it.layout.copy(sidebarCollapsed = !it.layout.sidebarCollapsed)) }
            }
        }
        Column(Modifier.weight(1f).fillMaxSize()) {
            when {
                showSettings -> SettingsScreen(container)
                active == ToolId.Mootool -> HomeScreen(container)
                active == ToolId.TextDiff && !detached.contains(ToolId.TextDiff) ->
                    TextDiffScreen(container, detached = false)
                active == ToolId.TextDiff && detached.contains(ToolId.TextDiff) ->
                    DetachedNotice(container, ToolId.TextDiff)
                active == ToolId.Reformat && !detached.contains(ToolId.Reformat) ->
                    ReformatScreen(container, detached = false)
                active == ToolId.Reformat && detached.contains(ToolId.Reformat) ->
                    DetachedNotice(container, ToolId.Reformat)
                active == ToolId.Json && !detached.contains(ToolId.Json) -> JsonScreen(container, detached = false)
                active == ToolId.Json && detached.contains(ToolId.Json) -> DetachedNotice(container, ToolId.Json)
                active == ToolId.TimeConvert && !detached.contains(ToolId.TimeConvert) ->
                    TimeConvertScreen(container, detached = false, active = true)
                active == ToolId.TimeConvert && detached.contains(ToolId.TimeConvert) ->
                    DetachedNotice(container, ToolId.TimeConvert)
                active == ToolId.Calculator && !detached.contains(ToolId.Calculator) ->
                    CalculatorScreen(container, detached = false)
                active == ToolId.Calculator && detached.contains(ToolId.Calculator) ->
                    DetachedNotice(container, ToolId.Calculator)
                active == ToolId.Encode && !detached.contains(ToolId.Encode) ->
                    EncodeScreen(container, detached = false)
                active == ToolId.Encode && detached.contains(ToolId.Encode) ->
                    DetachedNotice(container, ToolId.Encode)
                active == ToolId.UaParse && !detached.contains(ToolId.UaParse) ->
                    UaParseScreen(container, detached = false)
                active == ToolId.UaParse && detached.contains(ToolId.UaParse) ->
                    DetachedNotice(container, ToolId.UaParse)
                active == ToolId.Regex && !detached.contains(ToolId.Regex) ->
                    RegexScreen(container, detached = false)
                active == ToolId.Regex && detached.contains(ToolId.Regex) ->
                    DetachedNotice(container, ToolId.Regex)
                active == ToolId.Cron && !detached.contains(ToolId.Cron) ->
                    CronScreen(container, detached = false)
                active == ToolId.Cron && detached.contains(ToolId.Cron) ->
                    DetachedNotice(container, ToolId.Cron)
                else -> PlaceholderScreen(container, active)
            }
        }
    }
    if (searchOpen) {
        CommandSearch(container)
    }
}

@Composable
private fun CommandSearch(container: AppContainer) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(0) }
    val results = remember(query, container.settings.value.general.language) {
        ToolRegistry.search(query) { container.t(it) }
    }
    LaunchedEffect(results) { if (selected >= results.size) selected = 0 }
    Dialog(onDismissRequest = { container.setSearchOpen(false) }) {
        Column(
            Modifier.width(480.dp).background(MooTheme.colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> { selected = (selected + 1).coerceAtMost(results.lastIndex.coerceAtLeast(0)); true }
                        Key.DirectionUp -> { selected = (selected - 1).coerceAtLeast(0); true }
                        Key.Enter -> {
                            results.getOrNull(selected)?.let { container.openTool(it.id) }
                            container.setSearchOpen(false)
                            true
                        }
                        Key.Escape -> { container.setSearchOpen(false); true }
                        else -> false
                    }
                }
        ) {
            MooTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), placeholder = container.t("app.search.placeholder"))
            if (results.isEmpty()) {
                Text(container.t("app.search.empty"), color = MooTheme.colors.textSecondary, modifier = Modifier.padding(12.dp))
            } else {
                LazyColumn(Modifier.heightIn(max = 360.dp).padding(top = 8.dp)) {
                    itemsIndexed(results) { index, tool ->
                        Text(
                            "${tool.glyph}  ${container.t(tool.titleKey)}  (${tool.id.id})",
                            color = if (index == selected) MooTheme.colors.accent else MooTheme.colors.textPrimary,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}
