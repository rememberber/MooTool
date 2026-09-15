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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.DocumentFormatEngine
import com.rememberber.mootool.next.compose.domain.FindReplace
import com.rememberber.mootool.next.compose.domain.NoteAttachmentEngine
import com.rememberber.mootool.next.compose.domain.NoteColors
import com.rememberber.mootool.next.compose.domain.NoteFrontmatter
import com.rememberber.mootool.next.compose.domain.NoteListEngine
import com.rememberber.mootool.next.compose.domain.NoteMetadata
import com.rememberber.mootool.next.compose.domain.QuickReplaceAction
import com.rememberber.mootool.next.compose.domain.QuickReplaceEngine
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
import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.QuickNoteSession
import com.rememberber.mootool.next.compose.storage.NoteVault
import com.rememberber.mootool.next.compose.ui.components.FontSelect
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.domain.VaultMove
import com.rememberber.mootool.next.compose.ui.components.VaultContextAction
import com.rememberber.mootool.next.compose.ui.components.VaultContextId
import com.rememberber.mootool.next.compose.ui.components.VaultSortMenu
import com.rememberber.mootool.next.compose.ui.components.VaultTreeList
import com.rememberber.mootool.next.compose.ui.components.VerticalPaneHandle
import com.rememberber.mootool.next.compose.ui.components.setPaneSize
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Image as AwtImage
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

