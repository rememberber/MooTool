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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.ui.theme.AccentPresets
import com.rememberber.mootool.next.compose.ui.theme.MooTheme

@Composable
fun SettingsNavItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MooTheme.colors
    val radius = MooTheme.dimens.radius
    val raised = colors.styleId == "smartisan" || colors.styleId == "hero" || colors.styleId == "claude" || colors.styleId == "miui-v5"
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(radius)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .semantics { role = Role.Tab; this.selected = selected }
            .clip(shape)
            .then(
                if (selected && raised) Modifier.shadow(2.dp, shape, ambientColor = colors.lowlight, spotColor = colors.lowlight)
                else Modifier
            )
            .background(if (selected) colors.selected else Color.Transparent)
            .border(if (focused) 2.dp else 0.dp, if (focused) colors.focusRing else Color.Transparent, shape)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.width(3.dp).height(16.dp).clip(RoundedCornerShape(2.dp))
                .background(if (selected) colors.navActiveBar else Color.Transparent)
        )
        Text(label, color = if (selected) colors.textPrimary else colors.textSecondary, fontSize = 13.sp)
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = MooTheme.colors
    val radius = MooTheme.dimens.radiusLarge
    val elevated = colors.styleId == "hero" || colors.styleId == "claude" || colors.styleId == "smartisan" || colors.styleId == "modern"
    Column(
        modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, color = colors.textSecondary, fontSize = 12.sp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (elevated) Modifier.shadow(6.dp, RoundedCornerShape(radius), ambientColor = colors.lowlight, spotColor = colors.lowlight)
                    else Modifier
                )
                .clip(RoundedCornerShape(radius))
                .background(colors.surfaceSubtle)
                .border(1.dp, colors.border, RoundedCornerShape(radius)),
            content = content
        )
    }
}

@Composable
fun SettingRow(label: String, control: @Composable () -> Unit) {
    val colors = MooTheme.colors
    val min = if (colors.styleId == "hero") 58.dp else 54.dp
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = min).padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(label, color = colors.textPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Box { control() }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border.copy(alpha = 0.7f)))
    }
}

@Composable
fun MooSegmented(
    options: List<Pair<String, String>>,
    value: String,
    onChange: (String) -> Unit
) {
    val colors = MooTheme.colors
    val radius = MooTheme.dimens.radius
    val raised = colors.styleId == "smartisan" || colors.styleId == "hero" || colors.styleId == "miui-v5"
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(radius))
            .background(colors.control)
            .border(1.dp, colors.border, RoundedCornerShape(radius))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { (id, label) ->
            val active = value == id
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            val shape = RoundedCornerShape(6.dp)
            Box(
                modifier = Modifier
                    .clip(shape)
                    .then(
                        if (active && raised) Modifier.shadow(2.dp, shape, ambientColor = colors.lowlight, spotColor = colors.lowlight)
                        else Modifier
                    )
                    .background(if (active) colors.workspace else Color.Transparent)
                    .border(if (focused) 2.dp else 0.dp, if (focused) colors.focusRing else Color.Transparent, shape)
                    .clickable(
                        interactionSource = interaction,
                        indication = LocalIndication.current,
                        onClick = { onChange(id) }
                    )
                    .focusable(true, interaction)
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = if (active) colors.textPrimary else colors.textSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MooSelect(
    options: List<Pair<String, String>>,
    value: String,
    onChange: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val label = options.firstOrNull { it.first == value }?.second ?: value
    Box(Modifier.width(260.dp)) {
        MooButton(label, onClick = { open = true }, modifier = Modifier.fillMaxWidth())
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (id, text) ->
                DropdownMenuItem(onClick = {
                    open = false
                    onChange(id)
                }) {
                    Text(text, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun AccentSwatches(value: String, onChange: (String) -> Unit) {
    val colors = MooTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
        AccentPresets.ids.forEach { id ->
            val mapped = AccentPresets.normalize(value)
            val active = mapped == id
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(AccentPresets.swatch(id))
                    .border(
                        if (focused || active) 2.dp else 1.dp,
                        when {
                            focused -> colors.focusRing
                            active -> colors.textPrimary
                            else -> colors.border
                        },
                        CircleShape
                    )
                    .clickable(
                        interactionSource = interaction,
                        indication = LocalIndication.current,
                        onClick = { onChange(id) }
                    )
                    .focusable(true, interaction)
                    .semantics { role = Role.RadioButton; this.selected = active }
            )
        }
    }
}

@Composable
fun SettingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = ""
) {
    MooTextField(value, onValueChange, modifier = Modifier.width(260.dp), placeholder = placeholder)
}

@Composable
fun MooSwitch(checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    val colors = MooTheme.colors
    val width = 40.dp
    val height = 24.dp
    val thumb = 20.dp
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        modifier = Modifier
            .size(width, height)
            .clip(RoundedCornerShape(height / 2))
            .background(if (checked) colors.accentAction else colors.control)
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) colors.focusRing else if (checked) colors.accentAction else colors.border,
                RoundedCornerShape(height / 2)
            )
            .clickable(enabled = enabled, interactionSource = interaction, indication = LocalIndication.current) {
                onCheckedChange(!checked)
            }
            .focusable(enabled, interaction)
            .semantics { role = Role.Switch; this.selected = checked },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .padding(2.dp)
                .offset(x = if (checked) (width - thumb - 4.dp) else 0.dp)
                .size(thumb)
                .clip(CircleShape)
                .background(if (checked) colors.onAccent else colors.workspace)
        )
    }
}
