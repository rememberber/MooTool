package com.rememberber.mootool.next.compose.features.json

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.FindReplace
import com.rememberber.mootool.next.compose.domain.JsonEngine
import com.rememberber.mootool.next.compose.domain.JsonStatus
import com.rememberber.mootool.next.compose.domain.JsonTranslator
import com.rememberber.mootool.next.compose.domain.VaultChangeKind
import com.rememberber.mootool.next.compose.domain.VaultConflictEngine
import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.domain.VaultRevisionMonitor
import com.rememberber.mootool.next.compose.domain.VaultSearchIndex
import com.rememberber.mootool.next.compose.features.git.VaultGitDialog
import com.rememberber.mootool.next.compose.features.vault.VaultConflictDialog
import com.rememberber.mootool.next.compose.editor.EditorAppShortcuts
import com.rememberber.mootool.next.compose.editor.EditorHost
import com.rememberber.mootool.next.compose.editor.EditorLimits
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.JsonSession
import com.rememberber.mootool.next.compose.storage.VaultEntry
import com.rememberber.mootool.next.compose.ui.components.FontSelect
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooSegmented
import com.rememberber.mootool.next.compose.ui.components.MooSwitch
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.VaultContextAction
import com.rememberber.mootool.next.compose.ui.components.VaultContextId
import com.rememberber.mootool.next.compose.ui.components.VaultSortMenu
import com.rememberber.mootool.next.compose.ui.components.VaultTreeList
import com.rememberber.mootool.next.compose.domain.VaultMove
import com.rememberber.mootool.next.compose.ui.components.VerticalPaneHandle
import com.rememberber.mootool.next.compose.ui.components.setPaneSize
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.CopyFeedbackPolicy
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import javax.swing.SwingUtilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun JsonScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.jsonSession(container.settings.value.editor.softWrap) }
    val settings by container.settings.collectAsState()
    val revision by container.sessionManager.revision.collectAsState()
    var tick by remember { mutableStateOf(0L) }
    var filterRev by remember { mutableStateOf(0) }
    var snapshot by remember { mutableStateOf(container.jsonVault.snapshot(container.settings.value.vault.hideGitignoredFiles)) }
    fun refresh() {
        tick += 1
        container.sessionManager.bump()
        container.sessionManager.persistJson()
    }
    fun persistFilter() {
        filterRev += 1
        container.sessionManager.bump()
        container.sessionManager.persistJson()
    }
    val translator = remember(settings.general.language) {
        JsonTranslator { key, params -> container.t(key, params) }
    }
    val status = remember(session.editor.revision, tick, settings.general.language) {
        JsonEngine.validate(session.editor.text, translator)
    }
    var gitOpen by remember { mutableStateOf(false) }
    var conflict by remember { mutableStateOf<VaultConflictState?>(null) }
    var monitor by remember { mutableStateOf<VaultRevisionMonitor?>(null) }
    val colors = MooTheme.colors
    DisposableEffect(container.jsonVault.root(), settings.vault.jsonPath) {
        val next = VaultRevisionMonitor(container.jsonVault.root(), ignoreAttachments = false) { paths ->
            SwingUtilities.invokeLater {
                handleJsonVaultChange(container, session, paths, { conflict = it }, { refresh() })
            }
        }
        next.start()
        monitor = next
        onDispose {
            next.close()
            if (monitor === next) monitor = null
        }
    }
    LaunchedEffect(tick, settings.vault.jsonPath, settings.vault.hideGitignoredFiles) {
        snapshot = withContext(Dispatchers.IO) { container.jsonVault.snapshot(settings.vault.hideGitignoredFiles) }
    }
    val vaultItems = remember(snapshot, session.vaultQuery, session.includeContent, filterRev) {
        VaultSearchIndex.filter(snapshot, session.vaultQuery, session.includeContent)
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.workspace).onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        val meta = event.isMetaPressed || event.isCtrlPressed
        when {
            meta && event.isShiftPressed && event.key == Key.F -> {
                transform(container, session, translator, container.t("json.notice.formatted")) {
                    JsonEngine.format(it, translator, session.formatOptions.spaces)
                }
                refresh()
                true
            }
            meta && event.key == Key.F -> {
                session.findOpen = true
                refresh()
                true
            }
            meta && event.key == Key.S -> {
                saveJsonVault(container, session, monitor, { conflict = it })
                    .onSuccess { session.notice = container.t("common.save") }
                    .onFailure { session.notice = it.message ?: container.t("json.notice.failed") }
                refresh()
                true
            }
            else -> false
        }
    }) {
        val compact = LayoutPolicy.isCompact(maxWidth.value)
        val overflow = LayoutPolicy.overflowToolbar(maxWidth.value)
        Column(Modifier.fillMaxSize()) {
        JsonToolbar(container, session, translator, compact = compact, overflow = overflow, detached = detached, onChanged = { refresh() }, onGit = { gitOpen = true })
        if (session.findOpen) {
            FindBar(container, session, onChanged = { refresh() })
        }
        val vaultWidth = settings.layout.pane(ToolId.Json.id, 0, 240f, 200f, 320f)
        val inspectorWidth = settings.layout.pane(ToolId.Json.id, 1, 280f, 240f, 340f)
        val showVault = LayoutPolicy.showVault(compact, session.compactAux)
        val showInspector = LayoutPolicy.showInspector(compact, session.compactAux, session.inspectorOpen)
        Row(Modifier.weight(1f).fillMaxWidth()) {
            if (showVault) {
                VaultPane(container, session, vaultItems, monitor, { conflict = it }, onChanged = { refresh() }, onFilter = { persistFilter() }, onGit = { gitOpen = true }, width = vaultWidth)
                VerticalPaneHandle(
                    onDelta = { container.setPaneSize(ToolId.Json.id, 0, vaultWidth + it, 2) },
                    onReset = { container.setPaneSize(ToolId.Json.id, 0, 240f, 2) }
                )
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                EditorHost(
                    buffer = session.editor,
                    dark = MooTheme.dark,
                    fontName = com.rememberber.mootool.next.compose.domain.DocumentFormatEngine.editorFont(settings.editor.jsonFontName),
                    fontSize = settings.editor.jsonFontSize,
                    wrap = session.wrap,
                    columnEditing = true,
                    columnDragWithoutAlt = session.columnLatch,
                    shortcuts = EditorAppShortcuts(
                        onFind = {
                            session.findOpen = true
                            refresh()
                        },
                        onFormat = {
                            transform(container, session, translator, container.t("json.notice.formatted")) {
                                JsonEngine.format(it, translator, session.formatOptions.spaces)
                            }
                            refresh()
                        },
                        onSave = {
                            saveJsonVault(container, session, monitor, { conflict = it })
                                .onSuccess { session.notice = container.t("common.save") }
                                .onFailure { session.notice = it.message ?: container.t("json.notice.failed") }
                            refresh()
                        }
                    )
                )
            }
            if (showInspector) {
                VerticalPaneHandle(
                    onDelta = { container.setPaneSize(ToolId.Json.id, 1, inspectorWidth + it, 2) },
                    onReset = { container.setPaneSize(ToolId.Json.id, 1, 280f, 2) }
                )
                InspectorPane(container, session, translator, status, onChanged = { refresh() }, width = inspectorWidth)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.statusBar).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(status.message, color = when (status.kind) {
                com.rememberber.mootool.next.compose.domain.JsonStatus.Kind.Error -> colors.danger
                com.rememberber.mootool.next.compose.domain.JsonStatus.Kind.Valid -> colors.success
                else -> colors.textSecondary
            }, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text(session.notice, color = colors.textSecondary, fontSize = 12.sp)
            if (EditorLimits.exceedsLargeDocument(session.editor.text)) Text(" · ${container.t("editor.largeDocument")}", color = colors.warning, fontSize = 12.sp)
            if (conflict != null) Text(" · ${container.t("vault.conflict.banner")}", color = colors.warning, fontSize = 12.sp)
            if (detached) Text(" · detached", color = colors.textSecondary, fontSize = 12.sp)
        }
        }
    }

    if (gitOpen) {
        VaultGitDialog(
            container = container,
            title = container.t("json.git.title"),
            defaultMessage = container.t("json.git.defaultMessage"),
            root = container.jsonVault.root(),
            onDismiss = { gitOpen = false },
            onFlush = {
                if (session.currentFile.isBlank()) {
                    if (session.editor.text.isNotBlank() && session.editor.text != JsonSession.SAMPLE_JSON) {
                        container.t("git.flush.untitled")
                    } else {
                        null
                    }
                } else {
                    saveJsonVault(container, session, monitor) { conflict = it }
                        .fold(onSuccess = { null }, onFailure = { it.message ?: container.t("quickNote.saveFailed") })
                }
            }
        )
    }
    conflict?.let { pending ->
        VaultConflictDialog(
            container = container,
            conflict = pending,
            onReload = {
                if (pending.deleted) {
                    session.currentFile = ""
                    session.savedText = session.editor.text
                } else {
                    onEdt { session.editor.setText(container.jsonVault.read(pending.relativePath), recordUndo = false) }
                    session.currentFile = pending.relativePath
                    session.savedText = session.editor.text
                }
                conflict = null
                session.notice = container.t("vault.conflict.reloaded")
                refresh()
            },
            onSaveCopy = {
                val copy = VaultConflictEngine.conflictCopyName(pending.relativePath, System.currentTimeMillis())
                runCatching { container.jsonVault.write(copy, pending.editorText) }
                    .onSuccess {
                        monitor?.noteOwnWrite(copy, VaultConflictEngine.sha256Text(pending.editorText))
                        if (pending.deleted) {
                            session.currentFile = copy
                            session.savedText = pending.editorText
                        } else {
                            onEdt { session.editor.setText(container.jsonVault.read(pending.relativePath), recordUndo = false) }
                            session.currentFile = pending.relativePath
                            session.savedText = session.editor.text
                        }
                        conflict = null
                        session.notice = container.t("vault.conflict.savedCopy", mapOf("path" to copy))
                    }
                    .onFailure { session.notice = it.message ?: container.t("json.notice.failed") }
                refresh()
            },
            onKeep = { conflict = null; refresh() }
        )
    }
    if (session.historyOpen) {
        HistoryBrowser(
            container = container,
            toolId = ToolId.Json.id,
            title = container.t("json.history.title"),
            onRestore = { item ->
                onEdt { session.editor.setText(item.output.ifBlank { item.input }, recordUndo = true) }
                session.notice = container.t("json.notice.restored")
                session.historyOpen = false
                refresh()
            },
            onDismiss = { session.historyOpen = false; refresh() }
        )
    }
    if (session.dialogTitle.isNotEmpty()) {
        ResultDialog(container, session) { refresh() }
    }
    if (session.dialogInputMode.isNotEmpty() && !session.dialogInputMode.startsWith("json-")) {
        InputDialog(container, session, translator) { refresh() }
    }
    if (session.pathPickerOpen) {
        PathPickerDialog(container, session) { refresh() }
    }
}

