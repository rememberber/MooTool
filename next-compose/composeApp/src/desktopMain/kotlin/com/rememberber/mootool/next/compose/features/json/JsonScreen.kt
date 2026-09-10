package com.rememberber.mootool.next.compose.features.json

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.FindReplace
import com.rememberber.mootool.next.compose.domain.JsonEngine
import com.rememberber.mootool.next.compose.domain.JsonTranslator
import com.rememberber.mootool.next.compose.editor.EditorHost
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.JsonSession
import com.rememberber.mootool.next.compose.storage.VaultEntry
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
fun JsonScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.jsonSession() }
    val settings by container.settings.collectAsState()
    val revision by container.sessionManager.revision.collectAsState()
    var tick by remember { mutableStateOf(0L) }
    fun refresh() {
        tick += 1
        container.sessionManager.bump()
        container.sessionManager.persistJson()
    }
    val translator = remember(settings.general.language) {
        JsonTranslator { key, params -> container.t(key, params) }
    }
    val status = remember(session.editor.revision, tick, settings.general.language) {
        JsonEngine.validate(session.editor.text, translator)
    }
    var vaultItems by remember { mutableStateOf(container.jsonVault.list(session.vaultQuery)) }
    var historyItems by remember { mutableStateOf(emptyList<HistoryRecord>()) }
    val colors = MooTheme.colors
    LaunchedEffect(session.vaultQuery, tick) {
        vaultItems = container.jsonVault.list(session.vaultQuery)
    }
    LaunchedEffect(session.historyOpen, tick) {
        if (session.historyOpen) historyItems = container.history.list(ToolId.Json.id)
    }

    Column(Modifier.fillMaxSize().background(colors.workspace)) {
        JsonToolbar(container, session, translator, onChanged = { refresh() })
        if (session.findOpen) {
            FindBar(container, session, onChanged = { refresh() })
        }
        Row(Modifier.weight(1f).fillMaxWidth()) {
            VaultPane(container, session, vaultItems, onChanged = { refresh() })
            Box(Modifier.weight(1f).fillMaxHeight()) {
                EditorHost(
                    buffer = session.editor,
                    dark = MooTheme.dark,
                    fontName = "Monospaced",
                    fontSize = settings.editor.jsonFontSize,
                    wrap = session.wrap
                )
            }
            if (session.inspectorOpen) {
                InspectorPane(container, session, translator, onChanged = { refresh() })
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(26.dp).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(status.message, color = when (status.kind) {
                com.rememberber.mootool.next.compose.domain.JsonStatus.Kind.Error -> colors.danger
                com.rememberber.mootool.next.compose.domain.JsonStatus.Kind.Valid -> colors.success
                else -> colors.textSecondary
            }, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text(session.notice, color = colors.textSecondary, fontSize = 12.sp)
            if (detached) Text(" · detached", color = colors.textSecondary, fontSize = 12.sp)
        }
    }

    if (session.historyOpen) {
        HistoryDialog(container, session, historyItems) { refresh() }
    }
    if (session.dialogTitle.isNotEmpty()) {
        ResultDialog(container, session) { refresh() }
    }
    if (session.dialogInputMode.isNotEmpty()) {
        InputDialog(container, session, translator) { refresh() }
    }
}

@Composable
private fun JsonToolbar(
    container: AppContainer,
    session: JsonSession,
    translator: JsonTranslator,
    onChanged: () -> Unit
) {
    val colors = MooTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().height(46.dp).background(colors.toolbar).padding(horizontal = 8.dp),
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
        MooButton(if (session.wrap) container.t("json.action.wrap") else container.t("json.action.nowrap"), onClick = {
            session.wrap = !session.wrap
            onChanged()
        })
        MooButton(container.t("json.action.copy"), onClick = {
            session.notice = copyText(session.editor.text, container)
            onChanged()
        })
        MooButton(container.t("json.action.find"), onClick = {
            session.findOpen = !session.findOpen
            onChanged()
        })
        MooButton(container.t("json.action.import"), onClick = {
            chooseFile(false)?.let { file ->
                onEdt {
                    session.editor.setText(file.readText(Charsets.UTF_8), recordUndo = true)
                    session.notice = container.t("json.notice.imported")
                }
                onChanged()
            }
        })
        MooButton(container.t("json.action.export"), onClick = {
            chooseFile(true)?.let { file ->
                file.writeText(session.editor.text, Charsets.UTF_8)
                session.notice = container.t("json.notice.exported")
                onChanged()
            }
        })
        MooButton(container.t("json.action.history"), onClick = {
            session.historyOpen = true
            onChanged()
        })
        MooButton(container.t("json.action.more"), onClick = {
            session.inspectorOpen = !session.inspectorOpen
            onChanged()
        })
        Spacer(Modifier.weight(1f))
        MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.Json) })
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
private fun VaultPane(container: AppContainer, session: JsonSession, items: List<VaultEntry>, onChanged: () -> Unit) {
    val colors = MooTheme.colors
    Column(Modifier.width(240.dp).fillMaxHeight().background(colors.sidebar).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(container.t("json.vault.title"), color = colors.textPrimary, fontSize = 12.sp)
        MooTextField(session.vaultQuery, { session.vaultQuery = it; onChanged() }, placeholder = container.t("app.search.placeholder"))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MooButton(container.t("json.vault.new"), onClick = {
                val name = "snippet-${System.currentTimeMillis()}.json"
                container.jsonVault.createFile(name, session.editor.text.ifBlank { "{\n}\n" })
                session.currentFile = name
                onChanged()
            })
            MooButton(container.t("json.vault.save"), onClick = {
                val name = session.currentFile.ifBlank { "draft.json" }
                container.jsonVault.write(name, session.editor.text)
                session.currentFile = name
                session.notice = container.t("common.save")
                onChanged()
            })
        }
        if (items.isEmpty()) {
            Text(container.t("json.vault.empty"), color = colors.textSecondary, fontSize = 12.sp)
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(items.filter { !it.directory }) { item ->
                    Text(
                        item.relativePath,
                        color = if (item.relativePath == session.currentFile) colors.accent else colors.textPrimary,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable {
                            onEdt { session.editor.setText(container.jsonVault.read(item.relativePath), recordUndo = false) }
                            session.currentFile = item.relativePath
                            onChanged()
                        }.padding(6.dp)
                    )
                }
            }
        }
        if (session.currentFile.isNotBlank()) {
            MooButton(container.t("json.vault.delete"), onClick = {
                container.jsonVault.delete(session.currentFile)
                session.currentFile = ""
                onChanged()
            })
        }
    }
}

