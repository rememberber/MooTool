package com.rememberber.mootool.next.compose.features.ua

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.UaEngine
import com.rememberber.mootool.next.compose.domain.UaWiringPresentation
import com.rememberber.mootool.next.compose.domain.UaException
import com.rememberber.mootool.next.compose.domain.UaHistoryMetadata
import com.rememberber.mootool.next.compose.domain.UaHistoryRestore
import com.rememberber.mootool.next.compose.domain.UaResult
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.UaSession
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.VerticalPaneHandle
import com.rememberber.mootool.next.compose.ui.components.setPaneSize
import com.rememberber.mootool.next.compose.ui.components.MooMenu
import com.rememberber.mootool.next.compose.ui.components.MooMenuItem
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.mooToolShell
import com.rememberber.mootool.next.compose.ui.components.mooUaParseBar
import com.rememberber.mootool.next.compose.ui.components.mooUaResultCell
import com.rememberber.mootool.next.compose.ui.components.mooStatusBarBackground
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.OverflowAction
import com.rememberber.mootool.next.compose.ui.components.OverflowActionCluster
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import com.rememberber.mootool.next.compose.sessions.dismissModalOverlays
import com.rememberber.mootool.next.compose.ui.workbench.DismissModalOverlaysOnDispose
import com.rememberber.mootool.next.compose.ui.workbench.applyUserEditClearingStatusNotice
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun UaParseScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.uaSession() }
    DismissModalOverlaysOnDispose(container, ToolId.UaParse) { session.dismissModalOverlays() }
    val revision by container.sessionManager.revision.collectAsState()
    val settings by container.settings.collectAsState()
    var presetOpen by remember { mutableStateOf(false) }
    val colors = MooTheme.colors

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistUa()
    }


    BoxWithConstraints(Modifier.fillMaxSize().background(colors.workspace)) {
        val overflow = LayoutPolicy.overflowToolbar(maxWidth.value)
        val minPane = 300f
        val handle = 10f
        val innerWidth = maxWidth.value - 24f
        val maxLeft = (innerWidth - handle - minPane).coerceAtLeast(minPane)
        val defaultLeft = (innerWidth * 0.45f).coerceIn(minPane, maxLeft)
        val leftWidth = settings.layout.pane(ToolId.UaParse.id, 0, defaultLeft, minPane, maxLeft)
        Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MooPageTitle(container.t("ua.title"))
            Spacer(Modifier.weight(1f))
            OverflowActionCluster(
                overflow = overflow,
                moreLabel = container.t("json.action.overflow"),
                actions = buildList {
                    add(OverflowAction(container.t("common.action.history")) { session.historyOpen = true; refresh() })
                    if (!detached) add(OverflowAction(container.t("app.tool.detach")) { container.sessionManager.detach(ToolId.UaParse) })
                }
            )
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .mooToolShell(p5 = true, endBorder = false)
        ) {
        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .width(leftWidth.dp)
                    .widthIn(min = 300.dp)
                    .fillMaxHeight()
                    .padding(start = 22.dp, end = 14.dp, top = 14.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(container.t("ua.input"), color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                MooTextField(
                    session.source,
                    {
                        applyUserEditClearingStatusNotice({ session.notice }, { session.notice = it }) {
                            session.source = it
                            session.error = ""
                        }
                        refresh()
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    singleLine = false,
                    borderless = true
                )
                Row(
                    Modifier.fillMaxWidth().mooUaParseBar(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                        MooButton(
                            container.t("ua.preset"),
                            onClick = { presetOpen = true },
                            modifier = Modifier.fillMaxWidth(),
                            p5Toolbar = true
                        )
                        MooMenu(expanded = presetOpen, onDismissRequest = { presetOpen = false }) {
                            UaEngine.presets.forEach { (name, value) ->
                                MooMenuItem(onClick = {
                                    session.source = value
                                    session.error = ""
                                    presetOpen = false
                                    refresh()
                                }) { Text(name, fontSize = 13.sp) }
                            }
                        }
                    }
                    MooButton(
                        container.t("common.action.paste"),
                        onClick = {
                            pasteFromClipboard()?.let { session.source = it; session.error = ""; refresh() }
                                ?: run { session.error = container.t("json.notice.copyFailed"); refresh() }
                        },
                        p5Toolbar = true
                    )
                    MooButton(
                        container.t("common.action.clear"),
                        onClick = {
                            session.source = ""
                            session.result = null
                            session.error = ""
                            refresh()
                        },
                        p5Toolbar = true
                    )
                    MooButton(
                        container.t("ua.parse"),
                        prominent = true,
                        enabled = UaWiringPresentation.canParse(session.source),
                        onClick = {
                            parseSource(container, session)
                            refresh()
                        },
                        p5Toolbar = true
                    )
                }
            }
            VerticalPaneHandle(
                onDelta = { container.setPaneSize(ToolId.UaParse.id, 0, leftWidth + it, 1) },
                onReset = { container.setPaneSize(ToolId.UaParse.id, 0, defaultLeft, 1) }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 300.dp)
                    .fillMaxHeight()
                    .background(colors.borderSoft)
                    .padding(1.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(container.t("common.result"), color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    MooButton(
                        container.t("time.copy"),
                        onClick = {
                            val text = session.result?.let { resultCodec.encodeToString(it) } ?: session.source
                            session.notice = copyText(text, container)
                            refresh()
                        },
                        p5Toolbar = true,
                        enabled = UaWiringPresentation.canCopyResult(
                            session.result?.let { resultCodec.encodeToString(it) } ?: session.source,
                        ),
                    )
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    resultRows(container, session.result).chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                            pair.forEach { (label, value) ->
                                Column(
                                    Modifier.weight(1f).mooUaResultCell().background(colors.workspace).padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Text(label, color = colors.textMuted, fontSize = 10.sp)
                                    Text(value.ifBlank { container.t("ua.unknown") }, color = colors.textBody, fontSize = 13.sp)
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f).mooUaResultCell().background(colors.workspace))
                        }
                    }
                }
            }
        }
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
            toolId = ToolId.UaParse.id,
            title = container.t("common.action.history"),
            onRestore = { item ->
                UaHistoryRestore.apply(session, item)
                session.historyOpen = false
                refresh()
            },
            onDismiss = { session.historyOpen = false; refresh() }
        )
    }
}

