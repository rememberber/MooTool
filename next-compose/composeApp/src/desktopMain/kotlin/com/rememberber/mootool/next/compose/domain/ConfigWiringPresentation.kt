package com.rememberber.mootool.next.compose.domain

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
}
