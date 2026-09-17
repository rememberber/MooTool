package com.rememberber.mootool.next.compose.domain

/**
 * 对齐 Electron `formatCodeEditorContent` 中非 JVM/Prettier 插件路径（Python、plain trim、JS/TS 表面间距）。
 * 完整 Prettier 语义见 parity-gap；此处不引入 Node。
 */
object CodeEditorSurfaceFormatEngine {
    fun trimTrailingWhitespace(content: String): String =
        content.split(Regex("\\r?\\n")).joinToString("\n") { it.trimEnd() }.trimEnd()

    fun formatPython(content: String, tabWidth: Int): String {
        val indent = " ".repeat(tabWidth.coerceIn(1, 8))
        return content
            .replace("\t", indent)
            .split(Regex("\\r?\\n"))
            .joinToString("\n") { it.trimEnd() }
            .trimEnd()
    }

    fun formatJavascript(content: String): String = formatScriptLike(content)

    fun formatTypescript(content: String): String = formatScriptLike(content)

    private fun formatScriptLike(content: String): String {
        if (content.isBlank()) return ""
        val lines = content.replace("\r\n", "\n").split("\n")
        return lines.joinToString("\n") { formatScriptLine(it) }.trimEnd()
    }

    private fun formatScriptLine(line: String): String {
        if (line.isBlank()) return line
        var text = line.trimEnd()
        text = text
            .replace(Regex("(?<![=!<>])=(?!=)"), " = ")
            .replace("{", " { ")
            .replace("}", " } ")
            .replace(",", ", ")
            .replace(Regex("(?<!:):(?!:)"), ": ")
        text = text.replace(Regex(" +"), " ").trim()
        if (!text.endsWith(";")) {
            text += ";"
        }
        return text
    }
}
