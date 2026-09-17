package com.rememberber.mootool.next.compose.domain

/** F18 时间：字段转换与复制守卫。 */
object TimeWiringPresentation {
    fun canConvertTimestamp(timestamp: String): Boolean = timestamp.isNotBlank()

    fun canConvertLocal(localTime: String): Boolean = localTime.isNotBlank()

    fun canCopyField(value: String): Boolean = value.isNotBlank()
}
