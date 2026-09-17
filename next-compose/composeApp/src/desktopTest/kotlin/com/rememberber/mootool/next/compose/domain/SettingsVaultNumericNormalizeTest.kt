package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SettingsVaultNumericNormalizeTest {
    @Test
    fun commitAutoCommitIdleSeconds_clampsAndRejects() {
        assertIs<VaultNumericCommitResult.Accepted>(
            SettingsVaultNumericNormalize.commitAutoCommitIdleSeconds("30"),
        ).let { assertEquals(30, it.value) }
        assertEquals(
            5,
            (SettingsVaultNumericNormalize.commitAutoCommitIdleSeconds("1") as VaultNumericCommitResult.Accepted).value,
        )
        assertEquals(
            3_600,
            (SettingsVaultNumericNormalize.commitAutoCommitIdleSeconds("99999") as VaultNumericCommitResult.Accepted).value,
        )
        assertIs<VaultNumericCommitResult.Rejected>(
            SettingsVaultNumericNormalize.commitAutoCommitIdleSeconds("abc"),
        )
    }

    @Test
    fun commitAutoPullMinutes_allowsZeroAndClamps() {
        assertEquals(
            0,
            (SettingsVaultNumericNormalize.commitAutoPullMinutes("0") as VaultNumericCommitResult.Accepted).value,
        )
        assertEquals(
            1_440,
            (SettingsVaultNumericNormalize.commitAutoPullMinutes("5000") as VaultNumericCommitResult.Accepted).value,
        )
    }
}
