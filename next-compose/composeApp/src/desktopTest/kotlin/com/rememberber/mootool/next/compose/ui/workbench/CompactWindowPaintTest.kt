package com.rememberber.mootool.next.compose.ui.workbench

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.unit.dp
import com.rememberber.mootool.next.compose.model.ThemePreference
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.mooToolbarBackground
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import org.junit.Assume
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertTrue

class CompactWindowPaintTest {
    @Test
    fun jframePaint960ChromeIsNotUserWindowScreenshot() {
        Assume.assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance)
        lateinit var frame: JFrame
        SwingUtilities.invokeAndWait {
            frame = JFrame("next-compose-compact-960")
            val panel = ComposePanel()
            panel.setContent {
                MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                    val colors = MooTheme.colors
                    val collapsed = LayoutPolicy.collapseNavigation(959f, false, false)
                    Column(Modifier.fillMaxSize().background(colors.workspace)) {
                        Row(Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).mooToolbarBackground()) {
                            MooButton("JSON", onClick = {}, prominent = true)
                            MooButton("JSON Vault", onClick = {})
                            MooButton("More tools", onClick = {})
                        }
                        Row(Modifier.weight(1f).fillMaxWidth()) {
                            Box(Modifier.width(if (collapsed) 84.dp else 248.dp).fillMaxHeight().background(colors.sidebar))
                            Box(
                                Modifier.weight(1f).fillMaxHeight().background(colors.workspace).border(2.dp, colors.focusRing)
                            )
                        }
                    }
                }
            }
            frame.contentPane.add(panel)
            frame.size = Dimension(960, 640)
            frame.isVisible = true
        }
        try {
            val image = BufferedImage(960, 640, BufferedImage.TYPE_INT_RGB)
            SwingUtilities.invokeAndWait { frame.paint(image.graphics) }
            val cwd = File(".").canonicalFile
            val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
            val dir = File(root, "docs/evidence/2026-09-15-compact-font-shortcuts/captures")
            dir.mkdirs()
            val file = File(dir, "jframe-paint-960.png")
            assertTrue(ImageIO.write(image, "png", file))
            assertTrue(file.length() > 200)
            assertTrue(image.width == 960)
            assertTrue(image.height == 640)
        } finally {
            SwingUtilities.invokeAndWait { frame.dispose() }
        }
    }
}
