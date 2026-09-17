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
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.image.BufferedImage
import javax.swing.JWindow
import javax.swing.SwingUtilities

object ScreenRegionPicker {
    @Volatile
    private var dismissActivePicker: (() -> Unit)? = null

    /** 离开工具页或取消流程时关闭区域截图层，避免遮罩跨页残留。 */
    fun dismissActive() {
        val dismiss = dismissActivePicker
        dismissActivePicker = null
        dismiss?.invoke()
    }

    fun show(
        capture: ScreenCapture,
        translate: (String) -> String,
        onPicked: (BufferedImage) -> Unit,
        onCancel: () -> Unit,
    ) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater { show(capture, translate, onPicked, onCancel) }
            return
        }
        val overlays = mutableListOf<RegionOverlayWindow>()
        var closed = false
        var selection: ImageCropRect? = null
        var drag: ActiveCaptureDrag? = null
        var preview: ImageCropRect? = null
        var keyDispatcher: KeyEventDispatcher? = null

        fun currentRect(): ImageCropRect? = preview ?: selection

        fun closeAnd(action: () -> Unit) {
            if (closed) return
            closed = true
            dismissActivePicker = null
            keyDispatcher?.let {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(it)
            }
            overlays.forEach { it.dispose() }
            action()
        }

        fun acceptSelection(rect: ImageCropRect) {
            val cropped = ImageEngine.crop(capture.image, rect)
            closeAnd { onPicked(cropped) }
        }

        fun repaintAll() {
            overlays.forEach { it.repaint() }
        }

        fun finishDrag(finalRect: ImageCropRect?) {
            drag = null
            preview = null
            if (finalRect != null && ScreenCaptureInteraction.isValidSelection(finalRect)) {
                selection = finalRect
            }
            repaintAll()
        }

        keyDispatcher = KeyEventDispatcher { event ->
            if (closed || event.id != KeyEvent.KEY_PRESSED) return@KeyEventDispatcher false
            when (event.keyCode) {
                KeyEvent.VK_ESCAPE -> {
                    closeAnd(onCancel)
                    true
                }
                KeyEvent.VK_ENTER -> {
                    val rect = selection
                    if (rect != null && ScreenCaptureInteraction.isValidSelection(rect)) {
                        acceptSelection(rect)
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyDispatcher)
        dismissActivePicker = { closeAnd(onCancel) }

        val mouse = object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) {
                if (closed) return
                if (event.button == MouseEvent.BUTTON3 || SwingUtilities.isRightMouseButton(event)) {
                    closeAnd(onCancel)
                    return
                }
                if (event.button != MouseEvent.BUTTON1) return
                val target = ScreenCaptureInteraction.hitTest(event.xOnScreen, event.yOnScreen, selection, capture)
                when (target) {
                    CapturePointerTarget.Confirm -> {
                        selection?.let { acceptSelection(it) }
                    }
                    CapturePointerTarget.Cancel -> closeAnd(onCancel)
                    else -> {
                        val (ix, iy) = ScreenCaptureInteraction.toImagePoint(event.xOnScreen, event.yOnScreen, capture)
                        val started = ScreenCaptureInteraction.beginDrag(
                            target,
                            ix,
                            iy,
                            selection,
                            capture.image.width,
                            capture.image.height,
                        )
                        if (started != null) {
                            drag = started
                            preview = ScreenCaptureInteraction.rectDuringDrag(
                                started,
                                ix,
                                iy,
                                capture.image.width,
                                capture.image.height,
                            )
                            if (started.mode == CaptureDragMode.Create) {
                                selection = null
                            }
                            repaintAll()
                        }
                    }
                }
            }

            override fun mouseReleased(event: MouseEvent) {
                if (closed || event.button != MouseEvent.BUTTON1) return
                val activeDrag = drag ?: return
                val (ix, iy) = ScreenCaptureInteraction.toImagePoint(event.xOnScreen, event.yOnScreen, capture)
                val rect = ScreenCaptureInteraction.rectDuringDrag(
                    activeDrag,
                    ix,
                    iy,
                    capture.image.width,
                    capture.image.height,
                )
                finishDrag(rect)
            }
        }
        val motion = object : MouseMotionAdapter() {
            override fun mouseDragged(event: MouseEvent) {
                val activeDrag = drag ?: return
                if (closed) return
                val (ix, iy) = ScreenCaptureInteraction.toImagePoint(event.xOnScreen, event.yOnScreen, capture)
                preview = ScreenCaptureInteraction.rectDuringDrag(
                    activeDrag,
                    ix,
                    iy,
                    capture.image.width,
                    capture.image.height,
                )
                repaintAll()
            }

            override fun mouseMoved(event: MouseEvent) {
                if (closed) return
                val target = ScreenCaptureInteraction.hitTest(event.xOnScreen, event.yOnScreen, selection, capture)
                val cursor = when (target) {
                    is CapturePointerTarget.Resize -> when (target.handle) {
                        CaptureResizeHandle.Nw, CaptureResizeHandle.Se ->
                            Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR)
                        CaptureResizeHandle.Ne, CaptureResizeHandle.Sw ->
                            Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR)
                    }
                    CapturePointerTarget.Move -> Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                    CapturePointerTarget.Confirm, CapturePointerTarget.Cancel ->
                        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    CapturePointerTarget.Create -> Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
                }
                overlays.forEach { it.cursor = cursor }
            }
        }

        GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.forEach { device ->
            val bounds = device.defaultConfiguration.bounds
            val overlay = RegionOverlayWindow(
                capture,
                bounds,
                translate,
                { currentRect() },
            )
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
    private val translate: (String) -> String,
    private val selection: () -> ImageCropRect?,
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
            null,
        )
        val rect = selection()
        if (rect == null) {
            graphics.color = Color(0, 0, 0, 110)
            graphics.fillRect(0, 0, width, height)
        } else {
            graphics.color = Color(0, 0, 0, 110)
            graphics.fillRect(0, 0, width, height)
            drawSelection(graphics, rect)
        }
        drawHint(graphics, rect != null)
    }

    private fun drawSelection(graphics: Graphics2D, rect: ImageCropRect) {
        val left = rect.x + capture.originX - x
        val top = rect.y + capture.originY - y
        val right = left + rect.width
        val bottom = top + rect.height
        val sx1 = rect.x.coerceIn(0, capture.image.width)
        val sy1 = rect.y.coerceIn(0, capture.image.height)
        val sx2 = (rect.x + rect.width).coerceIn(0, capture.image.width)
        val sy2 = (rect.y + rect.height).coerceIn(0, capture.image.height)
        if (sx2 > sx1 && sy2 > sy1) {
            graphics.drawImage(capture.image, left, top, right, bottom, sx1, sy1, sx2, sy2, null)
        }
        graphics.color = Color.WHITE
        graphics.stroke = BasicStroke(2f)
        graphics.drawRect(left, top, (right - left).coerceAtLeast(1), (bottom - top).coerceAtLeast(1))
        val handles = listOf(
            left to top,
            right to top,
            right to bottom,
            left to bottom,
        )
        graphics.color = Color.WHITE
        for ((hx, hy) in handles) {
            graphics.fillRect(hx - 4, hy - 4, 8, 8)
            graphics.color = Color(0x31, 0x6D, 0xC0)
            graphics.drawRect(hx - 4, hy - 4, 8, 8)
            graphics.color = Color.WHITE
        }
        graphics.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        graphics.drawString("${rect.width} × ${rect.height}", left + 6, top + 16)
        val (cancel, confirm) = ScreenCaptureInteraction.commandButtonBounds(rect, capture)
        drawCommandButton(graphics, cancel, "×", x, y)
        drawCommandButton(graphics, confirm, "✓", x, y)
    }

    private fun drawCommandButton(graphics: Graphics2D, screenRect: Rectangle, label: String, windowX: Int, windowY: Int) {
        val bx = screenRect.x - windowX
        val by = screenRect.y - windowY
        if (bx + screenRect.width < 0 || by + screenRect.height < 0 || bx > width || by > height) return
        graphics.color = Color(0, 0, 0, 160)
        graphics.fillRoundRect(bx, by, screenRect.width, screenRect.height, 6, 6)
        graphics.color = Color.WHITE
        graphics.font = Font(Font.SANS_SERIF, Font.BOLD, 16)
        val metrics = graphics.fontMetrics
        val tx = bx + (screenRect.width - metrics.stringWidth(label)) / 2
        val ty = by + (screenRect.height + metrics.ascent - metrics.descent) / 2
        graphics.drawString(label, tx, ty)
    }

    private fun drawHint(graphics: Graphics2D, hasSelection: Boolean) {
        graphics.color = Color.WHITE
        graphics.font = Font(Font.SANS_SERIF, Font.PLAIN, 14)
        val primary = if (hasSelection) {
            translate("image.captureOverlayHint")
        } else {
            translate("image.captureHint")
        }
        graphics.drawString(primary, 24, 32)
        graphics.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
        graphics.drawString(translate("image.captureOverlayKeys"), 24, 52)
    }
}
