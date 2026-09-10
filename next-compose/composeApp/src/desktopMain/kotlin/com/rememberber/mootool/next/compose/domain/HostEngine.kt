package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.app.AppDirectories
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.isWritable
import kotlin.io.path.readText

enum class HostErrorCode { INVALID, EMPTY, CONFLICT, PERMISSION, COMMAND_FAILED, MISSING }

class HostException(val code: HostErrorCode, message: String) : Exception(message)

data class SystemHostsFile(
    val path: String,
    val content: String,
    val writable: Boolean,
    val fingerprint: String
)

data class HostApplyResult(
    val system: SystemHostsFile,
    val backupPath: String?,
    val diff: String,
    val dnsFlushed: Boolean
)

data class HostApplyConfig(
    val systemPath: Path,
    val backupDir: Path,
    val allowElevation: Boolean = true,
    val flushDns: Boolean = true,
    val osFamily: OsFamily = detectOsFamily()
) {
    companion object {
        fun production(directories: AppDirectories): HostApplyConfig {
            val os = detectOsFamily()
            return HostApplyConfig(
                systemPath = defaultHostsPath(os),
                backupDir = directories.backups.resolve("hosts"),
                osFamily = os
            )
        }

        fun defaultHostsPath(os: OsFamily = detectOsFamily()): Path = when (os) {
            OsFamily.Windows -> {
                val root = System.getenv("SystemRoot")?.trim()?.ifBlank { null } ?: "C:\\Windows"
                Path.of(root, "System32", "drivers", "etc", "hosts")
            }
            OsFamily.Mac, OsFamily.Linux -> Path.of("/etc/hosts")
        }
    }
}

