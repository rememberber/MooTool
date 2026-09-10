package com.rememberber.mootool.next.compose.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.model.ThemePreference

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
    val selected: Color
)

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
    selected = Color(0xFFE8F0FB)
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
    selected = Color(0xFF2B3A4E)
)

val LocalMooColors = staticCompositionLocalOf { LightColors }
val LocalMooDimens = staticCompositionLocalOf { MooDimens() }
val LocalDarkTheme = staticCompositionLocalOf { false }

object MooTheme {
    val colors: MooColors @Composable get() = LocalMooColors.current
    val dimens: MooDimens @Composable get() = LocalMooDimens.current
    val dark: Boolean @Composable get() = LocalDarkTheme.current
    val uiText: TextStyle @Composable get() = TextStyle(
        color = colors.textPrimary,
        fontSize = 13.sp,
        fontFamily = FontFamily.SansSerif,
        lineHeight = 18.sp
    )
}

@Composable
fun MooTheme(
    preference: ThemePreference,
    systemDark: Boolean,
    content: @Composable () -> Unit
) {
    val dark = when (preference) {
        ThemePreference.Dark -> true
        ThemePreference.Light -> false
        ThemePreference.System -> systemDark
    }
    CompositionLocalProvider(
        LocalMooColors provides if (dark) DarkColors else LightColors,
        LocalMooDimens provides MooDimens(),
        LocalDarkTheme provides dark,
        content = content
    )
}
