package com.rememberber.mootool.next.compose.features.color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.ColorEngine
import com.rememberber.mootool.next.compose.domain.ColorException
import com.rememberber.mootool.next.compose.domain.ColorFormat
import com.rememberber.mootool.next.compose.domain.ColorOperation
import com.rememberber.mootool.next.compose.domain.ColorThemeId
import com.rememberber.mootool.next.compose.domain.RgbColor
import com.rememberber.mootool.next.compose.domain.ScreenColorPicker
import com.rememberber.mootool.next.compose.domain.ScreenColorSampler
import com.rememberber.mootool.next.compose.domain.ScreenPickerCopy
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.ColorSession
import com.rememberber.mootool.next.compose.storage.ColorFavoriteFolder
import com.rememberber.mootool.next.compose.storage.ColorFavoriteItem
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.JColorChooser
import java.awt.Color as AwtColor

@Composable
fun ColorBoardScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.colorSession() }
    val revision by container.sessionManager.revision.collectAsState()
    var historyItems by remember { mutableStateOf(emptyList<HistoryRecord>()) }
    var folders by remember { mutableStateOf(emptyList<ColorFavoriteFolder>()) }
    var favoriteItems by remember { mutableStateOf(emptyList<ColorFavoriteItem>()) }
    val colors = MooTheme.colors
    val scope = rememberCoroutineScope()

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistColor()
    }

    LaunchedEffect(session.historyOpen, session.historyTick, revision) {
        if (session.historyOpen) historyItems = container.history.list(ToolId.ColorBoard.id)
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

    Column(Modifier.fillMaxSize().background(colors.workspace)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("color.title"), color = colors.textPrimary, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            if (!detached) {
                MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.ColorBoard) })
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.surfaceSubtle)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MooButton(
                if (session.picking) container.t("common.processing") else container.t("color.picker"),
                enabled = !session.picking,
                onClick = { pickScreen(container, session, scope, ::refresh) }
            )
            MooButton(container.t("color.freePick"), onClick = { pickFree(container, session, scope, ::refresh) })
            ColorFormat.entries.forEach { format ->
                MooButton(formatLabel(format), primary = session.format == format, onClick = {
                    session.format = format
                    session.code = ColorEngine.formatColor(session.primary, format)
                    refresh()
                })
            }
            MooTextField(
                session.code,
                { session.code = it; session.error = ""; refresh() },
                modifier = Modifier.width(160.dp),
                placeholder = container.t("color.code")
            )
            MooButton(container.t("color.apply"), onClick = { applyCode(container, session, ::refresh) })
            MooButton(container.t("common.action.copy"), onClick = {
                session.notice = copyText(session.code, container)
                refresh()
            })
            MooButton(container.t("color.favorite"), onClick = {
                session.favoriteName = "Color-${session.primaryHex}"
                session.saveFavoriteOpen = true
                refresh()
            })
            MooButton(container.t("color.favorites"), onClick = { session.favoritesOpen = true; refresh() })
            MooButton(container.t("common.action.history"), onClick = { session.historyOpen = true; session.historyTick += 1; refresh() })
        }
        Row(Modifier.weight(1f).fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CurrentPanel(container, session, Modifier.width(280.dp).fillMaxHeight(), ::refresh)
            PalettePanel(container, session, Modifier.weight(1f).fillMaxHeight(), ::refresh)
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(26.dp).background(colors.toolbar).padding(horizontal = 12.dp),
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

    if (session.historyOpen) {
        ColorHistoryDialog(container, session, historyItems) { refresh() }
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
            Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp)).background(session.primary.toCompose()),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(container.t("color.current"), color = ColorEngine.parseColor(text).toCompose(), fontSize = 12.sp)
                Text(session.primaryHex, color = ColorEngine.parseColor(text).toCompose(), fontSize = 22.sp)
                Text(ColorEngine.formatColor(session.primary, ColorFormat.RGB), color = ColorEngine.parseColor(text).toCompose(), fontSize = 12.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(container.t("color.compare"), color = colors.textSecondary, fontSize = 12.sp)
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).border(1.dp, colors.border, RoundedCornerShape(6.dp))
                    .background(session.secondary.toCompose())
                    .clickable { pickFreeSecondary(container, session, onChanged) }
            )
            Text(session.secondaryHex, color = colors.textPrimary, fontSize = 13.sp)
            MooButton(container.t("color.operation.swap"), onClick = { swapColors(container, session, onChanged) })
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ColorOperation.entries.forEach { operation ->
                MooButton(container.t(operationKey(operation)), onClick = {
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
                MooButton(container.t(themeKey(id)), primary = session.theme == id, onClick = {
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
    Box(
        Modifier.size(size)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, MooTheme.colors.border, RoundedCornerShape(4.dp))
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
private fun ColorHistoryDialog(
    container: AppContainer,
    session: ColorSession,
    items: List<HistoryRecord>,
    onChanged: () -> Unit
) {
    Dialog(onDismissRequest = { session.historyOpen = false; onChanged() }) {
        Column(
            Modifier.width(520.dp).height(420.dp).background(MooTheme.colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("common.action.history"), color = MooTheme.colors.textPrimary)
            if (items.isEmpty()) {
                Text(container.t("json.history.empty"), color = MooTheme.colors.textSecondary)
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(items) { item ->
                        Column(Modifier.fillMaxWidth().clickable {
                            restoreHistory(container, session, item, onChanged)
                        }.padding(8.dp)) {
                            Text(item.summary, color = MooTheme.colors.textPrimary, fontSize = 13.sp)
                            Text(item.output.ifBlank { item.input }, color = MooTheme.colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.clear"), onClick = {
                    container.history.clear(ToolId.ColorBoard.id)
                    session.historyTick += 1
                    onChanged()
                })
                MooButton(container.t("common.close"), onClick = { session.historyOpen = false; onChanged() })
            }
        }
    }
}

@Composable
private fun SaveColorFavoriteDialog(
    container: AppContainer,
    session: ColorSession,
    folders: List<ColorFavoriteFolder>,
    onChanged: () -> Unit
) {
    Dialog(onDismissRequest = { session.saveFavoriteOpen = false; onChanged() }) {
        Column(
            Modifier.width(440.dp).background(MooTheme.colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
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
                MooButton(container.t("color.favorite"), primary = true, enabled = session.favoriteFolderId.isNotBlank(), onClick = {
                    container.colorFavorites.addItem(session.favoriteFolderId, session.favoriteName, session.primaryHex)
                    session.notice = container.t("favorite.saved")
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
    Dialog(onDismissRequest = { session.favoritesOpen = false; onChanged() }) {
        Column(
            Modifier.width(560.dp).height(460.dp).background(MooTheme.colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("color.favorites"), color = MooTheme.colors.textPrimary)
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
            if (items.isEmpty()) {
                Text(container.t("favorite.empty"), color = MooTheme.colors.textSecondary, modifier = Modifier.weight(1f))
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(items) { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(22.dp).clip(RoundedCornerShape(4.dp))
                                    .background(runCatching { ColorEngine.parseColor(item.value).toCompose() }.getOrDefault(Color.Transparent))
                            )
                            Column(Modifier.weight(1f).padding(horizontal = 8.dp).clickable {
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
) {
    session.picking = true
    session.error = ""
    session.notice = container.t("common.processing")
    onChanged()
    val copy = ScreenPickerCopy(container.t("color.pickerOverlayHint"), container.t("color.pickerOverlayKeys"))
    scope.launch(Dispatchers.Default) {
        val capture = runCatching { ScreenColorSampler.captureAllScreens() }
        withContext(Dispatchers.Swing) {
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
                session.notice = ""
                session.error = messageFor(container, error)
                onChanged()
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
            session.notice = ""
            session.error = messageFor(container, error)
            onChanged()
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
            session.notice = ""
            session.error = messageFor(container, error)
            onChanged()
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
        session.format.name
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
    container.history.save(ToolId.ColorBoard.id, label, label, before, session.primaryHex, operation.name)
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
        "swap"
    )
    onChanged()
}

private fun restoreHistory(container: AppContainer, session: ColorSession, item: HistoryRecord, onChanged: () -> Unit) {
    val hex = ColorEngine.extractHex(item.output.ifBlank { item.input })
    if (hex == null) {
        session.error = container.t("color.error.invalid")
        onChanged()
        return
    }
    session.historyOpen = false
    selectHex(container, session, hex, false, item.summary, onChanged)
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
            session.error = if (error.message == "duplicate") container.t("favorite.duplicateFolder") else container.t("color.error.folder")
            onChanged()
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
            session.error = if (error.message == "duplicate") container.t("favorite.duplicateFolder") else container.t("color.error.folder")
            onChanged()
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

private fun messageFor(container: AppContainer, error: Throwable): String {
    val code = (error as? ColorException)?.code
    return when (code) {
        "invalid-hex", "invalid-rgb" -> container.t("color.error.invalid")
        "permission", "picker" -> container.t("color.error.permission")
        else -> error.message ?: container.t("color.error.generic")
    }
}

private fun copyText(value: String, container: AppContainer): String {
    if (value.isEmpty()) return container.t("color.nothingToCopy")
    return runCatching {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
        container.t("json.notice.copied")
    }.getOrElse { container.t("common.copyFailed") }
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
