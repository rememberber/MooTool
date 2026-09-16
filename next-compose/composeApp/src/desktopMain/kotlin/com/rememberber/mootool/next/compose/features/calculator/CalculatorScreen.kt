package com.rememberber.mootool.next.compose.features.calculator

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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.CalculatorEngine
import com.rememberber.mootool.next.compose.domain.CalculatorException
import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.CalculatorSession
import com.rememberber.mootool.next.compose.ui.components.HistoryBrowser
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.VerticalPaneHandle
import com.rememberber.mootool.next.compose.ui.components.setPaneSize
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.components.mooStatusBarBackground
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.components.OverflowAction
import com.rememberber.mootool.next.compose.ui.components.OverflowActionCluster
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import com.rememberber.mootool.next.compose.ui.workbench.blockedByIme
import com.rememberber.mootool.next.compose.sessions.dismissModalOverlays
import com.rememberber.mootool.next.compose.ui.workbench.DismissModalOverlaysOnDispose
import com.rememberber.mootool.next.compose.ui.workbench.applyUserEditClearingStatusNotice

@Composable
fun CalculatorScreen(container: AppContainer, detached: Boolean) {
    val session = remember { container.sessionManager.calculatorSession() }
    DismissModalOverlaysOnDispose(container, ToolId.Calculator) { session.dismissModalOverlays() }
    val revision by container.sessionManager.revision.collectAsState()
    val settings by container.settings.collectAsState()
    val colors = MooTheme.colors

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistCalculator()
    }


    BoxWithConstraints(Modifier.fillMaxSize().background(colors.workspace)) {
        val contentMaxWidth = maxWidth.value
        val overflow = LayoutPolicy.overflowToolbar(contentMaxWidth)
        val minLeft = 360f
        val minRight = 320f
        val paneHandle = 10f
        val maxLeft = (contentMaxWidth - paneHandle - minRight).coerceAtLeast(minLeft)
        val defaultLeft = (contentMaxWidth * 0.55f).coerceIn(minLeft, maxLeft)
        val leftWidth = settings.layout.pane(ToolId.Calculator.id, 0, defaultLeft, minLeft, maxLeft)
        Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MooPageTitle(container.t("calculator.title"))
            Spacer(Modifier.weight(1f))
            OverflowActionCluster(
                overflow = overflow,
                moreLabel = container.t("json.action.overflow"),
                actions = buildList {
                    add(OverflowAction(container.t("common.action.history")) { session.historyOpen = true; refresh() })
                    add(OverflowAction(container.t("time.copy")) {
                        session.notice = copyText(session.result, container)
                        session.error = ""
                        refresh()
                    })
                    if (!detached) add(OverflowAction(container.t("app.tool.detach")) { container.sessionManager.detach(ToolId.Calculator) })
                }
            )
        }
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier.width(leftWidth.dp).widthIn(min = 360.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Panel(container.t("calculator.arithmetic")) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.onPreviewKeyEvent { event ->
                            if (!event.blockedByIme() && event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                runCalc(container, session, container.t("calculator.expression"), session.expression) {
                                    CalculatorEngine.evaluateExpression(session.expression)
                                }
                                refresh()
                                true
                            } else false
                        }
                    ) {
                        MooTextField(
                            session.expression,
                            {
                                applyUserEditClearingStatusNotice({ session.notice }, { session.notice = it }) {
                                    session.expression = it
                                }
                                refresh()
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = container.t("calculator.expression")
                        )
                        MooButton("=", prominent = true, p5Toolbar = true, onClick = {
                            runCalc(container, session, container.t("calculator.expression"), session.expression) {
                                CalculatorEngine.evaluateExpression(session.expression)
                            }
                            refresh()
                        })
                    }
                }
                Panel(container.t("calculator.base")) {
                    LabeledField(container.t("calculator.hex"), session.hex) { session.hex = it; refresh() }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MooButton("HEX → DEC", p5Toolbar = true, onClick = {
                            runCalc(container, session, "HEX → DEC", session.hex) {
                                CalculatorEngine.convertBase(session.hex, 16, 10).also { session.decimal = it }
                            }
                            refresh()
                        })
                        MooButton("DEC → HEX", p5Toolbar = true, onClick = {
                            runCalc(container, session, "DEC → HEX", session.decimal) {
                                CalculatorEngine.convertBase(session.decimal, 10, 16).also { session.hex = it }
                            }
                            refresh()
                        })
                    }
                    LabeledField(container.t("calculator.decimal"), session.decimal) { session.decimal = it; refresh() }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MooButton("DEC → BIN", p5Toolbar = true, onClick = {
                            runCalc(container, session, "DEC → BIN", session.decimal) {
                                CalculatorEngine.convertBase(session.decimal, 10, 2).also { session.binary = it }
                            }
                            refresh()
                        })
                        MooButton("BIN → DEC", p5Toolbar = true, onClick = {
                            runCalc(container, session, "BIN → DEC", session.binary) {
                                CalculatorEngine.convertBase(session.binary, 2, 10).also { session.decimal = it }
                            }
                            refresh()
                        })
                    }
                    LabeledField(container.t("calculator.binary"), session.binary) { session.binary = it; refresh() }
                }
                OperationPanel(
                    title = container.t("calculator.gcd"),
                    firstLabel = container.t("calculator.first"),
                    secondLabel = container.t("calculator.second"),
                    first = session.gcdFirst,
                    second = session.gcdSecond,
                    onFirst = { session.gcdFirst = it; refresh() },
                    onSecond = { session.gcdSecond = it; refresh() },
                    action = container.t("calculator.calculateGcd"),
                    onAction = {
                        runCalc(container, session, container.t("calculator.gcd"), "${session.gcdFirst}, ${session.gcdSecond}") {
                            CalculatorEngine.gcd(session.gcdFirst, session.gcdSecond)
                        }
                        refresh()
                    }
                )
                OperationPanel(
                    title = container.t("calculator.lcm"),
                    firstLabel = container.t("calculator.first"),
                    secondLabel = container.t("calculator.second"),
                    first = session.lcmFirst,
                    second = session.lcmSecond,
                    onFirst = { session.lcmFirst = it; refresh() },
                    onSecond = { session.lcmSecond = it; refresh() },
                    action = container.t("calculator.calculateLcm"),
                    onAction = {
                        runCalc(container, session, container.t("calculator.lcm"), "${session.lcmFirst}, ${session.lcmSecond}") {
                            CalculatorEngine.lcm(session.lcmFirst, session.lcmSecond)
                        }
                        refresh()
                    }
                )
                OperationPanel(
                    title = container.t("calculator.permutation"),
                    firstLabel = container.t("calculator.n"),
                    secondLabel = container.t("calculator.m"),
                    first = session.permutationN,
                    second = session.permutationM,
                    onFirst = { session.permutationN = it; refresh() },
                    onSecond = { session.permutationM = it; refresh() },
                    action = "A(n,m)",
                    onAction = {
                        runCalc(container, session, container.t("calculator.permutation"), "${session.permutationN}, ${session.permutationM}") {
                            CalculatorEngine.permutation(session.permutationN, session.permutationM)
                        }
                        refresh()
                    }
                )
                OperationPanel(
                    title = container.t("calculator.combination"),
                    firstLabel = container.t("calculator.n"),
                    secondLabel = container.t("calculator.m"),
                    first = session.combinationN,
                    second = session.combinationM,
                    onFirst = { session.combinationN = it; refresh() },
                    onSecond = { session.combinationM = it; refresh() },
                    action = "C(n,m)",
                    onAction = {
                        runCalc(container, session, container.t("calculator.combination"), "${session.combinationN}, ${session.combinationM}") {
                            CalculatorEngine.combination(session.combinationN, session.combinationM)
                        }
                        refresh()
                    }
                )
            }
            VerticalPaneHandle(
                onDelta = { container.setPaneSize(ToolId.Calculator.id, 0, leftWidth + it, 1) },
                onReset = { container.setPaneSize(ToolId.Calculator.id, 0, defaultLeft, 1) }
            )
            Column(
                modifier = Modifier.weight(1f).widthIn(min = 320.dp).fillMaxHeight().background(colors.surfaceSubtle).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(container.t("common.result"), color = colors.textBody, fontSize = 12.sp)
                Text(
                    session.result.ifEmpty { "—" },
                    color = colors.accent,
                    fontSize = 26.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.workspace)
                        .border(1.dp, colors.borderControl, RoundedCornerShape(6.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
                Text(container.t("calculator.history"), color = colors.textBody, fontSize = 12.sp)
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(MooTheme.dimens.cardRadius))
                        .background(colors.workspace)
                        .border(1.dp, colors.borderControl, RoundedCornerShape(MooTheme.dimens.cardRadius))
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    session.log.forEach { line ->
                        Column(Modifier.fillMaxWidth()) {
                            Text(
                                line,
                                color = colors.textMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp)
                            )
                            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSoft))
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
            toolId = ToolId.Calculator.id,
            title = container.t("calculator.history"),
            onRestore = { item ->
                session.expression = item.input
                session.result = item.output.ifBlank { session.result }
                session.historyOpen = false
                refresh()
            },
            onDismiss = { session.historyOpen = false; refresh() }
        )
    }
}

