package com.rememberber.mootool.next.compose.storage

import com.rememberber.mootool.next.compose.app.AppDirectories
import com.rememberber.mootool.next.compose.domain.ImageEngine
import com.rememberber.mootool.next.compose.domain.ImageException
import com.rememberber.mootool.next.compose.domain.ImageOutputFormat
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

data class ImageAssetSummary(
    val name: String,
    val size: Long,
    val width: Int,
    val height: Int,
    val modifiedTime: String
)

data class ImageAsset(
    val name: String,
    val size: Long,
    val width: Int,
    val height: Int,
    val modifiedTime: String,
    val bytes: ByteArray,
    val image: BufferedImage
)

class ImageLibraryStore(private val root: Path) {
    constructor(directories: AppDirectories) : this(directories.dataRoot.resolve("images"))

    fun list(): List<ImageAssetSummary> {
        ensureRoot()
        return root.listDirectoryEntries()
            .filter { it.isRegularFile() && supported(it.extension) }
            .mapNotNull { runCatching { summary(it.name) }.getOrNull() }
            .sortedByDescending { it.modifiedTime }
    }

    fun read(name: String): ImageAsset {
        val path = resolve(name)
        if (!path.exists()) throw ImageException("missing", "File not found")
        val bytes = path.readBytes()
        val image = ImageEngine.decode(bytes)
        val stat = Files.getLastModifiedTime(path)
        return ImageAsset(
            name = sanitize(name),
            size = path.fileSize(),
            width = image.width,
            height = image.height,
            modifiedTime = Instant.ofEpochMilli(stat.toMillis()).toString(),
            bytes = bytes,
            image = image
        )
    }

    fun importFiles(paths: List<Path>): List<ImageAssetSummary> {
        ensureRoot()
        val imported = mutableListOf<ImageAssetSummary>()
        for (source in paths) {
            if (!supported(source.extension)) continue
            val bytes = runCatching { source.readBytes() }.getOrNull() ?: continue
            runCatching { ImageEngine.decode(bytes) }.getOrNull() ?: continue
            val name = uniqueName(source.fileName.toString())
            resolve(name).writeBytes(bytes)
            imported += summary(name)
        }
        return imported
    }

    fun save(name: String, image: BufferedImage, jpeg: Boolean = false): ImageAssetSummary {
        ensureRoot()
        val requested = sanitize(name)
        val extension = requested.substringAfterLast('.', "").lowercase()
        val stored = if (jpeg || extension == "jpg" || extension == "jpeg") {
            if (extension == "jpg" || extension == "jpeg") requested else "${requested.substringBeforeLast('.', requested)}.jpg"
        } else {
            "${requested.substringBeforeLast('.', requested)}.png"
        }
        writeImage(stored, image, ImageEngine.isJpegName(stored))
        return summary(stored)
    }

    fun saveBytes(name: String, bytes: ByteArray): ImageAssetSummary {
        val image = ImageEngine.decode(bytes)
        return save(name, image, ImageEngine.isJpegName(name))
    }

    fun rename(name: String, nextName: String): ImageAssetSummary {
        val source = resolve(name)
        if (!source.exists()) throw ImageException("missing", "File not found")
        val sourceExt = source.extension.ifBlank { "png" }
        val requested = sanitize(nextName)
        val targetName = if (requested.contains('.')) requested else "$requested.$sourceExt"
        val target = resolve(targetName)
        if (target.exists() && target != source) throw ImageException("exists", "A file with that name already exists")
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
        return summary(targetName)
    }

    fun delete(names: List<String>) {
        names.forEach { Files.deleteIfExists(resolve(it)) }
    }

    fun export(names: List<String>, directory: Path) {
        Files.createDirectories(directory)
        names.forEach { name ->
            Files.copy(resolve(name), directory.resolve(sanitize(name)), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    fun pathFor(name: String): Path = resolve(name)

    private fun writeImage(name: String, image: BufferedImage, jpeg: Boolean) {
        val format = if (jpeg) ImageOutputFormat.Jpeg else ImageOutputFormat.Png
        resolve(name).writeBytes(ImageEngine.encode(image, format, 0.92f))
    }

    private fun summary(name: String): ImageAssetSummary {
        val path = resolve(name)
        val image = ImageEngine.decode(path.readBytes())
        return ImageAssetSummary(
            name = sanitize(name),
            size = path.fileSize(),
            width = image.width,
            height = image.height,
            modifiedTime = Instant.ofEpochMilli(path.getLastModifiedTime().toMillis()).toString()
        )
    }

    private fun uniqueName(requested: String): String {
        val safe = sanitize(requested)
        val dot = safe.lastIndexOf('.')
        val base = if (dot > 0) safe.substring(0, dot) else safe
        val ext = if (dot > 0) safe.substring(dot) else ".png"
        var candidate = safe
        var index = 2
        while (resolve(candidate).exists()) {
            candidate = "$base-$index$ext"
            index += 1
        }
        return candidate
    }

    private fun resolve(name: String): Path {
        val safe = sanitize(name)
        val base = root.toAbsolutePath().normalize()
        val path = base.resolve(safe).normalize()
        if (path.parent != base) throw ImageException("invalid-name", "Invalid image name")
        return path
    }

    private fun ensureRoot() {
        Files.createDirectories(root)
    }

    companion object {
        private val extensions = setOf("png", "jpg", "jpeg", "gif", "bmp", "webp")
        private val allowed = Regex("[^a-zA-Z0-9._\\-\\u4e00-\\u9fff\\u3040-\\u30ff]")

        fun supported(extension: String): Boolean = extension.lowercase() in extensions

        fun sanitize(value: String): String {
            val name = value.substringAfterLast('/').substringAfterLast('\\').trim().replace(allowed, "_").take(160)
            if (name.isEmpty() || name == "." || name == "..") throw ImageException("invalid-name", "Invalid image name")
            val extension = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
            if (extension.isNotEmpty() && extension !in extensions) throw ImageException("unsupported", "Unsupported image format")
            return name
        }
    }
}
