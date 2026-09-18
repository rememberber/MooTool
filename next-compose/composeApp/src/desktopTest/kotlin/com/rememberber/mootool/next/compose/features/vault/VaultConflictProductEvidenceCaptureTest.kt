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
import com.rememberber.mootool.next.compose.domain.VaultConflictProductEvidencePresentation
import com.rememberber.mootool.next.compose.domain.VaultConflictState
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

/** §A 证据脚本 `sample.json` 外部冲突叠层「重新加载」钮 Compose 焦点帧（非产品主窗）。 */
class VaultConflictProductEvidenceCaptureTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureEvidenceSampleJsonConflictReloadTabFocus() = runDesktopComposeUiTest(width = 680, height = 460) {
        val reloadFocus = FocusRequester()
        val productRoot = createTempDirectory("mootool-vault-evidence-capture-")
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
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                Box(Modifier.fillMaxSize().background(colors.workspace)) {
                    VaultConflictDialog(
                        container = container,
                        conflict = VaultConflictState(
                            relativePath = VaultConflictProductEvidencePresentation.EVIDENCE_JSON_RELATIVE_PATH,
                            editorText = VaultConflictProductEvidencePresentation.evidenceJsonInitialBody,
                            diskText = VaultConflictProductEvidencePresentation.EVIDENCE_JSON_DISK_REWRITE,
                            deleted = false,
                        ),
                        onReload = {},
                        onSaveCopy = {},
                        onKeep = {},
                        reloadButtonModifier = Modifier.focusRequester(reloadFocus),
                    )
                }
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val expected = 0x316DC0
        runOnIdle { reloadFocus.requestFocus() }
        waitForIdle()
        val file = File(dir, "200-compose-vault-external-conflict-sample-reload-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "vault evidence sample reload focus ring")
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
