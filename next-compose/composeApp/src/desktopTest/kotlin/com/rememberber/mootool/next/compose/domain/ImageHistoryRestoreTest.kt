package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.HistoryRecord
import kotlin.test.Test
import kotlin.test.assertEquals

class ImageHistoryRestoreTest {
    @Test
    fun prefersAssetNameFromInputWhenPresentInLibrary() {
        val item = HistoryRecord(
            toolId = "image",
            operation = ImageHistoryMetadata.OP_PROCESS,
            summary = "done",
            input = "a.png,b.png",
            output = "/tmp/out.png",
            createdAt = "",
        )
        val plan = ImageHistoryRestore.plan(item, setOf("b.png", "c.png"))
        assertEquals(listOf("/tmp/out.png"), plan.lastOutputs)
        assertEquals("b.png", plan.preferredAsset)
    }
}
