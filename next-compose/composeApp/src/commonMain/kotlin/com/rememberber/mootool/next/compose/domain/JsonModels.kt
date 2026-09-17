package com.rememberber.mootool.next.compose.domain

data class JsonStatus(
    val kind: Kind,
    val message: String,
    val line: Int? = null,
    val column: Int? = null
) {
    enum class Kind { Idle, Valid, Error }
}

data class JsonFormatOptions(
    val spaces: Int = 2,
    val sortKeys: Boolean = false,
    val ignoreCase: Boolean = false,
    val checkDuplicateKeys: Boolean = true
) {
    /** Aligns with Electron `JsonInspector` indent `<select>` (only 2 or 4). */
    fun normalizeInspectorIndent(): JsonFormatOptions {
        val normalized = if (spaces == 4) 4 else 2
        return if (normalized == spaces) this else copy(spaces = normalized)
    }
}

data class JsonPathEntry(
    val path: String,
    val label: String,
    val preview: String,
    val depth: Int
)

class JsonException(message: String, val line: Int? = null, val column: Int? = null) : RuntimeException(message)

fun interface JsonTranslator {
    fun t(key: String, params: Map<String, String>): String
}

fun JsonTranslator.t(key: String): String = t(key, emptyMap())
