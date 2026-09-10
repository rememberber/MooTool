package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.app.AppDirectories
import com.rememberber.mootool.next.compose.app.ProductIdentity
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

enum class EnvTab { Environment, Runtime }

enum class EnvDisplayScope { User, System, Process }

enum class EnvPersistScope { User, System }

enum class EnvErrorCode { INVALID_NAME, INVALID_VALUE, PERMISSION, COMMAND_FAILED, UNSUPPORTED }

data class EnvEntry(val key: String, val value: String)

data class EnvSnapshot(
    val process: List<EnvEntry>,
    val runtime: List<EnvEntry>,
    val user: List<EnvEntry>,
    val system: List<EnvEntry>,
    val userFile: String,
    val systemFile: String,
    val shellProfile: String
)

data class EnvWriteResult(
    val snapshot: EnvSnapshot,
    val backupPath: String?,
    val diff: String
)

data class EnvStoreConfig(
    val userFile: Path,
    val systemFile: Path,
    val backupDir: Path,
    val shellProfile: Path,
    val installShellHook: Boolean = true,
    val allowElevation: Boolean = true,
    val applyLaunchctl: Boolean = true,
    val osFamily: OsFamily = detectOsFamily()
) {
    companion object {
        fun production(directories: AppDirectories): EnvStoreConfig {
            val home = Path.of(System.getProperty("user.home"))
            val os = detectOsFamily()
            return EnvStoreConfig(
                userFile = directories.dataRoot.resolve("environment"),
                systemFile = when (os) {
                    OsFamily.Windows -> Path.of("")
                    OsFamily.Mac -> Path.of("/etc/zshenv")
                    OsFamily.Linux -> Path.of("/etc/environment")
                },
                backupDir = directories.backups.resolve("environment"),
                shellProfile = when (os) {
                    OsFamily.Mac -> home.resolve(".zshenv")
                    OsFamily.Linux -> home.resolve(".profile")
                    OsFamily.Windows -> home.resolve(".unused-compose-env-profile")
                },
                osFamily = os
            )
        }
    }
}

class EnvException(val code: EnvErrorCode, message: String) : Exception(message)

enum class OsFamily { Windows, Mac, Linux }

fun detectOsFamily(): OsFamily {
    val name = System.getProperty("os.name").orEmpty().lowercase()
    return when {
        name.contains("win") -> OsFamily.Windows
        name.contains("mac") || name.contains("darwin") -> OsFamily.Mac
        else -> OsFamily.Linux
    }
}

