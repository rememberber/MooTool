package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppDirectories
import com.rememberber.mootool.next.compose.app.ProductIdentity
import com.rememberber.mootool.next.compose.domain.EditorFontSettings
import com.rememberber.mootool.next.compose.domain.NavigationToolVisibility
import com.rememberber.mootool.next.compose.domain.TranslationEngine
import com.rememberber.mootool.next.compose.ui.components.normalizeVaultTreeExpandMode
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
            val base = parsed.copy(schemaVersion = SETTINGS_SCHEMA_VERSION)
            val sanitized = sanitizeLoadedSettings(base)
            current = sanitized
            if (sanitized != base) {
                save(sanitized)
            }
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
        current = sanitizeLoadedSettings(settings.copy(schemaVersion = SETTINGS_SCHEMA_VERSION))
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

    private fun sanitizeLoadedSettings(settings: AppSettings): AppSettings {
        val vault = settings.vault
        val quickNotePath = VaultPathConfig.effectiveCustomRoot(vault.quickNotePath)
        val jsonPath = VaultPathConfig.effectiveCustomRoot(vault.jsonPath)
        val exportDirectory = VaultPathConfig.effectiveCustomRoot(settings.tools.exportDirectory)
        val dataDirectory = DataPathConfig.normalizedCustomDataRoot(settings.data.directory) ?: ""
        val hiddenNavigationToolIds =
            NavigationToolVisibility.normalizeHiddenNavigationToolIds(settings.layout.hiddenNavigationToolIds)
        val layout = settings.layout.copy(hiddenNavigationToolIds = hiddenNavigationToolIds)
        val jsonTreeExpandMode = normalizeVaultTreeExpandMode(vault.jsonTreeExpandMode)
        val quickNoteTreeExpandMode = normalizeVaultTreeExpandMode(vault.quickNoteTreeExpandMode)
        val translationLanguages = TranslationEngine.normalizeLanguagePair(
            settings.tools.translationSourceLang,
            settings.tools.translationTargetLang,
        )
        val tools = settings.tools.copy(
            exportDirectory = exportDirectory,
            translationSourceLang = translationLanguages.first,
            translationTargetLang = translationLanguages.second,
        )
        val editor = EditorFontSettings.normalizeEditorSettings(settings.editor, AppSettings.Default.editor)
        if (quickNotePath == vault.quickNotePath &&
            jsonPath == vault.jsonPath &&
            exportDirectory == settings.tools.exportDirectory &&
            dataDirectory == settings.data.directory &&
            layout == settings.layout &&
            jsonTreeExpandMode == vault.jsonTreeExpandMode &&
            quickNoteTreeExpandMode == vault.quickNoteTreeExpandMode &&
            tools == settings.tools &&
            editor == settings.editor
        ) {
            return settings
        }
        return settings.copy(
            layout = layout,
            data = settings.data.copy(directory = dataDirectory),
            editor = editor,
            vault = vault.copy(
                quickNotePath = quickNotePath,
                jsonPath = jsonPath,
                jsonTreeExpandMode = jsonTreeExpandMode,
                quickNoteTreeExpandMode = quickNoteTreeExpandMode,
            ),
            tools = tools,
        )
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
