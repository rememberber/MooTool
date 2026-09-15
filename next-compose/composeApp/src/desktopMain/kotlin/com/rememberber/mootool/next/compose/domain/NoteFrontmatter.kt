package com.rememberber.mootool.next.compose.domain

import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.time.Instant
import kotlin.math.roundToInt

data class NoteMetadata(
    val title: String,
    val style: String = "",
    val syntax: String = "text/plain",
    val fontName: String = "",
    val fontSize: Int = 14,
    val lineSpacing: Double = 1.0,
    val color: String = "default",
    val lineWrap: Boolean = true,
    val createdAt: String = "",
    val modifiedAt: String = ""
) {
    companion object {
        fun defaults(title: String, now: Instant = Instant.now()): NoteMetadata {
            val stamp = now.toString()
            return NoteMetadata(title = NoteFrontmatter.normalizeTitle(title), createdAt = stamp, modifiedAt = stamp)
        }
    }
}

data class ParsedNote(
    val content: String,
    val metadata: NoteMetadata
)

object NoteFrontmatter {
    private val fence = Regex("^---\\r?\\n([\\s\\S]*?)\\r?\\n---(?:\\r?\\n)?")
    val noteExtensions: Set<String> = setOf("txt", "md", "json", "java", "js", "ts", "py", "xml", "yaml", "yml", "sql")

    fun parse(raw: String, fallbackTitle: String, createdAt: String = Instant.now().toString(), modifiedAt: String = createdAt): ParsedNote {
        val match = fence.find(raw)
        val values = if (match != null) loadMap(match.groupValues[1]) else emptyMap()
        val content = if (match != null) raw.substring(match.value.length) else raw
        return ParsedNote(
            content = content,
            metadata = NoteMetadata(
                title = stringValue(values["title"], fallbackTitle),
                style = stringValue(values["style"], ""),
                syntax = stringValue(values["syntax"], "text/plain"),
                fontName = stringValue(values["font_name"], ""),
                fontSize = clampFontSize(numberValue(values["font_size"], 14.0)),
                lineSpacing = clampLineSpacing(numberValue(values["line_spacing"], 1.0)),
                color = stringValue(values["color"], "default"),
                lineWrap = booleanValue(values["line_wrap"], true),
                createdAt = stringValue(values["created_at"], createdAt),
                modifiedAt = stringValue(values["modified_at"], modifiedAt)
            )
        )
    }

    fun serialize(metadata: NoteMetadata, content: String): String {
        val normalized = normalize(metadata, metadata.title)
        val yaml = buildString {
            appendLine("title: ${yamlScalar(normalized.title)}")
            appendLine("style: ${yamlScalar(normalized.style)}")
            appendLine("syntax: ${yamlScalar(normalized.syntax)}")
            appendLine("font_name: ${yamlScalar(normalized.fontName)}")
            appendLine("font_size: \"${normalized.fontSize}\"")
            appendLine("line_spacing: \"${formatLineSpacing(normalized.lineSpacing)}\"")
            appendLine("color: ${yamlScalar(normalized.color)}")
            appendLine("line_wrap: \"${if (normalized.lineWrap) "1" else "0"}\"")
            appendLine("created_at: ${yamlScalar(normalized.createdAt)}")
            appendLine("modified_at: ${yamlScalar(normalized.modifiedAt)}")
        }
        val prefix = if (content.isNotEmpty() && !content.startsWith("\n")) "\n" else ""
        return "---\n$yaml---$prefix$content"
    }

    fun normalize(metadata: NoteMetadata, fallbackTitle: String, now: Instant = Instant.now()): NoteMetadata {
        val stamp = now.toString()
        return NoteMetadata(
            title = normalizeTitle(metadata.title.ifBlank { fallbackTitle }),
            style = metadata.style.take(64),
            syntax = metadata.syntax.ifBlank { "text/plain" }.take(80),
            fontName = metadata.fontName.take(120),
            fontSize = clampFontSize(metadata.fontSize.toDouble()),
            lineSpacing = clampLineSpacing(metadata.lineSpacing),
            color = metadata.color.ifBlank { "default" }.take(32),
            lineWrap = metadata.lineWrap,
            createdAt = metadata.createdAt.ifBlank { stamp },
            modifiedAt = stamp
        )
    }

