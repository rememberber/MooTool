package com.rememberber.mootool.next.compose.features.protobuf

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.ProtobufBinaryFormat
import com.rememberber.mootool.next.compose.domain.ProtobufEngine
import com.rememberber.mootool.next.compose.domain.ProtobufException
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.ProtobufSession
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun ProtobufScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.protobufSession() }
    val revision by container.sessionManager.revision.collectAsState()
    var historyItems by remember { mutableStateOf(emptyList<HistoryRecord>()) }
    val colors = MooTheme.colors

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistProtobuf()
    }

    LaunchedEffect(session.historyOpen, revision) {
        if (session.historyOpen) historyItems = container.history.list(ToolId.Protobuf.id)
    }

    Column(Modifier.fillMaxSize().background(colors.workspace)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("protobuf.title"), color = colors.textPrimary, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            MooButton(container.t("common.action.history"), onClick = { session.historyOpen = true; refresh() })
            MooButton(container.t("protobuf.copy"), onClick = {
                val content = when (session.tab) {
                    "wire" -> session.wireOutput
                    "convert" -> session.base64.ifEmpty { session.hex }
                    else -> session.binary
                }
                if (content.isEmpty()) {
                    session.notice = container.t("protobuf.nothingToCopy")
                } else {
                    copyText(content)
                    session.notice = container.t("common.copied")
                    session.error = ""
                }
                refresh()
            })
            if (!detached) {
                MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.Protobuf) })
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.surfaceSubtle).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("json" to "protobuf.tab.json", "wire" to "protobuf.tab.wire", "convert" to "protobuf.tab.convert").forEach { (id, key) ->
                MooButton(container.t(key), primary = session.tab == id, onClick = {
                    session.tab = id
                    session.error = ""
                    refresh()
                })
            }
        }
        when (session.tab) {
            "wire" -> WireTab(container, session, Modifier.weight(1f)) { refresh() }
            "convert" -> ConvertTab(container, session, Modifier.weight(1f)) { refresh() }
            else -> JsonTab(container, session, Modifier.weight(1f)) { refresh() }
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
        ProtobufHistoryDialog(container, session, historyItems) { refresh() }
    }
}

@Composable
private fun JsonTab(container: AppContainer, session: ProtobufSession, modifier: Modifier, onChanged: () -> Unit) {
    Row(modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(0.9f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(container.t("protobuf.definition"), color = MooTheme.colors.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                MooButton(container.t("protobuf.format"), onClick = {
                    val before = session.proto
                    session.proto = ProtobufEngine.formatProtoDefinition(session.proto)
                    session.notice = container.t("protobuf.history.format")
                    session.error = ""
                    container.history.save(ToolId.Protobuf.id, session.notice, session.notice, before, session.proto, "json|format")
                    onChanged()
                })
            }
            MooTextField(session.proto, { session.proto = it; session.error = ""; onChanged() }, modifier = Modifier.weight(1f).fillMaxWidth(), singleLine = false)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(container.t("protobuf.message"), color = MooTheme.colors.textSecondary, fontSize = 12.sp)
                MooTextField(session.messageName, { session.messageName = it; onChanged() }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MooButton("Hex", primary = session.format == ProtobufBinaryFormat.Hex, onClick = { session.format = ProtobufBinaryFormat.Hex; onChanged() })
                MooButton("Base64", primary = session.format == ProtobufBinaryFormat.Base64, onClick = { session.format = ProtobufBinaryFormat.Base64; onChanged() })
            }
        }
        Column(Modifier.weight(1.1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("JSON", color = MooTheme.colors.textSecondary, fontSize = 12.sp)
            MooTextField(session.json, { session.json = it; session.error = ""; onChanged() }, modifier = Modifier.weight(1f).fillMaxWidth(), singleLine = false)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("protobuf.toBinary"), primary = true, onClick = {
                    runOp(container, session, container.t("protobuf.history.jsonToBinary"), session.json, "json|jsonToBinary|${session.messageName}|${session.format.name}") {
                        ProtobufEngine.jsonToProtobuf(session.proto, session.messageName, session.json, session.format).also { session.binary = it }
                    }
                    onChanged()
                })
                MooButton(container.t("protobuf.toJson"), onClick = {
                    runOp(container, session, container.t("protobuf.history.binaryToJson"), session.binary, "json|binaryToJson|${session.messageName}|${session.format.name}") {
                        ProtobufEngine.protobufToJson(session.proto, session.messageName, session.binary, session.format).also { session.json = it }
                    }
                    onChanged()
                })
            }
            Text(container.t("protobuf.binary"), color = MooTheme.colors.textSecondary, fontSize = 12.sp)
            MooTextField(session.binary, { session.binary = it; session.error = ""; onChanged() }, modifier = Modifier.weight(1f).fillMaxWidth(), singleLine = false)
        }
    }
}

