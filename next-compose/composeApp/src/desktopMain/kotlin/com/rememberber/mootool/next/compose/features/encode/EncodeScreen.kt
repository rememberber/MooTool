package com.rememberber.mootool.next.compose.features.encode

import androidx.compose.foundation.background
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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import com.rememberber.mootool.next.compose.domain.AsciiFormat
import com.rememberber.mootool.next.compose.domain.EncodeException
import com.rememberber.mootool.next.compose.domain.EncodeTab
import com.rememberber.mootool.next.compose.domain.UrlCharset
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.EncodeSession
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooMenu
import com.rememberber.mootool.next.compose.ui.components.MooMenuItem
import com.rememberber.mootool.next.compose.ui.components.MooToolTab
import com.rememberber.mootool.next.compose.ui.components.MooToolTabsRow
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.VerticalPaneHandle
import com.rememberber.mootool.next.compose.ui.components.setPaneSize
import com.rememberber.mootool.next.compose.ui.components.mooEncodeControlColumn
import com.rememberber.mootool.next.compose.ui.components.mooEncodeConvertButton
import com.rememberber.mootool.next.compose.ui.components.mooToolShell
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.mooStatusBarBackground
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import com.rememberber.mootool.next.compose.sessions.dismissModalOverlays
import com.rememberber.mootool.next.compose.ui.workbench.DismissModalOverlaysOnDispose
import com.rememberber.mootool.next.compose.ui.workbench.applyUserEditClearingStatusNotice
import com.rememberber.mootool.next.compose.domain.EncodeHistoryMetadata
import com.rememberber.mootool.next.compose.domain.EncodeHistoryRestore
import com.rememberber.mootool.next.compose.domain.EncodeWiringPresentation

