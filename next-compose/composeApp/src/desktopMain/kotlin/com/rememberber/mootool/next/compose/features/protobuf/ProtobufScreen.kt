package com.rememberber.mootool.next.compose.features.protobuf

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.ProtobufBinaryFormat
import com.rememberber.mootool.next.compose.domain.ProtobufEngine
import com.rememberber.mootool.next.compose.domain.ProtobufException
import com.rememberber.mootool.next.compose.domain.ProtobufHistoryMetadata
import com.rememberber.mootool.next.compose.domain.ProtobufHistoryRestore
import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.ProtobufSession
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooToolTab
import com.rememberber.mootool.next.compose.ui.components.MooToolTabsRow
import com.rememberber.mootool.next.compose.ui.components.IoThreePaneRow
import com.rememberber.mootool.next.compose.ui.components.IoTwoPaneRow
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.mooProtobufWirePane
import com.rememberber.mootool.next.compose.ui.components.mooToolShell
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.mooStatusBarBackground
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.OverflowAction
import com.rememberber.mootool.next.compose.ui.components.OverflowActionCluster
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import com.rememberber.mootool.next.compose.sessions.dismissModalOverlays
import com.rememberber.mootool.next.compose.ui.workbench.DismissModalOverlaysOnDispose
import com.rememberber.mootool.next.compose.ui.workbench.onUserInput

@Composable
fun ProtobufScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.protobufSession() }
    DismissModalOverlaysOnDispose(container, ToolId.Protobuf) { session.dismissModalOverlays() }
    val revision by container.sessionManager.revision.collectAsState()
    val settings by container.settings.collectAsState()
    val colors = MooTheme.colors

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistProtobuf()
    }


    BoxWithConstraints(Modifier.fillMaxSize().background(colors.workspace)) {
        val overflow = LayoutPolicy.overflowToolbar(maxWidth.value)
        Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MooPageTitle(container.t("protobuf.title"))
            Spacer(Modifier.weight(1f))
            OverflowActionCluster(
                overflow = overflow,
                moreLabel = container.t("json.action.overflow"),
                actions = buildList {
                    add(OverflowAction(container.t("common.action.history")) { session.historyOpen = true; refresh() })
                    add(OverflowAction(container.t("protobuf.copy")) {
                        val content = when (session.tab) {
                            "wire" -> session.wireOutput
                            "convert" -> session.base64.ifEmpty { session.hex }
                            else -> session.binary
                        }
                        if (content.isEmpty()) {
                            session.notice = container.t("protobuf.nothingToCopy")
                        } else {
                            container.copyText(content)
                            session.notice = container.t("common.copied")
                            session.error = ""
                        }
                        refresh()
                    })
                    if (!detached) add(OverflowAction(container.t("app.tool.detach")) { container.sessionManager.detach(ToolId.Protobuf) })
                }
            )
        }
        MooToolTabsRow {
            listOf("json" to "protobuf.tab.json", "wire" to "protobuf.tab.wire", "convert" to "protobuf.tab.convert").forEach { (id, key) ->
                MooToolTab(container.t(key), selected = session.tab == id, onClick = {
                    session.tab = id
                    session.error = ""
                    refresh()
                })
            }
        }
        when (session.tab) {
            "wire" -> WireTab(container, session, settings, Modifier.weight(1f)) { refresh() }
            "convert" -> ConvertTab(container, session, settings, Modifier.weight(1f)) { refresh() }
            else -> JsonTab(container, session, settings, Modifier.weight(1f)) { refresh() }
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
            toolId = ToolId.Protobuf.id,
            title = container.t("common.action.history"),
            onRestore = { item ->
                ProtobufHistoryRestore.apply(session, item)
                session.historyOpen = false
                refresh()
            },
            onDismiss = { session.historyOpen = false; refresh() }
        )
    }
}

