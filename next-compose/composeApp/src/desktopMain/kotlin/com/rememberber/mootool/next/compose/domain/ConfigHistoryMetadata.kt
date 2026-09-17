package com.rememberber.mootool.next.compose.domain

/** F06 配置转换历史 `options`（convert / validate / format）。 */
object ConfigHistoryMetadata {
    const val TO_YAML = "toYaml"
    const val TO_PROPERTIES = "toProperties"
    const val VALIDATE = "validate"
    const val FORMAT = "format"

    fun encodeConvert(toYaml: Boolean): String = if (toYaml) TO_YAML else TO_PROPERTIES

    fun isConvert(options: String): Boolean =
        options == TO_YAML || options == TO_PROPERTIES
}
