package com.rememberber.mootool.next.compose.features.diff

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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.DiffEngine
import com.rememberber.mootool.next.compose.domain.DiffSegment
import com.rememberber.mootool.next.compose.domain.DiffSegmentType
import com.rememberber.mootool.next.compose.domain.UnifiedSpan
import com.rememberber.mootool.next.compose.domain.UnifiedSpanType
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.DiffSession
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File

@Composable
fun TextDiffScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.diffSession() }
    val revision by container.sessionManager.revision.collectAsState()
    var historyItems by remember { mutableStateOf(emptyList<HistoryRecord>()) }
    var leftField by remember { mutableStateOf(TextFieldValue(session.left)) }
    var rightField by remember { mutableStateOf(TextFieldValue(session.right)) }
    val colors = MooTheme.colors
    val sharedScroll = rememberScrollState()

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistDiff()
    }

    LaunchedEffect(session.historyOpen, revision) {
        if (session.historyOpen) historyItems = container.history.list(ToolId.TextDiff.id)
    }
    LaunchedEffect(session.left, session.right, revision) {
        if (leftField.text != session.left) leftField = TextFieldValue(session.left)
        if (rightField.text != session.right) rightField = TextFieldValue(session.right)
    }
    LaunchedEffect(session.left, session.right, session.ignoreWhitespace) {
        delay(160)
        runCompare(container, session, saveHistory = false) { refresh() }
    }

    Column(Modifier.fillMaxSize().background(colors.workspace)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.toolbar).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(container.t("diff.title"), color = colors.textPrimary, fontSize = 16.sp)
            MooButton(container.t("diff.compare"), primary = true, onClick = {
                runCompare(container, session, saveHistory = true) { refresh() }
            })
            MooButton(container.t("common.action.clear"), onClick = {
                session.left = ""
                session.right = ""
                session.result = DiffEngine.compare("", "", session.ignoreWhitespace)
                session.notice = container.t("diff.status.cleared")
                session.navIndex = -1
                refresh()
            })
            MooButton(container.t("common.action.swap"), onClick = {
                val previousLeft = session.left
                session.left = session.right
                session.right = previousLeft
                session.result = DiffEngine.compare(session.left, session.right, session.ignoreWhitespace)
                session.notice = container.t("diff.status.swapped")
                session.navIndex = -1
                refresh()
            })
            MooButton(container.t("diff.copy"), onClick = {
                if (session.result.unified.isEmpty()) {
                    session.notice = container.t("diff.status.noCopy")
                } else {
                    copyText(session.result.unified)
                    session.notice = container.t("diff.status.copied")
                }
                refresh()
            })
            MooButton(container.t("diff.importLeft"), onClick = {
                readImportedText()?.let { session.left = it; session.notice = ""; refresh() }
            })
            MooButton(container.t("diff.importRight"), onClick = {
                readImportedText()?.let { session.right = it; session.notice = ""; refresh() }
            })
            val visible = session.visibleSegments()
            MooButton(container.t("diff.previous"), enabled = visible.isNotEmpty(), onClick = {
                navigate(session, visible, -1, { leftField = it }, { rightField = it })
                refresh()
            })
            MooButton(container.t("diff.next"), enabled = visible.isNotEmpty(), onClick = {
                navigate(session, visible, 1, { leftField = it }, { rightField = it })
                refresh()
            })
            Spacer(Modifier.weight(1f))
            MooButton(container.t("common.action.history"), onClick = { session.historyOpen = true; refresh() })
            if (!detached) {
                MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.TextDiff) })
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.surfaceSubtle).padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MooButton(container.t("diff.ignoreWhitespace"), primary = session.ignoreWhitespace, onClick = {
                session.ignoreWhitespace = !session.ignoreWhitespace
                session.navIndex = -1
                refresh()
            })
            MooButton(container.t("diff.highlightBoth"), primary = session.highlightMode == "both", onClick = {
                session.highlightMode = "both"; session.navIndex = -1; refresh()
            })
            MooButton(container.t("diff.highlightCharacters"), primary = session.highlightMode == "characters", onClick = {
                session.highlightMode = "characters"; session.navIndex = -1; refresh()
            })
            MooButton(container.t("diff.highlightLines"), primary = session.highlightMode == "lines", onClick = {
                session.highlightMode = "lines"; session.navIndex = -1; refresh()
            })
            MooButton(container.t("diff.sideBySide"), primary = session.mode == "side", onClick = {
                session.mode = "side"; refresh()
            })
            MooButton(container.t("diff.unified"), primary = session.mode == "unified", onClick = {
                session.mode = "unified"; refresh()
            })
        }
        Row(Modifier.weight(1f).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DiffEditorPane(
                title = container.t("diff.left"),
                value = leftField,
                onValueChange = {
                    leftField = it
                    session.left = it.text
                    session.navIndex = -1
                    refresh()
                },
                segments = session.visibleSegments(),
                side = "left",
                highlightMode = session.highlightMode,
                scrollState = sharedScroll,
                modifier = Modifier.weight(1f)
            )
            DiffEditorPane(
                title = container.t("diff.right"),
                value = rightField,
                onValueChange = {
                    rightField = it
                    session.right = it.text
                    session.navIndex = -1
                    refresh()
                },
                segments = session.visibleSegments(),
                side = "right",
                highlightMode = session.highlightMode,
                scrollState = sharedScroll,
                modifier = Modifier.weight(1f)
            )
            if (session.mode == "unified") {
                UnifiedPane(container.t("diff.unifiedPanel"), session, Modifier.weight(1f))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(26.dp).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(statusText(container, session), color = colors.textSecondary, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            if (detached) Text("detached", color = colors.textSecondary, fontSize = 12.sp)
        }
    }

    if (session.historyOpen) {
        DiffHistoryDialog(container, session, historyItems) { refresh() }
    }
}

