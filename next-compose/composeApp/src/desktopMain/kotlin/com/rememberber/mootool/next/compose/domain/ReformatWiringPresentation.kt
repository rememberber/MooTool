package com.rememberber.mootool.next.compose.domain

/** F03 格式化：另存对话框默认名（对齐 `ReformatScreen.saveResult`）。 */
object ReformatWiringPresentation {
    fun canRunFormat(busy: Boolean, inputNotBlank: Boolean): Boolean = !busy && inputNotBlank

    fun defaultSaveFileName(sourceFileName: String, type: ReformatType): String {
        val extension =
            when (type) {
                ReformatType.Nginx -> "conf"
                ReformatType.Java -> "java"
                ReformatType.Xml -> "xml"
                ReformatType.Html -> "html"
            }
        val base = sourceFileName.replace(Regex("\\.[^.]+$"), "").ifEmpty { "formatted" }
        return "$base.$extension"
    }
}
