package com.rememberber.mootool.next.compose.domain

/** F21 计算器：运算与复制守卫。 */
object CalculatorWiringPresentation {
    fun canEvaluate(expression: String): Boolean = expression.isNotBlank()

    fun canCopyResult(result: String): Boolean = result.isNotBlank()

    fun canConvertField(value: String): Boolean = value.isNotBlank()

    fun canBinaryOp(first: String, second: String): Boolean = first.isNotBlank() && second.isNotBlank()
}
