package com.rememberber.mootool.next.compose.features.color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.FavoritePresentation
import com.rememberber.mootool.next.compose.domain.ColorEngine
import com.rememberber.mootool.next.compose.domain.ColorWiringPresentation
import com.rememberber.mootool.next.compose.domain.ColorHistoryMetadata
import com.rememberber.mootool.next.compose.domain.ColorHistoryRestore
import com.rememberber.mootool.next.compose.domain.ColorFormat
import com.rememberber.mootool.next.compose.domain.ColorOperation
import com.rememberber.mootool.next.compose.domain.ColorThemeId
import com.rememberber.mootool.next.compose.domain.RgbColor
import com.rememberber.mootool.next.compose.domain.ScreenCaptureFailureMessages
import com.rememberber.mootool.next.compose.domain.ScreenColorPicker
import com.rememberber.mootool.next.compose.domain.ScreenColorSampler
import com.rememberber.mootool.next.compose.domain.ScreenPickerCopy
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.ColorSession
import com.rememberber.mootool.next.compose.storage.ColorFavoriteFolder
import com.rememberber.mootool.next.compose.storage.ColorFavoriteItem
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooMenu
import com.rememberber.mootool.next.compose.ui.components.MooMenuItem
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.VerticalPaneHandle
import com.rememberber.mootool.next.compose.ui.components.setPaneSize
import com.rememberber.mootool.next.compose.ui.components.mooColorFormatRow
import com.rememberber.mootool.next.compose.ui.components.mooColorHexColumn
import com.rememberber.mootool.next.compose.ui.components.mooColorPreviewPane
import com.rememberber.mootool.next.compose.ui.components.mooFocusClickable
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.mooStatusBarBackground
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.OverflowAction
import com.rememberber.mootool.next.compose.ui.components.OverflowActionCluster
import com.rememberber.mootool.next.compose.ui.components.MooOverlay
import com.rememberber.mootool.next.compose.ui.components.mooDialogSurface
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.CopyFeedbackPolicy
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import com.rememberber.mootool.next.compose.sessions.dismissModalOverlays
import com.rememberber.mootool.next.compose.ui.workbench.DismissModalOverlaysOnDispose
import com.rememberber.mootool.next.compose.ui.workbench.onUserInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import javax.swing.JColorChooser
import java.awt.Color as AwtColor

