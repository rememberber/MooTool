package com.rememberber.mootool.next.compose.features.quicknote

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.DocumentFormatEngine
import com.rememberber.mootool.next.compose.domain.EditorSettingsLiveApply
import com.rememberber.mootool.next.compose.domain.EditorColumnEditPresentation
import com.rememberber.mootool.next.compose.domain.FindReplace
import com.rememberber.mootool.next.compose.domain.NoteAttachmentEngine
import com.rememberber.mootool.next.compose.domain.NoteColors
import com.rememberber.mootool.next.compose.domain.NoteFrontmatter
import com.rememberber.mootool.next.compose.domain.NoteListEngine
import com.rememberber.mootool.next.compose.domain.NoteMetadata
import com.rememberber.mootool.next.compose.domain.QuickReplaceAction
import com.rememberber.mootool.next.compose.domain.QuickReplaceEngine
import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.domain.QuickNoteHistoryRestore
import com.rememberber.mootool.next.compose.domain.VaultGitCheckpointMessages
import com.rememberber.mootool.next.compose.domain.VaultRevisionMonitor
import com.rememberber.mootool.next.compose.domain.noteOwnWriteQuickNoteFile
import com.rememberber.mootool.next.compose.domain.rebaselineAfterLocalCrud
import com.rememberber.mootool.next.compose.domain.VaultSearchIndex
import com.rememberber.mootool.next.compose.features.git.GitActionButton
import com.rememberber.mootool.next.compose.features.git.VaultGitDialog
import com.rememberber.mootool.next.compose.features.git.gitActionMenuLabel
import com.rememberber.mootool.next.compose.features.git.rememberVaultGitChangeCount
import com.rememberber.mootool.next.compose.domain.QuickNoteVaultFooterPresentation
import com.rememberber.mootool.next.compose.features.vault.QuickNoteVaultConflictOverlay
import com.rememberber.mootool.next.compose.ui.components.mooQuickNoteVaultFooter
import com.rememberber.mootool.next.compose.features.vault.RebBaselineVaultMonitorOnSessionReload
import com.rememberber.mootool.next.compose.features.vault.dismissVaultScopedOverlays
import com.rememberber.mootool.next.compose.features.vault.vaultMoveFolderOptions
import com.rememberber.mootool.next.compose.features.vault.vaultPathsAfterDelete
import com.rememberber.mootool.next.compose.editor.EditorAppShortcuts
import com.rememberber.mootool.next.compose.editor.EditorFindShortcutPolicy
import com.rememberber.mootool.next.compose.editor.onFindBarRowKeys
import com.rememberber.mootool.next.compose.editor.onFindQueryEnterKey
import com.rememberber.mootool.next.compose.editor.openFindBarSeedingSelection
import com.rememberber.mootool.next.compose.editor.EditorFindHighlight
import com.rememberber.mootool.next.compose.editor.RstaFindNavigation
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.rememberber.mootool.next.compose.editor.EditorHost
import com.rememberber.mootool.next.compose.editor.EditorLimits
import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.QuickNoteSession
import com.rememberber.mootool.next.compose.sessions.dismissModalOverlays
import com.rememberber.mootool.next.compose.storage.NoteDocument
import com.rememberber.mootool.next.compose.storage.NoteVault
import com.rememberber.mootool.next.compose.storage.VaultEntry
import com.rememberber.mootool.next.compose.ui.components.desktopFileDropTarget
import com.rememberber.mootool.next.compose.ui.components.FontSelect
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.MooCompactListButton
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooMenu
import com.rememberber.mootool.next.compose.ui.components.MooMenuItem
import com.rememberber.mootool.next.compose.ui.components.MooMenuSeparator
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.mooEditorFrame
import com.rememberber.mootool.next.compose.ui.components.mooToolShell
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.mooStatusBarBackground
import com.rememberber.mootool.next.compose.ui.components.mooFindBarBackground
import com.rememberber.mootool.next.compose.ui.components.MooStatusMeta
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.domain.VaultMove
import com.rememberber.mootool.next.compose.domain.VaultSelectionPath
import com.rememberber.mootool.next.compose.ui.components.VaultContextAction
import com.rememberber.mootool.next.compose.ui.components.VaultContextId
import com.rememberber.mootool.next.compose.ui.components.VaultSortMenu
import com.rememberber.mootool.next.compose.ui.components.VaultSelectionFooter
import com.rememberber.mootool.next.compose.ui.components.OnVaultEffectiveRootChanged
import com.rememberber.mootool.next.compose.ui.components.VaultTreeList
import com.rememberber.mootool.next.compose.ui.components.resetVaultTreeOnCustomRootChange
import com.rememberber.mootool.next.compose.ui.components.IoTwoPaneRow
import com.rememberber.mootool.next.compose.ui.components.VerticalPaneHandle
import com.rememberber.mootool.next.compose.ui.components.setPaneSize
import com.rememberber.mootool.next.compose.ui.components.mooDialogSurface
import com.rememberber.mootool.next.compose.ui.components.MooOverlay
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.DismissModalOverlaysOnDispose
import com.rememberber.mootool.next.compose.ui.workbench.QuickNoteEditorWindowFocus
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import com.rememberber.mootool.next.compose.ui.workbench.blockedByIme
import com.rememberber.mootool.next.compose.ui.chooseFileWithExportDirectory
import com.rememberber.mootool.next.compose.ui.persistToolsExportDirectory
import java.awt.Image as AwtImage
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

