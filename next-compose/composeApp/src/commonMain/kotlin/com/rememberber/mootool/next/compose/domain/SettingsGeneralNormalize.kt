package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.AppLanguage
import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.model.CloseBehavior

/** Aligns with Electron `normalizeSettings` general slice in `next/src/shared/contracts/settings.ts`. */
object SettingsGeneralNormalize {
    fun normalizeLanguage(value: String, fallback: String = AppLanguage.ZhCN.code): String =
        AppLanguage.entries.firstOrNull { it.code.equals(value.trim(), ignoreCase = true) }?.code ?: fallback

    fun normalizeCloseBehavior(value: String, fallback: String = CloseBehavior.Ask.name.lowercase()): String =
        CloseBehavior.entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }?.name?.lowercase()
            ?: fallback

    fun apply(settings: AppSettings, defaults: AppSettings = AppSettings.Default): AppSettings =
        settings.copy(
            general = settings.general.copy(
                language = normalizeLanguage(settings.general.language, defaults.general.language),
                closeBehavior = normalizeCloseBehavior(settings.general.closeBehavior, defaults.general.closeBehavior),
            ),
        )
}
