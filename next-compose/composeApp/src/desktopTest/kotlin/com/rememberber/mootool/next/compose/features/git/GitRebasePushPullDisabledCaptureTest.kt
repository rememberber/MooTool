package com.rememberber.mootool.next.compose.features.git

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
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
import com.rememberber.mootool.next.compose.ui.components.MooStatusKind
import com.rememberber.mootool.next.compose.ui.components.MooStatusPill
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/** Vault Git rebase 进行中 push/pull 禁用态 Compose 帧（§C，非产品主窗；对齐 merge 帧 `204`）。 */
class GitRebasePushPullDisabledCaptureTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureRebasePushPullDisabledWhileMerging() = runDesktopComposeUiTest(width = 520, height = 120) {
        val zh = Translator(AppLanguage.ZhCN)
        val remote = "https://example.git/repo.git"
        setContent {
            MooTheme(preference = ThemePreference.Light, systemDark = false, interfaceStyle = "modern") {
                val colors = MooTheme.colors
                Row(
                    Modifier
                        .padding(16.dp)
                        .background(colors.workspace),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MooStatusPill(zh.t("git.operationRebase"), kind = MooStatusKind.Error)
                    MooButton(
                        zh.t("git.pull"),
                        p5Toolbar = true,
                        enabled = GitVaultRemotePresentation.pullActionEnabled(
                            persistedRemote = remote,
                            merging = true,
                            busy = false,
                        ),
                        onClick = {},
                    )
                    MooButton(
                        zh.t("git.push"),
                        p5Toolbar = true,
                        enabled = GitVaultRemotePresentation.pushActionEnabled(
                            persistedRemote = remote,
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
        val file = File(dir, "211-compose-git-rebase-push-pull-disabled-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(file.length() > 500, "rebase push/pull disabled frame should be non-empty")
    }
}
