package com.rememberber.mootool.next.compose.ui.workbench

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.ToolRegistry
import com.rememberber.mootool.next.compose.domain.NavigationToolVisibility
import com.rememberber.mootool.next.compose.model.AppLanguage
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooMenu
import com.rememberber.mootool.next.compose.ui.components.MooMenuItem
import com.rememberber.mootool.next.compose.ui.components.MooGhostButton
import com.rememberber.mootool.next.compose.ui.components.MooTooltip
import com.rememberber.mootool.next.compose.ui.components.mooFocusOutline
import com.rememberber.mootool.next.compose.ui.components.mooSidebarBackground
import com.rememberber.mootool.next.compose.ui.components.mooStatusBarBackground
import com.rememberber.mootool.next.compose.ui.components.MooStatusMeta
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.icons.ToolIcon
import com.rememberber.mootool.next.compose.ui.theme.MooTheme

@Composable
fun Sidebar(container: AppContainer, collapsed: Boolean, onToggle: () -> Unit) {
    val colors = MooTheme.colors
    val active by container.activeTool.collectAsState()
    val settings by container.settings.collectAsState()
    val hidden = settings.layout.hiddenNavigationToolIds.toSet()
    val compactNav = settings.layout.compactNavigation
    val showSeparators = settings.layout.showSeparators
    val navigationStyle = settings.layout.navigationStyle
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxHeight()) {
    Column(
        modifier = Modifier
            .width(if (collapsed) 84.dp else settings.layout.sidebarWidth.dp.coerceIn(208.dp, 300.dp))
            .fillMaxHeight()
            .mooSidebarBackground()
            .drawBehind {
                val x = size.width - 0.5f
                drawLine(
                    color = colors.borderSoft,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (collapsed) "M" else container.t("app.name"),
                color = colors.textPrimary,
                fontSize = if (collapsed) 16.sp else 13.sp
            )
            Spacer(Modifier.weight(1f))
            SidebarGhost(
                label = container.t(if (collapsed) "app.nav.expand" else "app.nav.collapse"),
                glyph = if (collapsed) "»" else "«",
                onClick = onToggle
            )
            SidebarGhost(
                label = searchTooltip(container.t("app.nav.search")),
                glyph = "⌕",
                onClick = { container.setSearchOpen(true) }
            )
            SidebarGhost(
                label = container.t("app.nav.manageGroups"),
                glyph = "+",
                onClick = { container.setGroupManagerOpen(true) }
            )
        }
        Box(Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 8.dp, vertical = 4.dp)) {
                NavItem(container, ToolId.Mootool, collapsed, active == ToolId.Mootool && !container.showSettings.value)
                if (settings.layout.showRecent && settings.workspace.recentToolIds.isNotEmpty()) {
                    NavigationToolGroup(showSeparators, navigationStyle, compactNav) {
                        if (WorkbenchNavPresentation.showNavigationGroupLabel(navigationStyle, showSeparators, customGroup = false)) {
                            GroupLabel(container.t("app.nav.recent"), collapsed, compactNav)
                        }
                        settings.workspace.recentToolIds.mapNotNull { ToolId.fromId(it) }
                            .filter { it != ToolId.Mootool }
                            .forEach { id ->
                                NavItem(container, id, collapsed, active == id)
                            }
                    }
                }
                settings.layout.customGroups.forEach { group ->
                    val items = group.toolIds.mapNotNull { ToolId.fromId(it) }.filter { it.id !in hidden }
                    if (items.isEmpty()) return@forEach
                    NavigationToolGroup(showSeparators, navigationStyle, compactNav) {
                        if (WorkbenchNavPresentation.showNavigationGroupLabel(navigationStyle, showSeparators, customGroup = true)) {
                            GroupLabel(group.name, collapsed, compactNav)
                        }
                        items.forEach { id ->
                            NavItem(container, id, collapsed, active == id && !container.showSettings.value)
                        }
                    }
                }
                if (
                    settings.layout.customGroups.isNotEmpty() &&
                    showSeparators &&
                    NavigationToolVisibility.visibleNavigationToolCount(hidden) > 0
                ) {
                    if (WorkbenchNavPresentation.showNavigationGroupLabel(navigationStyle, showSeparators, customGroup = false)) {
                        GroupLabel(container.t("app.group.all"), collapsed, compactNav)
                    }
                }
                ToolRegistry.groups.forEach { group ->
                    val items = group.toolIds.filter { it.id !in hidden }
                    if (items.isEmpty()) return@forEach
                    if (settings.layout.navigationStyle == "grouped" && !collapsed) {
                        NavigationToolGroup(showSeparators, navigationStyle, compactNav) {
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(MooTheme.dimens.radius))
                                    .background(colors.surfaceSubtle).padding(6.dp)
                            ) {
                                if (WorkbenchNavPresentation.showNavigationGroupLabel(navigationStyle, showSeparators, customGroup = false)) {
                                    GroupLabel(container.t(group.titleKey), collapsed, compactNav)
                                }
                                items.forEach { id ->
                                    NavItem(container, id, collapsed, active == id && !container.showSettings.value)
                                }
                            }
                        }
                    } else {
                        NavigationToolGroup(showSeparators, navigationStyle, compactNav) {
                            if (WorkbenchNavPresentation.showNavigationGroupLabel(navigationStyle, showSeparators, customGroup = false)) {
                                GroupLabel(container.t(group.titleKey), collapsed, compactNav)
                            }
                            items.forEach { id ->
                                NavItem(container, id, collapsed, active == id && !container.showSettings.value)
                            }
                        }
                    }
                }
            }
            VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        }
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LanguageRow(container, collapsed)
            if (collapsed) {
                MooTooltip(container.t("app.nav.settings"), enabled = true) {
                    MooButton(container.t("app.nav.settings"), dense = compactNav, onClick = { container.openSettings(true) })
                }
            } else {
                MooButton(container.t("app.nav.settings"), dense = compactNav, onClick = { container.openSettings(true) })
            }
        }
    }
    if (!collapsed) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(6.dp)
                .pointerInput(settings.layout.sidebarWidth) {
                    var dragged = false
                    detectDragGestures(
                        onDragStart = { dragged = false },
                        onDrag = { _, amount ->
                            if (abs(amount.x) > 0.5f) {
                                dragged = true
                                val next = (settings.layout.sidebarWidth + amount.x).coerceIn(208f, 300f)
                                container.updateSettings { it.copy(layout = it.layout.copy(sidebarWidth = next)) }
                            }
                        },
                        onDragEnd = { }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            container.updateSettings { it.copy(layout = it.layout.copy(sidebarWidth = 248f)) }
                        }
                    )
                }
        )
    }
    }
}

