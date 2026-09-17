package com.rememberber.mootool.next.compose.domain

/** A03 更新包下载完成后的「打开安装包」应用路径（对齐 Electron `shell.openPath`）。 */
object UpdateInstallApplyPresentation {
    fun canOpenInstaller(busy: Boolean, installerReady: Boolean): Boolean =
        UpdateAboutPresentation.showOpenInstallerAction(installerReady) && !busy

    fun shouldRecordOpenFailure(previousError: String, failureMessage: String?): Boolean =
        previousError.isBlank() && !failureMessage.isNullOrBlank()
}
