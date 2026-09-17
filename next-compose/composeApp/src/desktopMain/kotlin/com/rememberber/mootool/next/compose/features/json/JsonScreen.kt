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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.EditorColumnEditPresentation
import com.rememberber.mootool.next.compose.domain.JsonVaultFooterPresentation
import com.rememberber.mootool.next.compose.domain.EditorSettingsLiveApply
import com.rememberber.mootool.next.compose.domain.FindReplace
import com.rememberber.mootool.next.compose.domain.JsonEngine
import com.rememberber.mootool.next.compose.domain.JsonWiringPresentation
import com.rememberber.mootool.next.compose.domain.JsonStatus
import com.rememberber.mootool.next.compose.domain.JsonTranslator
import com.rememberber.mootool.next.compose.domain.VaultSelectionPath
import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.domain.JsonHistoryMetadata
import com.rememberber.mootool.next.compose.domain.JsonHistoryRestore
import com.rememberber.mootool.next.compose.domain.VaultGitCheckpointMessages
import com.rememberber.mootool.next.compose.domain.VaultRevisionMonitor
import com.rememberber.mootool.next.compose.domain.noteOwnWriteJsonVaultFile
import com.rememberber.mootool.next.compose.domain.rebaselineAfterLocalCrud
import com.rememberber.mootool.next.compose.domain.VaultSearchIndex
import com.rememberber.mootool.next.compose.features.git.GitActionButton
import com.rememberber.mootool.next.compose.features.git.VaultGitDialog
import com.rememberber.mootool.next.compose.features.git.gitActionMenuLabel
import com.rememberber.mootool.next.compose.features.git.rememberVaultGitChangeCount
import com.rememberber.mootool.next.compose.features.vault.JsonVaultConflictOverlay
import com.rememberber.mootool.next.compose.features.vault.jsonVaultRenameDefault
import com.rememberber.mootool.next.compose.features.vault.jsonVaultRenameDefaultFromFileName
import com.rememberber.mootool.next.compose.features.vault.vaultMoveFolderOptions
import com.rememberber.mootool.next.compose.features.vault.vaultPathsAfterDelete
import com.rememberber.mootool.next.compose.editor.EditorAppShortcuts
import com.rememberber.mootool.next.compose.editor.EditorFindShortcutPolicy
import com.rememberber.mootool.next.compose.editor.onFindBarRowKeys
import com.rememberber.mootool.next.compose.editor.onFindQueryEnterKey
import com.rememberber.mootool.next.compose.editor.openFindBarSeedingSelection
import com.rememberber.mootool.next.compose.editor.EditorFindHighlight
import com.rememberber.mootool.next.compose.editor.RstaFindNavigation
import com.rememberber.mootool.next.compose.editor.EditorHost
import com.rememberber.mootool.next.compose.editor.EditorLimits
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.JsonSession
import com.rememberber.mootool.next.compose.sessions.dismissModalOverlays
import com.rememberber.mootool.next.compose.storage.VaultEntry
import com.rememberber.mootool.next.compose.ui.components.desktopFileDropTarget
import com.rememberber.mootool.next.compose.ui.components.FontSelect
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooIconButton
import com.rememberber.mootool.next.compose.ui.components.MooCompactSearch
import com.rememberber.mootool.next.compose.ui.components.MooMenu
import com.rememberber.mootool.next.compose.ui.components.MooMenuItem
import com.rememberber.mootool.next.compose.ui.components.MooCard
import com.rememberber.mootool.next.compose.ui.components.mooEditorFrame
import com.rememberber.mootool.next.compose.ui.components.mooFocusClickable
import com.rememberber.mootool.next.compose.ui.components.mooJsonVaultFooter
import com.rememberber.mootool.next.compose.ui.components.mooJsonVaultFooterActions
import com.rememberber.mootool.next.compose.ui.components.mooJsonInspectorPathActions
import com.rememberber.mootool.next.compose.ui.components.mooJsonInspectorSectionResult
import com.rememberber.mootool.next.compose.ui.components.mooJsonVaultSearch
import com.rememberber.mootool.next.compose.ui.components.mooToolShell
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.mooStatusBarBackground
import com.rememberber.mootool.next.compose.ui.components.mooFindBarBackground
import com.rememberber.mootool.next.compose.ui.components.MooSegmented
import com.rememberber.mootool.next.compose.ui.components.MooStatusMeta
import com.rememberber.mootool.next.compose.ui.components.MooSwitch
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.VaultContextAction
import com.rememberber.mootool.next.compose.ui.components.VaultContextId
import com.rememberber.mootool.next.compose.ui.components.VaultSortMenu
import com.rememberber.mootool.next.compose.ui.components.VaultDeleteConfirmOverlay
import com.rememberber.mootool.next.compose.ui.components.VaultSelectionFooter
import com.rememberber.mootool.next.compose.ui.components.OnVaultEffectiveRootChanged
import com.rememberber.mootool.next.compose.ui.components.VaultTreeList
import com.rememberber.mootool.next.compose.ui.components.resetVaultTreeOnCustomRootChange
import com.rememberber.mootool.next.compose.domain.VaultMove
import com.rememberber.mootool.next.compose.features.vault.RebBaselineVaultMonitorOnSessionReload
import com.rememberber.mootool.next.compose.features.vault.dismissVaultScopedOverlays
import com.rememberber.mootool.next.compose.ui.components.VerticalPaneHandle
import com.rememberber.mootool.next.compose.ui.components.setPaneSize
import com.rememberber.mootool.next.compose.ui.components.MooOverlay
import com.rememberber.mootool.next.compose.ui.components.mooDialogSurface
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.CopyFeedbackPolicy
import com.rememberber.mootool.next.compose.ui.workbench.DismissModalOverlaysOnDispose
import com.rememberber.mootool.next.compose.ui.workbench.JsonEditorWindowFocus
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import com.rememberber.mootool.next.compose.ui.workbench.blockedByIme
import com.rememberber.mootool.next.compose.ui.chooseFileWithExportDirectory
import com.rememberber.mootool.next.compose.ui.persistToolsExportDirectory
import java.io.File
import javax.swing.SwingUtilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