@Composable
fun EncodeScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.encodeSession() }
    DismissModalOverlaysOnDispose(container, ToolId.Encode) { session.dismissModalOverlays() }
    val settings by container.settings.collectAsState()
    val colors = MooTheme.colors
    val labels = tabLabels(container, session.tab)
    var charsetOpen by remember { mutableStateOf(false) }
    var asciiOpen by remember { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistEncode()
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.workspace)) {
        val contentMaxWidth = maxWidth.value
        val overflow = LayoutPolicy.overflowToolbar(contentMaxWidth)
        Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MooPageTitle(container.t("encode.title"))
            Spacer(Modifier.weight(1f))
            if (!overflow) {
                MooButton(container.t("common.action.history"), onClick = { session.historyOpen = true; refresh() })
                MooButton(container.t("common.clear"), onClick = { session.clearCurrent(); refresh() })
                if (!detached) {
                    MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.Encode) })
                }
            } else {
                Box {
                    MooButton(container.t("json.action.overflow"), primary = moreOpen, onClick = { moreOpen = true })
                    MooMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                        MooMenuItem(onClick = { moreOpen = false; session.historyOpen = true; refresh() }) {
                            Text(container.t("common.action.history"))
                        }
                        MooMenuItem(onClick = {
                            moreOpen = false
                            session.clearCurrent()
                            refresh()
                        }) {
                            Text(container.t("common.clear"))
                        }
                        if (!detached) {
                            MooMenuItem(onClick = { moreOpen = false; container.sessionManager.detach(ToolId.Encode) }) {
                                Text(container.t("app.tool.detach"))
                            }
                        }
                    }
                }
            }
        }
        MooToolTabsRow {
            EncodeTab.entries.forEach { tab ->
                MooToolTab(container.t(tabTitleKey(tab)), selected = session.tab == tab, onClick = {
                    session.tab = tab
                    session.error = ""
                    refresh()
                })
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
            val innerWidth = maxWidth.value
            val minLeft = 240f
            val minMiddle = 120f
            val minRight = 240f
            val paneHandle = 10f
            val ratioSum = 1f + 0.32f + 1f
            val defaultLeft = (innerWidth * (1f / ratioSum)).coerceIn(minLeft, innerWidth)
            val defaultMiddle = (innerWidth * (0.32f / ratioSum)).coerceIn(minMiddle, 280f)
            val leftWidth = settings.layout.pane(ToolId.Encode.id, 0, defaultLeft, minLeft, innerWidth - 2f * paneHandle - minMiddle - minRight)
            val maxMiddle = (innerWidth - 2f * paneHandle - leftWidth - minRight).coerceAtLeast(minMiddle)
            val middleWidth = settings.layout.pane(ToolId.Encode.id, 1, defaultMiddle, minMiddle, maxMiddle)
            Row(Modifier.fillMaxSize()) {
            Column(
                Modifier.width(leftWidth.dp).widthIn(min = 240.dp).fillMaxHeight().mooToolShell().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(labels.left, color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                MooTextField(
                    session.left(),
                    {
                        applyUserEditClearingStatusNotice({ session.notice }, { session.notice = it }) {
                            session.setLeft(it)
                            session.error = ""
                        }
                        refresh()
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    singleLine = false
                )
            }
            VerticalPaneHandle(
                onDelta = { container.setPaneSize(ToolId.Encode.id, 0, leftWidth + it, 2) },
                onReset = { container.setPaneSize(ToolId.Encode.id, 0, defaultLeft, 2) }
            )
            Column(
                modifier = Modifier.width(middleWidth.dp).fillMaxHeight().mooEncodeControlColumn(),
                verticalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MooButton(
                    labels.forward,
                    prominent = true,
                    p5Toolbar = true,
                    enabled = EncodeWiringPresentation.canConvert(session.left()),
                    modifier = Modifier.mooEncodeConvertButton(),
                    onClick = {
                        convert(container, session, forward = true)
                        refresh()
                    },
                )
                MooButton(
                    labels.reverse,
                    p5Toolbar = true,
                    enabled = EncodeWiringPresentation.canConvert(session.right()),
                    modifier = Modifier.mooEncodeConvertButton(),
                    onClick = {
                        convert(container, session, forward = false)
                        refresh()
                    },
                )
                if (session.tab == EncodeTab.Url) {
                    Text(container.t("encode.charset"), color = colors.textSecondary, fontSize = 12.sp)
                    Box {
                        MooButton(if (session.charset == UrlCharset.Utf8) "UTF-8" else "GB2312", onClick = { charsetOpen = true }, p5Toolbar = true)
                        MooMenu(expanded = charsetOpen, onDismissRequest = { charsetOpen = false }) {
                            MooMenuItem(onClick = {
                                charsetOpen = false
                                session.charset = UrlCharset.Utf8
                                refresh()
                            }) { Text("UTF-8") }
                            MooMenuItem(onClick = {
                                charsetOpen = false
                                session.charset = UrlCharset.Gb2312
                                refresh()
                            }) { Text("GB2312") }
                        }
                    }
                }
                if (session.tab == EncodeTab.Ascii) {
                    Box {
                        MooButton(
                            if (session.asciiFormat == AsciiFormat.Decimal) container.t("encode.asciiDecimal") else container.t("encode.asciiHex"),
                            onClick = { asciiOpen = true },
                            p5Toolbar = true
                        )
                        MooMenu(expanded = asciiOpen, onDismissRequest = { asciiOpen = false }) {
                            MooMenuItem(onClick = {
                                asciiOpen = false
                                session.asciiFormat = AsciiFormat.Decimal
                                refresh()
                            }) { Text(container.t("encode.asciiDecimal")) }
                            MooMenuItem(onClick = {
                                asciiOpen = false
                                session.asciiFormat = AsciiFormat.Hex
                                refresh()
                            }) { Text(container.t("encode.asciiHex")) }
                        }
                    }
                }
            }
            VerticalPaneHandle(
                onDelta = { container.setPaneSize(ToolId.Encode.id, 1, middleWidth + it, 2) },
                onReset = { container.setPaneSize(ToolId.Encode.id, 1, defaultMiddle, 2) }
            )
            Column(
                Modifier.weight(1f).widthIn(min = 240.dp).fillMaxHeight().mooToolShell().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(labels.right, color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                MooTextField(
                    session.right(),
                    {
                        applyUserEditClearingStatusNotice({ session.notice }, { session.notice = it }) {
                            session.setRight(it)
                            session.error = ""
                        }
                        refresh()
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    singleLine = false
                )
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
            toolId = ToolId.Encode.id,
            title = container.t("common.action.history"),
            onRestore = { item ->
                EncodeHistoryRestore.apply(session, item)
                session.historyOpen = false
                refresh()
            },
            onDismiss = { session.historyOpen = false; refresh() }
        )
    }
}

private data class EncodeLabels(val left: String, val right: String, val forward: String, val reverse: String)

private fun tabTitleKey(tab: EncodeTab): String = when (tab) {
    EncodeTab.Unicode -> "encode.tab.unicode"
    EncodeTab.Url -> "encode.tab.url"
    EncodeTab.Hex -> "encode.tab.hex"
    EncodeTab.Ascii -> "encode.tab.ascii"
}

private fun tabLabels(container: AppContainer, tab: EncodeTab): EncodeLabels = when (tab) {
    EncodeTab.Unicode -> EncodeLabels(container.t("encode.native"), container.t("encode.unicode"), container.t("encode.toUnicode"), container.t("encode.fromUnicode"))
    EncodeTab.Url -> EncodeLabels(container.t("encode.url"), container.t("encode.encoded"), container.t("encode.urlEncode"), container.t("encode.urlDecode"))
    EncodeTab.Hex -> EncodeLabels(container.t("encode.native"), container.t("encode.hex"), container.t("encode.toHex"), container.t("encode.fromHex"))
    EncodeTab.Ascii -> EncodeLabels(container.t("encode.native"), container.t("encode.ascii"), container.t("encode.toAscii"), container.t("encode.fromAscii"))
}

private fun convert(container: AppContainer, session: EncodeSession, forward: Boolean) {
    val input = if (forward) session.left() else session.right()
    val labels = tabLabels(container, session.tab)
    val summary = if (forward) labels.forward else labels.reverse
    when (
        val outcome = EncodeWiringPresentation.runConvert(
            session.tab,
            forward,
            input,
            session.charset,
            session.asciiFormat,
        )
    ) {
        is EncodeWiringPresentation.ConvertOutcome.Success -> {
            val output = outcome.output
            if (forward) session.setRight(output) else session.setLeft(output)
            session.error = ""
            session.notice = summary
            container.toastSuccess(summary)
            val options = EncodeHistoryMetadata.encode(session.tab, forward, session.charset, session.asciiFormat)
            container.history.save(ToolId.Encode.id, summary, summary, input, output, options)
        }
        is EncodeWiringPresentation.ConvertOutcome.Failure -> {
            session.notice = ""
            val message = messageFor(container, outcome.error)
            session.error = message
            container.toastError(message)
        }
    }
}

private fun messageFor(container: AppContainer, error: Throwable): String {
    val code = (error as? EncodeException)?.code
    return when (code) {
        "invalid-hex" -> container.t("encode.error.hex")
        "invalid-bytes" -> container.t("encode.error.bytes")
        "unmappable" -> container.t("encode.error.unmappable")
        "invalid-code-point" -> container.t("encode.error.codePoint", mapOf("value" to (error.message ?: "")))
        else -> error.message ?: container.t("encode.error.generic")
    }
}

