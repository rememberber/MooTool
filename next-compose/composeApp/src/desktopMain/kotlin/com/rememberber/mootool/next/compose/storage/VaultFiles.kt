package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.domain.GitIgnoreMatcher
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

data class VaultEntry(
    val relativePath: String,
    val name: String,
    val directory: Boolean,
    val size: Long,
    val color: String = "",
    val createdAt: String = "",
    val modifiedAt: String = ""
)

data class VaultListOptions(
    val query: String = "",
    val includeContent: Boolean = true,
    val hideIgnored: Boolean = true
)

data class VaultIndexRecord(
    val entry: VaultEntry,
    val title: String = "",
    val content: String = ""
)

object VaultFiles {
    const val MAX_ENTRIES = 5_000
    const val MAX_DEPTH = 24
    const val MAX_FILE_SIZE = 20L * 1024 * 1024

    fun portable(path: Path, base: Path): String = path.relativeTo(base).pathString.replace('\\', '/')

    fun uniqueChild(directory: Path, stem: String, extension: String): String {
        repeat(10_000) { index ->
            val suffix = if (index == 0) "" else " (${index + 1})"
            val name = "$stem$suffix$extension"
            if (!directory.resolve(name).exists()) return name
        }
        error("Unable to allocate a unique path")
    }

    fun deleteRecursively(target: Path) {
        if (!target.exists()) return
        if (target.isDirectory()) {
            Files.walk(target).use { stream ->
                stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            }
        } else {
            Files.deleteIfExists(target)
        }
    }

    fun move(source: Path, target: Path) {
        check(!target.exists()) { "Target already exists: ${target.fileName}" }
        target.parent?.createDirectories()
        Files.move(source, target)
    }

    fun copyFile(source: Path, target: Path) {
        check(!target.exists()) { "Target already exists: ${target.fileName}" }
        target.parent?.createDirectories()
        Files.copy(source, target)
    }

    fun matcher(root: Path, hideIgnored: Boolean): GitIgnoreMatcher? =
        if (hideIgnored) GitIgnoreMatcher.load(root) else null

    fun shouldSkipName(name: String): Boolean = name.startsWith('.') || name == "attachments"

    fun ignored(matcher: GitIgnoreMatcher?, relativePath: String, directory: Boolean): Boolean {
        if (matcher == null) return false
        return matcher.ignores(relativePath, directory)
    }

    fun walk(
        base: Path,
        options: VaultListOptions,
        allowedFile: (Path) -> Boolean,
        contentMatch: (Path, VaultEntry) -> Boolean
    ): List<VaultEntry> {
        if (!base.exists()) return emptyList()
        val matcher = matcher(base, options.hideIgnored)
        val needle = options.query.trim().lowercase()
        val collected = ArrayList<VaultEntry>()
        val matched = HashSet<String>()
        fun visit(directory: Path, depth: Int) {
            if (depth > MAX_DEPTH || collected.size >= MAX_ENTRIES) return
            val children = runCatching { Files.list(directory).use { it.toList() } }.getOrDefault(emptyList())
            for (path in children.sortedBy { it.name.lowercase() }) {
                if (collected.size >= MAX_ENTRIES) return
                if (path.isSymbolicLink() || shouldSkipName(path.name)) continue
                val relative = portable(path, base)
                val directory = path.isDirectory()
                if (ignored(matcher, relative, directory)) continue
                if (directory) {
                    visit(path, depth + 1)
                    val selfMatch = needle.isEmpty() || relative.lowercase().contains(needle) || path.name.lowercase().contains(needle)
                    val descendantMatch = matched.any { it == relative || it.startsWith("$relative/") }
                    if (selfMatch || descendantMatch) {
                        collected += entryOf(path, relative, true, 0)
                        matched += relative
                    }
                } else if (path.isRegularFile() && allowedFile(path)) {
                    if (Files.size(path) > MAX_FILE_SIZE) continue
                    val entry = entryOf(path, relative, false, Files.size(path))
                    val pathMatch = needle.isEmpty() || relative.lowercase().contains(needle) || path.name.lowercase().contains(needle)
                    val matches = pathMatch || (needle.isNotEmpty() && contentMatch(path, entry))
                    if (matches) {
                        collected += entry
                        matched += relative
                    }
                }
            }
        }
        visit(base, 0)
        return collected.distinctBy { it.relativePath }.sortedWith { left, right ->
            when {
                left.directory != right.directory -> if (left.directory) -1 else 1
                else -> left.relativePath.compareTo(right.relativePath, ignoreCase = true)
            }
        }
    }

    private fun entryOf(path: Path, relative: String, directory: Boolean, size: Long): VaultEntry {
        val attrs = runCatching { Files.readAttributes(path, BasicFileAttributes::class.java) }.getOrNull()
        return VaultEntry(
            relativePath = relative,
            name = path.name,
            directory = directory,
            size = size,
            createdAt = attrs?.creationTime()?.toInstant()?.toString().orEmpty(),
            modifiedAt = attrs?.lastModifiedTime()?.toInstant()?.toString().orEmpty()
        )
    }
}
