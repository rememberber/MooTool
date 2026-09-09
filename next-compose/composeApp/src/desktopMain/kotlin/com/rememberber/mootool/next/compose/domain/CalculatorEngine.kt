package com.rememberber.mootool.next.compose.domain

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

class CalculatorException(val code: String, message: String) : RuntimeException(message)

object CalculatorEngine {
    private val allowedExpression = Regex("""^[\d+\-*/().\s]+$""")
    private val integerPattern = Regex("""^[+-]?\d+$""")
    private val displayContext = MathContext(14, RoundingMode.HALF_EVEN)

    fun evaluateExpression(expression: String): String {
        val source = expression.trim().removeSuffix("=").trim()
        if (source.isEmpty() || source.length > 500 || !allowedExpression.matches(source)) {
            throw CalculatorException("invalid-expression", "invalid-expression")
        }
        val result = ArithmeticParser(source).parse()
        if (!result.isFinite()) throw CalculatorException("non-finite", "non-finite")
        return BigDecimal.valueOf(result).round(displayContext).stripTrailingZeros().toPlainString()
    }

    fun convertBase(value: String, from: Int, to: Int): String {
        val normalized = value.trim()
        if (normalized.isEmpty()) throw CalculatorException("value-required", "value-required")
        val negative = normalized.startsWith("-")
        val unsigned = normalized.removePrefix("+").removePrefix("-")
        val valid = when (from) {
            2 -> Regex("^[01]+$")
            10 -> Regex("^\\d+$")
            16 -> Regex("^[\\da-fA-F]+$")
            else -> throw CalculatorException("invalid-base", "invalid-base")
        }
        if (!valid.matches(unsigned)) throw CalculatorException("invalid-base-value", "invalid-base-value")
        val parsed = when (from) {
            2 -> BigInteger(unsigned, 2)
            10 -> BigInteger(unsigned)
            else -> BigInteger(unsigned, 16)
        }
        val converted = parsed.toString(to)
        return if (negative && parsed != BigInteger.ZERO) "-$converted" else converted
    }

    fun gcd(left: String, right: String): String {
        var a = parseInteger(left).abs()
        var b = parseInteger(right).abs()
        while (b != BigInteger.ZERO) {
            val next = a.mod(b)
            a = b
            b = next
        }
        return a.toString()
    }

    fun lcm(left: String, right: String): String {
        val a = parseInteger(left)
        val b = parseInteger(right)
        if (a == BigInteger.ZERO || b == BigInteger.ZERO) return "0"
        return a.divide(BigInteger(gcd(left, right))).multiply(b).abs().toString()
    }

    fun permutation(nValue: String, mValue: String): String {
        val (n, m) = parseCountPair(nValue, mValue)
        var result = BigInteger.ONE
        var value = n - m + 1
        while (value <= n) {
            result = result.multiply(BigInteger.valueOf(value.toLong()))
            value += 1
        }
        return result.toString()
    }

    fun combination(nValue: String, mValue: String): String {
        val (n, requested) = parseCountPair(nValue, mValue)
        val m = minOf(requested, n - requested)
        var result = BigInteger.ONE
        var index = 1
        while (index <= m) {
            result = result.multiply(BigInteger.valueOf((n - m + index).toLong())).divide(BigInteger.valueOf(index.toLong()))
            index += 1
        }
        return result.toString()
    }

    private fun parseInteger(value: String): BigInteger {
        val trimmed = value.trim()
        if (!integerPattern.matches(trimmed)) throw CalculatorException("integer-required", "integer-required")
        return BigInteger(trimmed)
    }

    private fun parseCountPair(nValue: String, mValue: String): Pair<Int, Int> {
        val n = parseCount(nValue)
        val m = parseCount(mValue)
        if (m > n) throw CalculatorException("count-range", "count-range")
        return n to m
    }

    private fun parseCount(value: String): Int {
        val number = value.trim().toDoubleOrNull() ?: throw CalculatorException("count-range", "count-range")
        if (number != kotlin.math.floor(number) || number < 0 || number > 5000) {
            throw CalculatorException("count-range", "count-range")
        }
        return number.toInt()
    }
}

private class ArithmeticParser(private val source: String) {
    private var position = 0

    fun parse(): Double {
        val result = parseAddition()
        skipWhitespace()
        if (position != source.length) throw CalculatorException("invalid-expression", "invalid-expression")
        return result
    }

    private fun parseAddition(): Double {
        var result = parseMultiplication()
        while (true) {
            result = when {
                consume('+') -> result + parseMultiplication()
                consume('-') -> result - parseMultiplication()
                else -> return result
            }
        }
    }

    private fun parseMultiplication(): Double {
        var result = parseUnary()
        while (true) {
            result = when {
                consume('*') -> result * parseUnary()
                consume('/') -> result / parseUnary()
                else -> return result
            }
        }
    }

    private fun parseUnary(): Double {
        if (consume('+')) return parseUnary()
        if (consume('-')) return -parseUnary()
        return parsePrimary()
    }

    private fun parsePrimary(): Double {
        if (consume('(')) {
            val result = parseAddition()
            if (!consume(')')) throw CalculatorException("invalid-expression", "invalid-expression")
            return result
        }
        skipWhitespace()
        val remaining = source.substring(position)
        val match = Regex("""^(?:\d+(?:\.\d*)?|\.\d+)""").find(remaining)
            ?: throw CalculatorException("invalid-expression", "invalid-expression")
        position += match.value.length
        return match.value.toDouble()
    }

    private fun consume(token: Char): Boolean {
        skipWhitespace()
        if (source.getOrNull(position) != token) return false
        position += 1
        return true
    }

    private fun skipWhitespace() {
        while (source.getOrNull(position)?.isWhitespace() == true) position += 1
    }
}
