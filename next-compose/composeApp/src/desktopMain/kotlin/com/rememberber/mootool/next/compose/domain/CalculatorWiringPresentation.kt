package com.rememberber.mootool.next.compose.domain

/** F21 计算器：运算与复制守卫与引擎路径（可单测）。 */
object CalculatorWiringPresentation {
    fun canEvaluate(expression: String): Boolean = expression.isNotBlank()

    fun evaluateActionEnabled(expression: String): Boolean = canEvaluate(expression)

    fun canCopyResult(result: String): Boolean = result.isNotBlank()

    fun copyResultActionEnabled(result: String): Boolean = canCopyResult(result)

    fun canConvertField(value: String): Boolean = value.isNotBlank()

    fun convertBaseActionEnabled(value: String, fromRadix: Int, toRadix: Int): Boolean =
        runCatching {
            CalculatorEngine.convertBase(value, fromRadix, toRadix)
            true
        }.getOrDefault(false)

    fun canBinaryOp(first: String, second: String): Boolean = first.isNotBlank() && second.isNotBlank()

    sealed interface TextOutcome {
        data class Success(val value: String) : TextOutcome
        data class Failure(val error: Throwable) : TextOutcome
    }

    fun runEvaluate(expression: String): TextOutcome =
        runCatching { CalculatorEngine.evaluateExpression(expression) }.fold(
            onSuccess = { TextOutcome.Success(it) },
            onFailure = { TextOutcome.Failure(it) },
        )

    fun runConvertBase(value: String, fromRadix: Int, toRadix: Int): TextOutcome =
        runCatching { CalculatorEngine.convertBase(value, fromRadix, toRadix) }.fold(
            onSuccess = { TextOutcome.Success(it) },
            onFailure = { TextOutcome.Failure(it) },
        )

    fun runGcd(first: String, second: String): TextOutcome =
        runCatching { CalculatorEngine.gcd(first, second) }.fold(
            onSuccess = { TextOutcome.Success(it) },
            onFailure = { TextOutcome.Failure(it) },
        )

    fun runLcm(first: String, second: String): TextOutcome =
        runCatching { CalculatorEngine.lcm(first, second) }.fold(
            onSuccess = { TextOutcome.Success(it) },
            onFailure = { TextOutcome.Failure(it) },
        )

    fun runPermutation(n: String, m: String): TextOutcome =
        runCatching { CalculatorEngine.permutation(n, m) }.fold(
            onSuccess = { TextOutcome.Success(it) },
            onFailure = { TextOutcome.Failure(it) },
        )

    fun runCombination(n: String, m: String): TextOutcome =
        runCatching { CalculatorEngine.combination(n, m) }.fold(
            onSuccess = { TextOutcome.Success(it) },
            onFailure = { TextOutcome.Failure(it) },
        )

    fun shouldToastOperationFailure(error: Throwable): Boolean = true
}