@Composable
fun QuickNoteScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.quickNoteSession(container.settings.value.editor.softWrap) }
    val settings by container.settings.collectAsState()
    val revision by container.sessionManager.revision.collectAsState()
    val vault = remember(settings.vault.quickNotePath) { container.noteVault() }
    var tick by remember { mutableStateOf(0L) }
    var filterRev by remember { mutableStateOf(0) }
    var snapshot by remember { mutableStateOf(vault.snapshot(settings.vault.hideGitignoredFiles)) }
    fun refresh() {
        tick += 1
        container.sessionManager.bump()
        container.sessionManager.persistQuickNote()
    }
    fun persistFilter() {
        filterRev += 1
        container.sessionManager.bump()
        container.sessionManager.persistQuickNote()
    }
    var gitOpen by remember { mutableStateOf(false) }
    var conflict by remember { mutableStateOf<VaultConflictState?>(null) }
    var monitor by remember { mutableStateOf<VaultRevisionMonitor?>(null) }
    val colors = MooTheme.colors
    val noteShortcuts = EditorAppShortcuts(
        onFind = {
            session.findOpen = true
            refresh()
        },
        onFormat = {
            formatCurrentNote(container, session)
            refresh()
        },
        onSave = {
            saveCurrent(container, session, vault, monitor) { conflict = it }
            refresh()
        }
    )
    DisposableEffect(vault.root()) {
        val next = VaultRevisionMonitor(vault.root(), ignoreAttachments = true) { paths ->
            SwingUtilities.invokeLater {
                handleQuickNoteVaultChange(container, session, vault, paths, { conflict = it }, { refresh() })
            }
        }
        next.start()
        monitor = next
        onDispose {
            next.close()
            if (monitor === next) monitor = null
        }
    }
    LaunchedEffect(tick, settings.vault.quickNotePath, settings.vault.hideGitignoredFiles) {
        snapshot = withContext(Dispatchers.IO) { vault.snapshot(settings.vault.hideGitignoredFiles) }
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
            if (event.type == KeyEventType.KeyDown && event.key == Key.S && (event.isMetaPressed || event.isCtrlPressed)) {
                saveCurrent(container, session, vault, monitor) { conflict = it }
                refresh()
                true
            } else if (event.type == KeyEventType.KeyDown && event.key == Key.F && event.isShiftPressed && (event.isMetaPressed || event.isCtrlPressed)) {
                formatCurrentNote(container, session)
                refresh()
                true
            } else if (event.type == KeyEventType.KeyDown && event.key == Key.F && (event.isMetaPressed || event.isCtrlPressed)) {
                session.findOpen = true
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
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).background(colors.toolbarBrush()).padding(horizontal = 8.dp).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(container.t("quickNote.title"), color = colors.textPrimary, fontSize = 16.sp)
            MooButton(container.t("quickNote.save"), primary = true, onClick = {
                saveCurrent(container, session, vault, monitor) { conflict = it }
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
            Box {
                MooButton(
                    container.t("quickNote.color") + " · " + NoteColors.normalize(session.metadata.color),
                    onClick = { colorOpen = true }
                )
                DropdownMenu(expanded = colorOpen, onDismissRequest = { colorOpen = false }) {
                    NoteColors.swatches.forEach { (id, argb) ->
                        DropdownMenuItem(onClick = {
                            session.metadata = session.metadata.copy(color = id)
                            colorOpen = false
                            refresh()
                        }) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(Color(argb)))
                                Text(id, color = if (NoteColors.normalize(session.metadata.color) == id) MooTheme.colors.accent else MooTheme.colors.textPrimary)
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
                DropdownMenu(expanded = syntaxOpen, onDismissRequest = { syntaxOpen = false }) {
                    NoteSyntaxOptions.forEach { (id, label) ->
                        DropdownMenuItem(onClick = {
                            session.metadata = session.metadata.copy(syntax = id)
                            session.editor.syntax = DocumentFormatEngine.rstaSyntax(id)
                            syntaxOpen = false
                            refresh()
                        }) {
                            Text(label)
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
                session.notice = if (session.columnLatch) {
                    if (session.wrap) container.t("quickNote.columnEdit.wrap") else container.t("quickNote.columnEdit.hint")
                } else ""
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
            MooButton(container.t("quickNote.find"), onClick = { session.findOpen = !session.findOpen; refresh() })
            MooButton(container.t("json.action.copy"), onClick = {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(session.editor.text), null)
                session.notice = container.t("json.notice.copied")
                refresh()
            })
            if (!overflow) {
            MooButton(container.t("json.action.import"), onClick = {
                chooseFile(false)?.let { file ->
                    runCatching { vault.importFile(file.toPath()) }
                        .onSuccess { imported ->
                            openFile(session, vault, imported.fileName.toString().replace('\\', '/'))
                            session.notice = container.t("json.notice.imported")
                        }
                        .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
                    refresh()
                }
            })
            MooButton(container.t("quickNote.export"), enabled = session.currentFile.isNotBlank(), onClick = {
                chooseFile(true)?.let { file ->
                    runCatching { vault.exportFile(session.currentFile, file.toPath()) }
                        .onSuccess { session.notice = container.t("json.notice.exported") }
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
            MooButton(container.t("git.action"), onClick = { gitOpen = true })
            } else {
                Box {
                    MooButton(container.t("json.action.overflow"), primary = moreOpen, onClick = { moreOpen = true })
                    DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                        listOf(12, 13, 14, 16, 18).forEach { size ->
                            DropdownMenuItem(onClick = {
                                session.metadata = session.metadata.copy(fontSize = size)
                                moreOpen = false
                                refresh()
                            }) {
                                Text("${container.t("quickNote.fontSize")} $size")
                            }
                        }
                        listOf(1.0, 1.2, 1.4, 1.6, 1.8, 2.0).forEach { spacing ->
                            DropdownMenuItem(onClick = {
                                session.metadata = session.metadata.copy(lineSpacing = spacing)
                                moreOpen = false
                                refresh()
                            }) {
                                Text("${container.t("quickNote.lineSpacing")} ${"%.1f".format(java.util.Locale.US, spacing)}×")
                            }
                        }
                        DropdownMenuItem(onClick = {
                            moreOpen = false
                            session.columnLatch = !session.columnLatch
                            session.notice = if (session.columnLatch) {
                                if (session.wrap) container.t("quickNote.columnEdit.wrap") else container.t("quickNote.columnEdit.hint")
                            } else ""
                            refresh()
                        }) {
                            Text(container.t("quickNote.columnEdit"))
                        }
                        DropdownMenuItem(onClick = {
                            moreOpen = false
                            pasteClipboardImage(container, session, vault)
                            refresh()
                        }) {
                            Text(container.t("quickNote.pasteImage"))
                        }
                        DropdownMenuItem(onClick = {
                            moreOpen = false
                            insertImageFile(container, session, vault)
                            refresh()
                        }) {
                            Text(container.t("quickNote.insertImage"))
                        }
                        DropdownMenuItem(onClick = {
                            moreOpen = false
                            chooseFile(false)?.let { file ->
                                runCatching { vault.importFile(file.toPath()) }
                                    .onSuccess { imported ->
                                        openFile(session, vault, imported.fileName.toString().replace('\\', '/'))
                                        session.notice = container.t("json.notice.imported")
                                    }
                                    .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
                                refresh()
                            }
                        }) {
                            Text(container.t("json.action.import"))
                        }
                        DropdownMenuItem(onClick = {
                            moreOpen = false
                            if (session.currentFile.isBlank()) return@DropdownMenuItem
                            chooseFile(true)?.let { file ->
                                runCatching { vault.exportFile(session.currentFile, file.toPath()) }
                                    .onSuccess { session.notice = container.t("json.notice.exported") }
                                    .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
                                refresh()
                            }
                        }) {
                            Text(container.t("quickNote.export"))
                        }
                        DropdownMenuItem(onClick = {
                            moreOpen = false
                            session.historyOpen = true
                            refresh()
                        }) {
                            Text(container.t("common.action.history"))
                        }
                        DropdownMenuItem(onClick = {
                            moreOpen = false
                            gitOpen = true
                        }) {
                            Text(container.t("git.action"))
                        }
                        if (!detached) {
                            DropdownMenuItem(onClick = {
                                moreOpen = false
                                container.sessionManager.detach(ToolId.QuickNote)
                            }) {
                                Text(container.t("app.tool.detach"))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (!detached && !overflow) MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.QuickNote) })
        }
        if (session.findOpen) {
            Row(
                modifier = Modifier.fillMaxWidth().background(colors.surfaceSubtle).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MooTextField(session.findQuery, { session.findQuery = it; refresh() }, placeholder = container.t("quickNote.findPlaceholder"), modifier = Modifier.weight(1f))
                MooTextField(session.replaceText, { session.replaceText = it; refresh() }, placeholder = container.t("quickNote.replacePlaceholder"), modifier = Modifier.weight(1f))
                MooButton(container.t("quickNote.replaceAll"), onClick = {
                    val (next, count) = FindReplace.replaceAll(session.editor.text, session.findQuery, session.replaceText, session.findOptions)
                    onEdt { session.editor.setText(next, recordUndo = true) }
                    session.notice = container.t("json.find.matches", mapOf("count" to count.toString()))
                    refresh()
                })
                MooButton(container.t("common.close"), onClick = { session.findOpen = false; refresh() })
            }
        }
        val vaultWidth = settings.layout.pane(ToolId.QuickNote.id, 0, 240f, 200f, 320f)
        val replaceWidth = settings.layout.pane(ToolId.QuickNote.id, 1, 240f, 240f, 340f)
        val showVault = LayoutPolicy.showVault(compact, session.compactAux)
        val showReplace = LayoutPolicy.showReplace(compact, session.compactAux, session.replaceOpen)
        Row(Modifier.weight(1f).fillMaxWidth()) {
            if (showVault) {
            Column(Modifier.width(vaultWidth.dp).fillMaxHeight().background(colors.sidebar).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    MooButton(container.t("quickNote.newNote"), onClick = { session.dialogMode = "note"; session.dialogValue = "note.md"; refresh() })
                    MooButton(container.t("quickNote.newFolder"), onClick = { session.dialogMode = "folder"; session.dialogValue = "folder"; refresh() })
                }
                MooButton(container.t("quickNote.openVault"), onClick = { container.openDirectory(vault.root()) })
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
                if (vaultItems.isEmpty()) {
                    Text(container.t("quickNote.empty"), color = colors.textSecondary, fontSize = 12.sp)
                } else {
                    VaultTreeList(
                        items = vaultItems.map { item ->
                            if (item.relativePath == session.currentFile) item.copy(color = session.metadata.color) else item
                        },
                        selectedPath = session.currentFile,
                        emptyLabel = container.t("quickNote.empty"),
                        onOpen = { item ->
                            if (!item.directory) {
                                if (saveIfNeeded(container, session, vault, monitor) { conflict = it }) {
                                    openFile(session, vault, item.relativePath)
                                }
                                refresh()
                            }
                        },
                        onMove = { from, to ->
                            runCatching { vault.move(from, to) }
                                .onSuccess { next ->
                                    session.currentFile = VaultMove.retargetAfterMove(session.currentFile, from, next)
                                    session.notice = container.t("vault.moved")
                                }
                                .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
                            refresh()
                        },
                        modifier = Modifier.weight(1f),
                        expandMode = settings.vault.quickNoteTreeExpandMode,
                        sort = session.vaultSort,
                        contextActions = listOf(
                            VaultContextAction(VaultContextId.Rename, container.t("quickNote.rename")),
                            VaultContextAction(VaultContextId.Move, container.t("quickNote.move")),
                            VaultContextAction(VaultContextId.Duplicate, container.t("quickNote.duplicate"), filesOnly = true),
                            VaultContextAction(VaultContextId.Export, container.t("quickNote.export"), filesOnly = true),
                            VaultContextAction(VaultContextId.Delete, container.t("json.vault.delete")),
                            VaultContextAction(VaultContextId.Reveal, container.t("vault.reveal")),
                            VaultContextAction(VaultContextId.Git, container.t("git.action"))
                        ),
                        onContextAction = { entry, id ->
                            when (id) {
                                VaultContextId.Rename -> {
                                    session.dialogMode = "rename"
                                    session.dialogTarget = entry.relativePath
                                    session.dialogValue = entry.name.substringBeforeLast('.')
                                }
                                VaultContextId.Move -> {
                                    session.dialogMode = "move"
                                    session.dialogTarget = entry.relativePath
                                    session.dialogValue = ""
                                }
                                VaultContextId.Duplicate -> {
                                    runCatching { vault.duplicate(entry.relativePath) }
                                        .onSuccess { copy ->
                                            openFile(session, vault, copy.relativePath)
                                            session.notice = container.t("quickNote.duplicated")
                                        }
                                        .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
                                }
                                VaultContextId.Export -> {
                                    if (entry.directory) return@VaultTreeList
                                    chooseFile(true)?.let { file ->
                                        runCatching { vault.exportFile(entry.relativePath, file.toPath()) }
                                            .onSuccess { session.notice = container.t("json.notice.exported") }
                                            .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
                                    }
                                }
                                VaultContextId.Delete -> {
                                    runCatching { vault.delete(entry.relativePath) }
                                        .onSuccess {
                                            if (session.currentFile == entry.relativePath || session.currentFile.startsWith("${entry.relativePath}/")) {
                                                session.currentFile = ""
                                                session.savedText = session.editor.text
                                            }
                                            session.notice = container.t("quickNote.deleted")
                                        }
                                        .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
                                }
                                VaultContextId.Reveal -> container.revealInFileManager(vault.resolve(entry.relativePath))
                                VaultContextId.Git -> gitOpen = true
                            }
                            refresh()
                        }
                    )
                }
                if (session.currentFile.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        MooButton(container.t("quickNote.rename"), onClick = {
                            session.dialogMode = "rename"
                            session.dialogTarget = session.currentFile
                            session.dialogValue = session.currentFile.substringAfterLast('/')
                            refresh()
                        })
                        MooButton(container.t("quickNote.duplicate"), onClick = {
                            runCatching { vault.duplicate(session.currentFile) }
                                .onSuccess { copy ->
                                    openFile(session, vault, copy.relativePath)
                                    session.notice = container.t("quickNote.duplicated")
                                }
                                .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
                            refresh()
                        })
                        MooButton(container.t("quickNote.move"), onClick = {
                            session.dialogMode = "move"
                            session.dialogTarget = session.currentFile
                            session.dialogValue = ""
                            refresh()
                        })
                        MooButton(container.t("json.vault.delete"), onClick = {
                            runCatching { vault.delete(session.currentFile) }
                                .onSuccess {
                                    session.currentFile = ""
                                    session.savedText = session.editor.text
                                    session.notice = container.t("quickNote.deleted")
                                }
                                .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
                            refresh()
                        })
                    }
                }
            }
            VerticalPaneHandle(
                onDelta = { container.setPaneSize(ToolId.QuickNote.id, 0, vaultWidth + it, 2) },
                onReset = { container.setPaneSize(ToolId.QuickNote.id, 0, 240f, 2) }
            )
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                val showEditor = session.viewMode != "preview"
                val showPreview = session.viewMode != "edit"
                val previewText = remember(revision) { session.editor.text }
                if (showEditor && showPreview) {
                    Row(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                    QuickNoteEditor(container, session, settings, vault, noteShortcuts)
                        }
                        Box(Modifier.width(1.dp).fillMaxHeight().background(colors.border))
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            MarkdownPreviewPane(
                                markdown = previewText,
                                vault = vault,
                                missingLabel = container.t("quickNote.image.missing"),
                                remoteLabel = container.t("quickNote.image.remote"),
                                unsafeLabel = container.t("quickNote.image.unsafe")
                            )
                        }
                    }
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
                    onDelta = { container.setPaneSize(ToolId.QuickNote.id, 1, replaceWidth + it, 2) },
                    onReset = { container.setPaneSize(ToolId.QuickNote.id, 1, 240f, 2) }
                )
                Column(
                    Modifier.width(replaceWidth.dp).fillMaxHeight().background(colors.surfaceSubtle).padding(10.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(container.t("quickNote.quickReplace"), color = colors.textPrimary, fontSize = 12.sp)
                    QuickReplaceEngine.actions.forEach { action ->
                        MooButton(container.t("quickNote.quick.${action.name.replaceFirstChar { it.lowercase() }}"), onClick = {
                            applyReplace(session, action)
                            session.notice = container.t("quickNote.quick.${action.name.replaceFirstChar { it.lowercase() }}")
                            refresh()
                        })
                    }
                }
            }
        }
        val (chars, words, lines) = remember(session.editor.revision, revision, tick) { QuickReplaceEngine.stats(session.editor.text) }
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.statusBar).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dirty = session.currentFile.isNotBlank() && session.editor.text != session.savedText
            Text(
                listOfNotNull(
                    session.currentFile.ifBlank { container.t("quickNote.select") },
                    if (dirty) container.t("quickNote.unsaved") else if (session.currentFile.isNotBlank()) container.t("quickNote.saved") else null,
                    if (conflict != null) container.t("vault.conflict.banner") else null,
                    if (EditorLimits.exceedsLargeDocument(session.editor.text)) container.t("editor.largeDocument") else null,
                    container.t("quickNote.stats", mapOf("chars" to chars.toString(), "words" to words.toString(), "lines" to lines.toString()))
                ).joinToString(" · "),
                color = colors.textSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.weight(1f))
            if (session.error.isNotBlank()) Text(session.error, color = colors.danger, fontSize = 12.sp)
            else Text(session.notice, color = colors.textSecondary, fontSize = 12.sp)
            if (detached) Text(" · detached", color = colors.textSecondary, fontSize = 12.sp)
        }
        }
    }

    if (session.dialogMode.isNotBlank()) {
        Dialog(onDismissRequest = { session.dialogMode = ""; session.dialogTarget = ""; refresh() }) {
            Column(
                Modifier.width(420.dp).background(colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    when (session.dialogMode) {
                        "folder" -> container.t("quickNote.dialog.createFolder")
                        "rename" -> container.t("quickNote.dialog.rename")
                        "move" -> container.t("quickNote.dialog.move")
                        else -> container.t("quickNote.dialog.createNote")
                    },
                    color = colors.textPrimary
                )
                MooTextField(session.dialogValue, { session.dialogValue = it; refresh() }, placeholder = container.t("quickNote.dialog.name"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MooButton(container.t("common.save"), primary = true, onClick = {
                        val name = session.dialogValue.trim()
                        if (name.isNotEmpty() || session.dialogMode == "move") {
                            runCatching {
                                when (session.dialogMode) {
                                    "folder" -> vault.createDirectory(name)
                                    "rename" -> {
                                        val target = session.dialogTarget.ifBlank { session.currentFile }
                                        val next = vault.rename(target, name)
                                        session.currentFile = VaultMove.retargetAfterMove(session.currentFile, target, next)
                                    }
                                    "move" -> {
                                        val target = session.dialogTarget.ifBlank { session.currentFile }
                                        val next = vault.move(target, name)
                                        session.currentFile = VaultMove.retargetAfterMove(session.currentFile, target, next)
                                    }
                                    else -> {
                                        saveIfNeeded(container, session, vault, monitor) { conflict = it }
                                        val created = vault.createNote(
                                            title = name.substringBeforeLast('.'),
                                            fontName = settings.editor.quickNoteFontName,
                                            fontSize = settings.editor.quickNoteFontSize,
                                            lineWrap = session.wrap
                                        )
                                        val body = session.editor.text.takeIf { it.isNotBlank() && it != QuickNoteSession.SAMPLE }.orEmpty()
                                        val saved = if (body.isNotEmpty()) vault.saveNote(created.relativePath, body, created.metadata) else created
                                        openFile(session, vault, saved.relativePath)
                                    }
                                }
                            }.onSuccess {
                                session.dialogMode = ""
                                session.dialogTarget = ""
                                session.error = ""
                                session.notice = container.t("quickNote.saved")
                            }.onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
                            refresh()
                        }
                    })
                    MooButton(container.t("common.close"), onClick = { session.dialogMode = ""; session.dialogTarget = ""; refresh() })
                }
            }
        }
    }
    if (gitOpen) {
        VaultGitDialog(
            container = container,
            title = container.t("quickNote.git.title"),
            defaultMessage = container.t("quickNote.git.defaultMessage"),
            root = vault.root(),
            onDismiss = { gitOpen = false },
            onFlush = {
                if (session.currentFile.isBlank()) {
                    if (session.editor.text.isNotBlank() && session.editor.text != QuickNoteSession.SAMPLE) {
                        container.t("git.flush.untitled")
                    } else {
                        null
                    }
                } else {
                    saveIfNeeded(container, session, vault, monitor) { conflict = it }
                    session.error.takeIf { it.isNotBlank() }
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
                    openFile(session, vault, pending.relativePath)
                }
                conflict = null
                session.error = ""
                session.notice = container.t("vault.conflict.reloaded")
                refresh()
            },
            onSaveCopy = {
                val copy = VaultConflictEngine.conflictCopyName(pending.relativePath, System.currentTimeMillis())
                runCatching { vault.write(copy, pending.editorText) }
                    .onSuccess {
                        monitor?.noteOwnWrite(copy, VaultConflictEngine.sha256Text(pending.editorText))
                        if (pending.deleted) {
                            session.currentFile = copy
                            session.savedText = pending.editorText
                        } else {
                            openFile(session, vault, pending.relativePath)
                        }
                        conflict = null
                        session.error = ""
                        session.notice = container.t("vault.conflict.savedCopy", mapOf("path" to copy))
                    }
                    .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
                refresh()
            },
            onKeep = { conflict = null; refresh() }
        )
    }
    if (session.historyOpen) {
        HistoryBrowser(
            container = container,
            toolId = ToolId.QuickNote.id,
            title = container.t("quickNote.history"),
            onRestore = { item ->
                onEdt { session.editor.setText(item.input, recordUndo = true) }
                session.historyOpen = false
                session.notice = container.t("json.notice.restored")
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
                modifier = Modifier.fillMaxSize(),
                columnEditing = true,
                columnDragWithoutAlt = session.columnLatch,
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
            columnEditing = true,
            columnDragWithoutAlt = session.columnLatch,
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
            insertAttachmentBytes(container, session, vault, Files.readAllBytes(file.toPath()), extension)
        } else {
            val text = runCatching { file.readText(Charsets.UTF_8) }.getOrNull() ?: return@forEach
            val area = session.editor.area
            onEdt {
                val start = area.selectionStart
                val end = area.selectionEnd
                session.editor.replaceRange(start, end, text)
            }
            session.notice = container.t("quickNote.drop.inserted")
        }
    }
    container.recordVaultActivity("Update Quick Note")
    return true
}

private fun pasteClipboardImage(container: AppContainer, session: QuickNoteSession, vault: NoteVault) {
    val image = readClipboardImage()
    if (image == null) {
        session.error = container.t("quickNote.image.clipboardEmpty")
        session.notice = ""
        return
    }
    insertAttachmentBytes(container, session, vault, pngBytes(image), "png")
}

private fun insertImageFile(container: AppContainer, session: QuickNoteSession, vault: NoteVault) {
    val file = chooseFile(false) ?: return
    val extension = file.extension
    runCatching {
        check(NoteAttachmentEngine.extensions.contains(extension.lowercase()) || extension.lowercase() == "jpeg") {
            container.t("quickNote.image.unsupported")
        }
        val bytes = Files.readAllBytes(file.toPath())
        insertAttachmentBytes(container, session, vault, bytes, extension)
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
    extension: String
) {
    runCatching { NoteAttachmentEngine.store(vault, bytes, extension) }
        .onSuccess { stored ->
            val area = session.editor.area
            val insertion = NoteAttachmentEngine.prepareInsertion(
                session.editor.text,
                area.selectionStart,
                area.selectionEnd,
                stored.markdown
            )
            onEdt { session.editor.replaceRange(insertion.start, insertion.end, insertion.text) }
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
    onEdt {
        if (end > start) {
            val source = session.editor.text.substring(start, end)
            session.editor.replaceRange(start, end, QuickReplaceEngine.run(source, action))
        } else {
            session.editor.setText(QuickReplaceEngine.run(session.editor.text, action), recordUndo = true)
        }
    }
}

private fun saveCurrent(
    container: AppContainer,
    session: QuickNoteSession,
    vault: NoteVault,
    monitor: VaultRevisionMonitor?,
    onConflict: (VaultConflictState) -> Unit
): Boolean {
    val name = session.currentFile.ifBlank { "note-${System.currentTimeMillis()}.md" }
    if (session.currentFile.isNotBlank()) {
        val disk = runCatching { vault.readOrNull(name) }.getOrNull()
        val diskBody = disk?.let { NoteFrontmatter.parse(it, name.substringAfterLast('/').substringBeforeLast('.')).content }
        if (!VaultConflictEngine.canOverwrite(session.savedText, diskBody, session.editor.text)) {
            onConflict(VaultConflictState(name, session.editor.text, diskBody, disk == null))
            session.error = container.t("vault.conflict.blocked")
            return false
        }
    }
    val previous = if (session.currentFile.isNotBlank()) runCatching { vault.readNote(session.currentFile).content }.getOrNull() else null
    val metadata = session.metadata.copy(
        title = session.metadata.title.ifBlank { name.substringAfterLast('/').substringBeforeLast('.') },
        syntax = session.metadata.syntax.ifBlank { NoteFrontmatter.syntaxForExtension(name.substringAfterLast('.')) },
        lineWrap = session.wrap,
        fontName = session.metadata.fontName.ifBlank { container.settings.value.editor.quickNoteFontName },
        fontSize = session.metadata.fontSize.takeIf { it > 0 } ?: container.settings.value.editor.quickNoteFontSize
    )
    return runCatching { vault.saveNote(name, session.editor.text, metadata) }
        .onSuccess { document ->
            val raw = vault.read(document.relativePath)
            monitor?.noteOwnWrite(document.relativePath, VaultConflictEngine.sha256Text(raw))
            session.currentFile = document.relativePath
            session.savedText = document.content
            session.metadata = document.metadata
            session.error = ""
            session.notice = container.t("quickNote.saved")
            container.history.save(ToolId.QuickNote.id, document.relativePath, document.relativePath, document.content.take(8_000), "")
            container.recordVaultActivity("Update Quick Note")
            previous?.let { old ->
                val removed = NoteAttachmentEngine.extractPaths(old) - NoteAttachmentEngine.extractPaths(document.content)
                removed.forEach { path -> runCatching { NoteAttachmentEngine.deleteIfUnreferenced(vault, path) } }
            }
        }
        .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
        .isSuccess
}

private fun saveIfNeeded(
    container: AppContainer,
    session: QuickNoteSession,
    vault: NoteVault,
    monitor: VaultRevisionMonitor?,
    onConflict: (VaultConflictState) -> Unit
): Boolean {
    if (session.currentFile.isNotBlank() && session.editor.text != session.savedText) {
        return saveCurrent(container, session, vault, monitor, onConflict)
    }
    return session.error.isBlank()
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
    val current = session.currentFile
    if (current.isBlank() || current !in paths) return
    val disk = runCatching { vault.readOrNull(current) }.getOrNull()
    val diskBody = disk?.let { NoteFrontmatter.parse(it, current.substringAfterLast('/').substringBeforeLast('.')).content }
    when (VaultConflictEngine.decide(current, current, session.editor.text, session.savedText, diskBody)) {
        VaultChangeKind.Reload -> {
            openFile(session, vault, current)
            session.notice = container.t("vault.conflict.reloaded")
            refresh()
        }
        VaultChangeKind.Deleted -> {
            session.currentFile = ""
            session.savedText = session.editor.text
            session.notice = container.t("vault.conflict.deleted")
            refresh()
        }
        VaultChangeKind.Conflict -> onConflict(VaultConflictState(current, session.editor.text, diskBody, disk == null))
        VaultChangeKind.Ignored, VaultChangeKind.TreeChanged -> Unit
    }
}

private fun openFile(session: QuickNoteSession, vault: NoteVault, relativePath: String) {
    val note = vault.readNote(relativePath)
    onEdt { session.editor.setText(note.content, recordUndo = false) }
    session.currentFile = note.relativePath
    session.savedText = note.content
    session.metadata = note.metadata
    session.wrap = note.metadata.lineWrap
    session.editor.syntax = DocumentFormatEngine.rstaSyntax(note.metadata.syntax.ifBlank { NoteFrontmatter.syntaxForExtension(relativePath.substringAfterLast('.')) })
    session.error = ""
}

private fun applyListPrefix(container: AppContainer, session: QuickNoteSession, prefix: NoteListEngine.Prefix) {
    val area = session.editor.area
    val result = NoteListEngine.prefixSelectedLines(session.editor.text, area.selectionStart, area.selectionEnd, prefix)
    onEdt {
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
            onEdt { session.editor.setText(formatted, recordUndo = true) }
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

private fun chooseFile(save: Boolean): File? {
    val dialog = FileDialog(null as Frame?, if (save) "Export" else "Import", if (save) FileDialog.SAVE else FileDialog.LOAD)
    dialog.isVisible = true
    val directory = dialog.directory ?: return null
    val file = dialog.file ?: return null
    return File(directory, file)
}

private fun onEdt(block: () -> Unit) {
    if (SwingUtilities.isEventDispatchThread()) block() else SwingUtilities.invokeLater(block)
}
