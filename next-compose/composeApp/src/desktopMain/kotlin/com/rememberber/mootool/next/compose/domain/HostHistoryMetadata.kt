package com.rememberber.mootool.next.compose.domain

/** F10 Host 历史：`operation`=apply，`options`=备份路径，`output`=系统 hosts 路径。 */
object HostHistoryMetadata {
    const val OPERATION_APPLY = "apply"

    fun encodeApplyBackup(backupPath: String): String = backupPath.trim()

    fun decodeBackup(options: String): String = options.trim()
}
