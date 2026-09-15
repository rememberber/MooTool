package com.rememberber.mootool.next.compose.ui.workbench

import java.awt.GraphicsEnvironment
import java.awt.Toolkit

data class DisplayWorkArea(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    fun titleBarVisible(windowX: Int, windowY: Int, windowWidth: Int): Boolean {
        val overlapLeft = maxOf(windowX, x)
        val overlapRight = minOf(windowX + windowWidth, x + width)
        val overlapTop = maxOf(windowY, y)
        val overlapBottom = minOf(windowY + WindowBoundsPolicy.TITLE_BAR_HEIGHT, y + height)
        return overlapRight - overlapLeft >= WindowBoundsPolicy.MIN_TITLE_VISIBLE &&
            overlapBottom - overlapTop > 0
    }

    fun nearestPointDistanceSquared(px: Int, py: Int): Long {
        val nx = px.coerceIn(x, x + (width - 1).coerceAtLeast(0))
        val ny = py.coerceIn(y, y + (height - 1).coerceAtLeast(0))
        val dx = (px - nx).toLong()
        val dy = (py - ny).toLong()
        return dx * dx + dy * dy
    }
}

data class RestoredWindow(
    val x: Int?,
    val y: Int?,
    val width: Int,
    val height: Int
)

object WindowBoundsPolicy {
    const val TITLE_BAR_HEIGHT = 28
    const val MIN_TITLE_VISIBLE = 48
    const val MIN_WIDTH = 960
    const val MIN_HEIGHT = 640

    fun clamp(
        x: Int?,
        y: Int?,
        width: Int,
        height: Int,
        screens: List<DisplayWorkArea>
    ): RestoredWindow {
        val w = width.coerceAtLeast(MIN_WIDTH)
        val h = height.coerceAtLeast(MIN_HEIGHT)
        if (x == null || y == null) return RestoredWindow(null, null, w, h)
        if (screens.isEmpty() || screens.any { it.titleBarVisible(x, y, w) }) {
            return RestoredWindow(x, y, w, h)
        }
        val nearest = screens.minBy { it.nearestPointDistanceSquared(x, y) }
        val maxX = (nearest.x + nearest.width - MIN_TITLE_VISIBLE).coerceAtLeast(nearest.x)
        val maxY = (nearest.y + nearest.height - TITLE_BAR_HEIGHT).coerceAtLeast(nearest.y)
        return RestoredWindow(
            x.coerceIn(nearest.x, maxX),
            y.coerceIn(nearest.y, maxY),
            w,
            h
        )
    }

    fun currentWorkAreas(): List<DisplayWorkArea> = runCatching {
        val env = GraphicsEnvironment.getLocalGraphicsEnvironment()
        if (env.isHeadlessInstance) emptyList()
        else env.screenDevices.mapNotNull { device ->
            val configuration = device.defaultConfiguration
            val bounds = configuration.bounds
            val insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration)
            val workWidth = bounds.width - insets.left - insets.right
            val workHeight = bounds.height - insets.top - insets.bottom
            if (workWidth <= 0 || workHeight <= 0) null
            else DisplayWorkArea(
                x = bounds.x + insets.left,
                y = bounds.y + insets.top,
                width = workWidth,
                height = workHeight
            )
        }
    }.getOrDefault(emptyList())
}
