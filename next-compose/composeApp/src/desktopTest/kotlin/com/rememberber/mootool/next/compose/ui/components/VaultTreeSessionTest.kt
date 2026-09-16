package com.rememberber.mootool.next.compose.ui.components

import com.rememberber.mootool.next.compose.sessions.JsonSession
import com.rememberber.mootool.next.compose.sessions.QuickNoteSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VaultTreeSessionTest {
    @Test
    fun resetClearsJsonVaultTreeUi() {
        val session = JsonSession().apply {
            vaultTreeExpanded = mapOf("a" to true)
            vaultTreeScrollOffset = 120
        }
        session.resetVaultTreeOnCustomRootChange()
        assertTrue(session.vaultTreeExpanded.isEmpty())
        assertEquals(0, session.vaultTreeScrollOffset)
    }

    @Test
    fun resetClearsQuickNoteVaultTreeUi() {
        val session = QuickNoteSession().apply {
            vaultTreeExpanded = mapOf("notes" to true)
            vaultTreeScrollOffset = 88
        }
        session.resetVaultTreeOnCustomRootChange()
        assertTrue(session.vaultTreeExpanded.isEmpty())
        assertEquals(0, session.vaultTreeScrollOffset)
    }
}
