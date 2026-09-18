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
import com.rememberber.mootool.next.compose.domain.GitMergeConflictPresentation
import com.rememberber.mootool.next.compose.domain.GitMergeProductFlowPresentation
import com.rememberber.mootool.next.compose.i18n.Translator
import com.rememberber.mootool.next.compose.model.AppLanguage
import com.rememberber.mootool.next.compose.model.ThemePreference
import com.rememberber.mootool.next.compose.ui.components.MooButton
import com.rememberber.mootool.next.compose.ui.components.MooStatusKind
import com.rememberber.mootool.next.compose.ui.components.MooStatusPill
import com.rememberber.mootool.next.compose.ui.components.mooGitMergeFlowHint
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/** Vault Git rebase 进行中 §C hint 叠层 Compose Tab 焦点帧（对齐 [GitMergeFlowOverlayCaptureTest] merge 帧 `153`）。 */
class GitRebaseFlowOverlayCaptureTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureRebaseFlowHintRefreshButtonFocusRing() = runDesktopComposeUiTest(width = 640, height = 160) {
        val zh = Translator(AppLanguage.ZhCN)
        val refreshFocus = FocusRequester()
        val merging = true
        val conflicts = 1
        val selectedConflict = false
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
                    ) {
                        MooStatusPill(zh.t("git.operationRebase"), kind = MooStatusKind.Error)
                        Text(
                            zh.t("git.counts", mapOf("changes" to "2", "conflicts" to conflicts.toString())),
                            color = colors.danger,
                            fontSize = 11.sp,
                        )
                    }
                    GitMergeConflictPresentation.unresolvedHintKey(merging, conflicts, "rebase")?.let { key ->
                        Text(zh.t(key), color = colors.danger, fontSize = 11.sp)
                    }
                    GitMergeProductFlowPresentation.productFlowHintKey(
                        merging,
                        conflicts,
                        selectedConflict,
                        "rebase",
                    )?.let { key ->
                        Text(
                            zh.t(key),
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.mooGitMergeFlowHint(),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MooButton(
                            zh.t("git.refresh"),
                            p5Toolbar = true,
                            onClick = {},
                            modifier = Modifier.focusRequester(refreshFocus),
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
        runOnIdle { refreshFocus.requestFocus() }
        waitForIdle()
        val file = File(dir, "209-compose-git-rebase-flow-hint-tab-focus.png")
        val image = onRoot().captureToImage().toAwtImage()
        assertTrue(ImageIO.write(image, "png", file))
        assertTrue(countRingPixels(image, expected) >= 8, "git rebase flow hint refresh focus ring")
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