@Composable
private fun JsonToolbar(
    container: AppContainer,
    session: JsonSession,
    translator: JsonTranslator,
    compact: Boolean,
    overflow: Boolean,
    detached: Boolean,
    onChanged: () -> Unit,
    onGit: () -> Unit
) {
    val colors = MooTheme.colors
    val settings by container.settings.collectAsState()
    val inspectorVisible = LayoutPolicy.showInspector(compact, session.compactAux, session.inspectorOpen)
    var moreOpen by remember { mutableStateOf(false) }

    LaunchedEffect(session.copyGeneration, session.copyState) {
        if (session.copyState == CopyFeedbackPolicy.IDLE) return@LaunchedEffect
        delay(CopyFeedbackPolicy.RESET_MS)
        session.copyState = CopyFeedbackPolicy.IDLE
        onChanged()
    }

    fun toggleInspector() {
        if (compact) {
            session.compactAux = LayoutPolicy.toggleAux(session.compactAux, "inspector")
            session.inspectorOpen = session.compactAux == "inspector"
        } else {
            session.inspectorOpen = !session.inspectorOpen
        }
        onChanged()
    }

    fun toggleWrap() {
        session.wrap = !session.wrap
        onChanged()
    }

    fun toggleColumn() {
        session.columnLatch = !session.columnLatch
        session.notice = if (session.columnLatch) container.t("quickNote.columnEdit.hint") else ""
        onChanged()
    }

    fun importFile() {
        chooseFile(false)?.let { file ->
            onEdt {
                session.editor.setText(file.readText(Charsets.UTF_8), recordUndo = true)
                session.notice = container.t("json.notice.imported")
            }
            onChanged()
        }
    }

    fun exportFile() {
        chooseFile(true)?.let { file ->
            file.writeText(session.editor.text, Charsets.UTF_8)
            session.notice = container.t("json.notice.exported")
            onChanged()
        }
    }

    fun openHistory() {
        session.historyOpen = true
        onChanged()
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).background(colors.toolbarBrush()).padding(horizontal = 8.dp).horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MooButton(container.t("json.action.format"), primary = true, onClick = {
            transform(container, session, translator, container.t("json.notice.formatted")) {
                JsonEngine.format(it, translator, session.formatOptions.spaces)
            }
            onChanged()
        })
        MooButton(container.t("json.action.compress"), onClick = {
            transform(container, session, translator, container.t("json.notice.compressed")) {
                JsonEngine.compress(it, translator)
            }
            onChanged()
        })
        FontSelect(
            value = settings.editor.jsonFontName,
            ariaLabel = container.t("json.font"),
            labels = mapOf("ui-monospace" to container.t("quickNote.font.mono")),
            searchPlaceholder = container.t("json.font"),
            onChange = { name ->
                container.updateSettings { it.copy(editor = it.editor.copy(jsonFontName = name)) }
                onChanged()
            }
        )
        if (!overflow) {
            MooButton(if (session.wrap) container.t("json.action.wrap") else container.t("json.action.nowrap"), onClick = { toggleWrap() })
            MooButton(container.t("quickNote.columnEdit"), primary = session.columnLatch, onClick = { toggleColumn() })
        }
        MooButton(container.t(CopyFeedbackPolicy.buttonKey(session.copyState)), onClick = {
            val outcome = copyText(session.editor.text, container)
            session.notice = outcome.notice
            session.copyState = CopyFeedbackPolicy.afterCopy(outcome.success)
            session.copyGeneration += 1
            onChanged()
        })
        MooButton(container.t("json.action.find"), onClick = {
            session.findOpen = !session.findOpen
            onChanged()
        })
        if (!overflow) {
            MooButton(container.t("json.action.import"), onClick = { importFile() })
            MooButton(container.t("json.action.export"), onClick = { exportFile() })
            MooButton(container.t("json.action.history"), onClick = { openHistory() })
            MooButton(container.t("git.action"), onClick = onGit)
        }
        if (compact) {
            MooButton(
                container.t("json.vault.title"),
                primary = session.compactAux == "vault",
                onClick = {
                    session.compactAux = LayoutPolicy.toggleAux(session.compactAux, "vault")
                    onChanged()
                }
            )
        }
        if (overflow) {
            Box {
                MooButton(container.t("json.action.overflow"), primary = inspectorVisible || moreOpen, onClick = { moreOpen = true })
                DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                    DropdownMenuItem(onClick = { moreOpen = false; toggleWrap() }) {
                        Text(if (session.wrap) container.t("json.action.wrap") else container.t("json.action.nowrap"))
                    }
                    DropdownMenuItem(onClick = { moreOpen = false; toggleColumn() }) {
                        Text(container.t("quickNote.columnEdit"))
                    }
                    DropdownMenuItem(onClick = { moreOpen = false; importFile() }) {
                        Text(container.t("json.action.import"))
                    }
                    DropdownMenuItem(onClick = { moreOpen = false; exportFile() }) {
                        Text(container.t("json.action.export"))
                    }
                    DropdownMenuItem(onClick = { moreOpen = false; openHistory() }) {
                        Text(container.t("json.action.history"))
                    }
                    DropdownMenuItem(onClick = { moreOpen = false; onGit() }) {
                        Text(container.t("git.action"))
                    }
                    DropdownMenuItem(onClick = { moreOpen = false; toggleInspector() }) {
                        Text(container.t("json.panel.inspector"))
                    }
                    if (!detached) {
                        DropdownMenuItem(onClick = { moreOpen = false; container.sessionManager.detach(ToolId.Json) }) {
                            Text(container.t("app.tool.detach"))
                        }
                    }
                }
            }
        } else {
            MooButton(container.t("json.action.more"), primary = inspectorVisible, onClick = { toggleInspector() })
        }
        Spacer(Modifier.weight(1f))
        if (!overflow && !detached) {
            MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.Json) })
        }
        MooButton(container.t("json.action.clear"), onClick = {
            onEdt { session.editor.setText("", recordUndo = true) }
            session.notice = container.t("json.notice.cleared")
            onChanged()
        })
    }
}

