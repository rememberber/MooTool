package com.rememberber.mootool.next.compose.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** F05 运行台历史 `options`：对齐 Electron `JSON.stringify(runOption)`。 */
@Serializable
data class CodeRunHistoryMeta(
    val arguments: String = "",
    val workingDirectory: String = "",
    val runtime: String = "",
)

object CodeRunHistoryMetadata {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(runtime: CodeRuntime, arguments: String, workingDirectory: String): String =
        json.encodeToString(
            CodeRunHistoryMeta.serializer(),
            CodeRunHistoryMeta(
                arguments = CodeRunRuntimeOptionsNormalize.normalizeArguments(arguments),
                workingDirectory = CodeRunRuntimeOptionsNormalize.normalizeWorkingDirectory(workingDirectory),
                runtime = runtimeWire(runtime),
            ),
        )

    fun decode(options: String): CodeRunHistoryMeta? {
        val trimmed = options.trim()
        if (trimmed.isEmpty()) return null
        return runCatching { json.decodeFromString<CodeRunHistoryMeta>(trimmed) }.getOrNull()
    }

    fun runtimeWire(runtime: CodeRuntime): String = when (runtime) {
        CodeRuntime.Groovy -> "groovy"
        CodeRuntime.Python -> "python"
        CodeRuntime.Node -> "node"
        CodeRuntime.Java -> "java"
    }

    fun parseRuntime(wire: String): CodeRuntime = CodeRunEngine.parseProvider(wire)
}
