package com.rememberber.mootool.next.compose.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Text

@Composable
fun MooButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true
) {
    val colors = MooTheme.colors
    val radius = MooTheme.dimens.radius
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val content = if (primary) colors.onAccent else colors.textPrimary
    val shape = RoundedCornerShape(radius)
    val raised = colors.styleId == "smartisan" || colors.styleId == "hero" || colors.styleId == "miui-v5"
    Row(
        modifier = modifier
            .semantics { role = Role.Button; contentDescription = label }
            .then(if (raised && !primary) Modifier.shadow(2.dp, shape, ambientColor = colors.lowlight, spotColor = colors.lowlight) else Modifier)
            .clip(shape)
            .background(colors.controlBrush(primary))
            .border(if (focused) 2.dp else 1.dp, if (focused) colors.focusRing else colors.border, shape)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .focusable(enabled, interaction)
            .defaultMinSize(minHeight = MooTheme.dimens.controlHeight)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(label, color = content.copy(alpha = if (enabled) 1f else 0.5f), fontSize = 13.sp)
    }
}

data class OverflowAction(
    val label: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

@Composable
fun OverflowActionCluster(
    overflow: Boolean,
    moreLabel: String,
    actions: List<OverflowAction>
) {
    if (actions.isEmpty()) return
    if (!overflow) {
        actions.forEach { action ->
            MooButton(action.label, onClick = action.onClick, enabled = action.enabled)
        }
        return
    }
    var open by remember { mutableStateOf(false) }
    Box {
        MooButton(moreLabel, primary = open, onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    onClick = {
                        open = false
                        action.onClick()
                    },
                    enabled = action.enabled
                ) {
                    Text(action.label)
                }
            }
        }
    }
}

@Composable
fun MooIconButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val colors = MooTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(MooTheme.dimens.radius)
    Row(
        modifier = modifier
            .semantics { role = Role.Button; contentDescription = label }
            .clip(shape)
            .background(colors.controlBrush())
            .border(if (focused) 2.dp else 1.dp, if (focused) colors.focusRing else colors.border, shape)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
            .focusable(true, interaction)
            .defaultMinSize(minWidth = 32.dp, minHeight = MooTheme.dimens.controlHeight)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
fun MooTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    fieldModifier: Modifier = Modifier
) {
    val colors = MooTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(MooTheme.dimens.radius)
    Box(
        modifier = modifier
            .then(if (singleLine) Modifier.height(MooTheme.dimens.controlHeight) else Modifier.defaultMinSize(minHeight = 120.dp))
            .clip(shape)
            .background(colors.inset)
            .border(if (focused) 2.dp else 1.dp, if (focused) colors.focusRing else colors.border, shape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = colors.textSecondary, fontSize = 13.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(color = colors.textPrimary, fontSize = 13.sp),
            cursorBrush = SolidColor(colors.accent),
            interactionSource = interaction,
            modifier = Modifier.matchParentSize().then(fieldModifier)
        )
    }
}

@Composable
fun Modifier.mooToolbarBackground(): Modifier {
    val colors = MooTheme.colors
    return background(colors.toolbarBrush())
}

@Composable
fun Modifier.mooWorkspaceBackground(): Modifier {
    val colors = MooTheme.colors
    return if (colors.styleId == "smartisan" || colors.styleId == "hero" || colors.styleId == "claude") {
        drawBehind {
            drawRect(colors.workspace)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(colors.surfaceSubtle.copy(alpha = 0.56f), Color.Transparent),
                    center = Offset(size.width * 0.5f, -size.height * 0.2f),
                    radius = size.maxDimension * 0.72f
                )
            )
        }
    } else {
        background(colors.workspace)
    }
}

@Composable
fun Modifier.mooSidebarBackground(): Modifier {
    val colors = MooTheme.colors
    return if (colors.styleId == "smartisan" || colors.styleId == "miui-v5") {
        background(
            Brush.horizontalGradient(
                listOf(colors.sidebar, lerp(colors.sidebar, colors.workspace, 0.12f))
            )
        )
    } else {
        background(colors.sidebar)
    }
}