@Composable
private fun FindBar(container: AppContainer, session: JsonSession, onChanged: () -> Unit) {
    val matches = FindReplace.findAll(session.editor.text, session.findQuery, session.findOptions)
    Row(
        modifier = Modifier.fillMaxWidth().background(MooTheme.colors.surfaceSubtle).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MooTextField(session.findQuery, { session.findQuery = it; onChanged() }, modifier = Modifier.width(220.dp), placeholder = container.t("json.find.placeholder"))
        MooTextField(session.replaceText, { session.replaceText = it; onChanged() }, modifier = Modifier.width(180.dp), placeholder = container.t("json.find.replace"))
        MooButton(container.t("find.matchCase") + ": ${session.findOptions.matchCase}", onClick = {
            session.findOptions = session.findOptions.copy(matchCase = !session.findOptions.matchCase)
            onChanged()
        })
        MooButton(container.t("find.wholeWord") + ": ${session.findOptions.wholeWord}", onClick = {
            session.findOptions = session.findOptions.copy(wholeWord = !session.findOptions.wholeWord)
            onChanged()
        })
        MooButton(container.t("find.regex") + ": ${session.findOptions.regex}", onClick = {
            session.findOptions = session.findOptions.copy(regex = !session.findOptions.regex)
            onChanged()
        })
        Text(container.t("json.find.matches", mapOf("count" to matches.size.toString())), color = MooTheme.colors.textSecondary, fontSize = 12.sp)
        MooButton(container.t("find.previous"), onClick = { jump(session, false); onChanged() })
        MooButton(container.t("find.next"), onClick = { jump(session, true); onChanged() })
        MooButton(container.t("find.replace"), onClick = {
            val (next, match) = FindReplace.replaceCurrent(session.editor.text, session.findQuery, session.replaceText, session.findOptions, session.editor.area.caretPosition)
            onEdt { session.editor.setText(next, recordUndo = true); match?.let { session.editor.select(it.start, it.end) } }
            onChanged()
        })
        MooButton(container.t("find.replaceAll"), onClick = {
            val (next, count) = FindReplace.replaceAll(session.editor.text, session.findQuery, session.replaceText, session.findOptions)
            onEdt { session.editor.setText(next, recordUndo = true) }
            session.notice = container.t("json.find.matches", mapOf("count" to count.toString()))
            onChanged()
        })
        MooButton(container.t("common.close"), onClick = { session.findOpen = false; onChanged() })
    }
}

