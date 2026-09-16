package com.rememberber.mootool.next.compose.features.git

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Vault Git 面板冲突行「使用本地/远端」按钮样式与 [VaultGitDialog] 一致；Compose 场景 Tab 焦点帧，非产品主窗。
 */
class GitConflictActionsCaptureTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureMergeConflictOursButtonFocusRing() = runDesktopComposeUiTest(width = 520, height = 120) {
        val zh = Translator(AppLanguage.ZhCN)
        val oursFocus = FocusRequester()
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                Row(
                    Modifier
                        .padding(16.dp)
                        .background(colors.workspace),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MooButton(
                        zh.t("git.ours"),
                        p5Toolbar = true,
                        onClick = {},
                        modifier = Modifier.focusRequester(oursFocus),
                    )
                    MooButton(zh.t("git.theirs"), p5Toolbar = true, onClick = {})
                }
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val expected = 0x316DC0
        runOnIdle { oursFocus.requestFocus() }
        waitForIdle()
        val file = File(dir, "142-compose-git-conflict-ours-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "git conflict ours focus ring")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureMergeConflictTheirsButtonFocusRing() = runDesktopComposeUiTest(width = 520, height = 120) {
        val zh = Translator(AppLanguage.ZhCN)
        val theirsFocus = FocusRequester()
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                Row(
                    Modifier
                        .padding(16.dp)
                        .background(colors.workspace),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MooButton(zh.t("git.ours"), p5Toolbar = true, onClick = {})
                    MooButton(
                        zh.t("git.theirs"),
                        p5Toolbar = true,
                        onClick = {},
                        modifier = Modifier.focusRequester(theirsFocus),
                    )
                }
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val expected = 0x316DC0
        runOnIdle { theirsFocus.requestFocus() }
        waitForIdle()
        val file = File(dir, "144-compose-git-conflict-theirs-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "git conflict theirs focus ring")
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
