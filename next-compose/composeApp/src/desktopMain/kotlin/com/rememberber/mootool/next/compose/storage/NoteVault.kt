package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppDirectories
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText

class NoteVault(
    private val directories: AppDirectories,
    private val customRoot: String = ""
) {
    fun root(): Path {
        val configured = customRoot.trim()
        val path = if (configured.isEmpty()) directories.quickNoteVault else Path.of(configured)
        return path.apply { createDirectories() }
    }

    fun list(query: String = ""): List<VaultEntry> {
        val base = root().toRealPath()
        if (!base.exists()) return emptyList()
        val needle = query.trim().lowercase()
        return Files.walk(base).use { stream ->
            stream.filter { it != base }
                .map { path ->
                    VaultEntry(
                        relativePath = path.relativeTo(base).pathString.replace('\\', '/'),
                        name = path.name,
                        directory = path.isDirectory(),
                        size = if (path.isRegularFile()) Files.size(path) else 0
                    )
                }
                .filter { needle.isEmpty() || it.relativePath.lowercase().contains(needle) || it.name.lowercase().contains(needle) }
                .sorted { left, right ->
                    when {
                        left.directory != right.directory -> if (left.directory) -1 else 1
                        else -> left.relativePath.compareTo(right.relativePath, ignoreCase = true)
                    }
                }
                .toList()
        }
    }

    fun read(relativePath: String): String = resolve(relativePath).readText(Charsets.UTF_8)

    fun write(relativePath: String, content: String): Path {
        val target = resolve(relativePath)
        target.parent.createDirectories()
        val temp = target.resolveSibling(".${target.name}.tmp")
        temp.writeText(content, Charsets.UTF_8)
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        return target
    }

    fun createFile(relativePath: String, content: String = ""): Path {
        val target = resolve(relativePath)
        check(!target.exists()) { "File already exists: $relativePath" }
        return write(relativePath, content)
    }

    fun createDirectory(relativePath: String): Path {
        val target = resolve(relativePath)
        check(!target.exists()) { "Path already exists: $relativePath" }
        target.createDirectories()
        return target
    }

    fun rename(relativePath: String, nextName: String): Path {
        val source = resolve(relativePath)
        val target = source.resolveSibling(nextName)
        check(!target.exists()) { "Target already exists: $nextName" }
        Files.move(source, target)
        return target
    }

    fun delete(relativePath: String) {
        val target = resolve(relativePath)
        if (target.isDirectory()) {
            check(Files.list(target).use { it.findFirst().isEmpty }) { "Folder is not empty: $relativePath" }
            Files.deleteIfExists(target)
        } else {
            Files.deleteIfExists(target)
        }
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
        val canonicalBase = if (base.exists()) base.toRealPath() else base
        val canonicalTargetParent = if (target.parent.exists()) target.parent.toRealPath() else target.parent.normalize()
        val allowed = canonicalTargetParent == canonicalBase || canonicalTargetParent.startsWith(canonicalBase)
        check(allowed) { "Path escapes note vault: $relativePath" }
        return target
    }
}
