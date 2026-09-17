package com.rememberber.mootool.next.compose.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.rememberber.mootool.next.compose.domain.SettingsRowPresentation
import com.rememberber.mootool.next.compose.ui.theme.AccentPresets
import com.rememberber.mootool.next.compose.ui.theme.MooTheme

@Composable
fun SettingsNavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
) {
    val colors = MooTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(MooTheme.dimens.navRadius)
    val inset = colors.styleId == "smartisan" || colors.styleId == "miui-v5"
    Row(
        modifier = modifier
            .mooSettingsNavItem()
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .semantics { role = Role.Tab; this.selected = selected }
            .mooFocusOutline(focused, shape)
            .clip(shape)
            .then(
                if (selected && colors.styleId == "miui-v5") {
                    Modifier.shadow(2.dp, shape, ambientColor = colors.shadowSoft, spotColor = colors.shadowSoft)
                } else Modifier
            )
            .background(colors.sidebarItemBrush(selected, card = false, hovered))
            .border(
                if (focused) 2.dp else 1.dp,
                when {
                    focused -> colors.focusRing
                    else -> colors.navItemBorder(selected, hovered)
                },
                shape
            )
            .hoverable(interaction)
            .focusable(true, interaction)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (inset) {
            Box(
                Modifier.width(3.dp).height(16.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (selected) colors.navActiveBar else Color.Transparent)
            )
        }
        if (!icon.isNullOrBlank()) {
            Text(
                icon,
                color = if (selected) colors.navSelectedIcon() else colors.textMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(22.dp),
            )
        }
        Text(
            label,
            color = if (selected) colors.navSelectedContent() else colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = MooTheme.colors
    val dimens = MooTheme.dimens
    val radius = dimens.shellRadius
    val fill = colors.settingsGroupRowsFill()
    val elevated = colors.styleId != "quiet"
    val elevation = dimens.shellElevation
    val stroke = colors.settingsGroupRowsBorder()
    val titleWeight = when (colors.styleId) {
        "smartisan" -> FontWeight(650)
        "modern", "hero" -> FontWeight.SemiBold
        else -> FontWeight.Medium
    }
    Column(
        modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            title,
            color = colors.textMuted,
            fontSize = 12.sp,
            fontWeight = titleWeight,
            letterSpacing = when (colors.styleId) {
                "miui-v5" -> 0.42.sp
                "claude" -> 0.15.sp
                else -> 0.sp
            },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (elevated) Modifier.shadow(
                        elevation,
                        RoundedCornerShape(radius),
                        ambientColor = colors.shadowSoft,
                        spotColor = colors.shadowSoft
                    ) else Modifier
                )
                .clip(RoundedCornerShape(radius))
                .background(fill)
                .border(1.dp, stroke, RoundedCornerShape(radius)),
            content = content
        )
    }
}

@Composable
fun SettingRow(label: String, control: @Composable () -> Unit) {
    val colors = MooTheme.colors
    val min = MooTheme.dimens.settingsRowMin
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = min)
                .mooSettingsSettingRow()
                .padding(
                    horizontal = SettingsRowPresentation.HORIZONTAL_PADDING_DP.dp,
                    vertical = SettingsRowPresentation.VERTICAL_PADDING_DP.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SettingsRowPresentation.LABEL_CONTROL_GAP_DP.dp)
        ) {
            Text(label, color = colors.textBody, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Box { control() }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSoft))
    }
}

