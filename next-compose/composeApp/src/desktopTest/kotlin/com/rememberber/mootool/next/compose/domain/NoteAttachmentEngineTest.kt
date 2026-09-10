package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.storage.NoteVault
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class NoteAttachmentEngineTest {
    @Test
    fun insertsMarkdownOnOwnLineLikeElectron() {
        val markdown = "![image](attachments/pixel.png)"
        assertEquals(
            MarkdownImageInsertion(6, 6, "\n$markdown\n", 6 + markdown.length + 2),
            NoteAttachmentEngine.prepareInsertion("beforeafter", 6, 6, markdown)
        )
        assertEquals(
            MarkdownImageInsertion(7, 7, "$markdown\n", 7 + markdown.length + 1),
            NoteAttachmentEngine.prepareInsertion("before\nafter", 7, 7, markdown)
        )
        assertEquals(
            MarkdownImageInsertion(0, 0, markdown, markdown.length),
            NoteAttachmentEngine.prepareInsertion("", 20, 40, markdown)
        )
        assertEquals(setOf("attachments/pixel.png"), NoteAttachmentEngine.extractPaths("see ![image](attachments/pixel.png)"))
    }

    @Test
    fun storesUniqueRelativePngAndRefusesOverwrite() {
        val root = createTempDirectory("note-attach-store")
        val vault = NoteVault(AppPaths.resolve(root.toString()).also { it.ensureCreated() })
        val clock = Clock.fixed(Instant.parse("2026-09-09T14:53:00Z"), ZoneOffset.UTC)
        val first = NoteAttachmentEngine.store(vault, byteArrayOf(1, 2, 3), "png", clock) { "abcd1234" }
        assertEquals("attachments/20260909145300_abcd1234.png", first.relativePath)
        assertEquals("![image](attachments/20260909145300_abcd1234.png)", first.markdown)
        assertEquals(byteArrayOf(1, 2, 3).toList(), vault.resolve(first.relativePath).readBytes().toList())
        val second = NoteAttachmentEngine.store(vault, byteArrayOf(4, 5), "png", clock, countingIds("abcd1234", "eeeeffff"))
        assertEquals("attachments/20260909145300_eeeeffff.png", second.relativePath)
        assertTrue(vault.resolve(first.relativePath).exists())
        assertTrue(vault.resolve(second.relativePath).exists())
        assertFails { NoteAttachmentEngine.store(vault, ByteArray(0), "png", clock) }
        assertFails { NoteAttachmentEngine.store(vault, ByteArray(3), "exe", clock) }
    }

    @Test
    fun refusesToDeleteReferencedAttachments() {
        val root = createTempDirectory("note-attach-ref")
        val vault = NoteVault(AppPaths.resolve(root.toString()).also { it.ensureCreated() })
        val clock = Clock.fixed(Instant.parse("2026-09-09T14:53:00Z"), ZoneOffset.UTC)
        val stored = NoteAttachmentEngine.store(vault, byteArrayOf(9), "png", clock) { "deadbeef" }
        vault.write("note.md", "see ${stored.markdown}")
        val orphan = NoteAttachmentEngine.store(vault, byteArrayOf(8), "png", clock, countingIds("deadbeef", "cafef00d"))
        assertEquals(listOf(orphan.relativePath), NoteAttachmentEngine.unreferenced(vault))
        assertFails { NoteAttachmentEngine.deleteIfUnreferenced(vault, stored.relativePath) }
        assertTrue(vault.resolve(stored.relativePath).exists())
        NoteAttachmentEngine.deleteIfUnreferenced(vault, orphan.relativePath)
        assertTrue(!vault.resolve(orphan.relativePath).exists())
    }
}

private fun countingIds(vararg ids: String): () -> String {
    var index = 0
    return {
        val value = ids[index.coerceAtMost(ids.lastIndex)]
        index += 1
        value
    }
}
