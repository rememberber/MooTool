package com.rememberber.mootool.next.compose.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
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
    val background = if (primary) colors.accentAction else colors.control
    val content = if (primary) colors.onAccent else colors.textPrimary
    Row(
        modifier = modifier
            .semantics { role = Role.Button; contentDescription = label }
            .clip(RoundedCornerShape(8.dp))
            .background(background.copy(alpha = if (enabled) 1f else 0.5f))
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(enabled = enabled, onClick = onClick)
            .defaultMinSize(minHeight = 32.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(label, color = content, fontSize = 13.sp)
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
    Row(
        modifier = modifier
            .semantics { role = Role.Button; contentDescription = label }
            .clip(RoundedCornerShape(8.dp))
            .background(colors.control)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick)
            .defaultMinSize(minWidth = 32.dp, minHeight = 32.dp)
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
    singleLine: Boolean = true
) {
    val colors = MooTheme.colors
    Box(
        modifier = modifier
            .then(if (singleLine) Modifier.height(32.dp) else Modifier.defaultMinSize(minHeight = 120.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(colors.workspace)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
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
            modifier = Modifier.matchParentSize()
        )
    }
}