@Composable
fun ColorBoardScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.colorSession() }
    var screenPickJob by remember { mutableStateOf<Job?>(null) }
    DismissModalOverlaysOnDispose(container, ToolId.ColorBoard) {
        screenPickJob?.cancel()
        screenPickJob = null
        session.dismissModalOverlays()
    }
    val revision by container.sessionManager.revision.collectAsState()
    val sessionGeneration by container.sessionManager.sessionGeneration.collectAsState()
    val settings by container.settings.collectAsState()
    var folders by remember { mutableStateOf(emptyList<ColorFavoriteFolder>()) }
    var favoriteItems by remember { mutableStateOf(emptyList<ColorFavoriteItem>()) }
    val colors = MooTheme.colors
    val scope = rememberCoroutineScope()

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistColor()
    }

    LaunchedEffect(session.copyGeneration, session.copyState) {
        if (session.copyState == CopyFeedbackPolicy.IDLE) return@LaunchedEffect
        delay(CopyFeedbackPolicy.RESET_MS)
        session.copyState = CopyFeedbackPolicy.IDLE
        refresh()
    }

    fun copyCode() {
        if (session.code.isEmpty()) {
            session.notice = container.t("color.nothingToCopy")
            session.copyState = CopyFeedbackPolicy.IDLE
        } else {
            session.notice = copyText(session.code, container)
            session.copyState = CopyFeedbackPolicy.afterCopy(session.notice != container.t("common.copyFailed"))
            session.copyGeneration += 1
        }
        refresh()
    }

    fun openFavorite() {
        session.favoriteName = "Color-${session.primaryHex}"
        session.saveFavoriteOpen = true
        refresh()
    }

    fun openFavorites() {
        session.favoritesOpen = true
        refresh()
    }

    fun openHistory() {
        session.historyOpen = true
        session.historyTick += 1
        refresh()
    }

    LaunchedEffect(session.favoritesOpen, session.saveFavoriteOpen, session.favoriteFolderId, revision) {
        if (session.favoritesOpen || session.saveFavoriteOpen) {
            val defaultFolder = container.colorFavorites.ensureDefaultFolder(container.t("color.theme.default"))
            folders = container.colorFavorites.folders()
            if (session.favoriteFolderId.isBlank() || folders.none { it.id == session.favoriteFolderId }) {
                session.favoriteFolderId = defaultFolder.id
            }
            favoriteItems = container.colorFavorites.items(session.favoriteFolderId)
        }
    }
    LaunchedEffect(settings.data.directory, sessionGeneration) {
        if (session.favoritesOpen || session.saveFavoriteOpen) {
            val defaultFolder = container.colorFavorites.ensureDefaultFolder(container.t("color.theme.default"))
            folders = container.colorFavorites.folders()
            if (session.favoriteFolderId.isBlank() || folders.none { it.id == session.favoriteFolderId }) {
                session.favoriteFolderId = defaultFolder.id
            }
            favoriteItems = container.colorFavorites.items(session.favoriteFolderId)
        }
        refresh()
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
    val contentMaxWidth = maxWidth.value
    val overflow = LayoutPolicy.overflowToolbar(contentMaxWidth)
    val minLeft = 240f
    val minRight = 420f
    val paneHandle = 10f
    val maxLeft = (contentMaxWidth - paneHandle - minRight).coerceAtLeast(minLeft)
    val defaultLeft = (contentMaxWidth * 0.34f).coerceIn(minLeft, maxLeft)
    val leftWidth = settings.layout.pane(ToolId.ColorBoard.id, 0, defaultLeft, minLeft, maxLeft)
    var moreOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(colors.workspace)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MooPageTitle(container.t("color.title"))
            Spacer(Modifier.weight(1f))
            OverflowActionCluster(
                overflow = overflow,
                moreLabel = container.t("json.action.overflow"),
                actions = buildList {
                    if (!detached) add(OverflowAction(container.t("app.tool.detach")) { container.sessionManager.detach(ToolId.ColorBoard) })
                }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().mooColorFormatRow().background(colors.surfaceSubtle)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MooButton(
                if (session.picking) container.t("common.processing") else container.t("color.picker"),
                enabled = ColorWiringPresentation.canScreenPick(session.picking),
                p5Toolbar = true,
                onClick = {
                    screenPickJob?.cancel()
                    screenPickJob = pickScreen(container, session, scope, ::refresh)
                }
            )
            MooButton(container.t("color.freePick"), onClick = { pickFree(container, session, scope, ::refresh) }, p5Toolbar = true)
            ColorFormat.entries.forEach { format ->
                MooButton(formatLabel(format), primary = session.format == format, p5Toolbar = true, onClick = {
                    session.format = format
                    session.code = ColorWiringPresentation.runFormatColor(session.primary, format)
                    refresh()
                })
            }
            MooTextField(
                session.code,
                {
                    session.onUserInput { session.code = it; session.error = "" }
                    refresh()
                },
                modifier = Modifier.mooColorHexColumn(),
                placeholder = container.t("color.code"),
                compact = true
            )
            MooButton(
                container.t("color.apply"),
                enabled = ColorWiringPresentation.canApplyCode(session.code),
                onClick = { applyCode(container, session, ::refresh) },
                p5Toolbar = true,
            )
            if (!overflow) {
                MooButton(
                    container.t(CopyFeedbackPolicy.buttonKey(session.copyState, "common.action.copy")),
                    onClick = { copyCode() },
                    p5Toolbar = true
                )
                MooButton(container.t("color.favorite"), onClick = { openFavorite() }, p5Toolbar = true)
                MooButton(container.t("color.favorites"), onClick = { openFavorites() }, p5Toolbar = true)
                MooButton(container.t("common.action.history"), onClick = { openHistory() }, p5Toolbar = true)
            } else {
                Box {
                    MooButton(container.t("json.action.overflow"), primary = moreOpen, onClick = { moreOpen = true }, p5Toolbar = true)
                    MooMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                        MooMenuItem(onClick = { moreOpen = false; copyCode() }) {
                            Text(container.t(CopyFeedbackPolicy.buttonKey(session.copyState, "common.action.copy")))
                        }
                        MooMenuItem(onClick = { moreOpen = false; openFavorite() }) {
                            Text(container.t("color.favorite"))
                        }
                        MooMenuItem(onClick = { moreOpen = false; openFavorites() }) {
                            Text(container.t("color.favorites"))
                        }
                        MooMenuItem(onClick = { moreOpen = false; openHistory() }) {
                            Text(container.t("common.action.history"))
                        }
                    }
                }
            }
        }
        Row(Modifier.weight(1f).fillMaxWidth()) {
            CurrentPanel(
                container,
                session,
                Modifier.width(leftWidth.dp).widthIn(min = 240.dp).fillMaxHeight().padding(16.dp),
                ::refresh
            )
            VerticalPaneHandle(
                onDelta = { container.setPaneSize(ToolId.ColorBoard.id, 0, leftWidth + it, 1) },
                onReset = { container.setPaneSize(ToolId.ColorBoard.id, 0, defaultLeft, 1) }
            )
            PalettePanel(container, session, Modifier.weight(1f).widthIn(min = 420.dp).fillMaxHeight().padding(16.dp), ::refresh)
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.statusBar).mooStatusBarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                session.error.ifEmpty { session.notice },
                color = if (session.error.isNotEmpty()) colors.danger else colors.textSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.weight(1f))
            if (detached) Text("detached", color = colors.textSecondary, fontSize = 12.sp)
        }
    }
    }

    if (session.historyOpen) {
        HistoryBrowser(
            container = container,
            toolId = ToolId.ColorBoard.id,
            title = container.t("common.action.history"),
            onRestore = { item ->
                when (ColorHistoryRestore.apply(session, item)) {
                    ColorHistoryRestore.Result.Ok -> {
                        session.historyOpen = false
                        refresh()
                    }
                    ColorHistoryRestore.Result.InvalidColor -> {
                        notifyColorFailure(container, session, container.t("color.error.invalid")) { refresh() }
                    }
                }
            },
            onDismiss = { session.historyOpen = false; refresh() }
        )
    }
    if (session.saveFavoriteOpen) {
        SaveColorFavoriteDialog(container, session, folders) {
            folders = container.colorFavorites.folders()
            refresh()
        }
    }
    if (session.favoritesOpen) {
        ColorFavoritesDialog(container, session, folders, favoriteItems) {
            folders = container.colorFavorites.folders()
            favoriteItems = container.colorFavorites.items(session.favoriteFolderId)
            refresh()
        }
    }
}

