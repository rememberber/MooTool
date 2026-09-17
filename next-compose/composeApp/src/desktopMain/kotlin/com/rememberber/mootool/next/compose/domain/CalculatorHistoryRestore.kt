package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.CalculatorSession

/** F03 计算器历史恢复。 */
object CalculatorHistoryRestore {
    fun apply(session: CalculatorSession, item: HistoryRecord) {
        session.expression = item.input
        session.result = item.output.ifBlank { session.result }
    }
}
