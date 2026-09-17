package com.rememberber.mootool.next.compose.domain

/** F03 格式化：引擎调用、另存默认名与 UI 守卫（对齐 `ReformatScreen`）。 */
object ReformatWiringPresentation {
    fun canRunFormat(busy: Boolean, inputNotBlank: Boolean): Boolean = !busy && inputNotBlank

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
}
