package com.rememberber.mootool.next.compose.features.quicknote

import androidx.compose.foundation.background
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.FindReplace
import com.rememberber.mootool.next.compose.domain.QuickReplaceAction
import com.rememberber.mootool.next.compose.domain.QuickReplaceEngine
import com.rememberber.mootool.next.compose.editor.EditorHost
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.QuickNoteSession
import com.rememberber.mootool.next.compose.storage.NoteVault
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import javax.swing.SwingUtilities

@Composable
fun QuickNoteScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.quickNoteSession() }
    val settings by container.settings.collectAsState()
    val revision by container.sessionManager.revision.collectAsState()
    val vault = remember(settings.vault.quickNotePath) { container.noteVault() }
    var tick by remember { mutableStateOf(0L) }
    fun refresh() {
        tick += 1
        container.sessionManager.bump()
        container.sessionManager.persistQuickNote()
    }
    var vaultItems by remember { mutableStateOf(vault.list(session.vaultQuery)) }
    var historyItems by remember { mutableStateOf(emptyList<HistoryRecord>()) }
    val colors = MooTheme.colors
    LaunchedEffect(session.vaultQuery, tick, settings.vault.quickNotePath) {
        vaultItems = vault.list(session.vaultQuery)
    }
    LaunchedEffect(session.historyOpen, tick) {
        if (session.historyOpen) historyItems = container.history.list(ToolId.QuickNote.id)
    }

    Column(
        Modifier.fillMaxSize().background(colors.workspace).onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.S && (event.isMetaPressed || event.isCtrlPressed)) {
                saveCurrent(container, session, vault)
                refresh()
                true
            } else false
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp).background(colors.toolbar).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(container.t("quickNote.title"), color = colors.textPrimary, fontSize = 16.sp)
            MooButton(container.t("quickNote.save"), primary = true, onClick = {
                saveCurrent(container, session, vault)
                refresh()
            })
            MooButton(if (session.wrap) container.t("quickNote.wrap") else container.t("json.action.nowrap"), onClick = {
                session.wrap = !session.wrap
                refresh()
            })
            MooButton(container.t("quickNote.find"), onClick = { session.findOpen = !session.findOpen; refresh() })
            MooButton(container.t("json.action.copy"), onClick = {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(session.editor.text), null)
                session.notice = container.t("json.notice.copied")
                refresh()
            })
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
            MooButton(container.t("quickNote.quickReplace"), primary = session.replaceOpen, onClick = {
                session.replaceOpen = !session.replaceOpen
                refresh()
            })
            MooButton(container.t("common.action.history"), onClick = { session.historyOpen = true; refresh() })
            Spacer(Modifier.weight(1f))
            if (!detached) MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.QuickNote) })
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
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.width(240.dp).fillMaxHeight().background(colors.sidebar).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(container.t("quickNote.vault"), color = colors.textPrimary, fontSize = 12.sp)
                MooTextField(session.vaultQuery, { session.vaultQuery = it; refresh() }, placeholder = container.t("app.search.placeholder"))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MooButton(container.t("quickNote.newNote"), onClick = { session.dialogMode = "note"; session.dialogValue = "note.md"; refresh() })
                    MooButton(container.t("quickNote.newFolder"), onClick = { session.dialogMode = "folder"; session.dialogValue = "folder"; refresh() })
                }
                MooButton(container.t("quickNote.openVault"), onClick = { container.openDirectory(vault.root()) })
                if (vaultItems.isEmpty()) {
                    Text(container.t("quickNote.empty"), color = colors.textSecondary, fontSize = 12.sp)
                } else {
                    LazyColumn(Modifier.weight(1f)) {
                        items(vaultItems) { item ->
                            Text(
                                (if (item.directory) "▸ " else "") + item.relativePath,
                                color = if (item.relativePath == session.currentFile) colors.accent else colors.textPrimary,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable {
                                    if (!item.directory) {
                                        saveIfNeeded(container, session, vault)
                                        openFile(session, vault, item.relativePath)
                                        refresh()
                                    }
                                }.padding(6.dp)
                            )
                        }
                    }
                }
                if (session.currentFile.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        MooButton(container.t("quickNote.rename"), onClick = {
                            session.dialogMode = "rename"
                            session.dialogValue = session.currentFile.substringAfterLast('/')
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
            Box(Modifier.weight(1f).fillMaxHeight()) {
                if (session.currentFile.isBlank() && session.editor.text == QuickNoteSession.SAMPLE) {
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(container.t("quickNote.select"), color = colors.textSecondary)
                        EditorHost(
                            buffer = session.editor,
                            dark = MooTheme.dark,
                            fontName = settings.editor.quickNoteFontName.ifBlank { "Monospaced" },
                            fontSize = settings.editor.quickNoteFontSize,
                            wrap = session.wrap,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    EditorHost(
                        buffer = session.editor,
                        dark = MooTheme.dark,
                        fontName = settings.editor.quickNoteFontName.ifBlank { "Monospaced" },
                        fontSize = settings.editor.quickNoteFontSize,
                        wrap = session.wrap
                    )
                }
            }
            if (session.replaceOpen) {
                Column(
                    Modifier.width(240.dp).fillMaxHeight().background(colors.surfaceSubtle).padding(10.dp).verticalScroll(rememberScrollState()),
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
            modifier = Modifier.fillMaxWidth().height(26.dp).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dirty = session.currentFile.isNotBlank() && session.editor.text != session.savedText
            Text(
                listOfNotNull(
                    session.currentFile.ifBlank { container.t("quickNote.select") },
                    if (dirty) container.t("quickNote.unsaved") else if (session.currentFile.isNotBlank()) container.t("quickNote.saved") else null,
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

    if (session.dialogMode.isNotBlank()) {
        Dialog(onDismissRequest = { session.dialogMode = ""; refresh() }) {
            Column(
                Modifier.width(420.dp).background(colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    when (session.dialogMode) {
                        "folder" -> container.t("quickNote.dialog.createFolder")
                        "rename" -> container.t("quickNote.dialog.rename")
                        else -> container.t("quickNote.dialog.createNote")
                    },
                    color = colors.textPrimary
                )
                MooTextField(session.dialogValue, { session.dialogValue = it; refresh() }, placeholder = container.t("quickNote.dialog.name"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MooButton(container.t("common.save"), primary = true, onClick = {
                        val name = session.dialogValue.trim()
                        if (name.isNotEmpty()) {
                            runCatching {
                                when (session.dialogMode) {
                                    "folder" -> vault.createDirectory(name)
                                    "rename" -> {
                                        val parent = session.currentFile.substringBeforeLast('/', missingDelimiterValue = "")
                                        val next = listOf(parent, name).filter { it.isNotBlank() }.joinToString("/")
                                        vault.rename(session.currentFile, name)
                                        session.currentFile = next
                                    }
                                    else -> {
                                        val fileName = if (name.contains('.')) name else "$name.md"
                                        saveIfNeeded(container, session, vault)
                                        vault.createFile(fileName, session.editor.text.ifBlank { "" })
                                        openFile(session, vault, fileName)
                                    }
                                }
                            }.onSuccess {
                                session.dialogMode = ""
                                session.error = ""
                                session.notice = container.t("quickNote.saved")
                            }.onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
                            refresh()
                        }
                    })
                    MooButton(container.t("common.close"), onClick = { session.dialogMode = ""; refresh() })
                }
            }
        }
    }
    if (session.historyOpen) {
        Dialog(onDismissRequest = { session.historyOpen = false; refresh() }) {
            Column(
                Modifier.width(520.dp).height(420.dp).background(colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(container.t("quickNote.history"), color = colors.textPrimary)
                if (historyItems.isEmpty()) {
                    Text(container.t("time.history.empty"), color = colors.textSecondary)
                } else {
                    LazyColumn(Modifier.weight(1f)) {
                        items(historyItems) { item ->
                            Column(Modifier.fillMaxWidth().clickable {
                                onEdt { session.editor.setText(item.input, recordUndo = true) }
                                session.historyOpen = false
                                session.notice = container.t("json.notice.restored")
                                refresh()
                            }.padding(8.dp)) {
                                Text(item.summary, color = colors.textPrimary, fontSize = 13.sp)
                                Text(item.createdAt, color = colors.textSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
                MooButton(container.t("common.close"), onClick = { session.historyOpen = false; refresh() })
            }
        }
    }
}

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

private fun saveCurrent(container: AppContainer, session: QuickNoteSession, vault: NoteVault) {
    val name = session.currentFile.ifBlank { "note-${System.currentTimeMillis()}.md" }
    runCatching { vault.write(name, session.editor.text) }
        .onSuccess {
            session.currentFile = name
            session.savedText = session.editor.text
            session.error = ""
            session.notice = container.t("quickNote.saved")
            container.history.save(ToolId.QuickNote.id, name, name, session.editor.text.take(8_000), "")
        }
        .onFailure { session.error = it.message ?: container.t("quickNote.saveFailed") }
}

private fun saveIfNeeded(container: AppContainer, session: QuickNoteSession, vault: NoteVault) {
    if (session.currentFile.isNotBlank() && session.editor.text != session.savedText) {
        saveCurrent(container, session, vault)
    }
}

private fun openFile(session: QuickNoteSession, vault: NoteVault, relativePath: String) {
    val text = vault.read(relativePath)
    onEdt { session.editor.setText(text, recordUndo = false) }
    session.currentFile = relativePath
    session.savedText = text
    session.error = ""
}

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