@Composable
private fun CurrentPanel(
    container: AppContainer,
    session: ColorSession,
    modifier: Modifier,
    onChanged: () -> Unit
) {
    val colors = MooTheme.colors
    val text = ColorEngine.bestTextColor(session.primary)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.fillMaxWidth().mooColorPreviewPane().clip(RoundedCornerShape(8.dp))
                .border(1.dp, ColorEngine.parseColor(text).toCompose().copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                .background(session.primary.toCompose())
                .padding(18.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Column {
                Text(container.t("color.current"), color = ColorEngine.parseColor(text).toCompose(), fontSize = 11.sp)
                Text(session.primaryHex, color = ColorEngine.parseColor(text).toCompose(), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 4.dp))
                Text(ColorWiringPresentation.runFormatColor(session.primary, ColorFormat.RGB), color = ColorEngine.parseColor(text).toCompose(), fontSize = 11.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(container.t("color.compare"), color = colors.textMuted, fontSize = 11.sp)
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).border(1.dp, colors.border, RoundedCornerShape(6.dp))
                    .background(session.secondary.toCompose())
                    .mooFocusClickable { pickFreeSecondary(container, session, onChanged) }
            )
            Text(session.secondaryHex, color = colors.textPrimary, fontSize = 13.sp)
            MooButton(container.t("color.operation.swap"), onClick = { swapColors(container, session, onChanged) }, p5Toolbar = true)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ColorOperation.entries.forEach { operation ->
                MooButton(container.t(operationKey(operation)), p5Toolbar = true, onClick = {
                    runOperation(container, session, operation, onChanged)
                })
            }
        }
    }
}

