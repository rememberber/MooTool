package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppDirectories
import com.rememberber.mootool.next.compose.domain.VaultSearchIndex
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

internal fun jsonVaultImportRelativePath(targetDirectory: String, fileName: String): String =
    listOfNotNull(targetDirectory.trim().takeIf { it.isNotBlank() }, fileName).joinToString("/")

internal fun jsonVaultEntryRelativePath(vaultRoot: Path, entry: Path): String =
    vaultRoot.toAbsolutePath().normalize()
        .relativize(entry.toAbsolutePath().normalize())
        .toString()
        .replace('\\', '/')

class JsonVault(
    private val directories: AppDirectories,
    private val customRoot: String = ""
) {
    fun root(): Path {
        val effective = VaultPathConfig.effectiveCustomRoot(customRoot)
        val path = if (effective.isEmpty()) directories.jsonVault else Path.of(effective)
        return path.apply { createDirectories() }
    }

    fun snapshot(hideIgnored: Boolean = true): List<VaultIndexRecord> {
        val base = root().toAbsolutePath().normalize().let { if (it.exists()) it.toRealPath() else it }
        return VaultFiles.walk(
            base,
            VaultListOptions("", includeContent = false, hideIgnored = hideIgnored),
            allowedFile = { path -> path.extension.lowercase() in setOf("json", "txt", "jsonl") }
        ) { _, _ -> false }.map { entry ->
            if (entry.directory) {
                VaultIndexRecord(entry)
            } else {
                val content = runCatching { root().resolve(entry.relativePath).readText(Charsets.UTF_8) }.getOrDefault("")
                VaultIndexRecord(entry, title = entry.name, content = content)
            }
        }
    }

    fun list(
        query: String = "",
        includeContent: Boolean = true,
        hideIgnored: Boolean = true
    ): List<VaultEntry> = VaultSearchIndex.filter(snapshot(hideIgnored), query, includeContent)

    fun read(relativePath: String): String = resolve(relativePath).readText(Charsets.UTF_8)

    fun readOrNull(relativePath: String): String? {
        val target = resolve(relativePath)
        return if (Files.isRegularFile(target)) target.readText(Charsets.UTF_8) else null
    }

    fun write(relativePath: String, content: String): Path {
        val target = resolve(relativePath)
        target.parent.createDirectories()
        val temp = target.resolveSibling("${target.name}.tmp")
        temp.writeText(content, Charsets.UTF_8)
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        return target
    }

    fun createFile(relativePath: String, content: String = "{\n}\n"): Path {
        val target = resolve(withJsonExtension(relativePath))
        check(!target.exists()) { "File already exists: $relativePath" }
        return write(withJsonExtension(relativePath), content)
    }

    fun createDirectory(relativePath: String): Path {
        val target = resolve(relativePath)
        check(!target.exists()) { "Path already exists: $relativePath" }
        target.createDirectories()
        return target
    }

    fun rename(relativePath: String, nextName: String): String {
        val source = resolve(relativePath)
        val sanitized = nextName.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
        require(sanitized.isNotEmpty()) { "Invalid file name" }
        val name = if (source.isDirectory() || sanitized.contains('.')) sanitized else "$sanitized.json"
        val target = source.resolveSibling(name)
        VaultFiles.move(source, target)
        return VaultFiles.portable(target, root())
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
            val unique = VaultFiles.uniqueChild(parent, source.name.substringBeforeLast('.'), ".json")
            target = parent.resolve(unique)
        }
        Files.move(source, target)
        return VaultFiles.portable(target, root())
    }

    fun duplicate(relativePath: String): String {
        val source = resolve(relativePath)
        check(Files.isRegularFile(source)) { "Only files can be duplicated" }
        val parent = source.parent
        val unique = VaultFiles.uniqueChild(parent, source.name.substringBeforeLast('.') + " Copy", ".json")
        val target = parent.resolve(unique)
        Files.copy(source, target)
        return VaultFiles.portable(target, root())
    }

    fun delete(relativePath: String) {
        VaultFiles.deleteRecursively(resolve(relativePath))
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
        check(target.startsWith(base) && target != base) { "Path escapes JSON vault: $relativePath" }
        if (base.exists()) {
            val canonicalBase = base.toRealPath()
            val existing = generateSequence(target) { it.parent }.first { it.exists() }
            val canonicalExisting = existing.toRealPath()
            check(canonicalExisting == canonicalBase || canonicalExisting.startsWith(canonicalBase)) {
                "Path escapes JSON vault: $relativePath"
            }
        }
        if (target.extension.isNotEmpty() && target.extension.lowercase() !in setOf("json", "txt", "jsonl")) {
            check(relativePath.endsWith(".json") || target.isDirectory() || target.exists()) {
                "JSON vault only accepts json files"
            }
        }
        return target
    }

    private fun withJsonExtension(relativePath: String): String {
        val portable = relativePath.replace('\\', '/')
        return if (portable.substringAfterLast('/').contains('.')) portable else "$portable.json"
    }
}
