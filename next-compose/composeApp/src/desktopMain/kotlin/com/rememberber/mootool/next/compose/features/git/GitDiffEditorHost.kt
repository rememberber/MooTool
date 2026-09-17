package com.rememberber.mootool.next.compose.features.git

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.rememberber.mootool.next.compose.ui.theme.toAwtColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rememberber.mootool.next.compose.app.AppContainer
import com.rememberber.mootool.next.compose.domain.DiffResult
import com.rememberber.mootool.next.compose.domain.GitDiffPresentation
import com.rememberber.mootool.next.compose.domain.GitFileDiff
import com.rememberber.mootool.next.compose.editor.EditorBuffer
import com.rememberber.mootool.next.compose.editor.EditorHost
import com.rememberber.mootool.next.compose.ui.theme.MooTheme
import java.util.concurrent.atomic.AtomicBoolean
import java.awt.event.AdjustmentListener

@Composable
internal fun GitDiffSideBySideEditors(
    container: AppContainer,
    fileDiff: GitFileDiff,
    comparison: DiffResult,
    dark: Boolean,
    fontName: String,
    fontSize: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MooTheme.colors
    val syntax = remember(fileDiff.path, fileDiff.originalPath) { GitDiffPresentation.rstaSyntaxForFile(fileDiff) }
    val leftBuffer = remember { EditorBuffer("", syntax) }
    val rightBuffer = remember { EditorBuffer("", syntax) }
    val syncing = remember { AtomicBoolean(false) }

    LaunchedEffect(fileDiff.before, fileDiff.after, syntax) {
        leftBuffer.applySyntax(syntax)
        rightBuffer.applySyntax(syntax)
        leftBuffer.setText(fileDiff.before, recordUndo = false)
        rightBuffer.setText(fileDiff.after, recordUndo = false)
        leftBuffer.setEditable(false)
        rightBuffer.setEditable(false)
    }

    LaunchedEffect(comparison, colors.success, colors.danger, colors.accent) {
        leftBuffer.markDiffSide(
            comparison.segments,
            side = "left",
            added = colors.success.toAwtColor(),
            removed = colors.danger.toAwtColor(),
            changed = colors.accent.toAwtColor(),
        )
        rightBuffer.markDiffSide(
            comparison.segments,
            side = "right",
            added = colors.success.toAwtColor(),
            removed = colors.danger.toAwtColor(),
            changed = colors.accent.toAwtColor(),
        )
    }

    DisposableEffect(leftBuffer, rightBuffer) {
        val leftBar = leftBuffer.scrollPane.verticalScrollBar
        val rightBar = rightBuffer.scrollPane.verticalScrollBar
        val leftListener = AdjustmentListener {
            if (syncing.get()) return@AdjustmentListener
            syncing.set(true)
            rightBar.value = leftBar.value
            syncing.set(false)
        }
        val rightListener = AdjustmentListener {
            if (syncing.get()) return@AdjustmentListener
            syncing.set(true)
            leftBar.value = rightBar.value
            syncing.set(false)
        }
        leftBar.addAdjustmentListener(leftListener)
        rightBar.addAdjustmentListener(rightListener)
        onDispose {
            leftBar.removeAdjustmentListener(leftListener)
            rightBar.removeAdjustmentListener(rightListener)
        }
    }

    Row(modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Text(container.t("git.diffBefore"), color = colors.textSecondary, fontSize = 11.sp)
            EditorHost(
                buffer = leftBuffer,
                dark = dark,
                fontName = fontName,
                fontSize = fontSize,
                wrap = false,
                modifier = Modifier.weight(1f).fillMaxWidth().heightIn(min = 120.dp),
            )
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Text(container.t("git.diffAfter"), color = colors.textSecondary, fontSize = 11.sp)
            EditorHost(
                buffer = rightBuffer,
                dark = dark,
                fontName = fontName,
                fontSize = fontSize,
                wrap = false,
                modifier = Modifier.weight(1f).fillMaxWidth().heightIn(min = 120.dp),
            )
        }
    }
}
