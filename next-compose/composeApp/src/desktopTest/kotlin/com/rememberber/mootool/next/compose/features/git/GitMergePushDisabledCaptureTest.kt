package com.rememberber.mootool.next.compose.features.git

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.rememberber.mootool.next.compose.domain.GitVaultRemotePresentation
import com.rememberber.mootool.next.compose.i18n.Translator
import com.rememberber.mootool.next.compose.model.AppLanguage
import com.rememberber.mootool.next.compose.model.ThemePreference
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/** Vault Git merge 进行中 push 禁用态 Compose 帧（§B 步骤 1，非产品主窗）。 */
class GitMergePushDisabledCaptureTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureMergePushDisabledWhileMerging() = runDesktopComposeUiTest(width = 480, height = 120) {
        val zh = Translator(AppLanguage.ZhCN)
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                Row(
                    Modifier
                        .padding(16.dp)
                        .background(colors.workspace),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MooButton(
                        zh.t("git.push"),
                        p5Toolbar = true,
                        enabled = GitVaultRemotePresentation.pushActionEnabled(
                            persistedRemote = "https://example.git/repo.git",
                            merging = true,
                            busy = false,
                        ),
                        onClick = {},
                    )
                }
            }
        }
        val cwd = File(".").canonicalFile
        val root = if (cwd.name == "composeApp") cwd.parentFile else cwd
        val dir = File(root, "docs/evidence/2026-09-15-inspector-screencapture/windows")
        dir.mkdirs()
        waitForIdle()
        val file = File(dir, "204-compose-git-merge-push-disabled-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(file.length() > 500, "merge push disabled frame should be non-empty")
    }
}
