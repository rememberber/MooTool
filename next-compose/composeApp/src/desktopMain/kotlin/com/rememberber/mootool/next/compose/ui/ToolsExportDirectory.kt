package com.rememberber.mootool.next.compose.ui

import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.storage.VaultPathConfig
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

fun persistToolsExportDirectory(container: AppContainer, file: File) {
    val parent = when {
        file.isDirectory -> file.absolutePath.takeIf { it.isNotBlank() }
        else -> file.parentFile?.absolutePath?.takeIf { it.isNotBlank() }
    } ?: return
    if (!java.nio.file.Path.of(parent).isAbsolute) return
    if (container.settings.value.tools.exportDirectory == parent) return
    container.updateSettings { current -> current.copy(tools = current.tools.copy(exportDirectory = parent)) }
}

fun chooseFileWithExportDirectory(
    container: AppContainer,
    save: Boolean,
    title: String,
    defaultFileName: String = "",
): File? {
    val exportDir = VaultPathConfig.effectiveCustomRoot(container.settings.value.tools.exportDirectory)
    val dialog = FileDialog(null as Frame?, title, if (save) FileDialog.SAVE else FileDialog.LOAD)
    if (defaultFileName.isNotBlank()) {
        dialog.file = defaultFileName
    }
    if (exportDir.isNotBlank()) {
        val dir = File(exportDir)
        if (dir.isDirectory) {
            dialog.directory = exportDir
        } else {
            dir.parentFile?.takeIf { it.isDirectory }?.let { dialog.directory = it.absolutePath }
        }
    }
    dialog.isVisible = true
    val directory = dialog.directory ?: return null
    val name = dialog.file ?: return null
    return File(directory, name)
}

fun chooseFilesWithExportDirectory(
    container: AppContainer,
    title: String,
    fileFilter: String = "",
): List<File> {
    val exportDir = VaultPathConfig.effectiveCustomRoot(container.settings.value.tools.exportDirectory)
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.isMultipleMode = true
    if (fileFilter.isNotBlank()) {
        dialog.file = fileFilter
    }
    if (exportDir.isNotBlank()) {
        val dir = File(exportDir)
        if (dir.isDirectory) {
            dialog.directory = exportDir
        } else {
            dir.parentFile?.takeIf { it.isDirectory }?.let { dialog.directory = it.absolutePath }
        }
    }
    dialog.isVisible = true
    val multi = dialog.files?.toList().orEmpty()
    if (multi.isNotEmpty()) return multi
    val directory = dialog.directory ?: return emptyList()
    val name = dialog.file ?: return emptyList()
    return listOf(File(directory, name))
}
