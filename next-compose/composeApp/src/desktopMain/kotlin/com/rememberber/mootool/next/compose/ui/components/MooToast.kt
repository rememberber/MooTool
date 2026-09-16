package com.rememberber.mootool.next.compose.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.ToastItem
import com.rememberber.mootool.next.compose.app.ToastTone
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import kotlinx.coroutines.delay

@Composable
fun MooToastHost(container: AppContainer, windowFocused: Boolean = true) {
    if (!windowFocused) return
    val items by container.toasts.items.collectAsState()
    if (items.isEmpty()) return
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        Column(
            modifier = Modifier
                .padding(22.dp)
                .widthIn(max = 360.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            items.forEach { item ->
                LaunchedEffect(item.id) {
                    if (item.durationMs > 0) {
                        delay(item.durationMs)
                        container.toasts.dismiss(item.id)
                    }
                }
                MooToastCard(container, item)
            }
        }
    }
}

@Composable
private fun MooToastCard(container: AppContainer, item: ToastItem) {
    val colors = MooTheme.colors
    val radius = MooTheme.dimens.shellRadius
    val shape = RoundedCornerShape(radius)
    val icon = when (item.tone) {
        ToastTone.Success -> colors.success
        ToastTone.Error -> colors.danger
        ToastTone.Info -> colors.accent
    }
    val glyph = when (item.tone) {
        ToastTone.Success -> "✓"
        ToastTone.Error -> "!"
        ToastTone.Info -> "i"
    }
    val alert = item.tone == ToastTone.Error
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .shadow(14.dp, shape, ambientColor = colors.shadow, spotColor = colors.shadow)
            .clip(shape)
            .background(colors.workspace)
            .border(1.dp, colors.border, shape)
            .padding(start = 13.dp, top = 9.dp, end = 9.dp, bottom = 9.dp)
            .semantics {
                liveRegion = if (alert) LiveRegionMode.Assertive else LiveRegionMode.Polite
                contentDescription = item.message
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(glyph, color = icon, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.size(20.dp))
        Text(
            item.message,
            color = colors.textBody,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
        MooGhostButton(container.t("common.toast.dismiss"), onClick = { container.toasts.dismiss(item.id) }) {
            Text("×", color = colors.textMuted, fontSize = 16.sp)
        }
    }
}
