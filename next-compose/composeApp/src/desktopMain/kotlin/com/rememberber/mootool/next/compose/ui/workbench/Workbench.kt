package com.rememberber.mootool.next.compose.ui.workbench

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.ToolRegistry
import com.rememberber.mootool.next.compose.features.calculator.CalculatorScreen
import com.rememberber.mootool.next.compose.features.config.ConfigConvertScreen
import com.rememberber.mootool.next.compose.features.color.ColorBoardScreen
import com.rememberber.mootool.next.compose.features.crypto.CryptoScreen
import com.rememberber.mootool.next.compose.features.cron.CronScreen
import com.rememberber.mootool.next.compose.features.diff.TextDiffScreen
import com.rememberber.mootool.next.compose.features.encode.EncodeScreen
import com.rememberber.mootool.next.compose.features.hardware.HardwareScreen
import com.rememberber.mootool.next.compose.features.host.HostScreen
import com.rememberber.mootool.next.compose.features.http.HttpScreen
import com.rememberber.mootool.next.compose.features.home.HomeScreen
import com.rememberber.mootool.next.compose.features.image.ImageScreen
import com.rememberber.mootool.next.compose.features.json.JsonScreen
import com.rememberber.mootool.next.compose.features.messageboard.MessageBoardScreen
import com.rememberber.mootool.next.compose.features.translation.TranslationScreen
import com.rememberber.mootool.next.compose.features.net.NetScreen
import com.rememberber.mootool.next.compose.features.pdf.PdfScreen
import com.rememberber.mootool.next.compose.features.regex.RegexScreen
import com.rememberber.mootool.next.compose.features.reformat.ReformatScreen
import com.rememberber.mootool.next.compose.features.protobuf.ProtobufScreen
import com.rememberber.mootool.next.compose.features.quicknote.QuickNoteScreen
import com.rememberber.mootool.next.compose.features.runtime.CodeRunScreen
import com.rememberber.mootool.next.compose.features.placeholder.DetachedNotice
import com.rememberber.mootool.next.compose.features.placeholder.PlaceholderScreen
import com.rememberber.mootool.next.compose.features.qrcode.QrCodeScreen
import com.rememberber.mootool.next.compose.features.settings.LegacyMigrationHintDialog
import com.rememberber.mootool.next.compose.features.settings.SettingsScreen
import com.rememberber.mootool.next.compose.features.time.TimeConvertScreen
import com.rememberber.mootool.next.compose.features.ua.UaParseScreen
import com.rememberber.mootool.next.compose.features.variables.VariablesScreen
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.ui.components.mooFocusClickable
import com.rememberber.mootool.next.compose.ui.components.mooFocusOutline
import com.rememberber.mootool.next.compose.ui.components.MooOverlay
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.mooDialogSurface
import com.rememberber.mootool.next.compose.ui.components.mooWorkspaceBackground
import com.rememberber.mootool.next.compose.ui.icons.ToolIcon
import com.rememberber.mootool.next.compose.ui.theme.MooTheme

