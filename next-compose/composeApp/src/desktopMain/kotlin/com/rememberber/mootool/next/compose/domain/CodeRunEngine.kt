package com.rememberber.mootool.next.compose.domain

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isExecutable
import kotlin.io.path.pathString

enum class CodeRuntime { Java, Groovy, Python, Node }

enum class CodeRunErrorCode { INVALID_REQUEST, MISSING_RUNTIME, START_FAILED, TIMEOUT, ABORTED, TRUNCATED }

data class CodeRunPaths(
    val java: String = "",
    val groovy: String = "",
    val python: String = "",
    val node: String = ""
)

data class CodeRunInput(
    val requestId: String,
    val runtime: CodeRuntime,
    val code: String,
    val timeoutMs: Int = 30_000,
    val arguments: List<String> = emptyList(),
    val workingDirectory: String = ""
)

data class CodeRunResult(
    val requestId: String,
    val runtime: CodeRuntime,
    val command: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int? = null,
    val durationMs: Long,
    val timedOut: Boolean = false,
    val cancelled: Boolean = false,
    val truncated: Boolean = false,
    val errorCode: CodeRunErrorCode? = null,
    val statusText: String = ""
)

data class CodeRuntimeStatus(
    val id: CodeRuntime,
    val available: Boolean,
    val command: String,
    val version: String
)

data class CodeRunOutputEvent(
    val requestId: String,
    val stream: String,
    val text: String
)

class CodeRunException(val code: CodeRunErrorCode, message: String) : RuntimeException(message)

private class ActiveCodeRun(
    val process: Process,
    val cancelled: AtomicBoolean = AtomicBoolean(false),
    val timedOut: AtomicBoolean = AtomicBoolean(false),
    val truncated: AtomicBoolean = AtomicBoolean(false)
)

object CodeRunEngine {
    const val MAX_CODE_BYTES = 1024 * 1024
    const val HISTORY_LIMIT_HINT = 2 * 1024 * 1024
    val SAMPLES: Map<CodeRuntime, String> = mapOf(
        CodeRuntime.Java to """
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello from MooTool");
                }
            }
        """.trimIndent(),
        CodeRuntime.Groovy to """
            def tools = ['Java', 'Python', 'Node.js']
            tools.eachWithIndex { tool, index ->
                println "${'$'}{index + 1}. ${'$'}{tool}"
            }
        """.trimIndent(),
        CodeRuntime.Python to """
            tools = ["Java", "Python", "Node.js"]
            for index, tool in enumerate(tools, start=1):
                print(f"{index}. {tool}")
        """.trimIndent(),
        CodeRuntime.Node to """
            const tools = ['Java', 'Python', 'Node.js']
            tools.forEach((tool, index) => {
              console.log(`${'$'}{index + 1}. ${'$'}{tool}`)
            })
        """.trimIndent()
    )
    private val publicType = Regex("""\bpublic\s+(?:(?:abstract|final|sealed|non-sealed)\s+)*(?:class|record|interface|enum)\s+([A-Za-z_$][\w$]*)""")
    private val requestIdPattern = Regex("^[A-Za-z0-9_-]{8,80}$")
    private val allowedEnv = setOf(
        "PATH", "HOME", "USERPROFILE", "TMPDIR", "TMP", "TEMP", "SystemRoot", "WINDIR",
        "LANG", "LC_ALL", "JAVA_HOME", "GROOVY_HOME"
    )
    private val active = ConcurrentHashMap<String, ActiveCodeRun>()
    @Volatile var maxOutputBytes: Int = 2 * 1024 * 1024

    fun clampTimeout(value: Int): Int = value.coerceIn(1_000, 120_000)

    fun parseProvider(value: String): CodeRuntime = when (value.lowercase(Locale.ROOT)) {
        "groovy" -> CodeRuntime.Groovy
        "python" -> CodeRuntime.Python
        "node" -> CodeRuntime.Node
        else -> CodeRuntime.Java
    }

    fun displayName(runtime: CodeRuntime): String = when (runtime) {
        CodeRuntime.Node -> "Node.js"
        else -> runtime.name
    }

    fun syntax(runtime: CodeRuntime): String = when (runtime) {
        CodeRuntime.Java -> org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_JAVA
        CodeRuntime.Groovy -> org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_GROOVY
        CodeRuntime.Python -> org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_PYTHON
        CodeRuntime.Node -> org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT
    }

