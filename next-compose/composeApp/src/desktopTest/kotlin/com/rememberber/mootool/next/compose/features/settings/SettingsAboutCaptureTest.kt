package com.rememberber.mootool.next.compose.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.app.UpdateUiState
import com.rememberber.mootool.next.compose.domain.UpdateCheckResult
import com.rememberber.mootool.next.compose.domain.UpdateCheckStatus
import com.rememberber.mootool.next.compose.domain.UpdateDownload
import com.rememberber.mootool.next.compose.model.ThemePreference
import com.rememberber.mootool.next.compose.storage.AppDatabase
import com.rememberber.mootool.next.compose.storage.HistoryRepository
import com.rememberber.mootool.next.compose.storage.LegacyMigrationRowRepository
import com.rememberber.mootool.next.compose.storage.SessionStore
import com.rememberber.mootool.next.compose.storage.SettingsRepository
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertTrue

/** 设置 · 关于页 `settings-update-result` Compose 回归帧（非产品主窗）。 */
class SettingsAboutCaptureTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureAboutUpdateCheckButtonFocusRing() = runDesktopComposeUiTest(width = 720, height = 520) {
        val checkFocus = FocusRequester()
        val productRoot = createTempDirectory("mootool-settings-about-ui-")
        val directories = AppPaths.resolve(productRoot.toString()).also { it.ensureCreated() }
        val settingsRepo = SettingsRepository(directories).also { it.load() }
        val database = AppDatabase(directories)
        val container = AppContainer(
            directories = directories,
            settingsRepository = settingsRepo,
            database = database,
            history = HistoryRepository(database),
            migrationRows = LegacyMigrationRowRepository(database),
            sessions = SessionStore(database),
        )
        val settings = settingsRepo.current
        val update = UpdateUiState(
            result = UpdateCheckResult(
                status = UpdateCheckStatus.Available,
                productId = "next-compose",
                productName = "MooTool",
                currentVersion = "0.1.0",
                latestVersion = "0.2.0",
                releaseUrl = "https://example.com/release",
                releaseNotes = "Line one\nLine two",
                platform = "macos",
                architecture = "aarch64",
                download = UpdateDownload(
                    fileName = "MooTool-next-compose-0.2.0-macos-aarch64.dmg",
                    url = "https://example.com/pkg.dmg",
                    sha512 = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==",
                    size = 1_024,
                ),
            ),
            status = "available",
        )
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                Box(Modifier.fillMaxSize().background(colors.workspace).padding(16.dp)) {
                    SettingsAboutPanel(
                        container = container,
                        settings = settings,
                        update = update,
                        checkButtonModifier = Modifier.focusRequester(checkFocus),
                    )
                }
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val expected = 0x316DC0
        runOnIdle { checkFocus.requestFocus() }
        waitForIdle()
        val file = File(dir, "152-compose-settings-about-update-check-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "settings about update check focus ring")
        productRoot.toFile().deleteRecursively()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureAboutUpdateDownloadButtonFocusRing() = runDesktopComposeUiTest(width = 720, height = 520) {
        val downloadFocus = FocusRequester()
        val productRoot = createTempDirectory("mootool-settings-about-download-ui-")
        val directories = AppPaths.resolve(productRoot.toString()).also { it.ensureCreated() }
        val settingsRepo = SettingsRepository(directories).also { it.load() }
        val database = AppDatabase(directories)
        val container = AppContainer(
            directories = directories,
            settingsRepository = settingsRepo,
            database = database,
            history = HistoryRepository(database),
            migrationRows = LegacyMigrationRowRepository(database),
            sessions = SessionStore(database),
        )
        val settings = settingsRepo.current
        val update = UpdateUiState(
            result = UpdateCheckResult(
                status = UpdateCheckStatus.Available,
                productId = "next-compose",
                productName = "MooTool",
                currentVersion = "0.1.0",
                latestVersion = "0.2.0",
                releaseUrl = "https://example.com/release",
                releaseNotes = "Notes",
                platform = "macos",
                architecture = "aarch64",
                download = UpdateDownload(
                    fileName = "MooTool-next-compose-0.2.0-macos-aarch64.dmg",
                    url = "https://example.com/pkg.dmg",
                    sha512 = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==",
                    size = 1_024,
                ),
            ),
            status = "available",
        )
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                Box(Modifier.fillMaxSize().background(colors.workspace).padding(16.dp)) {
                    SettingsAboutPanel(
                        container = container,
                        settings = settings,
                        update = update,
                        downloadButtonModifier = Modifier.focusRequester(downloadFocus),
                    )
                }
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val expected = 0x316DC0
        runOnIdle { downloadFocus.requestFocus() }
        waitForIdle()
        val file = File(dir, "155-compose-settings-about-update-download-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "settings about update download focus ring")
        productRoot.toFile().deleteRecursively()
    }

    private fun countRingPixels(image: java.awt.image.BufferedImage, rgb: Int): Int {
        var hits = 0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if (image.getRGB(x, y) and 0x00FFFFFF == rgb) hits++
            }
        }
        return hits
    }
}
