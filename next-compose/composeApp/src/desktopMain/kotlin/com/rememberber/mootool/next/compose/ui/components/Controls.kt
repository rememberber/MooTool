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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.ui.theme.LocalCompactNavigation
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import androidx.compose.material.Text

@Composable
fun MooButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    prominent: Boolean = false,
    danger: Boolean = false,
    dense: Boolean = false,
    p5Toolbar: Boolean = false,
    enabled: Boolean = true
) {
    val colors = MooTheme.colors
    val dark = MooTheme.dark
    val radius = MooTheme.dimens.radius
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val content = when {
        danger -> colors.danger
        prominent -> colors.prominentContent(dark)
        primary && (colors.styleId == "hero" || colors.styleId == "claude" || colors.styleId == "miui-v5") -> colors.navSelectedContent()
        primary -> colors.textStrong
        else -> colors.textBody
    }
    val p5Dense = p5Toolbar && LayoutPolicy.p5ToolbarDense(LocalCompactNavigation.current)
    val nonP5CornerDp = if (!p5Toolbar) {
        LayoutPolicy.nonP5ToolbarCornerRadiusDp(dense, colors.styleId)
    } else {
        null
    }
    val shape = when {
        p5Toolbar -> RoundedCornerShape(6.dp)
        nonP5CornerDp != null -> RoundedCornerShape(nonP5CornerDp.dp)
        else -> RoundedCornerShape(radius)
    }
    val p5HeightDp = LayoutPolicy.p5ToolbarButtonHeightDp(
        dense = dense || p5Dense,
        controlHeightDp = MooTheme.dimens.controlHeight.value,
    )
    val compactHeight = when {
        dense || p5Dense -> p5HeightDp.dp
        p5Toolbar -> p5HeightDp.dp
        else -> MooTheme.dimens.controlHeight
    }
    val compactPaddingH = when {
        dense || p5Dense -> 8.dp
        p5Toolbar -> 9.dp
        else -> 10.dp
    }
    val compactPaddingV = when {
        dense || p5Dense -> 3.dp
        p5Toolbar -> 4.dp
        else -> 6.dp
    }
    val p5FontSp = if (p5Toolbar) {
        LayoutPolicy.p5ToolbarFontSp(dense || p5Dense, colors.styleId)
    } else {
        null
    }
    val nonP5FontSp = if (!p5Toolbar) {
        LayoutPolicy.nonP5ToolbarFontSp(dense, colors.styleId)
    } else {
        null
    }
    val compactFontSize = when {
        p5FontSp != null -> p5FontSp.sp
        nonP5FontSp != null -> nonP5FontSp.sp
        dense -> 11.sp
        else -> 12.sp
    }
    val compactFontWeight = when {
        p5Toolbar && LayoutPolicy.p5ToolbarFontWeightMedium(dense || p5Dense, colors.styleId) -> FontWeight.Medium
        !p5Toolbar && LayoutPolicy.nonP5ToolbarFontWeightMedium(dense, colors.styleId) -> FontWeight.Medium
        dense || p5Dense -> FontWeight.Medium
        else -> FontWeight.SemiBold
    }
    val dangerFill = colors.danger.copy(alpha = if (hovered || pressed) 0.22f else 0.14f).compositeOver(colors.workspace)
    val tactile = colors.styleId == "smartisan" || colors.styleId == "miui-v5"
    val fill = when {
        danger -> SolidColor(dangerFill)
        prominent -> SolidColor(colors.prominentFill(dark, hovered, pressed))
        primary && pressed -> SolidColor(colors.pressedControlFill())
        primary && hovered -> SolidColor(colors.hoveredControlFill())
        primary -> SolidColor(colors.navSelectedFill())
        tactile && pressed && colors.styleId == "miui-v5" -> SolidColor(colors.inset)
        tactile && pressed -> Brush.verticalGradient(listOf(colors.raisedBottom, colors.raisedTop))
        tactile -> colors.controlBrush()
        pressed -> SolidColor(colors.pressedControlFill())
        hovered -> SolidColor(colors.hoveredControlFill())
        else -> colors.controlBrush()
    }
    val prominentColor = colors.prominentFill(dark, hovered, pressed)
    val borderColor = when {
        focused -> colors.focusRing
        danger -> colors.danger.copy(alpha = if (hovered) 0.45f else 0.28f)
        prominent -> prominentColor
        primary -> colors.borderSoft
        hovered -> colors.borderControlHover
        colors.styleId == "modern" || colors.styleId == "quiet" -> Color.Transparent
        else -> colors.borderControl
    }
    val elevation = when {
        p5Toolbar && primary && (colors.styleId == "modern" || colors.styleId == "quiet") -> 1.dp
        !p5Toolbar && primary && (colors.styleId == "modern" || colors.styleId == "quiet") -> 1.dp
        pressed || primary || danger || colors.styleId == "smartisan" || colors.styleId == "miui-v5" -> 0.dp
        else -> 1.dp
    }
    Row(
        modifier = modifier
            .semantics { role = Role.Button; contentDescription = label }
            .graphicsLayer { alpha = if (enabled) 1f else 0.42f }
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(elevation, shape, ambientColor = colors.shadowSoft, spotColor = colors.shadowSoft)
                } else {
                    Modifier
                }
            )
            .mooFocusOutline(focused, shape)
            .clip(shape)
            .background(fill)
            .border(if (focused) 2.dp else 1.dp, borderColor, shape)
            .pointerHoverIcon(PointerIcon.Hand)
            .hoverable(interaction)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .focusable(enabled, interaction)
            .defaultMinSize(minHeight = compactHeight)
            .padding(horizontal = compactPaddingH, vertical = compactPaddingV),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            label,
            color = content,
            fontSize = compactFontSize,
            fontWeight = compactFontWeight,
            letterSpacing = if (dense) 0.sp else (-0.06).sp
        )
    }
}

