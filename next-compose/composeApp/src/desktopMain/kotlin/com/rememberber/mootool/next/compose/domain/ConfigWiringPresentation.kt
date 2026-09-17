package com.rememberber.mootool.next.compose.domain

/** F06 配置转换：源非空守卫（Properties/YAML/校验 Tab）。 */
object ConfigWiringPresentation {
    fun canToYaml(properties: String): Boolean = properties.isNotBlank()

    fun canToProperties(yaml: String): Boolean = yaml.isNotBlank()

    fun canValidateSource(source: String): Boolean = source.isNotBlank()
}
