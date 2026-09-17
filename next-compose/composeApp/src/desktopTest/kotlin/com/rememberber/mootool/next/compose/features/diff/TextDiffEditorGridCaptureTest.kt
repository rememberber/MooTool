package com.rememberber.mootool.next.compose.features.diff

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.i18n.Translator
import com.rememberber.mootool.next.compose.model.AppLanguage
import com.rememberber.mootool.next.compose.model.ThemePreference
import com.rememberber.mootool.next.compose.ui.components.mooDiffEditorGrid
import com.rememberber.mootool.next.compose.ui.components.mooDiffEditorPane
import com.rememberber.mootool.next.compose.ui.components.mooDiffEditorSeam
import com.rememberber.mootool.next.compose.ui.components.mooFocusClickable
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/** F02 `.diff-editor-grid` 并排编辑区 Compose Tab 焦点帧（对齐 [TextDiffScreen] 左栏标题+输入）。 */
class TextDiffEditorGridCaptureTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureDiffEditorGridTabFocus() = runDesktopComposeUiTest(width = 640, height = 220) {
        val zh = Translator(AppLanguage.ZhCN)
        val focus = FocusRequester()
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                Row(
                    Modifier
                        .padding(12.dp)
                        .mooDiffEditorGrid()
                        .background(colors.surfaceSubtle),
                ) {
                    Column(
                        Modifier
                            .width(280.dp)
                            .fillMaxHeight()
                            .mooDiffEditorPane(),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text(
                            zh.t("diff.left"),
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .focusRequester(focus)
                                .mooFocusClickable { },
                        )
                        BasicTextField(
                            value = TextFieldValue("left\n"),
                            onValueChange = {},
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                            ),
                            cursorBrush = SolidColor(colors.accent),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Box(Modifier.mooDiffEditorSeam().background(colors.borderSoft))
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .mooDiffEditorPane(),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text(
                            zh.t("diff.right"),
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        BasicTextField(
                            value = TextFieldValue("right\n"),
                            onValueChange = {},
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                            ),
                            cursorBrush = SolidColor(colors.accent),
                            modifier = Modifier.weight(1f),
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
        val file = File(dir, "169-compose-text-diff-editor-grid-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "text diff editor grid focus ring")
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
