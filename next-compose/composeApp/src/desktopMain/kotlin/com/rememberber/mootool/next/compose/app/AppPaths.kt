package com.rememberber.mootool.next.compose.app

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isWritable
import kotlin.io.path.pathString

data class AppDirectories(
    val configRoot: Path,
    val dataRoot: Path,
    val cacheRoot: Path,
    val logRoot: Path
) {
    val settingsFile: Path get() = configRoot.resolve("config").resolve(ProductIdentity.SETTINGS_FILE)
    val bootstrapFile: Path get() = configRoot.resolve("config").resolve("bootstrap.json")
    val productMarker: Path get() = dataRoot.resolve(ProductIdentity.PRODUCT_MARKER)
    val databaseFile: Path get() = dataRoot.resolve(ProductIdentity.DATABASE_NAME)
    val jsonVault: Path get() = dataRoot.resolve("vaults").resolve("json")
    val quickNoteVault: Path get() = dataRoot.resolve("vaults").resolve("quick-note")
    val drafts: Path get() = dataRoot.resolve("drafts")
    val backups: Path get() = dataRoot.resolve("backups")
    val lockFile: Path get() = configRoot.resolve(ProductIdentity.LOCK_NAME)

    fun ensureCreated() {
        listOf(
            configRoot.resolve("config"),
            dataRoot,
            jsonVault,
            quickNoteVault,
            drafts,
            backups,
            cacheRoot,
            logRoot
        ).forEach { it.createDirectories() }
    }
}

object AppPaths {
    fun resolve(overrideRoot: String? = System.getenv(ProductIdentity.DATA_DIR_ENV)): AppDirectories {
        val override = overrideRoot?.trim()?.takeIf { it.isNotEmpty() }
        if (override != null) {
            val root = Path.of(override).toAbsolutePath().normalize()
            requireWritableDirectory(root)
            return AppDirectories(
                configRoot = root,
                dataRoot = root.resolve("data"),
                cacheRoot = root.resolve("cache"),
                logRoot = root.resolve("logs")
            )
        }
        val os = System.getProperty("os.name").orEmpty().lowercase()
        val home = Path.of(System.getProperty("user.home"))
        return when {
            os.contains("mac") -> macDirectories(home)
            os.contains("win") -> windowsDirectories()
            else -> linuxDirectories(home)
        }
    }

    fun display(path: Path): String = path.absolute().normalize().pathString

    private fun macDirectories(home: Path): AppDirectories {
        val support = home.resolve("Library/Application Support").resolve(ProductIdentity.APPLICATION_ID)
        return AppDirectories(
            configRoot = support,
            dataRoot = support.resolve("data"),
            cacheRoot = home.resolve("Library/Caches").resolve(ProductIdentity.APPLICATION_ID),
            logRoot = home.resolve("Library/Logs/MooToolNextCompose")
        )
    }

    private fun windowsDirectories(): AppDirectories {
        val appData = envPath("APPDATA") ?: Path.of(System.getProperty("user.home"), "AppData", "Roaming")
        val local = envPath("LOCALAPPDATA") ?: Path.of(System.getProperty("user.home"), "AppData", "Local")
        val root = appData.resolve("MooToolNextCompose")
        return AppDirectories(
            configRoot = root,
            dataRoot = root.resolve("data"),
            cacheRoot = local.resolve("MooToolNextCompose").resolve("cache"),
            logRoot = local.resolve("MooToolNextCompose").resolve("logs")
        )
    }

    private fun linuxDirectories(home: Path): AppDirectories {
        val config = xdg("XDG_CONFIG_HOME", home.resolve(".config")).resolve(ProductIdentity.LINUX_PACKAGE)
        val data = xdg("XDG_DATA_HOME", home.resolve(".local/share")).resolve(ProductIdentity.LINUX_PACKAGE)
        val cache = xdg("XDG_CACHE_HOME", home.resolve(".cache")).resolve(ProductIdentity.LINUX_PACKAGE)
        val state = xdg("XDG_STATE_HOME", home.resolve(".local/state")).resolve(ProductIdentity.LINUX_PACKAGE)
        return AppDirectories(config, data, cache, state)
    }

    private fun xdg(name: String, fallback: Path): Path = envPath(name) ?: fallback

    private fun envPath(name: String): Path? {
        val value = System.getenv(name)?.trim().orEmpty()
        if (value.isEmpty()) return null
        val path = Path.of(value)
        return if (path.isAbsolute) path else null
    }

    private fun requireWritableDirectory(root: Path) {
        if (!root.exists()) {
            root.createDirectories()
        }
        check(Files.isDirectory(root)) { "Data directory is not a directory: $root" }
        check(root.isWritable()) { "Data directory is not writable: $root" }
    }
}
