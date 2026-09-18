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

/** 未解决冲突时 push 禁用（DIFF-516 / `GitEngine.push` 守卫；非 merge 进行中亦可出现冲突计数）。 */
class GitVaultPushDisabledConflictsCaptureTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun capturePushDisabledWhenConflictsRemain() = runDesktopComposeUiTest(width = 480, height = 120) {
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
                            merging = false,
                            busy = false,
                            conflicts = 2,
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
        val file = File(dir, "216-compose-git-push-disabled-unresolved-conflicts-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(file.length() > 500, "push disabled with conflicts frame should be non-empty")
    }
}
