package com.rememberber.mootool.next.compose.features.diff

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.TextDiffPresentation
import com.rememberber.mootool.next.compose.domain.DiffHistoryMetadata
import com.rememberber.mootool.next.compose.domain.DiffHistoryRestore
import com.rememberber.mootool.next.compose.domain.DiffSegment
import com.rememberber.mootool.next.compose.domain.DiffSegmentType
import com.rememberber.mootool.next.compose.domain.UnifiedSpan
import com.rememberber.mootool.next.compose.domain.UnifiedSpanType
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.DiffSession
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooMenu
import com.rememberber.mootool.next.compose.ui.components.MooMenuItem
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.VerticalPaneHandle
import com.rememberber.mootool.next.compose.ui.components.setPaneSize
import com.rememberber.mootool.next.compose.ui.components.mooDiffEditorGrid
import com.rememberber.mootool.next.compose.ui.components.mooDiffEditorPane
import com.rememberber.mootool.next.compose.ui.components.mooDiffEditorSeam
import com.rememberber.mootool.next.compose.ui.components.mooDiffNavCluster
import com.rememberber.mootool.next.compose.ui.components.mooDiffToolbarOptions
import com.rememberber.mootool.next.compose.ui.components.mooDiffWorkspace
import com.rememberber.mootool.next.compose.ui.components.mooToolShell
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.mooStatusBarBackground
import com.rememberber.mootool.next.compose.ui.components.rememberPairedScrollStates
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import com.rememberber.mootool.next.compose.sessions.dismissModalOverlays
import com.rememberber.mootool.next.compose.ui.workbench.DismissModalOverlaysOnDispose
import com.rememberber.mootool.next.compose.ui.workbench.applyUserEditClearingStatusNotice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun TextDiffScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.diffSession() }
    DismissModalOverlaysOnDispose(container, ToolId.TextDiff) { session.dismissModalOverlays() }
    val revision by container.sessionManager.revision.collectAsState()
    val sessionGeneration by container.sessionManager.sessionGeneration.collectAsState()
    val settings by container.settings.collectAsState()
    var leftField by remember { mutableStateOf(TextFieldValue(session.left)) }
    var rightField by remember { mutableStateOf(TextFieldValue(session.right)) }
    val colors = MooTheme.colors
    val (leftScroll, rightScroll) = rememberPairedScrollStates()
    var moreOpen by remember { mutableStateOf(false) }

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistDiff()
    }

    LaunchedEffect(session.left, session.right, revision, sessionGeneration) {
        if (leftField.text != session.left) leftField = TextFieldValue(session.left)
        if (rightField.text != session.right) rightField = TextFieldValue(session.right)
    }
    LaunchedEffect(session.left, session.right, session.ignoreWhitespace) {
        delay(TextDiffPresentation.AUTO_COMPARE_DEBOUNCE_MS)
        runCompare(container, session, saveHistory = false) { refresh() }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
    val overflow = LayoutPolicy.overflowToolbar(maxWidth.value)
    val minPane = 240f
    val paneHandle = 10f
    val maxLeftPane = (maxWidth.value - paneHandle - minPane).coerceAtLeast(minPane)
    val defaultLeftPane = (maxWidth.value * 0.5f).coerceIn(minPane, maxLeftPane)
    val leftPaneWidth = settings.layout.pane(ToolId.TextDiff.id, 0, defaultLeftPane, minPane, maxLeftPane)
    Column(Modifier.fillMaxSize().background(colors.workspace)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MooPageTitle(container.t("diff.title"))
            MooButton(
                container.t("diff.compare"),
                prominent = true,
                enabled = TextDiffPresentation.canManualCompare(session.left, session.right),
                p5Toolbar = true,
                onClick = {
                    runCompare(container, session, saveHistory = true) { refresh() }
                },
            )
            val visible = session.visibleSegments()
            MooButton(
                container.t("diff.previous"),
                enabled = TextDiffPresentation.canNavigateDiffs(visible.size),
                p5Toolbar = true,
                onClick = {
                navigate(session, visible, -1, { leftField = it }, { rightField = it })
                refresh()
            })
            MooButton(
                container.t("diff.next"),
                enabled = TextDiffPresentation.canNavigateDiffs(visible.size),
                p5Toolbar = true,
                onClick = {
                navigate(session, visible, 1, { leftField = it }, { rightField = it })
                refresh()
            })
            if (!overflow) {
                MooButton(container.t("common.action.clear"), p5Toolbar = true, onClick = {
                    session.left = ""
                    session.right = ""
                    session.result = TextDiffPresentation.runCompare("", "", session.ignoreWhitespace)
                    session.notice = container.t("diff.status.cleared")
                    session.navIndex = -1
                    refresh()
                })
                MooButton(container.t("common.action.swap"), p5Toolbar = true, onClick = {
                    val previousLeft = session.left
                    session.left = session.right
                    session.right = previousLeft
                    session.result = TextDiffPresentation.runCompare(session.left, session.right, session.ignoreWhitespace)
                    session.notice = container.t("diff.status.swapped")
                    session.navIndex = -1
                    refresh()
                })
                MooButton(container.t("diff.copy"), p5Toolbar = true, onClick = {
                    if (session.result.unified.isEmpty()) {
                        session.notice = container.t("diff.status.noCopy")
                    } else {
                        container.copyText(session.result.unified)
                        session.notice = container.t("diff.status.copied")
                    }
                    refresh()
                })
                MooButton(container.t("diff.importLeft"), p5Toolbar = true, onClick = {
                    importSide(container, session, side = "left") { refresh() }
                })
                MooButton(container.t("diff.importRight"), p5Toolbar = true, onClick = {
                    importSide(container, session, side = "right") { refresh() }
                })
            }
            Spacer(Modifier.weight(1f))
            if (overflow) {
                Box {
                    MooButton(container.t("json.action.overflow"), primary = moreOpen, onClick = { moreOpen = true }, p5Toolbar = true)
                    MooMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                        MooMenuItem(onClick = {
                            moreOpen = false
                            session.left = ""
                            session.right = ""
                            session.result = TextDiffPresentation.runCompare("", "", session.ignoreWhitespace)
                            session.notice = container.t("diff.status.cleared")
                            session.navIndex = -1
                            refresh()
                        }) { Text(container.t("common.action.clear")) }
                        MooMenuItem(onClick = {
                            moreOpen = false
                            val previousLeft = session.left
                            session.left = session.right
                            session.right = previousLeft
                            session.result = TextDiffPresentation.runCompare(session.left, session.right, session.ignoreWhitespace)
                            session.notice = container.t("diff.status.swapped")
                            session.navIndex = -1
                            refresh()
                        }) { Text(container.t("common.action.swap")) }
                        MooMenuItem(onClick = {
                            moreOpen = false
                            if (session.result.unified.isEmpty()) {
                                session.notice = container.t("diff.status.noCopy")
                            } else {
                                container.copyText(session.result.unified)
                                session.notice = container.t("diff.status.copied")
                            }
                            refresh()
                        }) { Text(container.t("diff.copy")) }
                        MooMenuItem(onClick = {
                            moreOpen = false
                            importSide(container, session, side = "left") { refresh() }
                        }) { Text(container.t("diff.importLeft")) }
                        MooMenuItem(onClick = {
                            moreOpen = false
                            importSide(container, session, side = "right") { refresh() }
                        }) { Text(container.t("diff.importRight")) }
                        MooMenuItem(onClick = {
                            moreOpen = false
                            session.historyOpen = true
                            refresh()
                        }) { Text(container.t("common.action.history")) }
                        if (!detached) {
                            MooMenuItem(onClick = {
                                moreOpen = false
                                container.sessionManager.detach(ToolId.TextDiff)
                            }) { Text(container.t("app.tool.detach")) }
                        }
                    }
                }
            } else {
                MooButton(container.t("common.action.history"), onClick = { session.historyOpen = true; refresh() })
                if (!detached) {
                    MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.TextDiff) })
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .mooDiffToolbarOptions()
                .background(colors.surfaceSubtle)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MooButton(container.t("diff.ignoreWhitespace"), primary = session.ignoreWhitespace, p5Toolbar = true, onClick = {
                session.ignoreWhitespace = !session.ignoreWhitespace
                session.navIndex = -1
                refresh()
            })
            MooButton(container.t("diff.highlightBoth"), primary = session.highlightMode == "both", p5Toolbar = true, onClick = {
                session.highlightMode = "both"; session.navIndex = -1; refresh()
            })
            MooButton(container.t("diff.highlightCharacters"), primary = session.highlightMode == "characters", p5Toolbar = true, onClick = {
                session.highlightMode = "characters"; session.navIndex = -1; refresh()
            })
            MooButton(container.t("diff.highlightLines"), primary = session.highlightMode == "lines", p5Toolbar = true, onClick = {
                session.highlightMode = "lines"; session.navIndex = -1; refresh()
            })
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .mooDiffNavCluster()
                .background(colors.surfaceSubtle)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MooButton(container.t("diff.sideBySide"), primary = session.mode == "side", p5Toolbar = true, onClick = {
                session.mode = "side"; refresh()
            })
            MooButton(container.t("diff.unified"), primary = session.mode == "unified", p5Toolbar = true, onClick = {
                session.mode = "unified"; refresh()
            })
        }
        Row(Modifier.weight(1f).mooDiffWorkspace().mooDiffEditorGrid()) {
            if (session.mode == "side") {
                DiffEditorPane(
                    title = container.t("diff.left"),
                    value = leftField,
                    onValueChange = {
                        applyUserEditClearingStatusNotice({ session.notice }, { session.notice = it }) {
                            leftField = it
                            session.left = it.text
                            session.navIndex = -1
                        }
                        refresh()
                    },
                    segments = session.visibleSegments(),
                    side = "left",
                    highlightMode = session.highlightMode,
                    scrollState = leftScroll,
                    modifier = Modifier.width(leftPaneWidth.dp).widthIn(min = 240.dp)
                )
                VerticalPaneHandle(
                    onDelta = { container.setPaneSize(ToolId.TextDiff.id, 0, leftPaneWidth + it, 1) },
                    onReset = { container.setPaneSize(ToolId.TextDiff.id, 0, defaultLeftPane, 1) }
                )
                DiffEditorPane(
                    title = container.t("diff.right"),
                    value = rightField,
                    onValueChange = {
                        applyUserEditClearingStatusNotice({ session.notice }, { session.notice = it }) {
                            rightField = it
                            session.right = it.text
                            session.navIndex = -1
                        }
                        refresh()
                    },
                    segments = session.visibleSegments(),
                    side = "right",
                    highlightMode = session.highlightMode,
                    scrollState = rightScroll,
                    modifier = Modifier.weight(1f).widthIn(min = 240.dp)
                )
            } else {
                DiffEditorPane(
                    title = container.t("diff.left"),
                    value = leftField,
                    onValueChange = {
                        applyUserEditClearingStatusNotice({ session.notice }, { session.notice = it }) {
                            leftField = it
                            session.left = it.text
                            session.navIndex = -1
                        }
                        refresh()
                    },
                    segments = session.visibleSegments(),
                    side = "left",
                    highlightMode = session.highlightMode,
                    scrollState = leftScroll,
                    modifier = Modifier.weight(1f)
                )
                Box(Modifier.mooDiffEditorSeam().background(colors.borderSoft))
                DiffEditorPane(
                    title = container.t("diff.right"),
                    value = rightField,
                    onValueChange = {
                        applyUserEditClearingStatusNotice({ session.notice }, { session.notice = it }) {
                            rightField = it
                            session.right = it.text
                            session.navIndex = -1
                        }
                        refresh()
                    },
                    segments = session.visibleSegments(),
                    side = "right",
                    highlightMode = session.highlightMode,
                    scrollState = rightScroll,
                    modifier = Modifier.weight(1f)
                )
                Box(Modifier.mooDiffEditorSeam().background(colors.borderSoft))
                UnifiedPane(container.t("diff.unifiedPanel"), session, Modifier.weight(1f))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.statusBar).mooStatusBarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(statusText(container, session), color = colors.textMuted, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            if (detached) Text("detached", color = colors.textSecondary, fontSize = 12.sp)
        }
    }
    }

    if (session.historyOpen) {
        HistoryBrowser(
            container = container,
            toolId = ToolId.TextDiff.id,
            title = container.t("common.action.history"),
            onRestore = { item ->
                DiffHistoryRestore.apply(session, item)
                session.historyOpen = false
                refresh()
            },
            onDismiss = { session.historyOpen = false; refresh() }
        )
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
        modifier = modifier.fillMaxHeight().mooDiffEditorPane(),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(title, color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = colors.textPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(colors.accent),
            visualTransformation = DiffHighlightTransformation(segments, side, highlightMode, colors.success, colors.danger, colors.accent),
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState)
        )
    }
}

