package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.storage.VaultPathConfig
import java.io.File

/** F10/F22/F24 外置导出/合并初始目录（对齐 `tools.exportDirectory` + Electron Desktop 回退）。 */
object ToolsExportWiringPresentation {
    fun defaultExportDirectory(exportDirectorySetting: String): File {
        val export = VaultPathConfig.effectiveCustomRoot(exportDirectorySetting)
        val desktop = File(System.getProperty("user.home"), "Desktop").takeIf { it.isDirectory }
            ?: File(System.getProperty("user.home"))
        return export.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.isDirectory } ?: desktop
    }

    fun defaultMergePdfPath(exportDirectorySetting: String): String =
        File(defaultExportDirectory(exportDirectorySetting), "merge.pdf").absolutePath

    fun defaultHostExportFileName(profileName: String): String {
        val stem = profileName.trim().ifBlank { "hosts" }
            .replace(Regex("[^a-zA-Z0-9._-]+"), "-")
            .trim('-', '.')
            .ifBlank { "hosts" }
        return "$stem.txt"
    }

    fun defaultSvgPath(exportDirectorySetting: String, assetBaseName: String): String {
        val base = assetBaseName.substringBeforeLast('.').ifBlank { "image" }
        return File(defaultExportDirectory(exportDirectorySetting), "$base.svg").absolutePath
    }
}