@Composable
private fun JsonTab(
    container: AppContainer,
    session: ProtobufSession,
    settings: AppSettings,
    modifier: Modifier,
    onChanged: () -> Unit
) {
    IoTwoPaneRow(
        container = container,
        settings = settings,
        paneKey = "protobuf-json",
        minLeft = 240f,
        minRight = 420f,
        defaultLeftFraction = 0.34f,
        modifier = modifier.padding(12.dp),
        left = {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(container.t("protobuf.definition"), color = MooTheme.colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                MooButton(container.t("protobuf.format"), p5Toolbar = true, onClick = {
                    val before = session.proto
                    session.proto = ProtobufEngine.formatProtoDefinition(session.proto)
                    session.notice = container.t("protobuf.history.format")
                    container.toastSuccess(session.notice)
                    session.error = ""
                    container.history.save(
                        ToolId.Protobuf.id,
                        session.notice,
                        session.notice,
                        before,
                        session.proto,
                        ProtobufHistoryMetadata.encodeLegacyPipe("json|format"),
                    )
                    onChanged()
                })
            }
            MooTextField(session.proto, { session.onUserInput { session.proto = it; session.error = ""; onChanged() } }, modifier = Modifier.weight(1f).fillMaxWidth(), singleLine = false)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(container.t("protobuf.message"), color = MooTheme.colors.textMuted, fontSize = 10.sp)
                    MooTextField(session.messageName, { session.onUserInput { session.messageName = it; onChanged() } }, modifier = Modifier.fillMaxWidth(), compact = true)
                }
                Column(Modifier.width(104.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Hex / Base64", color = MooTheme.colors.textMuted, fontSize = 10.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MooButton("Hex", primary = session.format == ProtobufBinaryFormat.Hex, p5Toolbar = true, onClick = { session.format = ProtobufBinaryFormat.Hex; onChanged() })
                        MooButton("Base64", primary = session.format == ProtobufBinaryFormat.Base64, p5Toolbar = true, onClick = { session.format = ProtobufBinaryFormat.Base64; onChanged() })
                    }
                }
            }
        }
        },
        right = {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("JSON", color = MooTheme.colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            MooTextField(session.json, { session.onUserInput { session.json = it; session.error = ""; onChanged() } }, modifier = Modifier.weight(1f).fillMaxWidth(), singleLine = false)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("protobuf.toBinary"), prominent = true, p5Toolbar = true, onClick = {
                    runOp(container, session, container.t("protobuf.history.jsonToBinary"), session.json, "json|jsonToBinary|${session.messageName}|${session.format.name}") {
                        ProtobufEngine.jsonToProtobuf(session.proto, session.messageName, session.json, session.format).also { session.binary = it }
                    }
                    onChanged()
                })
                MooButton(container.t("protobuf.toJson"), p5Toolbar = true, onClick = {
                    runOp(container, session, container.t("protobuf.history.binaryToJson"), session.binary, "json|binaryToJson|${session.messageName}|${session.format.name}") {
                        ProtobufEngine.protobufToJson(session.proto, session.messageName, session.binary, session.format).also { session.json = it }
                    }
                    onChanged()
                })
            }
            Text(container.t("protobuf.binary"), color = MooTheme.colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            MooTextField(session.binary, { session.onUserInput { session.binary = it; session.error = ""; onChanged() } }, modifier = Modifier.weight(1f).fillMaxWidth(), singleLine = false)
        }
        }
    )
}

