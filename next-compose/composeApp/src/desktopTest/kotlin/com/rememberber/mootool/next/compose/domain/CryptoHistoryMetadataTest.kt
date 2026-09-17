package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CryptoHistoryMetadataTest {
    @Test
    fun roundTripsTabOperationAndAlgorithm() {
        val encoded = CryptoHistoryMetadata.encode("asymmetric", "sign", AsymmetricAlgorithm.SM2.name)
        val parsed = CryptoHistoryMetadata.decode(encoded)!!
        assertEquals("asymmetric", parsed.tab)
        assertEquals("sign", parsed.operation)
        assertEquals(AsymmetricAlgorithm.SM2.name, parsed.algorithm)
    }

    @Test
    fun decodeReturnsNullForEmptyOptions() {
        assertNull(CryptoHistoryMetadata.decode(""))
    }
}
