package com.rememberber.mootool.next.compose.domain

import com.rememberber.mootool.next.compose.model.RuntimeSettings
import java.nio.file.Path

/** F05 设置 → 检测/运行守卫（可单测，对齐 Electron 运行台可用性语义）。 */
object CodeRunWiringPresentation {
    fun pathsFrom(settings: RuntimeSettings): CodeRunPaths =
        CodeRunPaths(settings.javaPath, settings.groovyPath, settings.pythonPath, settings.nodePath)

    fun availableCount(statuses: List<CodeRuntimeStatus>): Int = statuses.count { it.available }

    fun showConfigureBanner(status: CodeRuntimeStatus?): Boolean = status?.available == false

    fun canRun(status: CodeRuntimeStatus?): Boolean = status?.available != false

    fun runDetect(paths: CodeRunPaths): List<CodeRuntimeStatus> = CodeRunEngine.detect(paths)

    sealed interface ArgumentsOutcome {
        data class Success(val arguments: List<String>) : ArgumentsOutcome
        data class Failure(val error: Exception) : ArgumentsOutcome
    }

    fun parseRunArguments(raw: String): ArgumentsOutcome =
        runCatching { CodeRunEngine.parseArguments(raw) }.fold(
            onSuccess = { ArgumentsOutcome.Success(it) },
            onFailure = { ArgumentsOutcome.Failure(it as? Exception ?: Exception(it)) },
        )

    fun runCode(
        input: CodeRunInput,
        paths: CodeRunPaths,
        cacheRoot: Path,
        onOutput: (CodeRunOutputEvent) -> Unit = {},
    ): CodeRunResult = CodeRunEngine.run(input, paths, cacheRoot, onOutput)
}
