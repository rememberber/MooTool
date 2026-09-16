package com.rememberber.mootool.next.compose.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.blockedByIme

object ModalOverlayState {
    var count by mutableStateOf(0)
        private set

    fun enter() {
        count++
    }

    fun leave() {
        count = (count - 1).coerceAtLeast(0)
    }
}

@Composable
fun Modifier.mooDialogSurface(radius: Dp? = null): Modifier {
    val colors = MooTheme.colors
    val shape = RoundedCornerShape(radius ?: MooTheme.dimens.dialogRadius)
    val border = when (colors.styleId) {
        "smartisan", "miui-v5" -> colors.borderControl
        else -> colors.borderSoft
    }
    return this
        .shadow(24.dp, shape, ambientColor = colors.shadow, spotColor = colors.shadow)
        .clip(shape)
        .background(colors.workspace)
        .border(1.dp, border, shape)
}

@Composable
fun MooOverlay(
    onDismiss: () -> Unit,
    alignment: Alignment = Alignment.Center,
    contentPadding: PaddingValues = PaddingValues(24.dp),
    fillMaxSize: Boolean = false,
    content: @Composable () -> Unit
) {
    val scrim = MooTheme.colors.overlayScrim()
    DisposableEffect(Unit) {
        ModalOverlayState.enter()
        onDispose { ModalOverlayState.leave() }
    }
    Box(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (!event.blockedByIme() && event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                    onDismiss()
                    true
                } else {
                    false
                }
            }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
        val consumeClicks = remember { MutableInteractionSource() }
        val panelModifier = if (fillMaxSize) {
            Modifier.fillMaxSize()
        } else {
            Modifier.align(alignment)
        }
        Box(
            panelModifier
                .padding(contentPadding)
                .clickable(
                    interactionSource = consumeClicks,
                    indication = null,
                    onClick = {}
                )
        ) {
            content()
        }
    }
}
