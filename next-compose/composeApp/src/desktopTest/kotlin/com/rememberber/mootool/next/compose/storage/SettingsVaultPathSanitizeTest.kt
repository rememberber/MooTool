package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.model.CustomToolGroup
import com.rememberber.mootool.next.compose.model.ToolId
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsVaultPathSanitizeTest {
    @Test
    fun loadClearsRelativeVaultPaths() {
        val root = kotlin.io.path.createTempDirectory("mootool-settings-vault-")
        val directories = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val repository = SettingsRepository(directories)
        repository.save(
            AppSettings.Default.copy(
                vault = AppSettings.Default.vault.copy(
                    quickNotePath = "relative/quick",
                    jsonPath = "./json"
                ),
                tools = AppSettings.Default.tools.copy(exportDirectory = "exports/out")
            )
        )
        val loaded = SettingsRepository(directories).load()
        assertEquals("", loaded.vault.quickNotePath)
        assertEquals("", loaded.vault.jsonPath)
        assertEquals("", loaded.tools.exportDirectory)
        assertTrue(SettingsRepository(directories).load().vault.quickNotePath.isEmpty())
        assertTrue(directories.settingsFile.readText().contains("\"quickNotePath\": \"\""))
        root.toFile().deleteRecursively()
    }

    @Test
    fun loadNormalizesHiddenNavigationToolIds() {
        val root = kotlin.io.path.createTempDirectory("mootool-settings-nav-")
        val directories = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val repository = SettingsRepository(directories)
        repository.save(
            AppSettings.Default.copy(
                layout = AppSettings.Default.layout.copy(
                    hiddenNavigationToolIds = listOf("json", "json", ToolId.Mootool.id, "bogus", "qrCode")
                )
            )
        )
        val loaded = SettingsRepository(directories).load()
        assertEquals(listOf("json", "qrCode"), loaded.layout.hiddenNavigationToolIds)
        assertTrue(directories.settingsFile.readText().contains("\"hiddenNavigationToolIds\""))
        root.toFile().deleteRecursively()
    }

    @Test
    fun loadNormalizesUnknownVaultTreeExpandModes() {
        val root = kotlin.io.path.createTempDirectory("mootool-settings-vault-expand-")
        val directories = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val repository = SettingsRepository(directories)
        repository.save(
            AppSettings.Default.copy(
                vault = AppSettings.Default.vault.copy(
                    jsonTreeExpandMode = "unknown",
                    quickNoteTreeExpandMode = "bogus",
                )
            )
        )
        val loaded = SettingsRepository(directories).load()
        assertEquals("expandAll", loaded.vault.jsonTreeExpandMode)
        assertEquals("expandAll", loaded.vault.quickNoteTreeExpandMode)
        assertTrue(directories.settingsFile.readText().contains("\"jsonTreeExpandMode\": \"expandAll\""))
        root.toFile().deleteRecursively()
    }

    @Test
    fun loadNormalizesLegacyTranslationLanguageNames() {
        val root = kotlin.io.path.createTempDirectory("mootool-settings-translation-lang-")
        val directories = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val repository = SettingsRepository(directories)
        repository.save(
            AppSettings.Default.copy(
                tools = AppSettings.Default.tools.copy(
                    translationSourceLang = "English",
                    translationTargetLang = "英语",
                )
            )
        )
        val loaded = SettingsRepository(directories).load()
        assertEquals("auto", loaded.tools.translationSourceLang)
        assertEquals("en", loaded.tools.translationTargetLang)
        assertTrue(directories.settingsFile.readText().contains("\"translationTargetLang\": \"en\""))
        root.toFile().deleteRecursively()
    }

    @Test
    fun loadNormalizesEditorFontNames() {
        val root = kotlin.io.path.createTempDirectory("mootool-settings-editor-font-")
        val directories = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val repository = SettingsRepository(directories)
        repository.save(
            AppSettings.Default.copy(
                editor = AppSettings.Default.editor.copy(
                    jsonFontName = "  PingFang SC  ",
                    quickNoteFontName = "",
                )
            )
        )
        val loaded = SettingsRepository(directories).load()
        assertEquals("PingFang SC", loaded.editor.jsonFontName)
        assertEquals("ui-monospace", loaded.editor.quickNoteFontName)
        assertTrue(directories.settingsFile.readText().contains("\"jsonFontName\": \"PingFang SC\""))
        root.toFile().deleteRecursively()
    }

    @Test
    fun loadNormalizesNumericSettingBoundaries() {
        val root = kotlin.io.path.createTempDirectory("mootool-settings-numeric-")
        val directories = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val repository = SettingsRepository(directories)
        repository.save(
            AppSettings.Default.copy(
                appearance = AppSettings.Default.appearance.copy(fontSize = 200),
                editor = AppSettings.Default.editor.copy(jsonFontSize = 1),
                network = AppSettings.Default.network.copy(
                    requestTimeoutMs = 50,
                    translationTimeoutMs = 999_999,
                ),
                vault = AppSettings.Default.vault.copy(
                    autoCommitIdleSeconds = 1,
                    autoCommitInactiveSeconds = 999_999,
                ),
                tools = AppSettings.Default.tools.copy(
                    qrCodeSize = 12,
                    randomStringLength = 99_999,
                    translationProvider = "bing",
                ),
            )
        )
        val loaded = SettingsRepository(directories).load()
        assertEquals(18, loaded.appearance.fontSize)
        assertEquals(11, loaded.editor.jsonFontSize)
        assertEquals(120, loaded.tools.qrCodeSize)
        assertEquals(4_096, loaded.tools.randomStringLength)
        assertEquals(1_000, loaded.network.requestTimeoutMs)
        assertEquals(120_000, loaded.network.translationTimeoutMs)
        assertEquals(5, loaded.vault.autoCommitIdleSeconds)
        assertEquals(3_600, loaded.vault.autoCommitInactiveSeconds)
        assertEquals("bing", loaded.tools.translationProvider)
        root.toFile().deleteRecursively()
    }

    @Test
    fun loadNormalizesUnknownInterfaceStyleAndCustomGroups() {
        val root = kotlin.io.path.createTempDirectory("mootool-settings-layout-")
        val directories = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val repository = SettingsRepository(directories)
        repository.save(
            AppSettings.Default.copy(
                appearance = AppSettings.Default.appearance.copy(interfaceStyle = "unknown"),
                layout = AppSettings.Default.layout.copy(
                    navigationStyle = "bogus",
                    customGroups = listOf(
                        CustomToolGroup("daily", "  My tools  ", listOf("json", "mootool", "unknown")),
                        CustomToolGroup("daily", "Second", listOf("cron")),
                    ),
                ),
            )
        )
        val loaded = SettingsRepository(directories).load()
        assertEquals("modern", loaded.appearance.interfaceStyle)
        assertEquals("classic", loaded.layout.navigationStyle)
        assertEquals(
            listOf(
                CustomToolGroup("daily", "My tools", listOf("json")),
                CustomToolGroup("daily-2", "Second", listOf("cron")),
            ),
            loaded.layout.customGroups,
        )
        root.toFile().deleteRecursively()
    }

    @Test
    fun saveNormalizesHiddenNavigationToolIds() {
        val root = kotlin.io.path.createTempDirectory("mootool-settings-nav-save-")
        val directories = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val repository = SettingsRepository(directories)
        repository.save(
            AppSettings.Default.copy(
                layout = AppSettings.Default.layout.copy(
                    hiddenNavigationToolIds = listOf("json", "json", ToolId.Mootool.id, "bogus")
                )
            )
        )
        assertEquals(listOf("json"), repository.current.layout.hiddenNavigationToolIds)
        root.toFile().deleteRecursively()
    }
}
