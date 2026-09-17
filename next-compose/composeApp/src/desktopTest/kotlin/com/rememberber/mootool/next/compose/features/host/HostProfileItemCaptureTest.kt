package com.rememberber.mootool.next.compose.features.host

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
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
import com.rememberber.mootool.next.compose.ui.components.mooHostProfilesPane
import com.rememberber.mootool.next.compose.ui.components.mooHttpSavedItem
import com.rememberber.mootool.next.compose.ui.components.mooHttpSavedList
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import com.rememberber.mootool.next.compose.model.ThemePreference
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/** F10 Host `.host-profile` 方案行 Compose Tab 焦点帧（对齐 [HostScreen] 列表行，非搜索框 `133`）。 */
class HostProfileItemCaptureTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureHostProfileItemTabFocus() = runDesktopComposeUiTest(width = 240, height = 120) {
        val focus = FocusRequester()
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                Column(
                    Modifier
                        .width(210.dp)
                        .mooHostProfilesPane()
                        .padding(7.dp)
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
                            "Local dev",
                            color = colors.textBody,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Text(
                            "2026-09-17 12:00",
                            color = colors.textMuted,
                            fontSize = 10.sp,
                            maxLines = 1,
                        )
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
        val file = File(dir, "164-compose-host-profile-item-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "host profile item focus ring")
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
