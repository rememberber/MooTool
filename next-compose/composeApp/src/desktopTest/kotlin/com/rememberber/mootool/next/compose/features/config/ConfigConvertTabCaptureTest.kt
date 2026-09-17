package com.rememberber.mootool.next.compose.features.config

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.rememberber.mootool.next.compose.i18n.Translator
import com.rememberber.mootool.next.compose.model.AppLanguage
import com.rememberber.mootool.next.compose.model.ThemePreference
import com.rememberber.mootool.next.compose.ui.components.MooToolTab
import com.rememberber.mootool.next.compose.ui.components.mooConfigTabsRow
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/** F06 配置转换 Tab 行 Compose Tab 焦点帧（非产品主窗）。 */
class ConfigConvertTabCaptureTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureConfigConvertTabFocus() = runDesktopComposeUiTest(width = 320, height = 120) {
        val zh = Translator(AppLanguage.ZhCN)
        val focus = FocusRequester()
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                Row(
                    Modifier
                        .width(280.dp)
                        .background(colors.workspace)
                        .mooConfigTabsRow()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MooToolTab(
                        zh.t("config.tab.convert"),
                        selected = true,
                        modifier = Modifier.focusRequester(focus),
                        onClick = {},
                    )
                    MooToolTab(
                        zh.t("config.tab.validate"),
                        selected = false,
                        onClick = {},
                    )
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
        val file = File(dir, "171-compose-config-convert-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "config convert tab focus ring")
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
