package com.rememberber.mootool.next.compose.domain

import java.io.InputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.writeText

data class UpdateProgress(val transferred: Long, val total: Long) {
    val percent: Int get() = if (total <= 0L) 0 else ((transferred * 100) / total).toInt().coerceIn(0, 100)
}

class UpdateCancelled : RuntimeException("Update download cancelled")

fun interface UpdateBytesFetcher {
    fun open(url: String): Pair<Int, InputStream>
}

class UpdateDownloader(
    private val directory: Path,
    private val fetcher: UpdateBytesFetcher
) {
    private val cancelled = AtomicBoolean(false)

    fun cancel() {
        cancelled.set(true)
    }

    fun download(download: UpdateDownload, onProgress: (UpdateProgress) -> Unit = {}): Path {
        cancelled.set(false)
        UpdateEngine.requireHttps(download.url, "download")
        directory.createDirectories()
        val destination = directory.resolve(download.fileName)
        val temporary = directory.resolve(".${download.fileName}.download")
        temporary.deleteIfExists()
        onProgress(UpdateProgress(0, download.size))
        try {
            val (code, stream) = fetcher.open(download.url)
            check(code in 200..299) { "Update download returned HTTP $code" }
            stream.use { input ->
                val digest = MessageDigest.getInstance("SHA-512")
                DigestInputStream(input, digest).use { digested ->
                    Files.newOutputStream(temporary).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var transferred = 0L
                        while (true) {
                            if (cancelled.get()) throw UpdateCancelled()
                            val read = digested.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            transferred += read
                            check(transferred <= download.size) { "Update download size mismatch" }
                            onProgress(UpdateProgress(transferred, download.size))
                        }
                        check(transferred == download.size) {
                            "Update download size mismatch: expected ${download.size}, received $transferred"
                        }
                    }
                }
                val actual = Base64.getEncoder().encodeToString(digest.digest())
                check(actual == download.sha512) { "Update download checksum mismatch" }
            }
            try {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING)
            }
            directory.resolve("${download.fileName}.ready").writeText(download.sha512)
            onProgress(UpdateProgress(download.size, download.size))
            return destination
        } catch (error: Throwable) {
            temporary.deleteIfExists()
            throw error
        }
    }

    fun readyFile(download: UpdateDownload): Path? {
        val destination = directory.resolve(download.fileName)
        val marker = directory.resolve("${download.fileName}.ready")
        if (!destination.exists() || !marker.exists()) return null
        return if (marker.toFile().readText().trim() == download.sha512) destination else null
    }
}