@Composable
fun JsonScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.jsonSession(container.settings.value.editor.softWrap) }
    val settings by container.settings.collectAsState()
    val revision by container.sessionManager.revision.collectAsState()
    val sessionGeneration by container.sessionManager.sessionGeneration.collectAsState()
    var tick by remember { mutableStateOf(0L) }
    var filterRev by remember { mutableStateOf(0) }
    var gitCountRev by remember { mutableIntStateOf(0) }
    var snapshot by remember { mutableStateOf(container.jsonVault.snapshot(container.settings.value.vault.hideGitignoredFiles)) }
    fun refresh() {
        tick += 1
        container.sessionManager.bump()
        container.sessionManager.persistJson()
    }
    fun persistFilter() {
        jsonVaultReloadOpenFileIfClean(container, session)
        filterRev += 1
        gitCountRev++
        container.sessionManager.bump()
        container.sessionManager.persistJson()
    }
    val jsonVaultAutoPullTick by container.jsonVaultAutoPullTick.collectAsState()
    LaunchedEffect(settings.editor.softWrap) {
        val next = EditorSettingsLiveApply.jsonSoftWrapFromSettings(session.wrap, settings.editor.softWrap)
        if (next != session.wrap) {
            session.wrap = next
            refresh()
        }
    }
    LaunchedEffect(jsonVaultAutoPullTick, settings.vault.hideGitignoredFiles) {
        if (jsonVaultAutoPullTick == 0L) return@LaunchedEffect
        snapshot = withContext(Dispatchers.IO) {
            container.jsonVault.snapshot(settings.vault.hideGitignoredFiles)
        }
        persistFilter()
    }
    val jsonVaultRootKey = remember(settings.vault.jsonPath, settings.data.directory) {
        container.jsonVault.root().toAbsolutePath().normalize().toString()
    }
    OnVaultEffectiveRootChanged(jsonVaultRootKey) {
        session.dismissVaultScopedOverlays()
        session.resetVaultTreeOnCustomRootChange()
        filterRev += 1
        gitCountRev++
        when (jsonVaultReloadOpenFileIfClean(container, session)) {
            JsonVaultReloadOpenResult.ClearedMissing -> session.notice = container.t("vault.conflict.deleted")
            JsonVaultReloadOpenResult.Reloaded -> session.notice = container.t("vault.conflict.reloaded")
            JsonVaultReloadOpenResult.Unchanged -> Unit
        }
        container.sessionManager.bump()
    }
    val translator = remember(settings.general.language) {
        JsonTranslator { key, params -> container.t(key, params) }
    }
    val status = remember(session.editor.revision, tick, settings.general.language) {
        JsonWiringPresentation.runValidate(session.editor.text, translator)
    }
    val gitChangeCount = rememberVaultGitChangeCount(container.jsonVault.root(), tick + gitCountRev)
    var monitor by remember { mutableStateOf<VaultRevisionMonitor?>(null) }
    DisposableEffect(session.editor) {
        session.editor.onUserDocumentChange = {
            session.pathResult = ""
            if (!session.columnLatch) session.notice = ""
            session.copyState = CopyFeedbackPolicy.IDLE
            container.sessionManager.bump()
        }
        onDispose { session.editor.onUserDocumentChange = null }
    }
    DismissModalOverlaysOnDispose(container, ToolId.Json) { session.dismissModalOverlays() }
    EditorFindHighlight.ClearOnDispose(session.editor)
    val activeTool by container.activeTool.collectAsState()
    val jsonToolActive = detached || activeTool == ToolId.Json
    JsonEditorWindowFocus(session, jsonToolActive)
    LaunchedEffect(session.editor.revision, session.currentFile, session.vaultConflict) {
        if (session.vaultConflict != null) return@LaunchedEffect
        val file = session.currentFile
        if (file.isBlank()) return@LaunchedEffect
        if (session.editor.text == session.savedText) return@LaunchedEffect
        delay(250)
        if (file != session.currentFile) return@LaunchedEffect
        if (session.editor.text == session.savedText) return@LaunchedEffect
        jsonVaultIdleAutosaveAttempt(container, session, monitor) { session.vaultConflict = it }
        refresh()
    }
    val colors = MooTheme.colors
    DisposableEffect(jsonVaultRootKey) {
        val next = VaultRevisionMonitor(container.jsonVault.root(), ignoreAttachments = false) { paths ->
            SwingUtilities.invokeLater {
                container.notifyJsonVaultTreeChanged()
                handleJsonVaultChange(container, session, paths, { session.vaultConflict = it }) {
                    gitCountRev++
                    refresh()
                }
            }
        }
        next.start()
        monitor = next
        onDispose {
            next.close()
            if (monitor === next) monitor = null
        }
    }
    RebBaselineVaultMonitorOnSessionReload(sessionGeneration, monitor)
    LaunchedEffect(tick, settings.vault.jsonPath, settings.data.directory, settings.vault.hideGitignoredFiles, sessionGeneration) {
        snapshot = withContext(Dispatchers.IO) { container.jsonVault.snapshot(settings.vault.hideGitignoredFiles) }
    }
    LaunchedEffect(activeTool, detached, settings.vault.jsonPath, settings.data.directory, settings.vault.hideGitignoredFiles, sessionGeneration) {
        if (!detached && activeTool != ToolId.Json) return@LaunchedEffect
        snapshot = withContext(Dispatchers.IO) { container.jsonVault.snapshot(settings.vault.hideGitignoredFiles) }
        filterRev += 1
        val current = session.currentFile
        if (current.isBlank()) return@LaunchedEffect
        if (session.editor.text != session.savedText) return@LaunchedEffect
        withContext(Dispatchers.Swing) {
            when (jsonVaultReloadOpenFileIfClean(container, session)) {
                JsonVaultReloadOpenResult.Reloaded -> session.notice = container.t("vault.conflict.reloaded")
                JsonVaultReloadOpenResult.ClearedMissing -> session.notice = container.t("vault.conflict.deleted")
                JsonVaultReloadOpenResult.Unchanged -> Unit
            }
        }
    }
    val vaultItems = remember(snapshot, session.vaultQuery, session.includeContent, filterRev) {
        VaultSearchIndex.filter(snapshot, session.vaultQuery, session.includeContent)
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.workspace).onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown || event.blockedByIme()) return@onPreviewKeyEvent false
        val meta = event.isMetaPressed || event.isCtrlPressed
        when {
            meta && event.isShiftPressed && event.key == Key.F -> {
                transform(
                    container,
                    session,
                    translator,
                    container.t("json.notice.formatted"),
                    historySummary = container.t("json.action.format"),
                ) { input ->
                    JsonWiringPresentation.runQuickFormat(
                        input,
                        translator,
                        JsonToolbarFormatPolicy.QUICK_FORMAT_SPACES,
                    )
                }
                refresh()
                true
            }
            EditorFindShortcutPolicy.opensShellFind(
                ToolId.Json,
                event.key,
                meta = meta,
                shift = event.isShiftPressed,
                alt = event.isAltPressed,
            ) -> {
                openFindBarSeedingSelection(session.editor) { selected ->
                    selected?.let { session.findQuery = it }
                    session.findOpen = true
                    session.findReplacedCount = 0
                    refresh()
                }
                true
            }
            meta && event.key == Key.S -> {
                jsonVaultSaveFromUserAction(container, session, vaultItems, monitor) { session.vaultConflict = it }
                refresh()
                true
            }
            event.key == Key.Escape && session.findOpen -> {
                session.findOpen = false
                session.findReplacedCount = 0
                refresh()
                true
            }
            else -> false
        }
    }) {
        val compact = LayoutPolicy.isCompact(maxWidth.value)
        val overflow = LayoutPolicy.overflowToolbar(maxWidth.value)
        val desktopInspectorOpen = LayoutPolicy.jsonInspectorDesktopOpen(maxWidth.value)
        var lastDesktopInspectorBand by remember { mutableStateOf<Boolean?>(null) }
        LaunchedEffect(compact, desktopInspectorOpen) {
            if (compact) {
                lastDesktopInspectorBand = null
                return@LaunchedEffect
            }
            val previous = lastDesktopInspectorBand
            lastDesktopInspectorBand = desktopInspectorOpen
            if (previous != null && previous != desktopInspectorOpen) {
                session.inspectorOpen = desktopInspectorOpen
                container.sessionManager.persistJson()
            }
        }
        Column(Modifier.fillMaxSize()) {
        JsonToolbar(
            container,
            session,
            translator,
            compact = compact,
            overflow = overflow,
            detached = detached,
            gitChangeCount = gitChangeCount,
            monitor = monitor,
            onConflict = { session.vaultConflict = it },
            onChanged = { refresh() },
            onGit = { session.gitDialogOpen = true; refresh() }
        )
        if (session.findOpen) {
            FindBar(container, session, onChanged = { refresh() })
        }
        LaunchedEffect(session.findOpen, session.findQuery, session.findOptions, tick, session.editor.revision) {
            onEdt {
                EditorFindHighlight.sync(session.editor, session.findOpen, session.findQuery, session.findOptions, colors)
            }
        }
        val vaultWidth = settings.layout.pane(ToolId.Json.id, 0, 240f, 200f, 320f)
        val inspectorWidth = settings.layout.pane(ToolId.Json.id, 1, 280f, 240f, 340f)
        val showVault = LayoutPolicy.showVault(compact, session.compactAux)
        val showInspector = LayoutPolicy.showInspector(compact, session.compactAux, session.inspectorOpen)
        Row(Modifier.weight(1f).fillMaxWidth()) {
            if (showVault) {
                VaultPane(
                    container,
                    session,
                    vaultItems,
                    monitor,
                    { session.vaultConflict = it },
                    onChanged = { refresh() },
                    onFilter = { persistFilter() },
                    onGit = { session.gitDialogOpen = true; refresh() },
                    onDeleteRequest = { session.vaultDeleteConfirmPath = it; refresh() },
                    gitChangeCount = gitChangeCount,
                    width = vaultWidth
                )
                VerticalPaneHandle(
                    onDelta = { container.setPaneSize(ToolId.Json.id, 0, vaultWidth + it, 2) },
                    onReset = { container.setPaneSize(ToolId.Json.id, 0, 240f, 2) }
                )
            }
            Box(Modifier.weight(1f).fillMaxHeight().mooEditorFrame(flatten = true)) {
                val jsonDropHandler: (List<File>) -> Boolean = { files ->
                    handleJsonEditorFileDrop(
                        container,
                        session,
                        monitor,
                        vaultItems,
                        files,
                        onConflict = { session.vaultConflict = it },
                    ) {
                        refresh()
                    }
                }
                EditorHost(
                    buffer = session.editor,
                    dark = MooTheme.dark,
                    fontName = com.rememberber.mootool.next.compose.domain.DocumentFormatEngine.editorFont(settings.editor.jsonFontName),
                    fontSize = EditorSettingsLiveApply.jsonEditorFontSize(settings.editor.jsonFontSize),
                    wrap = session.wrap,
                    columnEditing = true,
                    columnDragWithoutAlt = EditorColumnEditPresentation.columnDragWithoutAlt(session.columnLatch),
                    onFilesDropped = jsonDropHandler,
                    shortcuts = EditorAppShortcuts(
                        onFind = {
                            openFindBarSeedingSelection(session.editor) { selected ->
                                selected?.let { session.findQuery = it }
                                session.findOpen = true
                                session.findReplacedCount = 0
                                refresh()
                            }
                        },
                        onFormat = {
                            transform(
                                container,
                                session,
                                translator,
                                container.t("json.notice.formatted"),
                                historySummary = container.t("json.action.format"),
                            ) { input ->
                                JsonWiringPresentation.runQuickFormat(
                                    input,
                                    translator,
                                    JsonToolbarFormatPolicy.QUICK_FORMAT_SPACES,
                                )
                            }
                            refresh()
                        },
                        onSave = {
                            jsonVaultSaveFromUserAction(container, session, vaultItems, monitor) { session.vaultConflict = it }
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
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.statusBar).mooStatusBarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MooStatusMeta(status.message, color = when (status.kind) {
                com.rememberber.mootool.next.compose.domain.JsonStatus.Kind.Error -> colors.danger
                com.rememberber.mootool.next.compose.domain.JsonStatus.Kind.Valid -> colors.success
                else -> colors.textMuted
            })
            Spacer(Modifier.weight(1f))
            if (session.notice.isNotBlank()) MooStatusMeta(session.notice)
            if (EditorLimits.exceedsLargeDocument(session.editor.text)) MooStatusMeta(" · ${container.t("editor.largeDocument")}", color = colors.warning)
            if (session.vaultConflict != null) MooStatusMeta(" · ${container.t("vault.conflict.banner")}", color = colors.warning)
            if (detached) MooStatusMeta(" · detached")
        }
        }
    }

    if (session.gitDialogOpen) {
        VaultGitDialog(
            container = container,
            title = container.t("json.git.title"),
            defaultMessage = container.t("json.git.defaultMessage"),
            root = container.jsonVault.root(),
            onDismiss = { session.gitDialogOpen = false; gitCountRev++; refresh() },
            onFlush = {
                jsonGitFlushBeforeAction(container, session, monitor) { session.vaultConflict = it }
            },
            onVaultRefresh = {
                container.notifyJsonVaultTreeChanged()
                val current = session.currentFile
                if (current.isBlank()) refresh()
                else handleJsonVaultChange(container, session, listOf(current), { session.vaultConflict = it }, { refresh() })
            },
            onGitStatusChanged = {
                gitCountRev++
                refresh()
            },
        )
    }
    VaultDeleteConfirmOverlay(
        container = container,
        relativePath = session.vaultDeleteConfirmPath.ifBlank { null },
        messageKey = "json.vault.confirmDelete",
        onConfirm = {
            val path = session.vaultDeleteConfirmPath
            if (path.isBlank()) return@VaultDeleteConfirmOverlay
            runCatching {
                container.jsonVault.delete(path)
                val afterDelete = vaultPathsAfterDelete(path, session.currentFile, session.vaultSelectedPath)
                session.currentFile = afterDelete.currentFile
                session.vaultSelectedPath = afterDelete.vaultSelectedPath
                if (afterDelete.clearedOpenFile) {
                    session.savedText = ""
                }
                session.notice = container.t("json.vault.deleted")
                container.toastSuccess(container.t("json.vault.deleted"))
                container.recordVaultActivity(VaultGitCheckpointMessages.DELETE_JSON_ENTRY, json = true)
                monitor.rebaselineAfterLocalCrud()
            }.onFailure { session.notice = it.message ?: container.t("json.notice.failed") }
            session.vaultDeleteConfirmPath = ""
            refresh()
        },
        onDismiss = { session.vaultDeleteConfirmPath = ""; refresh() }
    )
    JsonVaultConflictOverlay(container, session, monitor, onRefresh = { refresh() })
    if (session.historyOpen) {
        HistoryBrowser(
            container = container,
            toolId = ToolId.Json.id,
            title = container.t("json.history.title"),
            onRestore = { item ->
                if (!jsonVaultFlushDirtyOrNotice(container, session, monitor) { session.vaultConflict = it }) {
                    refresh()
                    return@HistoryBrowser
                }
                JsonHistoryRestore.apply(session, item)
                JsonHistoryRestore.editorText(item)?.let { text ->
                    onEdt { session.editor.setText(text, recordUndo = true) }
                }
                session.historyOpen = false
                refresh()
            },
            onDismiss = { session.historyOpen = false; refresh() }
        )
    }
    if (session.dialogTitle.isNotEmpty()) {
        ResultDialog(
            container,
            session,
            monitor,
            onConflict = { session.vaultConflict = it },
        ) { refresh() }
    }
    if (session.conversionMode.isNotEmpty()) {
        InputDialog(
            container,
            session,
            translator,
            monitor,
            onConflict = { session.vaultConflict = it },
        ) { refresh() }
    }
    if (session.pathPickerOpen) {
        JsonPathPickerDialog(container, session) { refresh() }
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
    gitChangeCount: Int,
    monitor: VaultRevisionMonitor?,
    onConflict: (VaultConflictState) -> Unit,
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

    fun syncColumnNotice() {
        session.notice = EditorColumnEditPresentation.columnNoticeKey(session.columnLatch, session.wrap)
            ?.let(container::t).orEmpty()
    }

    fun toggleWrap() {
        session.wrap = !session.wrap
        if (session.columnLatch) syncColumnNotice()
        onChanged()
    }

    fun toggleColumn() {
        session.columnLatch = !session.columnLatch
        syncColumnNotice()
        onChanged()
    }

    fun importFile() {
        chooseFileWithExportDirectory(container, save = false, title = "Import JSON")?.let { file ->
            if (!jsonVaultFlushDirtyOrNotice(container, session, monitor, onConflict)) {
                onChanged()
                return@let
            }
            when (val outcome = JsonWiringPresentation.runReadImportFile(file)) {
                is JsonWiringPresentation.ImportOutcome.Success -> {
                    onEdt {
                        session.editor.setText(outcome.content, recordUndo = true)
                        session.clearJsonPathQueryResult()
                        session.notice = container.t("json.notice.imported")
                    }
                    persistToolsExportDirectory(container, file)
                    container.toastSuccess(container.t("json.notice.imported"))
                }
                is JsonWiringPresentation.ImportOutcome.Failure -> {
                    session.notice = container.t(
                        "reformat.error.read",
                        mapOf("message" to (outcome.error.message ?: file.path)),
                    )
                }
            }
            onChanged()
        }
    }

    fun exportFile() {
        val defaultName = session.currentFile.substringAfterLast('/').ifBlank { "export.json" }
        chooseFileWithExportDirectory(container, save = true, title = "Export JSON", defaultFileName = defaultName)?.let { file ->
            when (val outcome = JsonWiringPresentation.runWriteExportFile(file, session.editor.text)) {
                JsonWiringPresentation.WriteExportOutcome.Success -> {
                    persistToolsExportDirectory(container, file)
                    session.notice = container.t("json.notice.exported")
                    container.toastSuccess(container.t("json.notice.exported"))
                }
                is JsonWiringPresentation.WriteExportOutcome.Failure -> {
                    session.notice = container.t(
                        "reformat.error.write",
                        mapOf("message" to (outcome.error.message ?: file.path)),
                    )
                }
            }
            onChanged()
        }
    }

    fun openHistory() {
        session.historyOpen = true
        onChanged()
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 8.dp).horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MooButton(container.t("json.action.format"), prominent = true, onClick = {
            transform(
                container,
                session,
                translator,
                container.t("json.notice.formatted"),
                historySummary = container.t("json.action.format"),
            ) { input ->
                JsonWiringPresentation.runQuickFormat(
                    input,
                    translator,
                    JsonToolbarFormatPolicy.QUICK_FORMAT_SPACES,
                )
            }
            onChanged()
        })
        MooButton(container.t("json.action.compress"), onClick = {
            transform(
                container,
                session,
                translator,
                container.t("json.notice.compressed"),
                historySummary = container.t("json.action.compress"),
            ) { input -> JsonWiringPresentation.runCompress(input, translator) }
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
            if (session.findOpen) {
                session.findOpen = false
                onChanged()
            } else {
                openFindBarSeedingSelection(session.editor) { selected ->
                    selected?.let { session.findQuery = it }
                    session.findOpen = true
                    session.findReplacedCount = 0
                    onChanged()
                }
            }
        })
        if (!overflow) {
            MooButton(container.t("json.action.import"), onClick = { importFile() })
            MooButton(container.t("json.action.export"), onClick = { exportFile() })
            MooButton(container.t("json.action.history"), onClick = { openHistory() })
            GitActionButton(container.t("git.action"), gitChangeCount, onClick = onGit)
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
                MooMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                    MooMenuItem(onClick = { moreOpen = false; toggleWrap() }) {
                        Text(if (session.wrap) container.t("json.action.wrap") else container.t("json.action.nowrap"))
                    }
                    MooMenuItem(onClick = { moreOpen = false; toggleColumn() }) {
                        Text(container.t("quickNote.columnEdit"))
                    }
                    MooMenuItem(onClick = { moreOpen = false; importFile() }) {
                        Text(container.t("json.action.import"))
                    }
                    MooMenuItem(onClick = { moreOpen = false; exportFile() }) {
                        Text(container.t("json.action.export"))
                    }
                    MooMenuItem(onClick = { moreOpen = false; openHistory() }) {
                        Text(container.t("json.action.history"))
                    }
                    MooMenuItem(onClick = { moreOpen = false; onGit() }) {
                        Text(gitActionMenuLabel(container.t("git.action"), gitChangeCount))
                    }
                    MooMenuItem(onClick = { moreOpen = false; toggleInspector() }) {
                        Text(container.t("json.panel.inspector"))
                    }
                    if (!detached) {
                        MooMenuItem(onClick = { moreOpen = false; container.sessionManager.detach(ToolId.Json) }) {
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
            session.clearJsonPathQueryResult()
            onChanged()
        })
    }
}

@Composable
private fun FindBar(container: AppContainer, session: JsonSession, onChanged: () -> Unit) {
    val matches = FindReplace.findAll(session.editor.text, session.findQuery, session.findOptions)
    val findFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        findFocus.requestFocus()
    }
    fun closeFind() {
        session.findOpen = false
        session.findReplacedCount = 0
        onChanged()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .mooFindBarBackground(json = true)
            .onFindBarRowKeys(
                onPrevious = { jumpFind(container, session, false); onChanged() },
                onNext = { jumpFind(container, session, true); onChanged() },
                onClose = ::closeFind,
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MooTextField(
            session.findQuery,
            {
                session.findQuery = it
                session.findReplacedCount = 0
                onChanged()
            },
            modifier = Modifier.width(220.dp),
            placeholder = container.t("json.find.placeholder"),
            fieldModifier = Modifier
                .focusRequester(findFocus)
                .onFindQueryEnterKey { jumpFind(container, session, true); onChanged() },
        )
        MooButton(
            container.t("find.find"),
            enabled = session.findQuery.isNotBlank(),
            onClick = { jumpFind(container, session, true); onChanged() },
        )
        MooTextField(session.replaceText, { session.replaceText = it; onChanged() }, modifier = Modifier.width(180.dp), placeholder = container.t("json.find.replace"))
        MooButton(container.t("find.matchCase") + ": ${session.findOptions.matchCase}", onClick = {
            session.findOptions = session.findOptions.copy(matchCase = !session.findOptions.matchCase)
            session.findReplacedCount = 0
            onChanged()
        })
        MooButton(container.t("find.wholeWord") + ": ${session.findOptions.wholeWord}", onClick = {
            session.findOptions = session.findOptions.copy(wholeWord = !session.findOptions.wholeWord)
            session.findReplacedCount = 0
            onChanged()
        })
        MooButton(container.t("find.regex") + ": ${session.findOptions.regex}", onClick = {
            session.findOptions = session.findOptions.copy(regex = !session.findOptions.regex)
            session.findReplacedCount = 0
            onChanged()
        })
        Text(
            "${container.t("find.foundPrefix")} ${matches.size}",
            color = MooTheme.colors.textSecondary,
            fontSize = 12.sp,
        )
        MooButton(container.t("find.previous"), onClick = { jumpFind(container, session, false); onChanged() })
        MooButton(container.t("find.next"), onClick = { jumpFind(container, session, true); onChanged() })
        MooButton(container.t("find.replace"), onClick = {
            onEdt {
                if (!RstaFindNavigation.replaceAndSelectNext(session.editor, session.findQuery, session.replaceText, session.findOptions)) {
                    container.toastFindNoMatches()
                } else {
                    session.findReplacedCount += 1
                    session.clearJsonPathQueryResult()
                    session.notice = ""
                    session.copyState = CopyFeedbackPolicy.IDLE
                }
                onChanged()
            }
        })
        MooButton(container.t("find.replaceAll"), onClick = {
            val (next, count) = FindReplace.replaceAll(session.editor.text, session.findQuery, session.replaceText, session.findOptions)
            if (count == 0) {
                container.toastFindNoMatches()
            } else {
                onEdt { session.editor.setText(next, recordUndo = true) }
                session.clearJsonPathQueryResult()
                session.findReplacedCount = count
                session.notice = ""
                session.copyState = CopyFeedbackPolicy.IDLE
            }
            onChanged()
        })
        Text(
            "${container.t("find.replacedPrefix")} ${session.findReplacedCount}",
            color = MooTheme.colors.textSecondary,
            fontSize = 12.sp,
        )
        MooButton(container.t("common.close"), onClick = ::closeFind)
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
    onDeleteRequest: (String) -> Unit,
    gitChangeCount: Int,
    width: Float
) {
    val colors = MooTheme.colors
    val settings by container.settings.collectAsState()
    var vaultMoreOpen by remember { mutableStateOf(false) }
    Column(Modifier.width(width.dp).fillMaxHeight().mooToolShell(colors.sidebar, flatten = true).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(container.t("json.vault.title"), color = colors.textPrimary, fontSize = 12.sp)
        MooCompactSearch(
            session.vaultQuery,
            { session.vaultQuery = it; onFilter() },
            placeholder = container.t("app.search.placeholder"),
            modifier = Modifier.mooJsonVaultSearch(),
        )
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
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            MooButton(container.t("json.vault.openFolder"), onClick = { container.openDirectory(container.jsonVault.root()) })
            MooButton(container.t("json.vault.refresh"), onClick = {
                container.notifyJsonVaultTreeChanged()
                onChanged()
            })
            Box {
                MooButton(container.t("json.vault.more"), primary = vaultMoreOpen, onClick = { vaultMoreOpen = true })
                MooMenu(expanded = vaultMoreOpen, onDismissRequest = { vaultMoreOpen = false }) {
                    val selected = session.vaultSelectedPath.ifBlank { session.currentFile }
                    val selectedEntry = items.find { it.relativePath == selected }
                    val hasSelection = selected.isNotBlank()
                    val canDuplicateFile = selectedEntry?.directory == false
                    MooMenuItem(
                        enabled = hasSelection,
                        onClick = {
                            vaultMoreOpen = false
                            if (selected.isBlank()) return@MooMenuItem
                            val entry = selectedEntry
                                ?: VaultEntry(selected, selected.substringAfterLast('/'), false, 0)
                            if (!prepareJsonVaultContext(container, session, monitor, entry, onConflict)) {
                                onChanged()
                                return@MooMenuItem
                            }
                            session.dialogInputMode = "json-rename"
                            session.dialogTarget = selected
                            session.dialogInput = jsonVaultRenameDefault(entry)
                            onChanged()
                        }
                    ) { Text(container.t("json.vault.rename")) }
                    MooMenuItem(
                        enabled = hasSelection,
                        onClick = {
                            vaultMoreOpen = false
                            if (selected.isBlank()) return@MooMenuItem
                            val entry = selectedEntry
                                ?: VaultEntry(selected, selected.substringAfterLast('/'), false, 0)
                            if (!prepareJsonVaultContext(container, session, monitor, entry, onConflict)) {
                                onChanged()
                                return@MooMenuItem
                            }
                            session.dialogInputMode = "json-move"
                            session.dialogTarget = selected
                            session.dialogInput = VaultMove.parentDirectory(selected)
                            onChanged()
                        }
                    ) { Text(container.t("json.vault.move")) }
                    MooMenuItem(
                        enabled = canDuplicateFile,
                        onClick = {
                            vaultMoreOpen = false
                            if (!canDuplicateFile) return@MooMenuItem
                            val entry = selectedEntry!!
                            if (!prepareJsonVaultContext(container, session, monitor, entry, onConflict)) {
                                onChanged()
                                return@MooMenuItem
                            }
                            duplicateJsonVaultSnippet(container, session, monitor, selected, onConflict)
                                .onSuccess {
                                    session.notice = container.t("json.vault.duplicated")
                                    container.toastSuccess(container.t("json.vault.duplicated"))
                                }
                                .onFailure { session.notice = it.message ?: container.t("json.notice.failed") }
                            onChanged()
                        }
                    ) { Text(container.t("json.vault.duplicate")) }
                    MooMenuItem(onClick = {
                        vaultMoreOpen = false
                        container.notifyJsonVaultTreeChanged()
                        onChanged()
                    }) {
                        Text(container.t("json.vault.refresh"))
                    }
                    MooMenuItem(onClick = { vaultMoreOpen = false; container.openDirectory(container.jsonVault.root()) }) {
                        Text(container.t("json.vault.openFolder"))
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MooButton(container.t("json.vault.new"), onClick = {
                openJsonVaultNewFileDialog(session, items)
                onChanged()
            })
            MooButton(container.t("json.vault.folder"), onClick = {
                session.dialogInputMode = "json-folder"
                val parent = jsonVaultImportTargetDirectory(
                    session.vaultSelectedPath,
                    session.currentFile,
                    items,
                )
                session.dialogInput = VaultSelectionPath.join(parent, container.t("json.vault.defaultFolder"))
                onChanged()
            })
            MooButton(container.t("json.vault.save"), onClick = {
                jsonVaultSaveFromUserAction(container, session, items, monitor, onConflict)
                onChanged()
            })
            GitActionButton(container.t("json.git.open"), gitChangeCount, onClick = onGit)
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .desktopFileDropTarget(acceptMultiple = true) { dropped ->
                    val dir = jsonVaultImportTargetDirectory(
                        session.vaultSelectedPath,
                        session.currentFile,
                        items,
                    )
                    handleJsonVaultTreeFileDrop(
                        container,
                        session,
                        monitor,
                        dropped,
                        dir,
                        onConflict = onConflict,
                    ) {
                        onFilter()
                        onChanged()
                    }
                }
        ) {
            if (items.isEmpty()) {
                Text(
                    container.t("json.vault.empty"),
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                VaultTreeList(
                items = items,
                selectedPath = session.vaultSelectedPath.ifBlank { session.currentFile },
                emptyLabel = container.t("json.vault.empty"),
                onSelect = { entry ->
                    if (entry.directory) {
                        if (!prepareJsonVaultContext(container, session, monitor, entry, onConflict)) {
                            onChanged()
                            return@VaultTreeList
                        }
                    } else {
                        session.vaultSelectedPath = entry.relativePath
                    }
                    onChanged()
                },
                activeFilePath = session.currentFile,
                activeFileDirty = session.editor.text != session.savedText,
                onOpen = { item ->
                    if (item.directory) return@VaultTreeList
                    openJsonVaultTreeFile(container, session, monitor, item.relativePath, onConflict)
                    onChanged()
                },
                onMove = { from, to ->
                    if (VaultMove.moveAffectsOpenPath(session.currentFile, from) &&
                        !jsonVaultFlushDirtyOrNotice(container, session, monitor, onConflict)
                    ) {
                        onChanged()
                        return@VaultTreeList
                    }
                    runCatching { container.jsonVault.move(from, to) }
                        .onSuccess { next ->
                            val (file, selected) = VaultMove.retargetVaultPaths(
                                session.currentFile,
                                session.vaultSelectedPath,
                                from,
                                next,
                            )
                            session.currentFile = file
                            session.vaultSelectedPath = selected
                            session.notice = container.t("json.vault.moved")
                            container.toastSuccess(container.t("json.vault.moved"))
                            container.recordVaultActivity(VaultGitCheckpointMessages.MOVE_JSON_ENTRY, json = true)
                            monitor.rebaselineAfterLocalCrud()
                        }
                        .onFailure { session.notice = it.message ?: container.t("json.notice.failed") }
                    onChanged()
                },
                modifier = Modifier.fillMaxSize(),
                expandMode = settings.vault.jsonTreeExpandMode,
                sort = session.vaultSort,
                contextMenuPath = session.vaultContextMenuPath,
                onContextMenuPathChange = { session.vaultContextMenuPath = it; onChanged() },
                treeExpanded = session.vaultTreeExpanded,
                onTreeExpandedChange = { session.vaultTreeExpanded = it; onChanged() },
                treeScrollOffset = session.vaultTreeScrollOffset,
                onTreeScrollOffsetChange = { session.vaultTreeScrollOffset = it; onChanged() },
                contextActions = listOf(
                    VaultContextAction(VaultContextId.Rename, container.t("json.vault.rename")),
                    VaultContextAction(VaultContextId.Move, container.t("json.vault.move")),
                    VaultContextAction(VaultContextId.Duplicate, container.t("json.vault.duplicate"), filesOnly = true),
                    VaultContextAction(VaultContextId.Export, container.t("json.action.export"), filesOnly = true),
                    VaultContextAction(VaultContextId.Delete, container.t("json.vault.delete")),
                    VaultContextAction(VaultContextId.Reveal, container.t("vault.reveal")),
                    VaultContextAction(VaultContextId.Git, container.t("git.action"))
                ),
                onContextAction = { entry, id ->
                    if (!prepareJsonVaultContext(container, session, monitor, entry, onConflict)) {
                        onChanged()
                        return@VaultTreeList
                    }
                    if (id == VaultContextId.Delete) {
                        onDeleteRequest(entry.relativePath)
                    } else {
                        handleJsonVaultContext(container, session, monitor, onConflict, onGit, entry, id)
                    }
                    onChanged()
                }
            )
            }
        }
        val vaultFooterPath = JsonVaultFooterPresentation.effectivePath(session.vaultSelectedPath, session.currentFile)
        if (JsonVaultFooterPresentation.showFooter(vaultFooterPath)) {
            VaultSelectionFooter(
                path = vaultFooterPath,
                dirty = JsonVaultFooterPresentation.footerDirty(
                    vaultFooterPath,
                    session.currentFile,
                    session.editor.text,
                    session.savedText,
                ),
            )
        }
        if (JsonVaultFooterPresentation.showFooter(vaultFooterPath)) {
            val footerEntry = items.find { it.relativePath == vaultFooterPath }
            Row(
                modifier = Modifier.mooJsonVaultFooter().mooJsonVaultFooterActions(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MooButton(
                    container.t("json.vault.rename"),
                    enabled = JsonVaultFooterPresentation.canRename(footerEntry?.directory),
                    onClick = {
                    val entry = footerEntry ?: VaultEntry(
                        vaultFooterPath,
                        vaultFooterPath.substringAfterLast('/'),
                        false,
                        0,
                    )
                    if (!prepareJsonVaultContext(container, session, monitor, entry, onConflict)) {
                        onChanged()
                        return@MooButton
                    }
                    session.dialogInputMode = "json-rename"
                    session.dialogTarget = vaultFooterPath
                    session.dialogInput = jsonVaultRenameDefault(entry)
                    onChanged()
                })
                if (JsonVaultFooterPresentation.canDuplicate(footerEntry?.directory)) {
                    MooButton(container.t("json.vault.duplicate"), onClick = {
                        val entry = footerEntry ?: VaultEntry(
                            vaultFooterPath,
                            vaultFooterPath.substringAfterLast('/'),
                            false,
                            0,
                        )
                        if (!prepareJsonVaultContext(container, session, monitor, entry, onConflict)) {
                            onChanged()
                            return@MooButton
                        }
                        duplicateJsonVaultSnippet(container, session, monitor, vaultFooterPath, onConflict)
                            .onSuccess {
                                session.notice = container.t("json.vault.duplicated")
                                container.toastSuccess(container.t("json.vault.duplicated"))
                            }
                            .onFailure { session.notice = it.message ?: container.t("json.notice.failed") }
                        onChanged()
                    })
                }
                MooButton(container.t("json.vault.move"), onClick = {
                    val entry = footerEntry ?: VaultEntry(
                        vaultFooterPath,
                        vaultFooterPath.substringAfterLast('/'),
                        false,
                        0,
                    )
                    if (!prepareJsonVaultContext(container, session, monitor, entry, onConflict)) {
                        onChanged()
                        return@MooButton
                    }
                    session.dialogInputMode = "json-move"
                    session.dialogTarget = vaultFooterPath
                    session.dialogInput = VaultMove.parentDirectory(vaultFooterPath)
                    onChanged()
                })
                MooButton(container.t("json.vault.delete"), onClick = {
                    val entry = footerEntry ?: VaultEntry(
                        vaultFooterPath,
                        vaultFooterPath.substringAfterLast('/'),
                        false,
                        0,
                    )
                    if (!prepareJsonVaultContext(container, session, monitor, entry, onConflict)) {
                        onChanged()
                        return@MooButton
                    }
                    onDeleteRequest(vaultFooterPath)
                    onChanged()
                })
            }
        }
        if (session.dialogInputMode.startsWith("json-")) {
            val moveSource = session.dialogTarget.ifBlank { session.currentFile }
            val moveFolderOptions = remember(items, moveSource) {
                vaultMoveFolderOptions(items, moveSource, container.t("json.vault.root"))
            }
            val jsonDialogPrimary = when (session.dialogInputMode) {
                "json-rename" -> container.t("common.save")
                "json-move" -> container.t("common.save")
                else -> container.t("json.vault.create")
            }
            val jsonDialogFieldLabel = when (session.dialogInputMode) {
                "json-folder" -> container.t("json.vault.folderName")
                "json-rename" -> container.t("json.vault.renameName")
                else -> container.t("json.vault.fileName")
            }
            val jsonDialogCanSubmit = session.dialogInputMode == "json-move" || session.dialogInput.trim().isNotEmpty()
            MooOverlay(onDismiss = { session.dialogInputMode = ""; session.dialogTarget = ""; onChanged() }) {
                Column(
                    Modifier.width(440.dp).mooDialogSurface().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        when (session.dialogInputMode) {
                            "json-file" -> container.t("json.vault.new")
                            "json-folder" -> container.t("json.vault.folder")
                            "json-move" -> container.t("json.vault.move")
                            else -> container.t("json.vault.rename")
                        },
                        color = colors.textPrimary
                    )
                    if (session.dialogInputMode == "json-move") {
                        Text(container.t("json.vault.moveTo"), color = colors.textSecondary, fontSize = 11.sp)
                        VaultSortMenu(
                            current = session.dialogInput,
                            options = moveFolderOptions,
                            onChange = { session.dialogInput = it; onChanged() }
                        )
                    } else {
                        Text(jsonDialogFieldLabel, color = colors.textSecondary, fontSize = 11.sp)
                        MooTextField(session.dialogInput, { session.dialogInput = it; onChanged() })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MooButton(jsonDialogPrimary, prominent = true, enabled = jsonDialogCanSubmit, onClick = {
                            val name = session.dialogInput.trim()
                            val target = session.dialogTarget.ifBlank { session.currentFile }
                            if (name.isNotEmpty() || session.dialogInputMode == "json-move") {
                                val dialogMode = session.dialogInputMode
                                runCatching {
                                    when (dialogMode) {
                                        "json-file" -> {
                                            jsonVaultRequireFlushDirty(container, session, monitor, onConflict)
                                            val path = VaultSelectionPath.resolveEntryPath(
                                                name,
                                                session.vaultSelectedPath,
                                                session.currentFile,
                                                items,
                                            )
                                            val body = newJsonVaultFileContent(session.editor.text)
                                            container.jsonVault.createFile(path, body)
                                            loadJsonVaultSnippet(session, path, body)
                                            container.recordVaultActivity(VaultGitCheckpointMessages.UPDATE_JSON_SNIPPET, json = true)
                                        }
                                        "json-folder" -> {
                                            jsonVaultRequireFlushDirty(container, session, monitor, onConflict)
                                            val path = VaultSelectionPath.resolveEntryPath(
                                                name,
                                                session.vaultSelectedPath,
                                                session.currentFile,
                                                items,
                                            )
                                            container.jsonVault.createDirectory(path)
                                            session.vaultSelectedPath = path
                                        }
                                        "json-move" -> {
                                            if (VaultMove.moveAffectsOpenPath(session.currentFile, target)) {
                                                jsonVaultRequireFlushDirty(container, session, monitor, onConflict)
                                            }
                                            val dest = session.dialogInput.trim()
                                            val next = container.jsonVault.move(target, dest)
                                            val (file, selected) = VaultMove.retargetVaultPaths(
                                                session.currentFile,
                                                session.vaultSelectedPath,
                                                target,
                                                next,
                                            )
                                            session.currentFile = file
                                            session.vaultSelectedPath = selected
                                        }
                                        else -> {
                                            if (VaultMove.moveAffectsOpenPath(session.currentFile, target)) {
                                                jsonVaultRequireFlushDirty(container, session, monitor, onConflict)
                                            }
                                            val next = container.jsonVault.rename(target, name)
                                            val (file, selected) = VaultMove.retargetVaultPaths(
                                                session.currentFile,
                                                session.vaultSelectedPath,
                                                target,
                                                next,
                                            )
                                            session.currentFile = file
                                            session.vaultSelectedPath = selected
                                        }
                                    }
                                }.onSuccess {
                                    VaultGitCheckpointMessages.jsonDialogMode(dialogMode)?.let {
                                        container.recordVaultActivity(it, json = true)
                                    }
                                    monitor.rebaselineAfterLocalCrud()
                                    val mode = dialogMode
                                    session.dialogInputMode = ""
                                    session.dialogTarget = ""
                                    when (mode) {
                                        "json-move" -> {
                                            session.notice = container.t("json.vault.moved")
                                            container.toastSuccess(container.t("json.vault.moved"))
                                        }
                                        "json-file" -> {
                                            session.notice = container.t("json.vault.created")
                                            container.toastSuccess(container.t("json.vault.created"))
                                        }
                                        "json-folder" -> {
                                            session.notice = container.t("json.vault.folderCreated")
                                            container.toastSuccess(container.t("json.vault.folderCreated"))
                                        }
                                        "json-rename" -> {
                                            session.notice = container.t("json.vault.renamed")
                                            container.toastSuccess(container.t("json.vault.renamed"))
                                        }
                                        else -> {
                                            session.notice = container.t("json.vault.saved")
                                            container.toastSuccess(container.t("json.vault.saved"))
                                        }
                                    }
                                }.onFailure { session.notice = it.message ?: container.t("json.notice.failed") }
                                onChanged()
                            }
                        })
                        MooButton(container.t("common.cancel"), onClick = { session.dialogInputMode = ""; session.dialogTarget = ""; onChanged() })
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
    var quickPathMenuOpen by remember { mutableStateOf(false) }
    Column(
        Modifier.width(width.dp).fillMaxHeight().mooToolShell(colors.surfaceSubtle, flatten = true, endBorder = false).padding(10.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(container.t("json.action.more"), color = colors.textPrimary, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            MooIconButton(
                label = container.t("common.close"),
                modifier = Modifier.width(28.dp).height(28.dp),
                onClick = {
                    session.inspectorOpen = false
                    if (session.compactAux == "inspector") session.compactAux = ""
                    onChanged()
                },
            ) {
                Text("×", color = colors.textMuted, fontSize = 14.sp)
            }
        }
        val inspectorText = session.editor.text
        val structureAnalysis = remember(inspectorText, session.editor.revision) {
            JsonEngine.analyzeStructure(inspectorText, translator)
        }
        val structureDuplicates = remember(
            inspectorText,
            session.editor.revision,
            session.formatOptions.ignoreCase,
        ) {
            jsonInspectorDuplicateKeys(inspectorText, session.formatOptions.ignoreCase)
        }
        val pathEntries = remember(inspectorText, session.editor.revision, translator) {
            runCatching { JsonEngine.listPaths(inspectorText, translator) }.getOrDefault(emptyList())
        }
        MooCard(Modifier.fillMaxWidth()) {
            Text(container.t("json.panel.structure"), color = colors.textPrimary, fontSize = 12.sp)
            JsonInspectorStructurePanel(
                analysis = structureAnalysis,
                duplicates = structureDuplicates,
                rootTypeLabel = container.t("json.analysis.rootType"),
                nodesLabel = container.t("json.analysis.nodes"),
                keysLabel = container.t("json.analysis.keys"),
                maxDepthLabel = container.t("json.analysis.maxDepth"),
                duplicatesLabel = container.t("json.analysis.duplicates"),
                utf8Label = container.t("json.analysis.utf8"),
                onDuplicatePathClick = { jsonInspectorCopyJsonPath(it, container) },
            )
            if (jsonInspectorInferSchemaEnabled(structureAnalysis)) {
                MooButton(
                    container.t("json.action.inferSchema"),
                    p5Toolbar = true,
                    modifier = Modifier.padding(top = 6.dp),
                    onClick = {
                        showResult(container, session, translator, container.t("json.action.inferSchema")) { input ->
                            JsonWiringPresentation.runTransform(input) { JsonEngine.inferJsonSchema(it, translator) }
                        }
                        onChanged()
                    },
                )
            }
        }
        MooCard(Modifier.fillMaxWidth()) {
        Text(container.t("json.panel.format"), color = colors.textPrimary, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(container.t("json.format.indent"), color = colors.textSecondary, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            MooSegmented(
                options = listOf("2" to "2", "4" to "4"),
                value = session.formatOptions.normalizeInspectorIndent().spaces.toString(),
                onChange = {
                    session.formatOptions = session.formatOptions.copy(spaces = it.toInt()).normalizeInspectorIndent()
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
        MooButton(
            container.t("json.format.apply"),
            prominent = true,
            p5Toolbar = true,
            onClick = {
                transform(
                    container,
                    session,
                    translator,
                    container.t("json.notice.formatted"),
                    historySummary = container.t("json.format.apply"),
                ) { input ->
                    JsonWiringPresentation.runFormatAdvanced(input, translator, session.formatOptions)
                }
                onChanged()
            },
            modifier = Modifier.fillMaxWidth()
        )
        }
        MooCard(Modifier.fillMaxWidth()) {
        Text(container.t("json.panel.convert"), color = colors.textPrimary, fontSize = 12.sp)
        InspectorActionGrid(
            listOf(
                container.t("json.action.jsonToXml") to {
                    showResult(container, session, translator, container.t("json.action.jsonToXml")) { input ->
                        JsonWiringPresentation.runTransform(input) { JsonEngine.jsonToXml(it, translator) }
                    }
                    onChanged()
                },
                container.t("json.action.xmlToJson") to {
                    session.conversionMode = "xml"
                    session.conversionInput = ""
                    onChanged()
                },
                container.t("json.action.beanToJson") to {
                    session.conversionMode = "bean"
                    session.conversionInput = ""
                    onChanged()
                },
                container.t("json.action.jsonToBean") to {
                    showResult(container, session, translator, container.t("json.action.jsonToBean")) { input ->
                        JsonWiringPresentation.runTransform(input) {
                            JsonEngine.jsonToJavaBean(it, translator, session.className.ifBlank { "Root" })
                        }
                    }
                    onChanged()
                },
                container.t("json.action.swap") to {
                    transform(container, session, translator, container.t("json.action.swap")) { input ->
                        JsonWiringPresentation.runTransform(input) { JsonEngine.swapKeysAndValues(it, translator) }
                    }
                    onChanged()
                },
                container.t("json.action.escape") to {
                    transform(
                        container,
                        session,
                        translator,
                        container.t("json.notice.escaped"),
                        historySummary = container.t("json.action.escape"),
                    ) { input -> JsonWiringPresentation.runTransform(input) { JsonEngine.escapeJsonString(it) } }
                    onChanged()
                },
                container.t("json.action.unescape") to {
                    transform(
                        container,
                        session,
                        translator,
                        container.t("json.notice.unescaped"),
                        historySummary = container.t("json.action.unescape"),
                    ) { input ->
                        JsonWiringPresentation.runTransform(input) { JsonEngine.unescapeJsonString(it, translator) }
                    }
                    onChanged()
                },
                container.t("json.action.escapeText") to {
                    transform(container, session, translator, container.t("json.action.escapeText")) { input ->
                        JsonWiringPresentation.runTransform(input) { JsonEngine.escapeJavaString(it) }
                    }
                    onChanged()
                },
                container.t("json.action.unescapeText") to {
                    transform(container, session, translator, container.t("json.action.unescapeText")) { input ->
                        JsonWiringPresentation.runTransform(input) { JsonEngine.unescapeJsonText(it) }
                    }
                    onChanged()
                }
            )
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(container.t("json.dialog.className"), color = colors.textSecondary, fontSize = 11.sp)
            MooTextField(session.className, { session.className = it; onChanged() }, placeholder = "Root", modifier = Modifier.weight(1f))
        }
        }
        MooCard(Modifier.fillMaxWidth()) {
        Text(container.t("json.panel.jsonPath"), color = colors.textPrimary, fontSize = 12.sp)
        val pathAppliedNotice = container.t("json.notice.pathApplied")
        if (pathEntries.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(container.t("json.path.picker"), color = colors.textSecondary, fontSize = 11.sp)
                Box(Modifier.weight(1f)) {
                    MooButton(
                        jsonPathQuickPickerButtonLabel(session.jsonPath, pathEntries),
                        p5Toolbar = true,
                        onClick = { quickPathMenuOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    MooMenu(expanded = quickPathMenuOpen, onDismissRequest = { quickPathMenuOpen = false }) {
                        jsonInspectorVisiblePathEntries(pathEntries).forEach { entry ->
                            MooMenuItem(onClick = {
                                quickPathMenuOpen = false
                                session.applyInspectorJsonPathInput(entry.path, pathAppliedNotice)
                                onChanged()
                            }) {
                                Text(jsonPathQuickPickerMenuLabel(entry), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().mooJsonInspectorPathActions(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
        MooTextField(
            session.jsonPath,
            { session.applyInspectorJsonPathInput(it, pathAppliedNotice); onChanged() },
            placeholder = container.t("json.path.placeholder"),
            modifier = Modifier.weight(1f),
            fieldModifier = Modifier.onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || event.blockedByIme()) return@onPreviewKeyEvent false
                if (event.key == Key.Enter || event.key == Key.NumPadEnter) {
                    queryJsonPathInspector(container, session, translator, onChanged)
                    true
                } else {
                    false
                }
            }
        )
        MooButton(
            container.t("json.path.copy"),
            p5Toolbar = true,
            onClick = { jsonInspectorCopyJsonPath(session.jsonPath, container) },
        )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            MooButton(
                container.t("json.path.query"),
                prominent = true,
                p5Toolbar = true,
                onClick = { queryJsonPathInspector(container, session, translator, onChanged) },
                modifier = Modifier.weight(0.8f)
            )
            MooButton(
                container.t("json.path.pick"),
                p5Toolbar = true,
                onClick = {
                    session.pathPickerOpen = true
                    onChanged()
                },
                modifier = Modifier.weight(1.2f)
            )
        }
        }
        MooCard(Modifier.fillMaxWidth().mooJsonInspectorSectionResult()) {
        Text(container.t("json.panel.result"), color = colors.textPrimary, fontSize = 12.sp)
        val resultDisplay = jsonInspectorResultDisplay(
            pathResult = session.pathResult,
            notice = session.notice,
            status = status,
            pathAppliedNotice = container.t("json.notice.pathApplied"),
            jsonPathPanelTitle = container.t("json.panel.jsonPath"),
        )
        Text(
            resultDisplay.text,
            color = if (resultDisplay.isError) colors.danger else colors.textPrimary,
            fontSize = 11.sp
        )
        }
        if (pathEntries.isNotEmpty()) {
            MooCard(Modifier.fillMaxWidth()) {
            Text(container.t("json.panel.pathTree"), color = colors.textPrimary, fontSize = 12.sp)
            val visiblePaths = jsonInspectorVisiblePathEntries(pathEntries)
            val inspectorInput = session.editor.text
            val selectedPathPreview = remember(inspectorInput, session.editor.revision, session.jsonPath) {
                val fallback = visiblePaths.find { it.path == session.jsonPath }?.preview ?: ""
                jsonPathNodePreview(inspectorInput, session.jsonPath, translator, fallback)
            }
            visiblePaths.forEach { entry ->
                val selected = session.jsonPath == entry.path
                JsonPathListRow(
                    entry = entry,
                    selected = selected,
                    showInlinePreview = true,
                    inlinePreviewText = if (selected) selectedPathPreview else null,
                    onTap = {
                        val preview = jsonPathNodePreview(inspectorInput, entry.path, translator, entry.preview)
                        session.applyInlinePathTreePreview(
                            entry.path,
                            preview,
                            container.t("json.notice.pathApplied"),
                        )
                        onChanged()
                    },
                    onDoubleTap = {
                        val title = container.t("json.panel.jsonPath")
                        session.performInlinePathTreeDoubleTapQuery(
                            inspectorInput,
                            entry.path,
                            entry.preview,
                            container.t("json.notice.pathApplied"),
                            title,
                            translator,
                        )
                        if (session.pathResult.isNotBlank()) {
                            session.dialogTitle = title
                            session.dialogBody = session.pathResult
                            container.history.save(
                                ToolId.Json.id,
                                title,
                                title,
                                inspectorInput,
                                session.pathResult,
                                JsonHistoryMetadata.encodePathQuery(),
                            )
                        } else if (session.notice.isNotBlank()) {
                            container.toastError(session.notice)
                        }
                        onChanged()
                    }
                )
            }
            if (jsonInspectorPathTreeShowsTruncationHint(pathEntries)) {
                Text(
                    container.t(
                        "json.pathTree.truncated",
                        mapOf("total" to pathEntries.size.toString(), "limit" to JSON_INSPECTOR_INLINE_PATH_LIMIT.toString())
                    ),
                    color = colors.textMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            }
        }
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
        Text(label, color = colors.textPrimary, fontSize = 12.sp, modifier = Modifier.mooFocusClickable { onChange(!checked) }.weight(1f))
    }
}

@Composable
private fun InspectorActionGrid(actions: List<Pair<String, () -> Unit>>) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        actions.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                pair.forEach { (label, onClick) ->
                    MooButton(label, onClick = onClick, p5Toolbar = true, modifier = Modifier.weight(1f))
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ResultDialog(
    container: AppContainer,
    session: JsonSession,
    monitor: VaultRevisionMonitor?,
    onConflict: (VaultConflictState) -> Unit,
    onChanged: () -> Unit,
) {
    MooOverlay(onDismiss = { session.dialogTitle = ""; session.dialogBody = ""; onChanged() }) {
        Column(
            Modifier.width(560.dp).height(420.dp).mooDialogSurface().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(session.dialogTitle, color = MooTheme.colors.textPrimary)
            Text(container.t("json.dialog.output"), color = MooTheme.colors.textSecondary, fontSize = 11.sp)
            Box(Modifier.weight(1f).fillMaxWidth().border(1.dp, MooTheme.colors.border, RoundedCornerShape(8.dp)).padding(8.dp).verticalScroll(rememberScrollState())) {
                Text(session.dialogBody, color = MooTheme.colors.textPrimary, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("json.action.copy"), onClick = {
                    val outcome = copyText(session.dialogBody, container)
                    session.notice = outcome.notice
                    session.copyState = CopyFeedbackPolicy.afterCopy(outcome.success)
                    session.copyGeneration += 1
                    onChanged()
                })
                MooButton(container.t("json.dialog.useOutput"), prominent = true, onClick = {
                    if (!jsonVaultFlushDirtyOrNotice(container, session, monitor, onConflict)) {
                        onChanged()
                        return@MooButton
                    }
                    onEdt { session.editor.setText(session.dialogBody, recordUndo = true) }
                    session.clearJsonPathQueryResult()
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
private fun InputDialog(
    container: AppContainer,
    session: JsonSession,
    translator: JsonTranslator,
    monitor: VaultRevisionMonitor?,
    onConflict: (VaultConflictState) -> Unit,
    onChanged: () -> Unit,
) {
    fun closeConversion() {
        session.conversionMode = ""
        session.conversionInput = ""
    }
    MooOverlay(onDismiss = { closeConversion(); onChanged() }) {
        Column(
            Modifier.width(520.dp).height(360.dp).mooDialogSurface().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(if (session.conversionMode == "xml") container.t("json.action.xmlToJson") else container.t("json.action.beanToJson"), color = MooTheme.colors.textPrimary)
            Text(container.t("json.dialog.input"), color = MooTheme.colors.textSecondary, fontSize = 11.sp)
            MooTextField(session.conversionInput, { session.conversionInput = it }, modifier = Modifier.fillMaxWidth().height(220.dp), singleLine = false)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.cancel"), onClick = { closeConversion(); onChanged() })
                MooButton(container.t("json.dialog.run"), prominent = true, onClick = {
                    if (!jsonVaultFlushDirtyOrNotice(container, session, monitor, onConflict)) {
                        onChanged()
                        return@MooButton
                    }
                    runCatching {
                        if (session.conversionMode == "xml") JsonEngine.xmlToJson(session.conversionInput, translator)
                        else JsonEngine.javaBeanToJson(session.conversionInput, translator)
                    }.onSuccess { output ->
                        val input = session.conversionInput
                        onEdt { session.editor.setText(output, recordUndo = true) }
                        session.clearJsonPathQueryResult()
                        val title = if (session.conversionMode == "xml") {
                            container.t("json.action.xmlToJson")
                        } else {
                            container.t("json.action.beanToJson")
                        }
                        session.notice = title
                        container.toastSuccess(title)
                        container.history.save(
                            ToolId.Json.id,
                            title,
                            title,
                            input,
                            output,
                            JsonHistoryMetadata.encodeEditor(),
                        )
                        closeConversion()
                    }.onFailure {
                        val message = it.message ?: container.t("json.notice.failed")
                        session.notice = message
                        container.toastError(message)
                    }
                    onChanged()
                })
            }
        }
    }
}

private fun queryJsonPathInspector(
    container: AppContainer,
    session: JsonSession,
    translator: JsonTranslator,
    onChanged: () -> Unit,
) {
    showResult(
        container,
        session,
        translator,
        container.t("json.panel.jsonPath"),
        fillPathResult = true,
    ) { input -> JsonWiringPresentation.runQueryPath(input, session.jsonPath, translator) }
    onChanged()
}

private fun transform(
    container: AppContainer,
    session: JsonSession,
    translator: JsonTranslator,
    notice: String,
    historySummary: String = notice,
    runner: (String) -> JsonWiringPresentation.TransformOutcome,
) {
    val input = session.editor.text
    when (val outcome = runner(input)) {
        is JsonWiringPresentation.TransformOutcome.Success -> {
            val output = outcome.output
            session.clearJsonPathQueryResult()
            onEdt { session.editor.setText(output, recordUndo = true) }
            session.notice = notice
            container.toastSuccess(notice)
            container.history.save(
                ToolId.Json.id,
                historySummary,
                historySummary,
                input,
                output,
                JsonHistoryMetadata.encodeEditor(),
            )
        }
        is JsonWiringPresentation.TransformOutcome.Failure -> {
            val message = outcome.error.message ?: container.t("json.notice.failed")
            session.notice = message
            container.toastError(message)
        }
    }
}

private fun handleJsonVaultContext(
    container: AppContainer,
    session: JsonSession,
    monitor: VaultRevisionMonitor?,
    onConflict: (VaultConflictState) -> Unit,
    onGit: () -> Unit,
    entry: VaultEntry,
    id: VaultContextId,
) {
    when (id) {
        VaultContextId.Rename -> {
            session.dialogInputMode = "json-rename"
            session.dialogTarget = entry.relativePath
            session.dialogInput = jsonVaultRenameDefault(entry)
        }
        VaultContextId.Move -> {
            session.dialogInputMode = "json-move"
            session.dialogTarget = entry.relativePath
            session.dialogInput = VaultMove.parentDirectory(entry.relativePath)
        }
        VaultContextId.Duplicate -> {
            duplicateJsonVaultSnippet(container, session, monitor, entry.relativePath, onConflict)
                .onSuccess {
                    session.notice = container.t("json.vault.duplicated")
                    container.toastSuccess(container.t("json.vault.duplicated"))
                }
                .onFailure { session.notice = it.message ?: container.t("json.notice.failed") }
        }
        VaultContextId.Export -> {
            val defaultName = entry.name.ifBlank { "export.json" }
            chooseFileWithExportDirectory(container, save = true, title = "Export JSON", defaultFileName = defaultName)?.let { file ->
                runCatching { exportJsonVaultEntryToPath(container, session, entry.relativePath, file.toPath()) }
                    .onSuccess {
                        persistToolsExportDirectory(container, file)
                        session.notice = container.t("json.notice.exported")
                        container.toastSuccess(container.t("json.notice.exported"))
                    }
                    .onFailure { session.notice = it.message ?: container.t("json.notice.failed") }
            }
        }
        VaultContextId.Delete -> Unit
        VaultContextId.Reveal -> container.revealInFileManager(container.jsonVault.resolve(entry.relativePath))
        VaultContextId.Info -> Unit
        VaultContextId.Git -> onGit()
    }
}

private fun showResult(
    container: AppContainer,
    session: JsonSession,
    translator: JsonTranslator,
    title: String,
    fillPathResult: Boolean = false,
    runner: (String) -> JsonWiringPresentation.TransformOutcome,
) {
    val input = session.editor.text
    when (val outcome = runner(input)) {
        is JsonWiringPresentation.TransformOutcome.Success -> {
            val output = outcome.output
            session.pathResult = if (fillPathResult) output else ""
            session.dialogTitle = title
            session.dialogBody = output
            session.notice = title
            container.history.save(ToolId.Json.id, title, title, input, output, JsonHistoryMetadata.encodeEditor())
        }
        is JsonWiringPresentation.TransformOutcome.Failure -> {
            if (fillPathResult) session.pathResult = ""
            val message = outcome.error.message ?: container.t("json.notice.failed")
            session.notice = message
            container.toastError(message)
        }
    }
}

private fun jumpFind(container: AppContainer, session: JsonSession, forward: Boolean) {
    if (session.findQuery.isBlank()) return
    val match = RstaFindNavigation.jump(session.editor, session.findQuery, session.findOptions, forward)
    if (match == null) {
        container.toastFindNoMatches()
    } else {
        onEdt { session.editor.select(match.start, match.end) }
    }
}

private data class CopyOutcome(val notice: String, val success: Boolean)

private fun copyText(value: String, container: AppContainer): CopyOutcome {
    val success = container.copyText(value)
    return CopyOutcome(container.t(if (success) "json.notice.copied" else "json.notice.copyFailed"), success)
}

internal fun newJsonVaultFileContent(editorText: String): String =
    editorText.ifBlank { "{\n\n}" }

private fun handleJsonVaultChange(
    container: AppContainer,
    session: JsonSession,
    paths: List<String>,
    onConflict: (VaultConflictState) -> Unit,
    refresh: () -> Unit
) {
    refresh()
    if (jsonVaultApplyExternalChange(container, session, paths, onConflict)) {
        refresh()
    }
}

private fun onEdt(block: () -> Unit) {
    if (SwingUtilities.isEventDispatchThread()) block() else SwingUtilities.invokeLater(block)
}

private fun handleJsonEditorFileDrop(
    container: AppContainer,
    session: JsonSession,
    monitor: VaultRevisionMonitor?,
    vaultItems: List<VaultEntry>,
    files: List<File>,
    onConflict: (VaultConflictState) -> Unit,
    onChanged: () -> Unit,
): Boolean {
    if (files.isEmpty()) return false
    val targetDirectory = jsonVaultImportTargetDirectory(
        session.vaultSelectedPath,
        session.currentFile,
        vaultItems,
    )
    var handled = false
    files.forEach { file ->
        if (!file.isFile) return@forEach
        val result = runCatching {
            when (file.extension.lowercase()) {
                "json" -> {
                    jsonVaultRequireFlushDirty(container, session, monitor, onConflict)
                    val relative = importJsonVaultFile(container.jsonVault, file, targetDirectory)
                    val text = container.jsonVault.read(relative)
                    onEdt { session.editor.setText(text, recordUndo = false) }
                    session.currentFile = relative
                    session.savedText = text
                    session.clearJsonPathQueryResult()
                    monitor.rebaselineAfterLocalCrud()
                    container.recordVaultActivity(VaultGitCheckpointMessages.UPDATE_JSON_SNIPPET, json = true)
                }
                else -> {
                    jsonVaultRequireFlushDirty(container, session, monitor, onConflict)
                    when (val outcome = JsonWiringPresentation.runReadImportFile(file)) {
                        is JsonWiringPresentation.ImportOutcome.Success -> {
                            onEdt { session.editor.setText(outcome.content, recordUndo = true) }
                            session.clearJsonPathQueryResult()
                        }
                        is JsonWiringPresentation.ImportOutcome.Failure ->
                            throw outcome.error
                    }
                }
            }
            session.notice = container.t("json.notice.imported")
            container.toastSuccess(container.t("json.notice.imported"))
        }
        if (result.isSuccess) handled = true
        else session.notice = result.exceptionOrNull()?.message ?: container.t("json.notice.failed")
    }
    if (handled) onChanged()
    return handled
}

private fun handleJsonVaultTreeFileDrop(
    container: AppContainer,
    session: JsonSession,
    monitor: VaultRevisionMonitor?,
    files: List<File>,
    targetDirectory: String,
    onConflict: (VaultConflictState) -> Unit,
    onChanged: () -> Unit,
): Boolean {
    val jsonFiles = files.filter { it.isFile && it.extension.lowercase() == "json" }
    if (jsonFiles.isEmpty()) return false
    if (!jsonVaultFlushDirtyOrNotice(container, session, monitor, onConflict)) return false
    var lastRelative: String? = null
    var importedCount = 0
    jsonFiles.forEach { file ->
        runCatching {
            val relative = importJsonVaultFile(container.jsonVault, file, targetDirectory)
            lastRelative = relative
            importedCount++
        }.onFailure { session.notice = it.message ?: container.t("json.notice.failed") }
    }
    if (importedCount == 0) return false
    monitor.rebaselineAfterLocalCrud()
    if (importedCount == 1 && lastRelative != null) {
        val text = container.jsonVault.read(lastRelative!!)
        onEdt { session.editor.setText(text, recordUndo = false) }
        session.currentFile = lastRelative!!
        session.savedText = text
        session.clearJsonPathQueryResult()
        container.recordVaultActivity(VaultGitCheckpointMessages.UPDATE_JSON_SNIPPET, json = true)
    } else if (importedCount > 1) {
        container.recordVaultActivity(VaultGitCheckpointMessages.UPDATE_JSON_SNIPPET, json = true)
    }
    session.notice = container.t("json.notice.imported")
    container.toastSuccess(container.t("json.notice.imported"))
    onChanged()
    return true
}
