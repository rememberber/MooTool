package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.CalculatorSession
import kotlin.test.Test
import kotlin.test.assertEquals

class CalculatorHistoryRestoreTest {
    @Test
    fun restoresExpressionAndResult() {
        val session = CalculatorSession()
        session.expression = "1+1"
        session.result = "2"
        CalculatorHistoryRestore.apply(
            session,
            HistoryRecord(
                toolId = "calculator",
                operation = "eval",
                summary = "2*(3+4)",
                input = "2*(3+4)",
                output = "14",
                options = "",
                createdAt = "",
            ),
        )
        assertEquals("2*(3+4)", session.expression)
        assertEquals("14", session.result)
    }
}
