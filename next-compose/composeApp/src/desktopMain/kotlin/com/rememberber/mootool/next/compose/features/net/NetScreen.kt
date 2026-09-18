package com.rememberber.mootool.next.compose.features.net

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.NetCommandHandle
import com.rememberber.mootool.next.compose.domain.NetConvertException
import com.rememberber.mootool.next.compose.domain.NetEngine
import com.rememberber.mootool.next.compose.domain.NetHistoryMetadata
import com.rememberber.mootool.next.compose.domain.NetWiringPresentation
import com.rememberber.mootool.next.compose.domain.NetHistoryRestore
import com.rememberber.mootool.next.compose.domain.NetworkAction
import com.rememberber.mootool.next.compose.domain.NetworkErrorCode
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.mooNetCommandRow
import com.rememberber.mootool.next.compose.ui.components.mooNetOutputMonospace
import com.rememberber.mootool.next.compose.ui.components.mooNetPortScanRow
import com.rememberber.mootool.next.compose.ui.components.mooNetSection
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.mooToolShell
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.VerticalPaneHandle
import com.rememberber.mootool.next.compose.ui.components.setPaneSize
import com.rememberber.mootool.next.compose.ui.components.OverflowAction
import com.rememberber.mootool.next.compose.ui.components.OverflowActionCluster
import com.rememberber.mootool.next.compose.ui.components.rememberFollowTailScroll
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import com.rememberber.mootool.next.compose.ui.workbench.blockedByIme
import com.rememberber.mootool.next.compose.sessions.NetSession
import com.rememberber.mootool.next.compose.sessions.dismissModalOverlays
import com.rememberber.mootool.next.compose.ui.workbench.DismissModalOverlaysOnDispose
import com.rememberber.mootool.next.compose.ui.workbench.OnToolLeaveUnlessDetached
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NetScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.netSession() }
    DismissModalOverlaysOnDispose(container, ToolId.Net) { session.dismissModalOverlays() }
    val revision by container.sessionManager.revision.collectAsState()
    val settings by container.settings.collectAsState()
    val colors = MooTheme.colors
    val scope = rememberCoroutineScope()
    var commandJob by remember { mutableStateOf<Job?>(null) }
    var handle by remember { mutableStateOf(NetCommandHandle()) }
    val outputScroll = rememberFollowTailScroll(
        contentKey = session.output.length,
        resetPinKey = session.running
    )

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
            val snapshot = runCatching { NetWiringPresentation.runLocalAddresses() }
            withContext(Dispatchers.Main) {
                snapshot.onSuccess {
                    session.ipv4Addresses = it.ipv4.joinToString("\n")
                    session.ipv6Addresses = it.ipv6.joinToString("\n")
                    session.error = ""
                }.onFailure {
                    notifyNetFailure(
                        container,
                        session,
                        it.message ?: container.t("net.error.COMMAND_FAILED"),
                    ) { persist() }
                }
                persist()
            }
        }
    }

    fun runAction(action: NetworkAction, target: String? = null, ports: String? = null) {
        if (session.running != null) return
        val resolvedTarget: String?
        val resolvedPorts: String?
        if (action == NetworkAction.PortScan) {
            when (val start = NetWiringPresentation.portScanStart(target.orEmpty(), ports.orEmpty())) {
                is NetWiringPresentation.PortScanStart.Blocked -> {
                    notifyNetFailure(
                        container,
                        session,
                        errorMessage(container, start.errorCode, ""),
                    ) { persist() }
                    return
                }
                is NetWiringPresentation.PortScanStart.Ready -> {
                    resolvedTarget = start.target
                    resolvedPorts = start.portSpec
                }
            }
        } else {
            val hostStart = when (action) {
                NetworkAction.Ping -> NetWiringPresentation.pingStart(target.orEmpty())
                NetworkAction.PingRange -> NetWiringPresentation.ipRangeStart(target.orEmpty())
                NetworkAction.Resolve -> NetWiringPresentation.resolveStart(target.orEmpty())
                NetworkAction.Whois -> NetWiringPresentation.whoisStart(target.orEmpty())
                else -> null
            }
            if (hostStart is NetWiringPresentation.HostCommandStart.Blocked) {
                notifyNetFailure(
                    container,
                    session,
                    errorMessage(container, hostStart.errorCode, ""),
                ) { persist() }
                return
            }
            if (hostStart is NetWiringPresentation.HostCommandStart.Ready) {
                resolvedTarget = hostStart.target
            } else {
                resolvedTarget = target
            }
            resolvedPorts = ports
        }
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
            val result = NetEngine.run(action, resolvedTarget, resolvedPorts, timeout, current) { chunk ->
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
                val label = networkActionLabel(container, action)
                session.notice = label
                notifyNetActionResult(container, session, result.errorCode, label)
                val historyOutput = session.output.take(8_000)
                val wire = NetHistoryMetadata.actionWireId(action)
                val summary = "${label} ${resolvedTarget.orEmpty()} ${resolvedPorts.orEmpty()}".trim()
                val historyOptions = if (action == NetworkAction.PortScan) {
                    NetHistoryMetadata.encodePortSpec(resolvedPorts)
                } else {
                    ""
                }
                container.history.save(
                    ToolId.Net.id,
                    wire,
                    summary,
                    resolvedTarget.orEmpty(),
                    historyOutput,
                    historyOptions,
                )
                persist()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (session.ipv4Addresses.isEmpty() && session.ipv6Addresses.isEmpty()) refreshAddresses()
    }
    OnToolLeaveUnlessDetached(container, ToolId.Net) {
        handle.cancel()
        session.running = null
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.workspace)) {
        val overflow = LayoutPolicy.overflowToolbar(maxWidth.value)
        val minOutputPane = 340f
        val minToolsPane = 300f
        val paneHandle = 10f
        val innerWidth = maxWidth.value - 24f
        val maxOutputWidth = (innerWidth - paneHandle - minToolsPane).coerceAtLeast(minOutputPane)
        val defaultOutputWidth = (innerWidth * (1.15f / 2f)).coerceIn(minOutputPane, maxOutputWidth)
        val outputWidth = settings.layout.pane(ToolId.Net.id, 0, defaultOutputWidth, minOutputPane, maxOutputWidth)
        Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MooPageTitle(container.t("net.title"))
            if (revision < 0) Spacer(Modifier.width(0.dp))
            Spacer(Modifier.weight(1f))
            OverflowActionCluster(
                overflow = overflow,
                moreLabel = container.t("json.action.overflow"),
                actions = buildList {
                    add(OverflowAction(container.t("common.action.history")) { session.historyOpen = true; persist() })
                    if (!detached) add(OverflowAction(container.t("app.tool.detach")) { container.sessionManager.detach(ToolId.Net) })
                }
            )
        }
        if (session.error.isNotEmpty()) {
            Text(session.error, color = colors.danger, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 12.sp)
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .mooToolShell(p5 = true, endBorder = false)
        ) {
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(outputWidth.dp).widthIn(min = 340.dp).fillMaxHeight()) {
                Row(
                    Modifier.fillMaxWidth().height(46.dp).background(colors.toolbar).padding(horizontal = 9.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MooButton(
                        NetEngine.interfacesCommandLabel(),
                        onClick = { runAction(NetworkAction.Interfaces) },
                        enabled = NetWiringPresentation.runCommandEnabled(idle = session.running == null, startReady = true),
                        p5Toolbar = true
                    )
                    MooButton(
                        "netstat",
                        onClick = { runAction(NetworkAction.Connections) },
                        enabled = NetWiringPresentation.runCommandEnabled(idle = session.running == null, startReady = true),
                        p5Toolbar = true
                    )
                    Spacer(Modifier.weight(1f))
                    if (NetWiringPresentation.stopEnabled(session.running != null)) {
                        MooButton(container.t("common.stop"), onClick = { stop() }, p5Toolbar = true)
                    }
                    OverflowActionCluster(
                        overflow = overflow,
                        moreLabel = container.t("json.action.overflow"),
                        actions = listOf(
                            OverflowAction(
                                container.t("common.action.copy"),
                                enabled = NetWiringPresentation.outputActionsEnabled(session.output.isNotBlank()),
                            ) {
                                session.notice = copyText(session.output, container)
                                persist()
                            },
                            OverflowAction(
                                container.t("common.action.clear"),
                                enabled = NetWiringPresentation.outputActionsEnabled(session.output.isNotBlank()),
                            ) {
                                session.output = ""
                                session.error = ""
                                persist()
                            }
                        )
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSoft))
                SelectionContainer(Modifier.weight(1f).fillMaxWidth()) {
                    Text(
                        session.output.ifBlank { container.t("net.outputPlaceholder") },
                        color = if (session.output.isBlank()) colors.textMuted else colors.textBody,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxSize().verticalScroll(outputScroll).mooNetOutputMonospace()
                    )
                }
            }
            VerticalPaneHandle(
                onDelta = { container.setPaneSize(ToolId.Net.id, 0, outputWidth + it, 1) },
                onReset = { container.setPaneSize(ToolId.Net.id, 0, defaultOutputWidth, 1) }
            )
            Column(
                Modifier.weight(1f).widthIn(min = 300.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 16.dp)
            ) {
                Section(container.t("net.ipv4Long")) {
                    LabeledField("IPv4", session.ipv4) { session.ipv4 = it; persist() }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MooButton(
                            "${container.t("common.convert")} ↓",
                            p5Toolbar = true,
                            enabled = NetWiringPresentation.ipv4ToLongActionEnabled(session.ipv4),
                            onClick = {
                            runCatching { NetEngine.ipv4ToLong(session.ipv4).toString() }
                                .onSuccess { value ->
                                    session.longValue = value
                                    session.error = ""
                                    container.history.save(
                                        ToolId.Net.id,
                                        NetHistoryMetadata.OP_IPV4_TO_LONG,
                                        value,
                                        session.ipv4,
                                        value,
                                    )
                                    persist()
                                }
                                .onFailure {
                                    notifyNetFailure(container, session, convertMessage(container, it)) { persist() }
                                }
                        })
                        MooButton(
                            "↑ ${container.t("common.convert")}",
                            p5Toolbar = true,
                            enabled = NetWiringPresentation.longToIpv4ActionEnabled(session.longValue),
                            onClick = {
                            runCatching { NetEngine.longToIpv4(session.longValue) }
                                .onSuccess { value ->
                                    session.ipv4 = value
                                    session.error = ""
                                    container.history.save(
                                        ToolId.Net.id,
                                        NetHistoryMetadata.OP_LONG_TO_IPV4,
                                        value,
                                        session.longValue,
                                        value,
                                    )
                                    persist()
                                }
                                .onFailure {
                                    notifyNetFailure(container, session, convertMessage(container, it)) { persist() }
                                }
                        })
                    }
                    LabeledField("Long", session.longValue) { session.longValue = it; persist() }
                }
                CommandSection(
                    title = container.t("net.ping"),
                    value = session.pingTarget,
                    button = "PING",
                    runEnabled = NetWiringPresentation.runCommandEnabled(
                        idle = session.running == null,
                        startReady = NetWiringPresentation.pingStart(session.pingTarget)
                            is NetWiringPresentation.HostCommandStart.Ready,
                    ),
                    placeholder = null,
                    onChange = { session.pingTarget = it; persist() },
                    onRun = { runAction(NetworkAction.Ping, session.pingTarget) }
                )
                CommandSection(
                    title = container.t("net.ipRangeScan"),
                    value = session.ipRange,
                    button = container.t("net.scan"),
                    runEnabled = NetWiringPresentation.runCommandEnabled(
                        idle = session.running == null,
                        startReady = NetWiringPresentation.ipRangeStart(session.ipRange)
                            is NetWiringPresentation.HostCommandStart.Ready,
                    ),
                    placeholder = container.t("net.ipRangePlaceholder"),
                    onChange = { session.ipRange = it; persist() },
                    onRun = { runAction(NetworkAction.PingRange, session.ipRange) }
                )
                Text(container.t("net.ipRangeHint"), color = colors.textMuted, fontSize = 9.sp, modifier = Modifier.padding(start = 20.dp, top = 2.dp))
                Section(container.t("net.portScan")) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .mooNetPortScanRow()
                            .onPreviewKeyEvent { event ->
                            if (!event.blockedByIme() && event.type == KeyEventType.KeyDown && event.key == Key.Enter && session.running == null) {
                                runAction(NetworkAction.PortScan, session.portScanTarget, session.portSpec)
                                true
                            } else false
                        }
                    ) {
                        MooTextField(
                            session.portScanTarget,
                            { session.portScanTarget = it; persist() },
                            modifier = Modifier.weight(0.8f),
                            placeholder = container.t("net.portScanTargetPlaceholder"),
                            dense = true
                        )
                        MooTextField(
                            session.portSpec,
                            { session.portSpec = it; persist() },
                            modifier = Modifier.weight(1.2f),
                            placeholder = container.t("net.portScanPortsPlaceholder"),
                            dense = true
                        )
                        MooButton(
                            container.t("net.scan"),
                            onClick = { runAction(NetworkAction.PortScan, session.portScanTarget, session.portSpec) },
                            enabled = NetWiringPresentation.runCommandEnabled(
                                idle = session.running == null,
                                startReady = NetWiringPresentation.portScanStart(session.portScanTarget, session.portSpec)
                                    is NetWiringPresentation.PortScanStart.Ready,
                            ),
                            p5Toolbar = true
                        )
                    }
                    Text(container.t("net.portScanHint"), color = colors.textMuted, fontSize = 9.sp, modifier = Modifier.padding(start = 20.dp, top = 2.dp))
                }
                CommandSection(
                    title = container.t("net.resolve"),
                    value = session.hostTarget,
                    button = container.t("net.resolveAction"),
                    runEnabled = NetWiringPresentation.runCommandEnabled(
                        idle = session.running == null,
                        startReady = NetWiringPresentation.resolveStart(session.hostTarget)
                            is NetWiringPresentation.HostCommandStart.Ready,
                    ),
                    placeholder = null,
                    onChange = { session.hostTarget = it; persist() },
                    onRun = { runAction(NetworkAction.Resolve, session.hostTarget) }
                )
                CommandSection(
                    title = container.t("net.whois"),
                    value = session.whoisTarget,
                    button = container.t("net.query"),
                    runEnabled = NetWiringPresentation.runCommandEnabled(
                        idle = session.running == null,
                        startReady = NetWiringPresentation.whoisStart(session.whoisTarget)
                            is NetWiringPresentation.HostCommandStart.Ready,
                    ),
                    placeholder = null,
                    onChange = { session.whoisTarget = it; persist() },
                    onRun = { runAction(NetworkAction.Whois, session.whoisTarget) }
                )
                Section(container.t("net.dns")) {
                    MooButton(
                        container.t("net.flushDns"),
                        onClick = { runAction(NetworkAction.FlushDns) },
                        enabled = NetWiringPresentation.flushDnsActionEnabled(session.running != null),
                        p5Toolbar = true
                    )
                }
                Section(container.t("net.localAddresses")) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("IPv4", color = colors.textMuted, fontSize = 9.sp)
                            MooTextField(session.ipv4Addresses, {}, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp), singleLine = false, code = true)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("IPv6", color = colors.textMuted, fontSize = 9.sp)
                            MooTextField(session.ipv6Addresses, {}, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp), singleLine = false, code = true)
                        }
                    }
                    MooButton(
                        container.t("common.refresh"),
                        onClick = { refreshAddresses() },
                        enabled = NetWiringPresentation.refreshLocalAddressesActionEnabled(session.running != null),
                        p5Toolbar = true,
                    )
                }
            }
        }
        }
    }
    }
    if (session.historyOpen) HistoryBrowser(
        container = container,
        toolId = ToolId.Net.id,
        title = container.t("common.action.history"),
        onRestore = { item ->
            NetHistoryRestore.apply(session, item)
            session.historyOpen = false
            persist()
        },
        onDismiss = { session.historyOpen = false; persist() }
    )
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val colors = MooTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().mooNetSection(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        content()
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSoft))
    }
}

