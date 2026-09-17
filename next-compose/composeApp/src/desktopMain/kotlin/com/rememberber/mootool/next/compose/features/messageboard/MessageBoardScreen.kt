package com.rememberber.mootool.next.compose.features.messageboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.ProductIdentity
import com.rememberber.mootool.next.compose.domain.BoardAlignment
import com.rememberber.mootool.next.compose.domain.BoardTheme
import com.rememberber.mootool.next.compose.domain.MessageBoardEngine
import com.rememberber.mootool.next.compose.domain.MessageBoardWiringPresentation
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.MessageBoardSession
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooMenu
import com.rememberber.mootool.next.compose.ui.components.MooMenuItem
import com.rememberber.mootool.next.compose.ui.components.MooPageTitle
import com.rememberber.mootool.next.compose.ui.components.mooFocusClickable
import com.rememberber.mootool.next.compose.ui.components.mooMessagePresetChip
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
import com.rememberber.mootool.next.compose.ui.workbench.onUserInput

private const val WAKE_TOKEN = "message-board"

@Composable
fun MessageBoardScreen(container: AppContainer, detached: Boolean) {
    val session = remember {
        container.sessionManager.messageBoardSession().also { current ->
            if (!current.restored && current.message.isEmpty()) {
                current.message = container.t("messageBoard.preset.away")
            }
        }
    }
    DismissModalOverlaysOnDispose(container, ToolId.MessageBoard) { session.dismissModalOverlays() }
    val revision by container.sessionManager.revision.collectAsState()
    val colors = MooTheme.colors

    fun refresh() {
        container.sessionManager.bump()
        container.sessionManager.persistMessageBoard()
    }

    fun exitPresentation() {
        if (!session.presenting) return
        session.presenting = false
        container.displayWake.release(WAKE_TOKEN)
        session.displayAwake = false
        refresh()
    }

    fun enterPresentation() {
        session.presenting = true
        session.displayAwake = container.displayWake.acquire(WAKE_TOKEN)
        session.error = if (MessageBoardWiringPresentation.wakeErrorIfNeeded(session.displayAwake)) {
            container.t("messageBoard.wakeUnavailable")
        } else {
            ""
        }
        refresh()
    }

    DisposableEffect(Unit) {
        onDispose { exitPresentation() }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.workspace)) {
        val overflow = LayoutPolicy.overflowToolbar(maxWidth.value)
        Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MooPageTitle(container.t("messageBoard.title"))
            if (revision < 0) Spacer(Modifier.width(0.dp))
            Spacer(Modifier.weight(1f))
            OverflowActionCluster(
                overflow = overflow,
                moreLabel = container.t("json.action.overflow"),
                actions = buildList {
                    if (!detached) add(OverflowAction(container.t("app.tool.detach")) { container.sessionManager.detach(ToolId.MessageBoard) })
                }
            )
        }
        Row(Modifier.weight(1f).fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!session.presenting) {
                ControlPanel(container, session, Modifier.width(300.dp).fillMaxHeight(), ::refresh)
            }
            StagePanel(container, session, Modifier.weight(1f).fillMaxHeight(), presenting = false, onPresent = ::enterPresentation)
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

    if (session.presenting) {
        Window(
            onCloseRequest = ::exitPresentation,
            title = "${container.t("messageBoard.title")} · ${ProductIdentity.DISPLAY_NAME}",
            undecorated = true,
            alwaysOnTop = true,
            state = rememberWindowState(placement = WindowPlacement.Maximized)
        ) {
            Box(
                Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                    if (!event.blockedByIme() && event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        exitPresentation()
                        true
                    } else {
                        false
                    }
                }
            ) {
                StagePanel(container, session, Modifier.fillMaxSize(), presenting = true, onPresent = {}, onExit = ::exitPresentation)
            }
        }
    }
}