@Composable
private fun DiffEditorPane(
    title: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    segments: List<DiffSegment>,
    side: String,
    highlightMode: String,
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier
) {
    val colors = MooTheme.colors
    Column(
        modifier = modifier.fillMaxHeight().clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceSubtle).border(1.dp, colors.border, RoundedCornerShape(12.dp)).padding(10.dp)
    ) {
        Text(title, color = colors.textSecondary, fontSize = 12.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = colors.textPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(colors.accent),
            visualTransformation = DiffHighlightTransformation(segments, side, highlightMode, colors.success, colors.danger, colors.accent),
            modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp).verticalScroll(scrollState)
        )
    }
}

@Composable
private fun UnifiedPane(title: String, session: DiffSession, modifier: Modifier) {
    val colors = MooTheme.colors
    Column(
        modifier.fillMaxHeight().clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceSubtle).border(1.dp, colors.border, RoundedCornerShape(12.dp)).padding(10.dp)
    ) {
        Text(title, color = colors.textSecondary, fontSize = 12.sp)
        Text(
            annotateUnified(session.result.unifiedView.text, session.result.unifiedView.lineSpans, session.result.unifiedView.characterSpans, session.highlightMode, colors.success, colors.danger, colors.accent),
            color = colors.textPrimary,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp).verticalScroll(rememberScrollState())
        )
    }
}

private class DiffHighlightTransformation(
    private val segments: List<DiffSegment>,
    private val side: String,
    private val highlightMode: String,
    private val added: Color,
    private val removed: Color,
    private val changed: Color
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(annotateSide(text.text, segments, side, highlightMode, added, removed, changed), OffsetMapping.Identity)
    }
}

private fun annotateSide(
    text: String,
    segments: List<DiffSegment>,
    side: String,
    highlightMode: String,
    added: Color,
    removed: Color,
    changed: Color
): AnnotatedString {
    val builder = AnnotatedString.Builder(text)
    for (segment in segments) {
        val from = if (side == "left") segment.leftStart else segment.rightStart
        val to = if (side == "left") segment.leftEnd else segment.rightEnd
        if (from < 0 || to < 0 || to <= from) continue
        val color = when (segment.type) {
            DiffSegmentType.Insert -> added
            DiffSegmentType.Delete -> removed
            DiffSegmentType.Change -> changed
        }
        val end = to.coerceAtMost(text.length)
        val start = from.coerceIn(0, text.length)
        if (start >= end) continue
        if (highlightMode != "characters") {
            val lineFrom = lineStartAt(text, start)
            val lineTo = lineEndAt(text, start)
            if (lineTo > lineFrom) builder.addStyle(SpanStyle(background = color.copy(alpha = 0.16f)), lineFrom, lineTo)
        }
        if (highlightMode != "lines") {
            builder.addStyle(SpanStyle(background = color.copy(alpha = 0.42f)), start, end)
        }
    }
    return builder.toAnnotatedString()
}

