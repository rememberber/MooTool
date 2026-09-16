package com.rememberber.mootool.next.compose.storage

import java.nio.file.Path

object VaultPathConfig {
    /**
     * Empty string keeps the product default vault directory.
     * Non-empty values must be absolute paths (Electron: user-provided vault roots).
     */
    fun normalizedCustomRoot(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return ""
        val path = runCatching { Path.of(trimmed).normalize() }.getOrNull() ?: return null
        if (!path.isAbsolute) return null
        return path.toString()
    }

    fun resolveCustomRoot(value: String): Path {
        val normalized = normalizedCustomRoot(value)
            ?: throw IllegalArgumentException("Vault directory must be an absolute path")
        return Path.of(normalized)
    }

    /** Invalid or relative configured paths fall back to the product default vault. */
    fun effectiveCustomRoot(value: String): String = normalizedCustomRoot(value) ?: ""
}
