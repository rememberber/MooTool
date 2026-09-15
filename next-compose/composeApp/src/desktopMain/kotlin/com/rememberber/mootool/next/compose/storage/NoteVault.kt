package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppDirectories
import com.rememberber.mootool.next.compose.domain.NoteFrontmatter
import com.rememberber.mootool.next.compose.domain.NoteMetadata
import com.rememberber.mootool.next.compose.domain.VaultSearchIndex
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

data class NoteDocument(
    val relativePath: String,
    val content: String,
    val metadata: NoteMetadata
)

class NoteVault(
    private val directories: AppDirectories,
    private val customRoot: String = ""
) {
    fun root(): Path {
        val configured = customRoot.trim()
        val path = if (configured.isEmpty()) directories.quickNoteVault else Path.of(configured)
        return path.apply { createDirectories() }
    }

    fun snapshot(hideIgnored: Boolean = true): List<VaultIndexRecord> {
        val base = root().toAbsolutePath().normalize().let { if (it.exists()) it.toRealPath() else it }
        return VaultFiles.walk(
            base,
            VaultListOptions("", includeContent = false, hideIgnored = hideIgnored),
            allowedFile = { path -> path.extension.lowercase() in NoteFrontmatter.noteExtensions }
        ) { _, _ -> false }.map { entry ->
            if (entry.directory) {
                VaultIndexRecord(entry)
            } else {
                val parsed = runCatching {
                    val fallback = entry.name.substringBeforeLast('.', entry.name)
                    NoteFrontmatter.parse(root().resolve(entry.relativePath).readText(Charsets.UTF_8), fallback)
                }.getOrNull()
                VaultIndexRecord(
                    entry = entry.copy(
                        color = parsed?.metadata?.color.orEmpty(),
                        createdAt = parsed?.metadata?.createdAt?.ifBlank { entry.createdAt } ?: entry.createdAt,
                        modifiedAt = parsed?.metadata?.modifiedAt?.ifBlank { entry.modifiedAt } ?: entry.modifiedAt
                    ),
                    title = parsed?.metadata?.title.orEmpty(),
                    content = parsed?.content.orEmpty()
                )
            }
        }
    }

    fun list(
        query: String = "",
        includeContent: Boolean = true,
        hideIgnored: Boolean = true
    ): List<VaultEntry> = VaultSearchIndex.filter(snapshot(hideIgnored), query, includeContent)

    fun listAttachments(): List<String> {
        val folder = root().resolve("attachments")
        if (!folder.exists()) return emptyList()
        return Files.walk(folder).use { stream ->
            stream.filter { Files.isRegularFile(it) }
                .map { VaultFiles.portable(it, root()) }
                .sorted()
                .toList()
        }
    }

    fun read(relativePath: String): String = resolve(relativePath).readText(Charsets.UTF_8)

    fun readOrNull(relativePath: String): String? {
        val target = resolve(relativePath)
        return if (Files.isRegularFile(target)) target.readText(Charsets.UTF_8) else null
    }

    fun readNote(relativePath: String): NoteDocument {
        val target = resolve(relativePath)
        val attrs = Files.readAttributes(target, BasicFileAttributes::class.java)
        val created = Instant.ofEpochMilli(attrs.creationTime().toMillis()).toString()
        val modified = Instant.ofEpochMilli(attrs.lastModifiedTime().toMillis()).toString()
        val parsed = NoteFrontmatter.parse(target.readText(Charsets.UTF_8), target.name.substringBeforeLast('.'), created, modified)
        return NoteDocument(relativePath.replace('\\', '/'), parsed.content, parsed.metadata)
    }

    fun write(relativePath: String, content: String): Path {
        val target = resolve(relativePath)
        target.parent.createDirectories()
        val temp = target.resolveSibling(".${target.name}.tmp")
        temp.writeText(content, Charsets.UTF_8)
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        return target
    }

    fun writeBytes(relativePath: String, bytes: ByteArray): Path {
        val target = resolve(relativePath)
        check(!target.exists()) { "File already exists: $relativePath" }
        target.parent.createDirectories()
        val temp = target.resolveSibling(".${target.name}.tmp")
        Files.write(temp, bytes)
        Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE)
        return target
    }

    fun saveNote(relativePath: String, content: String, metadata: NoteMetadata): NoteDocument {
        check(content.toByteArray(Charsets.UTF_8).size <= VaultFiles.MAX_FILE_SIZE) { "Quick Note exceeds 20 MB limit" }
        val sourcePath = relativePath.replace('\\', '/')
        val nextPath = NoteFrontmatter.withNoteExtension(sourcePath, metadata.syntax.ifBlank { "text/plain" })
        val source = resolve(sourcePath)
        val target = resolve(nextPath)
        if (source != target && source.exists()) {
            check(!target.exists()) { "An entry with that name already exists" }
            Files.move(source, target)
        }
        val fallback = target.name.substringBeforeLast('.')
        val stored = NoteFrontmatter.normalize(metadata, fallback)
        write(nextPath, NoteFrontmatter.serialize(stored, content))
        return NoteDocument(nextPath, content, stored)
    }

    fun createFile(relativePath: String, content: String = ""): Path {
        val target = resolve(relativePath)
        check(!target.exists()) { "File already exists: $relativePath" }
        val title = target.name.substringBeforeLast('.')
        val metadata = NoteMetadata.defaults(NoteFrontmatter.sanitizeName(title)).copy(
            syntax = NoteFrontmatter.syntaxForExtension(target.extension)
        )
        return write(relativePath, NoteFrontmatter.serialize(metadata, content))
    }

    fun createNote(title: String, parentPath: String = "", fontName: String = "", fontSize: Int = 14, lineWrap: Boolean = true): NoteDocument {
        val parent = parentPath.trim().trim('/')
        if (parent.isNotEmpty()) resolve(parent).createDirectories()
        val name = NoteFrontmatter.sanitizeName(title)
        val directory = if (parent.isEmpty()) root() else resolve(parent)
        val relative = listOf(parent, VaultFiles.uniqueChild(directory, name, ".txt")).filter { it.isNotBlank() }.joinToString("/")
        val metadata = NoteMetadata.defaults(NoteFrontmatter.normalizeTitle(title)).copy(
            fontName = fontName,
            fontSize = NoteFrontmatter.clampFontSize(fontSize.toDouble()),
            lineWrap = lineWrap
        )
        return saveNote(relative, "", metadata)
    }

    fun createDirectory(relativePath: String): Path {
        val target = resolve(relativePath)
        check(!target.exists()) { "Path already exists: $relativePath" }
        target.createDirectories()
        return target
    }

    fun rename(relativePath: String, nextName: String): String {
        val source = resolve(relativePath)
        val sanitized = NoteFrontmatter.sanitizeName(nextName.substringBeforeLast('.'))
        val extension = if (source.isDirectory()) "" else {
            val ext = source.extension
            if (ext.isBlank()) "" else ".$ext"
        }
        val target = source.resolveSibling(sanitized + extension)
        val wasDirectory = source.isDirectory()
        check(!target.exists()) { "Target already exists: ${target.fileName}" }
        Files.move(source, target)
        val next = VaultFiles.portable(target, root())
        if (!wasDirectory && Files.isRegularFile(target)) {
            val note = readNote(next)
            return saveNote(next, note.content, note.metadata.copy(title = nextName.trim().ifBlank { sanitized })).relativePath
        }
        return next
    }

    fun move(relativePath: String, targetDirectory: String): String {
        val source = resolve(relativePath)
        val destinationDir = targetDirectory.trim().trim('/')
        if (destinationDir.isNotEmpty()) {
            require(destinationDir != relativePath && !destinationDir.startsWith("$relativePath/")) {
                "Cannot move a folder into itself"
            }
            resolve(destinationDir).createDirectories()
        }
        val parent = if (destinationDir.isEmpty()) root() else resolve(destinationDir)
        var target = parent.resolve(source.fileName)
        if (target.exists()) {
            check(!source.isDirectory()) { "Target folder already contains this entry" }
            val unique = VaultFiles.uniqueChild(parent, source.name.substringBeforeLast('.'), "." + source.extension.ifBlank { "txt" })
            target = parent.resolve(unique)
        }
        Files.move(source, target)
        return VaultFiles.portable(target, root())
    }

    fun duplicate(relativePath: String): NoteDocument {
        val source = readNote(relativePath)
        val parent = relativePath.substringBeforeLast('/', missingDelimiterValue = "").let { if (it == relativePath) "" else it }
        val copyTitle = "${source.metadata.title} Copy"
        val directory = if (parent.isEmpty()) root() else resolve(parent)
        val extension = "." + NoteFrontmatter.extensionForSyntax(source.metadata.syntax)
        val nextName = VaultFiles.uniqueChild(directory, NoteFrontmatter.sanitizeName(copyTitle), extension)
        val nextPath = listOf(parent, nextName).filter { it.isNotBlank() }.joinToString("/")
        val now = Instant.now().toString()
        return saveNote(nextPath, source.content, source.metadata.copy(title = copyTitle, createdAt = now, modifiedAt = now))
    }

    fun delete(relativePath: String) {
        VaultFiles.deleteRecursively(resolve(relativePath))
    }

    fun importFile(source: Path, relativePath: String? = null): Path {
        val name = relativePath ?: source.fileName.toString()
        val target = resolve(name)
        check(!target.exists()) { "File already exists: $name" }
        target.parent.createDirectories()
        Files.copy(source, target)
        return target
    }

    fun exportFile(relativePath: String, destination: Path) {
        Files.copy(resolve(relativePath), destination, StandardCopyOption.REPLACE_EXISTING)
    }

    fun resolve(relativePath: String): Path {
        val base = root().toAbsolutePath().normalize()
        val target = base.resolve(relativePath).toAbsolutePath().normalize()
        check(target.startsWith(base) && target != base) { "Path escapes note vault: $relativePath" }
        if (base.exists()) {
            val canonicalBase = base.toRealPath()
            val existing = generateSequence(target) { it.parent }.first { it.exists() }
            val canonicalExisting = existing.toRealPath()
            check(canonicalExisting == canonicalBase || canonicalExisting.startsWith(canonicalBase)) {
                "Path escapes note vault: $relativePath"
            }
        }
        return target
    }
}
