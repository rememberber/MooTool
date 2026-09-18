package com.rememberber.mootool.next.compose.features.git

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.rememberber.mootool.next.compose.domain.GitOperationPresentation
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

/** Vault Git merge/rebase 冲突期「提交」禁用态 Compose 帧（对齐 `VaultGitDialog` + DIFF-494）。 */
class GitVaultCommitDisabledCaptureTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureMergeCommitDisabledWhileConflictsRemain() = runDesktopComposeUiTest(width = 480, height = 120) {
        captureCommitDisabled(
            frameName = "212-compose-git-merge-commit-disabled-tab-focus.png",
            statusPillKey = "git.operationMerge",
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureRebaseCommitDisabledWhileConflictsRemain() = runDesktopComposeUiTest(width = 520, height = 120) {
        captureCommitDisabled(
            frameName = "213-compose-git-rebase-commit-disabled-tab-focus.png",
            statusPillKey = "git.operationRebase",
        )
    }

    @OptIn(ExperimentalTestApi::class)
    private fun ComposeUiTest.captureCommitDisabled(frameName: String, statusPillKey: String) {
        val zh = Translator(AppLanguage.ZhCN)
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
                    MooStatusPill(zh.t(statusPillKey), kind = MooStatusKind.Error)
                    MooButton(
                        zh.t("git.commit"),
                        prominent = true,
                        p5Toolbar = true,
                        enabled = GitOperationPresentation.commitActionEnabled(
                            busy = false,
                            merging = true,
                            conflicts = 1,
                            hasChanges = true,
                            messageTrimmed = "blocked",
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
        val file = File(dir, frameName)
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(file.length() > 500, "commit disabled frame should be non-empty")
    }
}
