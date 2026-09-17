package com.rememberber.mootool.next.compose.features.regex

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.domain.RegexMatch
import com.rememberber.mootool.next.compose.domain.RegexWiringPresentation
import com.rememberber.mootool.next.compose.i18n.Translator
import com.rememberber.mootool.next.compose.model.AppLanguage
import com.rememberber.mootool.next.compose.model.ThemePreference
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.mooRegexMatchCard
import com.rememberber.mootool.next.compose.ui.components.mooRegexResultsPane
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/** F15 正则测试钮 + 结果侧栏（对齐 [RegexScreen] 测试 Tab 控件）Compose 焦点帧。 */
class RegexMatchCaptureTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureRegexTestButtonFocusRing() = runDesktopComposeUiTest(width = 720, height = 200) {
        val zh = Translator(AppLanguage.ZhCN)
        val testFocus = FocusRequester()
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                Column(
                    Modifier
                        .padding(16.dp)
                        .background(colors.workspace),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MooButton(
                            zh.t("regex.tab.test"),
                            prominent = true,
                            enabled = RegexWiringPresentation.canRunTest("moo", running = false),
                            onClick = {},
                            p5Toolbar = true,
                            modifier = Modifier.focusRequester(testFocus),
                        )
                    }
                    Column(Modifier.mooRegexResultsPane()) {
                        Column(
                            Modifier
                                .mooRegexMatchCard()
                                .background(colors.workspace),
                        ) {
                            Text("#1 · 0", color = colors.textMuted, fontSize = 9.sp)
                            Text(RegexMatch(0, "moo", listOf("moo")).value, color = colors.textBody, fontSize = 11.sp)
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
        runOnIdle { testFocus.requestFocus() }
        waitForIdle()
        val file = File(dir, "159-compose-regex-test-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "regex test button focus ring")
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
