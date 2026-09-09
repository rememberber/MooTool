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
)

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
