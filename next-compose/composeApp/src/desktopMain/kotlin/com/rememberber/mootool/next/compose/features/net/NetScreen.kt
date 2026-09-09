package com.rememberber.mootool.next.compose.features.net

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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.NetCommandHandle
import com.rememberber.mootool.next.compose.domain.NetConvertException
import com.rememberber.mootool.next.compose.domain.NetEngine
import com.rememberber.mootool.next.compose.domain.NetworkAction
import com.rememberber.mootool.next.compose.domain.NetworkErrorCode
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.NetSession
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun NetScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.netSession() }
    val revision by container.sessionManager.revision.collectAsState()
    val colors = MooTheme.colors
    val scope = rememberCoroutineScope()
    var commandJob by remember { mutableStateOf<Job?>(null) }
    var handle by remember { mutableStateOf(NetCommandHandle()) }
    var historyItems by remember { mutableStateOf(emptyList<HistoryRecord>()) }
    val outputScroll = rememberScrollState()

    fun persist() {
        container.sessionManager.bump()
        container.sessionManager.persistNet()
    }

    fun stop() {
        handle.cancel()
        commandJob?.cancel()
        session.running = null
        persist()
    }

    fun refreshAddresses() {
        scope.launch(Dispatchers.IO) {
            val snapshot = runCatching { NetEngine.localAddresses() }
            withContext(Dispatchers.Main) {
                snapshot.onSuccess {
                    session.ipv4Addresses = it.ipv4.joinToString("\n")
                    session.ipv6Addresses = it.ipv6.joinToString("\n")
                    session.error = ""
                }.onFailure {
                    session.error = it.message ?: container.t("net.error.COMMAND_FAILED")
                }
                persist()
            }
        }
    }

    fun runAction(action: NetworkAction, target: String? = null, ports: String? = null) {
        if (session.running != null) return
        handle.cancel()
        val current = NetCommandHandle()
        handle = current
        session.running = action
        session.error = ""
        session.notice = ""
        session.output = container.t("net.running")
        persist()
        commandJob = session.commandScope.launch {
            val timeout = container.settings.value.network.requestTimeoutMs
            val result = NetEngine.run(action, target, ports, timeout, current) { chunk ->
                session.commandScope.launch(Dispatchers.Main) {
                    if (session.running != action) return@launch
                    session.output = if (session.output == container.t("net.running")) chunk else session.output + chunk
                    if (session.output.length > NetEngine.MAX_OUTPUT_BYTES) {
                        session.output = session.output.take(NetEngine.MAX_OUTPUT_BYTES)
                    }
                    container.sessionManager.bump()
                }
            }
            withContext(Dispatchers.Main) {
                if (session.running != action) return@withContext
                session.running = null
                val localized = result.errorCode?.let { errorMessage(container, it, result.output) }
                session.output = when {
                    result.errorCode == NetworkErrorCode.ABORTED && result.output in setOf("ABORTED", "") ->
                        container.t("net.error.ABORTED")
                    result.output.isBlank() && result.errorCode == null -> container.t("net.noOutput")
                    else -> result.output
                }
                session.error = if (result.errorCode != null && result.errorCode != NetworkErrorCode.ABORTED) {
                    localized ?: result.output
                } else ""
                session.notice = action.name
                val historyOutput = session.output.take(8_000)
                container.history.save(
                    ToolId.Net.id,
                    action.name,
                    "${action.name} ${target.orEmpty()} ${ports.orEmpty()}".trim(),
                    target.orEmpty(),
                    historyOutput,
                    action.name
                )
                persist()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (session.ipv4Addresses.isEmpty() && session.ipv6Addresses.isEmpty()) refreshAddresses()
    }
    LaunchedEffect(session.historyOpen, revision) {
        if (session.historyOpen) historyItems = container.history.list(ToolId.Net.id)
    }
    LaunchedEffect(session.output, session.running) {
        if (session.running != null) outputScroll.animateScrollTo(outputScroll.maxValue)
    }
    DisposableEffect(Unit) {
        onDispose {
            if (!container.sessionManager.isDetached(ToolId.Net)) {
                handle.cancel()
                session.running = null
            }
        }
    }

    Column(Modifier.fillMaxSize().background(colors.workspace)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("net.title"), color = colors.textPrimary, fontSize = 16.sp)
            if (revision < 0) Spacer(Modifier.width(0.dp))
            Spacer(Modifier.weight(1f))
            MooButton(container.t("common.action.history"), onClick = { session.historyOpen = true; persist() })
            if (!detached) {
                MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.Net) })
            }
        }
        if (session.error.isNotEmpty()) {
            Text(session.error, color = colors.danger, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 12.sp)
        }
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier.weight(1.15f).fillMaxHeight().padding(12.dp)
                    .clip(RoundedCornerShape(12.dp)).background(colors.surfaceSubtle).border(1.dp, colors.border, RoundedCornerShape(12.dp))
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MooButton(
                        NetEngine.interfacesCommandLabel(),
                        onClick = { runAction(NetworkAction.Interfaces) },
                        enabled = session.running == null
                    )
                    MooButton(
                        "netstat",
                        onClick = { runAction(NetworkAction.Connections) },
                        enabled = session.running == null
                    )
                    Spacer(Modifier.weight(1f))
                    if (session.running != null) {
                        MooButton(container.t("common.stop"), onClick = { stop() })
                    }
                    MooButton(container.t("common.action.copy"), onClick = {
                        session.notice = copyText(session.output, container)
                        persist()
                    }, enabled = session.output.isNotBlank())
                    MooButton(container.t("common.action.clear"), onClick = {
                        session.output = ""
                        session.error = ""
                        persist()
                    }, enabled = session.output.isNotBlank())
                }
                SelectionContainer {
                    Text(
                        session.output.ifBlank { container.t("net.outputPlaceholder") },
                        color = if (session.output.isBlank()) colors.textSecondary else colors.textPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxSize().verticalScroll(outputScroll).padding(12.dp)
                    )
                }
            }
            Column(
                Modifier.weight(0.85f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Section(container.t("net.ipv4Long")) {
                    LabeledField("IPv4", session.ipv4) { session.ipv4 = it; persist() }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MooButton("${container.t("common.convert")} ↓", onClick = {
                            runCatching { NetEngine.ipv4ToLong(session.ipv4).toString() }
                                .onSuccess { value ->
                                    session.longValue = value
                                    session.error = ""
                                    container.history.save(ToolId.Net.id, "ipv4-to-long", value, session.ipv4, value)
                                    persist()
                                }
                                .onFailure {
                                    session.error = convertMessage(container, it)
                                    persist()
                                }
                        })
                        MooButton("↑ ${container.t("common.convert")}", onClick = {
                            runCatching { NetEngine.longToIpv4(session.longValue) }
                                .onSuccess { value ->
                                    session.ipv4 = value
                                    session.error = ""
                                    container.history.save(ToolId.Net.id, "long-to-ipv4", value, session.longValue, value)
                                    persist()
                                }
                                .onFailure {
                                    session.error = convertMessage(container, it)
                                    persist()
                                }
                        })
                    }
                    LabeledField("Long", session.longValue) { session.longValue = it; persist() }
                }
                CommandSection(
                    title = container.t("net.ping"),
                    value = session.pingTarget,
                    button = "PING",
                    disabled = session.running != null,
                    placeholder = null,
                    onChange = { session.pingTarget = it; persist() },
                    onRun = { runAction(NetworkAction.Ping, session.pingTarget) }
                )
                CommandSection(
                    title = container.t("net.ipRangeScan"),
                    value = session.ipRange,
                    button = container.t("net.scan"),
                    disabled = session.running != null,
                    placeholder = container.t("net.ipRangePlaceholder"),
                    onChange = { session.ipRange = it; persist() },
                    onRun = { runAction(NetworkAction.PingRange, session.ipRange) }
                )
                Text(container.t("net.ipRangeHint"), color = colors.textSecondary, fontSize = 11.sp)
                Section(container.t("net.portScan")) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Enter && session.running == null) {
                                runAction(NetworkAction.PortScan, session.portScanTarget, session.portSpec)
                                true
                            } else false
                        }
                    ) {
                        MooTextField(
                            session.portScanTarget,
                            { session.portScanTarget = it; persist() },
                            modifier = Modifier.weight(1f),
                            placeholder = container.t("net.portScanTargetPlaceholder")
                        )
                        MooTextField(
                            session.portSpec,
                            { session.portSpec = it; persist() },
                            modifier = Modifier.weight(1f),
                            placeholder = container.t("net.portScanPortsPlaceholder")
                        )
                        MooButton(container.t("net.scan"), onClick = {
                            runAction(NetworkAction.PortScan, session.portScanTarget, session.portSpec)
                        }, enabled = session.running == null)
                    }
                    Text(container.t("net.portScanHint"), color = colors.textSecondary, fontSize = 11.sp)
                }
                CommandSection(
                    title = container.t("net.resolve"),
                    value = session.hostTarget,
                    button = container.t("net.resolveAction"),
                    disabled = session.running != null,
                    placeholder = null,
                    onChange = { session.hostTarget = it; persist() },
                    onRun = { runAction(NetworkAction.Resolve, session.hostTarget) }
                )
                CommandSection(
                    title = container.t("net.whois"),
                    value = session.whoisTarget,
                    button = container.t("net.query"),
                    disabled = session.running != null,
                    placeholder = null,
                    onChange = { session.whoisTarget = it; persist() },
                    onRun = { runAction(NetworkAction.Whois, session.whoisTarget) }
                )
                Section(container.t("net.dns")) {
                    MooButton(
                        container.t("net.flushDns"),
                        onClick = { runAction(NetworkAction.FlushDns) },
                        enabled = session.running == null
                    )
                }
                Section(container.t("net.localAddresses")) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("IPv4", color = colors.textSecondary, fontSize = 12.sp)
                            MooTextField(session.ipv4Addresses, {}, modifier = Modifier.fillMaxWidth(), singleLine = false)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("IPv6", color = colors.textSecondary, fontSize = 12.sp)
                            MooTextField(session.ipv6Addresses, {}, modifier = Modifier.fillMaxWidth(), singleLine = false)
                        }
                    }
                    MooButton(container.t("common.refresh"), onClick = { refreshAddresses() })
                }
            }
        }
    }
    if (session.historyOpen) {
        NetHistoryDialog(container, session, historyItems) { persist() }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val colors = MooTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.surfaceSubtle)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, color = colors.textPrimary, fontSize = 14.sp)
        content()
    }
}

