package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import com.rememberber.mootool.next.compose.sessions.CryptoSession
import kotlin.test.Test
import kotlin.test.assertEquals

class CryptoHistoryRestoreTest {
    @Test
    fun restoresSymmetricEncryptFields() {
        val session = CryptoSession()
        val options = CryptoHistoryMetadata.encode("symmetric", "encrypt", SymmetricAlgorithm.AES.name)
        CryptoHistoryRestore.apply(
            session,
            HistoryRecord(
                id = 1,
                toolId = "crypto",
                operation = "encrypt",
                summary = "s",
                input = "plain",
                output = "cipher",
                options = options,
                createdAt = "0",
            ),
        )
        assertEquals(CryptoTab.Symmetric, session.tab)
        assertEquals(SymmetricAlgorithm.AES, session.symAlgorithm)
        assertEquals("plain", session.symPlain)
        assertEquals("cipher", session.symCipher)
        assertEquals("", session.error)
    }
}
