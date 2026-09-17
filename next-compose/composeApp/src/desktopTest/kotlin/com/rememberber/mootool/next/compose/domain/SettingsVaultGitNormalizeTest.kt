package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.AppSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsVaultGitNormalizeTest {
    @Test
    fun trimsAndCapsVaultGitFields() {
        val raw = AppSettings.Default.copy(
            vault = AppSettings.Default.vault.copy(
                gitUsername = "  name  ",
                gitRemote = " https://x.test/r.git ",
                gitToken = "\t token \n",
            ),
        )
        val normalized = SettingsVaultGitNormalize.apply(raw)
        assertEquals("name", normalized.vault.gitUsername)
        assertEquals("https://x.test/r.git", normalized.vault.gitRemote)
        assertEquals("token", normalized.vault.gitToken)
    }
}
