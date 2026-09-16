package com.rememberber.mootool.next.compose.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToastQueueTest {
    @Test
    fun keepsLatestFourAndDropsBlank() {
        val queue = ToastQueue()
        assertEquals(-1L, queue.show("  "))
        repeat(5) { index -> queue.success("item-$index") }
        val items = queue.items.value
        assertEquals(4, items.size)
        assertEquals(listOf("item-1", "item-2", "item-3", "item-4"), items.map { it.message })
        assertTrue(items.all { it.tone == ToastTone.Success })
    }

    @Test
    fun dismissRemovesOnlyTarget() {
        val queue = ToastQueue()
        val first = queue.info("one")
        queue.error("two")
        queue.dismiss(first)
        val items = queue.items.value
        assertEquals(listOf("two"), items.map { it.message })
        assertEquals(ToastTone.Error, items.single().tone)
    }
}