@Composable
fun QuickNoteScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.quickNoteSession(container.settings.value.editor.softWrap) }
    val settings by container.settings.collectAsState()
    val revision by container.sessionManager.revision.collectAsState()
    val sessionGeneration by container.sessionManager.sessionGeneration.collectAsState()
    val vault = remember(settings.vault.quickNotePath, settings.data.directory) { container.noteVault() }
    var tick by remember { mutableStateOf(0L) }
    var filterRev by remember { mutableStateOf(0) }
    var gitCountRev by remember { mutableIntStateOf(0) }
    var snapshot by remember { mutableStateOf(vault.snapshot(settings.vault.hideGitignoredFiles)) }
    fun refresh() {
        tick += 1
        container.sessionManager.bump()
        container.sessionManager.persistQuickNote()
    }
    fun persistFilter() {
        quickNoteReloadOpenFileIfClean(session, vault)
        filterRev += 1
        gitCountRev++
        container.sessionManager.bump()
        container.sessionManager.persistQuickNote()
    }
    val quickNoteVaultAutoPullTick by container.quickNoteVaultAutoPullTick.collectAsState()
    LaunchedEffect(quickNoteVaultAutoPullTick, settings.vault.hideGitignoredFiles) {
        if (quickNoteVaultAutoPullTick == 0L) return@LaunchedEffect
        snapshot = withContext(Dispatchers.IO) { vault.snapshot(settings.vault.hideGitignoredFiles) }
        persistFilter()
    }
    val quickNoteVaultRootKey = remember(settings.vault.quickNotePath, settings.data.directory) {
        vault.root().toAbsolutePath().normalize().toString()
    }
    OnVaultEffectiveRootChanged(quickNoteVaultRootKey) {
        session.dismissVaultScopedOverlays()
        session.resetVaultTreeOnCustomRootChange()
        filterRev += 1
        gitCountRev++
        when (quickNoteReloadOpenFileIfClean(session, vault)) {
            QuickNoteReloadOpenResult.ClearedMissing -> session.notice = container.t("vault.conflict.deleted")
            QuickNoteReloadOpenResult.Reloaded -> session.notice = container.t("vault.conflict.reloaded")
            QuickNoteReloadOpenResult.Unchanged -> Unit
        }
        container.sessionManager.bump()
    }
    val gitChangeCount = rememberVaultGitChangeCount(vault.root(), tick + gitCountRev)
    var monitor by remember { mutableStateOf<VaultRevisionMonitor?>(null) }
    DisposableEffect(session.editor) {
        val columnHint = EditorColumnEditPresentation.columnNoticeKey(session.columnLatch, session.wrap)
            ?.let(container::t).orEmpty()
        session.editor.onUserDocumentChange = {
            session.notice = quickNoteNoticeOnUserDocumentChange(session.columnLatch, columnHint)
            session.error = quickNoteErrorOnUserDocumentChange()
            container.sessionManager.bump()
        }
        onDispose { session.editor.onUserDocumentChange = null }
    }
    DismissModalOverlaysOnDispose(container, ToolId.QuickNote) { session.dismissModalOverlays() }
    EditorFindHighlight.ClearOnDispose(session.editor)
    val activeTool by container.activeTool.collectAsState()
    val quickNoteToolActive = detached || activeTool == ToolId.QuickNote
    QuickNoteEditorWindowFocus(session, quickNoteToolActive)
    LaunchedEffect(session.editor.revision, session.metadata, session.currentFile, session.vaultConflict) {
        if (session.vaultConflict != null) return@LaunchedEffect
        if (session.currentFile.isBlank()) return@LaunchedEffect
        if (!quickNoteDirty(session)) return@LaunchedEffect
        kotlinx.coroutines.delay(250)
        if (session.currentFile.isBlank()) return@LaunchedEffect
        if (!quickNoteDirty(session)) return@LaunchedEffect
        if (quickNoteIdleAutosaveAttempt(container, session, vault, monitor) { session.vaultConflict = it }) {
            persistFilter()
        } else {
            container.sessionManager.bump()
        }
    }
    val colors = MooTheme.colors
    val noteShortcuts = EditorAppShortcuts(
        onFind = {
            openFindBarSeedingSelection(session.editor) { selected ->
                selected?.let { session.findQuery = it }
                session.findOpen = true
                session.findReplacedCount = 0
                refresh()
            }
        },
        onFormat = {
            formatCurrentNote(container, session)
            refresh()
        },
        onSave = {
            quickNoteSaveFromUserAction(container, session, vault, monitor) { session.vaultConflict = it }
            refresh()
        }
    )
    DisposableEffect(vault.root()) {
        val next = VaultRevisionMonitor(vault.root(), ignoreAttachments = true) { paths ->
            SwingUtilities.invokeLater {
                container.notifyQuickNoteVaultTreeChanged()
                handleQuickNoteVaultChange(container, session, vault, paths, { session.vaultConflict = it }) {
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
    LaunchedEffect(tick, settings.vault.quickNotePath, settings.data.directory, settings.vault.hideGitignoredFiles, sessionGeneration) {
        snapshot = withContext(Dispatchers.IO) { vault.snapshot(settings.vault.hideGitignoredFiles) }
    }
    LaunchedEffect(activeTool, detached, settings.vault.quickNotePath, settings.data.directory, settings.vault.hideGitignoredFiles, sessionGeneration) {
        if (!detached && activeTool != ToolId.QuickNote) return@LaunchedEffect
        snapshot = withContext(Dispatchers.IO) { vault.snapshot(settings.vault.hideGitignoredFiles) }
        filterRev += 1
        val current = session.currentFile
        if (current.isBlank()) return@LaunchedEffect
        if (quickNoteDirty(session)) return@LaunchedEffect
        withContext(Dispatchers.Swing) {
            when (quickNoteReloadOpenFileIfClean(session, vault)) {
                QuickNoteReloadOpenResult.Reloaded -> session.notice = container.t("vault.conflict.reloaded")
                QuickNoteReloadOpenResult.ClearedMissing -> session.notice = container.t("vault.conflict.deleted")
                QuickNoteReloadOpenResult.Unchanged -> Unit
            }
        }
    }
    val vaultItems = remember(snapshot, session.vaultQuery, session.includeContent, filterRev) {
        VaultSearchIndex.filter(snapshot, session.vaultQuery, session.includeContent).filter { item ->
            val path = item.relativePath.replace('\\', '/')
            path != "attachments" && !path.startsWith("attachments/")
        }
    }
    LaunchedEffect(session) {
        val listener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) { container.sessionManager.bump() }
            override fun removeUpdate(e: DocumentEvent) { container.sessionManager.bump() }
            override fun changedUpdate(e: DocumentEvent) = Unit
        }
        session.editor.document.addDocumentListener(listener)
        try {
            awaitCancellation()
        } finally {
            session.editor.document.removeDocumentListener(listener)
        }
    }

    BoxWithConstraints(
        Modifier.fillMaxSize().background(colors.workspace).onPreviewKeyEvent { event ->
            if (event.blockedByIme()) {
                false
            } else if (event.type == KeyEventType.KeyDown && event.key == Key.S && (event.isMetaPressed || event.isCtrlPressed)) {
                quickNoteSaveFromUserAction(container, session, vault, monitor) { session.vaultConflict = it }
                refresh()
                true
            } else if (event.type == KeyEventType.KeyDown && event.key == Key.F && event.isShiftPressed && (event.isMetaPressed || event.isCtrlPressed)) {
                formatCurrentNote(container, session)
                refresh()
                true
            } else if (
                event.type == KeyEventType.KeyDown &&
                EditorFindShortcutPolicy.opensShellFind(
                    ToolId.QuickNote,
                    event.key,
                    meta = event.isMetaPressed || event.isCtrlPressed,
                    shift = event.isShiftPressed,
                    alt = event.isAltPressed,
                )
            ) {
                openFindBarSeedingSelection(session.editor) { selected ->
                    selected?.let { session.findQuery = it }
                    session.findOpen = true
                    session.findReplacedCount = 0
                    refresh()
                }
                true
            } else if (event.type == KeyEventType.KeyDown && event.key == Key.Escape && session.findOpen) {
                session.findOpen = false
                session.findReplacedCount = 0
                refresh()
                true
            } else false
        }
    ) {
        val compact = LayoutPolicy.isCompact(maxWidth.value)
        val overflow = LayoutPolicy.overflowToolbar(maxWidth.value)
        var moreOpen by remember { mutableStateOf(false) }
        Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 8.dp).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MooPageTitle(container.t("quickNote.title"))
            MooButton(
                if (session.vaultTreeOpen) container.t("quickNote.openVault") else container.t("quickNote.vault"),
                primary = session.vaultTreeOpen,
                onClick = {
                    session.vaultTreeOpen = !session.vaultTreeOpen
                    refresh()
                },
            )
            MooButton(container.t("quickNote.save"), prominent = true, onClick = {
                quickNoteSaveFromUserAction(container, session, vault, monitor) { session.vaultConflict = it }
                refresh()
            })
            MooButton(container.t("quickNote.format"), onClick = {
                formatCurrentNote(container, session)
                refresh()
            })
            MooButton(container.t("quickNote.bulletList"), onClick = {
                applyListPrefix(container, session, NoteListEngine.Prefix.Bullet)
                refresh()
            })
            MooButton(container.t("quickNote.numberedList"), onClick = {
                applyListPrefix(container, session, NoteListEngine.Prefix.Numbered)
                refresh()
            })
            MooButton(if (session.wrap) container.t("quickNote.wrap") else container.t("json.action.nowrap"), onClick = {
                session.wrap = !session.wrap
                session.metadata = session.metadata.copy(lineWrap = session.wrap)
                refresh()
            })
            var colorOpen by remember { mutableStateOf(false) }
            val colorShape = RoundedCornerShape(6.dp)
            val currentColor = NoteColors.normalize(session.metadata.color)
            Box {
                Box(
                    Modifier
                        .size(width = 36.dp, height = 32.dp)
                        .clip(colorShape)
                        .background(if (colorOpen) colors.hoveredControlFill() else colors.workspace)
                        .border(1.dp, if (colorOpen) colors.borderControlHover else colors.borderControl, colorShape)
                        .clickable { colorOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(NoteColors.argb(currentColor)))
                            .border(1.dp, colors.borderControl.copy(alpha = 0.78f), CircleShape)
                    )
                }
                MooMenu(expanded = colorOpen, onDismissRequest = { colorOpen = false }) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        NoteColors.swatches.chunked(4).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { (id, argb) ->
                                    val active = currentColor == id
                                    Box(
                                        Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(if (active) colors.hoveredControlFill() else Color.Transparent)
                                            .clickable {
                                                session.metadata = session.metadata.copy(color = id)
                                                colorOpen = false
                                                refresh()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Color(argb))
                                                .border(1.dp, colors.borderControl.copy(alpha = 0.78f), CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            FontSelect(
                value = session.metadata.fontName.ifBlank { settings.editor.quickNoteFontName },
                ariaLabel = container.t("quickNote.font"),
                labels = mapOf("ui-monospace" to container.t("quickNote.font.mono")),
                emptyLabel = container.t("quickNote.font.system"),
                searchPlaceholder = container.t("quickNote.font"),
                onChange = { name ->
                    session.metadata = session.metadata.copy(fontName = name)
                    refresh()
                }
            )
            if (!overflow) {
            Text(container.t("quickNote.fontSize"), color = colors.textSecondary, fontSize = 12.sp)
            listOf(12, 13, 14, 16, 18).forEach { size ->
                val current = if (session.metadata.fontSize > 0) session.metadata.fontSize else settings.editor.quickNoteFontSize
                MooButton(size.toString(), primary = current == size, onClick = {
                    session.metadata = session.metadata.copy(fontSize = size)
                    refresh()
                })
            }
            Text(container.t("quickNote.lineSpacing"), color = colors.textSecondary, fontSize = 12.sp)
            listOf(1.0, 1.2, 1.4, 1.6, 1.8, 2.0).forEach { spacing ->
                MooButton(
                    "${"%.1f".format(java.util.Locale.US, spacing)}×",
                    primary = NoteFrontmatter.clampLineSpacing(session.metadata.lineSpacing) == spacing,
                    onClick = {
                        session.metadata = session.metadata.copy(lineSpacing = spacing)
                        refresh()
                    }
                )
            }
            }
            val syntax = session.metadata.syntax.ifBlank { "text/plain" }
            var syntaxOpen by remember { mutableStateOf(false) }
            Box {
                MooButton(
                    "${container.t("quickNote.syntax")} · ${NoteSyntaxOptions.firstOrNull { it.first == syntax }?.second ?: syntax}",
                    onClick = { syntaxOpen = true }
                )
                MooMenu(expanded = syntaxOpen, onDismissRequest = { syntaxOpen = false }) {
                    NoteSyntaxOptions.forEach { (id, label) ->
                        MooMenuItem(label) {
                            session.metadata = session.metadata.copy(syntax = id)
                            session.editor.syntax = DocumentFormatEngine.rstaSyntax(id)
                            syntaxOpen = false
                            refresh()
                        }
                    }
                }
            }
            MooButton(container.t("quickNote.view.edit"), primary = session.viewMode == "edit", onClick = {
                session.viewMode = "edit"
                refresh()
            })
            MooButton(container.t("quickNote.view.split"), primary = session.viewMode == "split", onClick = {
                session.viewMode = "split"
                refresh()
            })
            MooButton(container.t("quickNote.view.preview"), primary = session.viewMode == "preview", onClick = {
                session.viewMode = "preview"
                refresh()
            })
            if (!overflow) {
            MooButton(container.t("quickNote.columnEdit"), primary = session.columnLatch, onClick = {
                session.columnLatch = !session.columnLatch
                session.notice = EditorColumnEditPresentation.columnNoticeKey(session.columnLatch, session.wrap)
                    ?.let(container::t).orEmpty()
                refresh()
            })
            MooButton(container.t("quickNote.pasteImage"), onClick = {
                pasteClipboardImage(container, session, vault)
                refresh()
            })
            MooButton(container.t("quickNote.insertImage"), onClick = {
                insertImageFile(container, session, vault)
                refresh()
            })
            }
            MooButton(container.t("quickNote.find"), onClick = {
                if (session.findOpen) {
                    session.findOpen = false
                    refresh()
                } else {
                    openFindBarSeedingSelection(session.editor) { selected ->
                        selected?.let { session.findQuery = it }
                        session.findOpen = true
                        session.findReplacedCount = 0
                        refresh()
                    }
                }
            })
            MooButton(container.t("json.action.copy"), onClick = {
                if (container.copyText(session.editor.text)) {
                    session.notice = container.t("json.notice.copied")
                } else {
                    session.notice = container.t("json.notice.copyFailed")
                }
                refresh()
            })
            if (!overflow) {
            MooButton(container.t("json.action.import"), onClick = {
                chooseFile(container, save = false)?.let { file ->
                    runQuickNoteVaultImport(container, session, vault, monitor, vaultItems, file) { session.vaultConflict = it }
                        .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
                    refresh()
                }
            })
            MooButton(container.t("quickNote.export"), enabled = session.currentFile.isNotBlank(), onClick = {
                val defaultName = quickNoteExportDefaultFileName(session.currentFile, session.metadata.title)
                chooseFile(container, save = true, defaultFileName = defaultName)?.let { file ->
                    runCatching { exportQuickNoteVaultEntryToPath(session, vault, session.currentFile, file.toPath()) }
                        .onSuccess {
                            persistToolsExportDirectory(container, file)
                            session.notice = container.t("json.notice.exported")
                            container.toastSuccess(container.t("json.notice.exported"))
                        }
                        .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
                    refresh()
                }
            })
            }
            MooButton(container.t("quickNote.quickReplace"), primary = LayoutPolicy.showReplace(compact, session.compactAux, session.replaceOpen), onClick = {
                if (compact) {
                    session.compactAux = LayoutPolicy.toggleAux(session.compactAux, "replace")
                    session.replaceOpen = session.compactAux == "replace"
                } else {
                    session.replaceOpen = !session.replaceOpen
                }
                refresh()
            })
            if (compact) {
                MooButton(container.t("quickNote.vault"), primary = session.compactAux == "vault", onClick = {
                    session.compactAux = LayoutPolicy.toggleAux(session.compactAux, "vault")
                    refresh()
                })
            }
            if (!overflow) {
            MooButton(container.t("common.action.history"), onClick = { session.historyOpen = true; refresh() })
            GitActionButton(container.t("quickNote.git"), gitChangeCount, onClick = { session.gitDialogOpen = true; refresh() })
            } else {
                Box {
                    MooButton(container.t("json.action.overflow"), primary = moreOpen, onClick = { moreOpen = true })
                    MooMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                        listOf(12, 13, 14, 16, 18).forEach { size ->
                            MooMenuItem("${container.t("quickNote.fontSize")} $size") {
                                session.metadata = session.metadata.copy(fontSize = size)
                                moreOpen = false
                                refresh()
                            }
                        }
                        listOf(1.0, 1.2, 1.4, 1.6, 1.8, 2.0).forEach { spacing ->
                            MooMenuItem("${container.t("quickNote.lineSpacing")} ${"%.1f".format(java.util.Locale.US, spacing)}×") {
                                session.metadata = session.metadata.copy(lineSpacing = spacing)
                                moreOpen = false
                                refresh()
                            }
                        }
                        MooMenuSeparator()
                        MooMenuItem(container.t("quickNote.columnEdit")) {
                            moreOpen = false
                            session.columnLatch = !session.columnLatch
                            session.notice = EditorColumnEditPresentation.columnNoticeKey(session.columnLatch, session.wrap)
                                ?.let(container::t).orEmpty()
                            refresh()
                        }
                        MooMenuItem(container.t("quickNote.pasteImage")) {
                            moreOpen = false
                            pasteClipboardImage(container, session, vault)
                            refresh()
                        }
                        MooMenuItem(container.t("quickNote.insertImage")) {
                            moreOpen = false
                            insertImageFile(container, session, vault)
                            refresh()
                        }
                        MooMenuItem(container.t("json.action.import")) {
                            moreOpen = false
                            chooseFile(container, save = false)?.let { file ->
                                runQuickNoteVaultImport(container, session, vault, monitor, vaultItems, file) { session.vaultConflict = it }
                                    .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
                                refresh()
                            }
                        }
                        MooMenuItem(container.t("quickNote.export")) {
                            moreOpen = false
                            if (session.currentFile.isBlank()) return@MooMenuItem
                            val defaultName = quickNoteExportDefaultFileName(session.currentFile, session.metadata.title)
                            chooseFile(container, save = true, defaultFileName = defaultName)?.let { file ->
                                runCatching { exportQuickNoteVaultEntryToPath(session, vault, session.currentFile, file.toPath()) }
                                    .onSuccess {
                                        persistToolsExportDirectory(container, file)
                                        session.notice = container.t("json.notice.exported")
                                        container.toastSuccess(container.t("json.notice.exported"))
                                    }
                                    .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
                                refresh()
                            }
                        }
                        MooMenuSeparator()
                        MooMenuItem(container.t("common.action.history")) {
                            moreOpen = false
                            session.historyOpen = true
                            refresh()
                        }
                        MooMenuItem(onClick = {
                            moreOpen = false
                            session.gitDialogOpen = true; refresh()
                        }) {
                            Text(gitActionMenuLabel(container.t("quickNote.git"), gitChangeCount))
                        }
                        if (!detached) {
                            MooMenuItem(container.t("app.tool.detach")) {
                                moreOpen = false
                                container.sessionManager.detach(ToolId.QuickNote)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (!detached && !overflow) MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.QuickNote) })
        }
        if (session.findOpen) {
            QuickNoteFindBar(container, session) { refresh() }
        }
        LaunchedEffect(session.findOpen, session.findQuery, session.findOptions, tick, session.editor.revision) {
            quickNoteOnEdt {
                EditorFindHighlight.sync(session.editor, session.findOpen, session.findQuery, session.findOptions, colors)
            }
        }
        val showVault = LayoutPolicy.showQuickNoteVault(compact, session.compactAux, session.vaultTreeOpen)
        val showReplace = LayoutPolicy.showReplace(compact, session.compactAux, session.replaceOpen)
        val workspacePaneKey = quickNoteWorkspacePaneKey(showVault, showReplace)
        val vaultWidth = quickNoteWorkspacePane(settings, workspacePaneKey, 0, 240f, 200f, 320f)
        val replaceWidth = quickNoteWorkspacePane(settings, workspacePaneKey, 1, 240f, 240f, 340f)
        Row(Modifier.weight(1f).fillMaxWidth()) {
            if (showVault) {
            Column(Modifier.width(vaultWidth.dp).fillMaxHeight().mooToolShell(colors.sidebar).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(container.t("quickNote.vault"), color = colors.textPrimary, fontSize = 12.sp)
                MooTextField(session.vaultQuery, { session.vaultQuery = it; persistFilter() }, placeholder = container.t("app.search.placeholder"))
                MooButton(
                    container.t("quickNote.searchContent") + ": ${session.includeContent}",
                    onClick = { session.includeContent = !session.includeContent; persistFilter() }
                )
                VaultSortMenu(
                    current = session.vaultSort,
                    options = listOf(
                        "modified" to container.t("quickNote.sort.modified"),
                        "created" to container.t("quickNote.sort.created"),
                        "name" to container.t("quickNote.sort.name")
                    ),
                    onChange = { session.vaultSort = it; persistFilter() }
                )
                MooButton(
                    if (settings.vault.quickNoteTreeExpandMode == "expandAll") container.t("quickNote.collapseAll") else container.t("quickNote.expandAll"),
                    onClick = {
                        val next = if (settings.vault.quickNoteTreeExpandMode == "expandAll") "collapseAll" else "expandAll"
                        container.updateSettings { it.copy(vault = it.vault.copy(quickNoteTreeExpandMode = next)) }
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MooButton(container.t("quickNote.newNote"), onClick = {
                        openQuickNoteNewNoteDialog(session)
                        refresh()
                    })
                    MooButton(container.t("quickNote.newFolder"), onClick = {
                        session.dialogMode = "folder"
                        session.dialogValue = ""
                        refresh()
                    })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MooButton(container.t("quickNote.openVault"), onClick = { container.openDirectory(vault.root()) })
                    MooButton(container.t("quickNote.refreshVault"), onClick = { container.notifyQuickNoteVaultTreeChanged() })
                }
                MooButton(container.t("quickNote.cleanOrphans"), onClick = {
                    val orphans = NoteAttachmentEngine.unreferenced(vault)
                    if (orphans.isEmpty()) {
                        session.notice = container.t("quickNote.orphans.empty")
                    } else {
                        runCatching { orphans.forEach { NoteAttachmentEngine.deleteIfUnreferenced(vault, it) } }
                            .onSuccess { session.notice = container.t("quickNote.orphans.removed", mapOf("count" to orphans.size.toString())) }
                            .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
                    }
                    refresh()
                })
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .desktopFileDropTarget(acceptMultiple = true) { dropped ->
                            val dir = quickNoteVaultImportTargetDirectory(
                                session.vaultSelectedPath,
                                session.currentFile,
                                vaultItems,
                            )
                            handleQuickNoteVaultTreeFileDrop(
                                container,
                                session,
                                vault,
                                monitor,
                                dropped,
                                dir,
                                onConflict = { session.vaultConflict = it },
                            ) {
                                persistFilter()
                                refresh()
                            }
                        }
                ) {
                    if (vaultItems.isEmpty()) {
                        Text(
                            container.t("quickNote.empty"),
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        VaultTreeList(
                        items = vaultItems.map { item ->
                            if (item.relativePath == session.currentFile) item.copy(color = session.metadata.color) else item
                        },
                        selectedPath = session.vaultSelectedPath.ifBlank { session.currentFile },
                        emptyLabel = container.t("quickNote.empty"),
                        onSelect = selectVaultEntry@ { entry ->
                            if (entry.directory) {
                                if (!prepareQuickNoteVaultContext(container, session, vault, monitor, entry) { session.vaultConflict = it }) {
                                    refresh()
                                    return@selectVaultEntry
                                }
                            } else {
                                session.vaultSelectedPath = entry.relativePath
                            }
                            refresh()
                        },
                        activeFilePath = session.currentFile,
                        activeFileDirty = quickNoteDirty(session),
                        onOpen = { item ->
                            if (!item.directory) {
                                prepareQuickNoteVaultContext(container, session, vault, monitor, item) { session.vaultConflict = it }
                                refresh()
                            }
                        },
                        onMove = { from, to ->
                            if (VaultMove.moveAffectsOpenPath(session.currentFile, from) &&
                                !quickNoteVaultSaveIfNeeded(container, session, vault, monitor) { session.vaultConflict = it }
                            ) {
                                session.error = session.error.ifBlank { container.t("quickNote.saveFailed") }
                                refresh()
                                return@VaultTreeList
                            }
                            runCatching { vault.move(from, to) }
                                .onSuccess { next ->
                                    val (file, selected) = VaultMove.retargetVaultPaths(
                                        session.currentFile,
                                        session.vaultSelectedPath,
                                        from,
                                        next,
                                    )
                                    session.currentFile = file
                                    session.vaultSelectedPath = selected
                                    session.notice = container.t("vault.moved")
                                    container.toastSuccess(container.t("quickNote.move"))
                                    container.recordVaultActivity(VaultGitCheckpointMessages.MOVE_QUICK_NOTE_ENTRY)
                                    monitor.rebaselineAfterLocalCrud()
                                }
                                .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
                            refresh()
                        },
                        modifier = Modifier.fillMaxSize(),
                        expandMode = settings.vault.quickNoteTreeExpandMode,
                        sort = session.vaultSort,
                        contextMenuPath = session.vaultContextMenuPath,
                        onContextMenuPathChange = { session.vaultContextMenuPath = it; refresh() },
                        treeExpanded = session.vaultTreeExpanded,
                        onTreeExpandedChange = { session.vaultTreeExpanded = it; refresh() },
                        treeScrollOffset = session.vaultTreeScrollOffset,
                        onTreeScrollOffsetChange = { session.vaultTreeScrollOffset = it; refresh() },
                        contextActions = listOf(
                            VaultContextAction(VaultContextId.Rename, container.t("quickNote.rename")),
                            VaultContextAction(VaultContextId.Move, container.t("quickNote.move")),
                            VaultContextAction(VaultContextId.Duplicate, container.t("quickNote.duplicate"), filesOnly = true),
                            VaultContextAction(VaultContextId.Export, container.t("quickNote.export"), filesOnly = true),
                            VaultContextAction(VaultContextId.Info, container.t("quickNote.info"), filesOnly = true),
                            VaultContextAction(VaultContextId.Delete, container.t("json.vault.delete")),
                            VaultContextAction(VaultContextId.Reveal, container.t("vault.reveal")),
                            VaultContextAction(VaultContextId.Git, container.t("git.action"))
                        ),
                        onContextAction = { entry, id ->
                            when (id) {
                                VaultContextId.Rename -> {
                                    if (!prepareQuickNoteVaultContext(container, session, vault, monitor, entry) { session.vaultConflict = it }) {
                                        refresh()
                                        return@VaultTreeList
                                    }
                                    session.dialogMode = "rename"
                                    session.dialogTarget = entry.relativePath
                                    session.dialogValue = if (entry.directory) {
                                        quickNoteRenameDefault(entry.relativePath, isDirectory = true)
                                    } else if (entry.relativePath == session.currentFile) {
                                        quickNoteRenameDefault(entry.relativePath, isDirectory = false, metadataTitle = session.metadata.title)
                                    } else {
                                        val title = runCatching { vault.readNote(entry.relativePath).metadata.title }.getOrNull()
                                        quickNoteRenameDefault(entry.relativePath, isDirectory = false, metadataTitle = title)
                                    }
                                }
                                VaultContextId.Move -> {
                                    if (!prepareQuickNoteVaultContext(container, session, vault, monitor, entry) { session.vaultConflict = it }) {
                                        refresh()
                                        return@VaultTreeList
                                    }
                                    session.dialogMode = "move"
                                    session.dialogTarget = entry.relativePath
                                    session.dialogValue = VaultMove.parentDirectory(entry.relativePath)
                                }
                                VaultContextId.Duplicate -> {
                                    if (entry.directory) return@VaultTreeList
                                    if (!prepareQuickNoteVaultContext(container, session, vault, monitor, entry) { session.vaultConflict = it }) {
                                        refresh()
                                        return@VaultTreeList
                                    }
                                    duplicateQuickNoteEntry(container, session, vault, monitor, entry.relativePath) { session.vaultConflict = it }
                                        .onSuccess {
                                            session.notice = container.t("quickNote.duplicated")
                                            container.toastSuccess(container.t("quickNote.duplicated"))
                                        }
                                        .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
                                }
                                VaultContextId.Info -> {
                                    if (entry.directory) return@VaultTreeList
                                    if (!prepareQuickNoteVaultContext(container, session, vault, monitor, entry) { session.vaultConflict = it }) {
                                        refresh()
                                        return@VaultTreeList
                                    }
                                    session.documentInfoPath = entry.relativePath
                                }
                                VaultContextId.Export -> {
                                    if (entry.directory) return@VaultTreeList
                                    if (!prepareQuickNoteVaultContext(container, session, vault, monitor, entry) { session.vaultConflict = it }) {
                                        refresh()
                                        return@VaultTreeList
                                    }
                                    val exportTitle = if (entry.relativePath == session.currentFile) {
                                        session.metadata.title
                                    } else {
                                        runCatching { vault.readNote(entry.relativePath).metadata.title }.getOrNull()
                                    }
                                    val defaultName = quickNoteExportDefaultFileName(entry.relativePath, exportTitle)
                                    chooseFile(container, save = true, defaultFileName = defaultName)?.let { file ->
                                        runCatching {
                                            exportQuickNoteVaultEntryToPath(session, vault, entry.relativePath, file.toPath())
                                        }
                                            .onSuccess {
                                                persistToolsExportDirectory(container, file)
                                                session.notice = container.t("json.notice.exported")
                                                container.toastSuccess(container.t("json.notice.exported"))
                                            }
                                            .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
                                    }
                                }
                                VaultContextId.Delete -> {
                                    if (!prepareQuickNoteVaultContext(container, session, vault, monitor, entry) { session.vaultConflict = it }) {
                                        refresh()
                                        return@VaultTreeList
                                    }
                                    session.dialogMode = "delete"
                                    session.dialogTarget = entry.relativePath
                                }
                                VaultContextId.Reveal -> container.revealInFileManager(vault.resolve(entry.relativePath))
                                VaultContextId.Git -> {
                                    session.gitDialogOpen = true
                                    refresh()
                                }
                            }
                            refresh()
                        }
                    )
                    }
                }
                val vaultFooterPath = QuickNoteVaultFooterPresentation.effectivePath(
                    session.vaultSelectedPath,
                    session.currentFile,
                )
                if (QuickNoteVaultFooterPresentation.showFooter(vaultFooterPath)) {
                    VaultSelectionFooter(
                        path = vaultFooterPath,
                        dirty = QuickNoteVaultFooterPresentation.footerDirty(
                            vaultFooterPath,
                            session.currentFile,
                            quickNoteDirty(session),
                        ),
                    )
                }
                if (QuickNoteVaultFooterPresentation.showFooter(vaultFooterPath)) {
                    val footerEntry = vaultItems.find { it.relativePath == vaultFooterPath }
                    Row(
                        modifier = Modifier.mooQuickNoteVaultFooter(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        MooButton(container.t("quickNote.rename"), onClick = {
                            val entry = footerEntry ?: VaultEntry(
                                vaultFooterPath,
                                vaultFooterPath.substringAfterLast('/'),
                                false,
                                0,
                            )
                            if (!prepareQuickNoteVaultContext(container, session, vault, monitor, entry) { session.vaultConflict = it }) {
                                refresh()
                                return@MooButton
                            }
                            session.dialogMode = "rename"
                            session.dialogTarget = vaultFooterPath
                            session.dialogValue = if (entry.directory) {
                                quickNoteRenameDefault(vaultFooterPath, isDirectory = true)
                            } else if (vaultFooterPath == session.currentFile) {
                                quickNoteRenameDefault(vaultFooterPath, isDirectory = false, metadataTitle = session.metadata.title)
                            } else {
                                val title = runCatching { vault.readNote(vaultFooterPath).metadata.title }.getOrNull()
                                quickNoteRenameDefault(vaultFooterPath, isDirectory = false, metadataTitle = title)
                            }
                            refresh()
                        })
                        if (QuickNoteVaultFooterPresentation.canDuplicate(footerEntry?.directory)) {
                            MooButton(container.t("quickNote.duplicate"), onClick = {
                                val entry = footerEntry ?: VaultEntry(
                                    vaultFooterPath,
                                    vaultFooterPath.substringAfterLast('/'),
                                    false,
                                    0,
                                )
                                if (!prepareQuickNoteVaultContext(container, session, vault, monitor, entry) { session.vaultConflict = it }) {
                                    refresh()
                                    return@MooButton
                                }
                                duplicateQuickNoteEntry(container, session, vault, monitor, vaultFooterPath) { session.vaultConflict = it }
                                    .onSuccess {
                                        session.notice = container.t("quickNote.duplicated")
                                        container.toastSuccess(container.t("quickNote.duplicated"))
                                    }
                                    .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
                                refresh()
                            })
                        }
                        MooButton(container.t("quickNote.move"), onClick = {
                            val entry = footerEntry ?: VaultEntry(
                                vaultFooterPath,
                                vaultFooterPath.substringAfterLast('/'),
                                false,
                                0,
                            )
                            if (!prepareQuickNoteVaultContext(container, session, vault, monitor, entry) { session.vaultConflict = it }) {
                                refresh()
                                return@MooButton
                            }
                            session.dialogMode = "move"
                            session.dialogTarget = vaultFooterPath
                            session.dialogValue = VaultMove.parentDirectory(vaultFooterPath)
                            refresh()
                        })
                        MooButton(container.t("json.vault.delete"), onClick = {
                            val entry = footerEntry ?: VaultEntry(
                                vaultFooterPath,
                                vaultFooterPath.substringAfterLast('/'),
                                false,
                                0,
                            )
                            if (!prepareQuickNoteVaultContext(container, session, vault, monitor, entry) { session.vaultConflict = it }) {
                                refresh()
                                return@MooButton
                            }
                            session.dialogMode = "delete"
                            session.dialogTarget = vaultFooterPath
                            refresh()
                        })
                    }
                }
            }
            VerticalPaneHandle(
                onDelta = { container.setPaneSize(workspacePaneKey, 0, vaultWidth + it, 2) },
                onReset = { container.setPaneSize(workspacePaneKey, 0, 240f, 2) }
            )
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                val showEditor = session.viewMode != "preview"
                val showPreview = session.viewMode != "edit"
                val previewText = remember(revision) { session.editor.text }
                if (showEditor && showPreview) {
                    IoTwoPaneRow(
                        container = container,
                        settings = settings,
                        paneKey = "quick-note-editor-preview",
                        minLeft = 260f,
                        minRight = 260f,
                        defaultLeftFraction = 0.5f,
                        modifier = Modifier.fillMaxSize(),
                        left = {
                            QuickNoteEditor(container, session, settings, vault, noteShortcuts)
                        },
                        right = {
                            MarkdownPreviewPane(
                                markdown = previewText,
                                vault = vault,
                                missingLabel = container.t("quickNote.image.missing"),
                                remoteLabel = container.t("quickNote.image.remote"),
                                unsafeLabel = container.t("quickNote.image.unsafe")
                            )
                        }
                    )
                } else if (showPreview) {
                    MarkdownPreviewPane(
                        markdown = previewText,
                        vault = vault,
                        missingLabel = container.t("quickNote.image.missing"),
                        remoteLabel = container.t("quickNote.image.remote"),
                        unsafeLabel = container.t("quickNote.image.unsafe")
                    )
                } else {
                    QuickNoteEditor(container, session, settings, vault, noteShortcuts)
                }
            }
            if (showReplace) {
                VerticalPaneHandle(
                    onDelta = { container.setPaneSize(workspacePaneKey, 1, replaceWidth + it, 2) },
                    onReset = { container.setPaneSize(workspacePaneKey, 1, 240f, 2) }
                )
                Column(
                    Modifier.width(replaceWidth.dp).fillMaxHeight().mooToolShell(colors.surfaceSubtle),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        container.t("quickNote.quickReplace"),
                        color = colors.textPrimary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 11.dp, end = 7.dp, top = 5.dp, bottom = 5.dp)
                    )
                    Column(
                        Modifier.weight(1f).fillMaxWidth().padding(8.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        QuickReplaceEngine.actions.forEach { action ->
                            MooCompactListButton(container.t("quickNote.quick.${action.name.replaceFirstChar { it.lowercase() }}")) {
                                applyReplace(session, action)
                                session.notice = container.t("quickNote.quick.${action.name.replaceFirstChar { it.lowercase() }}")
                                refresh()
                            }
                        }
                    }
                }
            }
        }
        val (chars, words, lines) = remember(session.editor.revision, revision, tick) { QuickReplaceEngine.stats(session.editor.text) }
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.statusBar).mooStatusBarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dirty = quickNoteDirty(session)
            val statusPath = session.currentFile.ifBlank { session.vaultSelectedPath }
            MooStatusMeta(
                listOfNotNull(
                    statusPath.ifBlank { container.t("quickNote.select") },
                    if (dirty) container.t("quickNote.unsaved") else if (session.currentFile.isNotBlank()) container.t("quickNote.saved") else null,
                    if (session.vaultConflict != null) container.t("vault.conflict.banner") else null,
                    if (EditorLimits.exceedsLargeDocument(session.editor.text)) container.t("editor.largeDocument") else null,
                    container.t("quickNote.stats", mapOf("chars" to chars.toString(), "words" to words.toString(), "lines" to lines.toString()))
                ).joinToString(" · "),
                color = colors.textMuted
            )
            Spacer(Modifier.weight(1f))
            if (session.error.isNotBlank()) MooStatusMeta(session.error, color = colors.danger)
            else if (session.notice.isNotBlank()) MooStatusMeta(session.notice)
            if (detached) MooStatusMeta(" · detached")
        }
        }
    }

    if (session.dialogMode.isNotBlank()) {
        MooOverlay(onDismiss = { session.dialogMode = ""; session.dialogTarget = ""; refresh() }) {
            Column(
                Modifier.width(420.dp).mooDialogSurface().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    when (session.dialogMode) {
                        "folder" -> container.t("quickNote.dialog.createFolder")
                        "rename" -> container.t("quickNote.dialog.rename")
                        "move" -> container.t("quickNote.dialog.move")
                        "delete" -> container.t("quickNote.delete")
                        else -> container.t("quickNote.dialog.createNote")
                    },
                    color = colors.textPrimary
                )
                if (session.dialogMode == "delete") {
                    val deleteName = session.dialogTarget.ifBlank { session.currentFile }
                    Text(
                        container.t("quickNote.confirmDelete", mapOf("name" to deleteName)),
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                } else if (session.dialogMode == "move") {
                    val moveSource = session.dialogTarget.ifBlank { session.currentFile }
                    val moveFolderOptions = remember(vaultItems, moveSource) {
                        vaultMoveFolderOptions(vaultItems, moveSource, container.t("quickNote.dialog.root"))
                    }
                    Text(container.t("quickNote.dialog.target"), color = colors.textSecondary, fontSize = 11.sp)
                    VaultSortMenu(
                        current = session.dialogValue,
                        options = moveFolderOptions,
                        onChange = { session.dialogValue = it; refresh() }
                    )
                } else {
                    Text(container.t("quickNote.dialog.name"), color = colors.textSecondary, fontSize = 11.sp)
                    MooTextField(session.dialogValue, { session.dialogValue = it; refresh() }, placeholder = container.t("quickNote.dialog.name"))
                }
                val dialogPrimaryLabel = when (session.dialogMode) {
                    "delete" -> container.t("quickNote.delete")
                    "note", "folder" -> container.t("quickNote.create")
                    else -> container.t("quickNote.apply")
                }
                val dialogCanSubmit = session.dialogMode == "delete" ||
                    session.dialogMode == "move" ||
                    session.dialogValue.trim().isNotEmpty()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (session.dialogMode == "delete") {
                        MooButton(container.t("quickNote.delete"), danger = true, onClick = {
                            val target = session.dialogTarget.ifBlank { session.currentFile }
                            if (target.isBlank()) return@MooButton
                            runCatching { vault.delete(target) }
                                .onSuccess {
                                    val afterDelete = vaultPathsAfterDelete(target, session.currentFile, session.vaultSelectedPath)
                                    session.currentFile = afterDelete.currentFile
                                    session.vaultSelectedPath = afterDelete.vaultSelectedPath
                                    if (afterDelete.clearedOpenFile) {
                                        quickNoteOnEdt { session.editor.setText("", recordUndo = false) }
                                        session.savedText = ""
                                        session.metadata = NoteMetadata.defaults("Untitled")
                                        session.savedMetadata = session.metadata
                                    }
                                    session.dialogMode = ""
                                    session.dialogTarget = ""
                                    session.error = ""
                                    session.notice = container.t("quickNote.deleted")
                                    container.recordVaultActivity(VaultGitCheckpointMessages.DELETE_QUICK_NOTE_ENTRY)
                                    monitor.rebaselineAfterLocalCrud()
                                }
                                .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
                            refresh()
                        })
                        MooButton(container.t("common.cancel"), onClick = { session.dialogMode = ""; session.dialogTarget = ""; refresh() })
                    } else {
                    MooButton(
                        dialogPrimaryLabel,
                        prominent = true,
                        enabled = dialogCanSubmit,
                        onClick = {
                        val name = session.dialogValue.trim()
                        if (name.isNotEmpty() || session.dialogMode == "move") {
                            val dialogMode = session.dialogMode
                            val parent = quickNoteVaultImportTargetDirectory(
                                session.vaultSelectedPath,
                                session.currentFile,
                                vaultItems,
                            )
                            runCatching {
                                when (dialogMode) {
                                    "folder" -> {
                                        if (!quickNoteVaultSaveIfNeeded(container, session, vault, monitor) { session.vaultConflict = it }) {
                                            error(session.error.ifBlank { container.t("quickNote.saveFailed") })
                                        }
                                        val path = VaultSelectionPath.resolveEntryPath(
                                            name,
                                            session.vaultSelectedPath,
                                            session.currentFile,
                                            vaultItems,
                                        )
                                        vault.createDirectory(path)
                                        session.vaultSelectedPath = path
                                        quickNoteOnEdt { session.editor.setText("", recordUndo = false) }
                                        session.currentFile = ""
                                        session.savedText = ""
                                        session.metadata = NoteMetadata.defaults("Untitled")
                                        session.savedMetadata = session.metadata
                                    }
                                    "rename" -> {
                                        val target = session.dialogTarget.ifBlank { session.currentFile }
                                        if (VaultMove.moveAffectsOpenPath(session.currentFile, target) &&
                                            !quickNoteVaultSaveIfNeeded(container, session, vault, monitor) { session.vaultConflict = it }
                                        ) {
                                            error(session.error.ifBlank { container.t("quickNote.saveFailed") })
                                        }
                                        val next = vault.rename(target, name)
                                        val (file, selected) = VaultMove.retargetVaultPaths(
                                            session.currentFile,
                                            session.vaultSelectedPath,
                                            target,
                                            next,
                                        )
                                        session.currentFile = file
                                        session.vaultSelectedPath = selected
                                    }
                                    "move" -> {
                                        val target = session.dialogTarget.ifBlank { session.currentFile }
                                        if (VaultMove.moveAffectsOpenPath(session.currentFile, target) &&
                                            !quickNoteVaultSaveIfNeeded(container, session, vault, monitor) { session.vaultConflict = it }
                                        ) {
                                            error(session.error.ifBlank { container.t("quickNote.saveFailed") })
                                        }
                                        val dest = session.dialogValue.trim()
                                        val next = vault.move(target, dest)
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
                                        if (!quickNoteVaultSaveIfNeeded(container, session, vault, monitor) { session.vaultConflict = it }) {
                                            error(session.error.ifBlank { container.t("quickNote.saveFailed") })
                                        }
                                        val created = vault.createNote(
                                            title = name,
                                            parentPath = parent,
                                            fontName = settings.editor.quickNoteFontName,
                                            fontSize = settings.editor.quickNoteFontSize,
                                            lineWrap = EditorSettingsLiveApply.newQuickNoteLineWrap(settings.editor.softWrap),
                                        )
                                        quickNoteOpenVaultFile(session, vault, created.relativePath)
                                    }
                                }
                            }.onSuccess {
                                VaultGitCheckpointMessages.quickNoteDialogMode(dialogMode)?.let {
                                    container.recordVaultActivity(it)
                                }
                                monitor.rebaselineAfterLocalCrud()
                                session.dialogMode = ""
                                session.dialogTarget = ""
                                session.error = ""
                                session.notice = container.t("quickNote.saved")
                            }.onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
                            refresh()
                        }
                    })
                    MooButton(container.t("common.cancel"), onClick = { session.dialogMode = ""; session.dialogTarget = ""; refresh() })
                    }
                }
            }
        }
    }
    val documentInfoPath = session.documentInfoPath
    if (documentInfoPath.isNotBlank()) {
        val infoNote = remember(documentInfoPath, revision, tick) {
            runCatching { vault.readNote(documentInfoPath) }.getOrNull()
        }
        if (infoNote != null) {
            QuickNoteDocumentInfoDialog(
                container = container,
                note = infoNote,
                onDismiss = { session.documentInfoPath = ""; refresh() },
            )
        }
    }
    if (session.gitDialogOpen) {
        VaultGitDialog(
            container = container,
            title = container.t("quickNote.git.title"),
            defaultMessage = container.t("quickNote.git.defaultMessage"),
            root = vault.root(),
            onDismiss = { session.gitDialogOpen = false; gitCountRev++; refresh() },
            onFlush = {
                quickNoteGitFlushBeforeAction(container, session, vault, monitor) { session.vaultConflict = it }
            },
            onVaultRefresh = {
                container.notifyQuickNoteVaultTreeChanged()
                val current = session.currentFile
                if (current.isBlank()) refresh()
                else handleQuickNoteVaultChange(container, session, vault, listOf(current), { session.vaultConflict = it }, { refresh() })
            },
            onGitStatusChanged = {
                gitCountRev++
                refresh()
            },
        )
    }
    QuickNoteVaultConflictOverlay(container, session, vault, monitor, onRefresh = { refresh() })
    if (session.historyOpen) {
        HistoryBrowser(
            container = container,
            toolId = ToolId.QuickNote.id,
            title = container.t("quickNote.history"),
            onRestore = { item ->
                if (!quickNoteVaultSaveIfNeeded(container, session, vault, monitor) { session.vaultConflict = it }) {
                    refresh()
                    return@HistoryBrowser
                }
                QuickNoteHistoryRestore.apply(session, item)
                quickNoteOnEdt { session.editor.setText(QuickNoteHistoryRestore.editorText(item), recordUndo = true) }
                session.historyOpen = false
                refresh()
            },
            onDismiss = { session.historyOpen = false; refresh() }
        )
    }
}

