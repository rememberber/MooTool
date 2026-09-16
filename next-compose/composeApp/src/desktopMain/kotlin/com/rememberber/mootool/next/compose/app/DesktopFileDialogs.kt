package com.rememberber.mootool.next.compose.app

import java.io.File
import javax.swing.JFileChooser

object DesktopFileDialogs {
    fun chooseDirectory(title: String, initialPath: String = ""): String? {
        val chooser = JFileChooser()
        chooser.dialogTitle = title
        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        val initial = initialPath.trim()
        if (initial.isNotEmpty()) {
            val file = File(initial)
            when {
                file.isDirectory -> chooser.currentDirectory = file
                file.parentFile?.isDirectory == true -> chooser.currentDirectory = file.parentFile
            }
        }
        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile?.absolutePath
        } else {
            null
        }
    }

    fun chooseExecutable(title: String, initialPath: String = ""): String? {
        val chooser = JFileChooser()
        chooser.dialogTitle = title
        chooser.fileSelectionMode = JFileChooser.FILES_ONLY
        val initial = initialPath.trim()
        if (initial.isNotEmpty()) {
            val file = File(initial)
            when {
                file.isFile -> chooser.selectedFile = file
                file.isDirectory -> chooser.currentDirectory = file
                file.parentFile?.isDirectory == true -> chooser.currentDirectory = file.parentFile
            }
        }
        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile?.absolutePath
        } else {
            null
        }
    }
}
