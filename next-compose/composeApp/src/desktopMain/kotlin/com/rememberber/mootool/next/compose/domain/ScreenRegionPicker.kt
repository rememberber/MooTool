package com.rememberber.mootool.next.compose.domain

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Cursor
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.image.BufferedImage
import javax.swing.JWindow
import javax.swing.SwingUtilities

object ScreenRegionPicker {
    fun show(
        capture: ScreenCapture,
        hint: String,
        onPicked: (BufferedImage) -> Unit,
        onCancel: () -> Unit
    ) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater { show(capture, hint, onPicked, onCancel) }
            return
        }
        val overlays = mutableListOf<RegionOverlayWindow>()
        var closed = false
        var start: Point? = null
        var current: Point? = null
        lateinit var dispatcher: KeyEventDispatcher

        fun closeAnd(action: () -> Unit) {
            if (closed) return
            closed = true
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(dispatcher)
            overlays.forEach { it.dispose() }
            action()
        }

        fun rect(): ImageCropRect? {
            val from = start ?: return null
            val to = current ?: return null
            val imageStartX = from.x - capture.originX
            val imageStartY = from.y - capture.originY
            val imageEndX = to.x - capture.originX
            val imageEndY = to.y - capture.originY
            val selected = ImageEngine.captureRectFromPoints(
                imageStartX,
                imageStartY,
                imageEndX,
                imageEndY,
                capture.image.width,
                capture.image.height
            )
            return if (selected.width >= 2 && selected.height >= 2) selected else null
        }

        dispatcher = KeyEventDispatcher { event ->
            if (!closed && event.id == KeyEvent.KEY_PRESSED && event.keyCode == KeyEvent.VK_ESCAPE) {
                closeAnd(onCancel)
                true
            } else {
                false
            }
        }
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher)

        val mouse = object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) {
                if (closed) return
                if (event.button == MouseEvent.BUTTON3 || SwingUtilities.isRightMouseButton(event)) {
                    closeAnd(onCancel)
                    return
                }
                if (event.button == MouseEvent.BUTTON1) {
                    start = Point(event.xOnScreen, event.yOnScreen)
                    current = start
                    overlays.forEach { it.repaint() }
                }
            }

            override fun mouseReleased(event: MouseEvent) {
                if (closed || event.button != MouseEvent.BUTTON1) return
                current = Point(event.xOnScreen, event.yOnScreen)
                val selected = rect()
                if (selected == null) {
                    closeAnd(onCancel)
                } else {
                    val cropped = ImageEngine.crop(capture.image, selected)
                    closeAnd { onPicked(cropped) }
                }
            }
        }
        val motion = object : MouseMotionAdapter() {
            override fun mouseDragged(event: MouseEvent) {
                if (closed || start == null) return
                current = Point(event.xOnScreen, event.yOnScreen)
                overlays.forEach { it.repaint() }
            }
        }

        GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.forEach { device ->
            val bounds = device.defaultConfiguration.bounds
            val overlay = RegionOverlayWindow(capture, bounds, hint) { start to current }
            overlay.addMouseListener(mouse)
            overlay.addMouseMotionListener(motion)
            overlay.isVisible = true
            overlays += overlay
        }
        overlays.firstOrNull()?.requestFocus()
    }
}

private class RegionOverlayWindow(
    private val capture: ScreenCapture,
    bounds: Rectangle,
    private val hint: String,
    private val selection: () -> Pair<Point?, Point?>
) : JWindow() {
    private val sourceX = bounds.x - capture.originX
    private val sourceY = bounds.y - capture.originY

    init {
        isAlwaysOnTop = true
        cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
        setBounds(bounds)
        focusableWindowState = true
    }

    override fun paint(g: Graphics) {
        val graphics = g as Graphics2D
        graphics.drawImage(
            capture.image,
            0,
            0,
            width,
            height,
            sourceX,
            sourceY,
            sourceX + width,
            sourceY + height,
            null
        )
        graphics.color = Color(0, 0, 0, 110)
        graphics.fillRect(0, 0, width, height)
        val (start, current) = selection()
        if (start != null && current != null) {
            val left = minOf(start.x, current.x) - bounds.x
            val top = minOf(start.y, current.y) - bounds.y
            val right = maxOf(start.x, current.x) - bounds.x
            val bottom = maxOf(start.y, current.y) - bounds.y
            val sx1 = (minOf(start.x, current.x) - capture.originX).coerceIn(0, capture.image.width)
            val sy1 = (minOf(start.y, current.y) - capture.originY).coerceIn(0, capture.image.height)
            val sx2 = (maxOf(start.x, current.x) - capture.originX).coerceIn(0, capture.image.width)
            val sy2 = (maxOf(start.y, current.y) - capture.originY).coerceIn(0, capture.image.height)
            if (sx2 > sx1 && sy2 > sy1) {
                graphics.drawImage(capture.image, left, top, right, bottom, sx1, sy1, sx2, sy2, null)
            }
            graphics.color = Color.WHITE
            graphics.stroke = BasicStroke(2f)
            graphics.drawRect(left, top, (right - left).coerceAtLeast(1), (bottom - top).coerceAtLeast(1))
        }
        graphics.color = Color.WHITE
        graphics.font = Font(Font.SANS_SERIF, Font.PLAIN, 14)
        graphics.drawString(hint, 24, 32)
    }
}