@Composable
private fun QuickNoteEditor(
    container: AppContainer,
    session: QuickNoteSession,
    settings: AppSettings,
    vault: NoteVault,
    shortcuts: EditorAppShortcuts
) {
    val fontName = DocumentFormatEngine.editorFont(session.metadata.fontName.ifBlank { settings.editor.quickNoteFontName })
    val fontSize = if (session.metadata.fontSize > 0) session.metadata.fontSize else settings.editor.quickNoteFontSize
    val dropHandler: (List<File>) -> Boolean = { files ->
        handleDroppedFiles(container, session, vault, files)
        true
    }
    if (session.currentFile.isBlank() && session.editor.text == QuickNoteSession.SAMPLE) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(container.t("quickNote.select"), color = MooTheme.colors.textSecondary)
            EditorHost(
                buffer = session.editor,
                dark = MooTheme.dark,
                fontName = fontName,
                fontSize = fontSize,
                wrap = session.wrap,
                modifier = Modifier.fillMaxSize().mooEditorFrame(),
                columnEditing = true,
                columnDragWithoutAlt = EditorColumnEditPresentation.columnDragWithoutAlt(session.columnLatch),
                lineSpacing = session.metadata.lineSpacing,
                shortcuts = shortcuts,
                onFilesDropped = dropHandler
            )
        }
    } else {
        EditorHost(
            buffer = session.editor,
            dark = MooTheme.dark,
            fontName = fontName,
            fontSize = fontSize,
            wrap = session.wrap,
            modifier = Modifier.fillMaxSize().mooEditorFrame(),
            columnEditing = true,
            columnDragWithoutAlt = EditorColumnEditPresentation.columnDragWithoutAlt(session.columnLatch),
            lineSpacing = session.metadata.lineSpacing,
            shortcuts = shortcuts,
            onFilesDropped = dropHandler
        )
    }
}