@Composable
private fun CommandSection(
    title: String,
    value: String,
    button: String,
    disabled: Boolean,
    placeholder: String?,
    onChange: (String) -> Unit,
    onRun: () -> Unit
) {
    Section(title) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Enter && !disabled) {
                    onRun()
                    true
                } else false
            }
        ) {
            MooTextField(value, onChange, modifier = Modifier.weight(1f), placeholder = placeholder.orEmpty())
            MooButton(button, onClick = onRun, enabled = !disabled)
        }
    }
}

@Composable
private fun LabeledField(label: String, value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = MooTheme.colors.textSecondary, fontSize = 12.sp)
        MooTextField(value, onChange, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun NetHistoryDialog(
    container: AppContainer,
    session: NetSession,
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
                            session.output = item.output
                            when (item.operation) {
                                "ipv4-to-long" -> {
                                    session.ipv4 = item.input
                                    session.longValue = item.output
                                }
                                "long-to-ipv4" -> {
                                    session.longValue = item.input
                                    session.ipv4 = item.output
                                }
                                else -> if (item.input.isNotBlank()) {
                                    when (item.options) {
                                        NetworkAction.Ping.name -> session.pingTarget = item.input
                                        NetworkAction.PingRange.name -> session.ipRange = item.input
                                        NetworkAction.PortScan.name -> session.portScanTarget = item.input
                                        NetworkAction.Resolve.name -> session.hostTarget = item.input
                                        NetworkAction.Whois.name -> session.whoisTarget = item.input
                                    }
                                }
                            }
                            session.historyOpen = false
                            onChanged()
                        }.padding(8.dp)) {
                            Text(item.summary, color = MooTheme.colors.textPrimary, fontSize = 13.sp)
                            Text(item.output.take(80), color = MooTheme.colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.clear"), onClick = {
                    container.history.clear(ToolId.Net.id)
                    onChanged()
                })
                MooButton(container.t("common.close"), onClick = { session.historyOpen = false; onChanged() })
            }
        }
    }
}

private fun errorMessage(container: AppContainer, code: NetworkErrorCode, raw: String): String {
    val key = "net.error.${code.name}"
    val localized = container.t(key)
    return if (raw.isNotBlank() && raw != code.name && raw != "INVALID_TARGET" && raw != "ABORTED" && raw != "TIMEOUT" && raw != "UNSUPPORTED") {
        "$localized\n$raw"
    } else localized
}

private fun convertMessage(container: AppContainer, error: Throwable): String {
    val message = error.message.orEmpty()
    return when {
        error is NetConvertException && message.contains("address") -> container.t("net.error.invalidIpv4")
        error is NetConvertException && message.contains("number") -> container.t("net.error.invalidLong")
        else -> error.message ?: container.t("net.error.COMMAND_FAILED")
    }
}

private fun copyText(value: String, container: AppContainer): String {
    return try {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
        container.t("json.notice.copied")
    } catch (_: Exception) {
        container.t("common.copyFailed")
    }
}
