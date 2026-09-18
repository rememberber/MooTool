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

    /** 对齐 Electron `formatRuntimeSource`；UI 格式化按钮与 `Cmd/Ctrl+Shift+F` 走此路径。 */
    fun formatSource(code: String, runtime: CodeRuntime): String =
        CodeRunEngine.formatSource(code, runtime)

    /** 对齐 Electron `runtimeDisplayName`（Tab 标签、横幅、历史摘要）。 */
    fun displayName(runtime: CodeRuntime): String = CodeRunEngine.displayName(runtime)

    fun cancelRun(requestId: String): Boolean = CodeRunEngine.cancel(requestId)

    /** 切页/关闭应用时终止全部在途运行（对齐 F05 切页取消链）。 */
    fun cancelAllRuns() {
        CodeRunEngine.cancelAll()
    }

    fun shouldToastRunFailure(errorCode: CodeRunErrorCode?): Boolean = errorCode != CodeRunErrorCode.ABORTED

    fun shouldToastValidationFailure(): Boolean = true
}