private fun handleDroppedFiles(
    container: AppContainer,
    session: QuickNoteSession,
    vault: NoteVault,
    files: List<File>
): Boolean {
    if (files.isEmpty()) return false
    files.forEach { file ->
        val extension = file.extension.lowercase()
        if (extension in NoteAttachmentEngine.extensions) {
            insertAttachmentBytes(
                container,
                session,
                vault,
                Files.readAllBytes(file.toPath()),
                extension,
                VaultGitCheckpointMessages.ADD_QUICK_NOTE_ATTACHMENT,
            )
        } else {
            val text = runCatching { file.readText(Charsets.UTF_8) }.getOrNull() ?: return@forEach
            val area = session.editor.area
            quickNoteOnEdt {
                val start = area.selectionStart
                val end = area.selectionEnd
                session.editor.replaceRange(start, end, text)
            }
            session.notice = container.t("quickNote.drop.inserted")
        }
    }
    container.recordVaultActivity(VaultGitCheckpointMessages.UPDATE_QUICK_NOTE)
    return true
}

private fun pasteClipboardImage(container: AppContainer, session: QuickNoteSession, vault: NoteVault) {
    val image = readClipboardImage()
    if (image == null) {
        session.error = container.t("quickNote.image.clipboardEmpty")
        session.notice = ""
        return
    }
    insertAttachmentBytes(
        container,
        session,
        vault,
        pngBytes(image),
        "png",
        VaultGitCheckpointMessages.PASTE_QUICK_NOTE_ATTACHMENT,
    )
}

