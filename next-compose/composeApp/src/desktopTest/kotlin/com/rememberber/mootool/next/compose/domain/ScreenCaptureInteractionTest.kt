package com.rememberber.mootool.next.compose.domain

import java.awt.Rectangle
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScreenCaptureInteractionTest {
    private val capture = ScreenCapture(
        originX = 100,
        originY = 50,
        image = BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB),
    )

    @Test
    fun hitTest_prioritizesConfirmCancelAndHandles() {
        val selection = ImageCropRect(200, 100, 400, 300)
        val (cancel, confirm) = ScreenCaptureInteraction.commandButtonBounds(selection, capture)
        assertEquals(
            CapturePointerTarget.Confirm,
            ScreenCaptureInteraction.hitTest(confirm.x + confirm.width / 2, confirm.y + confirm.height / 2, selection, capture),
        )
        assertEquals(
            CapturePointerTarget.Cancel,
            ScreenCaptureInteraction.hitTest(cancel.x + cancel.width / 2, cancel.y + cancel.height / 2, selection, capture),
        )
        val sr = ScreenCaptureInteraction.screenRect(selection, capture)
        assertEquals(
            CapturePointerTarget.Resize(CaptureResizeHandle.Nw),
            ScreenCaptureInteraction.hitTest(sr.x, sr.y, selection, capture),
        )
        assertEquals(
            CapturePointerTarget.Move,
            ScreenCaptureInteraction.hitTest(sr.x + sr.width / 2, sr.y + sr.height / 2, selection, capture),
        )
        assertEquals(CapturePointerTarget.Create, ScreenCaptureInteraction.hitTest(50, 50, selection, capture))
    }

    @Test
    fun dragMoveAndResize_useImageEngineGeometry() {
        val width = capture.image.width
        val height = capture.image.height
        val base = ImageCropRect(100, 100, 500, 300)
        val moveDrag = ScreenCaptureInteraction.beginDrag(
            CapturePointerTarget.Move,
            150,
            160,
            base,
            width,
            height,
        )!!
        assertEquals(
            ImageEngine.moveCaptureRect(base, 50, 40, width, height),
            ScreenCaptureInteraction.rectDuringDrag(moveDrag, 200, 200, width, height),
        )
        val resizeDrag = ScreenCaptureInteraction.beginDrag(
            CapturePointerTarget.Resize(CaptureResizeHandle.Se),
            600,
            400,
            base,
            width,
            height,
        )!!
        assertEquals(
            ImageEngine.resizeCaptureRect(base, CaptureResizeHandle.Se, 100, 80, width, height),
            ScreenCaptureInteraction.rectDuringDrag(resizeDrag, 700, 480, width, height),
        )
    }

    @Test
    fun createDrag_matchesCaptureRectFromPoints() {
        val width = capture.image.width
        val height = capture.image.height
        val drag = ScreenCaptureInteraction.beginDrag(
            CapturePointerTarget.Create,
            10,
            20,
            null,
            width,
            height,
        )!!
        val rect = ScreenCaptureInteraction.rectDuringDrag(drag, 110, 220, width, height)
        assertEquals(
            ImageEngine.captureRectFromPoints(10, 20, 110, 220, width, height),
            rect,
        )
        assertTrue(ScreenCaptureInteraction.isValidSelection(rect))
    }

    @Test
    fun tinySelectionIsInvalid() {
        assertFalse(ScreenCaptureInteraction.isValidSelection(ImageCropRect(0, 0, 1, 1)))
        assertTrue(ScreenCaptureInteraction.isValidSelection(ImageCropRect(0, 0, 2, 2)))
    }
}
