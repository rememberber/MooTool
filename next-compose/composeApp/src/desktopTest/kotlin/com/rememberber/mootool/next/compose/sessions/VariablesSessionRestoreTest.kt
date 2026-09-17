package com.rememberber.mootool.next.compose.sessions

import com.rememberber.mootool.next.compose.domain.EnvDisplayScope
import com.rememberber.mootool.next.compose.domain.EnvTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class VariablesSessionRestoreTest {
    @Test
    fun restoresTabScopeQueryAndClearsSnapshot() {
        val session = VariablesSession()
        session.tab = EnvTab.Runtime
        session.scope = EnvDisplayScope.System
        session.query = "PATH"
        session.snapshot = com.rememberber.mootool.next.compose.domain.EnvSnapshot(
            process = emptyList(),
            runtime = emptyList(),
            user = emptyList(),
            system = emptyList(),
            userFile = "",
            systemFile = "",
            shellProfile = "",
        )
        session.loading = true
        session.error = "err"

        session.restore(VariablesSessionSnapshot(tab = "environment", scope = "user", query = "JAVA"))

        assertEquals(EnvTab.Environment, session.tab)
        assertEquals(EnvDisplayScope.User, session.scope)
        assertEquals("JAVA", session.query)
        assertNull(session.snapshot)
        assertFalse(session.loading)
        assertEquals("", session.error)
    }
}
