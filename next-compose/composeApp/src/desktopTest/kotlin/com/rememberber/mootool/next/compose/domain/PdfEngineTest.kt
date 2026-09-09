package com.rememberber.mootool.next.compose.domain

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PdfEngineTest {
    @Test
    fun parsesRangesAndKeepsSourceOrder() {
        assertEquals(listOf(1, 2, 3, 7, 9, 10), PdfEngine.parsePageSelection("1-3;2;7;9-10", 10))
        assertEquals(listOf(1, 2, 4, 6), PdfEngine.parsePageSelection("1-2, 4，6", 10))
        assertFailsWith<PdfException> { PdfEngine.parsePageSelection("3-1", 5) }
        assertFailsWith<PdfException> { PdfEngine.parsePageSelection("1;;2", 5) }
        assertFailsWith<PdfException> { PdfEngine.parsePageSelection("1-6", 5) }
        assertEquals(listOf(3, 5, 7), PdfEngine.selectSplitPages("2-8", PdfSplitRule.Odd, "", 10))
        assertEquals(listOf(2, 4, 6, 8), PdfEngine.selectSplitPages("2-8", PdfSplitRule.Even, "", 10))
        assertEquals(listOf(3, 4, 8), PdfEngine.selectSplitPages("2-8", PdfSplitRule.Custom, "1;3-4;8-10", 10))
    }

    @Test
    fun inspectsMergesAndSplitsRealPdfs() {
        val directory = Files.createTempDirectory("mootool-pdf-")
        try {
            val first = writeNumberedPdf(directory.resolve("First.pdf"), 4)
            val second = writeNumberedPdf(directory.resolve("Second.pdf"), 3)
            assertEquals(4, PdfEngine.inspect(first).pageCount)
            val merged = directory.resolve("merge.pdf")
            val merge = PdfEngine.merge(
                listOf(
                    PdfEngine.MergeSource(first.toString(), "2-3"),
                    PdfEngine.MergeSource(second.toString(), "1,3")
                ),
                merged
            )
            assertEquals(4, merge.pageCount)
            assertEquals(listOf(merged.toAbsolutePath().toString()), merge.outputs)
            assertTrue(PdfEngine.extractText(merged, 1, 1).contains("Page 2"))
            assertTrue(PdfEngine.extractText(merged, 4, 4).contains("Page 3"))

            val source = writeNumberedPdf(directory.resolve("Quarterly.PDF"), 6)
            val split = PdfEngine.split(
                listOf(PdfEngine.SplitTask(source.toString(), "1-6", PdfSplitRule.Odd, ""))
            )
            assertEquals(3, split.pageCount)
            assertEquals(directory.resolve("quarterly_split.pdf").toAbsolutePath().toString(), split.outputs[0])
            assertEquals(3, PdfEngine.inspect(Path.of(split.outputs[0])).pageCount)
            assertTrue(PdfEngine.extractText(Path.of(split.outputs[0]), 1, 1).contains("Page 1"))
            assertTrue(PdfEngine.extractText(Path.of(split.outputs[0]), 3, 3).contains("Page 5"))
        } finally {
            directory.listDirectoryEntries().forEach { Files.deleteIfExists(it) }
            Files.deleteIfExists(directory)
        }
    }

    @Test
    fun rejectsUnsupportedFilesAndCancelsWithoutOrphans() {
        val directory = Files.createTempDirectory("mootool-pdf-reject-")
        try {
            val text = directory.resolve("not-a-pdf.txt")
            Files.writeString(text, "hello")
            assertFailsWith<PdfException> { PdfEngine.inspect(text) }.also { assertEquals("not-pdf", it.code) }
            assertFailsWith<PdfException> { PdfEngine.merge(emptyList(), directory.resolve("unused.pdf")) }.also {
                assertEquals("need-two", it.code)
            }
            val first = writeNumberedPdf(directory.resolve("A.pdf"), 2)
            val second = writeNumberedPdf(directory.resolve("B.pdf"), 2)
            val cancelled = java.util.concurrent.atomic.AtomicBoolean(true)
            assertFailsWith<PdfException> {
                PdfEngine.merge(
                    listOf(PdfEngine.MergeSource(first.toString(), "1"), PdfEngine.MergeSource(second.toString(), "1")),
                    directory.resolve("cancelled.pdf"),
                    cancelled = { cancelled.get() }
                )
            }.also { assertEquals("cancelled", it.code) }
            assertTrue(directory.listDirectoryEntries().none { it.fileName.toString() == "cancelled.pdf" })
        } finally {
            directory.listDirectoryEntries().forEach { Files.deleteIfExists(it) }
            Files.deleteIfExists(directory)
        }
    }

    private fun writeNumberedPdf(path: Path, pageCount: Int): Path {
        PDDocument().use { document ->
            val font = PDType1Font(Standard14Fonts.FontName.HELVETICA)
            repeat(pageCount) { index ->
                val page = PDPage(PDRectangle(320f + index, 480f + index))
                document.addPage(page)
                PDPageContentStream(document, page).use { stream ->
                    stream.beginText()
                    stream.setFont(font, 18f)
                    stream.newLineAtOffset(40f, 200f)
                    stream.showText("Page ${index + 1}")
                    stream.endText()
                }
            }
            document.save(path.toFile())
        }
        return path
    }
}