/** Electron `.selected-file-name`：有文件名时 11sp faint/muted 省略。 */
@Composable
fun SelectedFileName(
    fileName: String,
    modifier: Modifier = Modifier
) {
    if (fileName.isEmpty()) return
    val colors = MooTheme.colors
    Text(
        text = fileName,
        color = colors.textMuted,
        fontSize = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/** Electron `.file-drop-row`：选择按钮 + 11sp muted 省略文件名；可选系统文件拖放。 */
@Composable
fun FileDropRow(
    chooseLabel: String,
    fileName: String,
    emptyLabel: String,
    onChoose: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    p5Toolbar: Boolean = false,
    onDropFiles: ((List<java.io.File>) -> Unit)? = null,
    acceptMultiple: Boolean = false,
) {
    val colors = MooTheme.colors
    val dropModifier = if (onDropFiles != null) {
        Modifier.desktopFileDropTarget(
            enabled = enabled,
            acceptMultiple = acceptMultiple,
            onDrop = onDropFiles
        )
    } else {
        Modifier
    }
    Row(
        modifier = modifier.fillMaxWidth().then(dropModifier).padding(if (onDropFiles != null) 2.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MooButton(chooseLabel, onClick = onChoose, enabled = enabled, p5Toolbar = p5Toolbar)
        Text(
            text = fileName.ifEmpty { emptyLabel },
            color = colors.textMuted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
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
    actions: List<OverflowAction>,
    p5Toolbar: Boolean = false,
) {
    if (actions.isEmpty()) return
    if (!overflow) {
        actions.forEach { action ->
            MooButton(action.label, onClick = action.onClick, enabled = action.enabled, p5Toolbar = p5Toolbar)
        }
        return
    }
    var open by remember { mutableStateOf(false) }
    Box {
        MooButton(moreLabel, primary = open, onClick = { open = true }, p5Toolbar = p5Toolbar)
        MooMenu(expanded = open, onDismissRequest = { open = false }) {
            actions.forEach { action ->
                MooMenuItem(action.label, enabled = action.enabled) {
                    open = false
                    action.onClick()
                }
            }
        }
    }
}

@Composable
fun MooMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MooTheme.colors
    val shape = RoundedCornerShape(7.dp)
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .widthIn(min = 156.dp)
            .background(colors.workspace, shape)
            .border(1.dp, colors.borderControl, shape)
            .padding(5.dp)
    ) {
        content()
    }
}

@Composable
fun MooMenuItem(
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val colors = MooTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(5.dp)
    val active = hovered || focused
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(shape)
            .background(if (active) colors.hoveredControlFill() else Color.Transparent)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .focusable(enabled, interaction)
            .hoverable(interaction, enabled)
            .padding(horizontal = 9.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f },
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun MooMenuItem(label: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = MooTheme.colors
    MooMenuItem(enabled = enabled, onClick = onClick, modifier = modifier) {
        Text(
            label,
            fontSize = 11.sp,
            color = colors.textBody,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MooMenuSeparator() {
    val colors = MooTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 7.dp, vertical = 4.dp)
            .height(1.dp)
            .background(colors.borderControl)
    )
}

@Composable
fun FavoriteRow(
    name: String,
    snippet: String,
    deleteLabel: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    group: String = ""
) {
    val colors = MooTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surfaceSubtle)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            Modifier
                .weight(1f)
                .mooFocusClickable(onClick = onOpen)
                .padding(horizontal = 5.dp, vertical = 3.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                name,
                color = colors.textBody,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (group.isNotBlank()) {
                Text(group, color = colors.textMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                snippet,
                color = colors.textMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        MooGhostButton(deleteLabel, onClick = onDelete, size = 28.dp) {
            Text("×", color = colors.textMuted, fontSize = 16.sp)
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
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val cornerDp = LayoutPolicy.iconButtonCornerRadiusDp(colors.styleId)
    val shape = RoundedCornerShape(cornerDp.dp)
    val tactile = colors.styleId == "smartisan" || colors.styleId == "miui-v5"
    val fill = when {
        tactile && pressed && colors.styleId == "miui-v5" -> SolidColor(colors.inset)
        tactile && pressed -> Brush.verticalGradient(listOf(colors.raisedBottom, colors.raisedTop))
        tactile -> colors.controlBrush()
        pressed -> SolidColor(colors.pressedControlFill())
        hovered -> SolidColor(colors.hoveredControlFill())
        else -> SolidColor(Color.Transparent)
    }
    val borderColor = when {
        focused -> colors.focusRing
        tactile && hovered -> colors.borderControlHover
        tactile -> colors.borderControl
        hovered -> colors.borderControlHover
        else -> Color.Transparent
    }
    val elevation = if (LayoutPolicy.iconButtonSoftShadow(colors.styleId) && !tactile) 1.dp else 0.dp
    Row(
        modifier = modifier
            .semantics { role = Role.Button; contentDescription = label }
            .then(
                if (elevation > 0.dp) {
                    Modifier.shadow(elevation, shape, ambientColor = colors.shadowSoft, spotColor = colors.shadowSoft)
                } else Modifier
            )
            .mooFocusOutline(focused, shape)
            .clip(shape)
            .background(fill)
            .border(if (focused) 2.dp else 1.dp, borderColor, shape)
            .pointerHoverIcon(PointerIcon.Hand)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
            .focusable(true, interaction)
            .defaultMinSize(minWidth = MooTheme.dimens.controlHeight, minHeight = MooTheme.dimens.controlHeight)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
fun MooGhostButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    content: @Composable RowScope.() -> Unit
) {
    val colors = MooTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val cornerDp = LayoutPolicy.iconGhostCornerRadiusDp(colors.styleId)
    val shape = RoundedCornerShape(cornerDp.dp)
    val fill = if (hovered) colors.hoveredControlFill() else Color.Transparent
    Row(
        modifier = modifier
            .semantics { role = Role.Button; contentDescription = label }
            .mooFocusOutline(focused, shape)
            .clip(shape)
            .background(fill)
            .pointerHoverIcon(PointerIcon.Hand)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
            .focusable(true, interaction)
            .size(size),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
fun MooKbd(text: String) {
    val colors = MooTheme.colors
    val shape = RoundedCornerShape(5.dp)
    Text(
        text,
        color = colors.textBody,
        fontSize = 12.sp,
        modifier = Modifier
            .defaultMinSize(minWidth = 84.dp)
            .clip(shape)
            .background(colors.control)
            .drawBehind {
                drawLine(
                    colors.borderControl,
                    Offset(0f, size.height - 2.dp.toPx()),
                    Offset(size.width, size.height - 2.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }
            .border(1.dp, colors.borderControl, shape)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
fun MooCompactListButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = MooTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(5.dp)
    val fill = when {
        !enabled -> colors.workspace.copy(alpha = 0.4f)
        hovered -> colors.control
        else -> colors.workspace
    }
    Text(
        label,
        color = colors.textBody,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        modifier = Modifier
            .fillMaxWidth()
            .mooFocusOutline(focused, shape)
            .clip(shape)
            .background(fill)
            .border(1.dp, colors.borderControl, shape)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clickable(enabled = enabled, interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
            .focusable(enabled, interaction)
            .hoverable(interaction, enabled)
            .defaultMinSize(minHeight = 30.dp)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    )
}

@Composable
fun MooTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    compact: Boolean = false,
    dense: Boolean = false,
    borderless: Boolean = false,
    code: Boolean = false,
    mono: Boolean = false,
    hostsContent: Boolean = false,
    enabled: Boolean = true,
    fieldModifier: Modifier = Modifier
) {
    val colors = MooTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val typeface = if (mono || code || hostsContent) FontFamily.Monospace else FontFamily.Default
    val borderlessField = borderless || hostsContent
    val shape = RoundedCornerShape(
        when {
            borderlessField -> 0.dp
            compact -> 4.dp
            dense || code -> 6.dp
            else -> MooTheme.dimens.radius
        }
    )
    val border = when {
        borderlessField -> Color.Transparent
        focused -> colors.focusRing
        hovered -> colors.borderControlHover
        compact -> Color.Transparent
        else -> colors.borderControl
    }
    val inset = !compact && !dense && !borderlessField && !code && (colors.styleId == "smartisan" || colors.styleId == "miui-v5")
    val typeSize = when {
        compact -> 12.sp
        code -> 10.sp
        hostsContent -> 12.sp
        dense -> 11.sp
        borderless && !singleLine -> 14.sp
        else -> 13.sp
    }
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.42f)
            .then(
                when {
                    !singleLine && (borderlessField || code) -> Modifier
                    !singleLine -> Modifier.defaultMinSize(minHeight = 120.dp)
                    compact -> Modifier.height(27.dp)
                    dense -> Modifier.height(30.dp)
                    else -> Modifier.height(MooTheme.dimens.controlHeight)
                }
            )
            .mooFocusOutline(focused, shape)
            .clip(shape)
            .background(colors.workspace)
            .then(
                if (inset) {
                    Modifier.drawBehind {
                        val band = if (colors.styleId == "smartisan") 4.dp.toPx() else 3.dp.toPx()
                        drawRect(colors.lowlight.copy(alpha = 0.22f), Offset.Zero, Size(size.width, band))
                    }
                } else {
                    Modifier
                }
            )
            .border(if (focused && !borderlessField) 2.dp else 1.dp, border, shape)
            .hoverable(interaction)
            .padding(
                horizontal = when {
                    hostsContent -> 18.dp
                    borderless -> 20.dp
                    compact -> 6.dp
                    code -> 7.dp
                    dense && !singleLine -> 10.dp
                    dense -> 8.dp
                    else -> 8.dp
                },
                vertical = when {
                    hostsContent -> 16.dp
                    borderless -> 20.dp
                    compact -> 2.dp
                    code -> 7.dp
                    dense && !singleLine -> 9.dp
                    dense -> 4.dp
                    else -> 6.dp
                }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(
                placeholder,
                color = colors.textSecondary,
                fontSize = typeSize,
                fontFamily = typeface,
                fontWeight = if (compact) FontWeight.Normal else FontWeight.Medium,
                letterSpacing = if (compact) 0.sp else (-0.065).sp
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            textStyle = TextStyle(
                color = colors.textPrimary,
                fontSize = typeSize,
                fontFamily = typeface,
                fontWeight = if (compact) FontWeight.Normal else FontWeight.Medium,
                letterSpacing = if (compact) 0.sp else (-0.065).sp
            ),
            cursorBrush = SolidColor(colors.accent),
            interactionSource = interaction,
            modifier = Modifier.matchParentSize().then(fieldModifier)
        )
    }
}

@Composable
fun MooTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    compact: Boolean = false,
    dense: Boolean = false,
    borderless: Boolean = false,
    code: Boolean = false,
    mono: Boolean = false,
    hostsContent: Boolean = false,
    enabled: Boolean = true,
    fieldModifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val colors = MooTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val typeface = if (mono || code || hostsContent) FontFamily.Monospace else FontFamily.Default
    val borderlessField = borderless || hostsContent
    val shape = RoundedCornerShape(
        when {
            borderlessField -> 0.dp
            compact -> 4.dp
            dense || code -> 6.dp
            else -> MooTheme.dimens.radius
        }
    )
    val border = when {
        borderlessField -> Color.Transparent
        focused -> colors.focusRing
        hovered -> colors.borderControlHover
        compact -> Color.Transparent
        else -> colors.borderControl
    }
    val inset = !compact && !dense && !borderlessField && !code && (colors.styleId == "smartisan" || colors.styleId == "miui-v5")
    val typeSize = when {
        compact -> 12.sp
        code -> 10.sp
        hostsContent -> 12.sp
        dense -> 11.sp
        borderless && !singleLine -> 14.sp
        else -> 13.sp
    }
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.42f)
            .then(
                when {
                    !singleLine && (borderlessField || code) -> Modifier
                    !singleLine -> Modifier.defaultMinSize(minHeight = 120.dp)
                    compact -> Modifier.height(27.dp)
                    dense -> Modifier.height(30.dp)
                    else -> Modifier.height(MooTheme.dimens.controlHeight)
                }
            )
            .mooFocusOutline(focused, shape)
            .clip(shape)
            .background(colors.workspace)
            .then(
                if (inset) {
                    Modifier.drawBehind {
                        val band = if (colors.styleId == "smartisan") 4.dp.toPx() else 3.dp.toPx()
                        drawRect(colors.lowlight.copy(alpha = 0.22f), Offset.Zero, Size(size.width, band))
                    }
                } else {
                    Modifier
                }
            )
            .border(if (focused && !borderlessField) 2.dp else 1.dp, border, shape)
            .hoverable(interaction)
            .padding(
                horizontal = when {
                    hostsContent -> 18.dp
                    borderless -> 20.dp
                    compact -> 6.dp
                    code -> 7.dp
                    dense && !singleLine -> 10.dp
                    dense -> 8.dp
                    else -> 8.dp
                },
                vertical = when {
                    hostsContent -> 16.dp
                    borderless -> 20.dp
                    compact -> 2.dp
                    code -> 7.dp
                    dense && !singleLine -> 9.dp
                    dense -> 4.dp
                    else -> 6.dp
                }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.text.isEmpty()) {
            Text(
                placeholder,
                color = colors.textSecondary,
                fontSize = typeSize,
                fontFamily = typeface,
                fontWeight = if (compact) FontWeight.Normal else FontWeight.Medium,
                letterSpacing = if (compact) 0.sp else (-0.065).sp
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            textStyle = TextStyle(
                color = colors.textPrimary,
                fontSize = typeSize,
                fontFamily = typeface,
                fontWeight = if (compact) FontWeight.Normal else FontWeight.Medium,
                letterSpacing = if (compact) 0.sp else (-0.065).sp
            ),
            cursorBrush = SolidColor(colors.accent),
            interactionSource = interaction,
            modifier = Modifier.matchParentSize().then(fieldModifier)
        )
    }
}

@Composable
fun MooCompactSearch(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
) {
    val colors = MooTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(MooTheme.dimens.radius)
    val border = when {
        focused -> colors.accent
        hovered -> colors.borderControlHover
        else -> colors.borderControl
    }
    Row(
        modifier = modifier
            .height(30.dp)
            .mooFocusOutline(focused, shape)
            .clip(shape)
            .background(colors.workspace)
            .then(
                if (focused && (colors.styleId != "smartisan" && colors.styleId != "miui-v5")) {
                    Modifier.shadow(1.dp, shape, ambientColor = colors.focusRing.copy(alpha = 0.35f), spotColor = colors.focusRing.copy(alpha = 0.35f))
                } else Modifier
            )
            .border(1.dp, border, shape)
            .hoverable(interaction)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f).height(26.dp), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(placeholder, color = colors.textMuted, fontSize = 12.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.textBody, fontSize = 12.sp),
                cursorBrush = SolidColor(colors.accent),
                interactionSource = interaction,
                modifier = Modifier.fillMaxWidth().then(fieldModifier)
            )
        }
    }
}

@Composable
fun MooCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MooTheme.colors
    val radius = MooTheme.dimens.cardRadius
    val shape = RoundedCornerShape(radius)
    val fill: Brush = if (colors.styleId == "smartisan") {
        colors.controlBrush()
    } else {
        SolidColor(colors.workspace)
    }
    val stroke = when (colors.styleId) {
        "smartisan" -> colors.borderControl
        "miui-v5" -> colors.border
        else -> colors.borderSoft
    }
    val elevation = if (colors.styleId == "hero") 4.dp else 2.dp
    Column(
        modifier = modifier
            .shadow(elevation, shape, ambientColor = colors.shadowSoft, spotColor = colors.shadowSoft)
            .clip(shape)
            .background(fill)
            .border(1.dp, stroke, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
fun MooPageTitle(text: String, modifier: Modifier = Modifier, large: Boolean = false, settings: Boolean = false) {
    val colors = MooTheme.colors
    val style = colors.styleId
    val size = when {
        settings && style == "modern" -> 18.sp
        settings || large -> 22.sp
        else -> 16.sp
    }
    val weight = when (style) {
        "hero" -> FontWeight.Bold
        "claude" -> FontWeight(560)
        "miui-v5" -> FontWeight.SemiBold
        else -> FontWeight(650)
    }
    Text(
        text,
        modifier = modifier,
        color = colors.textStrong,
        fontSize = size,
        fontWeight = weight,
        letterSpacing = when {
            style == "claude" -> (-0.4).sp
            size >= 22.sp -> (-0.55).sp
            size >= 18.sp -> (-0.27).sp
            else -> (-0.4).sp
        },
        style = if (style == "smartisan") {
            TextStyle(shadow = Shadow(color = colors.highlight, offset = Offset(0f, 1f), blurRadius = 0f))
        } else {
            TextStyle.Default
        }
    )
}

@Composable
fun MooToolTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = MooTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
    val fill = if (hovered && enabled) colors.control else Color.Transparent
    val content = when {
        !enabled -> colors.textMuted.copy(alpha = 0.42f)
        selected -> colors.textStrong
        hovered -> colors.textBody
        else -> colors.textMuted
    }
    Box(
        modifier = modifier
            .semantics { role = Role.Tab; contentDescription = label }
            .mooFocusOutline(focused, shape)
            .clip(shape)
            .background(fill)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .hoverable(interaction, enabled = enabled)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .focusable(enabled, interaction)
            .height(37.dp)
            .drawBehind {
                if (selected) {
                    val inset = 12.dp.toPx()
                    val y = size.height - 1.dp.toPx()
                    drawLine(colors.accentAction, Offset(inset, y), Offset(size.width - inset, y), 2.dp.toPx())
                }
            }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = content,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun MooToolTabsRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .mooToolTabsBackground()
            .padding(start = 10.dp, end = 10.dp, top = 7.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}

enum class MooStatusKind { Neutral, Valid, Error }

fun statusPillFill(kind: MooStatusKind, dark: Boolean): Color = when (kind) {
    MooStatusKind.Valid -> if (dark) Color(0xFF20352C) else Color(0xFFE8F3EE)
    MooStatusKind.Error -> if (dark) Color(0xFF3A2523) else Color(0xFFF8EBE8)
    MooStatusKind.Neutral -> Color.Unspecified
}

fun statusPillContent(kind: MooStatusKind, dark: Boolean, fallback: Color): Color = when (kind) {
    MooStatusKind.Valid -> if (dark) Color(0xFF82C8A6) else Color(0xFF35765B)
    MooStatusKind.Error -> if (dark) Color(0xFFE28C7D) else Color(0xFFB25448)
    MooStatusKind.Neutral -> fallback
}

@Composable
fun MooStatusPill(
    text: String,
    tone: Color = MooTheme.colors.textBody,
    kind: MooStatusKind = MooStatusKind.Neutral
) {
    val colors = MooTheme.colors
    val dark = MooTheme.dark
    val style = colors.styleId
    val shape = RoundedCornerShape(
        when (style) {
            "miui-v5" -> 4.dp
            else -> 999.dp
        }
    )
    val kindFill = statusPillFill(kind, dark)
    val fillColor = when {
        kind != MooStatusKind.Neutral -> kindFill
        style == "claude" -> colors.navSelectedFill()
        style == "hero" -> colors.surfaceCard
        style == "miui-v5" -> colors.workspace
        else -> colors.surfaceCard
    }
    val fill: Brush = if (style == "smartisan" && kind == MooStatusKind.Neutral) {
        Brush.verticalGradient(listOf(colors.raisedTop, colors.raisedBottom))
    } else {
        SolidColor(fillColor)
    }
    val border = when {
        kind != MooStatusKind.Neutral -> Color.Transparent
        style == "smartisan" || style == "miui-v5" -> colors.border
        style == "claude" -> colors.accent.copy(alpha = 0.18f)
        else -> Color.Transparent
    }
    val textColor = if (kind == MooStatusKind.Neutral && style == "claude") {
        colors.navSelectedContent()
    } else {
        statusPillContent(kind, dark, tone)
    }
    Text(
        text,
        color = textColor,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .widthIn(max = 360.dp)
            .defaultMinSize(minHeight = 34.dp)
            .clip(shape)
            .background(fill)
            .then(if (border.alpha == 0f) Modifier else Modifier.border(1.dp, border, shape))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
fun MooStatusMeta(text: String, color: Color = MooTheme.colors.textMuted, modifier: Modifier = Modifier) {
    Text(text, color = color, fontSize = 10.sp, lineHeight = 12.sp, modifier = modifier)
}

@Composable
fun Modifier.mooFocusClickable(
    enabled: Boolean = true,
    shape: RoundedCornerShape = RoundedCornerShape(MooTheme.dimens.radius),
    onClick: () -> Unit
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    return this
        .mooFocusOutline(focused, shape)
        .focusable(enabled, interaction)
        .clickable(
            enabled = enabled,
            interactionSource = interaction,
            indication = LocalIndication.current,
            onClick = onClick
        )
}

@Composable
fun Modifier.mooFocusOutline(focused: Boolean, shape: RoundedCornerShape): Modifier {
    val ring = MooTheme.colors.focusRing
    if (!focused) return this
    return drawWithContent {
        drawContent()
        val stroke = 2.dp.toPx()
        val gap = 2.dp.toPx()
        val inset = -(gap + stroke / 2f)
        val corners = CornerRadius(shape.topStart.toPx(size, this))
        drawRoundRect(
            color = ring,
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2f, size.height - inset * 2f),
            cornerRadius = corners,
            style = Stroke(width = stroke)
        )
    }
}

@Composable
fun Modifier.mooToolShell(fill: Color? = null, flatten: Boolean = false, endBorder: Boolean = true, p5: Boolean = false): Modifier {
    val colors = MooTheme.colors
    val edge = flatten && !colors.restoresWorkspaceChrome() && !p5
    if (edge) {
        return background(fill ?: colors.workspace)
            .then(
                if (endBorder) {
                    Modifier.drawBehind {
                        drawLine(colors.borderSoft, Offset(size.width - 0.5f, 0f), Offset(size.width - 0.5f, size.height), 1.dp.toPx())
                    }
                } else {
                    Modifier
                }
            )
    }
    val radius = MooTheme.dimens.shellRadius
    val shape = RoundedCornerShape(radius)
    val elevation = if (p5) 8.dp else MooTheme.dimens.shellElevation
    val stroke = when (colors.styleId) {
        "smartisan", "miui-v5" -> colors.borderControl
        "hero" -> colors.borderSoft
        else -> colors.border
    }
    return padding(4.dp)
        .shadow(elevation, shape, ambientColor = colors.shadowSoft, spotColor = colors.shadowSoft)
        .clip(shape)
        .background(fill ?: colors.workspace)
        .border(1.dp, stroke, shape)
}

@Composable
fun Modifier.mooEditorFrame(flatten: Boolean = false): Modifier {
    val colors = MooTheme.colors
    val edge = flatten && !colors.restoresWorkspaceChrome()
    if (edge) {
        return background(colors.workspace)
            .drawBehind {
                drawLine(colors.borderSoft, Offset(size.width - 0.5f, 0f), Offset(size.width - 0.5f, size.height), 1.dp.toPx())
            }
    }
    val elevation = MooTheme.dimens.shellElevation
    val stroke = when (colors.styleId) {
        "smartisan", "miui-v5" -> colors.borderControl
        "hero" -> colors.borderSoft
        else -> colors.border
    }
    return padding(4.dp)
        .shadow(elevation, ambientColor = colors.shadowSoft, spotColor = colors.shadowSoft)
        .background(colors.workspace)
        .border(1.dp, stroke)
}

@Composable
fun Modifier.mooToolbarBackground(): Modifier {
    val colors = MooTheme.colors
    return background(colors.toolbarBrush()).drawBehind {
        val y = size.height - 0.5f
        drawLine(colors.borderSoft, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
    }
}

@Composable
fun Modifier.mooToolTabsBackground(): Modifier {
    val colors = MooTheme.colors
    return background(colors.toolbarBrush()).drawBehind {
        val y = size.height - 0.5f
        drawLine(colors.borderSoft, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
    }
}

@Composable
fun Modifier.mooStatusBarBackground(): Modifier {
    val colors = MooTheme.colors
    return background(colors.toolbarBrush()).drawBehind {
        drawLine(colors.borderSoft, Offset(0f, 0.5f), Offset(size.width, 0.5f), 1.dp.toPx())
    }
}

@Composable
fun Modifier.mooFindBarBackground(json: Boolean = false): Modifier {
    val colors = MooTheme.colors
    return background(if (json) colors.workspace else colors.surfaceSubtle).drawBehind {
        drawLine(colors.borderSoft, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1.dp.toPx())
    }
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