private fun insertImageFile(container: AppContainer, session: QuickNoteSession, vault: NoteVault) {
    val file = chooseFile(container, save = false) ?: return
    val extension = file.extension
    runCatching {
        check(NoteAttachmentEngine.extensions.contains(extension.lowercase()) || extension.lowercase() == "jpeg") {
            container.t("quickNote.image.unsupported")
        }
        val bytes = Files.readAllBytes(file.toPath())
        insertAttachmentBytes(
            container,
            session,
            vault,
            bytes,
            extension,
            VaultGitCheckpointMessages.ADD_QUICK_NOTE_ATTACHMENT,
        )
    }.onFailure {
        session.error = it.message ?: container.t("quickNote.saveFailed")
        session.notice = ""
    }
}

private fun insertAttachmentBytes(
    container: AppContainer,
    session: QuickNoteSession,
    vault: NoteVault,
    bytes: ByteArray,
    extension: String,
    checkpointMessage: String,
) {
    runCatching { NoteAttachmentEngine.store(vault, bytes, extension) }
        .onSuccess { stored ->
            container.recordVaultActivity(checkpointMessage)
            val area = session.editor.area
            val insertion = NoteAttachmentEngine.prepareInsertion(
                session.editor.text,
                area.selectionStart,
                area.selectionEnd,
                stored.markdown
            )
            quickNoteOnEdt { session.editor.replaceRange(insertion.start, insertion.end, insertion.text) }
            session.error = ""
            session.notice = container.t("quickNote.image.inserted")
        }
        .onFailure {
            session.error = it.message ?: container.t("quickNote.saveFailed")
            session.notice = ""
        }
}

