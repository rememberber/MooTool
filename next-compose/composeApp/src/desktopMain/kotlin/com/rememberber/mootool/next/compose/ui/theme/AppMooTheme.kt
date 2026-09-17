package com.rememberber.mootool.next.compose.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.model.ThemePreference

/** 主窗与分离工具窗共用的 `MooTheme` 入参，避免拆窗漏传 `compactNavigation` 等设置。 */
data class AppMooThemeInputs(
    val preference: ThemePreference,
    val systemDark: Boolean,
    val interfaceStyle: String,
    val accentColor: String,
    val unifiedBackground: Boolean,
    val fontFamily: String,
    val compactNavigation: Boolean,
) {
    companion object {
        fun from(
            preference: ThemePreference,
            systemDark: Boolean,
            settings: AppSettings,
        ): AppMooThemeInputs = AppMooThemeInputs(
            preference = preference,
            systemDark = systemDark,
            interfaceStyle = settings.appearance.interfaceStyle,
            accentColor = settings.appearance.accentColor,
            unifiedBackground = settings.appearance.unifiedBackground,
            fontFamily = settings.appearance.fontFamily,
            compactNavigation = settings.layout.compactNavigation,
        )
    }
}

@Composable
fun AppMooTheme(inputs: AppMooThemeInputs, content: @Composable () -> Unit) {
    MooTheme(
        preference = inputs.preference,
        systemDark = inputs.systemDark,
        interfaceStyle = inputs.interfaceStyle,
        accentColor = inputs.accentColor,
        unifiedBackground = inputs.unifiedBackground,
        fontFamily = inputs.fontFamily,
        compactNavigation = inputs.compactNavigation,
        content = content,
    )
}

@Composable
fun AppMooTheme(container: AppContainer, systemDark: Boolean, content: @Composable () -> Unit) {
    val settings by container.settings.collectAsState()
    AppMooTheme(
        AppMooThemeInputs.from(container.themePreference(), systemDark, settings),
        content = content,
    )
}
