package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.AppSettings

/** Trims Vault Git credential fields on load/import (aligns with `GitEngine.normalizeGitRemote` / commit identity). */
object SettingsVaultGitNormalize {
    private const val MAX_USERNAME = 128
    private const val MAX_REMOTE = 2048
    private const val MAX_TOKEN = 2048
    private val GIT_REMOTE_PREFIX = Regex("^(https?://|ssh://|git://|git@|file://)", RegexOption.IGNORE_CASE)

    fun apply(settings: AppSettings): AppSettings {
        val vault = settings.vault
        return settings.copy(
            vault = vault.copy(
                gitUsername = vault.gitUsername.trim().take(MAX_USERNAME),
                gitRemote = sanitizeGitRemote(vault.gitRemote),
                gitToken = vault.gitToken.trim().take(MAX_TOKEN),
            ),
        )
    }

    /** Clears invalid persisted remotes (aligns with `GitEngine.normalizeGitRemote` on configure). */
    fun sanitizeGitRemote(value: String): String {
        val remote = value.trim().take(MAX_REMOTE)
        if (remote.isEmpty()) return ""
        if (remote.any { it == '\r' || it == '\n' || it == '\u0000' }) return ""
        if (!GIT_REMOTE_PREFIX.containsMatchIn(remote)) return ""
        return remote
    }

    /** Settings blur commit: empty clears; valid prefix accepted; invalid non-empty rejected (Electron `TextSetting` trim). */
    fun commitGitRemote(raw: String): GitRemoteCommitResult {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return GitRemoteCommitResult.Cleared
        val sanitized = sanitizeGitRemote(trimmed)
        return if (sanitized.isEmpty()) {
            GitRemoteCommitResult.Rejected
        } else {
            GitRemoteCommitResult.Accepted(sanitized)
        }
    }
}

sealed class GitRemoteCommitResult {
    data object Cleared : GitRemoteCommitResult()

    data class Accepted(val remote: String) : GitRemoteCommitResult()

    data object Rejected : GitRemoteCommitResult()
}
