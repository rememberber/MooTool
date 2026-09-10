package com.rememberber.mootool.next.compose.features.messageboard

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.MessageBoardSession
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooTextField
import com.rememberber.mootool.next.compose.ui.theme.MooTheme

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
        session.error = if (session.displayAwake) "" else container.t("messageBoard.wakeUnavailable")
        refresh()
    }

    DisposableEffect(Unit) {
        onDispose { exitPresentation() }
    }

    Column(Modifier.fillMaxSize().background(colors.workspace)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp).background(colors.toolbar).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(container.t("messageBoard.title"), color = colors.textPrimary, fontSize = 16.sp)
            if (revision < 0) Spacer(Modifier.width(0.dp))
            Spacer(Modifier.weight(1f))
            if (!detached) {
                MooButton(container.t("app.tool.detach"), onClick = { container.sessionManager.detach(ToolId.MessageBoard) })
            }
        }
        Row(Modifier.weight(1f).fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!session.presenting) {
                ControlPanel(container, session, Modifier.width(300.dp).fillMaxHeight(), ::refresh)
            }
            StagePanel(container, session, Modifier.weight(1f).fillMaxHeight(), presenting = false, onPresent = ::enterPresentation)
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
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
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
            { session.message = MessageBoardEngine.clip(it); session.error = ""; onChanged() },
            modifier = Modifier.fillMaxWidth().height(96.dp),
            placeholder = container.t("messageBoard.placeholder"),
            singleLine = false
        )
        Text(container.t("messageBoard.messageHint"), color = colors.textSecondary, fontSize = 12.sp)
        Text(container.t("messageBoard.presets"), color = colors.textPrimary, fontSize = 14.sp)
        MessageBoardEngine.presets.forEach { preset ->
            val label = container.t(preset.messageKey)
            MooButton(label, primary = session.message == label, onClick = {
                session.message = label
                session.theme = preset.theme
                onChanged()
            })
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
                        .clickable { session.theme = theme; onChanged() }
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MooButton(container.t("messageBoard.alignLeft"), primary = session.alignment == BoardAlignment.Left, onClick = {
                session.alignment = BoardAlignment.Left
                onChanged()
            })
            MooButton(container.t("messageBoard.alignCenter"), primary = session.alignment == BoardAlignment.Center, onClick = {
                session.alignment = BoardAlignment.Center
                onChanged()
            })
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
                MooButton(container.t("messageBoard.display"), onClick = onPresent)
            } else {
                MooButton(container.t("messageBoard.exitHint"), onClick = onExit)
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