@Composable
private fun VaultPane(
    container: AppContainer,
    session: JsonSession,
    items: List<VaultEntry>,
    monitor: VaultRevisionMonitor?,
    onConflict: (VaultConflictState) -> Unit,
    onChanged: () -> Unit,
    onFilter: () -> Unit,
    onGit: () -> Unit,
    width: Float
) {
    val colors = MooTheme.colors
    val settings by container.settings.collectAsState()
    Column(Modifier.width(width.dp).fillMaxHeight().background(colors.sidebar).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(container.t("json.vault.title"), color = colors.textPrimary, fontSize = 12.sp)
        MooTextField(session.vaultQuery, { session.vaultQuery = it; onFilter() }, placeholder = container.t("app.search.placeholder"))
        MooButton(
            container.t("quickNote.searchContent") + ": ${session.includeContent}",
            onClick = { session.includeContent = !session.includeContent; onFilter() }
        )
        VaultSortMenu(
            current = session.vaultSort,
            options = listOf(
                "name" to container.t("json.vault.sortName"),
                "modified" to container.t("json.vault.sortModified")
            ),
            onChange = { session.vaultSort = it; onFilter() }
        )
        MooButton(
            if (settings.vault.jsonTreeExpandMode == "expandAll") container.t("json.vault.collapseAll") else container.t("json.vault.expandAll"),
            onClick = {
                val next = if (settings.vault.jsonTreeExpandMode == "expandAll") "collapseAll" else "expandAll"
                container.updateSettings { it.copy(vault = it.vault.copy(jsonTreeExpandMode = next)) }
            }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MooButton(container.t("json.vault.new"), onClick = {
                val name = "snippet-${System.currentTimeMillis()}.json"
                container.jsonVault.createFile(name, session.editor.text.ifBlank { "{\n}\n" })
                session.currentFile = name
                session.savedText = session.editor.text.ifBlank { "{\n}\n" }
                monitor?.noteOwnWrite(name, VaultConflictEngine.sha256Text(session.savedText))
                container.recordVaultActivity("Update JSON snippet", json = true)
                onChanged()
            })
            MooButton(container.t("json.vault.folder"), onClick = {
                session.dialogInputMode = "json-folder"
                session.dialogInput = "folder"
                onChanged()
            })
            MooButton(container.t("json.vault.save"), onClick = {
                saveJsonVault(container, session, monitor, onConflict)
                    .onSuccess { session.notice = container.t("common.save") }
                    .onFailure { session.notice = it.message ?: container.t("json.notice.failed") }
                onChanged()
            })
        }
        if (items.isEmpty()) {
            Text(container.t("json.vault.empty"), color = colors.textSecondary, fontSize = 12.sp)
        } else {
            VaultTreeList(
                items = items,
                selectedPath = session.currentFile,
                emptyLabel = container.t("json.vault.empty"),
                onOpen = { item ->
                    if (item.directory) return@VaultTreeList
                    if (session.currentFile.isNotBlank() && session.editor.text != session.savedText) {
                        val saved = saveJsonVault(container, session, monitor, onConflict)
                        if (saved.isFailure) {
                            onChanged()
                            return@VaultTreeList
                        }
                    }
                    onEdt { session.editor.setText(container.jsonVault.read(item.relativePath), recordUndo = false) }
                    session.currentFile = item.relativePath
                    session.savedText = session.editor.text
                    onChanged()
                },
                onMove = { from, to ->
                    runCatching { container.jsonVault.move(from, to) }
                        .onSuccess { next ->
                            session.currentFile = VaultMove.retargetAfterMove(session.currentFile, from, next)
                            session.notice = container.t("vault.moved")
                        }
                        .onFailure { session.notice = it.message ?: container.t("json.notice.failed") }
                    onChanged()
                },
                modifier = Modifier.weight(1f),
                expandMode = settings.vault.jsonTreeExpandMode,
                sort = session.vaultSort,
                contextActions = listOf(
                    VaultContextAction(VaultContextId.Rename, container.t("quickNote.rename")),
                    VaultContextAction(VaultContextId.Move, container.t("quickNote.move")),
                    VaultContextAction(VaultContextId.Duplicate, container.t("quickNote.duplicate"), filesOnly = true),
                    VaultContextAction(VaultContextId.Export, container.t("json.action.export"), filesOnly = true),
                    VaultContextAction(VaultContextId.Delete, container.t("json.vault.delete")),
                    VaultContextAction(VaultContextId.Reveal, container.t("vault.reveal")),
                    VaultContextAction(VaultContextId.Git, container.t("git.action"))
                ),
                onContextAction = { entry, id ->
                    handleJsonVaultContext(container, session, onGit, entry, id)
                    onChanged()
                }
            )
        }
        if (session.currentFile.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MooButton(container.t("quickNote.rename"), onClick = {
                    session.dialogInputMode = "json-rename"
                    session.dialogTarget = session.currentFile
                    session.dialogInput = session.currentFile.substringAfterLast('/')
                    onChanged()
                })
                MooButton(container.t("quickNote.duplicate"), onClick = {
                    runCatching { container.jsonVault.duplicate(session.currentFile) }
                        .onSuccess { copy ->
                            onEdt { session.editor.setText(container.jsonVault.read(copy), recordUndo = false) }
                            session.currentFile = copy
                            session.savedText = session.editor.text
                            session.notice = container.t("quickNote.duplicated")
                        }
                        .onFailure { session.notice = it.message ?: container.t("json.notice.failed") }
                    onChanged()
                })
                MooButton(container.t("json.vault.delete"), onClick = {
                    container.jsonVault.delete(session.currentFile)
                    session.currentFile = ""
                    session.savedText = session.editor.text
                    onChanged()
                })
            }
        }
        if (session.dialogInputMode.startsWith("json-")) {
            Dialog(onDismissRequest = { session.dialogInputMode = ""; session.dialogTarget = ""; onChanged() }) {
                Column(
                    Modifier.width(420.dp).background(colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        when (session.dialogInputMode) {
                            "json-folder" -> container.t("quickNote.dialog.createFolder")
                            "json-move" -> container.t("quickNote.dialog.move")
                            else -> container.t("quickNote.dialog.rename")
                        },
                        color = colors.textPrimary
                    )
                    MooTextField(session.dialogInput, { session.dialogInput = it; onChanged() })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MooButton(container.t("common.save"), primary = true, onClick = {
                            val name = session.dialogInput.trim()
                            val target = session.dialogTarget.ifBlank { session.currentFile }
                            if (name.isNotEmpty() || session.dialogInputMode == "json-move") {
                                runCatching {
                                    when (session.dialogInputMode) {
                                        "json-folder" -> container.jsonVault.createDirectory(name)
                                        "json-move" -> {
                                            val next = container.jsonVault.move(target, name)
                                            session.currentFile = VaultMove.retargetAfterMove(session.currentFile, target, next)
                                        }
                                        else -> {
                                            val next = container.jsonVault.rename(target, name)
                                            session.currentFile = VaultMove.retargetAfterMove(session.currentFile, target, next)
                                        }
                                    }
                                }.onSuccess {
                                    session.dialogInputMode = ""
                                    session.dialogTarget = ""
                                    session.notice = container.t("quickNote.saved")
                                }.onFailure { session.notice = it.message ?: container.t("json.notice.failed") }
                                onChanged()
                            }
                        })
                        MooButton(container.t("common.close"), onClick = { session.dialogInputMode = ""; session.dialogTarget = ""; onChanged() })
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectorPane(
    container: AppContainer,
    session: JsonSession,
    translator: JsonTranslator,
    status: JsonStatus,
    onChanged: () -> Unit,
    width: Float
) {
    val colors = MooTheme.colors
    Column(
        Modifier.width(width.dp).fillMaxHeight().background(colors.surfaceSubtle).padding(10.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(container.t("json.panel.inspector"), color = colors.textPrimary, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            MooButton(container.t("common.close"), onClick = {
                session.inspectorOpen = false
                if (session.compactAux == "inspector") session.compactAux = ""
                onChanged()
            })
        }
        Text(container.t("json.panel.format"), color = colors.textPrimary, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(container.t("json.format.indent"), color = colors.textSecondary, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            MooSegmented(
                options = listOf("2" to "2", "4" to "4"),
                value = session.formatOptions.spaces.toString(),
                onChange = {
                    session.formatOptions = session.formatOptions.copy(spaces = it.toInt())
                    onChanged()
                }
            )
        }
        InspectorCheck(container.t("json.format.sortKeys"), session.formatOptions.sortKeys) { checked ->
            session.formatOptions = session.formatOptions.copy(sortKeys = checked)
            onChanged()
        }
        InspectorCheck(container.t("json.format.ignoreCase"), session.formatOptions.ignoreCase) { checked ->
            session.formatOptions = session.formatOptions.copy(ignoreCase = checked)
            onChanged()
        }
        InspectorCheck(container.t("json.format.duplicateKeys"), session.formatOptions.checkDuplicateKeys) { checked ->
            session.formatOptions = session.formatOptions.copy(checkDuplicateKeys = checked)
            onChanged()
        }
        MooButton(container.t("json.format.apply"), primary = true, onClick = {
            transform(container, session, translator, container.t("json.notice.formatted")) {
                JsonEngine.formatAdvanced(it, translator, session.formatOptions)
            }
            onChanged()
        }, modifier = Modifier.fillMaxWidth())
        Text(container.t("json.panel.convert"), color = colors.textPrimary, fontSize = 12.sp)
        InspectorActionGrid(
            listOf(
                container.t("json.action.jsonToXml") to {
                    showResult(container, session, translator, container.t("json.action.jsonToXml")) { JsonEngine.jsonToXml(it, translator) }
                    onChanged()
                },
                container.t("json.action.xmlToJson") to {
                    session.dialogInputMode = "xml"
                    onChanged()
                },
                container.t("json.action.beanToJson") to {
                    session.dialogInputMode = "bean"
                    onChanged()
                },
                container.t("json.action.jsonToBean") to {
                    showResult(container, session, translator, container.t("json.action.jsonToBean")) {
                        JsonEngine.jsonToJavaBean(it, translator, session.className.ifBlank { "Root" })
                    }
                    onChanged()
                },
                container.t("json.action.swap") to {
                    transform(container, session, translator, container.t("json.action.swap")) { JsonEngine.swapKeysAndValues(it, translator) }
                    onChanged()
                },
                container.t("json.action.escape") to {
                    transform(container, session, translator, container.t("json.notice.escaped")) { JsonEngine.escapeJsonString(it) }
                    onChanged()
                },
                container.t("json.action.unescape") to {
                    transform(container, session, translator, container.t("json.notice.unescaped")) { JsonEngine.unescapeJsonString(it, translator) }
                    onChanged()
                },
                container.t("json.action.escapeText") to {
                    transform(container, session, translator, container.t("json.notice.escaped")) { JsonEngine.escapeJavaString(it) }
                    onChanged()
                },
                container.t("json.action.unescapeText") to {
                    transform(container, session, translator, container.t("json.notice.unescaped")) { JsonEngine.unescapeJsonText(it) }
                    onChanged()
                }
            )
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(container.t("json.dialog.className"), color = colors.textSecondary, fontSize = 11.sp)
            MooTextField(session.className, { session.className = it; onChanged() }, placeholder = "Root", modifier = Modifier.weight(1f))
        }
        Text(container.t("json.panel.jsonPath"), color = colors.textPrimary, fontSize = 12.sp)
        MooTextField(session.jsonPath, { session.jsonPath = it; onChanged() }, placeholder = container.t("json.path.placeholder"), modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            MooButton(container.t("json.path.query"), primary = true, onClick = {
                showResult(container, session, translator, container.t("json.notice.pathApplied")) {
                    JsonEngine.queryPath(it, session.jsonPath, translator)
                }
                onChanged()
            }, modifier = Modifier.weight(0.8f))
            MooButton(container.t("json.path.pick"), onClick = {
                session.pathPickerOpen = true
                onChanged()
            }, modifier = Modifier.weight(1.2f))
        }
        val paths = runCatching { JsonEngine.listPaths(session.editor.text, translator) }.getOrDefault(emptyList())
        paths.take(80).forEach { entry ->
            Column(
                Modifier.fillMaxWidth().pointerInput(entry.path) {
                    detectTapGestures(
                        onTap = {
                            session.jsonPath = entry.path
                            session.pathResult = entry.preview
                            onChanged()
                        },
                        onDoubleTap = {
                            session.jsonPath = entry.path
                            session.pathResult = entry.preview
                            showResult(container, session, translator, container.t("json.notice.pathApplied")) {
                                JsonEngine.queryPath(it, entry.path, translator)
                            }
                            onChanged()
                        }
                    )
                }.padding(vertical = 2.dp)
            ) {
                Text(
                    "${"  ".repeat(entry.depth)}${entry.label}",
                    color = if (session.jsonPath == entry.path) colors.accent else colors.textSecondary,
                    fontSize = 11.sp
                )
                if (session.jsonPath == entry.path) {
                    Text(entry.preview, color = colors.textPrimary, fontSize = 10.sp)
                }
            }
        }
        if (session.pathResult.isNotBlank()) {
            Text(session.pathResult, color = colors.textPrimary, fontSize = 11.sp)
        }
        Text(container.t("json.panel.result"), color = colors.textPrimary, fontSize = 12.sp)
        Text(
            session.notice.ifBlank { status.message },
            color = if (status.kind == JsonStatus.Kind.Error) colors.danger else colors.textPrimary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun InspectorCheck(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MooTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MooSwitch(checked = checked, onCheckedChange = onChange)
        Text(label, color = colors.textPrimary, fontSize = 12.sp, modifier = Modifier.clickable { onChange(!checked) }.weight(1f))
    }
}

@Composable
private fun InspectorActionGrid(actions: List<Pair<String, () -> Unit>>) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        actions.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                pair.forEach { (label, onClick) ->
                    MooButton(label, onClick = onClick, modifier = Modifier.weight(1f))
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ResultDialog(container: AppContainer, session: JsonSession, onChanged: () -> Unit) {
    Dialog(onDismissRequest = { session.dialogTitle = ""; session.dialogBody = ""; onChanged() }) {
        Column(
            Modifier.width(560.dp).height(420.dp).background(MooTheme.colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(session.dialogTitle, color = MooTheme.colors.textPrimary)
            Box(Modifier.weight(1f).fillMaxWidth().border(1.dp, MooTheme.colors.border, RoundedCornerShape(8.dp)).padding(8.dp).verticalScroll(rememberScrollState())) {
                Text(session.dialogBody, color = MooTheme.colors.textPrimary, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("json.action.copy"), onClick = { copyText(session.dialogBody, container) })
                MooButton(container.t("common.restore"), onClick = {
                    onEdt { session.editor.setText(session.dialogBody, recordUndo = true) }
                    session.dialogTitle = ""
                    session.dialogBody = ""
                    onChanged()
                })
                MooButton(container.t("common.close"), onClick = { session.dialogTitle = ""; session.dialogBody = ""; onChanged() })
            }
        }
    }
}

@Composable
private fun InputDialog(container: AppContainer, session: JsonSession, translator: JsonTranslator, onChanged: () -> Unit) {
    Dialog(onDismissRequest = { session.dialogInputMode = ""; onChanged() }) {
        Column(
            Modifier.width(520.dp).height(360.dp).background(MooTheme.colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(if (session.dialogInputMode == "xml") container.t("json.action.xmlToJson") else container.t("json.action.beanToJson"), color = MooTheme.colors.textPrimary)
            MooTextField(session.dialogInput, { session.dialogInput = it }, modifier = Modifier.fillMaxWidth().height(220.dp), singleLine = false)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("json.path.query"), primary = true, onClick = {
                    runCatching {
                        if (session.dialogInputMode == "xml") JsonEngine.xmlToJson(session.dialogInput, translator)
                        else JsonEngine.javaBeanToJson(session.dialogInput, translator)
                    }.onSuccess { output ->
                        onEdt { session.editor.setText(output, recordUndo = true) }
                        session.dialogInputMode = ""
                        session.dialogInput = ""
                    }.onFailure { session.notice = it.message ?: container.t("json.notice.failed") }
                    onChanged()
                })
                MooButton(container.t("common.close"), onClick = { session.dialogInputMode = ""; onChanged() })
            }
        }
    }
}

@Composable
private fun PathPickerDialog(container: AppContainer, session: JsonSession, onChanged: () -> Unit) {
    val translator = JsonTranslator { key, params -> container.t(key, params) }
    val entries = runCatching { JsonEngine.listPaths(session.editor.text, translator) }.getOrDefault(emptyList())
    var selected by remember { mutableStateOf(session.jsonPath) }
    val current = entries.find { it.path == selected }
    Dialog(onDismissRequest = { session.pathPickerOpen = false; onChanged() }) {
        Column(
            Modifier.width(640.dp).height(440.dp).background(MooTheme.colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("json.pathPicker.title"), color = MooTheme.colors.textPrimary)
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyColumn(Modifier.weight(1f)) {
                    items(entries) { entry ->
                        Text(
                            "${"  ".repeat(entry.depth)}${entry.label}",
                            color = if (entry.path == selected) MooTheme.colors.accent else MooTheme.colors.textPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth().pointerInput(entry.path) {
                                detectTapGestures(
                                    onTap = { selected = entry.path },
                                    onDoubleTap = {
                                        session.jsonPath = entry.path
                                        session.pathResult = entry.preview
                                        session.pathPickerOpen = false
                                        onChanged()
                                    }
                                )
                            }.padding(6.dp)
                        )
                    }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Text(container.t("json.pathPicker.path"), color = MooTheme.colors.textSecondary, fontSize = 12.sp)
                    Text(current?.path ?: selected, color = MooTheme.colors.textPrimary, fontSize = 12.sp)
                    Text(container.t("json.pathPicker.preview"), color = MooTheme.colors.textSecondary, fontSize = 12.sp)
                    Text(current?.preview.orEmpty(), color = MooTheme.colors.textPrimary, fontSize = 12.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("json.pathPicker.use"), primary = true, onClick = {
                    session.jsonPath = selected
                    session.pathResult = current?.preview.orEmpty()
                    session.pathPickerOpen = false
                    onChanged()
                })
                MooButton(container.t("common.close"), onClick = { session.pathPickerOpen = false; onChanged() })
            }
        }
    }
}

private fun transform(container: AppContainer, session: JsonSession, translator: JsonTranslator, summary: String, block: (String) -> String) {
    val input = session.editor.text
    runCatching { block(input) }
        .onSuccess { output ->
            onEdt { session.editor.setText(output, recordUndo = true) }
            session.notice = summary
            container.history.save(ToolId.Json.id, summary, summary, input, output)
        }
        .onFailure { error ->
            session.notice = error.message ?: container.t("json.notice.failed")
        }
}

private fun handleJsonVaultContext(
    container: AppContainer,
    session: JsonSession,
    onGit: () -> Unit,
    entry: VaultEntry,
    id: VaultContextId
) {
    when (id) {
        VaultContextId.Rename -> {
            session.dialogInputMode = "json-rename"
            session.dialogTarget = entry.relativePath
            session.dialogInput = entry.name
        }
        VaultContextId.Move -> {
            session.dialogInputMode = "json-move"
            session.dialogTarget = entry.relativePath
            session.dialogInput = ""
        }
        VaultContextId.Duplicate -> {
            runCatching { container.jsonVault.duplicate(entry.relativePath) }
                .onSuccess { copy ->
                    onEdt { session.editor.setText(container.jsonVault.read(copy), recordUndo = false) }
                    session.currentFile = copy
                    session.savedText = session.editor.text
                    session.notice = container.t("quickNote.duplicated")
                }
                .onFailure { session.notice = it.message ?: container.t("json.notice.failed") }
        }
        VaultContextId.Export -> {
            chooseFile(true)?.let { file ->
                runCatching { container.jsonVault.exportFile(entry.relativePath, file.toPath()) }
                    .onSuccess { session.notice = container.t("json.notice.exported") }
                    .onFailure { session.notice = it.message ?: container.t("json.notice.failed") }
            }
        }
        VaultContextId.Delete -> {
            container.jsonVault.delete(entry.relativePath)
            if (session.currentFile == entry.relativePath || session.currentFile.startsWith("${entry.relativePath}/")) {
                session.currentFile = ""
                session.savedText = session.editor.text
            }
        }
        VaultContextId.Reveal -> container.revealInFileManager(container.jsonVault.resolve(entry.relativePath))
        VaultContextId.Git -> onGit()
    }
}

private fun showResult(container: AppContainer, session: JsonSession, translator: JsonTranslator, title: String, block: (String) -> String) {
    val input = session.editor.text
    runCatching { block(input) }
        .onSuccess { output ->
            session.dialogTitle = title
            session.dialogBody = output
            session.notice = title
            container.history.save(ToolId.Json.id, title, title, input, output)
        }
        .onFailure { error ->
            session.notice = error.message ?: container.t("json.notice.failed")
        }
}

private fun jump(session: JsonSession, forward: Boolean) {
    val match = FindReplace.findNext(session.editor.text, session.findQuery, session.findOptions, session.editor.area.caretPosition, forward)
    if (match != null) onEdt { session.editor.select(match.start, match.end) }
}

private data class CopyOutcome(val notice: String, val success: Boolean)

private fun copyText(value: String, container: AppContainer): CopyOutcome {
    return try {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
        CopyOutcome(container.t("json.notice.copied"), true)
    } catch (_: Exception) {
        CopyOutcome(container.t("json.notice.copyFailed"), false)
    }
}

private fun chooseFile(save: Boolean): File? {
    val dialog = FileDialog(null as Frame?, if (save) "Export JSON" else "Import JSON", if (save) FileDialog.SAVE else FileDialog.LOAD)
    dialog.isVisible = true
    val file = dialog.file ?: return null
    val directory = dialog.directory ?: return null
    return File(directory, file)
}

private fun saveJsonVault(
    container: AppContainer,
    session: JsonSession,
    monitor: VaultRevisionMonitor?,
    onConflict: (VaultConflictState) -> Unit
): Result<Unit> {
    val name = session.currentFile.ifBlank { "draft.json" }
    if (session.currentFile.isNotBlank()) {
        val disk = runCatching { container.jsonVault.readOrNull(name) }.getOrNull()
        if (!VaultConflictEngine.canOverwrite(session.savedText, disk, session.editor.text)) {
            onConflict(VaultConflictState(name, session.editor.text, disk, disk == null))
            return Result.failure(IllegalStateException(container.t("vault.conflict.blocked")))
        }
    }
    return runCatching {
        container.jsonVault.write(name, session.editor.text)
        monitor?.noteOwnWrite(name, VaultConflictEngine.sha256Text(session.editor.text))
        session.currentFile = name
        session.savedText = session.editor.text
        container.recordVaultActivity("Update JSON snippet", json = true)
    }
}

private fun handleJsonVaultChange(
    container: AppContainer,
    session: JsonSession,
    paths: List<String>,
    onConflict: (VaultConflictState) -> Unit,
    refresh: () -> Unit
) {
    refresh()
    val current = session.currentFile
    if (current.isBlank() || current !in paths) return
    val disk = runCatching { container.jsonVault.readOrNull(current) }.getOrNull()
    when (VaultConflictEngine.decide(current, current, session.editor.text, session.savedText, disk)) {
        VaultChangeKind.Reload -> {
            onEdt { session.editor.setText(disk.orEmpty(), recordUndo = false) }
            session.savedText = disk.orEmpty()
            session.notice = container.t("vault.conflict.reloaded")
            refresh()
        }
        VaultChangeKind.Deleted -> {
            session.currentFile = ""
            session.savedText = session.editor.text
            session.notice = container.t("vault.conflict.deleted")
            refresh()
        }
        VaultChangeKind.Conflict -> onConflict(VaultConflictState(current, session.editor.text, disk, disk == null))
        VaultChangeKind.Ignored, VaultChangeKind.TreeChanged -> Unit
    }
}

private fun onEdt(block: () -> Unit) {
    if (SwingUtilities.isEventDispatchThread()) block() else SwingUtilities.invokeLater(block)
}
