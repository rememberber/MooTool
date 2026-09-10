package com.rememberber.mootool.next.compose.features.encode

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.rememberber.mootool.next.compose.domain.AsciiFormat
import com.rememberber.mootool.next.compose.domain.EncodeEngine
import com.rememberber.mootool.next.compose.domain.EncodeException
import com.rememberber.mootool.next.compose.domain.EncodeTab
import com.rememberber.mootool.next.compose.domain.UrlCharset
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.EncodeSession
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
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
    val revision by container.sessionManager.revision.collectAsState()
    var historyItems by remember { mutableStateOf(emptyList<HistoryRecord>()) }
    val colors = MooTheme.colors
    val labels = tabLabels(container, session.tab)

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistEncode()
    }

    LaunchedEffect(session.historyOpen, revision) {
        if (session.historyOpen) historyItems = container.history.list(ToolId.Encode.id)
    }

    Column(Modifier.fillMaxSize().background(colors.workspace)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("encode.title"), color = colors.textPrimary, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            MooButton(container.t("common.action.history"), onClick = { session.historyOpen = true; refresh() })
            MooButton(container.t("common.clear"), onClick = { session.clearCurrent(); session.notice = container.t("json.notice.cleared"); refresh() })
            if (!detached) {
                MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.Encode) })
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
                    MooButton("UTF-8", primary = session.charset == UrlCharset.Utf8, onClick = {
                        session.charset = UrlCharset.Utf8
                        refresh()
                    })
                    MooButton("GB2312", primary = session.charset == UrlCharset.Gb2312, onClick = {
                        session.charset = UrlCharset.Gb2312
                        refresh()
                    })
                }
                if (session.tab == EncodeTab.Ascii) {
                    MooButton(container.t("encode.asciiDecimal"), primary = session.asciiFormat == AsciiFormat.Decimal, onClick = {
                        session.asciiFormat = AsciiFormat.Decimal
                        refresh()
                    })
                    MooButton(container.t("encode.asciiHex"), primary = session.asciiFormat == AsciiFormat.Hex, onClick = {
                        session.asciiFormat = AsciiFormat.Hex
                        refresh()
                    })
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
        EncodeHistoryDialog(container, session, historyItems) { refresh() }
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

@Composable
private fun EncodeHistoryDialog(
    container: AppContainer,
    session: EncodeSession,
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
                            Text("${item.input} → ${item.output}", color = MooTheme.colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MooButton(container.t("common.clear"), onClick = {
                    container.history.clear(ToolId.Encode.id)
                    onChanged()
                })
                MooButton(container.t("common.close"), onClick = { session.historyOpen = false; onChanged() })
            }
        }
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
