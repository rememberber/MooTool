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
import androidx.compose.ui.graphics.Color
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
            val radius = colors.tooltipRadius()
            val shape = RoundedCornerShape(radius)
            val border = if (colors.styleId == "smartisan") {
                colors.tooltipContent().copy(alpha = 0.14f)
            } else {
                Color.Transparent
            }
            val elevation = when (colors.styleId) {
                "claude" -> 10.dp
                "miui-v5" -> 6.dp
                else -> 8.dp
            }
            val padH = if (colors.styleId == "hero") 10.dp else 8.dp
            val padV = if (colors.styleId == "hero") 7.dp else 6.dp
            Popup(alignment = Alignment.CenterEnd, offset = IntOffset(10, 0)) {
                Text(
                    text,
                    color = colors.tooltipContent(),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .shadow(elevation, shape, ambientColor = colors.shadow, spotColor = colors.shadow)
                        .background(colors.tooltipFill(), shape)
                        .border(1.dp, border, shape)
                        .padding(horizontal = padH, vertical = padV)
                )
            }
        }
    }
}
