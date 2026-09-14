package com.rememberber.mootool.next.compose.domain

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
import kotlin.streams.asSequence

class VaultRevisionMonitor(
    private val root: Path,
    private val ignoreAttachments: Boolean,
    private val intervalMs: Long = 400L,
    private val onChanged: (List<String>) -> Unit
) : AutoCloseable {
    private val expectedHashes = ConcurrentHashMap<String, String>()
    private val closed = AtomicBoolean(false)
    private val started = AtomicBoolean(false)
    @Volatile var previous: Map<String, String> = emptyMap()
        private set
    private var worker: Thread? = null

    fun noteOwnWrite(relativePath: String, hash: String) {
        expectedHashes[relativePath.replace('\\', '/')] = hash
    }

    fun snapshot(): Map<String, String> {
        val base = root.toAbsolutePath().normalize().let { if (it.exists()) it.toRealPath() else it }
        if (!base.exists() || !base.isDirectory()) return emptyMap()
        return Files.walk(base).use { stream ->
            stream.asSequence()
                .filter { it != base && it.isRegularFile() && !it.isSymbolicLink() }
                .mapNotNull { path ->
                    val relative = path.relativeTo(base).pathString.replace('\\', '/')
                    if (VaultConflictEngine.shouldIgnore(relative, ignoreAttachments)) null
                    else fingerprint(path)?.let { relative to it }
                }
                .toMap()
        }
    }

    fun diff(old: Map<String, String>, next: Map<String, String>): List<String> {
        return (old.keys + next.keys).filter { old[it] != next[it] }.sorted()
    }

    fun start() {
        check(started.compareAndSet(false, true)) { "VaultRevisionMonitor already started" }
        previous = snapshot()
        worker = Thread(
            {
                while (!closed.get()) {
                    try {
                        Thread.sleep(intervalMs.coerceAtLeast(50L))
                    } catch (_: InterruptedException) {
                        break
                    }
                    if (closed.get()) break
                    val next = snapshot()
                    val changed = diff(previous, next).filter { path ->
                        val expected = expectedHashes[path]
                        val actual = next[path]
                        if (expected != null && actual == expected) {
                            expectedHashes.remove(path)
                            false
                        } else {
                            true
                        }
                    }
                    previous = next
                    if (changed.isNotEmpty()) onChanged(changed)
                }
            },
            "next-compose-vault-watch"
        )
        worker?.isDaemon = true
        worker?.start()
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        worker?.interrupt()
        worker = null
    }

    private fun fingerprint(path: Path): String? {
        return try {
            val size = Files.size(path)
            if (size > VaultConflictEngine.MAX_HASH_BYTES) "size:$size"
            else VaultConflictEngine.sha256(Files.readAllBytes(path))
        } catch (_: Exception) {
            null
        }
    }
}
