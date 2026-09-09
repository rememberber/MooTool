package com.rememberber.mootool.next.compose.features.ua

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import com.rememberber.mootool.next.compose.domain.UaEngine
import com.rememberber.mootool.next.compose.domain.UaException
import com.rememberber.mootool.next.compose.domain.UaResult
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.UaSession
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun UaParseScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.uaSession() }
    val revision by container.sessionManager.revision.collectAsState()
    var historyItems by remember { mutableStateOf(emptyList<HistoryRecord>()) }
    var presetOpen by remember { mutableStateOf(false) }
    val colors = MooTheme.colors

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistUa()
    }

    LaunchedEffect(session.historyOpen, revision) {
        if (session.historyOpen) historyItems = container.history.list(ToolId.UaParse.id)
    }

    Column(Modifier.fillMaxSize().background(colors.workspace)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("ua.title"), color = colors.textPrimary, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            MooButton(container.t("common.action.history"), onClick = { session.historyOpen = true; refresh() })
            if (!detached) {
                MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.UaParse) })
            }
        }
        Row(Modifier.weight(1f).fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(container.t("ua.input"), color = colors.textSecondary, fontSize = 12.sp)
                MooTextField(
                    session.source,
                    { session.source = it; session.error = ""; refresh() },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    singleLine = false
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Box {
                        MooButton(container.t("ua.preset"), onClick = { presetOpen = true })
                        DropdownMenu(expanded = presetOpen, onDismissRequest = { presetOpen = false }) {
                            UaEngine.presets.forEach { (name, value) ->
                                DropdownMenuItem(onClick = {
                                    session.source = value
                                    session.error = ""
                                    presetOpen = false
                                    refresh()
                                }) { Text(name, fontSize = 13.sp) }
                            }
                        }
                    }
                    MooButton(container.t("common.action.paste"), onClick = {
                        pasteFromClipboard()?.let { session.source = it; session.error = ""; refresh() }
                            ?: run { session.error = container.t("json.notice.copyFailed"); refresh() }
                    })
                    MooButton(container.t("common.action.clear"), onClick = {
                        session.source = ""
                        session.result = null
                        session.error = ""
                        session.notice = container.t("json.notice.cleared")
                        refresh()
                    })
                    MooButton(container.t("ua.parse"), primary = true, onClick = {
                        parseSource(container, session)
                        refresh()
                    })
                }
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceSubtle).border(1.dp, colors.border, RoundedCornerShape(12.dp)).padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(container.t("common.result"), color = colors.textPrimary, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    MooButton(container.t("time.copy"), onClick = {
                        val text = session.result?.let { resultCodec.encodeToString(it) } ?: session.source
                        session.notice = copyText(text, container)
                        refresh()
                    })
                }
                resultRows(container, session.result).forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, color = colors.textSecondary, fontSize = 13.sp)
                        Text(value.ifBlank { container.t("ua.unknown") }, color = colors.textPrimary, fontSize = 13.sp)
                    }
                }
            }
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
        UaHistoryDialog(container, session, historyItems) { refresh() }
    }
}

private val resultCodec = Json { ignoreUnknownKeys = true; encodeDefaults = true }

private fun parseSource(container: AppContainer, session: UaSession) {
    runCatching { UaEngine.parse(session.source) }
        .onSuccess { result ->
            session.result = result
            session.error = ""
            session.notice = container.t("ua.parse")
            container.history.save(ToolId.UaParse.id, container.t("ua.title"), container.t("ua.title"), session.source, resultCodec.encodeToString(result))
        }
        .onFailure { error ->
            session.notice = ""
            session.error = if ((error as? UaException)?.code == "empty") container.t("ua.empty") else (error.message ?: container.t("ua.empty"))
        }
}

private fun resultRows(container: AppContainer, result: UaResult?): List<Pair<String, String>> {
    if (result == null) return emptyList()
    fun yn(value: Boolean) = if (value) container.t("common.yes") else container.t("common.no")
    return listOf(
        container.t("ua.browser") to result.browser,
        container.t("ua.browserVersion") to result.browserVersion,
        container.t("ua.engine") to result.engine,
        container.t("ua.engineVersion") to result.engineVersion,
        container.t("ua.os") to result.os,
        container.t("ua.osVersion") to result.osVersion,
        container.t("ua.deviceType") to result.deviceType,
        container.t("ua.deviceBrand") to result.deviceBrand,
        container.t("ua.deviceModel") to result.deviceModel,
        container.t("ua.mobile") to yn(result.mobile),
        container.t("ua.bot") to yn(result.bot)
    )
}

@Composable
private fun UaHistoryDialog(
    container: AppContainer,
    session: UaSession,
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
                            session.source = item.input
                            session.result = runCatching { resultCodec.decodeFromString<UaResult>(item.output) }.getOrNull()
                            session.notice = container.t("json.notice.restored")
                            session.historyOpen = false
                            onChanged()
                        }.padding(8.dp)) {
                            Text(item.summary, color = MooTheme.colors.textPrimary, fontSize = 13.sp)
                            Text(item.input.take(120), color = MooTheme.colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.clear"), onClick = {
                    container.history.clear(ToolId.UaParse.id)
                    onChanged()
                })
                MooButton(container.t("common.close"), onClick = { session.historyOpen = false; onChanged() })
            }
        }
    }
}

private fun pasteFromClipboard(): String? = try {
    Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String
} catch (_: Exception) {
    null
}

private fun copyText(value: String, container: AppContainer): String {
    return try {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
        container.t("time.notice.copied")
    } catch (_: Exception) {
        container.t("json.notice.copyFailed")
    }
}
