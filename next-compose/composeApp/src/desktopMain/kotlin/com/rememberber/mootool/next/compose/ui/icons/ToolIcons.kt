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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rememberber.mootool.next.compose.model.ToolId

@Composable
fun ToolIcon(id: ToolId, tint: Color, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Canvas(modifier.size(size)) {
        val stroke = Stroke(width = 1.35.dp.toPx().coerceAtLeast(1.1f), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val s = this.size.minDimension / 16f
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(tint, p(x1, y1), p(x2, y2), strokeWidth = stroke.width, cap = StrokeCap.Round)
        }
        fun rect(x: Float, y: Float, w: Float, h: Float) {
            drawRect(tint, p(x, y), Size(w * s, h * s), style = stroke)
        }
        fun circle(x: Float, y: Float, r: Float) {
            drawCircle(tint, radius = r * s, center = p(x, y), style = stroke)
        }
        when (id) {
            ToolId.Mootool -> {
                circle(8f, 5.5f, 2.4f)
                line(8f, 8.2f, 8f, 13.2f)
                line(5.2f, 10.4f, 10.8f, 10.4f)
            }
            ToolId.QuickNote -> {
                rect(4f, 2.8f, 8f, 10.4f)
                line(6f, 5.6f, 10f, 5.6f)
                line(6f, 8f, 10f, 8f)
                line(6f, 10.4f, 9f, 10.4f)
            }
            ToolId.TextDiff -> {
                line(4f, 4f, 7.5f, 8f)
                line(7.5f, 8f, 4f, 12f)
                line(12f, 4f, 8.5f, 8f)
                line(8.5f, 8f, 12f, 12f)
            }
            ToolId.Reformat -> {
                line(4f, 4.5f, 12f, 4.5f)
                line(4f, 8f, 10f, 8f)
                line(4f, 11.5f, 12f, 11.5f)
            }
            ToolId.Json -> {
                line(6f, 3.5f, 4f, 8f)
                line(4f, 8f, 6f, 12.5f)
                line(10f, 3.5f, 12f, 8f)
                line(12f, 8f, 10f, 12.5f)
            }
            ToolId.Java -> {
                rect(3.5f, 3.5f, 9f, 9f)
                line(6f, 6.2f, 10f, 6.2f)
                line(6f, 9.8f, 9.2f, 9.8f)
            }
            ToolId.YmlProperties -> {
                line(4f, 4.5f, 12f, 4.5f)
                line(4f, 8f, 12f, 8f)
                line(4f, 11.5f, 12f, 11.5f)
                circle(5.5f, 4.5f, 0.9f)
                circle(5.5f, 8f, 0.9f)
                circle(5.5f, 11.5f, 0.9f)
            }
            ToolId.Protobuf -> {
                line(8f, 3f, 12f, 6f)
                line(12f, 6f, 8f, 13f)
                line(8f, 13f, 4f, 6f)
                line(4f, 6f, 8f, 3f)
            }
            ToolId.Variables -> {
                line(4f, 12f, 8f, 4f)
                line(8f, 4f, 12f, 12f)
                line(5.6f, 9f, 10.4f, 9f)
            }
            ToolId.Http -> {
                line(3.5f, 8f, 12.5f, 8f)
                line(10f, 5.2f, 12.5f, 8f)
                line(10f, 10.8f, 12.5f, 8f)
            }
            ToolId.Host -> {
                circle(8f, 8f, 5f)
                line(8f, 3f, 8f, 13f)
                line(3f, 8f, 13f, 8f)
            }
            ToolId.Net -> {
                circle(8f, 8f, 2f)
                circle(8f, 8f, 4.2f)
                circle(8f, 8f, 6.2f)
            }
            ToolId.UaParse -> {
                rect(3.5f, 4f, 9f, 8f)
                line(3.5f, 7f, 12.5f, 7f)
            }
            ToolId.Encode -> {
                line(4f, 8f, 7f, 5f)
                line(4f, 8f, 7f, 11f)
                line(9f, 5f, 12f, 8f)
                line(12f, 8f, 9f, 11f)
            }
            ToolId.Crypto -> {
                rect(5f, 7.2f, 6f, 5.5f)
                drawArcLine(tint, stroke, 5.6f * s, 7.2f * s, 4.8f * s, 4.6f * s)
            }
            ToolId.Regex -> {
                line(4f, 11.5f, 6.5f, 4.5f)
                line(6.5f, 4.5f, 9.5f, 11.5f)
                line(9.5f, 11.5f, 12f, 4.5f)
            }
            ToolId.Cron -> {
                circle(8f, 8.2f, 5f)
                line(8f, 8.2f, 8f, 5.2f)
                line(8f, 8.2f, 10.6f, 9.6f)
            }
            ToolId.QrCode -> {
                rect(3.5f, 3.5f, 4f, 4f)
                rect(8.5f, 3.5f, 4f, 4f)
                rect(3.5f, 8.5f, 4f, 4f)
                rect(9.2f, 9.2f, 2.6f, 2.6f)
            }
            ToolId.TimeConvert -> {
                circle(8f, 8f, 5.2f)
                line(8f, 8f, 8f, 5f)
                line(8f, 8f, 11f, 8f)
            }
            ToolId.MessageBoard -> {
                rect(3.5f, 3.8f, 9f, 6.4f)
                line(3.5f, 10.2f, 6f, 13f)
                line(6f, 13f, 6f, 10.2f)
            }
            ToolId.Translation -> {
                rect(3.4f, 4f, 5.2f, 7.6f)
                rect(7.4f, 4.8f, 5.2f, 7.6f)
            }
            ToolId.Calculator -> {
                rect(4f, 3.2f, 8f, 9.6f)
                line(6f, 6.4f, 10f, 6.4f)
                line(6f, 9.2f, 10f, 9.2f)
            }
            ToolId.ColorBoard -> {
                circle(6.2f, 7.2f, 3.2f)
                circle(9.8f, 8.8f, 3.2f)
            }
            ToolId.Image -> {
                rect(3.4f, 4f, 9.2f, 8f)
                circle(6.2f, 7f, 1.2f)
                line(4.4f, 10.8f, 7.4f, 8.2f)
                line(7.4f, 8.2f, 12.2f, 11.2f)
            }
            ToolId.Pdf -> {
                rect(4.2f, 2.8f, 7.6f, 10.4f)
                line(4.2f, 6.4f, 11.8f, 6.4f)
            }
            ToolId.Hardware -> {
                circle(8f, 8f, 2.2f)
                line(8f, 3.2f, 8f, 5.2f)
                line(8f, 10.8f, 8f, 12.8f)
                line(3.2f, 8f, 5.2f, 8f)
                line(10.8f, 8f, 12.8f, 8f)
            }
        }
    }
}

private fun DrawScope.drawArcLine(tint: Color, stroke: Stroke, left: Float, top: Float, width: Float, height: Float) {
    drawArc(
        color = tint,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(left, top - height),
        size = Size(width, height * 2f),
        style = stroke
    )
}
