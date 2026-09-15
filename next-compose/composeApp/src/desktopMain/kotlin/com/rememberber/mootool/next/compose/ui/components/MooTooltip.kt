package com.rememberber.mootool.next.compose.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import kotlinx.coroutines.delay

@Composable
fun MooTooltip(
    text: String,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    var hovered by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(hovered, enabled, text) {
        if (!enabled || !hovered || text.isBlank()) {
            visible = false
            return@LaunchedEffect
        }
        delay(280)
        visible = hovered
    }
    Box(
        modifier = if (enabled && text.isNotBlank()) {
            Modifier.pointerInput(text) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> hovered = true
                            PointerEventType.Exit -> hovered = false
                            else -> Unit
                        }
                    }
                }
            }
        } else {
            Modifier
        }
    ) {
        content()
        if (visible) {
            val colors = MooTheme.colors
            Popup(alignment = Alignment.CenterEnd, offset = IntOffset(10, 0)) {
                Text(
                    text,
                    color = colors.workspace,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(6.dp))
                        .background(colors.textPrimary, RoundedCornerShape(6.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }
}