object EnvEngine {
    const val SHELL_HOOK_BEGIN = "# >>> MooTool Next Compose environment >>>"
    const val SHELL_HOOK_END = "# <<< MooTool Next Compose environment <<<"
    private val assignment = Regex("""^\s*(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$""")
    private val namePattern = Regex("""^[A-Za-z_][A-Za-z0-9_]*$""")
    private val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC)

    fun parseContent(content: String): List<EnvEntry> {
        val values = linkedMapOf<String, String>()
        for (line in content.split('\n', '\r')) {
            val match = assignment.matchEntire(line) ?: continue
            values[match.groupValues[1]] = unquote(match.groupValues[2].trim())
        }
        return values.entries.map { EnvEntry(it.key, it.value) }.sortedBy { it.key.lowercase() }
    }

    fun updateContent(content: String, key: String, value: String?, shellExport: Boolean): String {
        validateName(key)
        if (value != null) validateValue(value)
        val replacement = value?.let { serialize(key, it, shellExport) }
        val lines = content.split('\n').map { it.trimEnd('\r') }
        val result = mutableListOf<String>()
        var replaced = false
        for (line in lines) {
            val match = assignment.matchEntire(line)
            if (match?.groupValues?.get(1) == key) {
                if (!replaced && replacement != null) {
                    result.add(replacement)
                    replaced = true
                }
                continue
            }
            result.add(line)
        }
        while (result.isNotEmpty() && result.last().isEmpty()) result.removeAt(result.lastIndex)
        if (replacement != null && !replaced) result.add(replacement)
        return if (result.isEmpty()) "" else result.joinToString("\n", postfix = "\n")
    }

    fun validateName(key: String) {
        if (!namePattern.matches(key)) throw EnvException(EnvErrorCode.INVALID_NAME, "Invalid environment variable name")
    }

    fun validateValue(value: String) {
        if (value.length > 65_535 || value.any { it == '\u0000' || it == '\r' || it == '\n' }) {
            throw EnvException(EnvErrorCode.INVALID_VALUE, "Invalid environment variable value")
        }
    }

    fun snapshot(config: EnvStoreConfig): EnvSnapshot {
        val process = System.getenv().entries
            .map { EnvEntry(it.key, it.value ?: "") }
            .sortedBy { it.key.lowercase() }
        return EnvSnapshot(
            process = process,
            runtime = runtimeEntries(),
            user = persistentEntries(config, EnvPersistScope.User),
            system = persistentEntries(config, EnvPersistScope.System),
            userFile = config.userFile.toString(),
            systemFile = config.systemFile.toString(),
            shellProfile = config.shellProfile.toString()
        )
    }

    fun previewDiff(snapshot: EnvSnapshot, scope: EnvPersistScope, key: String, value: String?): String {
        val current = when (scope) {
            EnvPersistScope.User -> snapshot.user
            EnvPersistScope.System -> snapshot.system
        }.firstOrNull { it.key == key }?.value
        val file = if (scope == EnvPersistScope.User) snapshot.userFile else snapshot.systemFile
        return buildString {
            appendLine(if (scope == EnvPersistScope.User) "user" else "system")
            appendLine(file)
            appendLine("$key: ${current ?: "(absent)"}")
            appendLine("→ ${value ?: "(delete)"}")
        }.trimEnd()
    }

    fun set(config: EnvStoreConfig, scope: EnvPersistScope, key: String, value: String): EnvWriteResult =
        write(config, scope, key, value)

    fun delete(config: EnvStoreConfig, scope: EnvPersistScope, key: String): EnvWriteResult =
        write(config, scope, key, null)

    fun formatExport(snapshot: EnvSnapshot): String = buildString {
        appendLine("------------Persistent user environment---------------")
        snapshot.user.forEach { appendLine("${it.key}=${it.value}") }
        appendLine()
        appendLine("------------Persistent system environment---------------")
        snapshot.system.forEach { appendLine("${it.key}=${it.value}") }
        appendLine()
        appendLine("------------Current process environment---------------")
        snapshot.process.forEach { appendLine("${it.key}=${it.value}") }
        appendLine()
        appendLine("------------Compose runtime---------------")
        snapshot.runtime.forEach { appendLine("${it.key}=${it.value}") }
    }

    private fun write(config: EnvStoreConfig, scope: EnvPersistScope, key: String, value: String?): EnvWriteResult {
        validateName(key)
        if (value != null) validateValue(value)
        val before = snapshot(config)
        val diff = previewDiff(before, scope, key, value)
        if (config.osFamily == OsFamily.Windows) {
            writeWindows(config, scope, key, value)
            val after = snapshot(config)
            return EnvWriteResult(after, null, diff)
        }
        val destination = if (scope == EnvPersistScope.User) config.userFile else config.systemFile
        val current = readFileOrEmpty(destination)
        val shellExport = scope == EnvPersistScope.User || config.osFamily == OsFamily.Mac
        val content = updateContent(current, key, value, shellExport)
        val backup = backupIfPresent(config, scope, destination)
        if (scope == EnvPersistScope.User) {
            destination.parent?.createDirectories()
            atomicWrite(destination, content, ownerOnly = true)
            if (config.installShellHook) ensureShellHook(config)
            if (config.applyLaunchctl && config.osFamily == OsFamily.Mac) {
                val args = if (value == null) listOf("unsetenv", key) else listOf("setenv", key, value)
                runCatching { runCommand(listOf("/bin/launchctl") + args, 30_000) }
            }
        } else {
            try {
                atomicWrite(destination, content, ownerOnly = false)
            } catch (error: IOException) {
                if (!config.allowElevation) {
                    throw EnvException(EnvErrorCode.PERMISSION, error.message ?: destination.toString())
                }
                writeElevated(config, destination, content)
            }
        }
        val after = snapshot(config)
        return EnvWriteResult(after, backup?.toString(), diff)
    }

    private fun persistentEntries(config: EnvStoreConfig, scope: EnvPersistScope): List<EnvEntry> {
        if (config.osFamily == OsFamily.Windows) return readWindows(scope)
        val path = if (scope == EnvPersistScope.User) config.userFile else config.systemFile
        if (path.toString().isEmpty()) return emptyList()
        return try {
            parseContent(readFileOrEmpty(path))
        } catch (error: IOException) {
            throw EnvException(classifyIo(error), error.message ?: path.toString())
        }
    }

    private fun runtimeEntries(): List<EnvEntry> {
        val keys = listOf(
            "java.version", "java.vendor", "java.home", "java.vm.name", "java.vm.version",
            "os.name", "os.arch", "os.version", "user.dir", "user.home", "user.language",
            "user.timezone", "file.encoding", "native.encoding"
        )
        val fromSystem = keys.mapNotNull { key ->
            System.getProperty(key)?.let { EnvEntry(key, it) }
        }
        val product = listOf(
            EnvEntry("mootool.product", ProductIdentity.DISPLAY_NAME),
            EnvEntry("mootool.version", ProductIdentity.VERSION),
            EnvEntry("mootool.id", ProductIdentity.PRODUCT_ID)
        )
        return (product + fromSystem).sortedBy { it.key.lowercase() }
    }

    private fun readFileOrEmpty(path: Path): String {
        if (!path.exists()) return ""
        if (!path.isRegularFile()) throw IOException("Not a regular file: $path")
        return path.readText(StandardCharsets.UTF_8)
    }

    private fun backupIfPresent(config: EnvStoreConfig, scope: EnvPersistScope, destination: Path): Path? {
        if (!destination.exists() || !destination.isRegularFile()) return null
        config.backupDir.createDirectories()
        val name = "environment-${scope.name.lowercase()}-${stamp.format(Instant.now())}.bak"
        val backup = config.backupDir.resolve(name)
        Files.copy(destination, backup, StandardCopyOption.REPLACE_EXISTING)
        return backup
    }

    private fun atomicWrite(destination: Path, content: String, ownerOnly: Boolean) {
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
        if (ownerOnly) {
            runCatching {
                val file = temp.toFile()
                file.setReadable(false, false)
                file.setWritable(false, false)
                file.setExecutable(false, false)
                file.setReadable(true, true)
                file.setWritable(true, true)
            }
        }
        try {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: IOException) {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun ensureShellHook(config: EnvStoreConfig) {
        val profile = config.shellProfile
        val current = if (profile.exists()) profile.readText(StandardCharsets.UTF_8) else ""
        if (current.contains(SHELL_HOOK_BEGIN)) return
        if (profile.exists() && profile.isRegularFile()) {
            backupIfPresent(config, EnvPersistScope.User, profile)
        }
        val quoted = shellQuote(config.userFile.toString())
        val block = "$SHELL_HOOK_BEGIN\n[ -f $quoted ] && . $quoted\n$SHELL_HOOK_END\n"
        val separator = if (current.isEmpty() || current.endsWith("\n")) "" else "\n"
        profile.parent?.createDirectories()
        profile.writeText(current + separator + block, StandardCharsets.UTF_8)
    }

    private fun writeElevated(config: EnvStoreConfig, destination: Path, content: String) {
        config.backupDir.createDirectories()
        val temporary = config.backupDir.resolve("mootool-env-elevated-${ProcessHandle.current().pid()}.tmp")
        Files.writeString(temporary, content, StandardCharsets.UTF_8)
        try {
            when (config.osFamily) {
                OsFamily.Mac -> {
                    val command = "/bin/cp ${shellQuote(temporary.toString())} ${shellQuote(destination.toString())} && /usr/bin/chmod 644 ${shellQuote(destination.toString())}"
                    val script = "do shell script ${appleScriptString(command)} with administrator privileges"
                    runCommand(listOf("/usr/bin/osascript", "-e", script), 120_000)
                }
                OsFamily.Linux -> runCommand(
                    listOf("/usr/bin/pkexec", "/bin/sh", "-c", "/bin/cp \"\$1\" \"\$2\" && /bin/chmod 644 \"\$2\"", "mootool-next-compose", temporary.toString(), destination.toString()),
                    120_000
                )
                OsFamily.Windows -> throw EnvException(EnvErrorCode.UNSUPPORTED, "UNSUPPORTED")
            }
        } catch (error: EnvException) {
            throw error
        } catch (error: Exception) {
            throw EnvException(classifyIo(error), error.message ?: destination.toString())
        } finally {
            runCatching { Files.deleteIfExists(temporary) }
        }
    }

    private fun readWindows(scope: EnvPersistScope): List<EnvEntry> {
        val target = if (scope == EnvPersistScope.User) "User" else "Machine"
        val script = """
            [Console]::OutputEncoding=[Text.Encoding]::UTF8
            [Environment]::GetEnvironmentVariables([EnvironmentVariableTarget]::$target).GetEnumerator() |
              Sort-Object Name |
              ForEach-Object { '{0}={1}' -f ${'$'}_.Key, (${'$'}_.Value -replace '`n',' ') }
        """.trimIndent()
        val output = runCommand(listOf("powershell.exe", "-NoProfile", "-Command", script), 30_000)
        return output.lineSequence()
            .map { it.trim() }
            .filter { it.contains('=') }
            .map { line ->
                val index = line.indexOf('=')
                EnvEntry(line.substring(0, index), line.substring(index + 1))
            }
            .sortedBy { it.key.lowercase() }
            .toList()
    }

    private fun writeWindows(config: EnvStoreConfig, scope: EnvPersistScope, key: String, value: String?) {
        val target = if (scope == EnvPersistScope.User) "User" else "Machine"
        val psValue = if (value == null) "${'$'}null" else "'${psQuote(value)}'"
        val script = "[Environment]::SetEnvironmentVariable('${psQuote(key)}',$psValue,[EnvironmentVariableTarget]::$target)"
        val encoded = Base64.getEncoder().encodeToString(script.toByteArray(Charsets.UTF_16LE))
        if (scope == EnvPersistScope.User) {
            runCommand(listOf("powershell.exe", "-NoProfile", "-EncodedCommand", encoded), 30_000)
            return
        }
        if (!config.allowElevation) throw EnvException(EnvErrorCode.PERMISSION, "PERMISSION")
        val elevated = "${'$'}p=Start-Process -FilePath powershell.exe -Verb RunAs -Wait -PassThru -ArgumentList @('-NoProfile','-EncodedCommand','$encoded'); exit ${'$'}p.ExitCode"
        runCommand(listOf("powershell.exe", "-NoProfile", "-Command", elevated), 120_000)
    }

    private fun runCommand(command: List<String>, timeoutMs: Long): String {
        return try {
            val process = ProcessBuilder(command).start()
            val stdout = process.inputStream.readBytes()
            val stderr = process.errorStream.readBytes()
            val finished = process.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                throw EnvException(EnvErrorCode.COMMAND_FAILED, "TIMEOUT")
            }
            if (process.exitValue() != 0) {
                val message = stderr.decodeToString().ifBlank { stdout.decodeToString() }.ifBlank { "${command.first()} exited ${process.exitValue()}" }
                throw EnvException(classifyIo(IOException(message)), message.trim())
            }
            stdout.decodeToString()
        } catch (error: EnvException) {
            throw error
        } catch (error: IOException) {
            throw EnvException(classifyIo(error), error.message ?: command.first())
        }
    }

    private fun classifyIo(error: Throwable): EnvErrorCode {
        val message = (error.message.orEmpty() + " " + error.javaClass.simpleName).lowercase()
        return when {
            "permission" in message || "access" in message || "eacces" in message || "eperm" in message || "error=13" in message ->
                EnvErrorCode.PERMISSION
            "enoent" in message || "error=2" in message || "no such file" in message -> EnvErrorCode.COMMAND_FAILED
            else -> EnvErrorCode.COMMAND_FAILED
        }
    }

    internal fun serialize(key: String, value: String, shellExport: Boolean): String {
        return if (shellExport) {
            "export $key='${value.replace("'", "'\\''")}'"
        } else {
            "${key}=\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        }
    }

    internal fun unquote(value: String): String {
        if (value.length >= 2 && value.startsWith("'") && value.endsWith("'")) {
            return value.substring(1, value.lastIndex).replace("'\\''", "'")
        }
        if (value.length >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.lastIndex).replace("\\\"", "\"").replace("\\\\", "\\")
        }
        return value
    }

    internal fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    private fun appleScriptString(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun psQuote(value: String): String = value.replace("'", "''")
}
