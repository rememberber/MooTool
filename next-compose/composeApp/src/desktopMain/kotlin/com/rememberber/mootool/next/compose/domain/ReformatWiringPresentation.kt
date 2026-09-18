package com.rememberber.mootool.next.compose.domain

import java.io.File
import java.nio.charset.StandardCharsets

/** F03 格式化：引擎调用、另存默认名与 UI 守卫（对齐 `ReformatScreen`）。 */
object ReformatWiringPresentation {
    fun canRunFormat(busy: Boolean, inputNotBlank: Boolean): Boolean = !busy && inputNotBlank

    /** 对齐 `ReformatScreen` 格式化钮（Electron `disabled={busy || !input.trim()}`）。 */
    fun formatActionEnabled(busy: Boolean, inputNotBlank: Boolean): Boolean = canRunFormat(busy, inputNotBlank)

    /** 对齐 `ReformatScreen` 复制结果：当前 Tab 无输出时禁用。 */
    fun copyResultActionEnabled(output: String): Boolean = output.isNotBlank()

    /** 对齐 `ReformatScreen` 另存结果：当前 Tab 无输出时禁用。 */
    fun saveResultActionEnabled(output: String): Boolean = output.isNotBlank()

    sealed interface FormatRunOutcome {
        data class Success(val output: String) : FormatRunOutcome
        data class Failure(val error: Throwable) : FormatRunOutcome
    }

    data class FormatErrorMessage(val key: String, val params: Map<String, String> = emptyMap())

    fun runFormat(input: String, type: ReformatType, indent: Int): FormatRunOutcome =
        runCatching { ReformatEngine.format(input, type, indent) }.fold(
            onSuccess = { FormatRunOutcome.Success(it) },
            onFailure = { FormatRunOutcome.Failure(it) },
        )

    fun shouldToastFormatFailure(error: Throwable): Boolean = true

    fun shouldToastIoFailure(error: Throwable): Boolean = true

    fun formatErrorMessage(error: Throwable): FormatErrorMessage {
        val reformat = error as? ReformatException
        return if (reformat != null && reformat.line >= 1) {
            FormatErrorMessage(
                "reformat.error.located",
                mapOf(
                    "line" to reformat.line.toString(),
                    "column" to reformat.column.coerceAtLeast(0).toString(),
                    "message" to (reformat.message ?: ""),
                ),
            )
        } else {
            FormatErrorMessage("reformat.error.generic", mapOf("message" to (error.message ?: "")))
        }
    }

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

    sealed interface ReadSourceOutcome {
        data class Success(
            val fileName: String,
            val content: String,
            val inferredType: ReformatType?,
        ) : ReadSourceOutcome

        data class Failure(val error: Throwable) : ReadSourceOutcome
    }

    fun runReadSourceFile(file: File): ReadSourceOutcome =
        runCatching {
            ReadSourceOutcome.Success(
                fileName = file.name,
                content = file.readText(StandardCharsets.UTF_8),
                inferredType = inferTypeFromFileName(file.name),
            )
        }.getOrElse { ReadSourceOutcome.Failure(it) }

    fun inferTypeFromFileName(fileName: String): ReformatType? {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "conf", "nginx" -> ReformatType.Nginx
            "java" -> ReformatType.Java
            "xml", "xsd", "svg" -> ReformatType.Xml
            "html", "htm" -> ReformatType.Html
            else -> null
        }
    }

    sealed interface WriteResultOutcome {
        data object Success : WriteResultOutcome
        data class Failure(val error: Throwable) : WriteResultOutcome
    }

    fun runWriteResult(file: File, content: String): WriteResultOutcome =
        runCatching { file.writeText(content, StandardCharsets.UTF_8) }.fold(
            onSuccess = { WriteResultOutcome.Success },
            onFailure = { WriteResultOutcome.Failure(it) },
        )
}