@Composable
private fun PalettePanel(
    container: AppContainer,
    session: ColorSession,
    modifier: Modifier,
    onChanged: () -> Unit
) {
    val colors = MooTheme.colors
    val theme = ColorEngine.theme(session.theme)
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(container.t("color.themeColors"), color = colors.textPrimary, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ColorThemeId.entries.forEach { id ->
                MooButton(container.t(themeKey(id)), primary = session.theme == id, p5Toolbar = true, onClick = {
                    session.theme = id
                    onChanged()
                })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            theme.main.forEach { hex ->
                ColorChip(hex, 36.dp) { secondary -> selectHex(container, session, hex, secondary, container.t("color.select"), onChanged) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            theme.shades.forEach { column ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    column.forEach { hex ->
                        ColorChip(hex, 22.dp) { secondary -> selectHex(container, session, hex, secondary, container.t("color.select"), onChanged) }
                    }
                }
            }
        }
        Text(container.t("color.standardColors"), color = colors.textPrimary, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ColorEngine.STANDARD_COLORS.forEach { hex ->
                ColorChip(hex, 28.dp) { secondary -> selectHex(container, session, hex, secondary, container.t("color.select"), onChanged) }
            }
        }
        Text(container.t("color.shiftHint"), color = colors.textSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun ColorChip(hex: String, size: androidx.compose.ui.unit.Dp, onSelect: (Boolean) -> Unit) {
    val rgb = runCatching { ColorEngine.parseColor(hex) }.getOrNull()
    val colors = MooTheme.colors
    val interaction = remember(hex) { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val chipShape = RoundedCornerShape(4.dp)
    Box(
        Modifier.size(size)
            .hoverable(interaction)
            .drawWithContent {
                drawContent()
                if (hovered) {
                    val pad = 1.dp.toPx()
                    drawRoundRect(
                        color = colors.accent,
                        topLeft = Offset(-pad, -pad),
                        size = Size(this.size.width + pad * 2, this.size.height + pad * 2),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
            .clip(chipShape)
            .border(1.dp, colors.textStrong.copy(alpha = 0.12f), chipShape)
            .background(rgb?.toCompose() ?: Color.Transparent)
            .pointerHoverIcon(PointerIcon.Hand)
            .pointerInput(hex) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Release) {
                            onSelect(event.keyboardModifiers.isShiftPressed)
                        }
                    }
                }
            }
    )
}


@Composable
private fun SaveColorFavoriteDialog(
    container: AppContainer,
    session: ColorSession,
    folders: List<ColorFavoriteFolder>,
    onChanged: () -> Unit
) {
    MooOverlay(onDismiss = { session.saveFavoriteOpen = false; onChanged() }) {
        Column(
            Modifier.width(440.dp).mooDialogSurface().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("color.favoriteDialog"), color = MooTheme.colors.textPrimary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(session.primary.toCompose()))
                Text(session.primaryHex, color = MooTheme.colors.textPrimary)
            }
            MooTextField(
                session.favoriteName,
                { session.favoriteName = it; onChanged() },
                modifier = Modifier.fillMaxWidth(),
                placeholder = container.t("favorite.namePlaceholder")
            )
            Text(container.t("favorite.folder"), color = MooTheme.colors.textSecondary, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                folders.forEach { folder ->
                    MooButton(folder.title, primary = session.favoriteFolderId == folder.id, onClick = {
                        session.favoriteFolderId = folder.id
                        onChanged()
                    })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                MooTextField(
                    session.folderTitle,
                    { session.folderTitle = it; onChanged() },
                    modifier = Modifier.weight(1f),
                    placeholder = container.t("favorite.folderName")
                )
                MooButton(container.t("favorite.newFolder"), onClick = { addFolder(container, session, onChanged) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.cancel"), onClick = { session.saveFavoriteOpen = false; onChanged() })
                MooButton(
                    container.t("color.favorite"),
                    prominent = true,
                    enabled = ColorWiringPresentation.canFavorite(session.favoriteFolderId),
                    onClick = {
                    container.colorFavorites.addItem(session.favoriteFolderId, session.favoriteName, session.primaryHex)
                    session.notice = container.t("favorite.saved")
                    container.toastSuccess(container.t("favorite.saved"))
                    session.error = ""
                    session.saveFavoriteOpen = false
                    onChanged()
                })
            }
        }
    }
}

@Composable
private fun ColorFavoritesDialog(
    container: AppContainer,
    session: ColorSession,
    folders: List<ColorFavoriteFolder>,
    items: List<ColorFavoriteItem>,
    onChanged: () -> Unit
) {
    var favoriteQuery by remember { mutableStateOf("") }
    val visible = items.filter {
        FavoritePresentation.matchesQuery(favoriteQuery, groupFilter = "", it.name, it.value, group = "")
    }
    MooOverlay(onDismiss = { session.favoritesOpen = false; onChanged() }) {
        Column(
            Modifier.width(560.dp).height(460.dp).mooDialogSurface().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("color.favorites"), color = MooTheme.colors.textPrimary)
            MooTextField(
                favoriteQuery,
                { favoriteQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = container.t("favorite.queryPlaceholder")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                folders.forEach { folder ->
                    MooButton(folder.title, primary = session.favoriteFolderId == folder.id, onClick = {
                        session.favoriteFolderId = folder.id
                        onChanged()
                    })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                MooTextField(
                    session.folderTitle,
                    { session.folderTitle = it; onChanged() },
                    modifier = Modifier.weight(1f),
                    placeholder = container.t("favorite.folderName")
                )
                MooButton(container.t("favorite.newFolder"), onClick = { addFolder(container, session, onChanged) })
                MooButton(container.t("color.renameFolder"), onClick = { renameFolder(container, session, onChanged) })
                MooButton(container.t("color.deleteFolder"), onClick = { deleteFolder(container, session, onChanged) })
            }
            if (visible.isEmpty()) {
                Text(container.t("favorite.empty"), color = MooTheme.colors.textSecondary, modifier = Modifier.weight(1f))
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(visible) { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(22.dp).clip(RoundedCornerShape(4.dp))
                                    .background(runCatching { ColorEngine.parseColor(item.value).toCompose() }.getOrDefault(Color.Transparent))
                            )
                            Column(Modifier.weight(1f).padding(horizontal = 8.dp).mooFocusClickable {
                                selectHex(container, session, item.value, false, container.t("color.favorites"), onChanged)
                                session.favoritesOpen = false
                                onChanged()
                            }) {
                                Text(item.name, color = MooTheme.colors.textPrimary, fontSize = 13.sp)
                                Text(item.value, color = MooTheme.colors.textSecondary, fontSize = 12.sp)
                            }
                            MooButton(container.t("common.delete"), onClick = {
                                container.colorFavorites.deleteItem(item.id)
                                session.notice = container.t("favorite.deleted")
                                container.toastSuccess(container.t("favorite.deleted"))
                                onChanged()
                            })
                        }
                    }
                }
            }
            MooButton(container.t("common.close"), onClick = { session.favoritesOpen = false; onChanged() })
        }
    }
}

private fun pickScreen(
    container: AppContainer,
    session: ColorSession,
    scope: kotlinx.coroutines.CoroutineScope,
    onChanged: () -> Unit
): Job {
    session.picking = true
    session.error = ""
    session.notice = container.t("common.processing")
    onChanged()
    val copy = ScreenPickerCopy(container.t("color.pickerOverlayHint"), container.t("color.pickerOverlayKeys"))
    return scope.launch(Dispatchers.Default) {
        val capture = runCatching { ScreenColorSampler.captureAllScreens() }
        if (!isActive) {
            session.picking = false
            return@launch
        }
        withContext(Dispatchers.Swing) {
            if (!isActive) {
                session.picking = false
                onChanged()
                return@withContext
            }
            capture.onSuccess { image ->
                ScreenColorPicker.show(
                    image,
                    copy,
                    onPicked = { color ->
                        session.picking = false
                        selectColor(container, session, color, false, container.t("color.picker"), onChanged)
                    },
                    onCancel = {
                        session.picking = false
                        session.notice = container.t("color.pickerCancelled")
                        session.error = ""
                        onChanged()
                    }
                )
            }.onFailure { error ->
                session.picking = false
                notifyColorFailure(container, session, error, onChanged)
            }
        }
    }
}

private fun pickFree(
    container: AppContainer,
    session: ColorSession,
    scope: kotlinx.coroutines.CoroutineScope,
    onChanged: () -> Unit
) {
    scope.launch(Dispatchers.Swing) {
        val initial = AwtColor(session.primary.r, session.primary.g, session.primary.b)
        val chosen = JColorChooser.showDialog(null, container.t("color.freePick"), initial) ?: return@launch
        selectColor(container, session, RgbColor(chosen.red, chosen.green, chosen.blue), false, container.t("color.freePick"), onChanged)
    }
}

private fun pickFreeSecondary(container: AppContainer, session: ColorSession, onChanged: () -> Unit) {
    val initial = AwtColor(session.secondary.r, session.secondary.g, session.secondary.b)
    val chosen = JColorChooser.showDialog(null, container.t("color.compare"), initial) ?: return
    session.secondary = RgbColor(chosen.red, chosen.green, chosen.blue)
    session.error = ""
    onChanged()
}

private fun applyCode(container: AppContainer, session: ColorSession, onChanged: () -> Unit) {
    runCatching { ColorEngine.parseColor(session.code) }
        .onSuccess { color -> selectColor(container, session, color, false, container.t("color.inputColor"), onChanged) }
        .onFailure { error ->
            notifyColorFailure(container, session, error, onChanged)
        }
}

private fun selectHex(
    container: AppContainer,
    session: ColorSession,
    hex: String,
    asSecondary: Boolean,
    operation: String,
    onChanged: () -> Unit
) {
    runCatching { ColorEngine.parseColor(hex) }
        .onSuccess { selectColor(container, session, it, asSecondary, operation, onChanged) }
        .onFailure { error ->
            notifyColorFailure(container, session, error, onChanged)
        }
}

private fun selectColor(
    container: AppContainer,
    session: ColorSession,
    color: RgbColor,
    asSecondary: Boolean,
    operation: String,
    onChanged: () -> Unit
) {
    if (asSecondary) {
        session.secondary = color
        session.error = ""
        session.notice = container.t("color.compare")
        onChanged()
        return
    }
    val before = session.primaryHex
    session.primary = color
    session.code = ColorEngine.formatColor(color, session.format)
    session.error = ""
    session.notice = operation
    container.history.save(
        ToolId.ColorBoard.id,
        operation,
        operation,
        before,
        session.primaryHex,
        ColorHistoryMetadata.encode(session.format, "pick")
    )
    onChanged()
}

private fun runOperation(
    container: AppContainer,
    session: ColorSession,
    operation: ColorOperation,
    onChanged: () -> Unit
) {
    val before = "${session.primaryHex} / ${session.secondaryHex}"
    val output = ColorEngine.apply(operation, session.primary, session.secondary)
    session.primary = output
    session.code = ColorEngine.formatColor(output, session.format)
    session.error = ""
    val label = container.t(operationKey(operation))
    session.notice = label
    container.history.save(
        ToolId.ColorBoard.id,
        label,
        label,
        before,
        session.primaryHex,
        ColorHistoryMetadata.encode(session.format, operation.name),
    )
    onChanged()
}

private fun swapColors(container: AppContainer, session: ColorSession, onChanged: () -> Unit) {
    val before = "${session.primaryHex} / ${session.secondaryHex}"
    val primary = session.primary
    session.primary = session.secondary
    session.secondary = primary
    session.code = ColorEngine.formatColor(session.primary, session.format)
    session.error = ""
    session.notice = container.t("color.operation.swap")
    container.history.save(
        ToolId.ColorBoard.id,
        session.notice,
        session.notice,
        before,
        "${session.primaryHex} / ${session.secondaryHex}",
        ColorHistoryMetadata.encode(session.format, "swap"),
    )
    onChanged()
}

private fun addFolder(container: AppContainer, session: ColorSession, onChanged: () -> Unit) {
    runCatching { container.colorFavorites.addFolder(session.folderTitle) }
        .onSuccess { folder ->
            session.favoriteFolderId = folder.id
            session.folderTitle = ""
            session.error = ""
            session.notice = container.t("favorite.newFolder")
            onChanged()
        }
        .onFailure { error ->
            val message = if (error.message == "duplicate") container.t("favorite.duplicateFolder") else container.t("color.error.folder")
            notifyColorFailure(container, session, message, onChanged)
        }
}

private fun renameFolder(container: AppContainer, session: ColorSession, onChanged: () -> Unit) {
    if (session.favoriteFolderId.isBlank()) return
    runCatching { container.colorFavorites.renameFolder(session.favoriteFolderId, session.folderTitle) }
        .onSuccess {
            session.folderTitle = ""
            session.error = ""
            session.notice = container.t("color.renameFolder")
            onChanged()
        }
        .onFailure { error ->
            val message = if (error.message == "duplicate") container.t("favorite.duplicateFolder") else container.t("color.error.folder")
            notifyColorFailure(container, session, message, onChanged)
        }
}

private fun deleteFolder(container: AppContainer, session: ColorSession, onChanged: () -> Unit) {
    if (session.favoriteFolderId.isBlank()) return
    container.colorFavorites.deleteFolder(session.favoriteFolderId)
    session.favoriteFolderId = container.colorFavorites.ensureDefaultFolder(container.t("color.theme.default")).id
    session.notice = container.t("color.deleteFolder")
    session.error = ""
    onChanged()
}

private fun notifyColorFailure(
    container: AppContainer,
    session: ColorSession,
    message: String,
    onChanged: () -> Unit,
) {
    session.notice = ""
    session.error = message
    if (ColorWiringPresentation.shouldToastErrorMessage()) {
        container.toastError(message)
    }
    onChanged()
}

private fun notifyColorFailure(
    container: AppContainer,
    session: ColorSession,
    error: Throwable,
    onChanged: () -> Unit,
) {
    if (!ColorWiringPresentation.shouldToastOperationFailure(error)) {
        session.notice = ""
        session.error = messageFor(container, error)
        onChanged()
        return
    }
    notifyColorFailure(container, session, messageFor(container, error), onChanged)
}

private fun messageFor(container: AppContainer, error: Throwable): String =
    ScreenCaptureFailureMessages.colorPickerMessage({ container.t(it) }, error)

private fun copyText(value: String, container: AppContainer): String {
    if (value.isEmpty()) return container.t("color.nothingToCopy")
    return if (container.copyText(value)) container.t("json.notice.copied") else container.t("common.copyFailed")
}

private fun formatLabel(format: ColorFormat): String = when (format) {
    ColorFormat.HEX_UPPER -> "HTML"
    ColorFormat.HEX_LOWER -> "html"
    ColorFormat.RGB -> "RGB"
}

private fun operationKey(operation: ColorOperation): String = when (operation) {
    ColorOperation.Invert -> "color.operation.invert"
    ColorOperation.Intersect -> "color.operation.intersect"
    ColorOperation.Add -> "color.operation.add"
    ColorOperation.Difference -> "color.operation.difference"
    ColorOperation.Average -> "color.operation.average"
}

private fun themeKey(id: ColorThemeId): String = "color.theme.${ColorEngine.canonicalThemeId(id)}"

private fun RgbColor.toCompose(): Color = Color(r, g, b)
