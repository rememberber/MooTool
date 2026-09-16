package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppDirectories
import java.nio.file.Path
import kotlin.io.path.createDirectories

object DataPathConfig {
    /** Empty keeps the product default `dataRoot`; non-empty must be absolute (Electron `settings.data.directory`). */
    fun normalizedCustomDataRoot(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return ""
        val path = runCatching { Path.of(trimmed).normalize() }.getOrNull() ?: return null
        if (!path.isAbsolute) return null
        return path.toString()
    }

    fun effectiveDataRoot(defaultDataRoot: Path, configured: String): Path {
        val normalized = normalizedCustomDataRoot(configured)
        if (normalized == null || normalized.isEmpty()) return defaultDataRoot
        return Path.of(normalized)
    }

    fun withEffectiveDataRoot(bootstrap: AppDirectories, configuredDataDirectory: String): AppDirectories {
        val dataRoot = effectiveDataRoot(bootstrap.dataRoot, configuredDataDirectory)
        if (dataRoot == bootstrap.dataRoot) return bootstrap
        return bootstrap.copy(dataRoot = dataRoot)
    }

    fun ensureDataRootExists(directories: AppDirectories) {
        directories.dataRoot.createDirectories()
    }
}
