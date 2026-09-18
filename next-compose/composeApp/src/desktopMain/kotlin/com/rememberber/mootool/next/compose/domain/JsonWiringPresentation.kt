package com.rememberber.mootool.next.compose.domain

import java.io.File
import java.nio.charset.StandardCharsets

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

    sealed interface ImportOutcome {
        data class Success(val content: String) : ImportOutcome
        data class Failure(val error: Throwable) : ImportOutcome
    }

    fun runReadImportFile(file: File): ImportOutcome =
        runCatching { file.readText(StandardCharsets.UTF_8) }.fold(
            onSuccess = { ImportOutcome.Success(it) },
            onFailure = { ImportOutcome.Failure(it) },
        )

    sealed interface WriteExportOutcome {
        data object Success : WriteExportOutcome
        data class Failure(val error: Throwable) : WriteExportOutcome
    }

    fun runWriteExportFile(file: File, content: String): WriteExportOutcome =
        runCatching { file.writeText(content, StandardCharsets.UTF_8) }.fold(
            onSuccess = { WriteExportOutcome.Success },
            onFailure = { WriteExportOutcome.Failure(it) },
        )

    fun shouldToastTransformFailure(error: Throwable): Boolean = true

    fun shouldToastIoFailure(error: Throwable): Boolean = true
}
