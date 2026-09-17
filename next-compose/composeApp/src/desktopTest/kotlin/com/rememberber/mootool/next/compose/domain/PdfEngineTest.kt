package com.rememberber.mootool.next.compose.domain

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField
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
    fun rejectsMoreThanMaxTasksLikeElectronCap() {
        val directory = Files.createTempDirectory("mootool-pdf-cap-")
        try {
            val source = writeNumberedPdf(directory.resolve("Cap.pdf"), 2)
            val tasks = List(PdfEngine.MAX_TASKS + 1) {
                PdfEngine.SplitTask(source.toString(), "1", PdfSplitRule.Odd, "")
            }
            assertFailsWith<PdfException> { PdfEngine.split(tasks) }.also { assertEquals("too-many", it.code) }
            val mergeSources = List(PdfEngine.MAX_TASKS + 1) {
                PdfEngine.MergeSource(source.toString(), "1")
            }
            assertFailsWith<PdfException> {
                PdfEngine.merge(mergeSources, directory.resolve("cap.pdf"))
            }.also { assertEquals("too-many", it.code) }
        } finally {
            directory.listDirectoryEntries().forEach { Files.deleteIfExists(it) }
            Files.deleteIfExists(directory)
        }
    }

    @Test
    fun structuredFixtureExposesWidgetsOnPageAndInAcroForm() {
        val directory = Files.createTempDirectory("mootool-pdf-fixture-")
        try {
            val structured = writeStructuredPdf(directory.resolve("structured.pdf"))
            Loader.loadPDF(structured.toFile()).use { document ->
                assertEquals(2, document.getPage(0).annotations.size)
                assertEquals(2, document.documentCatalog.acroForm.fields.size)
            }
            assertEquals(2, PdfEngine.inspect(structured).formFieldCount)
        } finally {
            directory.listDirectoryEntries().forEach { Files.deleteIfExists(it) }
            Files.deleteIfExists(directory)
        }
    }

    @Test
    fun mergePreservesImportedPageFormAndSignatureWidgets() {
        val directory = Files.createTempDirectory("mootool-pdf-merge-structure-")
        try {
            val structured = writeStructuredPdf(directory.resolve("structured.pdf"))
            val plain = writeNumberedPdf(directory.resolve("plain.pdf"), 1)
            val sourceInfo = PdfEngine.inspect(structured)
            assertEquals(2, sourceInfo.formFieldCount)
            assertEquals(1, sourceInfo.signatureFieldCount)
            val merged = directory.resolve("merged.pdf")
            PdfEngine.merge(
                listOf(
                    PdfEngine.MergeSource(structured.toString(), "1"),
                    PdfEngine.MergeSource(plain.toString(), "1"),
                ),
                merged,
            )
            val mergedInfo = PdfEngine.inspect(merged)
            assertEquals(2, mergedInfo.pageCount)
            assertEquals(
                sourceInfo.formFieldCount,
                mergedInfo.formFieldCount,
                "AcroFormDefaultFixup should register widgets copied with importPage",
            )
            assertEquals(sourceInfo.signatureFieldCount, mergedInfo.signatureFieldCount)
            assertEquals(0, mergedInfo.bookmarkCount, "Page-only merge does not copy document outlines (same as Electron pdf-lib copyPages)")
        } finally {
            directory.listDirectoryEntries().forEach { Files.deleteIfExists(it) }
            Files.deleteIfExists(directory)
        }
    }

    @Test
    fun inspectCountsFormsBookmarksAndSignatureFields() {
        val directory = Files.createTempDirectory("mootool-pdf-structure-")
        try {
            val structured = writeStructuredPdf(directory.resolve("structured.pdf"))
            val info = PdfEngine.inspect(structured)
            assertEquals(2, info.formFieldCount)
            assertEquals(1, info.bookmarkCount)
            assertEquals(1, info.signatureFieldCount)
            assertTrue(info.hasSpecialObjects)
            val split = PdfEngine.split(
                listOf(PdfEngine.SplitTask(structured.toString(), "1", PdfSplitRule.Odd, ""))
            )
            val splitPath = Path.of(split.outputs[0])
            val outputStructure = PdfEngine.analyzeStructure(splitPath)
            assertEquals(1, PdfEngine.inspect(splitPath).pageCount)
            assertEquals(info.formFieldCount, outputStructure.formFieldCount)
            assertEquals(info.signatureFieldCount, outputStructure.signatureFieldCount)
            assertEquals(
                0,
                PdfEngine.inspect(splitPath).bookmarkCount,
                "importPage subset does not copy document outline (Electron pdf-lib copyPages)",
            )
            assertTrue(PdfEngine.ELECTRON_PARITY_OUTLINES_NOT_COPIED)
        } finally {
            directory.listDirectoryEntries().forEach { Files.deleteIfExists(it) }
            Files.deleteIfExists(directory)
        }
    }

    @Test
    fun inspectRejectsEncryptedPdfWithStableCode() {
        val directory = Files.createTempDirectory("mootool-pdf-encrypted-")
        try {
            val encrypted = writeEncryptedPdf(directory.resolve("locked.pdf"))
            assertFailsWith<PdfException> { PdfEngine.inspect(encrypted) }.also { assertEquals("encrypted", it.code) }
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

    private fun writeStructuredPdf(path: Path): Path {
        PDDocument().use { document ->
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)
            val outline = PDDocumentOutline()
            document.documentCatalog.documentOutline = outline
            PDOutlineItem().also { item ->
                item.title = "Section"
                outline.addLast(item)
            }
            val acroForm = PDAcroForm(document)
            document.documentCatalog.acroForm = acroForm
            PDTextField(acroForm).also { field ->
                field.partialName = "name"
                acroForm.fields.add(field)
                attachFieldWidget(field, page, PDRectangle(50f, 650f, 250f, 30f))
            }
            PDSignatureField(acroForm).also { field ->
                field.partialName = "sig"
                acroForm.fields.add(field)
                attachFieldWidget(field, page, PDRectangle(50f, 600f, 250f, 36f))
            }
            document.save(path.toFile())
        }
        return path
    }

    private fun attachFieldWidget(
        field: org.apache.pdfbox.pdmodel.interactive.form.PDTerminalField,
        page: PDPage,
        rectangle: PDRectangle,
    ) {
        val widget = field.widgets.firstOrNull() ?: PDAnnotationWidget().also { field.widgets = listOf(it) }
        widget.rectangle = rectangle
        widget.page = page
        if (!page.annotations.contains(widget)) {
            page.annotations.add(widget)
        }
    }

    private fun writeEncryptedPdf(path: Path): Path {
        PDDocument().use { document ->
            document.addPage(PDPage(PDRectangle.A4))
            val policy = StandardProtectionPolicy("owner", "user", AccessPermission())
            policy.encryptionKeyLength = 128
            document.protect(policy)
            document.save(path.toFile())
        }
        return path
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
