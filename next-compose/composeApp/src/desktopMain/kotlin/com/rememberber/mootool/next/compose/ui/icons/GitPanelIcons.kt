package com.rememberber.mootool.next.compose.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Lucide-aligned glyphs for Vault Git panel (Electron `VaultGitDialog.tsx`). */
enum class GitPanelIconKind {
    Branch,
    Refresh,
    CloudDownload,
    CloudUpload,
    Merge,
    Undo,
    ShieldCheck,
    Commit,
}

@Composable
fun GitPanelIcon(
    kind: GitPanelIconKind,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 13.dp,
) {
    Canvas(modifier.size(size)) {
        val stroke = Stroke(
            width = 1.35.dp.toPx().coerceAtLeast(1.1f),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val s = this.size.minDimension / 16f
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(tint, p(x1, y1), p(x2, y2), strokeWidth = stroke.width, cap = StrokeCap.Round)
        }
        fun circle(x: Float, y: Float, r: Float) {
            drawCircle(tint, radius = r * s, center = p(x, y), style = stroke)
        }
        when (kind) {
            GitPanelIconKind.Branch -> {
                circle(5.5f, 4.5f, 1.6f)
                circle(11f, 11.5f, 1.6f)
                line(5.5f, 6.2f, 5.5f, 9.5f)
                line(5.5f, 9.5f, 9.2f, 9.5f)
                line(9.2f, 9.5f, 9.2f, 11.5f)
                line(9.2f, 11.5f, 11f, 11.5f)
            }
            GitPanelIconKind.Refresh -> {
                circle(8f, 8f, 4.2f)
                line(8f, 3.2f, 8f, 5.2f)
                line(8f, 3.2f, 10.2f, 5.4f)
            }
            GitPanelIconKind.CloudDownload -> {
                line(4.5f, 11.5f, 11.5f, 11.5f)
                line(6.5f, 11.5f, 6.5f, 13f)
                line(9.5f, 11.5f, 9.5f, 13f)
                line(8f, 8.5f, 8f, 12.5f)
                line(6.2f, 10.2f, 8f, 12.5f)
                line(9.8f, 10.2f, 8f, 12.5f)
                line(5f, 9f, 6.2f, 7.5f)
                line(11f, 9f, 9.8f, 7.5f)
                line(6.2f, 7.5f, 9.8f, 7.5f)
            }
            GitPanelIconKind.CloudUpload -> {
                line(4.5f, 11.5f, 11.5f, 11.5f)
                line(6.5f, 11.5f, 6.5f, 13f)
                line(9.5f, 11.5f, 9.5f, 13f)
                line(8f, 5f, 8f, 9f)
                line(6.2f, 6.8f, 8f, 5f)
                line(9.8f, 6.8f, 8f, 5f)
                line(5f, 9f, 6.2f, 7.5f)
                line(11f, 9f, 9.8f, 7.5f)
                line(6.2f, 7.5f, 9.8f, 7.5f)
            }
            GitPanelIconKind.Merge -> {
                circle(4.5f, 8f, 1.5f)
                circle(11.5f, 5f, 1.5f)
                circle(11.5f, 11f, 1.5f)
                line(6f, 8f, 10f, 5.5f)
                line(6f, 8f, 10f, 10.5f)
            }
            GitPanelIconKind.Undo -> {
                line(5f, 5f, 5f, 8.5f)
                line(5f, 5f, 8.5f, 5f)
                line(5f, 8.5f, 11f, 8.5f)
                line(9f, 6.5f, 11f, 8.5f)
                line(9f, 10.5f, 11f, 8.5f)
            }
            GitPanelIconKind.ShieldCheck -> {
                line(8f, 2.5f, 12.5f, 4.5f)
                line(12.5f, 4.5f, 12.5f, 8.5f)
                line(12.5f, 8.5f, 8f, 13.5f)
                line(8f, 13.5f, 3.5f, 8.5f)
                line(3.5f, 8.5f, 3.5f, 4.5f)
                line(3.5f, 4.5f, 8f, 2.5f)
                line(5.8f, 8.2f, 7.2f, 9.8f)
                line(7.2f, 9.8f, 10.5f, 6.2f)
            }
            GitPanelIconKind.Commit -> {
                circle(4.5f, 8f, 1.8f)
                circle(11.5f, 8f, 1.8f)
                line(6.3f, 8f, 9.7f, 8f)
                line(11.5f, 6.2f, 11.5f, 4.5f)
                line(11.5f, 9.8f, 11.5f, 11.5f)
            }
        }
    }
}
