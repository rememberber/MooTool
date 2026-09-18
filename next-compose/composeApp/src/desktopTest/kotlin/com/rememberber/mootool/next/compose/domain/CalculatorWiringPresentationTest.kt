package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalculatorWiringPresentationTest {
    @Test
    fun evaluateRequiresExpression() {
        assertFalse(CalculatorWiringPresentation.canEvaluate(""))
        assertTrue(CalculatorWiringPresentation.canEvaluate("1+1"))
    }

    @Test
    fun binaryOpsNeedBothOperands() {
        assertFalse(CalculatorWiringPresentation.canBinaryOp("1", ""))
        assertTrue(CalculatorWiringPresentation.canBinaryOp("1", "2"))
    }

    @Test
    fun runEvaluateUsesEngine() {
        val outcome = CalculatorWiringPresentation.runEvaluate("1+2")
        assertTrue(outcome is CalculatorWiringPresentation.TextOutcome.Success)
        assertEquals("3", (outcome as CalculatorWiringPresentation.TextOutcome.Success).value)
    }

    @Test
    fun shouldToastOperationFailure() {
        assertTrue(CalculatorWiringPresentation.shouldToastOperationFailure(IllegalStateException()))
    }

    @Test
    fun convertBaseActionEnabled() {
        assertTrue(CalculatorWiringPresentation.convertBaseActionEnabled("ff", 16, 10))
        assertFalse(CalculatorWiringPresentation.convertBaseActionEnabled("", 16, 10))
        assertFalse(CalculatorWiringPresentation.convertBaseActionEnabled("gg", 16, 10))
        assertTrue(CalculatorWiringPresentation.convertBaseActionEnabled("1010", 2, 10))
        assertFalse(CalculatorWiringPresentation.convertBaseActionEnabled("102", 2, 10))
        assertFalse(CalculatorWiringPresentation.canCopyResult(""))
        assertTrue(CalculatorWiringPresentation.canCopyResult("42"))
        assertFalse(CalculatorWiringPresentation.evaluateActionEnabled(""))
        assertTrue(CalculatorWiringPresentation.evaluateActionEnabled("1+1"))
        assertTrue(CalculatorWiringPresentation.copyResultActionEnabled("42"))
    }
}
