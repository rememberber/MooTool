package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppDirectories
import com.rememberber.mootool.next.compose.app.ProductIdentity
import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.model.SETTINGS_SCHEMA_VERSION
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText

class SettingsRepository(
    private val directories: AppDirectories,
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {
    @Volatile
    var current: AppSettings = AppSettings.Default
        private set

    @Volatile
    var loadError: String? = null
        private set

    fun load(): AppSettings {
        val file = directories.settingsFile
        if (!file.exists()) {
            current = AppSettings.Default
            save(current)
            writeProductMarker()
            return current
        }
        return try {
            val parsed = json.decodeFromString<AppSettings>(file.readText())
            current = parsed.copy(schemaVersion = SETTINGS_SCHEMA_VERSION)
            loadError = null
            current
        } catch (error: Exception) {
            val backup = file.resolveSibling("settings.corrupt-${System.currentTimeMillis()}.json")
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING)
            loadError = "Failed to read settings: ${error.message}. Backup: $backup"
            current = AppSettings.Default
            current
        }
    }

    fun save(settings: AppSettings) {
        current = settings.copy(schemaVersion = SETTINGS_SCHEMA_VERSION)
        atomicWrite(directories.settingsFile, json.encodeToString(current))
        writeProductMarker()
    }

    fun update(transform: (AppSettings) -> AppSettings): AppSettings {
        val next = transform(current)
        save(next)
        return current
    }

    private fun writeProductMarker() {
        val payload = """
            {
              "productId": "${ProductIdentity.PRODUCT_ID}",
              "schemaVersion": ${ProductIdentity.SCHEMA_VERSION},
              "appVersion": "${ProductIdentity.VERSION}"
            }
        """.trimIndent()
        atomicWrite(directories.productMarker, payload)
    }

    companion object {
        fun atomicWrite(file: Path, content: String) {
            file.parent.createDirectories()
            val temp = file.resolveSibling("${file.fileName}.tmp")
            Files.writeString(
                temp,
                content,
                Charsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            )
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }
    }
}
