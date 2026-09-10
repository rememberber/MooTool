package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.app.AppPaths
import com.rememberber.mootool.next.compose.storage.NoteVault
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class MarkdownPreviewEngineTest {
    @Test
    fun parsesHeadingsListsTablesTasksAndCode() {
        val markdown = """
            # Title
            - [ ] open
            - [x] done
            1. first
            ```java
            class A {}
            ```
            | A | B |
            | --- | --- |
            | 1 | 2 |

            [link](https://example.com)

            ![img](attachments/pixel.png)
        """.trimIndent()
        val dump = MarkdownPreviewEngine.dump(MarkdownPreviewEngine.parse(markdown))
        assertTrue(dump.contains("H1 Title"), dump)
        assertTrue(dump.contains("LI [ ] P open") || dump.contains("LI [ ] open"), dump)
        assertTrue(dump.contains("LI [x] P done") || dump.contains("LI [x] done"), dump)
        assertTrue(dump.contains("OL"), dump)
        assertTrue(dump.contains("CODE java"), dump)
        assertTrue(dump.contains("class A {}"), dump)
        assertTrue(dump.contains("TABLE A|B / 1|2"), dump)
        assertTrue(dump.contains("[LINK https://example.com link]"), dump)
        assertTrue(dump.contains("[IMG attachments/pixel.png]"), dump)
    }

    @Test
    fun keepsRawHtmlAndScriptsAsTextNotExecutable() {
        val dump = MarkdownPreviewEngine.dump(
            MarkdownPreviewEngine.parse("<script>alert(1)</script>\n<p onclick=\"x()\">hi</p>")
        )
        assertTrue(dump.contains("HTML"), dump)
        assertTrue(dump.contains("<script>alert(1)</script>"), dump)
        assertTrue(dump.contains("onclick"), dump)
        assertTrue(!dump.contains("EXECUTE"), dump)
    }

    @Test
    fun classifiesRemoteUnsafeAndLocalImagesWithoutNetwork() {
        val root = createTempDirectory("md-preview-images")
        val vault = NoteVault(AppPaths.resolve(root.toString()).also { it.ensureCreated() })
        vault.writeBytes("attachments/pixel.png", byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))
        assertEquals(MarkdownImageKind.Remote, MarkdownPreviewEngine.classifyImage("https://example.com/a.png", vault))
        assertEquals(MarkdownImageKind.Remote, MarkdownPreviewEngine.classifyImage("//cdn.example/a.png", vault))
        assertEquals(MarkdownImageKind.Unsafe, MarkdownPreviewEngine.classifyImage("javascript:alert(1)", vault))
        assertEquals(MarkdownImageKind.Unsafe, MarkdownPreviewEngine.classifyImage("../secret.png", vault))
        assertEquals(MarkdownImageKind.Local, MarkdownPreviewEngine.classifyImage("attachments/pixel.png", vault))
        assertEquals(MarkdownImageKind.Missing, MarkdownPreviewEngine.classifyImage("attachments/missing.png", vault))
        assertTrue(vault.resolve("attachments/pixel.png").exists())
    }
}