@Composable
fun Workbench(container: AppContainer, showSidebar: Boolean) {
    val settings by container.settings.collectAsState()
    val active by container.activeTool.collectAsState()
    val showSettings by container.showSettings.collectAsState()
    val searchOpen by container.searchOpen.collectAsState()
    val groupManagerOpen by container.groupManagerOpen.collectAsState()
    val detached by container.sessionManager.detached.collectAsState()
    val density = LocalDensity.current
    val fontScale = (settings.appearance.fontSize.coerceIn(12, 18) / 13f)
    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * fontScale)) {
    BoxWithConstraints(Modifier.fillMaxSize().onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown || event.blockedByIme()) return@onPreviewKeyEvent false
            when {
                matchesShortcut(event, settings.shortcuts.search) -> {
                    container.setSearchOpen(true)
                    true
                }
                matchesShortcut(event, settings.shortcuts.settings) -> {
                    container.openSettings(!showSettings)
                    true
                }
                event.key == Key.Escape -> {
                    when {
                        searchOpen -> { container.setSearchOpen(false); true }
                        groupManagerOpen -> { container.setGroupManagerOpen(false); true }
                        showSettings -> { container.openSettings(false); true }
                        else -> false
                    }
                }
                else -> false
            }
        }) {
    val contentWidth = maxWidth.value
    val contentHeight = maxHeight.value
    val autoCollapse = LayoutPolicy.collapseNavigation(
        contentWidth,
        settings.layout.sidebarCollapsed,
        settings.layout.hideNavigationTitles
    )
    val collapsed = autoCollapse
    Row(
        modifier = Modifier.fillMaxSize().mooWorkspaceBackground()
    ) {
        if (showSidebar) {
            Sidebar(container, collapsed) {
                container.updateSettings { it.copy(layout = it.layout.copy(sidebarCollapsed = !it.layout.sidebarCollapsed)) }
            }
        }
        Column(Modifier.weight(1f).fillMaxSize()) {
            if (LayoutPolicy.showMinSizeHint(contentWidth, contentHeight)) {
                Text(
                    container.t("app.layout.minSizeHint"),
                    color = MooTheme.colors.warning,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().background(MooTheme.colors.surfaceSubtle).padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            Box(Modifier.weight(1f).fillMaxSize()) {
            when {
                showSettings -> SettingsScreen(container)
                active == ToolId.Mootool -> HomeScreen(container)
                active == ToolId.QuickNote && !detached.contains(ToolId.QuickNote) ->
                    QuickNoteScreen(container, detached = false)
                active == ToolId.QuickNote && detached.contains(ToolId.QuickNote) ->
                    DetachedNotice(container, ToolId.QuickNote)
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
                active == ToolId.YmlProperties && !detached.contains(ToolId.YmlProperties) ->
                    ConfigConvertScreen(container, detached = false)
                active == ToolId.YmlProperties && detached.contains(ToolId.YmlProperties) ->
                    DetachedNotice(container, ToolId.YmlProperties)
                active == ToolId.Protobuf && !detached.contains(ToolId.Protobuf) ->
                    ProtobufScreen(container, detached = false)
                active == ToolId.Protobuf && detached.contains(ToolId.Protobuf) ->
                    DetachedNotice(container, ToolId.Protobuf)
                active == ToolId.Java && !detached.contains(ToolId.Java) ->
                    CodeRunScreen(container, detached = false)
                active == ToolId.Java && detached.contains(ToolId.Java) ->
                    DetachedNotice(container, ToolId.Java)
                active == ToolId.Variables && !detached.contains(ToolId.Variables) ->
                    VariablesScreen(container, detached = false)
                active == ToolId.Variables && detached.contains(ToolId.Variables) ->
                    DetachedNotice(container, ToolId.Variables)
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
                active == ToolId.Crypto && !detached.contains(ToolId.Crypto) ->
                    CryptoScreen(container, detached = false)
                active == ToolId.Crypto && detached.contains(ToolId.Crypto) ->
                    DetachedNotice(container, ToolId.Crypto)
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
                active == ToolId.QrCode && !detached.contains(ToolId.QrCode) ->
                    QrCodeScreen(container, detached = false)
                active == ToolId.QrCode && detached.contains(ToolId.QrCode) ->
                    DetachedNotice(container, ToolId.QrCode)
                active == ToolId.ColorBoard && !detached.contains(ToolId.ColorBoard) ->
                    ColorBoardScreen(container, detached = false)
                active == ToolId.ColorBoard && detached.contains(ToolId.ColorBoard) ->
                    DetachedNotice(container, ToolId.ColorBoard)
                active == ToolId.MessageBoard && !detached.contains(ToolId.MessageBoard) ->
                    MessageBoardScreen(container, detached = false)
                active == ToolId.MessageBoard && detached.contains(ToolId.MessageBoard) ->
                    DetachedNotice(container, ToolId.MessageBoard)
                active == ToolId.Translation && !detached.contains(ToolId.Translation) ->
                    TranslationScreen(container, detached = false)
                active == ToolId.Translation && detached.contains(ToolId.Translation) ->
                    DetachedNotice(container, ToolId.Translation)
                active == ToolId.Image && !detached.contains(ToolId.Image) ->
                    ImageScreen(container, detached = false)
                active == ToolId.Image && detached.contains(ToolId.Image) ->
                    DetachedNotice(container, ToolId.Image)
                active == ToolId.Pdf && !detached.contains(ToolId.Pdf) ->
                    PdfScreen(container, detached = false)
                active == ToolId.Pdf && detached.contains(ToolId.Pdf) ->
                    DetachedNotice(container, ToolId.Pdf)
                active == ToolId.Http && !detached.contains(ToolId.Http) ->
                    HttpScreen(container, detached = false)
                active == ToolId.Http && detached.contains(ToolId.Http) ->
                    DetachedNotice(container, ToolId.Http)
                active == ToolId.Host && !detached.contains(ToolId.Host) ->
                    HostScreen(container, detached = false)
                active == ToolId.Host && detached.contains(ToolId.Host) ->
                    DetachedNotice(container, ToolId.Host)
                active == ToolId.Net && !detached.contains(ToolId.Net) ->
                    NetScreen(container, detached = false)
                active == ToolId.Net && detached.contains(ToolId.Net) ->
                    DetachedNotice(container, ToolId.Net)
                active == ToolId.Hardware && !detached.contains(ToolId.Hardware) ->
                    HardwareScreen(container, detached = false)
                active == ToolId.Hardware && detached.contains(ToolId.Hardware) ->
                    DetachedNotice(container, ToolId.Hardware)
                else -> PlaceholderScreen(container, active)
            }
            }
        }
    }
    if (searchOpen) {
        CommandSearch(container)
    }
    if (groupManagerOpen) {
        CustomGroupManager(container)
    }
    LegacyMigrationHintDialog(container)
    }
    }
}

