package com.rememberber.mootool.next.compose.features.git

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.domain.GitVaultRemotePresentation
import com.rememberber.mootool.next.compose.i18n.Translator
import com.rememberber.mootool.next.compose.model.AppLanguage
import com.rememberber.mootool.next.compose.model.ThemePreference
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.mooGitVaultRemoteRow
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/** Vault Git remote 行（对齐 [VaultGitDialog] 保存 remote 钮）Compose 焦点帧。 */
class GitVaultRemoteCaptureTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureGitVaultRemoteSaveButtonFocusRing() = runDesktopComposeUiTest(width = 640, height = 120) {
        val zh = Translator(AppLanguage.ZhCN)
        val saveFocus = FocusRequester()
        val draftRemote = "https://example.com/repo.git"
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                Column(
                    Modifier
                        .padding(16.dp)
                        .background(colors.workspace),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.mooGitVaultRemoteRow(),
                    ) {
                        Text(zh.t("git.remote"), color = colors.textMuted, fontSize = 11.sp)
                        Text(
                            draftRemote,
                            color = colors.textBody,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f),
                        )
                        MooButton(
                            zh.t("git.saveRemote"),
                            p5Toolbar = true,
                            enabled = GitVaultRemotePresentation.saveRemoteEnabled(
                                busy = false,
                                repository = true,
                                draftRemoteTrimmed = draftRemote,
                                statusRemote = "",
                            ),
                            onClick = {},
                            modifier = Modifier.focusRequester(saveFocus),
                        )
                    }
                    Text(
                        zh.t("git.remoteUnsavedHint"),
                        color = colors.textMuted,
                        fontSize = 10.sp,
                    )
                }
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        val expected = 0x316DC0
        runOnIdle { saveFocus.requestFocus() }
        waitForIdle()
        val file = File(dir, "156-compose-git-vault-remote-save-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "git vault remote save focus ring")
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