@Composable
private fun WireTab(container: AppContainer, session: ProtobufSession, modifier: Modifier, onChanged: () -> Unit) {
    Row(modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(container.t("protobuf.wireInput"), color = MooTheme.colors.textSecondary, fontSize = 12.sp)
            MooTextField(session.wireInput, { session.wireInput = it; session.error = ""; onChanged() }, modifier = Modifier.weight(1f).fillMaxWidth(), singleLine = false)
        }
        Column(
            modifier = Modifier.width(140.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MooButton("Hex", primary = session.wireFormat == ProtobufBinaryFormat.Hex, onClick = { session.wireFormat = ProtobufBinaryFormat.Hex; onChanged() })
            MooButton("Base64", primary = session.wireFormat == ProtobufBinaryFormat.Base64, onClick = { session.wireFormat = ProtobufBinaryFormat.Base64; onChanged() })
            MooButton(container.t("protobuf.decode"), primary = true, onClick = {
                runOp(container, session, container.t("protobuf.history.wire"), session.wireInput, "wire|decode|${session.wireFormat.name}") {
                    ProtobufEngine.decodeWire(session.wireInput, session.wireFormat).also { session.wireOutput = it }
                }
                onChanged()
            })
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(container.t("protobuf.wireOutput"), color = MooTheme.colors.textSecondary, fontSize = 12.sp)
            Column(Modifier.weight(1f).fillMaxWidth().background(MooTheme.colors.surfaceSubtle, RoundedCornerShape(8.dp)).padding(10.dp).verticalScroll(rememberScrollState())) {
                Text(session.wireOutput.ifEmpty { container.t("protobuf.decode") }, color = MooTheme.colors.textPrimary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ConvertTab(container: AppContainer, session: ProtobufSession, modifier: Modifier, onChanged: () -> Unit) {
    Row(modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Hex", color = MooTheme.colors.textSecondary, fontSize = 12.sp)
            MooTextField(session.hex, { session.hex = it; session.error = ""; onChanged() }, modifier = Modifier.weight(1f).fillMaxWidth(), singleLine = false)
        }
        Column(
            modifier = Modifier.width(168.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MooButton(container.t("protobuf.hexToBase64"), primary = true, onClick = {
                runOp(container, session, container.t("protobuf.history.hexToBase64"), session.hex, "convert|hexToBase64") {
                    ProtobufEngine.convertBinary(session.hex, ProtobufBinaryFormat.Hex, ProtobufBinaryFormat.Base64).also { session.base64 = it }
                }
                onChanged()
            })
            MooButton(container.t("protobuf.base64ToHex"), onClick = {
                runOp(container, session, container.t("protobuf.history.base64ToHex"), session.base64, "convert|base64ToHex") {
                    ProtobufEngine.convertBinary(session.base64, ProtobufBinaryFormat.Base64, ProtobufBinaryFormat.Hex).also { session.hex = it }
                }
                onChanged()
            })
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Base64", color = MooTheme.colors.textSecondary, fontSize = 12.sp)
            MooTextField(session.base64, { session.base64 = it; session.error = ""; onChanged() }, modifier = Modifier.weight(1f).fillMaxWidth(), singleLine = false)
        }
    }
}

private fun runOp(
    container: AppContainer,
    session: ProtobufSession,
    summary: String,
    input: String,
    options: String,
    block: () -> String
) {
    runCatching { block() }
        .onSuccess { output ->
            session.error = ""
            session.notice = summary
            container.history.save(ToolId.Protobuf.id, summary, summary, input, output, options)
        }
        .onFailure { error ->
            session.notice = ""
            session.error = messageFor(container, error)
        }
}

private fun messageFor(container: AppContainer, error: Throwable): String {
    val code = (error as? ProtobufException)?.code
    val message = error.message ?: ""
    return when (code) {
        "compile" -> container.t("protobuf.error.compile", mapOf("message" to message))
        "missing-message" -> container.t("protobuf.error.message", mapOf("message" to message))
        "json" -> container.t("protobuf.error.json", mapOf("message" to message))
        "invalid-hex" -> container.t("protobuf.error.hex")
        "invalid-base64" -> container.t("protobuf.error.base64")
        "wire" -> container.t("protobuf.error.wire", mapOf("message" to message))
        "compiler" -> container.t("protobuf.error.compiler", mapOf("message" to message))
        else -> container.t("protobuf.error.generic", mapOf("message" to message))
    }
}

private fun copyText(value: String) {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
}

@Composable
private fun ProtobufHistoryDialog(
    container: AppContainer,
    session: ProtobufSession,
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
                            applyHistory(session, item)
                            session.notice = container.t("json.notice.restored")
                            session.historyOpen = false
                            onChanged()
                        }.padding(8.dp)) {
                            Text(item.summary, color = MooTheme.colors.textPrimary, fontSize = 13.sp)
                            Text(item.createdAt, color = MooTheme.colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.clear"), onClick = {
                    container.history.clear(ToolId.Protobuf.id)
                    session.historyOpen = false
                    onChanged()
                })
                MooButton(container.t("common.close"), onClick = { session.historyOpen = false; onChanged() })
            }
        }
    }
}

private fun applyHistory(session: ProtobufSession, item: HistoryRecord) {
    val parts = item.options.split('|')
    val historyTab = parts.getOrNull(0).orEmpty()
    val operation = parts.getOrNull(1).orEmpty()
    val historyMessage = parts.getOrNull(2).orEmpty()
    val historyFormat = parts.getOrNull(3).orEmpty().ifEmpty { parts.getOrNull(2).orEmpty() }
    if (historyTab in listOf("json", "wire", "convert")) session.tab = historyTab
    if (historyMessage.isNotEmpty() && historyTab == "json") session.messageName = historyMessage
    if (historyFormat == "Hex" || historyFormat == "Base64") {
        val format = ProtobufSession.formatOf(historyFormat)
        if (historyTab == "wire") session.wireFormat = format else session.format = format
    }
    when (operation) {
        "jsonToBinary" -> { session.json = item.input; session.binary = item.output }
        "binaryToJson" -> { session.binary = item.input; session.json = item.output }
        "format" -> session.proto = item.output
        "decode" -> { session.wireInput = item.input; session.wireOutput = item.output }
        "hexToBase64" -> { session.hex = item.input; session.base64 = item.output }
        "base64ToHex" -> { session.base64 = item.input; session.hex = item.output }
    }
    session.error = ""
}
