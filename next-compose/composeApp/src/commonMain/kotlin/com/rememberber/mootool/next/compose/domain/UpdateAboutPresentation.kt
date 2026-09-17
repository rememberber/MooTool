package com.rememberber.mootool.next.compose.domain

/** 设置 · 关于与更新页文案/操作显隐（对齐 Electron `SettingsWindow` about/update 区）。 */
object UpdateAboutPresentation {
    fun statusMessageKey(status: String, hasDownloadPack: Boolean): String {
        if (status == "checking") return "settings.update.checking"
        if (status == "downloading") return "settings.update.downloading"
        if (status == "ready") return "settings.update.ready"
        if (status == "cancelled") return "settings.update.cancelled"
        if (status == "available") {
            return if (hasDownloadPack) "settings.update.available" else "settings.update.noPackage"
        }
        if (status == "latest") return "settings.update.upToDate"
        if (status == "unpublished") return "settings.update.unpublished"
        if (status == "idle") return "settings.update.idle"
        return "settings.update.idle"
    }

    fun showDownloadAction(hasDownloadPack: Boolean, installerReady: Boolean): Boolean =
        hasDownloadPack && !installerReady

    fun showOpenInstallerAction(installerReady: Boolean): Boolean = installerReady

    fun showCancelDownloadAction(status: String): Boolean = status == "downloading"

    fun showReleaseNotes(releaseNotes: String?, latestVersion: String?, checkStatus: String?): Boolean {
        if (releaseNotes.isNullOrBlank()) return false
        if (latestVersion.isNullOrBlank()) return false
        return checkStatus?.lowercase() != "unpublished"
    }

    fun canCheckForUpdates(busy: Boolean): Boolean = !busy

    fun showDownloadProgress(status: String, hasProgress: Boolean): Boolean =
        hasProgress && (status == "downloading" || status == "ready")
}