@Composable
private fun ControlPanel(
    container: AppContainer,
    session: MessageBoardSession,
    modifier: Modifier,
    onChanged: () -> Unit
) {
    val colors = MooTheme.colors
    Column(
        modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(container.t("messageBoard.message"), color = colors.textSecondary, fontSize = 12.sp)
        Text("${session.message.length}/${MessageBoardEngine.MAX_LENGTH}", color = colors.textSecondary, fontSize = 12.sp)
        MooTextField(
            session.message,
            { value ->
                session.onUserInput {
                    session.message = MessageBoardWiringPresentation.clippedMessage(value)
                    session.error = ""
                    onChanged()
                }
            },
            modifier = Modifier.fillMaxWidth().height(96.dp),
            placeholder = container.t("messageBoard.placeholder"),
            singleLine = false
        )
        Text(container.t("messageBoard.messageHint"), color = colors.textSecondary, fontSize = 12.sp)
        Text(container.t("messageBoard.presets"), color = colors.textPrimary, fontSize = 14.sp)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MessageBoardEngine.presets.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { preset ->
                        val label = container.t(preset.messageKey)
                        MessageBoardPresetChip(
                            label = label,
                            theme = preset.theme,
                            active = session.message == label,
                            onClick = {
                                session.onUserInput {
                                    session.message = label
                                    session.theme = preset.theme
                                    onChanged()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Text(container.t("messageBoard.style"), color = colors.textPrimary, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BoardTheme.entries.forEach { theme ->
                val palette = MessageBoardEngine.theme(theme)
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                        .border(
                            if (session.theme == theme) 2.dp else 1.dp,
                            if (session.theme == theme) colors.accent else colors.border,
                            RoundedCornerShape(8.dp)
                        )
                        .background(palette.background.toComposeColor())
                        .mooFocusClickable { session.theme = theme; onChanged() }
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${container.t("messageBoard.size")} ${session.size}%", color = colors.textSecondary, fontSize = 12.sp)
        }
        Slider(
            value = session.size.toFloat(),
            onValueChange = { session.size = MessageBoardEngine.snapSize(it.toInt()); onChanged() },
            valueRange = MessageBoardEngine.MIN_SIZE.toFloat()..MessageBoardEngine.MAX_SIZE.toFloat(),
            steps = ((MessageBoardEngine.MAX_SIZE - MessageBoardEngine.MIN_SIZE) / MessageBoardEngine.SIZE_STEP) - 1
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(container.t("messageBoard.alignment"), color = colors.textSecondary, fontSize = 12.sp)
            Box {
                var alignOpen by remember { mutableStateOf(false) }
                val current = when (session.alignment) {
                    BoardAlignment.Left -> container.t("messageBoard.alignLeft")
                    BoardAlignment.Center -> container.t("messageBoard.alignCenter")
                }
                MooButton(current, onClick = { alignOpen = true }, p5Toolbar = true)
                MooMenu(expanded = alignOpen, onDismissRequest = { alignOpen = false }) {
                    MooMenuItem(onClick = {
                        alignOpen = false
                        session.alignment = BoardAlignment.Left
                        onChanged()
                    }) { Text(container.t("messageBoard.alignLeft")) }
                    MooMenuItem(onClick = {
                        alignOpen = false
                        session.alignment = BoardAlignment.Center
                        onChanged()
                    }) { Text(container.t("messageBoard.alignCenter")) }
                }
            }
        }
    }
}

@Composable
private fun StagePanel(
    container: AppContainer,
    session: MessageBoardSession,
    modifier: Modifier,
    presenting: Boolean,
    onPresent: () -> Unit,
    onExit: () -> Unit = {}
) {
    val palette = MessageBoardEngine.theme(session.theme)
    val visible = session.message.trim().ifEmpty { container.t("messageBoard.empty") }
    Column(
        modifier.clip(RoundedCornerShape(if (presenting) 0.dp else 12.dp)).background(palette.background.toComposeColor()).padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(container.t("messageBoard.badge"), color = palette.foreground.toComposeColor(), fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text(
                if (session.displayAwake) container.t("messageBoard.keepAwake") else container.t("messageBoard.badgeEnglish"),
                color = palette.foreground.toComposeColor(),
                fontSize = 12.sp
            )
            if (!presenting) {
                MooButton(
                    container.t("messageBoard.display"),
                    onClick = onPresent,
                    p5Toolbar = true,
                    enabled = MessageBoardWiringPresentation.canEnterPresentation(session.message, session.displayAwake),
                )
            } else {
                MooButton(container.t("messageBoard.exitHint"), onClick = onExit, p5Toolbar = true)
            }
        }
        FittedMessage(
            text = visible,
            sizePercent = session.size,
            alignment = session.alignment,
            color = palette.foreground.toComposeColor(),
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp)
        )
        Text(container.t("messageBoard.footer"), color = palette.foreground.toComposeColor(), fontSize = 12.sp)
    }
}

@Composable
private fun FittedMessage(
    text: String,
    sizePercent: Int,
    alignment: BoardAlignment,
    color: Color,
    modifier: Modifier
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val maxW = constraints.maxWidth
        val maxH = constraints.maxHeight
        val fontPx = MessageBoardEngine.fitFontSize(maxW, maxH, sizePercent) { candidate ->
            val result = measurer.measure(
                text = AnnotatedString(text),
                style = TextStyle(fontSize = with(density) { candidate.toSp() }, color = color, fontWeight = FontWeight.SemiBold),
                constraints = Constraints(maxWidth = maxW.coerceAtLeast(1))
            )
            result.size.width <= maxW && result.size.height <= maxH
        }
        Text(
            text,
            color = color,
            fontSize = with(density) { fontPx.toSp() },
            fontWeight = FontWeight.SemiBold,
            textAlign = if (alignment == BoardAlignment.Center) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxSize(),
            lineHeight = with(density) { (fontPx * 1.15f).toInt().toSp() }
        )
    }
}

private fun String.toComposeColor(): Color {
    val rgb = removePrefix("#").toInt(16)
    return Color((rgb shr 16) and 0xff, (rgb shr 8) and 0xff, rgb and 0xff)
}

@Composable
private fun MessageBoardPresetChip(
    label: String,
    theme: BoardTheme,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MooTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(8.dp)
    val (dotFill, dotRing) = presetDot(theme)
    val fill = when {
        active -> colors.accent.copy(alpha = 0.08f).compositeOver(colors.workspace)
        hovered -> colors.hoveredControlFill()
        else -> colors.workspace
    }
    val stroke = when {
        active -> colors.accent.copy(alpha = 0.38f).compositeOver(colors.borderControl)
        hovered -> colors.borderControlHover
        else -> colors.borderControl
    }
    Row(
        modifier = modifier
            .mooMessagePresetChip()
            .clip(shape)
            .background(fill)
            .border(1.dp, stroke, shape)
            .hoverable(interaction)
            .mooFocusClickable(shape = shape, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            Modifier.size(7.dp)
                .drawBehind {
                    drawCircle(dotRing.copy(alpha = 0.10f), radius = size.minDimension / 2f + 3.dp.toPx())
                }
                .clip(CircleShape)
                .background(dotFill)
        )
        Text(
            label,
            color = if (active || hovered) colors.textStrong else colors.textBody,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun presetDot(theme: BoardTheme): Pair<Color, Color> = when (theme) {
    BoardTheme.Sunbeam -> Color(0xFFF4CE57) to Color(0xFFD3A81D)
    BoardTheme.Coral -> Color(0xFFF36B55) to Color(0xFFD74937)
    BoardTheme.Cobalt -> Color(0xFF5172E5) to Color(0xFF3459D4)
    BoardTheme.Forest -> Color(0xFF2D765C) to Color(0xFF0F4A3A)
    BoardTheme.Paper -> Color(0xFFD8C9AE) to Color(0xFFA38F70)
    BoardTheme.Midnight -> Color(0xFF697591) to Color(0xFF424A64)
}
