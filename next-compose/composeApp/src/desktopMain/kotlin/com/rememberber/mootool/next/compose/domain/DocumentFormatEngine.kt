package com.rememberber.mootool.next.compose.domain

object DocumentFormatEngine {
    private val jsonTranslator = JsonTranslator { key, _ -> key }

    fun format(content: String, syntax: String, sqlDialect: String, indent: Int = 2): String {
        if (content.isBlank()) return ""
        val tab = indent.coerceIn(1, 8)
        return when (normalize(syntax)) {
            "application/json", "json" -> JsonEngine.format(content, jsonTranslator, tab)
            "text/java", "java" -> ReformatEngine.format(content, ReformatType.Java, tab)
            "text/xml", "xml" -> ReformatEngine.format(content, ReformatType.Xml, tab)
            "text/html", "html" -> ReformatEngine.format(content, ReformatType.Html, tab)
            "text/yaml", "yaml", "yml" -> ConfigEngine.formatYaml(content)
            "text/sql", "sql" -> SqlFormatEngine.format(content, sqlDialect, tab)
            "text/python", "python", "py" -> CodeEditorSurfaceFormatEngine.formatPython(content, tab)
            "application/javascript", "text/javascript", "js", "javascript" ->
                CodeEditorSurfaceFormatEngine.formatJavascript(content)
            "text/typescript", "typescript", "ts" ->
                CodeEditorSurfaceFormatEngine.formatTypescript(content)
            "text/markdown", "markdown", "md" ->
                CodeEditorSurfaceFormatEngine.trimTrailingWhitespace(content)
            "text/plain", "plain" ->
                CodeEditorSurfaceFormatEngine.trimTrailingWhitespace(content)
            else -> CodeEditorSurfaceFormatEngine.trimTrailingWhitespace(content)
        }
    }

    fun rstaSyntax(syntax: String): String = when (normalize(syntax)) {
        "application/json", "json" -> org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_JSON
        "text/java", "java" -> org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_JAVA
        "application/javascript", "text/javascript", "js" -> org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT
        "text/typescript", "ts" -> org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT
        "text/python", "py" -> org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_PYTHON
        "text/xml", "application/xml", "xml" -> org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_XML
        "text/html", "html" -> org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_HTML
        "text/yaml", "yaml", "yml" -> org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_YAML
        "text/sql", "sql" -> org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_SQL
        "text/markdown", "markdown", "md" -> org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_MARKDOWN
        else -> org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_NONE
    }

    fun editorFont(name: String): String {
        val normalized = name.trim()
        return when (normalized.lowercase()) {
            "", "system", "ui-monospace", "monospace" -> "Monospaced"
            "sans", "sans-serif" -> "SansSerif"
            "serif" -> "Serif"
            else -> normalized
        }
    }

    private fun normalize(syntax: String): String = syntax.trim().lowercase()

}
