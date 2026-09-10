package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CalculatorEngineTest {
    @Test
    fun evaluatesArithmeticWithoutSymbolsOrAssignments() {
        assertEquals("14", CalculatorEngine.evaluateExpression("2 * (3 + 4)="))
        assertEquals("2", CalculatorEngine.evaluateExpression("-3 + 10 / 2"))
        assertEquals("4", CalculatorEngine.evaluateExpression(".5 * 8"))
        assertFailsWith<CalculatorException> { CalculatorEngine.evaluateExpression("x = 2") }
        assertFailsWith<CalculatorException> { CalculatorEngine.evaluateExpression("1 / 0") }
        assertFailsWith<CalculatorException> { CalculatorEngine.evaluateExpression("(1 + 2") }
    }

    @Test
    fun convertsIntegerBasesAndNumberOperations() {
        assertEquals("ff", CalculatorEngine.convertBase("255", 10, 16))
        assertEquals("255", CalculatorEngine.convertBase("11111111", 2, 10))
        assertEquals("6", CalculatorEngine.gcd("54", "24"))
        assertEquals("24", CalculatorEngine.lcm("6", "8"))
    }

    @Test
    fun calculatesPermutationsAndCombinationsExactly() {
        assertEquals("20", CalculatorEngine.permutation("5", "2"))
        assertEquals("10", CalculatorEngine.combination("5", "2"))
    }

    @Test
    fun rejectsZeroAndOutOfRangeCombinatorics() {
        assertEquals("0", CalculatorEngine.lcm("0", "8"))
        assertFailsWith<CalculatorException> { CalculatorEngine.permutation("3", "4") }
        assertFailsWith<CalculatorException> { CalculatorEngine.combination("-1", "1") }
        assertFailsWith<CalculatorException> { CalculatorEngine.convertBase("", 10, 16) }
        assertFailsWith<CalculatorException> { CalculatorEngine.convertBase("2", 2, 10) }
    }
}
