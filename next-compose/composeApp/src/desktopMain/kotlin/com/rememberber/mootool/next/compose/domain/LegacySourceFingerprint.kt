package com.rememberber.mootool.next.compose.domain

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.io.path.pathString

/**
 * Mirrors Electron `sourceFingerprint` / `addPathFingerprint` in `legacyMigrationService.ts`.
 */
object LegacySourceFingerprint {
    fun fingerprint(root: Path, paths: List<Path>): String {
        val rootReal = runCatching { root.toRealPath() }.getOrElse { root.toAbsolutePath().normalize() }
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(rootReal.pathString.toByteArray(Charsets.UTF_8))
        paths.forEach { path ->
            val resolved = runCatching { path.toRealPath() }.getOrElse { path.toAbsolutePath().normalize() }
            addPathFingerprint(digest, rootReal, resolved, 0)
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun addPathFingerprint(digest: MessageDigest, root: Path, path: Path, depth: Int) {
        if (depth > 32) return
        val absolute = path.toAbsolutePath().normalize()
        val attrs = runCatching {
            Files.readAttributes(absolute, BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
        }.getOrNull() ?: return
        if (attrs.isSymbolicLink) return
        val relative = root.relativize(absolute).pathString
        digest.update(relative.toByteArray(Charsets.UTF_8))
        digest.update(attrs.size().toString().toByteArray(Charsets.UTF_8))
        digest.update(nodeMtimeString(attrs).toByteArray(Charsets.UTF_8))
        if (!attrs.isDirectory) return
        val entries = Files.list(absolute).use { stream ->
            stream.sorted { left, right -> left.fileName.toString().compareTo(right.fileName.toString()) }.toList()
        }
        for (entry in entries) {
            if (entry.fileName.toString() == ".git") continue
            if (Files.isSymbolicLink(entry)) continue
            addPathFingerprint(digest, root, entry, depth + 1)
        }
    }

    /** Matches Node `fs.Stats.mtimeMs` stringification in Electron legacy migration. */
    private fun nodeMtimeString(attrs: BasicFileAttributes): String {
        val nanos = attrs.lastModifiedTime().to(TimeUnit.NANOSECONDS)
        val mtimeMs = nanos.toDouble() / 1_000_000.0
        if (mtimeMs % 1.0 == 0.0) return mtimeMs.toLong().toString()
        return String.format(Locale.US, "%.16g", mtimeMs)
    }
}
