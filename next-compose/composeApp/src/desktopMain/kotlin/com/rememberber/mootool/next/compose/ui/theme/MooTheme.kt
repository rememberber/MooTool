package com.rememberber.mootool.next.compose.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.model.ThemePreference
import kotlin.math.max
import kotlin.math.min

@Immutable
data class MooColors(
    val workspace: Color,
    val sidebar: Color,
    val toolbar: Color,
    val surfaceSubtle: Color,
    val control: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentAction: Color,
    val onAccent: Color,
    val focusRing: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val selected: Color,
    val raisedTop: Color,
    val raisedBottom: Color,
    val toolbarTop: Color,
    val toolbarBottom: Color,
    val inset: Color,
    val highlight: Color,
    val lowlight: Color,
    val navActiveBar: Color,
    val styleId: String
) {
    fun toolbarBrush(): Brush =
        if (toolbarTop == toolbarBottom) SolidColor(toolbar) else Brush.verticalGradient(listOf(toolbarTop, toolbarBottom))

    fun controlBrush(primary: Boolean = false): Brush = when {
        primary -> SolidColor(accentAction)
        raisedTop == raisedBottom -> SolidColor(control)
        else -> Brush.verticalGradient(listOf(raisedTop, raisedBottom))
    }

    fun sidebarItemBrush(selected: Boolean, card: Boolean, hovered: Boolean = false): Brush = when {
        selected && raisedChrome -> Brush.verticalGradient(listOf(raisedTop, raisedBottom))
        selected -> SolidColor(this.selected)
        hovered && raisedChrome -> Brush.verticalGradient(listOf(raisedTop, raisedBottom))
        hovered || card -> SolidColor(control)
        else -> SolidColor(Color.Transparent)
    }

    val raisedChrome: Boolean
        get() = styleId == "smartisan" || styleId == "hero" || styleId == "miui-v5"

    fun overlayScrim(): Color =
        Color.Black.copy(alpha = if (workspace.luminance() < 0.5f) 0.46f else 0.20f)
}

@Immutable
data class MooDimens(
    val sidebarExpanded: Dp = 248.dp,
    val sidebarCollapsed: Dp = 84.dp,
    val sidebarMin: Dp = 208.dp,
    val sidebarMax: Dp = 300.dp,
    val toolbar: Dp = 46.dp,
    val statusBar: Dp = 26.dp,
    val controlHeight: Dp = 32.dp,
    val radiusSmall: Dp = 6.dp,
    val radius: Dp = 8.dp,
    val radiusLarge: Dp = 12.dp
)

private val LightColors = MooColors(
    workspace = Color(0xFFFFFFFF),
    sidebar = Color(0xFFF5F5F6),
    toolbar = Color(0xFFFAFAFA),
    surfaceSubtle = Color(0xFFF7F7F8),
    control = Color(0xFFF1F1F2),
    border = Color(0xFFE2E2E5),
    textPrimary = Color(0xFF202124),
    textSecondary = Color(0xFF64676F),
    accent = Color(0xFF4F83CC),
    accentAction = Color(0xFF3F6FAE),
    onAccent = Color.White,
    focusRing = Color(0xFF316DC0),
    success = Color(0xFF246448),
    warning = Color(0xFF82520A),
    danger = Color(0xFFAA302E),
    selected = Color(0xFFE8F0FB),
    raisedTop = Color(0xFFF7F7F8),
    raisedBottom = Color(0xFFF1F1F2),
    toolbarTop = Color(0xFFFAFAFA),
    toolbarBottom = Color(0xFFFAFAFA),
    inset = Color(0xFFFFFFFF),
    highlight = Color(0xE8FFFFFF),
    lowlight = Color(0x14000000),
    navActiveBar = Color(0xFF4F83CC),
    styleId = "modern"
)

