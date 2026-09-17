package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.AppLanguage
import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.sessions.CodeRunSessionSnapshot
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ElectronNextSettingsImportTest {
    @Test
    fun loadsAndMergesElectronStoreWithoutSecrets() {
        val dir = createTempDirectory("electron-settings-")
        val store = dir.resolve("mootool-next.json")
        store.writeText(
            """
            {
              "settings": {
                "general": { "language": "en-US" },
                "network": { "proxyEnabled": true, "proxyPassword": "secret" },
                "vault": { "gitToken": "token", "gitRemote": "https://git.example/repo.git" }
              }
            }
            """.trimIndent()
        )
        val imported = ElectronNextSettingsImport.loadFromStore(store)
        assertEquals(AppLanguage.EnUS.code, imported?.general?.language)
        assertEquals("secret", imported?.network?.proxyPassword)
        assertEquals("token", imported?.vault?.gitToken)
        val merged = ElectronNextSettingsImport.mergeInto(AppSettings.Default, imported!!, retainSecrets = true)
        assertEquals(AppLanguage.EnUS.code, merged.general.language)
        assertEquals("https://git.example/repo.git", merged.vault.gitRemote)
        assertEquals("secret", merged.network.proxyPassword)
        assertEquals("token", merged.vault.gitToken)
    }

    @Test
    fun merge_retains_legacyMigrationHintDismissed() {
        val imported = AppSettings.Default.copy(
            general = AppSettings.Default.general.copy(legacyMigrationHintDismissed = true),
        )
        val merged = ElectronNextSettingsImport.mergeInto(AppSettings.Default, imported)
        assertTrue(merged.general.legacyMigrationHintDismissed)
    }

    @Test
    fun merge_oldElectronSchema_autoDismissesMigrationHint() {
        val imported = AppSettings.Default.copy(schemaVersion = 10)
        assertFalse(imported.general.legacyMigrationHintDismissed)
        val merged = ElectronNextSettingsImport.mergeInto(AppSettings.Default, imported)
        assertTrue(merged.general.legacyMigrationHintDismissed)
    }

    @Test
    fun merge_schema11_keepsExplicitMigrationHintFalse() {
        val imported = AppSettings.Default.copy(
            schemaVersion = 11,
            general = AppSettings.Default.general.copy(legacyMigrationHintDismissed = false),
        )
        val merged = ElectronNextSettingsImport.mergeInto(AppSettings.Default, imported)
        assertFalse(merged.general.legacyMigrationHintDismissed)
    }

    @Test
    fun detectsEncryptedSecretsBlobInElectronStore() {
        val dir = createTempDirectory("electron-secrets-")
        val store = dir.resolve("mootool-next.json")
        store.writeText(
            """
            {
              "settings": { "general": { "language": "zh-CN" } },
              "secrets": { "proxyPassword": "YmFzZTY0Y2lwaGVydGV4dA==", "gitToken": "" }
            }
            """.trimIndent()
        )
        assertTrue(ElectronNextSettingsImport.hasEncryptedSecretsInStore(store))
        val preview = CrossProductImporter.inspect(dir)
        assertTrue(preview.warnings.contains("electron:${ElectronNextSettingsImport.WARNING_ENCRYPTED_SECRETS_SKIPPED}"))
    }

    @Test
    fun stripsSecretsWhenEncryptedBlobPresent() {
        val dir = createTempDirectory("electron-secrets-plain-")
        val store = dir.resolve("mootool-next.json")
        store.writeText(
            """
            {
              "settings": {
                "network": { "proxyPassword": "plain" },
                "vault": { "gitToken": "plain-token" }
              },
              "secrets": { "proxyPassword": "ciphertext" }
            }
            """.trimIndent()
        )
        val imported = ElectronNextSettingsImport.loadFromStore(store)
        assertEquals("", imported?.network?.proxyPassword)
        assertEquals("", imported?.vault?.gitToken)
    }

    @Test
    fun emptySecretsObjectDoesNotWarn() {
        val dir = createTempDirectory("electron-secrets-empty-")
        val store = dir.resolve("mootool-next.json")
        store.writeText("""{"settings":{"general":{"language":"zh-CN"}},"secrets":{}}""")
        assertFalse(ElectronNextSettingsImport.hasEncryptedSecretsInStore(store))
    }

    @Test
    fun sanitize_normalizesLegacyTranslationLanguageNames() {
        val imported = AppSettings.Default.copy(
            tools = AppSettings.Default.tools.copy(
                translationSourceLang = "English",
                translationTargetLang = "英语",
            ),
        )
        val merged = ElectronNextSettingsImport.mergeInto(AppSettings.Default, imported)
        assertEquals("auto", merged.tools.translationSourceLang)
        assertEquals("en", merged.tools.translationTargetLang)
    }

    @Test
    fun merge_normalizesEditorFontNames() {
        val imported = AppSettings.Default.copy(
            editor = AppSettings.Default.editor.copy(
                jsonFontName = "  PingFang SC  ",
                quickNoteFontName = "",
            ),
        )
        val merged = ElectronNextSettingsImport.mergeInto(AppSettings.Default, imported)
        assertEquals("PingFang SC", merged.editor.jsonFontName)
        assertEquals("ui-monospace", merged.editor.quickNoteFontName)
    }

    @Test
    fun merge_normalizesUiFontFamily() {
        val imported = AppSettings.Default.copy(
            appearance = AppSettings.Default.appearance.copy(fontFamily = "  system-ui  "),
        )
        val merged = ElectronNextSettingsImport.mergeInto(AppSettings.Default, imported)
        assertEquals("system", merged.appearance.fontFamily)
    }

    @Test
    fun loadsRuntimeDraftsAndOptionsIntoCodeRunPatch() {
        val dir = createTempDirectory("electron-runtime-")
        val store = dir.resolve("mootool-next.json")
        store.writeText(
            """
            {
              "settings": {
                "runtime": {
                  "drafts": {
                    "java": "class Main {}",
                    "python": "print(42)"
                  },
                  "options": {
                    "java": { "arguments": "-ea", "workingDirectory": "/tmp/java" },
                    "node": { "arguments": "--inspect", "workingDirectory": "" }
                  }
                }
              }
            }
            """.trimIndent()
        )
        val patch = ElectronNextSettingsImport.loadCodeRunPatchFromStore(store)
        assertEquals("class Main {}", patch?.javaCode)
        assertEquals("print(42)", patch?.pythonCode)
        assertEquals("-ea", patch?.javaArguments)
        assertEquals("/tmp/java", patch?.javaWorkingDirectory)
        assertEquals("--inspect", patch?.nodeArguments)
        val merged = ElectronNextSettingsImport.mergeCodeRunSnapshots(CodeRunSessionSnapshot(), patch!!)
        assertEquals("class Main {}", merged.javaCode)
        assertEquals("print(42)", merged.pythonCode)
        assertTrue(ElectronNextSettingsImport.hasCodeRunPatch(patch))
    }

    @Test
    fun loadCodeRunPatchTruncatesOversizedOptionsLikeElectronNormalize() {
        val dir = createTempDirectory("electron-runtime-truncate-")
        val store = dir.resolve("mootool-next.json")
        val longArgs = "x".repeat(CodeRunRuntimeOptionsNormalize.MAX_ARGUMENTS_CHARS + 10)
        val longDir = "/w/" + "y".repeat(CodeRunRuntimeOptionsNormalize.MAX_WORKING_DIRECTORY_CHARS)
        store.writeText(
            """
            {
              "settings": {
                "runtime": {
                  "options": {
                    "java": { "arguments": "$longArgs", "workingDirectory": "$longDir" }
                  }
                }
              }
            }
            """.trimIndent()
        )
        val patch = ElectronNextSettingsImport.loadCodeRunPatchFromStore(store)!!
        assertEquals(CodeRunRuntimeOptionsNormalize.MAX_ARGUMENTS_CHARS, patch.javaArguments.length)
        assertEquals(CodeRunRuntimeOptionsNormalize.MAX_WORKING_DIRECTORY_CHARS, patch.javaWorkingDirectory.length)
    }
}
