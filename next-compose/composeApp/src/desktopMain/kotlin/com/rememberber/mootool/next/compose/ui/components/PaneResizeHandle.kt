package com.rememberber.mootool.next.compose.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.awt.Cursor
import kotlin.math.abs

fun AppContainer.setPaneSize(toolId: String, index: Int, value: Float, slots: Int) {
    updateSettings { current -> current.copy(layout = current.layout.withPane(toolId, index, value, slots)) }
}

fun paneKeyboardDelta(key: String, vertical: Boolean): Float? = when (key) {
    "left" -> if (!vertical) -16f else null
    "right" -> if (!vertical) 16f else null
    "up" -> if (vertical) -16f else null
    "down" -> if (vertical) 16f else null
    else -> null
}

fun paneHandleLineAlpha(hovered: Boolean, focused: Boolean, dragging: Boolean): Float = when {
    dragging -> 0.9f
    hovered || focused -> 0.5f
    else -> 0f
}

fun paneHandleLineThickness(dragging: Boolean): Float = if (dragging) 2f else 1f

@Composable
fun VerticalPaneHandle(
    onDelta: (Float) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    PaneHandle(
        vertical = false,
        onDelta = onDelta,
        onReset = onReset,
        modifier = modifier.fillMaxHeight().width(10.dp)
    )
}

@Composable
fun HorizontalPaneHandle(
    onDelta: (Float) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    PaneHandle(
        vertical = true,
        onDelta = onDelta,
        onReset = onReset,
        modifier = modifier.fillMaxWidth().height(10.dp)
    )
}

@Composable
private fun PaneHandle(
    vertical: Boolean,
    onDelta: (Float) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier
) {
    val colors = MooTheme.colors
    val icon = remember(vertical) {
        PointerIcon(Cursor.getPredefinedCursor(if (vertical) Cursor.N_RESIZE_CURSOR else Cursor.E_RESIZE_CURSOR))
    }
    val focus = remember { FocusRequester() }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val focused by interaction.collectIsFocusedAsState()
    var dragging by remember { mutableStateOf(false) }
    val lineAlpha = paneHandleLineAlpha(hovered, focused, dragging)
    val linePx = paneHandleLineThickness(dragging)
    androidx.compose.foundation.layout.Box(
        modifier
            .hoverable(interaction)
            .pointerHoverIcon(icon)
            .focusRequester(focus)
            .focusable()
            .drawBehind {
                if (lineAlpha <= 0f) return@drawBehind
                val stroke = linePx * density
                val color = colors.accentAction.copy(alpha = lineAlpha)
                if (vertical) {
                    val y = size.height / 2f - stroke / 2f
                    drawRect(color, Offset(0f, y), Size(size.width, stroke))
                } else {
                    val x = size.width / 2f - stroke / 2f
                    drawRect(color, Offset(x, 0f), Size(stroke, size.height))
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val name = when (event.key) {
                    Key.DirectionLeft -> "left"
                    Key.DirectionRight -> "right"
                    Key.DirectionUp -> "up"
                    Key.DirectionDown -> "down"
                    else -> return@onPreviewKeyEvent false
                }
                val delta = paneKeyboardDelta(name, vertical) ?: return@onPreviewKeyEvent false
                onDelta(delta)
                true
            }
            .pointerInput(vertical) {
                detectDragGestures(
                    onDragStart = {
                        dragging = true
                        focus.requestFocus()
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false }
                ) { _, amount ->
                    val value = if (vertical) amount.y else amount.x
                    if (abs(value) > 0.2f) onDelta(value)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { focus.requestFocus() },
                    onDoubleTap = { onReset() }
                )
            }
    )
}