@Composable
private fun Panel(title: String, content: @Composable () -> Unit) {
    val colors = MooTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(MooTheme.dimens.cardRadius)).background(colors.workspace)
            .border(1.dp, colors.borderControl, RoundedCornerShape(MooTheme.dimens.cardRadius)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, color = colors.textPrimary, fontSize = 14.sp)
        content()
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
private fun OperationPanel(
    title: String,
    firstLabel: String,
    secondLabel: String,
    first: String,
    second: String,
    onFirst: (String) -> Unit,
    onSecond: (String) -> Unit,
    action: String,
    onAction: () -> Unit
) {
    Panel(title) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(firstLabel, color = MooTheme.colors.textSecondary, fontSize = 12.sp)
                MooTextField(first, onFirst, modifier = Modifier.fillMaxWidth())
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(secondLabel, color = MooTheme.colors.textSecondary, fontSize = 12.sp)
                MooTextField(second, onSecond, modifier = Modifier.fillMaxWidth())
            }
            MooButton(action, onClick = onAction, p5Toolbar = true)
        }
    }
}


private fun runCalc(container: AppContainer, session: CalculatorSession, summary: String, input: String, block: () -> String) {
    runCatching(block)
        .onSuccess { output ->
            session.result = output
            session.error = ""
            session.notice = summary
            container.toastSuccess(summary)
            session.log = listOf("$summary: $input = $output") + session.log.take(11)
            container.history.save(ToolId.Calculator.id, summary, summary, input, output)
        }
        .onFailure { error ->
            session.notice = ""
            val message = messageFor(container, error)
            session.error = message
            container.toastError(message)
        }
}

private fun messageFor(container: AppContainer, error: Throwable): String {
    val code = (error as? CalculatorException)?.code
    return when (code) {
        "invalid-expression" -> container.t("calculator.error.expression")
        "non-finite" -> container.t("calculator.error.nonFinite")
        "value-required" -> container.t("calculator.error.valueRequired")
        "invalid-base-value", "invalid-base" -> container.t("calculator.error.base")
        "integer-required" -> container.t("calculator.error.integer")
        "count-range" -> container.t("calculator.error.count")
        else -> error.message ?: container.t("calculator.error.expression")
    }
}

private fun copyText(value: String, container: AppContainer): String {
    return if (container.copyText(value)) container.t("time.notice.copied") else container.t("json.notice.copyFailed")
}
