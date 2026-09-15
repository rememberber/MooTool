package com.rememberber.mootool.next.compose.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
        modifier = modifier.fillMaxHeight().width(6.dp)
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
        modifier = modifier.fillMaxWidth().height(6.dp)
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
    androidx.compose.foundation.layout.Box(
        modifier
            .background(colors.border.copy(alpha = 0.55f))
            .pointerHoverIcon(icon)
            .focusRequester(focus)
            .focusable()
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
                detectDragGestures { _, amount ->
                    focus.requestFocus()
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