    fun normalizeTitle(value: String): String {
        val title = value.trim()
        require(title.isNotEmpty() && title.length <= 180 && !title.contains('\n') && !title.contains('\r') && !title.contains('\u0000')) {
            "Invalid title"
        }
        return title
    }

    fun sanitizeName(value: String): String {
        val sanitized = value.replace(Regex("""[\\/:*?"<>|]"""), "_").replace(Regex("""^\.+"""), "").trim()
        require(sanitized.isNotEmpty()) { "Invalid file name" }
        return sanitized.take(180)
    }

    fun extensionForSyntax(syntax: String): String = when (syntax) {
        "text/markdown" -> "md"
        "application/json" -> "json"
        "text/java" -> "java"
        "text/javascript" -> "js"
        "text/typescript" -> "ts"
        "text/python" -> "py"
        "text/xml" -> "xml"
        "text/yaml" -> "yaml"
        "text/sql" -> "sql"
        else -> "txt"
    }

    fun syntaxForExtension(extension: String): String = when (extension.lowercase()) {
        "md", "markdown" -> "text/markdown"
        "json" -> "application/json"
        "java" -> "text/java"
        "js" -> "text/javascript"
        "ts" -> "text/typescript"
        "py" -> "text/python"
        "xml" -> "text/xml"
        "yaml", "yml" -> "text/yaml"
        "sql" -> "text/sql"
        else -> "text/plain"
    }

    fun withNoteExtension(path: String, syntax: String): String {
        val slash = path.lastIndexOf('/')
        val name = if (slash >= 0) path.substring(slash + 1) else path
        val parent = if (slash >= 0) path.substring(0, slash) else ""
        val stem = name.substringBeforeLast('.', name)
        val next = "$stem.${extensionForSyntax(syntax)}"
        return if (parent.isBlank()) next else "$parent/$next"
    }

    fun clampFontSize(value: Double): Int = value.roundToInt().coerceIn(8, 48).let { if (it == 0) 14 else it }

    fun clampLineSpacing(value: Double): Double {
        val normalized = if (value.isFinite()) value else 1.0
        return (normalized.coerceIn(1.0, 2.0) * 10).roundToInt() / 10.0
    }

    fun formatLineSpacing(value: Double): String = "%.1f".format(java.util.Locale.US, clampLineSpacing(value))

    private fun loadMap(yaml: String): Map<String, Any?> {
        if (yaml.isBlank()) return emptyMap()
        return try {
            val options = LoaderOptions().apply {
                maxAliasesForCollections = 16
                codePointLimit = 64 * 1024
            }
            val loaded = Yaml(SafeConstructor(options)).load<Any?>(yaml)
            if (loaded is Map<*, *>) loaded.entries.associate { it.key.toString() to it.value } else emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun stringValue(value: Any?, fallback: String): String = value?.toString() ?: fallback

    private fun numberValue(value: Any?, fallback: Double): Double = when (value) {
        null -> fallback
        is Number -> value.toDouble()
        else -> value.toString().toDoubleOrNull() ?: fallback
    }

    private fun booleanValue(value: Any?, fallback: Boolean): Boolean = when (value) {
        null, "" -> fallback
        true, 1, "1", "true", "TRUE" -> true
        false, 0, "0", "false", "FALSE" -> false
        else -> fallback
    }

    private fun yamlScalar(value: String): String {
        if (value.isEmpty()) return "\"\""
        if (value.matches(Regex("""^[A-Za-z0-9._ /+-]+$"""))) return value
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }
}