private val DarkColors = MooColors(
    workspace = Color(0xFF1C1C1E),
    sidebar = Color(0xFF242426),
    toolbar = Color(0xFF222224),
    surfaceSubtle = Color(0xFF252527),
    control = Color(0xFF2C2C2F),
    border = Color(0xFF39393D),
    textPrimary = Color(0xFFEDEDEE),
    textSecondary = Color(0xFFB0B0B8),
    accent = Color(0xFF85B4F0),
    accentAction = Color(0xFFA9CCFA),
    onAccent = Color(0xFF102033),
    focusRing = Color(0xFF94BDF4),
    success = Color(0xFF88D5AF),
    warning = Color(0xFFF1C56D),
    danger = Color(0xFFFFAAA6),
    selected = Color(0xFF2B3A4E),
    raisedTop = Color(0xFF323236),
    raisedBottom = Color(0xFF2C2C2F),
    toolbarTop = Color(0xFF222224),
    toolbarBottom = Color(0xFF222224),
    inset = Color(0xFF1C1C1E),
    highlight = Color(0x17FFFFFF),
    lowlight = Color(0x66000000),
    navActiveBar = Color(0xFF85B4F0),
    styleId = "modern"
)

val LocalMooColors = staticCompositionLocalOf { LightColors }
val LocalMooDimens = staticCompositionLocalOf { MooDimens() }
val LocalDarkTheme = staticCompositionLocalOf { false }
val LocalUiFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.SansSerif }

object MooTheme {
    val colors: MooColors @Composable get() = LocalMooColors.current
    val dimens: MooDimens @Composable get() = LocalMooDimens.current
    val dark: Boolean @Composable get() = LocalDarkTheme.current
    val uiText: TextStyle @Composable get() = TextStyle(
        color = colors.textPrimary,
        fontSize = 13.sp,
        fontFamily = LocalUiFontFamily.current,
        lineHeight = 18.sp
    )
}

fun contrastRatio(foreground: Color, background: Color): Float {
    val lighter = max(foreground.luminance(), background.luminance()) + 0.05f
    val darker = min(foreground.luminance(), background.luminance()) + 0.05f
    return lighter / darker
}

fun resolveMooColors(
    dark: Boolean,
    interfaceStyle: String,
    accentColor: String,
    unifiedBackground: Boolean = true
): MooColors = resolveColors(dark, interfaceStyle, accentColor, unifiedBackground)

fun resolveMooDimens(interfaceStyle: String): MooDimens = resolveDimens(interfaceStyle)

fun resolveUiFontFamily(name: String): FontFamily = when (name.trim().lowercase()) {
    "serif" -> FontFamily.Serif
    "mono", "monospace", "ui-monospace" -> FontFamily.Monospace
    else -> FontFamily.SansSerif
}

@Composable
fun MooTheme(
    preference: ThemePreference,
    systemDark: Boolean,
    interfaceStyle: String = "modern",
    accentColor: String = "blue",
    unifiedBackground: Boolean = true,
    fontFamily: String = "system",
    content: @Composable () -> Unit
) {
    val dark = when (preference) {
        ThemePreference.Dark -> true
        ThemePreference.Light -> false
        ThemePreference.System -> systemDark
    }
    CompositionLocalProvider(
        LocalMooColors provides resolveColors(dark, interfaceStyle, accentColor, unifiedBackground),
        LocalMooDimens provides resolveDimens(interfaceStyle),
        LocalDarkTheme provides dark,
        LocalUiFontFamily provides resolveUiFontFamily(fontFamily),
        content = content
    )
}

private data class AccentPalette(
    val accent: Color,
    val accentAction: Color,
    val onAccent: Color,
    val focusRing: Color,
    val selected: Color
)

private fun resolveDimens(style: String): MooDimens = when (style) {
    "hero", "claude" -> MooDimens(radiusSmall = 10.dp, radius = 12.dp, radiusLarge = 16.dp, controlHeight = 34.dp)
    "smartisan", "miui-v5" -> MooDimens(radiusSmall = 8.dp, radius = 12.dp, radiusLarge = 18.dp, controlHeight = 34.dp)
    "quiet" -> MooDimens(radiusSmall = 4.dp, radius = 6.dp, radiusLarge = 10.dp, controlHeight = 30.dp)
    else -> MooDimens()
}

