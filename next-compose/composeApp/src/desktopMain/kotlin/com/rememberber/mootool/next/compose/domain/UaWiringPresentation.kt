package com.rememberber.mootool.next.compose.domain

/** F12 UA 解析：解析按钮守卫与引擎路径（可单测）。 */
object UaWiringPresentation {
    fun canParse(source: String): Boolean = source.isNotBlank()

    fun canCopyResult(result: String): Boolean = result.isNotBlank()

    sealed interface ParseOutcome {
        data class Success(val result: UaResult) : ParseOutcome
        data class Failure(val error: Throwable) : ParseOutcome
    }

    fun runParse(source: String): ParseOutcome =
        runCatching { UaEngine.parse(source) }.fold(
            onSuccess = { ParseOutcome.Success(it) },
            onFailure = { ParseOutcome.Failure(it) },
        )
}