@Composable
private fun CommandSection(
    title: String,
    value: String,
    button: String,
    runEnabled: Boolean,
    placeholder: String?,
    onChange: (String) -> Unit,
    onRun: () -> Unit
) {
    Section(title) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.mooNetCommandRow().onPreviewKeyEvent { event ->
                if (!event.blockedByIme() && event.type == KeyEventType.KeyDown && event.key == Key.Enter && runEnabled) {
                    onRun()
                    true
                } else false
            }
        ) {
            MooTextField(value, onChange, modifier = Modifier.weight(1f), placeholder = placeholder.orEmpty(), dense = true)
            MooButton(button, onClick = onRun, enabled = runEnabled, p5Toolbar = true)
        }
    }
}

@Composable
private fun LabeledField(label: String, value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = MooTheme.colors.textMuted, fontSize = 9.sp)
        MooTextField(value, onChange, modifier = Modifier.fillMaxWidth(), dense = true)
    }
}

private fun networkActionLabel(container: AppContainer, action: NetworkAction): String = when (action) {
    NetworkAction.Interfaces -> "ifconfig"
    NetworkAction.Connections -> "netstat"
    NetworkAction.Ping -> container.t("net.ping")
    NetworkAction.PingRange -> container.t("net.ipRangeScan")
    NetworkAction.PortScan -> container.t("net.portScan")
    NetworkAction.FlushDns -> container.t("net.flushDns")
    NetworkAction.Resolve -> container.t("net.resolve")
    NetworkAction.Whois -> container.t("net.whois")
}

private fun notifyNetActionResult(
    container: AppContainer,
    session: NetSession,
    errorCode: NetworkErrorCode?,
    successLabel: String,
) {
    if (session.error.isNotEmpty()) {
        if (NetWiringPresentation.shouldToastNetworkError(errorCode)) {
            container.toastError(session.error)
        }
    } else {
        container.toastSuccess(successLabel)
    }
}

private fun notifyNetFailure(
    container: AppContainer,
    session: NetSession,
    message: String,
    onPersist: () -> Unit,
) {
    session.notice = ""
    session.error = message
    if (NetWiringPresentation.shouldToastLocalFailure()) {
        container.toastError(message)
    }
    onPersist()
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
    return if (container.copyText(value)) container.t("json.notice.copied") else container.t("common.copyFailed")
}
