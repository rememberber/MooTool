package com.rememberber.mootool.next.compose.domain

import java.awt.Rectangle

/** Pointer target on the region-screenshot overlay (aligns with Electron `data-capture-drag` / command buttons). */
sealed class CapturePointerTarget {
    data object Create : CapturePointerTarget()

    data object Move : CapturePointerTarget()

    data class Resize(val handle: CaptureResizeHandle) : CapturePointerTarget()

    data object Confirm : CapturePointerTarget()

    data object Cancel : CapturePointerTarget()
}

enum class CaptureDragMode {
    Create,
    Move,
    Nw,
    Ne,
    Se,
    Sw,
}

data class ActiveCaptureDrag(
    val mode: CaptureDragMode,
    val startImageX: Int,
    val startImageY: Int,
    val baseRect: ImageCropRect,
)

object ScreenCaptureInteraction {
    const val HANDLE_HIT_PX = 12
    const val COMMAND_BUTTON_PX = 26
    private const val MIN_SELECTION_PX = 2

    fun toImagePoint(screenX: Int, screenY: Int, capture: ScreenCapture): Pair<Int, Int> {
        val width = capture.image.width
        val height = capture.image.height
        val x = (screenX - capture.originX).coerceIn(0, width)
        val y = (screenY - capture.originY).coerceIn(0, height)
        return x to y
    }

    fun screenRect(selection: ImageCropRect, capture: ScreenCapture): Rectangle =
        Rectangle(
            selection.x + capture.originX,
            selection.y + capture.originY,
            selection.width,
            selection.height,
        )

    fun commandButtonBounds(selection: ImageCropRect, capture: ScreenCapture): Pair<Rectangle, Rectangle> {
        val sr = screenRect(selection, capture)
        val pad = 6
        val gap = 6
        val size = COMMAND_BUTTON_PX
        val confirm = Rectangle(sr.x + sr.width - pad - size, sr.y + sr.height - pad - size, size, size)
        val cancel = Rectangle(confirm.x - gap - size, confirm.y, size, size)
        return cancel to confirm
    }

    fun hitTest(screenX: Int, screenY: Int, selection: ImageCropRect?, capture: ScreenCapture): CapturePointerTarget {
        if (selection != null) {
            val (cancel, confirm) = commandButtonBounds(selection, capture)
            if (confirm.contains(screenX, screenY)) return CapturePointerTarget.Confirm
            if (cancel.contains(screenX, screenY)) return CapturePointerTarget.Cancel
            val sr = screenRect(selection, capture)
            val corners = listOf(
                CaptureResizeHandle.Nw to Pair(sr.x, sr.y),
                CaptureResizeHandle.Ne to Pair(sr.x + sr.width, sr.y),
                CaptureResizeHandle.Se to Pair(sr.x + sr.width, sr.y + sr.height),
                CaptureResizeHandle.Sw to Pair(sr.x, sr.y + sr.height),
            )
            for ((handle, corner) in corners) {
                if (kotlin.math.abs(screenX - corner.first) <= HANDLE_HIT_PX &&
                    kotlin.math.abs(screenY - corner.second) <= HANDLE_HIT_PX
                ) {
                    return CapturePointerTarget.Resize(handle)
                }
            }
            if (sr.contains(screenX, screenY)) return CapturePointerTarget.Move
        }
        return CapturePointerTarget.Create
    }

    fun dragModeFor(target: CapturePointerTarget): CaptureDragMode? =
        when (target) {
            CapturePointerTarget.Create -> CaptureDragMode.Create
            CapturePointerTarget.Move -> CaptureDragMode.Move
            is CapturePointerTarget.Resize -> when (target.handle) {
                CaptureResizeHandle.Nw -> CaptureDragMode.Nw
                CaptureResizeHandle.Ne -> CaptureDragMode.Ne
                CaptureResizeHandle.Se -> CaptureDragMode.Se
                CaptureResizeHandle.Sw -> CaptureDragMode.Sw
            }
            CapturePointerTarget.Confirm, CapturePointerTarget.Cancel -> null
        }

    fun beginDrag(
        target: CapturePointerTarget,
        imageX: Int,
        imageY: Int,
        selection: ImageCropRect?,
        imageWidth: Int,
        imageHeight: Int,
    ): ActiveCaptureDrag? {
        val mode = dragModeFor(target) ?: return null
        val baseRect = when (mode) {
            CaptureDragMode.Create ->
                selection ?: ImageEngine.captureRectFromPoints(imageX, imageY, imageX, imageY, imageWidth, imageHeight)
            else -> selection ?: return null
        }
        return ActiveCaptureDrag(mode, imageX, imageY, baseRect)
    }

    fun rectDuringDrag(
        drag: ActiveCaptureDrag,
        imageX: Int,
        imageY: Int,
        imageWidth: Int,
        imageHeight: Int,
    ): ImageCropRect {
        val deltaX = imageX - drag.startImageX
        val deltaY = imageY - drag.startImageY
        return when (drag.mode) {
            CaptureDragMode.Create ->
                ImageEngine.captureRectFromPoints(
                    drag.startImageX,
                    drag.startImageY,
                    imageX,
                    imageY,
                    imageWidth,
                    imageHeight,
                )
            CaptureDragMode.Move ->
                ImageEngine.moveCaptureRect(drag.baseRect, deltaX, deltaY, imageWidth, imageHeight)
            CaptureDragMode.Nw ->
                ImageEngine.resizeCaptureRect(
                    drag.baseRect,
                    CaptureResizeHandle.Nw,
                    deltaX,
                    deltaY,
                    imageWidth,
                    imageHeight,
                )
            CaptureDragMode.Ne ->
                ImageEngine.resizeCaptureRect(
                    drag.baseRect,
                    CaptureResizeHandle.Ne,
                    deltaX,
                    deltaY,
                    imageWidth,
                    imageHeight,
                )
            CaptureDragMode.Se ->
                ImageEngine.resizeCaptureRect(
                    drag.baseRect,
                    CaptureResizeHandle.Se,
                    deltaX,
                    deltaY,
                    imageWidth,
                    imageHeight,
                )
            CaptureDragMode.Sw ->
                ImageEngine.resizeCaptureRect(
                    drag.baseRect,
                    CaptureResizeHandle.Sw,
                    deltaX,
                    deltaY,
                    imageWidth,
                    imageHeight,
                )
        }
    }

    fun isValidSelection(rect: ImageCropRect): Boolean =
        rect.width >= MIN_SELECTION_PX && rect.height >= MIN_SELECTION_PX
}