@Composable
private fun CommandSearch(container: AppContainer) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(0) }
    val colors = MooTheme.colors
    val results = remember(query, container.settings.value.general.language) {
        ToolRegistry.search(query) { container.t(it) }
    }
    val searchFocus = remember { FocusRequester() }
    val closeFocus = remember { FocusRequester() }
    val resultFocus = remember { FocusRequester() }
    LaunchedEffect(results) { selected = nextCommandIndex(selected, results.size, stay = true) }
    LaunchedEffect(Unit) { searchFocus.requestFocus() }
    fun choose(index: Int) {
        results.getOrNull(index)?.let { container.openTool(it.id) }
        container.setSearchOpen(false)
    }
    fun focusCommandPaletteTarget(target: CommandPaletteFocusTarget) {
        when (target) {
            CommandPaletteFocusTarget.Search -> searchFocus.requestFocus()
            CommandPaletteFocusTarget.Close -> closeFocus.requestFocus()
            CommandPaletteFocusTarget.Result -> resultFocus.requestFocus()
        }
    }
    fun onPaletteKey(
        event: androidx.compose.ui.input.key.KeyEvent,
        from: CommandPaletteFocusTarget = CommandPaletteFocusTarget.Search,
    ): Boolean {
        if (event.type != KeyEventType.KeyDown || event.blockedByIme()) return false
        if (event.key == Key.Tab) {
            val next = commandPaletteTabFocusTransition(
                shift = event.isShiftPressed,
                from = from,
                resultCount = results.size,
            )
            if (next != null) {
                focusCommandPaletteTarget(next)
                return true
            }
        }
        if (from != CommandPaletteFocusTarget.Result) {
            return when (event.key) {
                Key.DirectionDown -> {
                    if (from == CommandPaletteFocusTarget.Search && results.isNotEmpty()) {
                        selected = nextCommandIndex(selected, results.size, down = true)
                        true
                    } else {
                        false
                    }
                }
                Key.DirectionUp -> {
                    if (from == CommandPaletteFocusTarget.Search && results.isNotEmpty()) {
                        selected = nextCommandIndex(selected, results.size, down = false)
                        true
                    } else {
                        false
                    }
                }
                Key.Enter -> {
                    if (from == CommandPaletteFocusTarget.Close) {
                        container.setSearchOpen(false)
                        true
                    } else if (from == CommandPaletteFocusTarget.Search && results.isNotEmpty()) {
                        choose(selected)
                        true
                    } else {
                        false
                    }
                }
                Key.Escape -> { container.setSearchOpen(false); true }
                else -> false
            }
        }
        return when (event.key) {
            Key.DirectionDown -> { selected = nextCommandIndex(selected, results.size, down = true); true }
            Key.DirectionUp -> { selected = nextCommandIndex(selected, results.size, down = false); true }
            Key.Enter -> { choose(selected); true }
            Key.Escape -> { container.setSearchOpen(false); true }
            else -> false
        }
    }
    MooOverlay(
        onDismiss = { container.setSearchOpen(false) },
        alignment = Alignment.TopCenter,
        contentPadding = PaddingValues(top = 96.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
    ) {
        Column(
            Modifier
                .width(620.dp)
                .mooDialogSurface(MooTheme.dimens.commandRadius)
                .semantics { contentDescription = container.t("app.search.title") }
                .onPreviewKeyEvent { onPaletteKey(it) }
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .then(
                        if (colors.styleId == "smartisan" || colors.styleId == "miui-v5") {
                            Modifier.background(colors.toolbarBrush())
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("⌕", color = colors.textSecondary, fontSize = 16.sp)
                MooTextField(
                    query,
                    {
                        query = it
                        selected = 0
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = container.t("app.search.placeholder"),
                    fieldModifier = Modifier.focusRequester(searchFocus)
                )
                Text(
                    "×",
                    color = colors.textSecondary,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .focusRequester(closeFocus)
                        .onPreviewKeyEvent { onPaletteKey(it, CommandPaletteFocusTarget.Close) }
                        .mooFocusClickable { container.setSearchOpen(false) }
                        .padding(6.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = container.t("app.search.close")
                        }
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
            if (results.isEmpty()) {
                Text(
                    container.t("app.search.empty"),
                    color = colors.textSecondary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 44.dp),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(Modifier.heightIn(max = 500.dp).padding(8.dp)) {
                    itemsIndexed(results) { index, tool ->
                        val active = index == selected
                        val groupKey = ToolRegistry.groupTitleKey(tool.groupId)
                        val rowShape = RoundedCornerShape(MooTheme.dimens.navRadius)
                        val rowInteraction = remember(index) { MutableInteractionSource() }
                        val rowFocused by rowInteraction.collectIsFocusedAsState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (active) Modifier.focusRequester(resultFocus) else Modifier)
                                .onPreviewKeyEvent { onPaletteKey(it, CommandPaletteFocusTarget.Result) }
                                .mooFocusOutline(rowFocused, rowShape)
                                .clip(rowShape)
                                .background(colors.sidebarItemBrush(active, card = false, hovered = false))
                                .border(
                                    if (rowFocused) 2.dp else 1.dp,
                                    if (rowFocused) colors.focusRing else colors.navItemBorder(active, hovered = false),
                                    rowShape,
                                )
                                .pointerInput(index) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (event.type == PointerEventType.Enter) {
                                                selected = index
                                            }
                                        }
                                    }
                                }
                                .focusable(true, rowInteraction)
                                .clickable(
                                    interactionSource = rowInteraction,
                                    indication = LocalIndication.current,
                                    onClick = { choose(index) },
                                )
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                .semantics {
                                    role = Role.Button
                                    this.selected = active
                                    contentDescription = container.t(tool.titleKey)
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ToolIcon(tool.id, if (active) colors.navSelectedIcon() else colors.textSecondary)
                            Text(
                                container.t(tool.titleKey),
                                color = if (active) colors.navSelectedContent() else colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            if (groupKey != null) {
                                Text(container.t(groupKey), color = colors.textSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

internal enum class CommandPaletteFocusTarget {
    Search,
    Close,
    Result,
}

/** Tab / Shift+Tab：搜索框 → 关闭钮 → 当前选中结果行（对齐 Electron 搜索行内关闭按钮与结果 `<button>` 的 Tab 序）。 */
internal fun commandPaletteTabFocusTransition(
    shift: Boolean,
    from: CommandPaletteFocusTarget,
    resultCount: Int,
): CommandPaletteFocusTarget? {
    return when (from) {
        CommandPaletteFocusTarget.Search -> if (shift) null else CommandPaletteFocusTarget.Close
        CommandPaletteFocusTarget.Close -> when {
            shift -> CommandPaletteFocusTarget.Search
            resultCount > 0 -> CommandPaletteFocusTarget.Result
            else -> null
        }
        CommandPaletteFocusTarget.Result -> if (shift && resultCount > 0) CommandPaletteFocusTarget.Close else null
    }
}

/** 从搜索框出发连续 Tab 的焦点链（用于键盘走查单测，不含循环回搜索框）。 */
internal fun commandPaletteTabForwardWalkthrough(resultCount: Int): List<CommandPaletteFocusTarget> {
    val steps = mutableListOf<CommandPaletteFocusTarget>()
    var current = CommandPaletteFocusTarget.Search
    while (true) {
        val next = commandPaletteTabFocusTransition(shift = false, from = current, resultCount = resultCount) ?: break
        steps += next
        current = next
    }
    return steps
}

/** 从结果行 Shift+Tab 回到搜索框的链。 */
internal fun commandPaletteTabBackwardWalkthrough(resultCount: Int): List<CommandPaletteFocusTarget> {
    if (resultCount <= 0) return emptyList()
    val steps = mutableListOf<CommandPaletteFocusTarget>()
    var current = CommandPaletteFocusTarget.Result
    while (true) {
        val next = commandPaletteTabFocusTransition(shift = true, from = current, resultCount = resultCount) ?: break
        steps += next
        current = next
    }
    return steps
}

internal fun nextCommandIndex(selected: Int, size: Int, down: Boolean = true, stay: Boolean = false): Int {
    if (size <= 0) return 0
    if (stay) return selected.coerceIn(0, size - 1)
    return if (down) (selected + 1).coerceAtMost(size - 1) else (selected - 1).coerceAtLeast(0)
}

private fun matchesShortcut(event: androidx.compose.ui.input.key.KeyEvent, binding: String): Boolean {
    val tokens = binding.lowercase().split('+').map { it.trim() }.filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return false
    val keyToken = tokens.last()
    val needModifier = tokens.any { it in setOf("meta", "cmd", "command", "super", "win", "ctrl", "control") }
    val hasModifier = event.isMetaPressed || event.isCtrlPressed
    if (needModifier && !hasModifier) return false
    val key = when (keyToken) {
        "k" -> Key.K
        "comma", "," -> Key.Comma
        "f" -> Key.F
        "s" -> Key.S
        else -> return false
    }
    return event.key == key
}
