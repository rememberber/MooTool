package com.rememberber.mootool.next.compose.domain

/** F21 计算器历史：options 留空，summary 存表达式摘要。 */
object CalculatorHistoryMetadata {
    const val MARKER = "calc"

    fun encode(): String = MARKER

    fun decode(options: String): String = options.trim().ifEmpty { MARKER }
}