object HostEngine {
    const val MAX_BYTES = 2 * 1024 * 1024
    const val DEFAULT_TEMPLATE = "# MooTool Next Compose hosts profile\n127.0.0.1 localhost\n::1 localhost\n"
    private val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC)
    private val ipv4 = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""")
    private val ipv6 = Regex("""^[0-9A-Fa-f:.]+(?:%[A-Za-z0-9._~-]+)?$""")

    fun normalize(content: String): String {
        if (content.length > MAX_BYTES || content.contains('\u0000')) {
            throw HostException(HostErrorCode.INVALID, "Invalid hosts content")
        }
        val unix = content.replace("\r\n", "\n").replace("\r", "\n").replace(Regex("\n*$"), "")
        return "$unix\n"
    }

    fun validate(content: String): String {
        val normalized = normalize(content)
        var entries = 0
        normalized.lineSequence().forEachIndexed { index, raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed
            val body = line.substringBefore("#").trim()
            val parts = body.split(Regex("""\s+""")).filter { it.isNotEmpty() }
            if (parts.size < 2 || !isAddress(parts[0]) || parts.drop(1).any { it.contains('\u0000') }) {
                throw HostException(HostErrorCode.INVALID, "Invalid hosts entry at line ${index + 1}")
            }
            entries += 1
        }
        if (entries == 0) {
            throw HostException(HostErrorCode.EMPTY, "Hosts content is empty")
        }
        return normalized
    }

    fun fingerprint(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    fun previewDiff(before: String, after: String, path: String): String {
        val normalizedAfter = normalize(after)
        val unified = DiffEngine.compare(before, normalizedAfter, false).unified
        return buildString {
            appendLine(path)
            append(unified.ifBlank { "(no textual difference)" })
        }.trimEnd()
    }

    fun readSystem(config: HostApplyConfig): SystemHostsFile {
        val content = readFileOrEmpty(config.systemPath)
        val writable = when {
            config.systemPath.exists() -> config.systemPath.isWritable()
            else -> config.systemPath.parent?.isWritable() == true
        }
        return SystemHostsFile(
            path = config.systemPath.toString(),
            content = content,
            writable = writable,
            fingerprint = fingerprint(content)
        )
    }

    fun apply(
        config: HostApplyConfig,
        content: String,
        expectedFingerprint: String?,
        validateEntries: Boolean = true
    ): HostApplyResult {
        val normalized = if (validateEntries) validate(content) else normalize(content)
        if (normalized.trim().isEmpty()) {
            throw HostException(HostErrorCode.EMPTY, "Hosts content is empty")
        }
        val before = readFileOrEmpty(config.systemPath)
        if (expectedFingerprint != null && fingerprint(before) != expectedFingerprint) {
            throw HostException(HostErrorCode.CONFLICT, "System hosts changed since last read")
        }
        val diff = previewDiff(before, normalized, config.systemPath.toString())
        if (before == normalized) {
            return HostApplyResult(readSystem(config), null, diff, dnsFlushed = false)
        }
        val backup = backupIfPresent(config, before)
        writeSystem(config, normalized)
        val flushed = if (config.flushDns) flushDns(config) else false
        return HostApplyResult(readSystem(config), backup, diff, flushed)
    }

    fun restore(config: HostApplyConfig, backupPath: String, expectedFingerprint: String?): HostApplyResult {
        val backup = Path.of(backupPath)
        if (!backup.exists() || !backup.isRegularFile()) {
            throw HostException(HostErrorCode.MISSING, "Backup not found: $backupPath")
        }
        return apply(config, backup.readText(StandardCharsets.UTF_8), expectedFingerprint, validateEntries = false)
    }

    fun copyName(name: String, suffix: String = " copy"): String {
        val base = name.trim().ifBlank { "hosts" }
        return "$base$suffix"
    }

    private fun writeSystem(config: HostApplyConfig, content: String) {
        try {
            atomicWrite(config.systemPath, content)
        } catch (error: Exception) {
            if (!isPermission(error)) throw HostException(classifyIo(error), error.message ?: config.systemPath.toString())
            if (!config.allowElevation) throw HostException(HostErrorCode.PERMISSION, "PERMISSION")
            writeElevated(config, content)
        }
    }

    private fun writeElevated(config: HostApplyConfig, content: String) {
        config.backupDir.createDirectories()
        val temporary = config.backupDir.resolve("mootool-hosts-elevated-${ProcessHandle.current().pid()}.tmp")
        Files.writeString(temporary, content, StandardCharsets.UTF_8)
        try {
            when (config.osFamily) {
                OsFamily.Mac -> {
                    val command = "/bin/cp ${EnvEngine.shellQuote(temporary.toString())} ${EnvEngine.shellQuote(config.systemPath.toString())} && /usr/bin/chmod 644 ${EnvEngine.shellQuote(config.systemPath.toString())} && /usr/bin/dscacheutil -flushcache"
                    val script = "do shell script ${appleScriptString(command)} with administrator privileges"
                    runCommand(listOf("/usr/bin/osascript", "-e", script), 120_000)
                }
                OsFamily.Linux -> runCommand(
                    listOf(
                        "/usr/bin/pkexec",
                        "/bin/sh",
                        "-c",
                        "/bin/cp \"\$1\" \"\$2\" && /bin/chmod 644 \"\$2\"",
                        "mootool-next-compose",
                        temporary.toString(),
                        config.systemPath.toString()
                    ),
                    120_000
                )
                OsFamily.Windows -> {
                    val script = "Copy-Item -LiteralPath '${psQuote(temporary.toString())}' -Destination '${psQuote(config.systemPath.toString())}' -Force"
                    val encoded = Base64.getEncoder().encodeToString(script.toByteArray(Charsets.UTF_16LE))
                    val elevated = "\$p=Start-Process -FilePath powershell.exe -Verb RunAs -Wait -PassThru -ArgumentList @('-NoProfile','-EncodedCommand','$encoded'); exit \$p.ExitCode"
                    runCommand(listOf("powershell.exe", "-NoProfile", "-Command", elevated), 120_000)
                }
            }
        } catch (error: HostException) {
            throw error
        } catch (error: Exception) {
            throw HostException(classifyIo(error), error.message ?: config.systemPath.toString())
        } finally {
            runCatching { Files.deleteIfExists(temporary) }
        }
    }

    private fun flushDns(config: HostApplyConfig): Boolean {
        val command = when (config.osFamily) {
            OsFamily.Mac -> listOf("/usr/bin/dscacheutil", "-flushcache")
            OsFamily.Windows -> listOf("ipconfig", "/flushdns")
            OsFamily.Linux -> listOf("resolvectl", "flush-caches")
        }
        return runCatching {
            runCommand(command, 15_000)
            true
        }.getOrDefault(false)
    }

    private fun backupIfPresent(config: HostApplyConfig, current: String): String? {
        if (current.isEmpty() && !config.systemPath.exists()) return null
        config.backupDir.createDirectories()
        val backup = config.backupDir.resolve("hosts-${stamp.format(Instant.now())}.bak")
        if (config.systemPath.exists() && config.systemPath.isRegularFile()) {
            Files.copy(config.systemPath, backup, StandardCopyOption.REPLACE_EXISTING)
        } else {
            Files.writeString(backup, current, StandardCharsets.UTF_8)
        }
        return backup.toString()
    }

    private fun atomicWrite(destination: Path, content: String) {
        destination.parent?.createDirectories()
        val temp = destination.resolveSibling("${destination.fileName}.tmp-${ProcessHandle.current().pid()}")
        Files.writeString(
            temp,
            content,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        )
        try {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: IOException) {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun readFileOrEmpty(path: Path): String {
        if (!path.exists()) return ""
        if (!path.isRegularFile()) throw HostException(HostErrorCode.INVALID, "Not a regular file: $path")
        return path.readText(StandardCharsets.UTF_8)
    }

    private fun isAddress(value: String): Boolean {
        val v4 = ipv4.matchEntire(value)
        if (v4 != null) return v4.groupValues.drop(1).all { it.toInt() in 0..255 }
        return value.contains(':') && ipv6.matches(value)
    }

    private fun runCommand(command: List<String>, timeoutMs: Long): String {
        return try {
            val process = ProcessBuilder(command).start()
            val stdout = process.inputStream.readBytes()
            val stderr = process.errorStream.readBytes()
            val finished = process.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                throw HostException(HostErrorCode.COMMAND_FAILED, "TIMEOUT")
            }
            if (process.exitValue() != 0) {
                val message = stderr.decodeToString().ifBlank { stdout.decodeToString() }
                    .ifBlank { "${command.first()} exited ${process.exitValue()}" }
                throw HostException(classifyIo(IOException(message)), message.trim())
            }
            stdout.decodeToString()
        } catch (error: HostException) {
            throw error
        } catch (error: IOException) {
            throw HostException(classifyIo(error), error.message ?: command.first())
        }
    }

    private fun isPermission(error: Throwable): Boolean = classifyIo(error) == HostErrorCode.PERMISSION

    private fun classifyIo(error: Throwable): HostErrorCode {
        if (error is AccessDeniedException) return HostErrorCode.PERMISSION
        val message = (error.message.orEmpty() + " " + error.javaClass.simpleName).lowercase()
        return when {
            "permission" in message || "access" in message || "eacces" in message ||
                "eperm" in message || "error=13" in message -> HostErrorCode.PERMISSION
            else -> HostErrorCode.COMMAND_FAILED
        }
    }

    private fun appleScriptString(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun psQuote(value: String): String = value.replace("'", "''")
}
