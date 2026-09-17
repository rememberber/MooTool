package com.rememberber.mootool.next.compose.domain

/** F10 Host：应用/备份恢复 busy 守卫。 */
object HostWiringPresentation {
    fun canConfirmApply(applying: Boolean): Boolean = !applying

    fun canOpenApplyConfirm(content: String, applying: Boolean): Boolean =
        content.isNotBlank() && !applying

    fun canRestoreBackup(lastBackup: String, applying: Boolean): Boolean =
        lastBackup.isNotBlank() && !applying
}
