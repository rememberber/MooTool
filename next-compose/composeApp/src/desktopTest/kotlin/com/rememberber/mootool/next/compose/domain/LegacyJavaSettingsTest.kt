package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.model.AppLanguage
import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.sessions.SessionManager
import com.rememberber.mootool.next.compose.storage.AppDatabase
import com.rememberber.mootool.next.compose.storage.HostProfileStore
import com.rememberber.mootool.next.compose.storage.SessionStore
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LegacyJavaSettingsTest {
    @Test
    fun applyVaultRootsAfterRelativeJavaConfigUsesComposeRoots() {
        val qRoot = createTempDirectory("compose-quick-root-")
        val jRoot = createTempDirectory("compose-json-root-")
        val config = LegacyJavaSettings.parse(
            """
            [func.quickNote]
            quickNoteVaultPath=relative-notes
            [func.jsonBeauty]
            jsonBeautyVaultPath=json-beauty
            """.trimIndent()
        )
        val patched = LegacyJavaSettings.applyVaultRootsAfterRelativeJavaConfig(
            AppSettings.Default,
            config,
            qRoot,
            jRoot,
            importedNotes = 2,
            importedJson = 1,
        )
        assertEquals(qRoot.toString(), patched.vault.quickNotePath)
        assertEquals(jRoot.toString(), patched.vault.jsonPath)
        qRoot.toFile().deleteRecursively()
        jRoot.toFile().deleteRecursively()
    }

    @Test
    fun applyVaultRootsAfterRelativeSkipsWhenAbsolutePathAlreadyPatched() {
        val root = createTempDirectory("compose-abs-vault-")
        val abs = root.resolve("custom-vault").also { it.toFile().mkdirs() }
        val qRoot = root.resolve("product-quick")
        val config = LegacyJavaSettings.parse(
            """
            [func.quickNote]
            quickNoteVaultPath=${abs}
            """.trimIndent()
        )
        val patched = LegacyJavaSettings.applyPatch(AppSettings.Default, config)
        val after = LegacyJavaSettings.applyVaultRootsAfterRelativeJavaConfig(
            patched,
            config,
            qRoot,
            qRoot,
            importedNotes = 5,
            importedJson = 0,
        )
        assertEquals(abs.toString(), after.vault.quickNotePath)
        root.toFile().deleteRecursively()
    }

    @Test
    fun applyPatchMapsAbsoluteVaultPathsAndIgnoresRelative() {
        val root = createTempDirectory("legacy-java-vault-paths-")
        val quick = root.resolve("custom-quick").also { it.toFile().mkdirs() }
        val json = root.resolve("custom-json").also { it.toFile().mkdirs() }
        val absoluteConfig = LegacyJavaSettings.parse(
            """
            [func.quickNote]
            quickNoteVaultPath=${quick}
            [func.jsonBeauty]
            jsonBeautyVaultPath=${json}
            """.trimIndent()
        )
        val patched = LegacyJavaSettings.applyPatch(AppSettings.Default, absoluteConfig)
        assertEquals(quick.toString(), patched.vault.quickNotePath)
        assertEquals(json.toString(), patched.vault.jsonPath)

        val relativeConfig = LegacyJavaSettings.parse(
            """
            [func.quickNote]
            quickNoteVaultPath=relative-notes
            """.trimIndent()
        )
        assertEquals("", LegacyJavaSettings.applyPatch(AppSettings.Default, relativeConfig).vault.quickNotePath)
        root.toFile().deleteRecursively()
    }

    @Test
    fun applyPatchMapsNavigationStyleAndAccentColor() {
        val cardConfig = LegacyJavaSettings.parse(
            """
            [setting.custom]
            funcTabGrouped=false
            tabCard=true
            [setting.quickNote]
            accentColor=Moo.accent.mooRed
            """.trimIndent()
        )
        val cardPatch = LegacyJavaSettings.applyPatch(AppSettings.Default, cardConfig)
        assertEquals("card", cardPatch.layout.navigationStyle)
        assertEquals("red", cardPatch.appearance.accentColor)

        val groupedConfig = LegacyJavaSettings.parse(
            """
            [setting.custom]
            funcTabGrouped=true
            """.trimIndent()
        )
        assertEquals("grouped", LegacyJavaSettings.applyPatch(AppSettings.Default, groupedConfig).layout.navigationStyle)
    }

    @Test
    fun parsesIniGroupsAndAppliesLanguageAndLayout() {
        val raw = """
            [setting.common]
            locale=en_US
            autoDownloadUpdate=false
            [setting.custom]
            tabCompact=true
        """.trimIndent()
        val config = LegacyJavaSettings.parse(raw)
        val patched = LegacyJavaSettings.applyPatch(AppSettings.Default, config)
        assertEquals(AppLanguage.EnUS.code, patched.general.language)
        assertTrue(patched.layout.compactNavigation)
        assertEquals(false, patched.general.autoDownloadUpdates)
    }

    @Test
    fun warnsWhenVaultRemotesDiffer() {
        val config = LegacyJavaSettings.parse(
            """
            [func.quickNote]
            quickNoteGitRemoteUrl=https://a.git
            [func.jsonBeauty]
            jsonBeautyGitRemoteUrl=https://b.git
            """.trimIndent()
        )
        assertTrue(LegacyJavaSettings.WARNING_DIFFERENT_VAULT_REMOTES in LegacyJavaSettings.warningCodes(config))
        val patched = LegacyJavaSettings.applyPatch(AppSettings.Default, config)
        assertEquals("", patched.vault.gitRemote)
    }

    @Test
    fun applySessionPatchesRegexPatternAndCalculatorExpression() {
        val config = LegacyJavaSettings.parse(
            """
            [func.regex]
            regexText=^legacy$
            [func.calculator]
            calculatorInputExpress=9*9
            """.trimIndent()
        )
        val root = createTempDirectory("legacy-java-session-")
        val paths = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val database = AppDatabase(paths)
        val sessions = SessionManager(SessionStore(database))
        assertEquals(2, LegacyJavaSettings.applySessionPatches(sessions, config))
        assertEquals("^legacy$", sessions.regexSession().pattern)
        assertEquals("9*9", sessions.calculatorSession().expression)
        database.close()
        root.toFile().deleteRecursively()
    }

    @Test
    fun applySessionPatchesSelectsHostProfileByJavaCurrentName() {
        val config = LegacyJavaSettings.parse(
            """
            [func.host]
            currentHostName=Work VPN
            """.trimIndent()
        )
        val root = createTempDirectory("legacy-java-host-")
        val paths = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val database = AppDatabase(paths)
        val sessions = SessionManager(SessionStore(database))
        val hosts = HostProfileStore(paths)
        val saved = hosts.save(null, "Work VPN", "127.0.0.1 localhost")
        assertEquals(1, LegacyJavaSettings.applySessionPatches(sessions, config, hosts))
        assertEquals(saved.id, sessions.hostSession().selectedId)
        assertEquals("Work VPN", sessions.hostSession().name)
        assertTrue(sessions.hostSession().content.contains("127.0.0.1"))
        database.close()
        root.toFile().deleteRecursively()
    }

    @Test
    fun applySessionPatchesRestoresQrLogoAndRecognitionPathsWhenFilesExist() {
        val root = createTempDirectory("legacy-java-qr-paths-")
        val logo = root.resolve("logo.png")
        logo.writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00))
        val scan = root.resolve("scan.png")
        scan.writeBytes(byteArrayOf(1, 2, 3))
        val config = LegacyJavaSettings.parse(
            """
            [func.qrCode]
            qrCodeLogoPath=${logo}
            qrCodeRecognitionImagePath=${scan}
            """.trimIndent()
        )
        val paths = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val database = AppDatabase(paths)
        val sessions = SessionManager(SessionStore(database))
        assertEquals(2, LegacyJavaSettings.applySessionPatches(sessions, config))
        assertEquals(logo.toString(), sessions.qrSession().logoPath)
        assertEquals("logo.png", sessions.qrSession().logoName)
        assertEquals("scan.png", sessions.qrSession().recognitionName)
        assertEquals(byteArrayOf(1, 2, 3).toList(), sessions.qrSession().recognitionBytes?.toList())
        database.close()
        root.toFile().deleteRecursively()
    }

    @Test
    fun applyPatchUsesQrSaveAsExportDirectory() {
        val root = createTempDirectory("legacy-java-qr-save-")
        val saveDir = root.resolve("qr-out").also { it.toFile().mkdirs() }
        val config = LegacyJavaSettings.parse(
            """
            [func.qrCode]
            qrCodeSaveAsPath=${saveDir}
            """.trimIndent()
        )
        val patched = LegacyJavaSettings.applyPatch(AppSettings.Default, config)
        assertEquals(saveDir.toString(), patched.tools.exportDirectory)
        root.toFile().deleteRecursively()
    }

    @Test
    fun applyPatchMapsHttpTimeoutAndHostExportDirectory() {
        val root = createTempDirectory("legacy-java-http-export-")
        val exportDir = root.resolve("host-out").also { it.toFile().mkdirs() }
        val config = LegacyJavaSettings.parse(
            """
            [setting.http]
            httpTimeoutMs=45000
            [func.host]
            hostExportPath=${exportDir}
            """.trimIndent()
        )
        val patched = LegacyJavaSettings.applyPatch(AppSettings.Default, config)
        assertEquals(45_000, patched.network.requestTimeoutMs)
        assertEquals(exportDir.toString(), patched.tools.exportDirectory)
        root.toFile().deleteRecursively()
    }

    @Test
    fun applySessionPatchesMapsHttpTimeoutToHttpSession() {
        val config = LegacyJavaSettings.parse(
            """
            [setting.http]
            httpTimeoutMs=60000
            """.trimIndent()
        )
        val root = createTempDirectory("legacy-java-http-session-")
        val paths = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val database = AppDatabase(paths)
        val sessions = SessionManager(SessionStore(database))
        assertEquals(1, LegacyJavaSettings.applySessionPatches(sessions, config))
        assertEquals(60_000, sessions.httpSession().timeoutMs)
        database.close()
        root.toFile().deleteRecursively()
    }

    @Test
    fun applyPatchMapsVaultTreeExpandModes() {
        val config = LegacyJavaSettings.parse(
            """
            [func.quickNote]
            quickNoteTreeExpandMode=COLLAPSE_ALL
            [func.jsonBeauty]
            jsonBeautyTreeExpandMode=EXPAND_ALL
            """.trimIndent()
        )
        val patched = LegacyJavaSettings.applyPatch(AppSettings.Default, config)
        assertEquals("collapseAll", patched.vault.quickNoteTreeExpandMode)
        assertEquals("expandAll", patched.vault.jsonTreeExpandMode)
    }

    @Test
    fun applySessionPatchesMapsVaultListSortModes() {
        val config = LegacyJavaSettings.parse(
            """
            [func.quickNote]
            quickNoteListSortMode=CREATE_TIME
            [func.jsonBeauty]
            jsonBeautyListSortMode=NAME
            """.trimIndent()
        )
        val root = createTempDirectory("legacy-java-vault-sort-")
        val paths = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val database = AppDatabase(paths)
        val sessions = SessionManager(SessionStore(database))
        assertEquals(2, LegacyJavaSettings.applySessionPatches(sessions, config))
        assertEquals(VaultSort.CREATED, sessions.quickNoteSession().vaultSort)
        assertEquals(VaultSort.NAME, sessions.jsonSession().vaultSort)
        database.close()
        root.toFile().deleteRecursively()
    }

    @Test
    fun applySessionPatchesRestoresColorBoardSession() {
        val config = LegacyJavaSettings.parse(
            """
            [func.colorBoard]
            lastSelectedColor=007AAE
            colorTheme=主题2
            colorCodeType=html
            """.trimIndent()
        )
        val root = createTempDirectory("legacy-java-color-")
        val paths = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val database = AppDatabase(paths)
        val sessions = SessionManager(SessionStore(database))
        assertEquals(1, LegacyJavaSettings.applySessionPatches(sessions, config))
        val color = sessions.colorSession()
        assertEquals(ColorThemeId.Theme2, color.theme)
        assertEquals(ColorFormat.HEX_LOWER, color.format)
        assertEquals("#007aae", color.code)
        assertEquals(0x00, color.primary.r)
        assertEquals(0x7A, color.primary.g)
        assertEquals(0xAE, color.primary.b)
        database.close()
        root.toFile().deleteRecursively()
    }

    @Test
    fun applySessionPatchesRestoresCryptoDigestFileAndRandomLengths() {
        val root = createTempDirectory("legacy-java-crypto-")
        val digestFile = root.resolve("sample.txt")
        digestFile.writeBytes("abc".encodeToByteArray())
        val config = LegacyJavaSettings.parse(
            """
            [func.crypto]
            digestFilePath=${digestFile}
            randomNumDigit=20
            randomPasswordDigit=12
            """.trimIndent()
        )
        val paths = AppPaths.resolve(root.toString()).also { it.ensureCreated() }
        val database = AppDatabase(paths)
        val sessions = SessionManager(SessionStore(database))
        assertEquals(2, LegacyJavaSettings.applySessionPatches(sessions, config))
        val crypto = sessions.cryptoSession()
        assertEquals("sample.txt", crypto.digestFileName)
        assertEquals(CryptoEngine.digestFile(crypto.digestAlgorithm, digestFile), crypto.digestOutput)
        assertEquals(20, crypto.randomLength)
        database.close()
        root.toFile().deleteRecursively()
    }
}