    fun publicTypeName(code: String): String =
        publicType.find(code)?.groupValues?.get(1) ?: "Main"

    fun parseArguments(value: String): List<String> {
        val result = ArrayList<String>()
        var current = StringBuilder()
        var quote = '\u0000'
        var escaped = false
        fun push() {
            if (current.isNotEmpty()) {
                result += current.toString()
                current = StringBuilder()
            }
        }
        for (character in value.trim()) {
            when {
                escaped -> {
                    current.append(character)
                    escaped = false
                }
                character == '\\' && quote != '\'' -> escaped = true
                quote != '\u0000' -> {
                    if (character == quote) quote = '\u0000' else current.append(character)
                }
                character == '"' || character == '\'' -> quote = character
                character.isWhitespace() -> push()
                else -> current.append(character)
            }
        }
        if (escaped || quote != '\u0000') throw CodeRunException(CodeRunErrorCode.INVALID_REQUEST, "Unterminated runtime argument")
        push()
        if (result.size > 40 || result.any { it.length > 1000 }) {
            throw CodeRunException(CodeRunErrorCode.INVALID_REQUEST, "Too many runtime arguments")
        }
        return result
    }

    fun formatSource(code: String, runtime: CodeRuntime): String {
        if (code.isBlank()) return ""
        return when (runtime) {
            CodeRuntime.Java -> ReformatEngine.format(code, ReformatType.Java, 4)
            else -> code.replace("\t", "    ").split(Regex("\\r?\\n")).joinToString("\n") { it.trimEnd() }.trimEnd()
        }
    }

    fun commandFor(runtime: CodeRuntime, paths: CodeRunPaths): String {
        val configured = pathFor(runtime, paths).trim()
        if (configured.isNotEmpty()) return configured
        return when (runtime) {
            CodeRuntime.Java -> "java"
            CodeRuntime.Groovy -> "groovy"
            CodeRuntime.Python -> if (isWindows()) "python" else "python3"
            CodeRuntime.Node -> "node"
        }
    }

    fun detect(paths: CodeRunPaths): List<CodeRuntimeStatus> =
        CodeRuntime.entries.map { runtime -> detectOne(runtime, commandFor(runtime, paths)) }

