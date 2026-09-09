package com.rememberber.mootool.next.compose.domain

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.pdfbox.text.PDFTextStripper
import java.nio.file.Files
import java.nio.file.Path
import java.util.LinkedHashSet
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

enum class PdfTab { Split, Merge }

enum class PdfSplitRule { Odd, Even, Custom }

enum class PdfTaskStatus { Ready, Running, Done, Error }

data class PdfFileInfo(
    val path: String,
    val name: String,
    val size: Long,
    val pageCount: Int
)

data class PdfOperationResult(
    val outputs: List<String>,
    val pageCount: Int
)

class PdfException(val code: String, message: String) : RuntimeException(message)

object PdfEngine {
    const val MAX_TASKS = 20

    fun parsePageSelection(expression: String, maxPage: Int): List<Int> {
        if (maxPage < 1) throw PdfException("no-pages", "PDF has no pages")
        val value = expression.trim().replace(Regex("[，,]"), ";").replace(Regex("\\s+"), "")
        if (value.isEmpty() || !Regex("^\\d+(?:-\\d+)?(?:;\\d+(?:-\\d+)?)*$").matches(value)) {
            throw PdfException("invalid-range", "Use page ranges such as 1-5;8;10-12")
        }
        val pages = LinkedHashSet<Int>()
        for (token in value.split(';')) {
            val parts = token.split('-')
            val start = parts[0].toInt()
            val end = (parts.getOrNull(1) ?: parts[0]).toInt()
            if (start < 1 || end < start || end > maxPage) {
                throw PdfException("invalid-range", "Page range must stay between 1 and $maxPage")
            }
            for (page in start..end) pages += page
        }
        return pages.toList()
    }

    fun selectSplitPages(pageRange: String, rule: PdfSplitRule, customRule: String, maxPage: Int): List<Int> {
        val candidates = parsePageSelection(pageRange, maxPage)
        return when (rule) {
            PdfSplitRule.Odd -> candidates.filter { it % 2 == 1 }
            PdfSplitRule.Even -> candidates.filter { it % 2 == 0 }
            PdfSplitRule.Custom -> {
                val selected = parsePageSelection(customRule, maxPage).toHashSet()
                candidates.filter { it in selected }
            }
        }
    }

    fun inspect(path: Path): PdfFileInfo {
        ensurePdf(path)
        return open(path).use { document ->
            PdfFileInfo(
                path = path.toAbsolutePath().toString(),
                name = path.name,
                size = path.fileSize(),
                pageCount = document.numberOfPages
            )
        }
    }

    fun splitOutputPath(source: Path): Path =
        source.resolveSibling("${source.nameWithoutExtension.lowercase()}_split.pdf")

    fun split(
        tasks: List<SplitTask>,
        cancelled: () -> Boolean = { false }
    ): PdfOperationResult {
        if (tasks.isEmpty()) throw PdfException("empty-selection", "Select at least one PDF task")
        if (tasks.size > MAX_TASKS) throw PdfException("too-many", "At most $MAX_TASKS PDF tasks")
        val outputs = mutableListOf<String>()
        var pageCount = 0
        try {
            for (task in tasks) {
                if (cancelled()) throw PdfException("cancelled", "Cancelled")
                val sourcePath = Path.of(task.path)
                val pages = open(sourcePath).use { source ->
                    val selected = selectSplitPages(task.pageRange, task.rule, task.customRule, source.numberOfPages)
                    if (selected.isEmpty()) throw PdfException("empty-pages", "No pages selected for ${sourcePath.name}")
                    val outputPath = splitOutputPath(sourcePath)
                    writePages(source, selected, outputPath, cancelled)
                    selected.size to outputPath
                }
                outputs += pages.second.toAbsolutePath().toString()
                pageCount += pages.first
            }
        } catch (error: Exception) {
            if (error is PdfException && error.code == "cancelled") {
                outputs.forEach { Files.deleteIfExists(Path.of(it)) }
            }
            throw error
        }
        return PdfOperationResult(outputs, pageCount)
    }

    fun merge(
        sources: List<MergeSource>,
        outputPath: Path,
        cancelled: () -> Boolean = { false }
    ): PdfOperationResult {
        if (sources.size < 2) throw PdfException("need-two", "Select at least two PDF files")
        if (sources.size > MAX_TASKS) throw PdfException("too-many", "At most $MAX_TASKS PDF files")
        if (cancelled()) throw PdfException("cancelled", "Cancelled")
        val opened = sources.map { open(Path.of(it.path)) }
        try {
            PDDocument().use { destination ->
                var pageCount = 0
                opened.forEachIndexed { index, document ->
                    if (cancelled()) throw PdfException("cancelled", "Cancelled")
                    val pages = parsePageSelection(sources[index].pages, document.numberOfPages)
                    if (pages.isEmpty()) throw PdfException("empty-pages", "No pages selected")
                    pages.forEach { destination.importPage(document.getPage(it - 1)) }
                    pageCount += pages.size
                }
                if (pageCount == 0) throw PdfException("empty-pages", "No pages selected")
                if (cancelled()) throw PdfException("cancelled", "Cancelled")
                outputPath.parent?.let { Files.createDirectories(it) }
                destination.save(outputPath.toFile())
                return PdfOperationResult(listOf(outputPath.toAbsolutePath().toString()), pageCount)
            }
        } finally {
            opened.forEach { runCatching { it.close() } }
        }
    }

    fun extractText(path: Path, startPage: Int, endPage: Int): String = open(path).use { document ->
        val stripper = PDFTextStripper()
        stripper.startPage = startPage
        stripper.endPage = endPage
        stripper.getText(document)
    }

    fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${"%.1f".format(java.util.Locale.US, bytes / 1024.0)} KB"
        else -> "${"%.1f".format(java.util.Locale.US, bytes / (1024.0 * 1024.0))} MB"
    }

    data class SplitTask(
        val path: String,
        val pageRange: String,
        val rule: PdfSplitRule,
        val customRule: String
    )

    data class MergeSource(
        val path: String,
        val pages: String
    )

    private fun writePages(source: PDDocument, pages: List<Int>, outputPath: Path, cancelled: () -> Boolean) {
        PDDocument().use { destination ->
            pages.forEach { page ->
                if (cancelled()) throw PdfException("cancelled", "Cancelled")
                destination.importPage(source.getPage(page - 1))
            }
            if (cancelled()) throw PdfException("cancelled", "Cancelled")
            destination.save(outputPath.toFile())
        }
    }

    private fun ensurePdf(path: Path) {
        if (!path.exists()) throw PdfException("missing", "File not found")
        if (path.extension.lowercase() != "pdf") throw PdfException("not-pdf", "Only PDF files are supported")
    }

    private fun open(path: Path): PDDocument {
        ensurePdf(path)
        return try {
            Loader.loadPDF(path.toFile())
        } catch (error: InvalidPasswordException) {
            throw PdfException("encrypted", "Encrypted PDF files are not supported")
        } catch (error: PdfException) {
            throw error
        } catch (error: Exception) {
            throw PdfException("invalid", error.message ?: "Unable to read PDF")
        }
    }
}