private fun pngBytes(image: BufferedImage): ByteArray {
    val output = ByteArrayOutputStream()
    check(ImageIO.write(image, "png", output)) { "PNG writer is unavailable" }
    val bytes = output.toByteArray()
    check(bytes.isNotEmpty()) { "PNG writer is unavailable" }
    return bytes
}

private fun readClipboardImage(): BufferedImage? = runCatching {
    val contents = Toolkit.getDefaultToolkit().systemClipboard.getContents(null) ?: return null
    if (!contents.isDataFlavorSupported(DataFlavor.imageFlavor)) return null
    when (val data = contents.getTransferData(DataFlavor.imageFlavor)) {
        is BufferedImage -> data
        is AwtImage -> {
            val buffered = BufferedImage(data.getWidth(null), data.getHeight(null), BufferedImage.TYPE_INT_ARGB)
            val graphics = buffered.createGraphics()
            graphics.drawImage(data, 0, 0, null)
            graphics.dispose()
            buffered
        }
        else -> null
    }
}.getOrNull()

private fun applyReplace(session: QuickNoteSession, action: QuickReplaceAction) {
    val area = session.editor.area
    val start = area.selectionStart
    val end = area.selectionEnd
    quickNoteOnEdt {
        if (end > start) {
            val source = session.editor.text.substring(start, end)
            session.editor.replaceRange(start, end, QuickReplaceEngine.run(source, action))
        } else {
            session.editor.setText(QuickReplaceEngine.run(session.editor.text, action), recordUndo = true)
        }
    }
}