private fun resolveColors(
    dark: Boolean,
    style: String,
    accentName: String,
    unifiedBackground: Boolean = true
): MooColors {
    val base = if (dark) DarkColors else LightColors
    val accent = accentPalette(dark, accentName)
    val styled = when (style) {
        "quiet" -> if (dark) base.copy(
            sidebar = Color(0xFF1F1F20),
            toolbar = Color(0xFF1E1E1F),
            surfaceSubtle = Color(0xFF202021),
            toolbarTop = Color(0xFF1E1E1F),
            toolbarBottom = Color(0xFF1E1E1F),
            raisedTop = Color(0xFF262628),
            raisedBottom = Color(0xFF222224),
            styleId = "quiet"
        ) else base.copy(
            sidebar = Color(0xFFF2F2F3),
            toolbar = Color(0xFFF7F7F7),
            surfaceSubtle = Color(0xFFF4F4F5),
            toolbarTop = Color(0xFFF7F7F7),
            toolbarBottom = Color(0xFFF7F7F7),
            styleId = "quiet"
        )
        "hero" -> if (dark) base.copy(
            workspace = Color(0xFF09090B),
            sidebar = Color(0xFF111113),
            toolbar = Color(0xFF18181B),
            surfaceSubtle = Color(0xFF202023),
            control = Color(0xFF27272A),
            border = Color(0xFF3F3F46),
            textPrimary = Color(0xFFF4F4F5),
            textSecondary = Color(0xFFA1A1AA),
            toolbarTop = Color(0xFF18181B),
            toolbarBottom = Color(0xFF111113),
            raisedTop = Color(0xFF27272A),
            raisedBottom = Color(0xFF18181B),
            inset = Color(0xFF09090B),
            navActiveBar = Color(0xFF7AA2E3),
            styleId = "hero"
        ) else base.copy(
            workspace = Color(0xFFF4F4F5),
            sidebar = Color(0xFFFAFAFA),
            toolbar = Color(0xFFFAFAFA),
            surfaceSubtle = Color(0xFFFAFAFA),
            control = Color(0xFFF4F4F5),
            border = Color(0xFFE4E4E7),
            textPrimary = Color(0xFF18181B),
            textSecondary = Color(0xFF3F3F46),
            toolbarTop = Color(0xFFFFFFFF),
            toolbarBottom = Color(0xFFFAFAFA),
            raisedTop = Color(0xFFFFFFFF),
            raisedBottom = Color(0xFFE4E4E7),
            inset = Color(0xFFE4E4E7),
            styleId = "hero"
        )
        "smartisan" -> if (dark) base.copy(
            workspace = Color(0xFF1A1917),
            sidebar = Color(0xFF22211F),
            toolbar = Color(0xFF2B2A28),
            surfaceSubtle = Color(0xFF302F2C),
            control = Color(0xFF343330),
            border = Color(0xFF484641),
            textPrimary = Color(0xFFF2F0EB),
            textSecondary = Color(0xFFD0CCC5),
            toolbarTop = Color(0xFF343330),
            toolbarBottom = Color(0xFF22211F),
            raisedTop = Color(0xFF403E3A),
            raisedBottom = Color(0xFF292826),
            inset = Color(0xFF1A1917),
            highlight = Color(0x17FFFFFF),
            lowlight = Color(0x8C000000),
            navActiveBar = Color(0xFFB75953),
            styleId = "smartisan"
        ) else base.copy(
            workspace = Color(0xFFDEDBD5),
            sidebar = Color(0xFFE9E7E2),
            toolbar = Color(0xFFE9E6E0),
            surfaceSubtle = Color(0xFFEBE8E2),
            control = Color(0xFFE5E1DA),
            border = Color(0xFFC4BFB6),
            textPrimary = Color(0xFF292622),
            textSecondary = Color(0xFF4F4A44),
            toolbarTop = Color(0xFFF8F6F2),
            toolbarBottom = Color(0xFFE2DED7),
            raisedTop = Color(0xFFFEFEFA),
            raisedBottom = Color(0xFFDDD9D2),
            inset = Color(0xFFD8D4CD),
            highlight = Color(0xEBFFFFFF),
            lowlight = Color(0x33453E37),
            navActiveBar = Color(0xFF9F403C),
            styleId = "smartisan"
        )
        "miui-v5" -> if (dark) base.copy(
            workspace = Color(0xFF191919),
            sidebar = Color(0xFF202020),
            toolbar = Color(0xFF2D2D2D),
            surfaceSubtle = Color(0xFF303030),
            control = Color(0xFF373737),
            border = Color(0xFF464646),
            textPrimary = Color(0xFFE9E9E9),
            textSecondary = Color(0xFFD4D4D4),
            toolbarTop = Color(0xFF373737),
            toolbarBottom = Color(0xFF202020),
            raisedTop = Color(0xFF424242),
            raisedBottom = Color(0xFF282828),
            inset = Color(0xFF191919),
            navActiveBar = Color(0xFFF36C21),
            styleId = "miui-v5"
        ) else base.copy(
            workspace = Color(0xFFE9E9E9),
            sidebar = Color(0xFFF2F2F2),
            toolbar = Color(0xFFF3F3F3),
            surfaceSubtle = Color(0xFFF5F5F5),
            control = Color(0xFFECECEC),
            border = Color(0xFFCECECE),
            textPrimary = Color(0xFF202020),
            textSecondary = Color(0xFF494949),
            toolbarTop = Color(0xFFFAFAFA),
            toolbarBottom = Color(0xFFEDEDED),
            raisedTop = Color(0xFFFFFFFF),
            raisedBottom = Color(0xFFE8E8E8),
            inset = Color(0xFFDDDDDD),
            navActiveBar = Color(0xFFF36C21),
            styleId = "miui-v5"
        )
        "claude" -> if (dark) base.copy(
            workspace = Color(0xFF201E1B),
            sidebar = Color(0xFF25221E),
            toolbar = Color(0xFF2E2B27),
            surfaceSubtle = Color(0xFF302D28),
            control = Color(0xFF37332E),
            border = Color(0xFF474139),
            textPrimary = Color(0xFFEEEAE3),
            textSecondary = Color(0xFFDED8CF),
            toolbarTop = Color(0xFF37332E),
            toolbarBottom = Color(0xFF25221E),
            raisedTop = Color(0xFF3E3933),
            raisedBottom = Color(0xFF2B2824),
            inset = Color(0xFF201E1B),
            navActiveBar = Color(0xFFE78A6C),
            styleId = "claude"
        ) else base.copy(
            workspace = Color(0xFFF7F5EF),
            sidebar = Color(0xFFEEECE5),
            toolbar = Color(0xFFFAF8F2),
            surfaceSubtle = Color(0xFFF3F0E9),
            control = Color(0xFFEEEBE4),
            border = Color(0xFFDED9D0),
            textPrimary = Color(0xFF25211F),
            textSecondary = Color(0xFF48423D),
            toolbarTop = Color(0xFFFFFEFA),
            toolbarBottom = Color(0xFFFAF8F2),
            raisedTop = Color(0xFFFFFEFA),
            raisedBottom = Color(0xFFE8E4DC),
            inset = Color(0xFFE8E4DC),
            navActiveBar = Color(0xFFD97757),
            styleId = "claude"
        )
        else -> base.copy(styleId = "modern")
    }
    val withAccent = styled.copy(
        accent = accent.accent,
        accentAction = accent.accentAction,
        onAccent = accent.onAccent,
        focusRing = accent.focusRing,
        selected = accent.selected
    )
    val themed = if (style == "smartisan" || style == "miui-v5" || style == "claude") {
        withAccent.copy(navActiveBar = styled.navActiveBar, selected = if (style == "smartisan") styled.selected else accent.selected)
    } else withAccent.copy(navActiveBar = accent.accent)
    if (unifiedBackground) {
        return if (style == "hero" || style == "smartisan" || style == "claude") {
            themed.copy(workspace = themed.inset)
        } else themed
    }
    val distinctWorkspace = when {
        dark && (style == "modern" || style == "quiet") -> Color(0xFF191A1D)
        !dark && (style == "modern" || style == "quiet") -> Color(0xFFFBFBFB)
        dark && style == "miui-v5" -> Color(0xFF191919)
        !dark && style == "miui-v5" -> Color(0xFFE9E9E9)
        else -> themed.workspace
    }
    return themed.copy(workspace = distinctWorkspace)
}