private val resultCodec = Json { ignoreUnknownKeys = true; encodeDefaults = true }

private fun parseSource(container: AppContainer, session: UaSession) {
    when (val outcome = UaWiringPresentation.runParse(session.source)) {
        is UaWiringPresentation.ParseOutcome.Success -> {
            val result = outcome.result
            session.result = result
            session.error = ""
            session.notice = container.t("ua.parse")
            container.toastSuccess(session.notice)
            container.history.save(
                ToolId.UaParse.id,
                container.t("ua.title"),
                container.t("ua.title"),
                session.source,
                resultCodec.encodeToString(result),
                UaHistoryMetadata.encode(),
            )
        }
        is UaWiringPresentation.ParseOutcome.Failure -> {
            val error = outcome.error
            session.notice = ""
            val message = if ((error as? UaException)?.code == "empty") container.t("ua.empty") else (error.message ?: container.t("ua.empty"))
            notifyUaFailure(container, session, message, error)
        }
    }
}

private fun notifyUaFailure(
    container: AppContainer,
    session: UaSession,
    message: String,
    error: Throwable,
) {
    session.error = message
    if (UaWiringPresentation.shouldToastParseFailure(error)) {
        container.toastError(message)
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


private fun pasteFromClipboard(): String? = try {
    Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String
} catch (_: Exception) {
    null
}

private fun copyText(value: String, container: AppContainer): String {
    return if (container.copyText(value)) container.t("time.notice.copied") else container.t("json.notice.copyFailed")
}
