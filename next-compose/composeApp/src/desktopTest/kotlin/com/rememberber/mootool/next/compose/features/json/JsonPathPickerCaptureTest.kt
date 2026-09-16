package com.rememberber.mootool.next.compose.features.json

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.app.AppPaths
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

/** JSONPath 选择器弹层 Compose 场景帧（非产品主窗）。 */
class JsonPathPickerCaptureTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun capturePathPickerUseButtonFocusRing() = runDesktopComposeUiTest(width = 800, height = 520) {
        val productRoot = createTempDirectory("mootool-json-path-picker-cap-")
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
        session.editor.setText("""{"a":1,"b":2}""", recordUndo = false)
        session.pathPickerOpen = true
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                Box(Modifier.fillMaxSize().background(colors.workspace)) {
                    JsonPathPickerDialog(container, session) {}
                }
            }
        }
        waitForIdle()
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val file = File(dir, "148-compose-json-path-picker-use-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(file.length() > 3_000)
        productRoot.toFile().deleteRecursively()
    }
}