@Composable
private fun NavigationToolGroup(
    showSeparators: Boolean,
    navigationStyle: String,
    compactNavigation: Boolean,
    content: @Composable () -> Unit
) {
    val colors = MooTheme.colors
    val topPad = LayoutPolicy.navigationGroupTopPaddingDp(navigationStyle, compactNavigation)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = topPad.dp)
            .then(
                if (showSeparators) {
                    Modifier
                        .padding(bottom = 12.dp)
                        .drawBehind {
                            val y = size.height - 0.5f
                            drawLine(
                                color = colors.borderSoft,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                } else {
                    Modifier
                }
            )
    ) {
        content()
    }
}

@Composable
private fun GroupLabel(text: String, collapsed: Boolean, compactNavigation: Boolean) {
    if (collapsed) {
        Spacer(Modifier.height(if (compactNavigation) 4.dp else 8.dp))
        return
    }
    Text(
        text,
        color = MooTheme.colors.textMuted,
        fontSize = 11.sp,
        modifier = Modifier.padding(
            top = if (compactNavigation) 0.dp else 2.dp,
            bottom = if (compactNavigation) 2.dp else 4.dp,
            start = 6.dp
        )
    )
}

@Composable
private fun NavItem(container: AppContainer, id: ToolId, collapsed: Boolean, selected: Boolean) {
    val tool = ToolRegistry.byId.getValue(id)
    val colors = MooTheme.colors
    val settings = container.settings.value
    val compact = settings.layout.compactNavigation
    val navFontSize = LayoutPolicy.navigationItemFontSp(compact).sp
    val navMinHeight = LayoutPolicy.navigationItemMinHeightDp(compact).dp
    val navVerticalPadding = LayoutPolicy.navigationItemVerticalPaddingDp(compact).dp
    val card = settings.layout.navigationStyle == "card"
    val label = container.t(tool.titleKey)
    val detached by container.sessionManager.detached.collectAsState()
    val isDetached = id in detached
    val showWindowAction = DetachPolicy.showSidebarWindowAction(
        collapsed,
        settings.layout.hideNavigationTitles,
        id.detachable
    )
    val actionLabel = container.t(
        if (isDetached) "app.tool.reattachTool" else "app.tool.detachTool",
        mapOf("tool" to label)
    )
    var menuOpen by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(if (card) MooTheme.dimens.radiusLarge else MooTheme.dimens.navRadius)
    val itemBorder = when {
        focused -> 2.dp to colors.focusRing
        else -> 1.dp to colors.navItemBorder(selected, hovered)
    }
    MooTooltip(label, enabled = collapsed) {
    Box {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = navMinHeight)
            .padding(vertical = if (compact) 1.dp else 2.dp)
            .mooFocusOutline(focused, shape)
            .clip(shape)
            .background(colors.sidebarItemBrush(selected, card, hovered))
            .border(itemBorder.first, itemBorder.second, shape)
            .hoverable(interaction)
            .focusable(true, interaction)
            .semantics {
                role = Role.Button
                contentDescription = label
                this.selected = selected
            }
            .pointerInput(id.detachable) {
                if (!id.detachable) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            event.changes.forEach { it.consume() }
                            menuOpen = true
                        }
                    }
                }
            }
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current
            ) { container.openTool(id) }
            .padding(horizontal = 8.dp, vertical = navVerticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected && (colors.styleId == "smartisan" || colors.styleId == "miui-v5")) {
            Box(Modifier.width(3.dp).height(18.dp).background(colors.navActiveBar, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(5.dp))
        }
        ToolIcon(id, if (selected) colors.navSelectedIcon() else colors.textSecondary, Modifier.size(18.dp))
        if (!collapsed) {
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                color = if (selected) colors.navSelectedContent() else colors.textPrimary,
                fontSize = navFontSize,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (showWindowAction) {
            val actionInteraction = remember { MutableInteractionSource() }
            val actionFocused by actionInteraction.collectIsFocusedAsState()
            val actionShape = RoundedCornerShape(4.dp)
            MooTooltip(actionLabel) {
                Text(
                    if (isDetached) "▣" else "⧉",
                    color = if (isDetached) colors.accent else colors.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .mooFocusOutline(actionFocused, actionShape)
                        .clip(actionShape)
                        .border(
                            if (actionFocused) 2.dp else 0.dp,
                            if (actionFocused) colors.focusRing else Color.Transparent,
                            actionShape
                        )
                        .focusable(true, actionInteraction)
                        .clickable(
                            interactionSource = actionInteraction,
                            indication = LocalIndication.current
                        ) { container.toggleDetach(id) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .semantics { contentDescription = actionLabel; role = Role.Button }
                )
            }
        }
    }
    if (id.detachable) {
        MooMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            MooMenuItem(onClick = {
                menuOpen = false
                container.toggleDetach(id)
            }) {
                Text(if (isDetached) container.t("app.tool.reattach") else container.t("app.tool.detach"))
            }
            if (isDetached) {
                MooMenuItem(onClick = {
                    menuOpen = false
                    container.openTool(id)
                }) {
                    Text(container.t("app.tool.focus"))
                }
            }
        }
    }
        }
    }
}