object AccentPresets {
    val ids = listOf("yellow", "coral", "blue", "green", "red", "purple")

    fun normalize(id: String): String = when (id) {
        "orange" -> "yellow"
        "teal" -> "green"
        else -> if (id in ids) id else "blue"
    }

    fun swatch(id: String): Color = when (normalize(id)) {
        "yellow" -> Color(0xFFE0B22B)
        "coral" -> Color(0xFFDE8F7D)
        "green" -> Color(0xFF4E9275)
        "red" -> Color(0xFFC96761)
        "purple" -> Color(0xFF8A72B5)
        else -> Color(0xFF4F83CC)
    }
}

private fun accentPalette(dark: Boolean, name: String): AccentPalette = when (AccentPresets.normalize(name)) {
    "yellow" -> if (dark) AccentPalette(Color(0xFFE8C56A), Color(0xFFE0B22B), Color(0xFF241C00), Color(0xFFE8C56A), Color(0xFF3F3420))
    else AccentPalette(Color(0xFFE0B22B), Color(0xFFB78300), Color(0xFF241C00), Color(0xFFB78300), Color(0xFFF8EDD2))
    "coral" -> if (dark) AccentPalette(Color(0xFFE8A89C), Color(0xFFDE8F7D), Color.White, Color(0xFFE8A89C), Color(0xFF3F2C28))
    else AccentPalette(Color(0xFFDE8F7D), Color(0xFFCD6F5E), Color.White, Color(0xFFCD6F5E), Color(0xFFF8E8E4))
    "green" -> if (dark) AccentPalette(Color(0xFF8FCB9B), Color(0xFFB7E0C0), Color(0xFF102016), Color(0xFFA6D8B2), Color(0xFF2A3F32))
    else AccentPalette(Color(0xFF4E9275), Color(0xFF34755B), Color.White, Color(0xFF34755B), Color(0xFFE4F3E8))
    "purple" -> if (dark) AccentPalette(Color(0xFFC2A6F0), Color(0xFFD4C0F6), Color(0xFF1C1230), Color(0xFFCDB4F4), Color(0xFF35284A))
    else AccentPalette(Color(0xFF8A72B5), Color(0xFF6F579C), Color.White, Color(0xFF6F579C), Color(0xFFECE6FA))
    "red" -> if (dark) AccentPalette(Color(0xFFFFA8A4), Color(0xFFFFC0BC), Color(0xFF2A1010), Color(0xFFFFB4B0), Color(0xFF3F2626))
    else AccentPalette(Color(0xFFC96761), Color(0xFFA84E49), Color.White, Color(0xFFA84E49), Color(0xFFF8E2E1))
    else -> if (dark) AccentPalette(Color(0xFF85B4F0), Color(0xFFA9CCFA), Color(0xFF102033), Color(0xFF94BDF4), Color(0xFF2B3A4E))
    else AccentPalette(Color(0xFF4F83CC), Color(0xFF3F6FAE), Color.White, Color(0xFF316DC0), Color(0xFFE8F0FB))
}
