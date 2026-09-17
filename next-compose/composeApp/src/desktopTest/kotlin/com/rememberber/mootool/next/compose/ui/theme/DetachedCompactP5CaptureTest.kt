package com.rememberber.mootool.next.compose.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.rememberber.mootool.next.compose.model.ThemePreference
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.workbench.LayoutPolicy
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/** 分离窗与主窗共用 `AppMooTheme` 时，紧凑导航应启用 p5 工具栏 dense 尺寸。 */
class DetachedCompactP5CaptureTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun compactNavigationRendersDenseP5ToolbarButton() = runDesktopComposeUiTest(width = 640, height = 200) {
        setContent {
            AppMooTheme(
                AppMooThemeInputs(
                    preference = ThemePreference.Light,
                    systemDark = false,
                    interfaceStyle = "modern",
                    accentColor = "blue",
                    unifiedBackground = true,
                    fontFamily = "system",
                    compactNavigation = true,
                )
            ) {
                val colors = MooTheme.colors
                Column(
                    Modifier.fillMaxSize().background(colors.workspace).mooToolbarBackground().padding(8.dp)
                ) {
                    MooButton("Run", p5Toolbar = true, onClick = {})
                }
            }
        }
        waitForIdle()
        assertTrue(LayoutPolicy.p5ToolbarDense(compactNavigation = true))
        val image = onRoot().captureToImage().toAwtImage()
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-tray-density/captures")
        dir.mkdirs()
        val file = File(dir, "detached-compact-p5-toolbar.png")
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(file.length() > 200)
    }
}
