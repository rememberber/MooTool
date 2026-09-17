package com.rememberber.mootool.next.compose.features.http

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.ui.components.mooFocusClickable
import com.rememberber.mootool.next.compose.ui.components.mooHttpCollection
import com.rememberber.mootool.next.compose.ui.components.mooHttpSavedItem
import com.rememberber.mootool.next.compose.ui.components.mooHttpSavedList
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.model.ThemePreference
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/** F09 HTTP 集合 `.http-saved-item` 行 Compose Tab 焦点帧（对齐 [HttpScreen] 集合列）。 */
class HttpSavedItemCaptureTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureHttpSavedItemTabFocus() = runDesktopComposeUiTest(width = 240, height = 120) {
        val focus = FocusRequester()
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                Column(
                    Modifier
                        .width(210.dp)
                        .mooHttpCollection()
                        .mooHttpSavedList()
                        .background(colors.surfaceSubtle),
                ) {
                    Column(
                        Modifier.fillMaxWidth()
                            .focusRequester(focus)
                            .mooHttpSavedItem(active = true, hovered = false)
                            .mooFocusClickable { },
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            "Sample request",
                            color = colors.textBody,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("GET", color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Text("https://example.com", color = colors.textMuted, fontSize = 10.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val expected = 0x316DC0
        runOnIdle { focus.requestFocus() }
        waitForIdle()
        val file = File(dir, "163-compose-http-saved-item-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "http saved item focus ring")
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