@Composable
private fun UnifiedPane(title: String, session: DiffSession, modifier: Modifier) {
    val colors = MooTheme.colors
    Column(
        modifier.fillMaxHeight().padding(9.dp, 10.dp, 10.dp, 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(title, color = colors.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(
            annotateUnified(session.result.unifiedView.text, session.result.unifiedView.lineSpans, session.result.unifiedView.characterSpans, session.highlightMode, colors.success, colors.danger, colors.accent),
            color = colors.textPrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
        )
    }
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
        val result = TextDiffPresentation.runCompare(left, right, ignore)
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
                    DiffHistoryMetadata.encode(ignore, session.highlightMode),
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
    val next = TextDiffPresentation.nextNavIndex(session.navIndex, step, visible.size)
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

private fun importSide(container: AppContainer, session: DiffSession, side: String, onDone: () -> Unit) {
    val title = if (side == "left") container.t("diff.importLeft") else container.t("diff.importRight")
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.isVisible = true
    val name = dialog.file ?: return
    val directory = dialog.directory ?: return
    val file = File(directory, name)
    when (val outcome = TextDiffPresentation.runReadImportFile(file)) {
        is TextDiffPresentation.ImportOutcome.Success -> {
            if (side == "left") session.left = outcome.content else session.right = outcome.content
            session.navIndex = -1
            session.notice = container.t("json.notice.imported")
            container.toastSuccess(container.t("json.notice.imported"))
            onDone()
        }
        is TextDiffPresentation.ImportOutcome.Failure -> {
            session.notice = container.t(
                "reformat.error.read",
                mapOf("message" to (outcome.error.message ?: file.path)),
            )
            onDone()
        }
    }
}