@Composable
private fun WireTab(
    container: AppContainer,
    session: ProtobufSession,
    settings: AppSettings,
    modifier: Modifier,
    onChanged: () -> Unit
) {
    IoThreePaneRow(
        container = container,
        settings = settings,
        paneKey = "protobuf-wire",
        middleRatio = 0.28f,
        minRight = 240f,
        modifier = modifier.padding(12.dp),
        left = {
            Column(
                Modifier.fillMaxSize().mooToolShell().mooProtobufWirePane().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(container.t("protobuf.wireInput"), color = MooTheme.colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                MooTextField(session.wireInput, { session.onUserInput { session.wireInput = it; session.error = ""; onChanged() } }, modifier = Modifier.weight(1f).fillMaxWidth(), singleLine = false)
            }
        },
        middle = {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MooButton("Hex", primary = session.wireFormat == ProtobufBinaryFormat.Hex, p5Toolbar = true, onClick = { session.wireFormat = ProtobufBinaryFormat.Hex; onChanged() })
                MooButton("Base64", primary = session.wireFormat == ProtobufBinaryFormat.Base64, p5Toolbar = true, onClick = { session.wireFormat = ProtobufBinaryFormat.Base64; onChanged() })
                MooButton(container.t("protobuf.decode"), prominent = true, p5Toolbar = true, onClick = {
                    runOp(container, session, container.t("protobuf.history.wire"), session.wireInput, "wire|decode|${session.wireFormat.name}") {
                        ProtobufEngine.decodeWire(session.wireInput, session.wireFormat).also { session.wireOutput = it }
                    }
                    onChanged()
                })
            }
        },
        right = {
            Column(Modifier.fillMaxSize().mooToolShell().padding(8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(container.t("protobuf.wireOutput"), color = MooTheme.colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Column(Modifier.weight(1f).fillMaxWidth().background(MooTheme.colors.surfaceSubtle, RoundedCornerShape(8.dp)).padding(10.dp).verticalScroll(rememberScrollState())) {
                    Text(session.wireOutput.ifEmpty { container.t("protobuf.decode") }, color = MooTheme.colors.textPrimary, fontSize = 13.sp)
                }
            }
        }
    )
}

@Composable
private fun ConvertTab(
    container: AppContainer,
    session: ProtobufSession,
    settings: AppSettings,
    modifier: Modifier,
    onChanged: () -> Unit
) {
    IoThreePaneRow(
        container = container,
        settings = settings,
        paneKey = "protobuf-convert",
        middleRatio = 0.28f,
        minRight = 240f,
        modifier = modifier.padding(12.dp),
        left = {
            Column(Modifier.fillMaxSize().mooToolShell().padding(8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Hex", color = MooTheme.colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                MooTextField(session.hex, { session.onUserInput { session.hex = it; session.error = ""; onChanged() } }, modifier = Modifier.weight(1f).fillMaxWidth(), singleLine = false)
            }
        },
        middle = {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MooButton(container.t("protobuf.hexToBase64"), prominent = true, p5Toolbar = true, onClick = {
                    runOp(container, session, container.t("protobuf.history.hexToBase64"), session.hex, "convert|hexToBase64") {
                        ProtobufEngine.convertBinary(session.hex, ProtobufBinaryFormat.Hex, ProtobufBinaryFormat.Base64).also { session.base64 = it }
                    }
                    onChanged()
                })
                MooButton(container.t("protobuf.base64ToHex"), p5Toolbar = true, onClick = {
                    runOp(container, session, container.t("protobuf.history.base64ToHex"), session.base64, "convert|base64ToHex") {
                        ProtobufEngine.convertBinary(session.base64, ProtobufBinaryFormat.Base64, ProtobufBinaryFormat.Hex).also { session.hex = it }
                    }
                    onChanged()
                })
            }
        },
        right = {
            Column(Modifier.fillMaxSize().mooToolShell().padding(8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Base64", color = MooTheme.colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                MooTextField(session.base64, { session.onUserInput { session.base64 = it; session.error = ""; onChanged() } }, modifier = Modifier.weight(1f).fillMaxWidth(), singleLine = false)
            }
        }
    )
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
            container.toastSuccess(summary)
            container.history.save(
                ToolId.Protobuf.id,
                summary,
                summary,
                input,
                output,
                ProtobufHistoryMetadata.encodeLegacyPipe(options),
            )
        }
        .onFailure { error ->
            session.notice = ""
            val message = messageFor(container, error)
            session.error = message
            container.toastError(message)
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