@Composable
private fun SidebarGhost(label: String, glyph: String, onClick: () -> Unit) {
    val colors = MooTheme.colors
    MooTooltip(label) {
        MooGhostButton(label, onClick = onClick) {
            Text(glyph, color = colors.textMuted, fontSize = 13.sp)
        }
    }
}

private fun searchTooltip(search: String): String {
    val mac = System.getProperty("os.name").orEmpty().lowercase().contains("mac")
    return "$search · ${if (mac) "⌘K" else "Ctrl+K"}"
}

@Composable
private fun LanguageRow(container: AppContainer, collapsed: Boolean) {
    if (collapsed) return
    val current = AppLanguage.fromCode(container.settings.value.general.language)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(AppLanguage.ZhCN to "中", AppLanguage.EnUS to "EN", AppLanguage.JaJP to "日").forEach { (language, label) ->
            val selected = language == current
            val interaction = remember(language) { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            val shape = RoundedCornerShape(6.dp)
            Text(
                label,
                color = if (selected) MooTheme.colors.accent else MooTheme.colors.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier
                    .mooFocusOutline(focused, shape)
                    .clip(shape)
                    .border(
                        if (focused) 2.dp else 0.dp,
                        if (focused) MooTheme.colors.focusRing else Color.Transparent,
                        shape
                    )
                    .focusable(true, interaction)
                    .clickable(
                        interactionSource = interaction,
                        indication = LocalIndication.current
                    ) {
                        container.updateSettings { it.copy(general = it.general.copy(language = language.code)) }
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .semantics { role = Role.Button; this.selected = selected; contentDescription = label }
            )
        }
    }
}

@Composable
fun StatusBar(container: AppContainer, extra: String) {
    val status by container.status.collectAsState()
    Row(
        modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.statusBar).mooStatusBarBackground()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MooStatusMeta(extra.ifBlank { status.ifBlank { container.t("app.status.ready") } })
        Spacer(Modifier.weight(1f))
        MooStatusMeta(container.dataDirectories().dataRoot.toString())
    }
}