private fun annotateUnified(
    text: String,
    lineSpans: List<UnifiedSpan>,
    characterSpans: List<UnifiedSpan>,
    highlightMode: String,
    added: Color,
    removed: Color,
    changed: Color
): AnnotatedString {
    val builder = AnnotatedString.Builder(text)
    if (highlightMode != "characters") {
        for (span in lineSpans) {
            val color = when (span.type) {
                UnifiedSpanType.AddLine -> added
                UnifiedSpanType.DeleteLine -> removed
                UnifiedSpanType.HunkLine, UnifiedSpanType.ChangeCharacter -> changed
                else -> changed.copy(alpha = 0.5f)
            }
            val start = span.start.coerceIn(0, text.length)
            val end = span.end.coerceIn(start, text.length)
            if (end > start) builder.addStyle(SpanStyle(background = color.copy(alpha = 0.18f)), start, end)
        }
    }
    if (highlightMode != "lines") {
        for (span in characterSpans) {
            val color = when (span.type) {
                UnifiedSpanType.AddCharacter -> added
                UnifiedSpanType.DeleteCharacter -> removed
                else -> changed
            }
            val start = span.start.coerceIn(0, text.length)
            val end = span.end.coerceIn(start, text.length)
            if (end > start) builder.addStyle(SpanStyle(background = color.copy(alpha = 0.42f)), start, end)
        }
    }
    return builder.toAnnotatedString()
}

private fun runCompare(container: AppContainer, session: DiffSession, saveHistory: Boolean, onChanged: () -> Unit) {
    session.compareGeneration += 1
    val generation = session.compareGeneration
    val left = session.left
    val right = session.right
    val ignore = session.ignoreWhitespace
    container.scope.launch {
        val result = DiffEngine.compare(left, right, ignore)
        withContext(Dispatchers.Swing) {
            if (generation != session.compareGeneration) return@withContext
            session.result = result
            session.navIndex = -1
            session.notice = when {
                left.isEmpty() && right.isEmpty() -> container.t("diff.status.enterText")
                result.segments.isEmpty() && (left.isNotEmpty() || right.isNotEmpty()) && left == right ->
                    container.t("diff.identical")
                else -> ""
            }
            if (saveHistory && (left.isNotEmpty() || right.isNotEmpty())) {
                container.history.save(
                    ToolId.TextDiff.id,
                    container.t("diff.summary", mapOf("added" to result.added.toString(), "removed" to result.removed.toString(), "changed" to result.changed.toString())),
                    container.t("diff.summary", mapOf("added" to result.added.toString(), "removed" to result.removed.toString(), "changed" to result.changed.toString())),
                    left,
                    right,
                    "${ignore}\t${session.highlightMode}"
                )
            }
            onChanged()
        }
    }
}

private fun navigate(
    session: DiffSession,
    visible: List<DiffSegment>,
    step: Int,
    setLeft: (TextFieldValue) -> Unit,
    setRight: (TextFieldValue) -> Unit
) {
    if (visible.isEmpty()) return
    val next = (session.navIndex + step + visible.size) % visible.size
    session.navIndex = next
    val segment = visible[next]
    if (segment.leftStart >= 0) setLeft(TextFieldValue(session.left, TextRange(segment.leftStart, segment.leftEnd.coerceAtLeast(segment.leftStart))))
    if (segment.rightStart >= 0) setRight(TextFieldValue(session.right, TextRange(segment.rightStart, segment.rightEnd.coerceAtLeast(segment.rightStart))))
    session.notice = ""
}

private fun statusText(container: AppContainer, session: DiffSession): String {
    val visible = session.visibleSegments()
    if (session.navIndex >= 0 && visible.isNotEmpty()) {
        return container.t("diff.status.navigation", mapOf("current" to (session.navIndex + 1).toString(), "total" to visible.size.toString()))
    }
    if (session.notice.isNotEmpty()) return session.notice
    return if (session.highlightMode == "characters") {
        container.t("diff.status.characterComplete", mapOf("count" to visible.size.toString()))
    } else {
        container.t("diff.status.complete", mapOf("count" to visible.size.toString()))
    }
}

private fun lineStartAt(text: String, offset: Int): Int {
    val position = offset.coerceIn(0, text.length)
    if (position == 0) return 0
    val index = text.lastIndexOf('\n', position - 1)
    return if (index < 0) 0 else index + 1
}

private fun lineEndAt(text: String, offset: Int): Int {
    val index = text.indexOf('\n', offset.coerceIn(0, text.length))
    return if (index < 0) text.length else index
}

private fun copyText(value: String) {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
}

private fun readImportedText(): String? {
    val dialog = FileDialog(null as Frame?, "Import text", FileDialog.LOAD)
    dialog.isVisible = true
    val file = dialog.file ?: return null
    val directory = dialog.directory ?: return null
    return File(directory, file).readText()
}

@Composable
private fun DiffHistoryDialog(
    container: AppContainer,
    session: DiffSession,
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
                            session.left = item.input
                            session.right = item.output
                            session.historyOpen = false
                            session.notice = container.t("json.notice.restored")
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
                    container.history.clear(ToolId.TextDiff.id)
                    onChanged()
                })
                MooButton(container.t("common.close"), onClick = { session.historyOpen = false; onChanged() })
            }
        }
    }
}
