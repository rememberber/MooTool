package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
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
}
