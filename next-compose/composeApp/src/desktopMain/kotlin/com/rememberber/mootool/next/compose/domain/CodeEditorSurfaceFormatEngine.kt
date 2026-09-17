package com.rememberber.mootool.next.compose.domain

/**
 * 对齐 Electron `formatCodeEditorContent` 中非 JVM/Prettier 插件路径（Python、plain trim、JS/TS 表面间距、Markdown 增量）。
 * Markdown/JS 完整 Prettier AST 见 parity-gap；此处不引入 Node。
 */
object CodeEditorSurfaceFormatEngine {
    fun trimTrailingWhitespace(content: String): String =
        content.split(Regex("\\r?\\n")).joinToString("\n") { it.trimEnd() }.trimEnd()

    /**
     * 对齐 Prettier `markdown` 插件的常见样本：行尾 trim、列表/引用间距、段落内空白折叠、标题后空行、围栏代码块原样保留。
     */
    fun formatMarkdown(content: String, @Suppress("UNUSED_PARAMETER") tabWidth: Int = 2): String {
        if (content.isBlank()) return ""
        val rawLines = content.replace("\r\n", "\n").split("\n")
        val formatted = mutableListOf<String>()
        var inFence = false
        var fenceMarker: String? = null
        var inFrontMatter = false
        var frontMatterSeenOpen = false

        for (line in rawLines) {
            val trimmedEnd = line.trimEnd()
            when {
                inFence -> {
                    formatted.add(trimmedEnd)
                    if (trimmedEnd.startsWith(fenceMarker!!)) {
                        inFence = false
                        fenceMarker = null
                    }
                    continue
                }
                inFrontMatter -> {
                    formatted.add(trimmedEnd)
                    if (trimmedEnd == "---" && frontMatterSeenOpen) {
                        inFrontMatter = false
                    }
                    continue
                }
                trimmedEnd == "---" && formatted.isEmpty() -> {
                    inFrontMatter = true
                    frontMatterSeenOpen = true
                    formatted.add(trimmedEnd)
                }
                isFenceOpen(trimmedEnd) -> {
                    inFence = true
                    fenceMarker = trimmedEnd.takeWhile { it == '`' || it == '~' }.ifEmpty { "```" }
                    formatted.add(formatMarkdownLine(trimmedEnd))
                }
                else -> formatted.add(formatMarkdownLine(trimmedEnd))
            }
        }

        val withHeadingSpacing = insertBlankLineAfterHeadings(formatted)
        return withHeadingSpacing.joinToString("\n").trimEnd() + "\n"
    }

    private fun isFenceOpen(line: String): Boolean {
        val fence = line.trim()
        return (fence.startsWith("```") && fence.all { it == '`' || it.isWhitespace() }) ||
            (fence.startsWith("~~~") && fence.all { it == '~' || it.isWhitespace() })
    }

    private fun formatMarkdownLine(line: String): String {
        if (line.isBlank()) return line
        HEADING.matchEntire(line)?.let { m ->
            return "${m.groupValues[1]} ${collapseInlineSpaces(m.groupValues[2])}"
        }
        UNORDERED_LIST.matchEntire(line)?.let { m ->
            return "${m.groupValues[1]}${m.groupValues[2]} ${collapseInlineSpaces(m.groupValues[3])}"
        }
        ORDERED_LIST.matchEntire(line)?.let { m ->
            return "${m.groupValues[1]}${m.groupValues[2]} ${collapseInlineSpaces(m.groupValues[3])}"
        }
        BLOCKQUOTE.matchEntire(line)?.let { m ->
            val markers = m.groupValues[2].replace(Regex("\\s"), "")
            return "${m.groupValues[1]}$markers ${collapseInlineSpaces(m.groupValues[3])}"
        }
        if (HORIZONTAL_RULE.matches(line)) return line.trim()
        return collapseInlineSpaces(line.trim())
    }

    private fun collapseInlineSpaces(text: String): String = text.replace(Regex(" +"), " ").trim()

    private fun insertBlankLineAfterHeadings(lines: List<String>): List<String> {
        if (lines.isEmpty()) return lines
        val out = mutableListOf<String>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            out.add(line)
            if (HEADING.matches(line) && i + 1 < lines.size) {
                val next = lines[i + 1]
                if (next.isNotBlank() && !startsMarkdownBlock(next) && !isFenceOpen(next)) {
                    out.add("")
                }
            }
            i++
        }
        return out
    }

    private fun startsMarkdownBlock(line: String): Boolean =
        line.isBlank() ||
            HEADING.matches(line) ||
            UNORDERED_LIST.matches(line) ||
            ORDERED_LIST.matches(line) ||
            BLOCKQUOTE.matches(line) ||
            HORIZONTAL_RULE.matches(line.trim()) ||
            isFenceOpen(line)

    private val HEADING = Regex("^(#{1,6})\\s+(.*)$")
    private val UNORDERED_LIST = Regex("^(\\s*)([-*+])\\s+(.*)$")
    private val ORDERED_LIST = Regex("^(\\s*)(\\d+\\.)\\s+(.*)$")
    private val BLOCKQUOTE = Regex("^(\\s*)((?:>\\s*)+)(.*)$")
    private val HORIZONTAL_RULE = Regex("^-{3,}\\s*$")

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
