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

    @Test
    fun clearsInvalidPersistedGitRemote() {
        val raw = AppSettings.Default.copy(
            vault = AppSettings.Default.vault.copy(
                gitRemote = " not-a-url ",
            ),
        )
        assertEquals("", SettingsVaultGitNormalize.apply(raw).vault.gitRemote)
        val ok = AppSettings.Default.copy(
            vault = AppSettings.Default.vault.copy(gitRemote = " git@github.com:org/repo.git "),
        )
        assertEquals("git@github.com:org/repo.git", SettingsVaultGitNormalize.apply(ok).vault.gitRemote)
    }

    @Test
    fun sanitizeGitRemoteMatchesGitEngineRules() {
        assertEquals("", SettingsVaultGitNormalize.sanitizeGitRemote("ftp://bad"))
        assertEquals("https://x.test/r.git", SettingsVaultGitNormalize.sanitizeGitRemote("https://x.test/r.git"))
    }
}
