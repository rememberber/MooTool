package com.rememberber.mootool.next.compose.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.model.ThemePreference
import com.rememberber.mootool.next.compose.ui.components.mooToolShell
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/** Non-p5 tool pages: modern/quiet default `mooToolShell` flatten (DIFF-508). */
class ModernToolShellCaptureTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureModernFlattenedEncodeStylePanels() = runDesktopComposeUiTest(width = 520, height = 120) {
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                Row(Modifier.fillMaxSize().background(colors.workspace).padding(12.dp)) {
                    Box(
                        Modifier
                            .width(160.dp)
                            .fillMaxHeight()
                            .mooToolShell()
                            .padding(8.dp),
                    ) {
                        Text("输入", color = colors.textMuted, fontSize = 11.sp)
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .mooToolShell(endBorder = false)
                            .padding(8.dp),
                    ) {
                        Text("输出", color = colors.textMuted, fontSize = 11.sp)
                    }
                }
            }
        }
        waitForIdle()
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val file = File(dir, "150-compose-modern-flatten-tool-shell.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(file.length() > 200)
        val workspace = 0xFFFFFF
        var workspaceHits = 0
        for (y in 20 until minOf(image.height, 100)) {
            for (x in 180 until minOf(image.width, 500)) {
                if (image.getRGB(x, y) and 0x00FFFFFF == workspace) workspaceHits++
            }
        }
        assertTrue(workspaceHits > 400, "flattened panels should read as workspace, hits=$workspaceHits")
    }
}
