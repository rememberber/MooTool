package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppDirectories
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText

data class VaultEntry(
    val relativePath: String,
    val name: String,
    val directory: Boolean,
    val size: Long
)

class JsonVault(private val directories: AppDirectories) {
    fun root(): Path = directories.jsonVault.apply { createDirectories() }

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
        val temp = target.resolveSibling("${target.name}.tmp")
        temp.writeText(content, Charsets.UTF_8)
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        return target
    }

    fun createFile(relativePath: String, content: String = "{\n}\n"): Path {
        val target = resolve(relativePath)
        check(!target.exists()) { "File already exists: $relativePath" }
        return write(relativePath, content)
    }

    fun delete(relativePath: String) {
        val target = resolve(relativePath)
        if (target.isDirectory()) {
            Files.walk(target).use { stream ->
                stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            }
        } else {
            Files.deleteIfExists(target)
        }
    }

    fun importFile(source: Path, relativePath: String? = null): Path {
        val name = relativePath ?: source.fileName.toString()
        val target = resolve(name)
        target.parent.createDirectories()
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
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
        check(allowed) { "Path escapes JSON vault: $relativePath" }
        if (target.extension.isNotEmpty() && target.extension.lowercase() !in setOf("json", "txt", "jsonl")) {
            check(relativePath.endsWith(".json") || target.isDirectory() || target.exists()) {
                "JSON vault only accepts json files"
            }
        }
        return target
    }
}
