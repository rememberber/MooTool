package com.rememberber.mootool.next.compose.domain

/**
 * 对齐 Electron `resolveTextCodeEditorLanguage`（`codeEditorLanguage.ts`）。
 */
enum class TextCodeEditorLanguage {
    Text,
    Json,
    Markdown,
    Java,
    Javascript,
    Typescript,
    Python,
    Xml,
    Html,
    Yaml,
    Sql,
}

object TextCodeEditorLanguages {
    fun resolve(value: String?): TextCodeEditorLanguage {
        val normalized = value?.trim()?.lowercase().orEmpty()
        if (normalized.isEmpty()) return TextCodeEditorLanguage.Text
        if (normalized == "json" || normalized == "application/json" || normalized.endsWith("+json")) {
            return TextCodeEditorLanguage.Json
        }
        if (normalized == "markdown" || normalized == "md" || normalized == "text/markdown") {
            return TextCodeEditorLanguage.Markdown
        }
        if (normalized == "java" || normalized == "text/java") return TextCodeEditorLanguage.Java
        if (normalized in setOf("javascript", "js", "node", "text/javascript", "application/javascript")) {
            return TextCodeEditorLanguage.Javascript
        }
        if (normalized in setOf("typescript", "ts", "text/typescript", "application/typescript")) {
            return TextCodeEditorLanguage.Typescript
        }
        if (normalized == "python" || normalized == "py" || normalized == "text/python") {
            return TextCodeEditorLanguage.Python
        }
        if (normalized == "xml" || normalized == "text/xml" || normalized == "application/xml" || normalized.endsWith("+xml")) {
            return TextCodeEditorLanguage.Xml
        }
        if (normalized == "html" || normalized == "text/html") return TextCodeEditorLanguage.Html
        if (normalized in setOf("yaml", "yml", "text/yaml", "application/yaml", "application/x-yaml")) {
            return TextCodeEditorLanguage.Yaml
        }
        if (normalized == "sql" || normalized == "text/sql" || normalized == "application/sql") {
            return TextCodeEditorLanguage.Sql
        }
        return TextCodeEditorLanguage.Text
    }

    /** Git diff / Vault 路径扩展名 → 语言 hint（对齐 Electron `VaultGitDiffView`）。 */
    fun resolveFromPath(path: String): TextCodeEditorLanguage {
        val extension = path.substringAfterLast('.', "").substringBefore('→').trim().lowercase()
        if (extension.isEmpty()) return TextCodeEditorLanguage.Text
        return resolve(extension)
    }
}
