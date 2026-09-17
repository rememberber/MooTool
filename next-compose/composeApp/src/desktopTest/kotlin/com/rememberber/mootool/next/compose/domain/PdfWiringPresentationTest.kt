package com.rememberber.mootool.next.compose.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PdfWiringPresentationTest {
    @Test
    fun limitsAddTaskAtMaxTasks() {
        assertTrue(PdfWiringPresentation.limitReached(PdfTab.Split, splitCount = 20, mergeCount = 0))
        assertFalse(PdfWiringPresentation.canAddTask(busy = false, PdfTab.Merge, splitCount = 0, mergeCount = 20))
    }

    @Test
    fun mergeRequiresTwoSelected() {
        assertFalse(PdfWiringPresentation.canStartMerge(busy = false, selectedCount = 1))
        assertTrue(PdfWiringPresentation.canStartMerge(busy = false, selectedCount = 2))
    }

    @Test
    fun ingestRemainingSlotsRespectsTab() {
        assertEquals(5, PdfWiringPresentation.ingestRemainingSlots(PdfTab.Split, splitCount = 15, mergeCount = 0))
    }
}