@Composable
fun MooSegmented(
    options: List<Pair<String, String>>,
    value: String,
    onChange: (String) -> Unit
) {
    val colors = MooTheme.colors
    val style = colors.styleId
    val radius = when (style) {
        "hero" -> MooTheme.dimens.radiusLarge
        "miui-v5" -> 4.dp
        else -> MooTheme.dimens.radius
    }
    val track = when (style) {
        "hero" -> colors.surfaceCard
        "smartisan", "miui-v5" -> colors.inset
        else -> colors.control
    }
    val trackBorder = when (style) {
        "hero" -> Color.Transparent
        "modern" -> colors.borderSoft
        else -> colors.borderControl
    }
    val trackPadding = when (style) {
        "hero" -> 4.dp
        "miui-v5" -> 0.dp
        else -> 3.dp
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(radius))
            .background(track)
            .border(1.dp, trackBorder, RoundedCornerShape(radius))
            .padding(trackPadding),
        horizontalArrangement = Arrangement.spacedBy(if (style == "miui-v5") 0.dp else 2.dp)
    ) {
        options.forEachIndexed { index, (id, label) ->
            val active = value == id
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            val shape = when (style) {
                "hero" -> RoundedCornerShape(10.dp)
                "claude" -> RoundedCornerShape(8.dp)
                "miui-v5" -> RoundedCornerShape(0.dp)
                else -> RoundedCornerShape(6.dp)
            }
            val fill: Brush = when {
                active && style == "smartisan" -> Brush.verticalGradient(listOf(colors.raisedTop, colors.raisedBottom))
                active -> SolidColor(colors.workspace)
                else -> SolidColor(Color.Transparent)
            }
            val itemBorder = when {
                focused -> colors.focusRing
                active && style == "smartisan" -> colors.borderControl
                else -> Color.Transparent
            }
            val itemBorderWidth = when {
                focused -> 2.dp
                active && style == "smartisan" -> 1.dp
                else -> 0.dp
            }
            val raised = active && style != "smartisan" && style != "miui-v5"
            val textColor = when {
                active && style == "miui-v5" -> colors.navSelectedContent()
                active -> colors.textStrong
                else -> colors.textSecondary
            }
            Box(
                modifier = Modifier
                    .mooFocusOutline(focused, shape)
                    .clip(shape)
                    .then(
                        if (raised) Modifier.shadow(2.dp, shape, ambientColor = colors.shadowSoft, spotColor = colors.shadowSoft)
                        else Modifier
                    )
                    .background(fill, shape)
                    .border(itemBorderWidth, itemBorder, shape)
                    .then(
                        if (style == "miui-v5") {
                            Modifier.drawBehind {
                                if (index < options.lastIndex) {
                                    drawLine(
                                        colors.border,
                                        Offset(size.width, 0f),
                                        Offset(size.width, size.height),
                                        1.dp.toPx()
                                    )
                                }
                                if (active) {
                                    drawLine(
                                        colors.navActiveBar,
                                        Offset(0f, size.height - 1.5.dp.toPx()),
                                        Offset(size.width, size.height - 1.5.dp.toPx()),
                                        3.dp.toPx()
                                    )
                                }
                            }
                        } else {
                            Modifier
                        }
                    )
                    .clickable(
                        interactionSource = interaction,
                        indication = LocalIndication.current,
                        onClick = { onChange(id) }
                    )
                    .focusable(true, interaction)
                    .padding(horizontal = 11.dp, vertical = if (style == "miui-v5") 8.dp else 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun MooSelect(
    options: List<Pair<String, String>>,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    triggerModifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val label = options.firstOrNull { it.first == value }?.second ?: value
    Box(modifier.width(260.dp)) {
        MooButton(
            label,
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth().then(triggerModifier),
        )
        MooMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (id, text) ->
                MooMenuItem(text) {
                    open = false
                    onChange(id)
                }
            }
        }
    }
}

@Composable
fun AccentSwatches(
    value: String,
    onChange: (String) -> Unit,
    firstSwatchModifier: Modifier = Modifier,
) {
    val colors = MooTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
        AccentPresets.ids.forEachIndexed { index, id ->
            val mapped = AccentPresets.normalize(value)
            val active = mapped == id
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            val swatch = AccentPresets.swatch(id)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(30.dp)) {
                if (active) {
                    Box(Modifier.size(30.dp).clip(CircleShape).background(swatch))
                    Box(Modifier.size(26.dp).clip(CircleShape).background(colors.workspace))
                } else {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(colors.borderControl))
                    Box(Modifier.size(22.dp).clip(CircleShape).background(colors.workspace))
                }
                val swatchShape = RoundedCornerShape(percent = 50)
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .then(if (index == 0) firstSwatchModifier else Modifier)
                        .semantics { role = Role.RadioButton; this.selected = active }
                        .mooFocusOutline(focused, swatchShape)
                        .clip(CircleShape)
                        .background(swatch)
                        .border(2.dp, colors.workspace, CircleShape)
                        .focusable(true, interaction)
                        .clickable(
                            interactionSource = interaction,
                            indication = LocalIndication.current,
                            onClick = { onChange(id) }
                        )
                )
            }
        }
    }
}

