package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.AppSettings

/** Trims Vault Git credential fields on load/import (aligns with `GitEngine.normalizeGitRemote` / commit identity). */
object SettingsVaultGitNormalize {
    private const val MAX_USERNAME = 128
    private const val MAX_REMOTE = 2048
    private const val MAX_TOKEN = 2048

    fun apply(settings: AppSettings): AppSettings {
        val vault = settings.vault
        return settings.copy(
            vault = vault.copy(
                gitUsername = vault.gitUsername.trim().take(MAX_USERNAME),
                gitRemote = vault.gitRemote.trim().take(MAX_REMOTE),
                gitToken = vault.gitToken.trim().take(MAX_TOKEN),
            ),
        )
    }
}
