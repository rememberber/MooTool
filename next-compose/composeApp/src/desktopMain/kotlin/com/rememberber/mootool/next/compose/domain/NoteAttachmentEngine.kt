package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.storage.NoteVault
import java.time.Clock
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.io.path.exists

data class StoredNoteAttachment(
    val relativePath: String,
    val markdown: String
)

data class MarkdownImageInsertion(
    val start: Int,
    val end: Int,
    val text: String,
    val caret: Int
)

object NoteAttachmentEngine {
    const val MAX_BYTES = 20 * 1024 * 1024
    val extensions: Set<String> = setOf("png", "jpg", "jpeg", "gif", "bmp", "webp")
    private val pathPattern = Regex("""(?:\(|src=["'])(attachments/[A-Za-z0-9_.-]+)(?:\)|["'])""")
    private val timestamp: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC)
    private val noteScanExtensions = setOf("md", "txt", "markdown", "json", "java", "js", "ts", "py", "xml", "yaml", "yml", "sql")

    fun extractPaths(content: String): Set<String> =
        pathPattern.findAll(content).map { it.groupValues[1] }.toSet()

    fun prepareInsertion(content: String, requestedStart: Int, requestedEnd: Int, markdown: String): MarkdownImageInsertion {
        val start = requestedStart.coerceIn(0, content.length)
        val end = requestedEnd.coerceAtLeast(start).coerceAtMost(content.length)
        val leadingBreak = if (start > 0 && content[start - 1] != '\n') "\n" else ""
        val trailingBreak = if (end < content.length && content[end] != '\n') "\n" else ""
        val text = "$leadingBreak$markdown$trailingBreak"
        return MarkdownImageInsertion(start, end, text, start + text.length)
    }

    fun store(
        vault: NoteVault,
        bytes: ByteArray,
        extension: String,
        clock: Clock = Clock.systemUTC(),
        idFactory: () -> String = { UUID.randomUUID().toString().replace("-", "").take(8) }
    ): StoredNoteAttachment {
        val ext = normalizeExtension(extension)
        check(bytes.isNotEmpty()) { "Attachment is empty" }
        check(bytes.size <= MAX_BYTES) { "Attachment exceeds 20 MB limit" }
        val stamp = timestamp.format(clock.instant())
        repeat(8) {
            val relativePath = "attachments/${stamp}_${idFactory()}.$ext"
            if (!vault.resolve(relativePath).exists()) {
                vault.writeBytes(relativePath, bytes)
                return StoredNoteAttachment(relativePath, "![image]($relativePath)")
            }
        }
        error("Unable to allocate unique attachment name")
    }

    fun referencedPaths(vault: NoteVault): Set<String> =
        vault.list().asSequence()
            .filter { !it.directory && noteScanExtensions.contains(it.name.substringAfterLast('.', "").lowercase(Locale.ROOT)) }
            .flatMap { extractPaths(runCatching { vault.read(it.relativePath) }.getOrDefault("")) }
            .toSet()

    fun unreferenced(vault: NoteVault): List<String> {
        val referenced = referencedPaths(vault)
        return vault.list()
            .filter { !it.directory && isAttachmentPath(it.relativePath) }
            .map { it.relativePath }
            .filter { it !in referenced }
            .sorted()
    }

    fun deleteIfUnreferenced(vault: NoteVault, relativePath: String) {
        check(isAttachmentPath(relativePath)) { "Not an attachment path: $relativePath" }
        check(relativePath !in referencedPaths(vault)) { "Attachment is still referenced: $relativePath" }
        vault.delete(relativePath)
    }

    fun isAttachmentPath(relativePath: String): Boolean {
        val normalized = relativePath.replace('\\', '/')
        val ext = normalized.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return normalized.startsWith("attachments/") &&
            normalized.split('/').size == 2 &&
            extensions.contains(ext)
    }

    fun normalizeExtension(extension: String): String {
        val ext = extension.trim().lowercase(Locale.ROOT).removePrefix(".")
        val mapped = if (ext == "jpeg") "jpg" else ext
        check(extensions.contains(mapped) || extensions.contains(ext)) { "Unsupported image attachment" }
        return mapped
    }
}
