package com.rememberber.mootool.next.compose.domain

import java.awt.Color
import java.awt.Cursor
import java.awt.Font
import java.awt.Graphics
import java.awt.GraphicsEnvironment
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.image.BufferedImage
import javax.swing.JWindow
import javax.swing.SwingUtilities

data class ScreenPickerCopy(
    val hint: String,
    val keys: String
)

object ScreenColorPicker {
    fun show(
        capture: ScreenCapture,
        copy: ScreenPickerCopy,
        onPicked: (RgbColor) -> Unit,
        onCancel: () -> Unit
    ) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater { show(capture, copy, onPicked, onCancel) }
            return
        }
        val overlays = mutableListOf<JWindow>()
        val preview = PreviewWindow(copy)
        var closed = false
        lateinit var dispatcher: KeyEventDispatcher

        fun closeAnd(action: () -> Unit) {
            if (closed) return
            closed = true
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(dispatcher)
            overlays.forEach { it.dispose() }
            preview.dispose()
            action()
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
                    val color = ScreenColorSampler.colorAt(capture, event.xOnScreen, event.yOnScreen)
                    closeAnd { onPicked(color) }
                }
            }
        }
        val motion = object : MouseMotionAdapter() {
            override fun mouseMoved(event: MouseEvent) {
                if (closed) return
                preview.update(capture, event.xOnScreen, event.yOnScreen)
            }

            override fun mouseDragged(event: MouseEvent) {
                mouseMoved(event)
            }
        }

        GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.forEach { device ->
            val bounds = device.defaultConfiguration.bounds
            val overlay = OverlayWindow(capture, bounds)
            overlay.addMouseListener(mouse)
            overlay.addMouseMotionListener(motion)
            overlay.isVisible = true
            overlays += overlay
        }
        preview.isVisible = true
        val pointer = runCatching { MouseInfo.getPointerInfo()?.location }.getOrNull()
            ?: Point(capture.originX, capture.originY)
        preview.update(capture, pointer.x, pointer.y)
        overlays.firstOrNull()?.requestFocus()
    }
}

private class OverlayWindow(
    private val capture: ScreenCapture,
    bounds: Rectangle
) : JWindow() {
    private val sourceX = bounds.x - capture.originX
    private val sourceY = bounds.y - capture.originY

    init {
        isAlwaysOnTop = true
        cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
        setBounds(bounds)
        focusableWindowState = true
    }

    override fun paint(graphics: Graphics) {
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
    }
}

private class PreviewWindow(private val copy: ScreenPickerCopy) : JWindow() {
    private var zoom = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
    private var hex = "#000000"
    private var position = "0, 0"

    init {
        isAlwaysOnTop = true
        focusableWindowState = false
        setSize(188, 188)
    }

    fun update(capture: ScreenCapture, screenX: Int, screenY: Int) {
        val color = ScreenColorSampler.colorAt(capture, screenX, screenY)
        zoom = ScreenColorSampler.zoom(capture, screenX, screenY)
        hex = ColorEngine.formatColor(color, ColorFormat.HEX_UPPER)
        position = "$screenX, $screenY"
        val offset = 28
        var x = screenX + offset
        var y = screenY + offset
        val screens = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
        if (x + width > screens.x + screens.width) x = screenX - width - offset
        if (y + height > screens.y + screens.height) y = screenY - height - offset
        setLocation(x, y)
        repaint()
    }

    override fun paint(graphics: Graphics) {
        graphics.color = Color(32, 32, 36)
        graphics.fillRect(0, 0, width, height)
        graphics.drawImage(zoom, 8, 8, 88, 88, null)
        graphics.color = Color.WHITE
        graphics.drawRect(8 + 40, 8 + 40, 7, 7)
        graphics.color = Color.decode(hex)
        graphics.fillRect(108, 8, 68, 48)
        graphics.color = Color.WHITE
        graphics.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        graphics.drawString(hex, 108, 76)
        graphics.drawString(position, 8, 112)
        graphics.font = Font(Font.SANS_SERIF, Font.PLAIN, 11)
        graphics.drawString(copy.hint, 8, 136)
        graphics.drawString(copy.keys, 8, 156)
        graphics.color = Color(90, 90, 96)
        graphics.drawRect(0, 0, width - 1, height - 1)
    }
}
