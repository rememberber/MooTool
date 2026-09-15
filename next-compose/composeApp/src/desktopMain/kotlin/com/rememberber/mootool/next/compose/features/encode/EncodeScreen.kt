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
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.AsciiFormat
import com.rememberber.mootool.next.compose.domain.EncodeEngine
import com.rememberber.mootool.next.compose.domain.EncodeException
import com.rememberber.mootool.next.compose.domain.EncodeTab
import com.rememberber.mootool.next.compose.domain.UrlCharset
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.EncodeSession
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class EncodeHistoryMeta(
    val tab: String,
    val direction: String,
    val charset: String,
    val asciiFormat: String
)

@Composable
fun EncodeScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.encodeSession() }
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
        val overflow = LayoutPolicy.overflowToolbar(maxWidth.value)
        Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).background(colors.toolbarBrush()).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("encode.title"), color = colors.textPrimary, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            if (!overflow) {
                MooButton(container.t("common.action.history"), onClick = { session.historyOpen = true; refresh() })
                MooButton(container.t("common.clear"), onClick = { session.clearCurrent(); session.notice = container.t("json.notice.cleared"); refresh() })
                if (!detached) {
                    MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.Encode) })
                }
            } else {
                Box {
                    MooButton(container.t("json.action.overflow"), primary = moreOpen, onClick = { moreOpen = true })
                    DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                        DropdownMenuItem(onClick = { moreOpen = false; session.historyOpen = true; refresh() }) {
                            Text(container.t("common.action.history"))
                        }
                        DropdownMenuItem(onClick = {
                            moreOpen = false
                            session.clearCurrent()
                            session.notice = container.t("json.notice.cleared")
                            refresh()
                        }) {
                            Text(container.t("common.clear"))
                        }
                        if (!detached) {
                            DropdownMenuItem(onClick = { moreOpen = false; container.sessionManager.detach(ToolId.Encode) }) {
                                Text(container.t("app.tool.detach"))
                            }
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.surfaceSubtle).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EncodeTab.entries.forEach { tab ->
                MooButton(container.t(tabTitleKey(tab)), primary = session.tab == tab, onClick = {
                    session.tab = tab
                    session.error = ""
                    refresh()
                })
            }
        }
        Row(Modifier.weight(1f).fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(labels.left, color = colors.textSecondary, fontSize = 12.sp)
                MooTextField(
                    session.left(),
                    { session.setLeft(it); session.error = ""; refresh() },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    singleLine = false
                )
            }
            Column(
                modifier = Modifier.width(168.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MooButton(labels.forward, primary = true, onClick = {
                    convert(container, session, forward = true)
                    refresh()
                })
                MooButton(labels.reverse, onClick = {
                    convert(container, session, forward = false)
                    refresh()
                })
                if (session.tab == EncodeTab.Url) {
                    Text(container.t("encode.charset"), color = colors.textSecondary, fontSize = 12.sp)
                    Box {
                        MooButton(if (session.charset == UrlCharset.Utf8) "UTF-8" else "GB2312", onClick = { charsetOpen = true })
                        DropdownMenu(expanded = charsetOpen, onDismissRequest = { charsetOpen = false }) {
                            DropdownMenuItem(onClick = {
                                charsetOpen = false
                                session.charset = UrlCharset.Utf8
                                refresh()
                            }) { Text("UTF-8") }
                            DropdownMenuItem(onClick = {
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
                            onClick = { asciiOpen = true }
                        )
                        DropdownMenu(expanded = asciiOpen, onDismissRequest = { asciiOpen = false }) {
                            DropdownMenuItem(onClick = {
                                asciiOpen = false
                                session.asciiFormat = AsciiFormat.Decimal
                                refresh()
                            }) { Text(container.t("encode.asciiDecimal")) }
                            DropdownMenuItem(onClick = {
                                asciiOpen = false
                                session.asciiFormat = AsciiFormat.Hex
                                refresh()
                            }) { Text(container.t("encode.asciiHex")) }
                        }
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(labels.right, color = colors.textSecondary, fontSize = 12.sp)
                MooTextField(
                    session.right(),
                    { session.setRight(it); session.error = ""; refresh() },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    singleLine = false
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.statusBar).background(colors.toolbar).padding(horizontal = 12.dp),
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
                applyHistory(session, item)
                session.notice = container.t("json.notice.restored")
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

private val historyCodec = Json { ignoreUnknownKeys = true }

private fun convert(container: AppContainer, session: EncodeSession, forward: Boolean) {
    val input = if (forward) session.left() else session.right()
    val labels = tabLabels(container, session.tab)
    val summary = if (forward) labels.forward else labels.reverse
    runCatching { EncodeEngine.convert(session.tab, forward, input, session.charset, session.asciiFormat) }
        .onSuccess { output ->
            if (forward) session.setRight(output) else session.setLeft(output)
            session.error = ""
            session.notice = summary
            val meta = EncodeHistoryMeta(
                tab = session.snapshot().tab,
                direction = if (forward) "forward" else "reverse",
                charset = if (session.charset == UrlCharset.Gb2312) "gb2312" else "utf-8",
                asciiFormat = if (session.asciiFormat == AsciiFormat.Hex) "hex" else "decimal"
            )
            container.history.save(ToolId.Encode.id, summary, summary, input, output, historyCodec.encodeToString(meta))
        }
        .onFailure { error ->
            session.notice = ""
            session.error = messageFor(container, error)
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

private fun applyHistory(session: EncodeSession, item: HistoryRecord) {
    val meta = runCatching { historyCodec.decodeFromString<EncodeHistoryMeta>(item.options) }.getOrNull()
    if (meta != null) {
        session.tab = when (meta.tab) {
            "url" -> EncodeTab.Url
            "hex" -> EncodeTab.Hex
            "ascii" -> EncodeTab.Ascii
            else -> EncodeTab.Unicode
        }
        session.charset = if (meta.charset == "gb2312") UrlCharset.Gb2312 else UrlCharset.Utf8
        session.asciiFormat = if (meta.asciiFormat == "hex") AsciiFormat.Hex else AsciiFormat.Decimal
        if (meta.direction == "reverse") {
            session.setLeft(item.output)
            session.setRight(item.input)
        } else {
            session.setLeft(item.input)
            session.setRight(item.output)
        }
    } else {
        session.setRight(item.output.ifBlank { item.input })
    }
    session.error = ""
}