private fun runQuickNoteVaultImport(
    container: AppContainer,
    session: QuickNoteSession,
    vault: NoteVault,
    monitor: VaultRevisionMonitor?,
    vaultItems: List<VaultEntry>,
    file: File,
    onConflict: (VaultConflictState) -> Unit,
): Result<Unit> {
    if (!quickNoteVaultSaveIfNeeded(container, session, vault, monitor, onConflict)) {
        return Result.failure(IllegalStateException(container.t("quickNote.saveFailed")))
    }
    val dir = quickNoteVaultImportTargetDirectory(session.vaultSelectedPath, session.currentFile, vaultItems)
    return runCatching {
        val relative = importQuickNoteVaultFile(vault, file, dir)
        persistToolsExportDirectory(container, file)
        quickNoteOpenVaultFile(session, vault, relative)
        monitor.rebaselineAfterLocalCrud()
        container.recordVaultActivity(VaultGitCheckpointMessages.UPDATE_QUICK_NOTE)
        session.notice = container.t("json.notice.imported")
        container.toastSuccess(container.t("json.notice.imported"))
    }
}

private fun handleQuickNoteVaultChange(
    container: AppContainer,
    session: QuickNoteSession,
    vault: NoteVault,
    paths: List<String>,
    onConflict: (VaultConflictState) -> Unit,
    refresh: () -> Unit
) {
    refresh()
    if (quickNoteApplyExternalChange(container, session, vault, paths, onConflict)) {
        refresh()
    }
}