@Composable
private fun InspectorPane(container: AppContainer, session: JsonSession, translator: JsonTranslator, onChanged: () -> Unit) {
    val colors = MooTheme.colors
    Column(
        Modifier.width(280.dp).fillMaxHeight().background(colors.surfaceSubtle).padding(10.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(container.t("json.panel.format"), color = colors.textPrimary, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(2, 4).forEach { spaces ->
                MooButton("$spaces", primary = session.formatOptions.spaces == spaces, onClick = {
                    session.formatOptions = session.formatOptions.copy(spaces = spaces)
                    onChanged()
                })
            }
        }
        MooButton(container.t("json.format.sortKeys") + ": ${session.formatOptions.sortKeys}", onClick = {
            session.formatOptions = session.formatOptions.copy(sortKeys = !session.formatOptions.sortKeys)
            onChanged()
        })
        MooButton(container.t("json.format.ignoreCase") + ": ${session.formatOptions.ignoreCase}", onClick = {
            session.formatOptions = session.formatOptions.copy(ignoreCase = !session.formatOptions.ignoreCase)
            onChanged()
        })
        MooButton(container.t("json.format.duplicateKeys") + ": ${session.formatOptions.checkDuplicateKeys}", onClick = {
            session.formatOptions = session.formatOptions.copy(checkDuplicateKeys = !session.formatOptions.checkDuplicateKeys)
            onChanged()
        })
        MooButton(container.t("json.format.apply"), primary = true, onClick = {
            transform(container, session, translator, container.t("json.notice.formatted")) {
                JsonEngine.formatAdvanced(it, translator, session.formatOptions)
            }
            onChanged()
        })
        Text(container.t("json.panel.convert"), color = colors.textPrimary, fontSize = 12.sp)
        MooButton(container.t("json.action.escape"), onClick = {
            transform(container, session, translator, container.t("json.notice.escaped")) { JsonEngine.escapeJsonString(it) }
            onChanged()
        })
        MooButton(container.t("json.action.unescape"), onClick = {
            transform(container, session, translator, container.t("json.notice.unescaped")) { JsonEngine.unescapeJsonString(it, translator) }
            onChanged()
        })
        MooButton(container.t("json.action.escapeText"), onClick = {
            transform(container, session, translator, container.t("json.notice.escaped")) { JsonEngine.escapeJavaString(it) }
            onChanged()
        })
        MooButton(container.t("json.action.unescapeText"), onClick = {
            transform(container, session, translator, container.t("json.notice.unescaped")) { JsonEngine.unescapeJsonText(it) }
            onChanged()
        })
        MooButton(container.t("json.action.swap"), onClick = {
            transform(container, session, translator, container.t("json.action.swap")) { JsonEngine.swapKeysAndValues(it, translator) }
            onChanged()
        })
        MooButton(container.t("json.action.jsonToXml"), onClick = {
            showResult(container, session, translator, container.t("json.action.jsonToXml")) { JsonEngine.jsonToXml(it, translator) }
            onChanged()
        })
        MooButton(container.t("json.action.jsonToBean"), onClick = {
            showResult(container, session, translator, container.t("json.action.jsonToBean")) { JsonEngine.jsonToJavaBean(it, translator) }
            onChanged()
        })
        MooButton(container.t("json.action.xmlToJson"), onClick = {
            session.dialogInputMode = "xml"
            onChanged()
        })
        MooButton(container.t("json.action.beanToJson"), onClick = {
            session.dialogInputMode = "bean"
            onChanged()
        })
        Text(container.t("json.panel.jsonPath"), color = colors.textPrimary, fontSize = 12.sp)
        MooTextField(session.jsonPath, { session.jsonPath = it; onChanged() }, placeholder = container.t("json.path.placeholder"))
        MooButton(container.t("json.path.query"), primary = true, onClick = {
            showResult(container, session, translator, container.t("json.notice.pathApplied")) {
                JsonEngine.queryPath(it, session.jsonPath, translator)
            }
            onChanged()
        })
        val paths = runCatching { JsonEngine.listPaths(session.editor.text, translator) }.getOrDefault(emptyList())
        paths.take(80).forEach { entry ->
            Text(
                "${"  ".repeat(entry.depth)}${entry.label}",
                color = colors.textSecondary,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().clickable {
                    session.jsonPath = entry.path
                    onChanged()
                }.padding(vertical = 2.dp)
            )
        }
        if (session.pathResult.isNotBlank()) {
            Text(session.pathResult, color = colors.textPrimary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun HistoryDialog(container: AppContainer, session: JsonSession, items: List<HistoryRecord>, onChanged: () -> Unit) {
    Dialog(onDismissRequest = { session.historyOpen = false; onChanged() }) {
        Column(
            Modifier.width(520.dp).height(420.dp).background(MooTheme.colors.workspace, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("json.history.title"), color = MooTheme.colors.textPrimary)
            if (items.isEmpty()) {
                Text(container.t("json.history.empty"), color = MooTheme.colors.textSecondary)
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(items) { item ->
                        Column(Modifier.fillMaxWidth().clickable {
                            onEdt { session.editor.setText(item.output.ifBlank { item.input }, recordUndo = true) }
                            session.notice = container.t("json.notice.restored")
                            session.historyOpen = false
                            onChanged()
                        }.padding(8.dp)) {
                            Text(item.summary, color = MooTheme.colors.textPrimary, fontSize = 13.sp)
                            Text(item.createdAt, color = MooTheme.colors.textSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.clear"), onClick = {
                    container.history.clear(ToolId.Json.id)
                    onChanged()
                })
                MooButton(container.t("common.close"), onClick = { session.historyOpen = false; onChanged() })
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

private fun copyText(value: String, container: AppContainer): String {
    return try {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
        container.t("json.notice.copied")
    } catch (_: Exception) {
        container.t("json.notice.copyFailed")
    }
}

private fun chooseFile(save: Boolean): File? {
    val dialog = FileDialog(null as Frame?, if (save) "Export JSON" else "Import JSON", if (save) FileDialog.SAVE else FileDialog.LOAD)
    dialog.isVisible = true
    val file = dialog.file ?: return null
    val directory = dialog.directory ?: return null
    return File(directory, file)
}

private fun onEdt(block: () -> Unit) {
    if (SwingUtilities.isEventDispatchThread()) block() else SwingUtilities.invokeLater(block)
}
