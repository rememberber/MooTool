package com.rememberber.mootool.next.compose.features.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.domain.VaultConflictState
import com.rememberber.mootool.next.compose.model.ThemePreference
import com.rememberber.mootool.next.compose.sessions.JsonSession
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

/** [JsonVaultConflictOverlay] 完整回调链上的 Compose 焦点帧（非产品主窗）。 */
class VaultConflictOverlayCaptureTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureJsonOverlayKeepButtonFocusRing() = runDesktopComposeUiTest(width = 720, height = 480) {
        val keepFocus = FocusRequester()
        val productRoot = createTempDirectory("mootool-json-overlay-capture-")
        val directories = AppPaths.resolve(productRoot.toString()).also { it.ensureCreated() }
        val settings = SettingsRepository(directories).also { it.load() }
        val database = AppDatabase(directories)
        val container = AppContainer(
            directories = directories,
            settingsRepository = settings,
            database = database,
            history = HistoryRepository(database),
            migrationRows = LegacyMigrationRowRepository(database),
            sessions = SessionStore(database),
        )
        val session = JsonSession()
        session.vaultConflict = VaultConflictState(
            relativePath = "vault/doc.json",
            editorText = """{"local":true}""",
            diskText = """{"disk":true}""",
            deleted = false,
        )
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                Box(Modifier.fillMaxSize().background(colors.workspace)) {
                    JsonVaultConflictOverlay(
                        container = container,
                        session = session,
                        monitor = null,
                        onRefresh = {},
                        keepButtonModifier = Modifier.focusRequester(keepFocus),
                    )
                }
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val expected = 0x316DC0
        runOnIdle { keepFocus.requestFocus() }
        waitForIdle()
        val file = File(dir, "147-compose-json-vault-conflict-overlay-keep-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "json vault overlay keep focus ring")
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
