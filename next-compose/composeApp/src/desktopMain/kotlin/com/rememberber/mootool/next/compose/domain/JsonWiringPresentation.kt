package com.rememberber.mootool.next.compose.domain

/** F04 JSON：校验/格式化/转换/JSONPath 引擎 run*（对齐 [ConfigWiringPresentation]）。 */
object JsonWiringPresentation {
    fun runValidate(input: String, t: JsonTranslator): JsonStatus = JsonEngine.validate(input, t)

    sealed interface TransformOutcome {
        data class Success(val output: String) : TransformOutcome
        data class Failure(val error: Throwable) : TransformOutcome
    }

    fun runTransform(input: String, block: (String) -> String): TransformOutcome =
        runCatching { block(input) }.fold(
            onSuccess = { TransformOutcome.Success(it) },
            onFailure = { TransformOutcome.Failure(it) },
        )

    fun runQuickFormat(input: String, t: JsonTranslator, spaces: Int): TransformOutcome =
        runTransform(input) { JsonEngine.format(it, t, spaces) }

    fun runCompress(input: String, t: JsonTranslator): TransformOutcome =
        runTransform(input) { JsonEngine.compress(it, t) }

    fun runFormatAdvanced(input: String, t: JsonTranslator, options: JsonFormatOptions): TransformOutcome =
        runTransform(input) { JsonEngine.formatAdvanced(it, t, options) }

    fun runQueryPath(input: String, path: String, t: JsonTranslator): TransformOutcome =
        runCatching { JsonEngine.queryPath(input, path, t) }.fold(
            onSuccess = { TransformOutcome.Success(it) },
            onFailure = { TransformOutcome.Failure(it) },
        )
}