    fun run(
        input: CodeRunInput,
        paths: CodeRunPaths,
        tempRoot: Path,
        onOutput: (CodeRunOutputEvent) -> Unit = {}
    ): CodeRunResult {
        val startedAt = System.currentTimeMillis()
        fun failed(code: CodeRunErrorCode, message: String) = CodeRunResult(
            requestId = input.requestId,
            runtime = input.runtime,
            command = "",
            stdout = "",
            stderr = "",
            durationMs = System.currentTimeMillis() - startedAt,
            errorCode = code,
            statusText = message
        )
        try {
            validate(input)
            if (active.containsKey(input.requestId)) {
                return failed(CodeRunErrorCode.INVALID_REQUEST, "Runtime request is already active")
            }
        } catch (error: CodeRunException) {
            return failed(error.code, error.message ?: error.code.name)
        }
        Files.createDirectories(tempRoot)
        val directory = Files.createTempDirectory(tempRoot, "run-")
        return try {
            val definition = definition(input.runtime, paths, directory, input.code)
            Files.writeString(definition.file, input.code, StandardCharsets.UTF_8)
            val workingDirectory = resolveWorkingDirectory(input.workingDirectory, directory)
            val arguments = definition.args + input.arguments
            val process = startProcess(definition.command, arguments, workingDirectory)
            val execution = ActiveCodeRun(process)
            active[input.requestId] = execution
            val timeoutMs = clampTimeout(input.timeoutMs)
            val stdout = StringBuilder()
            val stderr = StringBuilder()
            val outputBytes = AtomicInteger(0)
            val stdoutThread = streamReader(process.inputStream, "stdout", input.requestId, execution, stdout, outputBytes, onOutput)
            val stderrThread = streamReader(process.errorStream, "stderr", input.requestId, execution, stderr, outputBytes, onOutput)
            process.outputStream.close()
            val finished = process.waitFor(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
            if (!finished && !execution.cancelled.get() && !execution.truncated.get()) {
                execution.timedOut.set(true)
                terminate(process)
                process.waitFor(2_000, TimeUnit.MILLISECONDS)
            }
            stdoutThread.join(1_000)
            stderrThread.join(1_000)
            val exit = if (process.isAlive) null else runCatching { process.exitValue() }.getOrNull()
            val error = when {
                execution.cancelled.get() -> CodeRunErrorCode.ABORTED
                execution.timedOut.get() -> CodeRunErrorCode.TIMEOUT
                execution.truncated.get() -> CodeRunErrorCode.TRUNCATED
                else -> null
            }
            CodeRunResult(
                requestId = input.requestId,
                runtime = input.runtime,
                command = (listOf(definition.command) + arguments).joinToString(" ") { displayArgument(it) },
                stdout = stdout.toString(),
                stderr = stderr.toString(),
                exitCode = exit,
                durationMs = System.currentTimeMillis() - startedAt,
                timedOut = execution.timedOut.get(),
                cancelled = execution.cancelled.get(),
                truncated = execution.truncated.get(),
                errorCode = error
            )
        } catch (error: CodeRunException) {
            CodeRunResult(
                requestId = input.requestId,
                runtime = input.runtime,
                command = "",
                stdout = "",
                stderr = "",
                durationMs = System.currentTimeMillis() - startedAt,
                errorCode = error.code,
                statusText = error.message ?: error.code.name
            )
        } catch (error: IOException) {
            val missing = error.message.orEmpty().let {
                it.contains("Cannot run program") || it.contains("No such file") || it.contains("error=2")
            }
            CodeRunResult(
                requestId = input.requestId,
                runtime = input.runtime,
                command = "",
                stdout = "",
                stderr = "",
                durationMs = System.currentTimeMillis() - startedAt,
                errorCode = if (missing) CodeRunErrorCode.MISSING_RUNTIME else CodeRunErrorCode.START_FAILED,
                statusText = "Unable to start ${input.runtime.name.lowercase()}: ${error.message}"
            )
        } finally {
            active.remove(input.requestId)
            runCatching { directory.toFile().deleteRecursively() }
        }
    }

    fun cancel(requestId: String): Boolean {
        val execution = active[requestId] ?: return false
        execution.cancelled.set(true)
        terminate(execution.process)
        return true
    }

    fun cancelAll() {
        active.keys.toList().forEach { cancel(it) }
    }

    fun resetForTests() {
        cancelAll()
        maxOutputBytes = 2 * 1024 * 1024
    }

    private fun detectOne(runtime: CodeRuntime, command: String): CodeRuntimeStatus {
        return try {
            val args = if (runtime == CodeRuntime.Java) listOf("-version") else listOf("--version")
            val process = startProcess(command, args, Path.of(System.getProperty("java.io.tmpdir")), mergeError = true)
            val outputBytes = process.inputStream.readBytes()
            val finished = process.waitFor(3_000, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                return CodeRuntimeStatus(runtime, false, command, "")
            }
            val output = outputBytes.toString(StandardCharsets.UTF_8).trim().lineSequence().firstOrNull().orEmpty()
            CodeRuntimeStatus(runtime, process.exitValue() == 0, command, output)
        } catch (_: Exception) {
            CodeRuntimeStatus(runtime, false, command, "")
        }
    }

    private fun validate(input: CodeRunInput) {
        if (!requestIdPattern.matches(input.requestId)) {
            throw CodeRunException(CodeRunErrorCode.INVALID_REQUEST, "Invalid runtime request id")
        }
        val bytes = input.code.toByteArray(StandardCharsets.UTF_8)
        if (bytes.size > MAX_CODE_BYTES) {
            throw CodeRunException(CodeRunErrorCode.INVALID_REQUEST, "Code exceeds 1 MB limit")
        }
        if (input.arguments.size > 40 || input.arguments.any { it.length > 1000 || it.contains('\u0000') }) {
            throw CodeRunException(CodeRunErrorCode.INVALID_REQUEST, "Invalid runtime arguments")
        }
        if (input.workingDirectory.length > 1000 || input.workingDirectory.contains('\u0000')) {
            throw CodeRunException(CodeRunErrorCode.INVALID_REQUEST, "Invalid runtime working directory")
        }
    }

    private data class CommandDefinition(val command: String, val args: List<String>, val file: Path)

    private fun definition(runtime: CodeRuntime, paths: CodeRunPaths, directory: Path, code: String): CommandDefinition {
        val command = commandFor(runtime, paths)
        return when (runtime) {
            CodeRuntime.Java -> {
                val file = directory.resolve("${publicTypeName(code)}.java")
                CommandDefinition(command, listOf(file.pathString), file)
            }
            CodeRuntime.Groovy -> CommandDefinition(command, listOf(directory.resolve("main.groovy").pathString), directory.resolve("main.groovy"))
            CodeRuntime.Python -> CommandDefinition(command, listOf("-u", directory.resolve("main.py").pathString), directory.resolve("main.py"))
            CodeRuntime.Node -> CommandDefinition(command, listOf(directory.resolve("main.mjs").pathString), directory.resolve("main.mjs"))
        }
    }

    private fun resolveWorkingDirectory(value: String, fallback: Path): Path {
        if (value.isBlank()) return fallback
        val resolved = runCatching { Path.of(value.trim()).toRealPath() }
            .getOrElse { throw CodeRunException(CodeRunErrorCode.INVALID_REQUEST, "Runtime working directory is not a directory") }
        if (!resolved.isDirectory()) throw CodeRunException(CodeRunErrorCode.INVALID_REQUEST, "Runtime working directory is not a directory")
        return resolved
    }

    private fun startProcess(command: String, args: List<String>, workingDirectory: Path, mergeError: Boolean = false): Process {
        val executable = resolveExecutable(command)
        val builder = ProcessBuilder(listOf(executable) + args)
        builder.directory(workingDirectory.toFile())
        builder.redirectErrorStream(mergeError)
        val env = builder.environment()
        env.keys.toList().forEach { key ->
            if (key.uppercase(Locale.ROOT) !in allowedEnv) env.remove(key)
        }
        return builder.start()
    }

    private fun resolveExecutable(command: String): String {
        val path = Path.of(command)
        return if ((path.isAbsolute || command.contains('/') || command.contains('\\')) && path.exists()) {
            if (!path.isExecutable() && !isWindows()) command else path.pathString
        } else {
            command
        }
    }

    private fun streamReader(
        stream: java.io.InputStream,
        name: String,
        requestId: String,
        execution: ActiveCodeRun,
        target: StringBuilder,
        outputBytes: AtomicInteger,
        onOutput: (CodeRunOutputEvent) -> Unit
    ): Thread {
        return Thread({
            val buffer = ByteArray(8_192)
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                if (execution.truncated.get()) continue
                val remaining = maxOutputBytes - outputBytes.get()
                if (remaining <= 0) {
                    execution.truncated.set(true)
                    terminate(execution.process)
                    break
                }
                val take = minOf(read, remaining)
                val text = String(buffer, 0, take, StandardCharsets.UTF_8)
                outputBytes.addAndGet(take)
                synchronized(target) { target.append(text) }
                onOutput(CodeRunOutputEvent(requestId, name, text))
                if (take < read) {
                    execution.truncated.set(true)
                    terminate(execution.process)
                    break
                }
            }
        }, "code-run-$name").apply {
            isDaemon = true
            start()
        }
    }

