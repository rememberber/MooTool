package com.rememberber.mootool.next.compose.domain

import java.io.File
import java.nio.charset.StandardCharsets

/** F06 配置转换：源非空守卫与引擎 run*（对齐 [CronWiringPresentation.runPreview]）。 */
object ConfigWiringPresentation {
    fun canToYaml(properties: String): Boolean = properties.isNotBlank()

    fun canToProperties(yaml: String): Boolean = yaml.isNotBlank()

    fun canValidateSource(source: String): Boolean = source.isNotBlank()

    sealed interface ConvertOutcome {
        data class Success(val output: String) : ConvertOutcome
        data class Failure(val error: Throwable) : ConvertOutcome
    }

    fun runToYaml(properties: String): ConvertOutcome =
        runCatching { ConfigEngine.propertiesToYaml(properties) }.fold(
            onSuccess = { ConvertOutcome.Success(it) },
            onFailure = { ConvertOutcome.Failure(it) },
        )

    fun runToProperties(yaml: String): ConvertOutcome =
        runCatching { ConfigEngine.yamlToProperties(yaml) }.fold(
            onSuccess = { ConvertOutcome.Success(it) },
            onFailure = { ConvertOutcome.Failure(it) },
        )

    fun runValidate(source: String): YamlValidation = ConfigEngine.validateYaml(source)

    fun runFormat(source: String): ConvertOutcome =
        runCatching { ConfigEngine.formatYaml(source) }.fold(
            onSuccess = { ConvertOutcome.Success(it) },
            onFailure = { ConvertOutcome.Failure(it) },
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

    fun shouldToastConvertFailure(error: Throwable): Boolean = true

    fun shouldToastIoFailure(error: Throwable): Boolean = true
}
