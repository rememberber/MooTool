package com.rememberber.mootool.next.compose.services

import com.rememberber.mootool.next.compose.domain.RegexEngine
import com.rememberber.mootool.next.compose.domain.RegexException
import com.rememberber.mootool.next.compose.domain.RegexOptions
import com.rememberber.mootool.next.compose.domain.RegexWorkerRequest
import com.rememberber.mootool.next.compose.domain.RegexWorkerResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

object RegexWorker {
    private val codec = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @JvmStatic
    fun main(args: Array<String>) {
        val raw = System.`in`.bufferedReader(Charsets.UTF_8).readText()
        val request = codec.decodeFromString<RegexWorkerRequest>(raw)
        val response = try {
            val matches = RegexEngine.match(request.pattern, request.source, request.options, request.maxMatches)
            RegexWorkerResponse(ok = true, matches = matches)
        } catch (error: RegexException) {
            RegexWorkerResponse(ok = false, code = error.code, error = error.message ?: error.code)
        } catch (error: Exception) {
            RegexWorkerResponse(ok = false, code = "failed", error = error.message ?: "failed")
        }
        System.out.write(codec.encodeToString(response).toByteArray(Charsets.UTF_8))
        System.out.flush()
    }
}

class RegexWorkerClient(
    private val timeoutMs: Long = RegexEngine.DEFAULT_TIMEOUT_MS,
    private val javaHome: String = System.getProperty("java.home"),
    private val classpath: String = workerClasspath()
) {
    private val codec = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val lock = Any()
    private var current: Process? = null
    private var cancelled = false

    fun cancel() {
        synchronized(lock) {
            cancelled = true
            current?.destroyForcibly()
            current = null
        }
    }

    fun match(pattern: String, source: String, options: RegexOptions, maxMatches: Int = RegexEngine.DEFAULT_MAX_MATCHES): RegexWorkerResponse {
        val java = javaBinary(javaHome)
        val builder = ProcessBuilder(
            java.toString(),
            "-Xmx64m",
            "-Dfile.encoding=UTF-8",
            "-cp",
            classpath,
            RegexWorker::class.java.name
        )
        builder.redirectError(ProcessBuilder.Redirect.DISCARD)
        synchronized(lock) { cancelled = false }
        val process = builder.start()
        synchronized(lock) { current = process }
        try {
            process.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(codec.encodeToString(RegexWorkerRequest(pattern, source, options, maxMatches)))
            }
            val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            synchronized(lock) {
                if (cancelled) {
                    process.destroyForcibly()
                    return RegexWorkerResponse(ok = false, code = "cancelled", error = "cancelled")
                }
            }
            if (!finished) {
                process.destroyForcibly()
                process.waitFor(1, TimeUnit.SECONDS)
                return RegexWorkerResponse(ok = false, code = "timeout", error = "timeout")
            }
            val output = process.inputStream.bufferedReader(Charsets.UTF_8).readText()
            if (process.exitValue() != 0 && output.isBlank()) {
                return RegexWorkerResponse(ok = false, code = "failed", error = "worker-exit-${process.exitValue()}")
            }
            return runCatching { codec.decodeFromString<RegexWorkerResponse>(output) }
                .getOrElse { RegexWorkerResponse(ok = false, code = "failed", error = output.take(300).ifBlank { it.message ?: "failed" }) }
        } catch (error: Exception) {
            process.destroyForcibly()
            return RegexWorkerResponse(ok = false, code = "worker-unavailable", error = error.message ?: "worker-unavailable")
        } finally {
            synchronized(lock) {
                if (current === process) current = null
            }
        }
    }

    companion object {
        fun workerClasspath(): String {
            val configured = System.getProperty("java.class.path").orEmpty()
            if (configured.isNotBlank()) return configured
            val location = RegexWorker::class.java.protectionDomain.codeSource?.location ?: return "."
            return Path.of(location.toURI()).toString()
        }

        fun javaBinary(javaHome: String): Path {
            val name = if (System.getProperty("os.name").orEmpty().lowercase().contains("win")) "java.exe" else "java"
            val executable = Path.of(javaHome, "bin", name)
            check(Files.isRegularFile(executable)) { "Bundled Java not found: $executable" }
            return executable
        }
    }
}
