package com.rememberber.mootool.next.compose.ui.workbench

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.rememberber.mootool.next.compose.model.ThemePreference
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooSwitch
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

class CompactShellCaptureTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun capture960CollapsedShellAndFocusedControlRing() = runDesktopComposeUiTest(width = 960, height = 640) {
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                val collapsed = LayoutPolicy.collapseNavigation(960f - 1f, false, false)
                Column(Modifier.fillMaxSize().background(colors.workspace)) {
                    Row(
                        Modifier.fillMaxWidth().height(MooTheme.dimens.toolbar).background(colors.toolbarBrush())
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MooButton("JSON", onClick = {}, primary = true)
                        MooSwitch(checked = true, onCheckedChange = {})
                    }
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        Box(
                            Modifier.width(if (collapsed) 84.dp else 248.dp).fillMaxHeight().background(colors.sidebar)
                        )
                        val showVault = LayoutPolicy.showVault(compact = true, compactAux = "")
                        if (showVault) {
                            Box(Modifier.width(240.dp).fillMaxHeight().background(colors.sidebar))
                        }
                        Box(Modifier.weight(1f).fillMaxHeight().background(colors.workspace))
                    }
                }
            }
        }
        onNodeWithText("JSON").requestFocus()
        waitForIdle()
        val image = onRoot().captureToImage().toAwtImage()
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-tray-density/captures")
        dir.mkdirs()
        val file = File(dir, "compact-960-focus-ring.png")
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(file.length() > 200)
        assertTrue(image.width >= 900)
        assertTrue(image.height >= 600)
        val expected = 0x316DC0
        var hits = 0
        for (y in 0 until minOf(image.height, 80)) {
            for (x in 0 until minOf(image.width, 220)) {
                if (image.getRGB(x, y) and 0x00FFFFFF == expected) hits++
            }
        }
        assertTrue(hits >= 8, "focused MooButton must paint focusRing pixels, hits=$hits")
    }
}