@Composable
fun SettingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    enabled: Boolean = true
) {
    MooTextField(
        value,
        onValueChange,
        modifier = Modifier.width(260.dp),
        placeholder = placeholder,
        enabled = enabled
    )
}

/** Draft while focused; commits trimmed value on blur when changed (aligns with Electron `TextSetting`). */
@Composable
fun SettingCommitTextField(
    value: String,
    onCommit: (String) -> Boolean,
    placeholder: String = "",
    enabled: Boolean = true,
) {
    var draft by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        draft = value
    }
    MooTextField(
        draft,
        { draft = it },
        modifier = Modifier
            .width(260.dp)
            .onFocusChanged { state ->
                if (!state.isFocused && draft.trim() != value.trim()) {
                    if (!onCommit(draft)) {
                        draft = value
                    }
                }
            },
        placeholder = placeholder,
        enabled = enabled,
    )
}

@Composable
fun MooSwitch(checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    val colors = MooTheme.colors
    val style = colors.styleId
    val width = when (style) {
        "miui-v5" -> 42.dp
        "hero", "smartisan", "claude" -> 40.dp
        else -> 38.dp
    }
    val height = when (style) {
        "miui-v5" -> 22.dp
        "hero", "smartisan", "claude" -> 24.dp
        else -> 22.dp
    }
    val thumb = when (style) {
        "miui-v5" -> 16.dp
        "smartisan" -> 18.dp
        "hero", "claude" -> 20.dp
        else -> 18.dp
    }
    val trackShape = if (style == "miui-v5") RoundedCornerShape(3.dp) else RoundedCornerShape(height / 2)
    val thumbShape = if (style == "miui-v5") RoundedCornerShape(2.dp) else CircleShape
    val borderless = style == "modern" || style == "quiet" || style == "hero" || style == "claude"
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val track = when {
        checked && (style == "smartisan" || style == "miui-v5" || style == "claude") -> SolidColor(colors.navActiveBar)
        checked -> SolidColor(colors.accentAction)
        style == "smartisan" || style == "miui-v5" -> SolidColor(colors.inset)
        style == "hero" -> SolidColor(colors.surfaceCardHover)
        style == "claude" -> SolidColor(colors.borderControl)
        else -> SolidColor(colors.borderControlHover)
    }
    val borderColor = when {
        focused -> colors.focusRing
        borderless -> Color.Transparent
        checked && style == "smartisan" -> colors.navActiveBar
        checked && style == "miui-v5" -> colors.navSelectedContent()
        checked -> colors.accentAction
        else -> colors.borderControl
    }
    val thumbFill: Brush = when (style) {
        "smartisan", "miui-v5" -> Brush.verticalGradient(listOf(colors.raisedTop, colors.raisedBottom))
        "claude" -> SolidColor(Color(0xFFFFFEFA))
        else -> SolidColor(Color.White)
    }
    Box(
        modifier = Modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.42f }
            .size(width, height)
            .mooFocusOutline(focused, trackShape)
            .clip(trackShape)
            .background(track, trackShape)
            .border(if (focused) 2.dp else if (borderless) 0.dp else 1.dp, borderColor, trackShape)
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
                .clip(thumbShape)
                .then(
                    if (style == "smartisan" || style == "miui-v5") {
                        Modifier.border(1.dp, colors.borderControl, thumbShape)
                    } else {
                        Modifier
                    }
                )
                .background(thumbFill, thumbShape)
        )
    }
}