@Composable
private fun QuickNoteDocumentInfoDialog(
    container: AppContainer,
    note: NoteDocument,
    onDismiss: () -> Unit,
) {
    val colors = MooTheme.colors
    val (chars, words, lines) = remember(note.relativePath, note.content) { QuickReplaceEngine.stats(note.content) }
    val rows = listOf(
        container.t("quickNote.path") to note.relativePath,
        container.t("quickNote.created") to formatQuickNoteTimestamp(note.metadata.createdAt),
        container.t("quickNote.modified") to formatQuickNoteTimestamp(note.metadata.modifiedAt),
        container.t("quickNote.lines") to lines.toString(),
        container.t("quickNote.words") to words.toString(),
        container.t("quickNote.characters") to chars.toString(),
    )
    MooOverlay(onDismiss = onDismiss) {
        Column(
            Modifier.width(420.dp).mooDialogSurface().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(container.t("quickNote.info"), color = colors.textPrimary, fontSize = 14.sp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(label, color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.width(88.dp))
                        Text(value, color = colors.textPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.close"), onClick = onDismiss)
            }
        }
    }
}

internal fun quickNoteRenameDefault(
    relativePath: String,
    isDirectory: Boolean,
    metadataTitle: String? = null,
): String {
    val path = relativePath.trim()
    if (isDirectory) {
        val leaf = path.substringAfterLast('/')
        return if (leaf.isBlank() || leaf == path) path else leaf
    }
    return metadataTitle?.trim()?.takeIf { it.isNotEmpty() }
        ?: path.substringAfterLast('/').substringBeforeLast('.')
}

internal fun quickNoteMoveFolderOptions(
    entries: List<VaultEntry>,
    sourcePath: String,
    rootLabel: String,
): List<Pair<String, String>> = vaultMoveFolderOptions(entries, sourcePath, rootLabel)

private fun duplicateQuickNoteEntry(
    container: AppContainer,
    session: QuickNoteSession,
    vault: NoteVault,
    monitor: VaultRevisionMonitor?,
    sourcePath: String,
    onConflict: (VaultConflictState) -> Unit,
): Result<NoteDocument> {
    if (!quickNoteVaultSaveIfNeeded(container, session, vault, monitor, onConflict)) {
        return Result.failure(IllegalStateException(container.t("quickNote.saveFailed")))
    }
    return runCatching {
        vault.duplicate(sourcePath).also { copy ->
            quickNoteOpenVaultFile(session, vault, copy.relativePath)
            monitor.rebaselineAfterLocalCrud()
            container.recordVaultActivity(VaultGitCheckpointMessages.DUPLICATE_QUICK_NOTE)
        }
    }
}

internal fun formatQuickNoteTimestamp(raw: String): String {
    if (raw.isBlank()) return ""
    return runCatching {
        Instant.parse(raw).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }.getOrDefault(raw)
}

private fun applyListPrefix(container: AppContainer, session: QuickNoteSession, prefix: NoteListEngine.Prefix) {
    val area = session.editor.area
    val result = NoteListEngine.prefixSelectedLines(session.editor.text, area.selectionStart, area.selectionEnd, prefix)
    quickNoteOnEdt {
        session.editor.setText(result.text, recordUndo = true)
        session.editor.select(result.selectionStart, result.selectionEnd)
    }
    session.notice = container.t(if (prefix == NoteListEngine.Prefix.Bullet) "quickNote.bulletList" else "quickNote.numberedList")
    session.error = ""
}

private fun formatCurrentNote(container: AppContainer, session: QuickNoteSession) {
    val syntax = session.metadata.syntax.ifBlank {
        NoteFrontmatter.syntaxForExtension(session.currentFile.substringAfterLast('.'))
    }
    runCatching {
        DocumentFormatEngine.format(session.editor.text, syntax, container.settings.value.editor.sqlDialect)
    }.onSuccess { formatted ->
        if (formatted != session.editor.text) {
            quickNoteOnEdt { session.editor.setText(formatted, recordUndo = true) }
        }
        session.notice = container.t("quickNote.formatted")
        session.error = ""
    }.onFailure {
        session.error = it.message ?: container.t("quickNote.formatFailed")
        session.notice = ""
    }
}

private val NoteSyntaxOptions = listOf(
    "text/plain" to "Text",
    "text/markdown" to "Markdown",
    "application/json" to "JSON",
    "text/java" to "Java",
    "text/javascript" to "JavaScript",
    "text/typescript" to "TypeScript",
    "text/python" to "Python",
    "text/xml" to "XML",
    "text/yaml" to "YAML",
    "text/sql" to "SQL"
)

private val quickNoteVaultDropExtensions = setOf("md", "markdown", "txt")

private fun handleQuickNoteVaultTreeFileDrop(
    container: AppContainer,
    session: QuickNoteSession,
    vault: NoteVault,
    monitor: VaultRevisionMonitor?,
    files: List<File>,
    targetDirectory: String,
    onConflict: (VaultConflictState) -> Unit,
    onChanged: () -> Unit,
): Boolean {
    val imports = files.filter { it.isFile && it.extension.lowercase() in quickNoteVaultDropExtensions }
    if (imports.isEmpty()) return false
    if (!quickNoteVaultSaveIfNeeded(container, session, vault, monitor, onConflict)) {
        onChanged()
        return false
    }
    var lastRelative: String? = null
    var importedCount = 0
    imports.forEach { file ->
        runCatching {
            val relative = importQuickNoteVaultFile(vault, file, targetDirectory)
            lastRelative = relative
            importedCount++
        }.onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
    }
    if (importedCount == 0) return false
    monitor.rebaselineAfterLocalCrud()
    if (importedCount == 1 && lastRelative != null) {
        quickNoteOpenVaultFile(session, vault, lastRelative!!)
        container.recordVaultActivity(VaultGitCheckpointMessages.UPDATE_QUICK_NOTE)
    } else if (importedCount > 1) {
        container.recordVaultActivity(VaultGitCheckpointMessages.UPDATE_QUICK_NOTE)
    }
    session.notice = container.t("json.notice.imported")
    container.toastSuccess(container.t("json.notice.imported"))
    onChanged()
    return true
}

private fun chooseFile(container: AppContainer, save: Boolean, defaultFileName: String = ""): File? =
    chooseFileWithExportDirectory(
        container,
        save,
        title = if (save) "Export" else "Import",
        defaultFileName = defaultFileName,
    )

private fun quickNoteWorkspacePaneKey(showVault: Boolean, showReplace: Boolean): String {
    val tree = if (showVault) "tree" else "no-tree"
    val replace = if (showReplace) "replace" else "no-replace"
    return "quick-note-$tree-$replace"
}

private fun quickNoteWorkspacePane(
    settings: AppSettings,
    paneKey: String,
    index: Int,
    default: Float,
    min: Float,
    max: Float
): Float {
    if (settings.layout.paneSizes.containsKey(paneKey)) {
        return settings.layout.pane(paneKey, index, default, min, max)
    }
    return settings.layout.pane(ToolId.QuickNote.id, index, default, min, max)
}

@Composable
private fun QuickNoteFindBar(
    container: AppContainer,
    session: QuickNoteSession,
    onChanged: () -> Unit,
) {
    val matches = FindReplace.findAll(session.editor.text, session.findQuery, session.findOptions)
    val colors = MooTheme.colors
    val findFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        findFocus.requestFocus()
    }
    fun closeFind() {
        session.findOpen = false
        session.findReplacedCount = 0
        onChanged()
    }
    fun jump(forward: Boolean) {
        if (session.findQuery.isBlank()) return
        val match = RstaFindNavigation.jump(session.editor, session.findQuery, session.findOptions, forward)
        if (match == null) {
            container.toastFindNoMatches()
        } else {
            quickNoteOnEdt { session.editor.select(match.start, match.end) }
        }
        onChanged()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .mooFindBarBackground()
            .horizontalScroll(rememberScrollState())
            .onFindBarRowKeys(
                onPrevious = { jump(false) },
                onNext = { jump(true) },
                onClose = ::closeFind,
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MooTextField(
            session.findQuery,
            {
                session.findQuery = it
                session.findReplacedCount = 0
                onChanged()
            },
            modifier = Modifier.width(200.dp),
            placeholder = container.t("quickNote.findPlaceholder"),
            fieldModifier = Modifier
                .focusRequester(findFocus)
                .onFindQueryEnterKey { jump(true) },
        )
        MooButton(
            container.t("find.find"),
            enabled = session.findQuery.isNotBlank(),
            onClick = { jump(true) },
        )
        MooTextField(
            session.replaceText,
            { session.replaceText = it; onChanged() },
            modifier = Modifier.width(160.dp),
            placeholder = container.t("quickNote.replacePlaceholder"),
        )
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
            color = colors.textSecondary,
            fontSize = 12.sp,
        )
        MooButton(container.t("find.previous"), onClick = { jump(false) })
        MooButton(container.t("find.next"), onClick = { jump(true) })
        MooButton(container.t("find.replace"), onClick = {
            quickNoteOnEdt {
                if (!RstaFindNavigation.replaceAndSelectNext(session.editor, session.findQuery, session.replaceText, session.findOptions)) {
                    container.toastFindNoMatches()
                } else {
                    session.findReplacedCount += 1
                    session.notice = quickNoteNoticeOnUserDocumentChange(
                        session.columnLatch,
                        container.t("quickNote.columnEdit.hint"),
                    )
                }
                onChanged()
            }
        })
        MooButton(container.t("find.replaceAll"), onClick = {
            val (next, count) = FindReplace.replaceAll(session.editor.text, session.findQuery, session.replaceText, session.findOptions)
            if (count == 0) {
                container.toastFindNoMatches()
            } else {
                quickNoteOnEdt { session.editor.setText(next, recordUndo = true) }
                session.findReplacedCount = count
                session.notice = quickNoteNoticeOnUserDocumentChange(
                    session.columnLatch,
                    container.t("quickNote.columnEdit.hint"),
                )
            }
            onChanged()
        })
        Text(
            "${container.t("find.replacedPrefix")} ${session.findReplacedCount}",
            color = colors.textSecondary,
            fontSize = 12.sp,
        )
        MooButton(container.t("common.close"), onClick = ::closeFind)
    }
}

