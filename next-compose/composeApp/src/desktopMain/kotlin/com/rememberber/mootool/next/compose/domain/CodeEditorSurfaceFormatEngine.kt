package com.rememberber.mootool.next.compose.domain

/**
 * 对齐 Electron `formatCodeEditorContent` 中非 JVM/Prettier 插件路径（Python、plain trim、JS/TS 增量表面格式化、Markdown 增量）。
 * JS/TS/Markdown 完整 Prettier AST 见 parity-gap；此处不引入 Node。
 */
object CodeEditorSurfaceFormatEngine {
    private const val ARROW_PLACEHOLDER = "\uE000ARROW\uE001"
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

    private enum class ScriptChunkKind { Code, Literal }

    private data class ScriptChunk(val kind: ScriptChunkKind, val text: String)

    private fun formatScriptLine(line: String): String {
        if (line.isBlank()) return line
        val indent = line.takeWhile { it == ' ' || it == '\t' }
        val body = line.substring(indent.length)
        if (body.trimStart().startsWith("//")) return indent + body.trimEnd()
        val trimmedStart = body.trimStart()
        if (trimmedStart.startsWith("import") || trimmedStart.startsWith("export")) {
            return indent + formatImportExportLine(body.trimEnd())
        }
        val formattedBody =
            splitScriptLinePreservingLiterals(body.trimEnd()).joinToString("") { chunk ->
                if (chunk.kind == ScriptChunkKind.Literal) chunk.text else formatScriptCodeChunk(chunk.text)
            }
        return indent + finalizeScriptLine(formattedBody)
    }

    private fun finalizeScriptLine(line: String): String {
        val trimmedEnd = line.trimEnd()
        if (trimmedEnd.endsWith(";")) return trimmedEnd
        if (isScriptStructuralLine(trimmedEnd.trim())) return trimmedEnd
        if (shouldAppendStatementSemicolon(trimmedEnd.trim())) return "$trimmedEnd;"
        return trimmedEnd
    }

    private fun splitScriptLinePreservingLiterals(line: String): List<ScriptChunk> {
        val chunks = mutableListOf<ScriptChunk>()
        val code = StringBuilder()
        var i = 0
        fun flushCode() {
            if (code.isNotEmpty()) {
                chunks.add(ScriptChunk(ScriptChunkKind.Code, code.toString()))
                code.clear()
            }
        }
        while (i < line.length) {
            when (line[i]) {
                '\'', '"', '`' -> {
                    flushCode()
                    val (literal, next) = readScriptStringLiteral(line, i)
                    chunks.add(ScriptChunk(ScriptChunkKind.Literal, literal))
                    i = next
                }
                '/' -> {
                    if (i + 1 < line.length && line[i + 1] == '/') {
                        flushCode()
                        chunks.add(ScriptChunk(ScriptChunkKind.Literal, line.substring(i)))
                        return chunks
                    }
                    if (i + 1 < line.length && line[i + 1] == '*') {
                        flushCode()
                        val end = line.indexOf("*/", startIndex = i + 2)
                        if (end >= 0) {
                            chunks.add(ScriptChunk(ScriptChunkKind.Literal, line.substring(i, end + 2)))
                            i = end + 2
                        } else {
                            chunks.add(ScriptChunk(ScriptChunkKind.Literal, line.substring(i)))
                            return chunks
                        }
                    } else {
                        code.append(line[i])
                        i++
                    }
                }
                else -> {
                    code.append(line[i])
                    i++
                }
            }
        }
        flushCode()
        return chunks
    }

    private fun readScriptStringLiteral(source: String, start: Int): Pair<String, Int> {
        val quote = source[start]
        val out = StringBuilder()
        out.append(quote)
        var i = start + 1
        while (i < source.length) {
            val c = source[i]
            out.append(c)
            if (c == '\\' && i + 1 < source.length) {
                out.append(source[i + 1])
                i += 2
                continue
            }
            if (quote == '`' && c == '$' && i + 1 < source.length && source[i + 1] == '{') {
                out.append(source[i + 1])
                i += 2
                var depth = 1
                while (i < source.length && depth > 0) {
                    val inner = source[i]
                    out.append(inner)
                    when (inner) {
                        '{' -> depth++
                        '}' -> depth--
                    }
                    i++
                }
                continue
            }
            if (c == quote) return out.toString() to (i + 1)
            i++
        }
        return out.toString() to source.length
    }

    private fun formatScriptCodeChunk(code: String): String {
        if (code.isBlank()) return code
        var text = code.trimEnd()
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return text
        if (isScriptStructuralLine(trimmed)) return trimmed
        if (trimmed.startsWith("import ") || trimmed.startsWith("export ")) {
            text = formatImportExportLine(trimmed)
        } else {
            text = trimmed
                .replace("=>", ARROW_PLACEHOLDER)
                .replace(Regex("(?<![=!<>])=(?!=)"), " = ")
                .replace(Regex("(?<![+\\-*/%&|^])\\+(?![+=])"), " + ")
                .replace("{", " { ")
                .replace("}", " } ")
                .replace(",", ", ")
                .replace(Regex("(?<!:):(?!:)"), ": ")
                .replace(ARROW_PLACEHOLDER, " => ")
            text = text.replace(Regex(" +"), " ").trimStart()
            if (text.endsWith("=")) text += " "
        }
        return text
    }

    private fun formatImportExportLine(line: String): String {
        var text = line.trim()
        text = text.replace(Regex("^(import|export)\\b\\s*"), "$1 ")
        text = text.replace(Regex("\\s*\\{\\s*"), " { ")
        text = text.replace(Regex("\\}\\s*(?=from\\b)"), " } ")
        text = text.replace(Regex("\\s+from\\s+"), " from ")
        text = text.replace(Regex(" +"), " ").trim()
        if (!text.endsWith(";")) text += ";"
        return text
    }

    private fun isScriptStructuralLine(trimmed: String): Boolean =
        trimmed == "{" ||
            trimmed == "}" ||
            trimmed == "};" ||
            trimmed == "}," ||
            trimmed == "})" ||
            trimmed == "});" ||
            trimmed == "(" ||
            trimmed == ")" ||
            trimmed == "[" ||
            trimmed == "]"

    private fun shouldAppendStatementSemicolon(text: String): Boolean {
        if (text.endsWith(";")) return false
        if (text.endsWith("=")) return false
        if (text.endsWith("{") || text.endsWith("[") || text.endsWith("(")) return false
        if (text.endsWith(",") || text.endsWith(":") || text.endsWith("=>")) return false
        val trimmed = text.trimStart()
        if (
            trimmed.startsWith("interface ") ||
                trimmed.startsWith("class ") ||
                trimmed.startsWith("enum ") ||
                trimmed.startsWith("namespace ") ||
                trimmed.startsWith("declare ")
        ) {
            return false
        }
        if (trimmed.startsWith("type ") && trimmed.contains("=")) return true
        if (trimmed.startsWith("function ") || trimmed.startsWith("async function ")) return false
        if (Regex("^export\\s+(default\\s+)?function\\b").containsMatchIn(trimmed)) return false
        return true
    }
}