    private fun terminate(process: Process) {
        val pid = process.pid()
        if (isWindows()) {
            runCatching {
                ProcessBuilder("taskkill", "/PID", pid.toString(), "/T", "/F").redirectErrorStream(true).start()
            }
        } else {
            val handle = process.toHandle()
            handle.descendants().forEach { runCatching { it.destroy() } }
            runCatching { handle.destroy() }
            runCatching { ProcessBuilder("kill", "-TERM", "-$pid").start() }
        }
        Thread({
            Thread.sleep(1_200)
            if (process.isAlive) {
                if (isWindows()) {
                    runCatching { ProcessBuilder("taskkill", "/PID", pid.toString(), "/T", "/F").start() }
                } else {
                    val handle = process.toHandle()
                    handle.descendants().forEach { runCatching { it.destroyForcibly() } }
                    runCatching { handle.destroyForcibly() }
                    runCatching { ProcessBuilder("kill", "-KILL", "-$pid").start() }
                }
            }
        }, "code-run-kill").apply { isDaemon = true; start() }
    }

    private fun displayArgument(value: String): String =
        if (Regex("""[\s"']""").containsMatchIn(value)) {
            "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        } else value

    private fun pathFor(runtime: CodeRuntime, paths: CodeRunPaths): String = when (runtime) {
        CodeRuntime.Java -> paths.java
        CodeRuntime.Groovy -> paths.groovy
        CodeRuntime.Python -> paths.python
        CodeRuntime.Node -> paths.node
    }

    private fun isWindows(): Boolean = System.getProperty("os.name").orEmpty().lowercase(Locale.ROOT).contains("win")
}
