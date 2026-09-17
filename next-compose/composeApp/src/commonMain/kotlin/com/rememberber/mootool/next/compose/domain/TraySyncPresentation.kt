package com.rememberber.mootool.next.compose.domain

/** 托盘菜单重建触发条件（对齐 Electron `updateTray(settings)` + Host 列表变更）。 */
object TraySyncPresentation {
    fun shouldInstallTray(trayEnabled: Boolean, traySupported: Boolean): Boolean =
        trayEnabled && traySupported

    fun menuRevision(
        trayEnabled: Boolean,
        language: String,
        hostProfileMenuRevision: Int,
        generalRevision: Long,
        autoCheckUpdates: Boolean = false,
        autoDownloadUpdates: Boolean = false,
    ): Int = listOf(
        trayEnabled,
        language,
        hostProfileMenuRevision,
        generalRevision,
        autoCheckUpdates,
        autoDownloadUpdates,
    ).hashCode()
}
